"""Offline contracts: synthetic images and a loopback fake, never real inference."""

import copy
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import io
import json
from pathlib import Path
import tempfile
import threading
import unittest
from unittest.mock import patch

import comfy_asset as pipeline


SPEC_PATH = pipeline.HERE / "specs" / "multitool.json"


def source_image(prompt_graph=None):
    Image = pipeline.image_module()
    from PIL import ImageDraw
    image = Image.new("RGBA", (512, 512), "white")
    draw = ImageDraw.Draw(image)
    draw.rectangle((220, 200, 292, 430), fill="#82691A")
    draw.rectangle((234, 200, 269, 416), fill="#C9A227")
    draw.rectangle((246, 200, 256, 416), fill="#DBC278")
    draw.rectangle((150, 80, 362, 210), fill="#C9A227")
    draw.rectangle((150, 80, 362, 110), fill="#DBC278")
    draw.rectangle((220, 80, 292, 145), fill="white")
    # Enclosed highlights must not be erased by a global color-key replacement.
    draw.rectangle((175, 145, 190, 160), fill="white")
    data = io.BytesIO()
    from PIL.PngImagePlugin import PngInfo
    metadata = PngInfo()
    if prompt_graph is not None:
        metadata.add_text("prompt", json.dumps(prompt_graph))
    image.save(data, format="PNG", pnginfo=metadata)
    return data.getvalue()


class FakeApi:
    def __init__(self, fail_submission=False, completed=True):
        self.posts = 0
        self.fail_submission = fail_submission
        self.completed = completed
        self.prompt_id = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        self.graph = None
        self.requested_paths = []
        self.history_graph = None
        self.png_graph = None
        self.windows_subfolder = False

    def idle(self):
        pass

    def qualify(self, model, expected_nodes=pipeline.NODES):
        return {"comfyui_version": "synthetic", "devices": []}

    def request(self, path, value=None, binary=False):
        self.requested_paths.append(path)
        if path == "/prompt":
            self.posts += 1
            self.graph = value["prompt"]
            if self.fail_submission:
                raise TimeoutError("synthetic delivery ambiguity")
            return {"prompt_id": self.prompt_id}
        if path.startswith("/history/"):
            if not self.completed:
                return {}
            prefix = self.graph["7"]["inputs"]["filename_prefix"]
            subfolder, filename = prefix.rsplit("/", 1)
            if self.windows_subfolder:
                subfolder = subfolder.replace("/", "\\")
            return {self.prompt_id: {
                "prompt": [0, self.prompt_id, self.graph if self.history_graph is None else self.history_graph, {}, ["7"]],
                "status": {"completed": True, "status_str": "success"},
                "outputs": {"7": {"images": [
                    {"filename": filename + "_00001_.png", "subfolder": subfolder, "type": "output"}
                ]}},
            }}
        if path.startswith("/view?"):
            return source_image(self.png_graph)
        raise AssertionError("Unexpected fake API access: " + path)


class Contracts(unittest.TestCase):
    def setUp(self):
        self.spec = pipeline.load_spec(SPEC_PATH)
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.checkpoint = self.root / "synthetic.safetensors"
        self.checkpoint.write_bytes(b"synthetic weights, not a model")
        self.model = {
            "schema": "grindless.comfy-model.v1",
            "filename": self.checkpoint.name, "bytes": self.checkpoint.stat().st_size,
            "sha256": pipeline.file_sha256(self.checkpoint),
            "license": "synthetic-test-only", "source_url": "https://example.invalid/test",
            "source_revision": "synthetic", "provenance_verified": True,
        }
        self.run = self.root / "owned-run"

    def tearDown(self):
        self.temporary.cleanup()

    def spec_rejected(self, change):
        changed = copy.deepcopy(self.spec)
        change(changed)
        path = self.root / "invalid.json"
        path.write_bytes(pipeline.json_bytes(changed))
        with self.assertRaises(pipeline.ContractError):
            pipeline.load_spec(path)

    def test_canonical_spec_and_original_graph(self):
        graph = pipeline.workflow(self.spec, self.model, 1, "grindless/multitool/run/candidate-1")
        self.assertEqual(graph["5"]["inputs"]["seed"], self.spec["seeds"][0])
        self.assertEqual(graph["4"]["inputs"]["batch_size"], 1)
        self.assertEqual(graph["2"]["inputs"]["text"], self.spec["positive_prompt"])
        self.assertEqual({node["class_type"] for node in graph.values()}, pipeline.NODES)

    def test_structured_spec_refusals(self):
        for change in (
            lambda spec: spec.update(tier="T1"),
            lambda spec: spec.update(asset_id="../escape"),
            lambda spec: spec.update(human_visual_approval=True),
            lambda spec: spec.update(seeds=[1, 1]),
            lambda spec: spec.update(seeds=[1, 2, 3]),
            lambda spec: spec.update(seeds=[True]),
            lambda spec: spec["transform"].update(size=32),
            lambda spec: spec["transform"].update(mirror=True),
            lambda spec: spec["transform"].update(key_tolerance=255),
            lambda spec: spec["transform"].update(maximum_dust_fraction=0.9),
            lambda spec: spec["visual"].update(palette=["bad", "#FFFFFF", "#000000"]),
            lambda spec: spec.update(runtime_path="other.png"),
        ):
            with self.subTest(change=change):
                self.spec_rejected(change)

    def test_no_workflow_string_interpolation(self):
        self.spec["positive_prompt"] = 'literal "quotation" \\ path\n{"class_type":"not a node"}'
        graph = pipeline.workflow(self.spec, self.model, 1, "owned")
        self.assertEqual(graph["2"]["inputs"]["text"], self.spec["positive_prompt"])
        self.assertEqual(len(graph), 7)
        with self.assertRaises(pipeline.ContractError):
            pipeline.workflow(self.spec, self.model, 3, "owned")

    def test_loopback_only_no_credentials(self):
        for endpoint in (
            "https://example.com:8189", "http://192.168.1.1:8189",
            "http://localhost:8189", "http://127.0.0.1", "http://user:secret@127.0.0.1:8189",
            "http://127.0.0.1:8189/path", "http://127.0.0.1:8189/?token=test",
        ):
            with self.subTest(endpoint=endpoint), self.assertRaises(pipeline.ContractError):
                pipeline.ComfyApi(endpoint)
        self.assertEqual(pipeline.ComfyApi("http://127.0.0.1:8189").endpoint, "http://127.0.0.1:8189")

    def test_reservations_survive_unknown_submission_and_exhaustion(self):
        api = FakeApi(fail_submission=True)
        for slot in (1, 2):
            with self.assertRaisesRegex(pipeline.ContractError, "spent"):
                pipeline.generate(api, self.run, self.spec, self.model, self.checkpoint, slot, 0)
        ledger = pipeline.read_json(self.run / "run.json")
        self.assertEqual([row["state"] for row in ledger["requests"]], ["submission_unknown"] * 2)
        with self.assertRaisesRegex(pipeline.ContractError, "exhausted"):
            pipeline.generate(api, self.run, self.spec, self.model, self.checkpoint, 1, 0)
        self.assertEqual(api.posts, 2)

    def test_no_duplicate_slot_or_changed_identity(self):
        graph = pipeline.workflow(self.spec, self.model, 1, "owned")
        pipeline.reserve(self.run, self.spec, self.model, 1, graph)
        with self.assertRaisesRegex(pipeline.ContractError, "spent"):
            pipeline.reserve(self.run, self.spec, self.model, 1, graph)
        changed = copy.deepcopy(self.spec)
        changed["positive_prompt"] += " modified"
        with self.assertRaisesRegex(pipeline.ContractError, "identity"):
            pipeline.reserve(self.run, changed, self.model, 2, graph)

    def test_lock_is_not_silently_removed(self):
        self.run.mkdir()
        lock = self.run / ".request.lock"
        lock.write_text("unrelated owner", encoding="ascii")
        with self.assertRaisesRegex(pipeline.ContractError, "locked"):
            with pipeline.run_lock(self.run):
                self.fail("Must not enter a competing lock")
        self.assertEqual(lock.read_text(encoding="ascii"), "unrelated owner")

    def test_checkpoint_tamper_fails_before_api(self):
        self.checkpoint.write_bytes(b"tampered")
        api = FakeApi()
        with self.assertRaises(pipeline.ContractError):
            pipeline.generate(api, self.run, self.spec, self.model, self.checkpoint, 1, 0)
        self.assertEqual(api.posts, 0)
        self.assertFalse(self.run.exists())

    def conditioned_spec(self):
        spec = copy.deepcopy(self.spec)
        spec["conditioning"] = {
            "kind": "original-multitool-guide-v1", "filename": "grindless-multitool-guide-v1.png",
            "sha256": pipeline.sha256(pipeline.guide_image()), "denoise": 0.35,
        }
        return spec

    def test_conditioned_guide_and_graph_are_deterministic_and_pinned(self):
        Image = pipeline.image_module()
        guide = pipeline.guide_image()
        self.assertEqual(guide, pipeline.guide_image())
        with Image.open(io.BytesIO(guide)) as image:
            self.assertEqual(image.size, (512, 512))
        spec = self.conditioned_spec()
        committed = pipeline.load_spec(pipeline.HERE / "specs" / "multitool-conditioned.json")
        self.assertEqual(committed, spec)
        graph = pipeline.workflow(spec, self.model, 2, "owned")
        self.assertEqual(graph["5"]["inputs"]["denoise"], 0.35)
        self.assertEqual(graph["5"]["inputs"]["seed"], self.spec["seeds"][1])
        self.assertEqual(graph["4"]["class_type"], "LoadImage")
        self.assertEqual(graph["8"]["class_type"], "VAEEncode")
        self.assertEqual(graph["5"]["inputs"]["latent_image"], ["8", 0])
        with self.assertRaisesRegex(pipeline.ContractError, "input directory"):
            pipeline.generate(FakeApi(), self.run, spec, self.model, self.checkpoint, 2, 0)
        self.assertFalse(self.run.exists())

    def test_explicit_revision_preserves_the_spent_source_and_budget(self):
        api = FakeApi()
        pipeline.generate(api, self.run, self.spec, self.model, self.checkpoint, 1, 0)
        old_ledger = pipeline.read_json(self.run / "run.json")
        source = (self.run / "source-1.png").read_bytes()
        spec = self.conditioned_spec()
        expected = pipeline.file_sha256(self.run / "run.json")
        with self.assertRaisesRegex(pipeline.ContractError, "Ledger changed"):
            pipeline.revise_run(self.run, spec, self.model, "0" * 64, "test exact revision")
        pipeline.revise_run(self.run, spec, self.model, expected, "test purpose-preserving conditioning")
        revised = pipeline.read_json(self.run / "run.json")
        self.assertEqual(pipeline.file_sha256(self.run / "run.json"), expected)
        self.assertEqual(revised["requests"][0], old_ledger["requests"][0])
        self.assertEqual(revised["maximum_candidates"], 2)
        authorization = pipeline.read_json(self.run / "authorization-2.json")
        self.assertEqual(authorization["remaining_after"], 1)
        self.assertEqual(authorization["maximum_total"], 2)
        self.assertEqual((self.run / "source-1.png").read_bytes(), source)
        self.assertEqual(pipeline.read_json(self.run / "spec.json"), self.spec)
        input_dir = self.root / "input"
        input_dir.mkdir()
        (input_dir / spec["conditioning"]["filename"]).write_bytes(pipeline.guide_image())
        with self.assertRaisesRegex(pipeline.ContractError, "Duplicate source"):
            pipeline.generate(api, self.run, spec, self.model, self.checkpoint, 2, 0, input_dir)
        self.assertEqual(api.posts, 2)
        with self.assertRaisesRegex(pipeline.ContractError, "exhausted"):
            pipeline.generate(api, self.run, spec, self.model, self.checkpoint, 2, 0, input_dir)
        self.assertEqual(api.posts, 2)

    def test_revision_refuses_unresolved_sources_or_changed_purpose(self):
        graph = pipeline.workflow(self.spec, self.model, 1, "owned")
        pipeline.reserve(self.run, self.spec, self.model, 1, graph)
        with self.assertRaisesRegex(pipeline.ContractError, "resolved"):
            pipeline.revise_run(self.run, self.conditioned_spec(), self.model,
                                pipeline.file_sha256(self.run / "run.json"), "test")
        pipeline.update_request(self.run, 1, state="collected")
        changed = self.conditioned_spec()
        changed["role"] = "different gameplay purpose"
        with self.assertRaisesRegex(pipeline.ContractError, "purpose"):
            pipeline.revise_run(self.run, changed, self.model,
                                pipeline.file_sha256(self.run / "run.json"), "test")
        self.assertFalse((self.run / "authorization-2.json").exists())

    def test_collect_never_submits_and_timeout_can_resume(self):
        api = FakeApi(completed=False)
        with self.assertRaisesRegex(pipeline.ContractError, "expired"):
            pipeline.generate(api, self.run, self.spec, self.model, self.checkpoint, 1, 0)
        self.assertEqual(api.posts, 1)
        api.completed = True
        source = pipeline.collect(api, self.run, self.spec, self.model, 1, 0)
        self.assertEqual(source.read_bytes(), source_image())
        self.assertEqual(api.posts, 1)
        self.assertTrue(all(path.startswith(("/prompt", "/history/", "/view?")) for path in api.requested_paths))
        with self.assertRaisesRegex(pipeline.ContractError, "already collected"):
            pipeline.collect(api, self.run, self.spec, self.model, 1, 0)

    def pending_source(self):
        api = FakeApi(completed=False)
        with self.assertRaisesRegex(pipeline.ContractError, "expired"):
            pipeline.generate(api, self.run, self.spec, self.model, self.checkpoint, 1, 0)
        api.completed = True
        api.requested_paths.clear()
        return api

    def test_workflow_seed_prompt_prefix_model_and_digest_tamper_have_zero_http(self):
        api = self.pending_source()
        graph_path = self.run / "workflow-1.json"
        ledger_path = self.run / "run.json"
        original_graph, original_ledger = graph_path.read_bytes(), ledger_path.read_bytes()
        mutations = (
            lambda graph: graph["5"]["inputs"].update(seed=123),
            lambda graph: graph["2"]["inputs"].update(text="changed prompt"),
            lambda graph: graph["7"]["inputs"].update(filename_prefix="grindless/foreign/run/candidate-1"),
            lambda graph: graph["1"]["inputs"].update(ckpt_name="other.safetensors"),
        )
        for mutation in mutations:
            for rewrite_digest in (False, True):
                with self.subTest(mutation=mutation, rewrite_digest=rewrite_digest):
                    graph = json.loads(original_graph)
                    mutation(graph)
                    changed = pipeline.json_bytes(graph)
                    graph_path.write_bytes(changed)
                    ledger = json.loads(original_ledger)
                    if rewrite_digest:
                        ledger["requests"][0]["workflow_sha256"] = pipeline.sha256(changed)
                    ledger_path.write_bytes(pipeline.json_bytes(ledger))
                    with self.assertRaises(pipeline.ContractError):
                        pipeline.collect(api, self.run, self.spec, self.model, 1, 0)
                    self.assertEqual(api.requested_paths, [])
        graph_path.write_bytes(original_graph)
        ledger = json.loads(original_ledger)
        ledger["requests"][0]["workflow_sha256"] = "0" * 64
        ledger_path.write_bytes(pipeline.json_bytes(ledger))
        with self.assertRaises(pipeline.ContractError):
            pipeline.collect(api, self.run, self.spec, self.model, 1, 0)
        self.assertEqual(api.requested_paths, [])
        self.assertEqual(api.posts, 1)

    def test_history_graph_tamper_is_refused_before_view_or_collection(self):
        api = self.pending_source()
        for node, field, value in (
            ("5", "seed", 123), ("2", "text", "changed historical prompt"),
            ("7", "filename_prefix", "foreign"), ("1", "ckpt_name", "other.safetensors"),
        ):
            with self.subTest(node=node, field=field):
                api.history_graph = copy.deepcopy(api.graph)
                api.history_graph[node]["inputs"][field] = value
                api.requested_paths.clear()
                with self.assertRaisesRegex(pipeline.ContractError, "history graph"):
                    pipeline.collect(api, self.run, self.spec, self.model, 1, 0)
                self.assertTrue(all(path.startswith("/history/") for path in api.requested_paths))
                self.assertFalse((self.run / "source-1.png").exists())
        self.assertEqual(api.posts, 1)

    def test_valid_windows_collection_is_get_only_and_png_metadata_bound(self):
        api = self.pending_source()
        api.windows_subfolder = True
        api.png_graph = copy.deepcopy(api.graph)
        source = pipeline.collect(api, self.run, self.spec, self.model, 1, 0)
        self.assertEqual(api.posts, 1)
        self.assertEqual(source.read_bytes(), source_image(api.graph))
        self.assertTrue(all(path.startswith(("/history/", "/view?")) for path in api.requested_paths))
        self.assertTrue(any("subfolder=grindless%2Fmultitool%2Fowned-run" in path for path in api.requested_paths))
        proof = pipeline.read_json(self.run / "history-proof-1.json")
        self.assertEqual(proof["png_prompt_metadata"], "verified")
        self.assertEqual(proof["workflow_sha256"], pipeline.sha256(pipeline.json_bytes(api.graph)))

    def test_conflicting_png_prompt_metadata_cannot_be_collected(self):
        api = self.pending_source()
        api.png_graph = copy.deepcopy(api.graph)
        api.png_graph["5"]["inputs"]["seed"] = 123
        with self.assertRaisesRegex(pipeline.ContractError, "PNG prompt graph"):
            pipeline.collect(api, self.run, self.spec, self.model, 1, 0)
        self.assertFalse((self.run / "source-1.png").exists())
        self.assertNotEqual(pipeline.read_json(self.run / "run.json")["requests"][0]["state"], "collected")
        self.assertEqual(api.posts, 1)

    def test_retained_png_audit_is_offline_append_only_evidence(self):
        api = self.pending_source()
        api.png_graph = copy.deepcopy(api.graph)
        pipeline.collect(api, self.run, self.spec, self.model, 1, 0)
        before = {path.name: path.read_bytes() for path in self.run.iterdir()}
        with patch.object(pipeline.ComfyApi, "request", side_effect=AssertionError("Audit must be offline")):
            proof = pipeline.audit_retained(self.run, self.spec, self.model, 1)
        self.assertEqual(proof["network_requests"], 0)
        self.assertEqual(proof["png_prompt_sha256"], pipeline.sha256(pipeline.json_bytes(api.graph)))
        self.assertFalse(proof["original_receipts_modified"])
        self.assertEqual(before, {path.name: path.read_bytes() for path in self.run.iterdir()})

    def test_duplicate_sources_do_not_count_as_second_inference(self):
        api = FakeApi()
        pipeline.generate(api, self.run, self.spec, self.model, self.checkpoint, 1, 0)
        with self.assertRaisesRegex(pipeline.ContractError, "Duplicate source"):
            pipeline.generate(api, self.run, self.spec, self.model, self.checkpoint, 2, 0)
        self.assertFalse((self.run / "source-2.png").exists())
        second = pipeline.read_json(self.run / "run.json")["requests"][1]
        self.assertEqual(second["state"], "duplicate_source")
        self.assertEqual(second["duplicate_of_slot"], 1)

    def test_foreign_output_and_path_traversal_refused(self):
        prefix = "grindless/multitool/owned-run/candidate-1"
        valid = {"filename": "candidate-1_00001_.png", "subfolder": "grindless/multitool/owned-run", "type": "output"}
        self.assertEqual(pipeline.output_reference([valid], prefix), valid)
        windows = dict(valid, subfolder=valid["subfolder"].replace("/", "\\"))
        self.assertEqual(pipeline.output_reference([windows], prefix), valid)
        for change in (
            {"filename": "../private.png"}, {"filename": "other_00001_.png"},
            {"subfolder": "grindless/multitool/another-run"},
            {"subfolder": "../private"}, {"subfolder": "grindless\\private"}, {"type": "input"},
            {"subfolder": "grindless\\multitool\\..\\private"},
        ):
            with self.subTest(change=change), self.assertRaises(pipeline.ContractError):
                pipeline.output_reference([dict(valid, **change)], prefix)
        with self.assertRaises(pipeline.ContractError):
            pipeline.output_reference([valid, valid], prefix)

    def test_windows_raw_escape_forms_are_rejected_before_normalization(self):
        prefix = "grindless/multitool/owned-run/candidate-1"
        for subfolder in (
            "/grindless/multitool/owned-run", "\\grindless\\multitool\\owned-run",
            "C:\\grindless\\multitool\\owned-run", "C:grindless/multitool/owned-run",
            "\\\\server\\share\\grindless\\multitool\\owned-run",
            "grindless//multitool/owned-run", "grindless/./multitool/owned-run",
            "grindless/multitool/../owned-run", "grindless\\multitool/..\\owned-run",
        ):
            image = {"filename": "candidate-1_00001_.png", "subfolder": subfolder, "type": "output"}
            with self.subTest(subfolder=subfolder), self.assertRaisesRegex(
                    pipeline.ContractError, "Rooted, drive, UNC, empty or traversal"):
                pipeline.output_reference([image], prefix)

    def test_deterministic_transform_and_no_source_mutation(self):
        source = source_image()
        before = pipeline.sha256(source)
        first, report = pipeline.process_image(source, self.spec)
        second, repeat = pipeline.process_image(source, self.spec)
        self.assertEqual(first, second)
        self.assertEqual(report, repeat)
        self.assertEqual(before, pipeline.sha256(source))
        self.assertEqual(report["runtime_png_sha256"], pipeline.sha256(first))
        self.assertFalse(report["human_visual_approval"])
        Image = pipeline.image_module()
        with Image.open(io.BytesIO(first)) as image:
            pipeline.verify_sprite(image, self.spec)
            self.assertEqual(image.mode, "RGBA")

    def test_comparison_uses_actual_native_pixels_and_is_not_a_resource(self):
        source = source_image()
        native, _ = pipeline.process_image(source, self.spec)
        sheet = pipeline.comparison_image(source, native, native, self.spec)
        self.assertEqual(sheet, pipeline.comparison_image(source, native, native, self.spec))
        Image = pipeline.image_module()
        with Image.open(io.BytesIO(sheet)) as image:
            self.assertEqual(image.size, (1024, 600))
        with self.assertRaises(pipeline.ContractError):
            pipeline.process_image(sheet, self.spec)

    def test_committed_selected_source_reprocesses_to_exact_native_and_runtime_bytes(self):
        root = pipeline.HERE.parent.parent
        provenance = pipeline.read_json(pipeline.HERE / "approved" / "multitool.provenance.json")
        selected = pipeline.HERE / "approved" / "multitool.png"
        expected = (pipeline.HERE / "approved" / "multitool.sha256").read_text(encoding="ascii").strip()
        self.assertEqual(pipeline.file_sha256(selected), expected)
        self.assertEqual(provenance["native_png_sha256"], expected)
        source = (root / provenance["retained_source"]).read_bytes()
        self.assertEqual(pipeline.sha256(source), provenance["source_png_sha256"])
        spec = pipeline.load_spec(pipeline.HERE / "specs" / "multitool-conditioned.json")
        native, report = pipeline.process_image(source, spec)
        self.assertEqual(native, selected.read_bytes())
        self.assertEqual((root / provenance["runtime_png"]).read_bytes(), native)
        self.assertEqual(report["runtime_rgba_sha256"], provenance["native_rgba_sha256"])
        graph = pipeline.read_json(pipeline.HERE / "poc" / "multitool-workflow-2.json")
        proof = pipeline.png_graph_proof(source, graph, require_metadata=True)
        self.assertEqual(proof["png_prompt_sha256"], provenance["resolved_workflow_sha256"])
        for path, expected_hash in (
            ("multitool-processing.json", provenance["processing_receipt_sha256"]),
            ("multitool-run.json", provenance["run_ledger_sha256"]),
            ("multitool-integrity-1.json", provenance["integrity_addendum1_sha256"]),
            ("multitool-integrity-2.json", provenance["integrity_addendum2_sha256"]),
        ):
            self.assertEqual(pipeline.file_sha256(pipeline.HERE / "poc" / path), expected_hash)
        ledger = pipeline.read_json(pipeline.HERE / "poc" / "multitool-run.json")
        for row in ledger["requests"]:
            path = pipeline.HERE / "poc" / f"multitool-workflow-{row['slot']}.json"
            self.assertEqual(pipeline.file_sha256(path), row["workflow_sha256"])

    def test_multiple_objects_and_nonwhite_edge_are_refused(self):
        Image = pipeline.image_module()
        from PIL import ImageDraw
        for rectangles in (
            ((30, 80, 130, 430), (350, 80, 450, 430)),
            ((0, 80, 130, 430),),
        ):
            image = Image.new("RGBA", (512, 512), "white")
            draw = ImageDraw.Draw(image)
            for rectangle in rectangles:
                draw.rectangle(rectangle, fill="#C9A227")
            data = io.BytesIO()
            image.save(data, format="PNG")
            with self.assertRaises(pipeline.ContractError):
                pipeline.process_image(data.getvalue(), self.spec)

    def test_empty_wrong_dimensions_and_wrong_format_are_refused(self):
        Image = pipeline.image_module()
        for size, image_format in (((512, 512), "PNG"), ((16, 16), "PNG"), ((512, 512), "BMP")):
            image = Image.new("RGBA", size, "white")
            data = io.BytesIO()
            image.save(data, format=image_format)
            with self.assertRaises(pipeline.ContractError):
                pipeline.process_image(data.getvalue(), self.spec)

    def test_native_mutations_are_refused(self):
        Image = pipeline.image_module()
        good, _ = pipeline.process_image(source_image(), self.spec)
        with Image.open(io.BytesIO(good)) as opened:
            original = opened.copy()
        opaque = next((x, y) for y in range(16) for x in range(16) if original.getpixel((x, y))[3])
        for location, color in (
            (opaque, (201, 162, 39, 127)), (opaque, (255, 0, 255, 255)),
            ((0, 0), (201, 162, 39, 255)), ((0, 0), (1, 1, 1, 0)),
        ):
            changed = original.copy()
            changed.putpixel(location, color)
            with self.subTest(location=location, color=color), self.assertRaises(pipeline.ContractError):
                pipeline.verify_sprite(changed, self.spec)

    def test_cli_wrong_source_hash_does_not_create_output(self):
        source = self.root / "source.png"
        source.write_bytes(source_image())
        output = self.root / "processed"
        result = pipeline.main(["process", "--spec", str(SPEC_PATH), "--source", str(source),
                                "--source-sha256", "0" * 64, "--output", str(output)])
        self.assertEqual(result, 1)
        self.assertFalse(output.exists())

    def test_actual_loopback_transport_and_queue_privacy(self):
        requests = []
        sentinel = "PRIVATE-USER-PROMPT-MUST-NOT-BE-STORED"
        model_name = self.model["filename"]

        class Handler(BaseHTTPRequestHandler):
            busy = False

            def log_message(self, *_):
                pass

            def do_GET(self):
                requests.append(self.path)
                if self.path == "/queue":
                    body = {"queue_running": [[sentinel]] if self.busy else [], "queue_pending": []}
                elif self.path.startswith("/object_info/"):
                    name = self.path.rsplit("/", 1)[1]
                    body = {name: {"input": {"required": {"ckpt_name": [[model_name]]}}}}
                else:
                    body = {"system": {"comfyui_version": "synthetic", "private": sentinel},
                            "devices": [{"name": "synthetic GPU", "type": "test", "vram_total": 16}]}
                data = json.dumps(body).encode()
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)

        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            api = pipeline.ComfyApi(f"http://127.0.0.1:{server.server_port}")
            result = api.qualify(self.model)
            self.assertNotIn(sentinel, json.dumps(result))
            self.assertEqual(len([path for path in requests if path.startswith("/object_info/")]), len(pipeline.NODES))
            Handler.busy = True
            with self.assertRaisesRegex(pipeline.ContractError, "queued work"):
                api.idle()
            self.assertFalse(self.run.exists())
        finally:
            server.shutdown()
            server.server_close()
            thread.join()


if __name__ == "__main__":
    unittest.main(verbosity=2)
