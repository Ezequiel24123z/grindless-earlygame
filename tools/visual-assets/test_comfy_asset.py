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


def source_image():
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
    image.save(data, format="PNG")
    return data.getvalue()


class FakeApi:
    def __init__(self, fail_submission=False, completed=True):
        self.posts = 0
        self.fail_submission = fail_submission
        self.completed = completed
        self.prompt_id = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        self.graph = None
        self.requested_paths = []

    def idle(self):
        pass

    def qualify(self, model):
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
            return {self.prompt_id: {
                "status": {"completed": True, "status_str": "success"},
                "outputs": {"7": {"images": [
                    {"filename": filename + "_00001_.png", "subfolder": subfolder, "type": "output"}
                ]}},
            }}
        if path.startswith("/view?"):
            return source_image()
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
