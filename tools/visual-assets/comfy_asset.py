"""Project-owned ComfyUI candidate client; never installs or deploys anything."""

from __future__ import annotations

import argparse
from collections import deque
from contextlib import contextmanager
from datetime import datetime, timezone
import hashlib
import io
import json
import math
import os
from pathlib import Path, PurePosixPath
import re
import sys
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode, urlsplit
from urllib.request import HTTPRedirectHandler, ProxyHandler, Request, build_opener
import uuid


HERE = Path(__file__).resolve().parent
TRANSFORM = "edge-key-largest-component-nearest-palette-v1"
MAX_RESPONSE = 16 * 1024 * 1024
NODES = {
    "CheckpointLoaderSimple", "CLIPTextEncode", "EmptyLatentImage",
    "KSampler", "VAEDecode", "SaveImage",
}


class ContractError(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise ContractError(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def file_sha256(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def json_bytes(value):
    return (json.dumps(value, sort_keys=True, indent=2, ensure_ascii=True) + "\n").encode("ascii")


def read_json(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def atomic_json(path, value):
    path = Path(path)
    with tempfile.NamedTemporaryFile(dir=path.parent, prefix=".visual-", delete=False) as stream:
        temporary = Path(stream.name)
        try:
            stream.write(json_bytes(value))
            stream.flush()
            os.fsync(stream.fileno())
        except OSError:
            temporary.unlink(missing_ok=True)
            raise
    try:
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


def rgb(value):
    require(isinstance(value, str) and re.fullmatch(r"#[0-9A-Fa-f]{6}", value),
            "Palette/background colors must be #RRGGBB.")
    return tuple(int(value[index:index + 2], 16) for index in (1, 3, 5))


def load_spec(path):
    spec = read_json(path)
    if spec.get("schema") == "grindless.visual-asset.v2":
        from visual_presets import validate_spec
        return validate_spec(spec)
    require(spec.get("schema") == "grindless.visual-asset.v1", "Unsupported asset specification.")
    require(re.fullmatch(r"[a-z][a-z0-9_]{0,63}", spec.get("asset_id", "")), "Invalid asset id.")
    require(spec.get("registry_id") == "grindless:" + spec["asset_id"], "Registry identity mismatch.")
    require(spec.get("family") in ("item", "material"), "Only the item/material workflow is qualified.")
    require(spec.get("tier") == "T0", "This bounded workflow does not open future tiers.")
    for key in ("role", "positive_prompt", "negative_prompt"):
        require(isinstance(spec.get(key), str) and 1 <= len(spec[key]) <= 2000, "Missing/bounded " + key)
    require(re.fullmatch(r"[0-9a-f]{40}", spec.get("source_revision", "")), "Source revision must be exact.")
    require(isinstance(spec.get("source_refs"), list) and spec["source_refs"], "Source references are required.")
    visual = spec.get("visual", {})
    for key in ("materials", "silhouette", "motif", "wear", "state", "tier_readability"):
        require(bool(visual.get(key)), "Missing visual requirement: " + key)
    require(visual.get("lighting") == "upper-left", "Preserve the declared lighting convention.")
    require(visual.get("orientation") == "head-up", "Only head-up items are currently qualified.")
    colors = visual.get("palette", [])
    require(isinstance(colors, list) and 3 <= len(colors) <= 16, "Use a bounded shared material palette.")
    require(len(set(colors)) == len(colors), "Duplicate palette colors.")
    for color in colors:
        rgb(color)
    seeds = spec.get("seeds", [])
    require(isinstance(seeds, list) and 1 <= len(seeds) <= 2, "At most two source candidates.")
    require(all(type(seed) is int and 0 <= seed < 2**63 for seed in seeds), "Invalid seed.")
    require(len(set(seeds)) == len(seeds), "Seeds must be distinct; cache hits are not independent proof.")
    transform = spec.get("transform", {})
    require(transform.get("size") == 16 and transform.get("margin") == 1, "Runtime sprites must be 16px with 1px margin.")
    require(transform.get("resampling") == "nearest" and transform.get("alpha") == "binary",
            "Nearest-neighbor pixels and hard alpha are required.")
    require(transform.get("rotation") == 0 and transform.get("mirror") is False,
            "Never silently rotate or mirror the generated object.")
    require(type(transform.get("key_tolerance")) is int and 0 <= transform["key_tolerance"] <= 64,
            "Unsafe background tolerance.")
    rgb(transform.get("background"))
    low, high = transform.get("minimum_foreground_fraction"), transform.get("maximum_foreground_fraction")
    require(type(low) in (float, int) and type(high) in (float, int) and 0 < low < high < 1,
            "Invalid foreground bounds.")
    dust = transform.get("maximum_dust_fraction")
    require(type(dust) in (float, int) and 0 <= dust <= 0.02, "Unsafe dust allowance.")
    expected = f"common/src/main/resources/assets/grindless/textures/item/{spec['asset_id']}.png"
    require(spec.get("runtime_path") == expected, "Runtime path must match the asset identity.")
    require(spec.get("human_visual_approval") is False, "A technical specification cannot grant human approval.")
    if "conditioning" in spec:
        conditioning = spec["conditioning"]
        require(conditioning.get("kind") == "original-multitool-guide-v1" and spec["asset_id"] == "multitool",
                "Only the original Multitool guide is qualified.")
        require(conditioning.get("filename") == "grindless-multitool-guide-v1.png",
                "Use the isolated guide basename.")
        require(re.fullmatch(r"[0-9a-f]{64}", conditioning.get("sha256", "")), "Guide hash is required.")
        require(conditioning.get("denoise") == 0.35, "The conditioned prototype uses the approved 0.35 denoise.")
    return spec


def template_path(spec):
    return HERE / ("item-conditioned-workflow.json" if "conditioning" in spec else "item-workflow.json")


def load_model(path):
    model = read_json(path)
    require(model.get("schema") == "grindless.comfy-model.v1", "Unsupported model lock.")
    require(re.fullmatch(r"[A-Za-z0-9_.-]+\.safetensors", model.get("filename", "")), "Unsafe checkpoint name.")
    require(re.fullmatch(r"[0-9a-f]{64}", model.get("sha256", "")), "Missing exact checkpoint hash.")
    require(type(model.get("bytes")) is int and model["bytes"] > 0, "Missing checkpoint length.")
    require(model.get("license") and model.get("source_url") and model.get("source_revision"),
            "Source and license must be resolved before generation.")
    require(model.get("provenance_verified") is True, "The model has not been provenance-qualified.")
    return model


def workflow(spec, model, slot, prefix):
    require(type(slot) is int and 1 <= slot <= len(spec["seeds"]), "Candidate slot exceeds its allowance.")
    if spec.get("schema") == "grindless.visual-asset.v2":
        require(model["sha256"] == spec["model_sha256"], "Preset graph/model differs from the frozen specification.")
    replacements = {
        "@checkpoint": model["filename"], "@positive": spec["positive_prompt"],
        "@negative": spec["negative_prompt"], "@seed": spec["seeds"][slot - 1], "@prefix": prefix,
    }
    if "conditioning" in spec:
        replacements.update({"@guide": spec["conditioning"]["filename"], "@denoise": spec["conditioning"]["denoise"]})

    def replace(value):
        if isinstance(value, str) and value.startswith("@"):
            require(value in replacements, "Unresolved workflow parameter.")
            return replacements[value]
        if isinstance(value, dict):
            return {key: replace(item) for key, item in value.items()}
        if isinstance(value, list):
            return [replace(item) for item in value]
        return value

    graph = replace(read_json(template_path(spec)))
    expected = (NODES - {"EmptyLatentImage"}) | {"LoadImage", "VAEEncode"} if "conditioning" in spec else NODES
    require({node["class_type"] for node in graph.values()} == expected, "Unexpected workflow node.")
    if "conditioning" not in spec:
        require(graph["4"]["inputs"] == {"width": 512, "height": 512, "batch_size": 1}, "Unbounded image batch.")
    require(graph["5"]["inputs"]["steps"] == 20, "Unreviewed inference budget.")
    return graph


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        raise ContractError("ComfyUI redirects are not allowed.")


class ComfyApi:
    def __init__(self, endpoint, timeout=15):
        parts = urlsplit(endpoint)
        require(parts.scheme == "http" and parts.hostname in ("127.0.0.1", "::1")
                and parts.port is not None and not parts.username and not parts.password
                and parts.path in ("", "/") and not parts.query and not parts.fragment,
                "Use an explicit loopback HTTP endpoint without credentials or URL parameters.")
        self.endpoint = endpoint.rstrip("/")
        self.timeout = timeout
        self.opener = build_opener(ProxyHandler({}), NoRedirect())

    def request(self, path, value=None, binary=False):
        require(path.startswith("/") and not path.startswith("//"), "Invalid API path.")
        body = None if value is None else json_bytes(value)
        request = Request(self.endpoint + path, body, {"Content-Type": "application/json"})
        with self.opener.open(request, timeout=self.timeout) as response:
            data = response.read(MAX_RESPONSE + 1)
        require(len(data) <= MAX_RESPONSE, "ComfyUI response exceeds its bounded size.")
        return data if binary else json.loads(data)

    def idle(self):
        queue = self.request("/queue")
        require(isinstance(queue.get("queue_running"), list) and isinstance(queue.get("queue_pending"), list),
                "Malformed queue response.")
        require(not queue["queue_running"] and not queue["queue_pending"],
                "ComfyUI has queued work. Preserve it; do not submit or cancel anything.")

    def qualify(self, model, expected_nodes=NODES):
        self.idle()
        for name in sorted(expected_nodes):
            info = self.request("/object_info/" + name)
            require(name in info, "Missing core node: " + name)
            if name == "CheckpointLoaderSimple":
                choices = info[name]["input"]["required"]["ckpt_name"][0]
                require(choices.count(model["filename"]) == 1, "The pinned checkpoint is not uniquely selectable.")
        stats = self.request("/system_stats")
        system = stats.get("system", {})
        return {
            "comfyui_version": system.get("comfyui_version"),
            "pytorch_version": system.get("pytorch_version"),
            "devices": [{key: device.get(key) for key in ("name", "type", "vram_total")}
                        for device in stats.get("devices", [])],
        }


@contextmanager
def run_lock(directory):
    path = directory / ".request.lock"
    try:
        descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError as error:
        raise ContractError("Run is locked; inspect its owner before any recovery. No automatic removal.") from error
    try:
        os.write(descriptor, str(os.getpid()).encode("ascii"))
        yield
    finally:
        os.close(descriptor)
        path.unlink()


def ledger_identity(spec, model):
    return {
        "schema": "grindless.comfy-run.v1", "asset_id": spec["asset_id"],
        "spec_sha256": sha256(json_bytes(spec)), "model_sha256": model["sha256"],
        "template_sha256": file_sha256(template_path(spec)),
        "maximum_candidates": len(spec["seeds"]), "human_visual_approval": False,
    }


def read_ledger(directory, spec, model):
    expected = ledger_identity(spec, model)
    path = directory / "run.json"
    ledger = read_json(path) if path.exists() else dict(expected, requests=[])
    if not all(ledger.get(key) == value for key, value in expected.items()):
        authorization_path = directory / "authorization-2.json"
        require(authorization_path.exists(), "Run identity has changed.")
        authorization = read_json(authorization_path)
        base = {key: ledger.get(key) for key in expected}
        require(authorization.get("schema") == "grindless.remaining-source-authorization.v1"
                and authorization.get("identity") == expected
                and authorization.get("base_identity") == base
                and authorization.get("allowed_slot") == 2 and authorization.get("maximum_total") == 2
                and authorization.get("specification") == spec
                and len(ledger["requests"]) >= 1
                and sha256(json_bytes(ledger["requests"][0])) == authorization.get("first_request_sha256"),
                "Remaining-candidate authorization does not match the immutable first source.")
    return ledger


def reserve(directory, spec, model, slot, graph):
    require(re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}", directory.name), "Use a safe owned run directory name.")
    require(not directory.is_symlink(), "Run directory cannot be a symlink.")
    directory.mkdir(parents=True, exist_ok=True)
    with run_lock(directory):
        ledger = read_ledger(directory, spec, model)
        require(len(ledger["requests"]) < len(spec["seeds"]), "Source candidate allowance is exhausted.")
        require(not any(row["slot"] == slot for row in ledger["requests"]), "This slot is spent; collect it, never resubmit.")
        row = {
            "slot": slot, "seed": spec["seeds"][slot - 1], "state": "reserved",
            "reserved_at": datetime.now(timezone.utc).isoformat(),
            "workflow_sha256": sha256(json_bytes(graph)), "prompt_id": None,
            "spec_sha256": sha256(json_bytes(spec)), "template_sha256": file_sha256(template_path(spec)),
        }
        ledger["requests"].append(row)
        atomic_json(directory / f"workflow-{slot}.json", graph)
        atomic_json(directory / f"spec-{slot}.json", spec)
        if not (directory / "spec.json").exists():
            atomic_json(directory / "spec.json", spec)
        atomic_json(directory / "run.json", ledger)
    return row


def revise_run(directory, spec, model, expected_hash, reason):
    require(reason and len(reason) <= 500, "A bounded explicit revision reason is required.")
    with run_lock(directory):
        path = directory / "run.json"
        require(file_sha256(path) == expected_hash, "Ledger changed; revision is not authorized for these bytes.")
        ledger = read_json(path)
        require(not (directory / "authorization-2.json").exists() and len(ledger["requests"]) == 1
                and ledger["requests"][0]["state"] == "collected",
                "Only one resolved first source may precede the one remaining-candidate revision.")
        old = read_json(directory / "spec.json")
        for key in ("asset_id", "registry_id", "family", "tier", "role", "source_revision", "visual", "transform", "seeds"):
            require(old.get(key) == spec.get(key), "Revision cannot change purpose, identity, visual contract or seeds.")
        require(ledger["model_sha256"] == model["sha256"] and ledger["maximum_candidates"] == len(spec["seeds"]) == 2,
                "Revision cannot reset or increase the allowance or change the model.")
        require("conditioning" in spec and "conditioning" not in old, "Only the explicit original-guide revision is supported.")
        authorization = {
            "schema": "grindless.remaining-source-authorization.v1",
            "reason": reason, "base_identity": {key: ledger[key] for key in ledger_identity(old, model)},
            "identity": ledger_identity(spec, model), "specification": spec,
            "allowed_slot": 2, "maximum_total": 2, "ledger_sha256_before": expected_hash,
            "first_request_sha256": sha256(json_bytes(ledger["requests"][0])),
            "utc": datetime.now(timezone.utc).isoformat(), "spent_before": 1, "remaining_after": 1,
        }
        with (directory / "authorization-2.json").open("xb") as stream:
            stream.write(json_bytes(authorization))
            stream.flush()
            os.fsync(stream.fileno())


def update_request(directory, slot, **changes):
    with run_lock(directory):
        ledger = read_json(directory / "run.json")
        row = next(row for row in ledger["requests"] if row["slot"] == slot)
        row.update(changes)
        atomic_json(directory / "run.json", ledger)


def canonical_prefix(directory, spec, slot):
    require(re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}", directory.name), "Invalid owned run name.")
    return f"grindless/{spec['asset_id']}/{directory.name}/candidate-{slot}"


def reserved_workflow(directory, ledger, row, spec, model, slot):
    require(row["slot"] == slot and type(row["seed"]) is int and row["seed"] == spec["seeds"][slot - 1],
            "Reserved slot/seed differs from the approved specification.")
    require(row.get("spec_sha256", ledger["spec_sha256"]) == sha256(json_bytes(spec))
            and row.get("template_sha256", ledger["template_sha256"]) == file_sha256(template_path(spec)),
            "Reserved specification/template identity differs.")
    data = (directory / f"workflow-{slot}.json").read_bytes()
    require(sha256(data) == row["workflow_sha256"], "Retained workflow bytes differ from the reserved digest.")
    graph = json.loads(data)
    approved = workflow(spec, model, slot, canonical_prefix(directory, spec, slot))
    require(sha256(json_bytes(graph)) == row["workflow_sha256"]
            and sha256(json_bytes(approved)) == row["workflow_sha256"],
            "Retained workflow does not equal the approved typed graph and canonical run/slot prefix.")
    return graph


def png_graph_proof(data, graph, require_metadata=False):
    Image = image_module()
    with Image.open(io.BytesIO(data)) as image:
        require(image.format == "PNG" and image.size == (512, 512)
                and not getattr(image, "is_animated", False), "Expected the owned single-frame 512px source PNG.")
        raw = image.info.get("prompt")
        if raw is None:
            require(not require_metadata, "Retained PNG has no prompt metadata; do not invent historical evidence.")
            proof = {"png_prompt_metadata": "absent"}
        else:
            require(isinstance(raw, str), "PNG prompt metadata is not JSON text.")
            observed = json.loads(raw)
            require(isinstance(observed, dict) and sha256(json_bytes(observed)) == sha256(json_bytes(graph)),
                    "PNG prompt graph differs from the reserved approved graph.")
            proof = {"png_prompt_metadata": "verified", "png_prompt_sha256": sha256(json_bytes(observed))}
        image.verify()
    return proof


def audit_retained(directory, spec, model, slot):
    ledger = read_ledger(directory, spec, model)
    rows = [row for row in ledger["requests"] if row["slot"] == slot]
    require(len(rows) == 1 and rows[0]["state"] == "collected", "Only a retained collected source can be audited.")
    row = rows[0]
    graph = reserved_workflow(directory, ledger, row, spec, model, slot)
    data = (directory / f"source-{slot}.png").read_bytes()
    require(sha256(data) == row["source_sha256"], "Retained source bytes differ from the original collection receipt.")
    proof = png_graph_proof(data, graph, require_metadata=True)
    return {
        "schema": "grindless.retained-source-integrity.v1", "slot": slot, "prompt_id": row["prompt_id"],
        "source_sha256": sha256(data), "workflow_sha256": row["workflow_sha256"],
        "spec_sha256": sha256(json_bytes(spec)), "model_sha256": model["sha256"],
        "canonical_prefix": canonical_prefix(directory, spec, slot), **proof,
        "evidence_source": "actual retained PNG prompt metadata, not a new history observation",
        "audited_at": datetime.now(timezone.utc).isoformat(),
        "network_requests": 0, "new_inference": False, "original_receipts_modified": False,
    }


def generate(api, directory, spec, model, checkpoint, slot, wait_seconds, input_directory=None):
    checkpoint = Path(checkpoint)
    require(checkpoint.name == model["filename"] and checkpoint.stat().st_size == model["bytes"],
            "Checkpoint filename or size differs from its lock.")
    require(file_sha256(checkpoint) == model["sha256"], "Checkpoint hash differs from its lock.")
    prefix = canonical_prefix(directory, spec, slot)
    graph = workflow(spec, model, slot, prefix)
    if "conditioning" in spec:
        require(input_directory is not None, "Conditioning needs the isolated server input directory.")
        guide = Path(input_directory) / spec["conditioning"]["filename"]
        require(file_sha256(guide) == spec["conditioning"]["sha256"], "Conditioning guide hash differs.")
        Image = image_module()
        with Image.open(guide) as image:
            require(image.format == "PNG" and image.size == (512, 512) and not getattr(image, "is_animated", False),
                    "Conditioning must be the single 512px original guide.")
    runtime = api.qualify(model, {node["class_type"] for node in graph.values()})
    reserve(directory, spec, model, slot, graph)
    atomic_json(directory / f"runtime-{slot}.json", runtime)
    api.idle()
    try:
        result = api.request("/prompt", {"prompt": graph, "client_id": "grindless-" + str(uuid.uuid4())})
    except (URLError, TimeoutError, OSError, ContractError, json.JSONDecodeError) as error:
        update_request(directory, slot, state="submission_unknown", error_type=type(error).__name__)
        raise ContractError("Prompt submission failed/unknown; the slot remains spent. Inspect before collecting; never retry POST.") from error
    prompt_id = result.get("prompt_id")
    require(isinstance(prompt_id, str) and re.fullmatch(r"[0-9a-fA-F-]{36}", prompt_id),
            "Missing prompt id; reservation remains spent, do not resubmit.")
    update_request(directory, slot, state="submitted", prompt_id=prompt_id)
    return collect(api, directory, spec, model, slot, wait_seconds)


def output_reference(images, prefix):
    require(isinstance(images, list) and len(images) == 1, "Expected exactly one owned output image.")
    image = images[0]
    filename, subfolder = image.get("filename", ""), image.get("subfolder", "")
    require(re.fullmatch(r"[A-Za-z0-9_.-]+\.png", filename) and ".." not in filename, "Unsafe image filename.")
    require(isinstance(subfolder, str), "Unsafe output subfolder.")
    raw_parts = re.split(r"[/\\]", subfolder)
    require(":" not in subfolder and all(part not in ("", ".", "..") for part in raw_parts),
            "Rooted, drive, UNC, empty or traversal output components are forbidden.")
    subfolder = subfolder.replace("\\", "/")
    require(".." not in PurePosixPath(subfolder).parts, "Unsafe output subfolder.")
    require(subfolder == prefix.rsplit("/", 1)[0] and filename.startswith(prefix.rsplit("/", 1)[1] + "_"),
            "Output is not from this run and candidate.")
    require(image.get("type") == "output", "Refuse input/temp/unrelated output.")
    return {"filename": filename, "subfolder": subfolder, "type": "output"}


def collect(api, directory, spec, model, slot, wait_seconds):
    ledger = read_ledger(directory, spec, model)
    rows = [row for row in ledger["requests"] if row["slot"] == slot]
    require(len(rows) == 1, "Candidate was not reserved.")
    row = rows[0]
    prompt_id = row.get("prompt_id")
    require(isinstance(prompt_id, str) and re.fullmatch(r"[0-9a-fA-F-]{36}", prompt_id),
            "No known prompt id. Inspect only this owned server's evidence; do not submit again.")
    require(row["state"] != "collected", "Source already collected; process its retained bytes.")
    graph = reserved_workflow(directory, ledger, row, spec, model, slot)
    deadline = time.monotonic() + wait_seconds
    while True:
        history = api.request("/history/" + quote(prompt_id, safe=""))
        own = history.get(prompt_id)
        if own:
            historical_prompt = own.get("prompt")
            require(isinstance(historical_prompt, list) and len(historical_prompt) >= 3
                    and historical_prompt[1] == prompt_id and isinstance(historical_prompt[2], dict)
                    and sha256(json_bytes(historical_prompt[2])) == row["workflow_sha256"],
                    "Owned history graph/id differs from the reserved approved graph.")
            status = own.get("status", {})
            if status.get("status_str") == "error":
                update_request(directory, slot, state="execution_failed")
                raise ContractError("ComfyUI execution failed; retain its own history and do not resubmit this slot.")
            if status.get("completed") is True:
                require(status.get("status_str") == "success", "History is complete but not successful.")
                break
        if time.monotonic() >= deadline:
            update_request(directory, slot, state="wait_timeout")
            raise ContractError("Inference wait expired; collect this prompt later. No cancellation or resubmission.")
        time.sleep(min(1, max(0, deadline - time.monotonic())))
    reference = output_reference(own.get("outputs", {}).get("7", {}).get("images"),
                                 canonical_prefix(directory, spec, slot))
    data = api.request("/view?" + urlencode(reference), binary=True)
    require(data.startswith(b"\x89PNG\r\n\x1a\n"), "ComfyUI did not return a PNG.")
    png_proof = png_graph_proof(data, graph)
    source_hash = sha256(data)
    duplicate = next((previous for previous in ledger["requests"]
                      if previous.get("source_sha256") == source_hash), None)
    if duplicate is not None:
        update_request(directory, slot, state="duplicate_source", duplicate_of_slot=duplicate["slot"])
        raise ContractError("Duplicate source bytes: not another independent inference proof.")
    destination = directory / f"source-{slot}.png"
    with destination.open("xb") as stream:
        stream.write(data)
    history_proof = {
        "prompt_id": prompt_id, "workflow_sha256": row["workflow_sha256"], "graph": historical_prompt[2],
        "evidence_source": "actual owned history GET during collection", **png_proof,
    }
    with (directory / f"history-proof-{slot}.json").open("xb") as stream:
        stream.write(json_bytes(history_proof))
    update_request(directory, slot, state="collected", source_sha256=source_hash,
                   source_bytes=len(data), output_reference=reference,
                   completed_at=datetime.now(timezone.utc).isoformat())
    return destination


def image_module():
    try:
        from PIL import Image
    except ImportError as error:
        raise ContractError("Pillow is required for deterministic processing; use the inspected existing runtime.") from error
    return Image


def guide_image(spec=None):
    if spec and spec.get("schema") == "grindless.visual-asset.v2":
        from visual_presets import guide_image as preset_guide
        return preset_guide(spec)
    Image = image_module()
    from PIL import ImageDraw
    image = Image.new("RGB", (32, 32), "white")
    draw = ImageDraw.Draw(image)
    outline = [(7, 3), (11, 2), (11, 7), (14, 9), (17, 7), (17, 2), (21, 3),
               (24, 8), (22, 12), (18, 15), (18, 27), (16, 29), (13, 29), (11, 27),
               (11, 15), (7, 12), (5, 8)]
    draw.polygon(outline, fill="#322A24")
    body = [(8, 4), (10, 3), (10, 8), (14, 11), (18, 8), (18, 3), (20, 4),
            (22, 8), (21, 11), (16, 14), (16, 26), (15, 27), (13, 26),
            (13, 14), (8, 11), (7, 8)]
    draw.polygon(body, fill="#C9A227")
    draw.line([(8, 5), (7, 8), (9, 11), (14, 13), (14, 25)], fill="#DBC278", width=1)
    draw.line([(20, 5), (21, 8), (20, 10), (17, 12)], fill="#82691A", width=1)
    draw.rectangle((13, 19, 16, 24), fill="#59432A")
    draw.line([(13, 19), (13, 24)], fill="#96724C", width=1)
    output = io.BytesIO()
    image.resize((512, 512), Image.Resampling.NEAREST).save(output, format="PNG", compress_level=9)
    return output.getvalue()


def components(mask, width, height):
    remaining = set(mask)
    result = []
    while remaining:
        first = min(remaining)
        remaining.remove(first)
        group, queue = {first}, deque([first])
        while queue:
            current = queue.popleft()
            x, y = current % width, current // width
            for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                neighbor = ny * width + nx
                if 0 <= nx < width and 0 <= ny < height and neighbor in remaining:
                    remaining.remove(neighbor)
                    group.add(neighbor)
                    queue.append(neighbor)
        result.append(group)
    return sorted(result, key=lambda group: (-len(group), min(group)))


def process_image(source, spec):
    if spec.get("schema") == "grindless.visual-asset.v2":
        from visual_presets import process_image as preset_process
        return preset_process(source, spec)
    Image = image_module()
    with Image.open(io.BytesIO(source)) as opened:
        require(opened.format == "PNG" and not getattr(opened, "is_animated", False), "Use a single-frame source PNG.")
        require(opened.size == (512, 512), "Expected the workflow's exact 512px source.")
        image = opened.convert("RGBA")
    width, height = image.size
    pixels = list(image.get_flattened_data())
    transform = spec["transform"]
    background = rgb(transform["background"])
    tolerance = transform["key_tolerance"]

    def removable(index):
        value = pixels[index]
        return value[3] < 128 or max(abs(value[channel] - background[channel]) for channel in range(3)) <= tolerance

    edge = set(range(width)) | set(range(width * (height - 1), width * height))
    edge |= {y * width for y in range(height)} | {y * width + width - 1 for y in range(height)}
    require(all(removable(index) for index in edge), "Foreground/background touches the source edge; regenerate only within the allowance.")
    removed, queue = set(edge), deque(sorted(edge))
    while queue:
        current = queue.popleft()
        x, y = current % width, current // width
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            neighbor = ny * width + nx
            if 0 <= nx < width and 0 <= ny < height and neighbor not in removed and removable(neighbor):
                removed.add(neighbor)
                queue.append(neighbor)
    opaque = {index for index, pixel in enumerate(pixels) if index not in removed and pixel[3] >= 128}
    fraction = len(opaque) / (width * height)
    require(transform["minimum_foreground_fraction"] <= fraction <= transform["maximum_foreground_fraction"],
            "Source foreground coverage is outside its declared bounds.")
    groups = components(opaque, width, height)
    require(groups, "No object survives background cleanup.")
    dust = sum(len(group) for group in groups[1:])
    require(dust <= len(groups[0]) * transform["maximum_dust_fraction"],
            "Multiple substantial objects or detached fragments; do not silently erase them.")
    opaque = groups[0]
    for index, pixel in enumerate(pixels):
        pixels[index] = pixel[:3] + (255,) if index in opaque else (0, 0, 0, 0)
    image.putdata(pixels)
    box = image.getbbox()
    crop = image.crop(box)
    available = transform["size"] - 2 * transform["margin"]
    scale = available / max(crop.size)
    size = tuple(max(1, math.floor(dimension * scale + 0.5)) for dimension in crop.size)
    sprite = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    sprite.paste(crop.resize(size, Image.Resampling.NEAREST), ((16 - size[0]) // 2, (16 - size[1]) // 2))
    palette = [rgb(color) for color in spec["visual"]["palette"]]
    mapped = []
    for pixel in sprite.get_flattened_data():
        if pixel[3] == 0:
            mapped.append((0, 0, 0, 0))
        else:
            nearest = min(palette, key=lambda color: sum((color[channel] - pixel[channel])**2 for channel in range(3)))
            mapped.append(nearest + (255,))
    sprite.putdata(mapped)
    verify_sprite(sprite, spec)
    output = io.BytesIO()
    sprite.save(output, format="PNG", optimize=False, compress_level=9)
    import PIL
    report = {
        "transform": TRANSFORM, "source_sha256": sha256(source), "spec_sha256": sha256(json_bytes(spec)),
        "template_sha256": file_sha256(template_path(spec)),
        "source_crop_xyxy": box, "removed_dust_pixels": dust, "source_foreground_fraction": fraction,
        "runtime_rgba_sha256": sha256(sprite.tobytes()), "runtime_png_sha256": sha256(output.getvalue()),
        "pillow_version": PIL.__version__, "python_version": sys.version.split()[0],
        "rotation": 0, "mirror": False, "human_visual_approval": False,
        "orientation_claim": "Preserved without transforms; semantics require source-pixel/human review.",
    }
    return output.getvalue(), report


def verify_sprite(image, spec):
    if spec.get("schema") == "grindless.visual-asset.v2":
        from visual_presets import verify_sprite as preset_verify
        return preset_verify(image, spec)
    require(image.size == (16, 16) and image.mode == "RGBA", "Runtime image must be 16x16 RGBA.")
    pixels = list(image.get_flattened_data())
    palette = {rgb(value) for value in spec["visual"]["palette"]}
    opaque = []
    for index, pixel in enumerate(pixels):
        require(pixel[3] in (0, 255), "Runtime alpha must be binary.")
        if pixel[3]:
            require(pixel[:3] in palette, "Runtime pixel is outside its declared palette.")
            x, y = index % 16, index // 16
            require(1 <= x <= 14 and 1 <= y <= 14, "Runtime image needs a transparent 1px margin.")
            opaque.append(index)
        else:
            require(pixel[:3] == (0, 0, 0), "Transparent RGB must be canonical zero.")
    require(12 <= len(opaque) <= 196, "Runtime silhouette coverage is invalid.")
    require(len({pixels[index][:3] for index in opaque}) >= 3, "Runtime object lacks material/shading separation.")
    require(len(components(set(opaque), 16, 16)) == 1, "Runtime silhouette must remain connected.")


def comparison_image(source, baseline, candidate, spec):
    Image = image_module()
    from PIL import ImageDraw
    images = []
    for data, size in ((source, (512, 512)), (baseline, (16, 16)), (candidate, (16, 16))):
        with Image.open(io.BytesIO(data)) as image:
            require(image.format == "PNG" and image.size == size, "Comparison needs the actual source and native PNGs.")
            images.append(image.convert("RGBA"))
    verify_sprite(images[2], spec)
    sheet = Image.new("RGBA", (1024, 600), "white")
    draw = ImageDraw.Draw(sheet)
    draw.text((16, 12), "Multitool: actual source pixels, not an in-game screenshot", fill="black")
    for column, image, title in ((16, images[1], "Predecessor"), (240, images[2], "Candidate 2")):
        draw.text((column, 52), title + " / native 16px + nearest 8x", fill="black")
        for top, background, foreground in ((78, "#E0E8F0", "black"), (296, "#20252B", "white")):
            draw.rectangle((column, top, column + 200, top + 190), fill=background)
            sheet.paste(image, (column + 12, top + 20), image)
            draw.text((column + 40, top + 20), "1x", fill=foreground)
            enlarged = image.resize((128, 128), Image.Resampling.NEAREST)
            sheet.paste(enlarged, (column + 36, top + 48), enlarged)
    draw.text((496, 32), "Retained generated source, actual 512px", fill="black")
    sheet.paste(images[0], (496, 52), images[0])
    draw.text((16, 530), "Light/dark backgrounds are review aids; no resource pixels are edited.", fill="black")
    draw.text((496, 578), "Source " + sha256(source)[:16] + " / native " + sha256(candidate)[:16], fill="black")
    output = io.BytesIO()
    sheet.save(output, format="PNG", compress_level=9)
    return output.getvalue()


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("plan", "generate", "collect", "process", "verify", "guide", "revise", "compare", "audit"))
    parser.add_argument("--spec", type=Path, required=True)
    parser.add_argument("--model-lock", type=Path, default=HERE / "sd15-model.json")
    parser.add_argument("--endpoint", default="http://127.0.0.1:8189")
    parser.add_argument("--run-dir", type=Path)
    parser.add_argument("--checkpoint", type=Path)
    parser.add_argument("--slot", type=int, default=1)
    parser.add_argument("--wait-seconds", type=int, default=600)
    parser.add_argument("--source", type=Path)
    parser.add_argument("--source-sha256")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--allow-generation", action="store_true")
    parser.add_argument("--input-directory", type=Path)
    parser.add_argument("--expected-ledger-sha256")
    parser.add_argument("--revision-reason")
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--candidate", type=Path)
    args = parser.parse_args(argv)
    try:
        spec = load_spec(args.spec)
        if args.action == "compare":
            require(args.source and args.baseline and args.candidate and args.output, "Comparison needs actual source, predecessor, native candidate and a new output PNG.")
            data = comparison_image(args.source.read_bytes(), args.baseline.read_bytes(), args.candidate.read_bytes(), spec)
            with args.output.open("xb") as stream:
                stream.write(data)
            print(json_bytes({"comparison_sha256": sha256(data), "bytes": len(data)}).decode("ascii"), end="")
        elif args.action == "guide":
            require(args.output and (spec["asset_id"] == "multitool" or spec.get("schema") == "grindless.visual-asset.v2"),
                    "An original source-bound guide and a new output path are required.")
            data = guide_image(spec)
            with args.output.open("xb") as stream:
                stream.write(data)
            print(json_bytes({"guide_sha256": sha256(data), "bytes": len(data)}).decode("ascii"), end="")
        elif args.action in ("plan", "generate", "collect", "revise", "audit"):
            model = load_model(args.model_lock)
            require(1 <= args.slot <= len(spec["seeds"]), "Candidate slot is outside its allowance.")
            require(0 <= args.wait_seconds <= 1800, "Inference wait is bounded to 30 minutes.")
            if args.action == "plan":
                graph = workflow(spec, model, args.slot, f"grindless/{spec['asset_id']}/RUN/candidate-{args.slot}")
                print(json_bytes(graph).decode("ascii"), end="")
                return 0
            require(args.run_dir is not None, "--run-dir is required.")
            if args.action == "audit":
                require(args.output is not None, "Retrospective integrity needs a new addendum output, never an overwritten receipt.")
                report = audit_retained(args.run_dir, spec, model, args.slot)
                with args.output.open("xb") as stream:
                    stream.write(json_bytes(report))
                print(json_bytes(report).decode("ascii"), end="")
                return 0
            if args.action == "revise":
                revise_run(args.run_dir, spec, model, args.expected_ledger_sha256, args.revision_reason)
                print("Revised the remaining candidate; one prior source and the total allowance remain unchanged.")
                return 0
            api = ComfyApi(args.endpoint)
            if args.action == "generate":
                require(args.allow_generation and args.checkpoint, "Generation needs --allow-generation, a pinned checkpoint and an external resource lease.")
                result = generate(api, args.run_dir, spec, model, args.checkpoint, args.slot,
                                  args.wait_seconds, args.input_directory)
            else:
                result = collect(api, args.run_dir, spec, model, args.slot, args.wait_seconds)
            print(json_bytes({"source": result.name, "sha256": file_sha256(result)}).decode("ascii"), end="")
        elif args.action == "process":
            require(args.source and args.output and args.source_sha256, "Processing needs source, expected source hash and a new output directory.")
            source = args.source.read_bytes()
            require(sha256(source) == args.source_sha256, "Retained source hash mismatch.")
            output, report = process_image(source, spec)
            args.output.mkdir(parents=True, exist_ok=False)
            with (args.output / "sprite.png").open("xb") as stream:
                stream.write(output)
            atomic_json(args.output / "processing.json", report)
            print(json_bytes(report).decode("ascii"), end="")
        else:
            require(args.source is not None, "--source is required.")
            Image = image_module()
            with Image.open(args.source) as image:
                require(image.format == "PNG" and not getattr(image, "is_animated", False), "Expected one PNG frame.")
                verify_sprite(image, spec)
            print("PASS: native dimensions, hard alpha, margins, palette, silhouette; not artistic acceptance.")
        return 0
    except (ContractError, OSError, URLError, KeyError, TypeError, json.JSONDecodeError) as error:
        print(f"{type(error).__name__}: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
