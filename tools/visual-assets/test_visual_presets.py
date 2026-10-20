"""Offline preset/inventory contracts; synthetic processing and fake API, not diffusion."""

import copy
import io
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

import comfy_asset as core
import t0_visuals as audit
import visual_presets as presets
from test_comfy_asset import FakeApi, source_image


class PresetContracts(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.Image = core.image_module()
        self.source = self.root / "source.png"
        self.mask = self.root / "mask.png"
        mask = self.Image.new("RGB", (16, 16), "black")
        for y in range(4, 12):
            for x in range(4, 12):
                mask.putpixel((x, y), (255, 255, 255))
        mask.save(self.mask)

    def tearDown(self):
        self.temporary.cleanup()

    def prepare(self, kind="block-face", suffix="one", masked=False):
        if kind == "item-concept":
            self.source.write_bytes((core.HERE / "approved" / "multitool.png").read_bytes())
        else:
            image = self.Image.new("RGBA", (16, 16), "#795C48")
            for y in range(4, 12):
                for x in range(4, 12):
                    image.putpixel((x, y), (241, 222, 184, 255))
            if kind == "masked-overlay":
                image.putpixel((0, 0), (0, 0, 0, 0))
            image.save(self.source)
        return presets.prepare(kind, "test_" + suffix, "grindless:quern", "b"*40,
                               self.source, self.root / suffix, self.mask if masked else None)

    def png(self, image):
        output = io.BytesIO()
        image.save(output, format="PNG")
        return output.getvalue()

    def test_four_reusable_core_workflows(self):
        model = core.load_model(core.HERE / "sd15-model.json")
        for kind in ("item-concept", "block-face", "machine-family", "masked-overlay"):
            with self.subTest(kind=kind):
                spec = self.prepare(kind, kind.replace("-", "_"), kind == "masked-overlay")
                saved = self.root / kind.replace("-", "_") / "spec.json"
                self.assertEqual(core.load_spec(saved), spec)
                graph = core.workflow(spec, model, 2, "grindless/target/run/candidate-2")
                self.assertEqual(len(graph), 8)
                self.assertEqual(graph["5"]["inputs"]["seed"], spec["seeds"][1])
                self.assertEqual(graph["5"]["inputs"]["denoise"], spec["conditioning"]["denoise"])
                self.assertEqual(graph["5"]["inputs"]["steps"], 20)
                self.assertEqual({node["class_type"] for node in graph.values()},
                                 (core.NODES - {"EmptyLatentImage"}) | {"LoadImage", "VAEEncode"})

    def test_native_guide_retained_and_hashed_before_generation(self):
        spec = self.prepare()
        directory = self.root / "one"
        self.assertEqual((directory / "guide-source.png").read_bytes(), self.source.read_bytes())
        guide = (directory / spec["conditioning"]["filename"]).read_bytes()
        self.assertEqual(core.sha256(guide), spec["conditioning"]["sha256"])
        with self.Image.open(io.BytesIO(guide)) as image:
            self.assertEqual(image.size, (512, 512))
            self.assertEqual(image.getpixel((0, 0)), image.getpixel((31, 31)))

    def test_full_tile_preserves_uv_frame_and_palette(self):
        spec = self.prepare()
        image = self.Image.new("RGBA", (512, 512), "#795C48")
        image.putpixel((16, 16), (241, 222, 184, 255))
        output, report = core.process_image(self.png(image), spec)
        with self.Image.open(io.BytesIO(output)) as tile:
            self.assertEqual(tile.size, (16, 16))
            self.assertEqual(tile.getpixel((0, 0)), (241, 222, 184, 255))
            self.assertEqual(tile.getpixel((15, 15)), (121, 92, 72, 255))
        self.assertFalse(report["diffusion_inpainting"])
        self.assertEqual(report["rotation"], 0)
        self.assertFalse(report["mirror"])

    def test_native_mask_preserves_every_outside_pixel(self):
        spec = self.prepare(masked=True)
        source = self.png(self.Image.new("RGBA", (512, 512), "#F1DEB8"))
        output, report = core.process_image(source, spec)
        with self.Image.open(io.BytesIO(output)) as tile:
            guide = presets.native_guide(spec)
            for y in range(16):
                for x in range(16):
                    if spec["transform"]["mask_rows"][y][x] == "0":
                        self.assertEqual(tile.getpixel((x, y)), guide.getpixel((x, y)))
            changed = tile.copy()
            changed.putpixel((0, 0), (241, 222, 184, 255))
            with self.assertRaises(core.ContractError):
                presets.verify_sprite(changed, spec)
        self.assertTrue(report["mask_preserves_outside_pixels"])

    def test_overlay_preserves_alpha_footprint_and_zero_hidden_rgb(self):
        spec = self.prepare("masked-overlay", masked=True)
        output, _ = core.process_image(self.png(self.Image.new("RGBA", (512, 512), "white")), spec)
        with self.Image.open(io.BytesIO(output)) as image:
            self.assertEqual(image.getpixel((0, 0)), (0, 0, 0, 0))
            self.assertEqual(image.getpixel((8, 8))[3], 255)
            presets.verify_sprite(image, spec)

    def test_item_reuses_qualified_cleanup_and_refuses_two_objects(self):
        spec = self.prepare("item-concept")
        output, report = core.process_image(source_image(), spec)
        with self.Image.open(io.BytesIO(output)) as image:
            core.verify_sprite(image, spec)
        self.assertEqual(report["spec_sha256"], core.sha256(core.json_bytes(spec)))
        image = self.Image.open(io.BytesIO(source_image())).convert("RGBA")
        for y in range(20, 90):
            for x in range(20, 90):
                image.putpixel((x, y), (201, 162, 39, 255))
        with self.assertRaises(core.ContractError):
            core.process_image(self.png(image), spec)

    def test_exact_mask_requires_black_white_not_ambiguous_alpha(self):
        self.Image.new("RGBA", (16, 16), (255, 255, 255, 128)).save(self.mask)
        with self.assertRaises(core.ContractError):
            self.prepare(masked=True)
        self.assertFalse((self.root / "one").exists())

    def test_block_guide_and_source_reject_transparency(self):
        spec = self.prepare()
        changed = copy.deepcopy(spec)
        changed["guide"]["rows"][0] = "." + changed["guide"]["rows"][0][1:]
        changed["conditioning"]["sha256"] = core.sha256(presets.guide_image(changed))
        with self.assertRaises(core.ContractError):
            presets.validate_spec(changed)
        with self.assertRaises(core.ContractError):
            core.process_image(self.png(self.Image.new("RGBA", (512, 512))), spec)

    def test_no_overwrite_of_source_or_previous_preparation(self):
        self.prepare()
        before = self.source.read_bytes()
        with self.assertRaises(FileExistsError):
            presets.prepare("block-face", "test_one", "grindless:quern", "b"*40,
                            self.source, self.root / "one")
        self.assertEqual(self.source.read_bytes(), before)

    def test_spec_mutations_refused(self):
        spec = self.prepare()
        changes = (
            lambda value: value.update(human_visual_approval=True),
            lambda value: value.update(tier="T1"),
            lambda value: value.update(preset_sha256="0"*64),
            lambda value: value.update(model_sha256="0"*64),
            lambda value: value.update(seeds=[1, True]),
            lambda value: value.update(seeds=[1, 1]),
            lambda value: value.update(runtime_path="../escape.png"),
            lambda value: value["transform"].update(rotation=90),
            lambda value: value["transform"].update(crop="auto"),
            lambda value: value["conditioning"].update(denoise=1),
            lambda value: value["conditioning"].update(sha256="0"*64),
            lambda value: value["visual"]["palette"].append("#FFFFFF"),
        )
        for change in changes:
            changed = copy.deepcopy(spec)
            change(changed)
            with self.subTest(change=change), self.assertRaises(core.ContractError):
                presets.validate_spec(changed)

    def test_new_target_ledger_keeps_two_slots_and_uncertain_submit_spent(self):
        spec = self.prepare()
        checkpoint = self.root / "synthetic.safetensors"
        checkpoint.write_bytes(b"synthetic, not a model")
        model = {"filename": checkpoint.name, "bytes": checkpoint.stat().st_size,
                 "sha256": core.file_sha256(checkpoint)}
        spec["model_sha256"] = model["sha256"]
        run = self.root / "new-target-purpose"
        api = FakeApi(fail_submission=True)
        with self.assertRaises(core.ContractError):
            core.generate(api, run, spec, model, checkpoint, 1, 0, self.root / "one")
        self.assertEqual(api.posts, 1)
        ledger = core.read_json(run / "run.json")
        self.assertEqual(ledger["maximum_candidates"], 2)
        self.assertEqual(ledger["requests"][0]["state"], "submission_unknown")
        with self.assertRaises(core.ContractError):
            core.generate(api, run, spec, model, checkpoint, 1, 0, self.root / "one")
        self.assertEqual(api.posts, 1)
        graph = core.read_json(run / "workflow-1.json")
        self.assertEqual(graph["5"]["inputs"]["seed"], spec["seeds"][0])
        self.assertEqual(ledger["requests"][0]["workflow_sha256"], core.sha256(core.json_bytes(graph)))

    def test_changed_model_is_refused_before_reservation_or_http(self):
        spec = self.prepare()
        model = core.load_model(core.HERE / "sd15-model.json")
        model["sha256"] = "0"*64
        with self.assertRaises(core.ContractError):
            core.workflow(spec, model, 1, "grindless/test/run/candidate-1")


class InventoryContracts(unittest.TestCase):
    def test_inventory_and_contact_sheet_are_independent_of_process_hash_seed(self):
        code = (
            "import sys,io;sys.path.insert(0,sys.argv[1]);import t0_visuals as a;import comfy_asset as c;"
            "r=a.inventory(a.ROOT,'b'*40);"
            "rows=[(p['owner'],a.image_bytes((a.ROOT/p['runtime']).read_bytes())) "
            "for p in r['textures'] if '/textures/block/' in p['runtime']];"
            "b=io.BytesIO();a.contact_sheet(rows[:24],'Determinism fixture, not gameplay').save(b,format='PNG');"
            "print(c.sha256(c.json_bytes(r)));print(c.sha256(b.getvalue()))"
        )
        results = []
        for seed in ("1", "2"):
            result = subprocess.run([sys.executable, "-B", "-c", code, str(core.HERE)],
                                    env={**os.environ, "PYTHONHASHSEED": seed}, capture_output=True,
                                    text=True, check=True, timeout=30)
            results.append(result.stdout)
        self.assertEqual(results[0], results[1])

    def test_complete_baseline_scope_and_all_distributed_pngs(self):
        report = audit.inventory(audit.ROOT, "b"*40)
        self.assertEqual(report["counts"]["blocks"], 19)
        self.assertEqual(report["counts"]["state_models"], 65)
        self.assertEqual(report["counts"]["blockstate_variants"], 257)
        self.assertEqual(report["counts"]["standalone_items_including_books"], 14)
        self.assertEqual(report["counts"]["resolved_t0_pngs"], 104 + len(report["port_hooks"]["available_overlays"]))
        paths = {path.relative_to(audit.ROOT).as_posix()
                 for path in (audit.ROOT / audit.ASSETS / "textures").rglob("*.png")}
        self.assertEqual(set(report["all_texture_index"]), paths)
        self.assertFalse(report["human_in_client_review"])
        self.assertFalse(report["human_art_approval"])
        self.assertTrue(all(not block["human_in_client_review"] for block in report["blocks"]))

    def test_protected_sources_and_no_extra_item_replacement(self):
        report = audit.inventory(audit.ROOT, "b"*40)
        protected = {row["owner"] for row in report["textures"] if row.get("protected")}
        self.assertEqual(protected, audit.PROTECTED)
        self.assertEqual(report["files_sha256"][audit.ASSETS + "textures/item/multitool.png"],
                         "127b662a124c30490c3d6737dc29ce8175d37b201e45bdad18bc1ea7360c7d38")

    def test_actual_model_projection_is_deterministic_and_rotates(self):
        model = audit.ASSETS + "models/block/quern_idle.json"
        first = audit.render_model(audit.ROOT, model)
        self.assertEqual(first.size, (16, 16))
        self.assertEqual(first.tobytes(), audit.render_model(audit.ROOT, model).tobytes())
        self.assertNotEqual(first.tobytes(), audit.render_model(audit.ROOT, model, 90).tobytes())

    def test_contact_sheet_contains_unfiltered_native_and_exact_eightfold_pixels(self):
        image = core.image_module().new("RGBA", (16, 16), "#F1DEB8")
        sheet = audit.contact_sheet([("synthetic test", image)], "Synthetic, not gameplay")
        for offset in (0, 166):
            self.assertEqual(sheet.getpixel((offset+6, 54+27)), image.getpixel((0, 0)))
            self.assertEqual(sheet.getpixel((offset+30+7, 54+44+7)), image.getpixel((0, 0)))

    def test_missing_cyclic_and_escaping_references_refused(self):
        with self.assertRaises(core.ContractError):
            audit.texture_reference({"a": "#b", "b": "#a"}, "#a")
        with self.assertRaises(core.ContractError):
            audit.texture_reference({}, "#missing")
        with self.assertRaises(core.ContractError):
            audit.local(audit.ROOT, "../outside.txt")


if __name__ == "__main__":
    unittest.main()
