"""Prepare immutable guided concept specs and deterministic native processing presets."""

from __future__ import annotations

import argparse
import copy
import io
from pathlib import Path
import re

import comfy_asset as core


SCHEMA = "grindless.visual-asset.v2"
PRESETS = core.HERE / "presets.json"
DIGITS = "0123456789ABCDEF"


def palette_map(image, palette):
    colors = [core.rgb(value) for value in palette]
    mapped = image.copy()
    mapped.putdata([
        min(colors, key=lambda color: sum((color[channel] - pixel[channel])**2 for channel in range(3))) + (255,)
        if pixel[3] >= 128 else (0, 0, 0, 0)
        for pixel in image.get_flattened_data()
    ])
    return mapped


def native_guide(spec):
    Image = core.image_module()
    colors = [core.rgb(value) for value in spec["visual"]["palette"]]
    image = Image.new("RGBA", (16, 16))
    image.putdata([(0, 0, 0, 0) if value == "." else colors[DIGITS.index(value)] + (255,)
                   for row in spec["guide"]["rows"] for value in row])
    return image


def guide_image(spec):
    Image = core.image_module()
    guide = native_guide(spec)
    opaque = Image.new("RGBA", (16, 16), "white")
    opaque.alpha_composite(guide)
    output = io.BytesIO()
    opaque.convert("RGB").resize((512, 512), Image.Resampling.NEAREST).save(
        output, format="PNG", compress_level=9)
    return output.getvalue()


def validate_spec(spec):
    core.require(spec.get("schema") == SCHEMA, "Unsupported preset asset schema.")
    core.require(re.fullmatch(r"[a-z][a-z0-9_]{0,63}", spec.get("asset_id", "")), "Invalid preset asset id.")
    core.require(re.fullmatch(r"grindless:[a-z][a-z0-9_]{0,63}", spec.get("registry_id", "")), "Invalid registry reference.")
    core.require(spec.get("tier") == "T0", "Presets do not authorize another tier.")
    core.require(re.fullmatch(r"[0-9a-f]{40}", spec.get("source_revision", "")), "Use an exact source revision.")
    core.require(spec.get("source_refs") and isinstance(spec["source_refs"], list), "Source references are required.")
    core.require(spec.get("human_visual_approval") is False, "Technical specs cannot grant human approval.")
    catalogue = core.read_json(PRESETS)
    generation = catalogue.get("generation", {})
    for key, expected in {
        "model_lock": "sd15-model.json", "size": 512, "steps": 20, "cfg": 7,
        "sampler": "euler", "scheduler": "normal", "batch_size": 1, "candidates_per_target": 2,
    }.items():
        core.require(generation.get(key) == expected, "Preset settings differ from the qualified graph: " + key)
    model = core.load_model(core.HERE / "sd15-model.json")
    core.require(spec.get("model_sha256") == model["sha256"]
                 and spec.get("model_lock_sha256") == core.sha256(core.json_bytes(model)),
                 "Preset specification/model identity differs.")
    preset_id = spec.get("preset_id")
    core.require(preset_id in catalogue["presets"], "Unknown visual preset.")
    preset = catalogue["presets"][preset_id]
    core.require(spec.get("family") == preset["kind"], "Preset family differs.")
    core.require(spec.get("preset_sha256") == core.sha256(core.json_bytes(catalogue)),
                 "Preset revision changed; make a new frozen specification, never revise a spent source.")
    for key in ("role", "positive_prompt", "negative_prompt"):
        core.require(isinstance(spec.get(key), str) and 1 <= len(spec[key]) <= 2000, "Missing bounded " + key)
    core.require(spec["negative_prompt"] == catalogue["negative_prompt"], "Negative prompt differs from its preset.")
    seeds = spec.get("seeds", [])
    core.require(len(seeds) == 2 and all(type(value) is int and 0 <= value < 2**63 for value in seeds)
                 and len(set(seeds)) == 2, "Exactly two reserved distinct seeds per target.")
    visual = spec.get("visual", {})
    core.require(visual.get("palette") == catalogue["palettes"][preset["palette"]], "Preserve the shared palette lock.")
    core.require(visual.get("lighting") == "upper-left", "Preserve shared upper-left lighting.")
    guide = spec.get("guide", {})
    rows = guide.get("rows", [])
    colors = visual["palette"]
    core.require(len(rows) == 16 and all(isinstance(row, str) and len(row) == 16
                 and set(row) <= set(DIGITS[:len(colors)] + ".") for row in rows), "Invalid original native guide.")
    core.require(re.fullmatch(r"[0-9a-f]{64}", guide.get("source_sha256", "")), "Original guide source hash is required.")
    conditioning = spec.get("conditioning", {})
    core.require(conditioning.get("kind") == "original-guide-v2"
                 and conditioning.get("filename") == f"grindless-{spec['asset_id']}-guide-v2.png"
                 and conditioning.get("denoise") == preset["denoise"], "Unqualified conditioning settings.")
    core.require(conditioning.get("sha256") == core.sha256(guide_image(spec)), "Native guide/hash mismatch.")
    transform = spec.get("transform", {})
    core.require(transform.get("size") == 16 and transform.get("resampling") == "nearest"
                 and transform.get("rotation") == 0 and transform.get("mirror") is False
                 and transform.get("alpha") == "binary", "Preserve native hard pixels, UV orientation and size.")
    kind = preset["kind"]
    expected = "item" if kind == "item" else "block"
    core.require(spec.get("runtime_path") == f"common/src/main/resources/assets/grindless/textures/{expected}/{spec['asset_id']}.png",
                 "Runtime path differs from the declared asset.")
    if kind == "item":
        core.require(transform.get("margin") == 1 and transform.get("background") == "#FFFFFF"
                     and transform.get("key_tolerance") == 44
                     and transform.get("minimum_foreground_fraction") == 0.025
                     and transform.get("maximum_foreground_fraction") == 0.75
                     and transform.get("maximum_dust_fraction") == 0.015, "Unqualified item cleanup settings.")
    else:
        core.require(transform.get("crop") == "full-frame" and transform.get("margin") == 0,
                     "A block face/overlay must never auto-crop or shift its UVs.")
    if "mask_rows" in transform:
        core.require(kind != "item", "Item silhouette cleanup cannot silently ignore a tile mask.")
        masks = transform["mask_rows"]
        core.require(len(masks) == 16 and all(isinstance(row, str) and len(row) == 16
                     and set(row) <= {"0", "1"} for row in masks)
                     and "1" in "".join(masks) and "0" in "".join(masks), "Use a nonempty bounded native binary edit mask.")
        core.require(re.fullmatch(r"[0-9a-f]{64}", transform.get("mask_source_sha256", "")), "Mask source hash is required.")
    core.require(kind != "overlay" or "mask_rows" in transform, "Overlay processing needs an explicit native edit mask.")
    if kind in ("block_face", "machine_family"):
        core.require(all("." not in row for row in rows), "Opaque block guides must cover the full frame.")
    return spec


def legacy_item(spec):
    adapted = copy.deepcopy(spec)
    adapted["schema"] = "grindless.visual-asset.v1"
    adapted["family"] = "item"
    return adapted


def verify_sprite(image, spec):
    if spec["family"] == "item":
        core.verify_sprite(image, legacy_item(spec))
        return
    core.require(image.size == (16, 16) and image.mode == "RGBA", "Expected a native RGBA tile.")
    palette = {core.rgb(value) for value in spec["visual"]["palette"]}
    guide = native_guide(spec)
    mask = spec["transform"].get("mask_rows")
    for index, pixel in enumerate(image.get_flattened_data()):
        core.require(pixel[3] in (0, 255), "Native alpha must be binary.")
        core.require(pixel[:3] in palette if pixel[3] else pixel[:3] == (0, 0, 0),
                     "Palette or transparent RGB differs.")
        if spec["family"] != "overlay":
            core.require(pixel[3] == 255, "Block tiles must be opaque.")
        else:
            core.require(pixel[3] == guide.getpixel((index % 16, index // 16))[3],
                         "Overlay alpha footprint changed.")
        if mask and mask[index // 16][index % 16] == "0":
            core.require(pixel == guide.getpixel((index % 16, index // 16)), "A protected outside-mask pixel changed.")


def process_image(source, spec):
    validate_spec(spec)
    Image = core.image_module()
    if spec["family"] == "item":
        output, report = core.process_image(source, legacy_item(spec))
        report.update(spec_sha256=core.sha256(core.json_bytes(spec)), preset_id=spec["preset_id"],
                      preset_sha256=spec["preset_sha256"])
        return output, report
    with Image.open(io.BytesIO(source)) as opened:
        core.require(opened.format == "PNG" and opened.size == (512, 512)
                     and not getattr(opened, "is_animated", False), "Expected one full-frame 512px source PNG.")
        core.require(opened.convert("RGBA").getextrema()[3] == (255, 255), "Opaque concepts must not contain transparent holes.")
        image = palette_map(opened.convert("RGBA").resize((16, 16), Image.Resampling.NEAREST), spec["visual"]["palette"])
    guide = native_guide(spec)
    mask = spec["transform"].get("mask_rows")
    for y in range(16):
        for x in range(16):
            if mask and mask[y][x] == "0":
                image.putpixel((x, y), guide.getpixel((x, y)))
            if spec["family"] == "overlay" and guide.getpixel((x, y))[3] == 0:
                image.putpixel((x, y), (0, 0, 0, 0))
    verify_sprite(image, spec)
    output = io.BytesIO()
    image.save(output, format="PNG", compress_level=9)
    return output.getvalue(), {
        "transform": "full-frame-nearest-palette-native-mask-v1", "source_sha256": core.sha256(source),
        "spec_sha256": core.sha256(core.json_bytes(spec)), "preset_id": spec["preset_id"],
        "preset_sha256": spec["preset_sha256"], "template_sha256": core.file_sha256(core.template_path(spec)),
        "runtime_png_sha256": core.sha256(output.getvalue()), "runtime_rgba_sha256": core.sha256(image.tobytes()),
        "mask_preserves_outside_pixels": bool(mask), "diffusion_inpainting": False,
        "rotation": 0, "mirror": False, "human_visual_approval": False,
    }


def prepare(preset_id, asset_id, registry_id, revision, source, output, mask=None):
    catalogue = core.read_json(PRESETS)
    core.require(preset_id in catalogue["presets"], "Unknown preset.")
    preset = catalogue["presets"][preset_id]
    source_bytes = source.read_bytes()
    Image = core.image_module()
    with Image.open(io.BytesIO(source_bytes)) as opened:
        core.require(opened.format == "PNG" and opened.size == (16, 16)
                     and not getattr(opened, "is_animated", False), "Use an actual single native source guide, not a 512px AI image.")
        original = opened.convert("RGBA")
        core.require(all(pixel[3] in (0, 255) for pixel in original.get_flattened_data()),
                     "Native source guides must have hard alpha before conditioning.")
        guide = palette_map(original, catalogue["palettes"][preset["palette"]])
    palette = catalogue["palettes"][preset["palette"]]
    indices = {core.rgb(value): DIGITS[index] for index, value in enumerate(palette)}
    rows = ["".join(indices[guide.getpixel((x, y))[:3]] if guide.getpixel((x, y))[3] else "." for x in range(16))
            for y in range(16)]
    kind = preset["kind"]
    transform = {"size": 16, "resampling": "nearest", "rotation": 0, "mirror": False, "alpha": "binary", "margin": 0,
                 "crop": "full-frame"}
    if kind == "item":
        transform.update(margin=1, background="#FFFFFF", key_tolerance=44, minimum_foreground_fraction=0.025,
                         maximum_foreground_fraction=0.75, maximum_dust_fraction=0.015)
        del transform["crop"]
    if mask:
        mask_bytes = mask.read_bytes()
        with Image.open(io.BytesIO(mask_bytes)) as opened:
            core.require(opened.format == "PNG" and opened.size == (16, 16)
                         and not getattr(opened, "is_animated", False), "Mask must be one native PNG.")
            rgba = opened.convert("RGBA")
            core.require(all(pixel in ((0, 0, 0, 255), (255, 255, 255, 255)) for pixel in rgba.get_flattened_data()),
                         "Masks use exact opaque black/white, never antialiasing or alpha thresholds.")
            transform["mask_rows"] = ["".join("1" if rgba.getpixel((x, y))[0] else "0" for x in range(16)) for y in range(16)]
            transform["mask_source_sha256"] = core.sha256(mask_bytes)
    spec = {
        "schema": SCHEMA, "asset_id": asset_id, "registry_id": registry_id, "family": kind, "tier": "T0",
        "role": "Source concept only; the existing registered role and accepted art remain authoritative.",
        "source_revision": revision, "source_refs": ["tools/visual-assets/presets.json"],
        "preset_id": preset_id, "preset_sha256": core.sha256(core.json_bytes(catalogue)),
        "model_sha256": core.load_model(core.HERE / "sd15-model.json")["sha256"],
        "model_lock_sha256": core.sha256(core.json_bytes(core.load_model(core.HERE / "sd15-model.json"))),
        "visual": {"palette": palette, "lighting": "upper-left"},
        "positive_prompt": preset["prompt"], "negative_prompt": catalogue["negative_prompt"],
        "seeds": catalogue["generation"]["seeds"], "guide": {"source_sha256": core.sha256(source_bytes), "rows": rows},
        "conditioning": {"kind": "original-guide-v2", "filename": f"grindless-{asset_id}-guide-v2.png",
                         "denoise": preset["denoise"]},
        "transform": transform, "runtime_path": f"common/src/main/resources/assets/grindless/textures/{'item' if kind == 'item' else 'block'}/{asset_id}.png",
        "human_visual_approval": False,
    }
    spec["conditioning"]["sha256"] = core.sha256(guide_image(spec))
    validate_spec(spec)
    output.mkdir(parents=True, exist_ok=False)
    for name, data in (("spec.json", core.json_bytes(spec)), (spec["conditioning"]["filename"], guide_image(spec)),
                       ("guide-source.png", source_bytes)):
        with (output / name).open("xb") as stream:
            stream.write(data)
    if mask:
        with (output / "mask-source.png").open("xb") as stream:
            stream.write(mask.read_bytes())
    return spec


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preset", required=True, choices=("item-concept", "block-face", "machine-family", "masked-overlay"))
    parser.add_argument("--asset-id", required=True)
    parser.add_argument("--registry-id", required=True)
    parser.add_argument("--source-revision", required=True)
    parser.add_argument("--guide-source", type=Path, required=True)
    parser.add_argument("--mask", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        spec = prepare(args.preset, args.asset_id, args.registry_id, args.source_revision,
                       args.guide_source, args.output, args.mask)
        print(core.json_bytes({"spec_sha256": core.sha256(core.json_bytes(spec)),
                               "guide_sha256": spec["conditioning"]["sha256"],
                               "requests": 0, "human_visual_approval": False}).decode("ascii"), end="")
        return 0
    except (core.ContractError, OSError, ValueError, KeyError) as error:
        parser.exit(1, "Preset preparation refused: " + str(error) + "\n")


if __name__ == "__main__":
    raise SystemExit(main())
