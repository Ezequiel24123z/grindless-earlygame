"""Source-bound T0 visual inventory and CPU contact sheets, not a Minecraft renderer."""

from __future__ import annotations

import argparse
from functools import lru_cache
import io
import math
from pathlib import Path
import re

import comfy_asset as core


ROOT = Path(__file__).resolve().parents[2]
JAVA = "common/src/main/java/io/github/ezequiel24123z/grindless/"
ASSETS = "common/src/main/resources/assets/grindless/"
ART = "tools/assetgen/io/github/ezequiel24123z/grindless/assetgen/"
FACINGS = ("north", "east", "south", "west")
PROTECTED = {
    "mineral_aggregate", "biomass", "slag", "ceramic_grog", "unfired_ceramic",
    "ceramic_insulator", "data_core", "calibrated_data_core", "relay_matrix",
    "process_atlas", "quest_book", "field_guide", "multitool",
}
ROLES = {
    "hand_crank_dynamo": "Manual F0 work; flywheel, crank and output state.",
    "crude_extractor": "Optional finite deposit extraction; barred feed aperture.",
    "research_terminal": "Physical Data Core calibration; raked display, not a global unlock.",
    "quern": "Dry grinding and recycling; stone discs and feed mouth.",
    "clay_mixer": "Wet ceramic/fibre mixing; bowl, paddle and copper binding.",
    "firing_chamber": "Formed ceramic firing controller; brick courses and fire door.",
    "settling_trough": "Oriented wet-separation controller; headbox and output gate.",
    "settling_channel": "Passive open channel; continuous rails, no live state.",
    "settling_screen": "Passive raised separation grate; no live state.",
    "geological_collector": "Renewable aggregate source; stone collector and timber.",
    "biocultivator": "Daylit biomass source; open planted bed.",
    "biomass_dynamo": "Biomass-powered F0 generation; stone firebox and clay.",
    "assembly_bench": "Primitive assembly; timber work surface, not Relay fabrication.",
    "fibreboard": "Renewable board stock; pressed strands, no machine indicator.",
    "hand_pump": "Manual water movement; upright piston and outlet.",
    "clay_conduit": "Directional gravity liquid transport; open trough.",
    "basic_tank": "Passive fluid storage; vessel and static state sight glass.",
    "conveyor_belt": "Directional item transport; two lanes.",
    "crude_manipulator": "Directional item transfer; short arm on a post.",
    "multitool": "Manual rotate/relocate tool; not a pickaxe or powered drill.",
    "crank_handle": "Offset wooden manual grip with a bored stone socket.",
    "mineral_aggregate": "Mixed geological fragments; renewable source material.",
    "biomass": "Fibrous organic clump; renewable biological feed.",
    "slag": "Reduction by-product; not an ore or a new T0 consumer.",
    "ceramic_grog": "Angular fired fragments for ceramic recycling.",
    "unfired_ceramic": "Wet-formed recoverable ceramic body.",
    "ceramic_insulator": "Fired ribbed ceramic substrate.",
    "data_core": "Uncalibrated physical ceramic data substrate.",
    "calibrated_data_core": "Calibrated physical substrate; distinct mark, not a global flag.",
    "relay_matrix": "Physical F1 exit component; rating remains NBT/text, not another PNG.",
    "process_atlas": "Live process/recipe reference; cyan connected schematic.",
    "quest_book": "Quest entry point; static gold list, not live progress.",
    "field_guide": "Retained deprecated compatibility item; teal bookmark; use redirects to current quest instruction.",
}
SCREEN_REFS = [
    JAVA + "client/ProcessMachineScreen.java",
    JAVA + "client/ProcessAtlasScreen.java",
    JAVA + "client/QuestBookScreen.java",
    JAVA + "client/FieldGuideScreen.java",
    JAVA + "menu/MachineMenuLayout.java",
    JAVA + "quest/QuestCatalogue.java",
    "forge/src/main/java/io/github/ezequiel24123z/grindless/forge/jei/GrindlessJeiPlugin.java",
    "forge/src/main/java/io/github/ezequiel24123z/grindless/forge/quest/HeraclesDefinitions.java",
    "forge/src/main/java/io/github/ezequiel24123z/grindless/client/forge/FluidClientPlatformImpl.java",
    JAVA + "client/ShopScreen.java",
    JAVA + "menu/ShopMenu.java",
    JAVA + "item/FieldGuideItem.java",
    JAVA + "client/QuestTeachingClient.java",
    "forge/src/main/java/io/github/ezequiel24123z/grindless/forge/quest/HeraclesQuestClient.java",
]


def local(root, relative):
    path = root / relative
    core.require(path.resolve().is_relative_to(root.resolve()), "Reference escapes the owned repository.")
    core.require(path.is_file(), "Missing source/runtime reference: " + relative)
    return path


def text(root, relative):
    return local(root, relative).read_text(encoding="utf-8")


def catalogues(root):
    source = text(root, JAVA + "registry/BlockCatalogue.java")
    statuses = {
        name: re.findall(r'"([a-z_]+)"', values)
        for name, values in re.findall(r'List<String>\s+(\w+)\s*=\s*List\.of\(([^;]+)\);', source)
    }
    entries = {}
    pattern = r'new Entry\("([a-z_]+)", Geometry\.(\w+), \d+, (true|false), (List\.of\([^)]*\)|\w+), MiningTool\.\w+\)'
    for name, geometry, facing, states in re.findall(pattern, source):
        entries[name] = {
            "id": name, "geometry": geometry.lower(), "facing": facing == "true",
            "states": re.findall(r'"([a-z_]+)"', states) if states.startswith("List.of") else statuses[states],
        }
    core.require(entries, "BlockCatalogue parsing found no entries; update the source projection.")
    bootstrap = text(root, JAVA + "recipe/BootstrapRecipes.java")
    ids = set(re.findall(r'"grindless:([a-z_]+)"', bootstrap))
    source_logic = text(root, JAVA + "source/SourceLogic.java")
    board = re.search(r'FIBREBOARD\s*=\s*"grindless:([a-z_]+)"', source_logic)
    core.require(board is not None, "No canonical fibreboard source.")
    ids.add(board[1])
    port_ids = {row["id"] for row in core.read_json(local(root, "tools/visual-assets/port-assets.json"))["ports"]}
    blocks = [entries[name] for name in entries if name in ids and name not in port_ids]
    items_source = text(root, JAVA + "registry/ModItems.java")
    registered = set(re.findall(r'register\("([a-z_]+)"', items_source))
    # These component outputs are authored outside the shaped crafting catalogue.
    ids.update(("calibrated_data_core", "relay_matrix", "mineral_aggregate", "biomass",
                "ceramic_grog", "unfired_ceramic", "ceramic_insulator", "slag",
                "process_atlas", "quest_book", "field_guide"))
    items = sorted(ids & registered)
    core.require(set(ROLES) == {block["id"] for block in blocks} | set(items),
                 "The T0 projection changed; reconcile roles with actual catalogues, never silently omit it.")
    return blocks, items


@lru_cache(maxsize=512)
def image_bytes(data):
    Image = core.image_module()
    with Image.open(io.BytesIO(data)) as opened:
        core.require(opened.format == "PNG" and not getattr(opened, "is_animated", False),
                     "Expected one PNG frame.")
        return opened.convert("RGBA")


def png_info(path):
    image = image_bytes(path.read_bytes())
    pixels = list(image.get_flattened_data())
    alpha = {pixel[3] for pixel in pixels}
    return {
        "dimensions": list(image.size), "single_frame": True,
        "binary_alpha": alpha <= {0, 255}, "opaque": alpha == {255},
        "visible_colors": len({pixel[:3] for pixel in pixels if pixel[3]}),
        "opaque_pixels": sum(pixel[3] == 255 for pixel in pixels),
        "transparent_rgb_zero": all(pixel[:3] == (0, 0, 0) for pixel in pixels if not pixel[3]),
    }


def model_data(root, relative, seen=()):
    core.require(relative not in seen, "Cyclic model parent.")
    model = core.read_json(local(root, relative))
    parent = model.get("parent", "")
    if parent.startswith("grindless:"):
        inherited = model_data(root, ASSETS + "models/" + parent.split(":")[1] + ".json", seen + (relative,))
        model = {**inherited, **model, "textures": {**inherited.get("textures", {}), **model.get("textures", {})}}
    return model


def texture_reference(textures, value):
    value = texture_value(textures, value)
    core.require(value.startswith("grindless:"), "External texture is not a Grindless source image.")
    return ASSETS + "textures/" + value.split(":", 1)[1] + ".png"


def texture_value(textures, value):
    seen = set()
    while value.startswith("#"):
        core.require(value not in seen and value[1:] in textures, "Missing/cyclic model texture alias.")
        seen.add(value)
        value = textures[value[1:]]
    return value


def indicator_mask(image):
    return ["".join("1" if sum(a * b for a, b in zip(image.getpixel((x, y))[:3],
                                                     (0.2126, 0.7152, 0.0722))) >= 80 else "0"
                    for x in range(4, 12)) for y in range(12, 15)]


def inventory(root, revision):
    core.require(re.fullmatch(r"[0-9a-f]{40}", revision), "Bind the inventory to an exact source revision.")
    blocks, items = catalogues(root)
    hashes, used, records, block_rows = {}, set(), [], []

    def bind(relative):
        hashes[relative] = core.file_sha256(local(root, relative))
        return relative

    def asset(relative, role, owner, source):
        bind(relative)
        used.add(relative)
        records.append({
            "runtime": relative, "owner": owner, "role": role, **png_info(root / relative),
            "source": bind(source), "provenance": "project-original deterministic JDK writer",
            "readability_review": "source pixels/contact sheets only; native client lighting/scale still pending",
            "human_visual_approved": False,
        })

    for block in blocks:
        name = block["id"]
        state_file = bind(ASSETS + f"blockstates/{name}.json")
        variants = core.read_json(root / state_file)["variants"]
        expected = {
            (f"facing={facing}" if block["facing"] else "")
            + (("," if block["facing"] else "") + "status=" + state if len(block["states"]) > 1 else "")
            for facing in FACINGS[:4 if block["facing"] else 1] for state in block["states"]
        }
        core.require(set(variants) == expected, "Actual blockstate/catalogue drift: " + name)
        masks, state_rows = {}, []
        for state in block["states"]:
            model_path = bind(ASSETS + f"models/block/{name}_{state}.json")
            model = model_data(root, model_path)
            slots = {key: texture_reference(model["textures"], value) for key, value in model["textures"].items()}
            for relative in sorted(set(slots.values())):
                if relative not in used:
                    writer = "BootstrapArt.java" if "bootstrap_" in relative or name == "fibreboard" else "MachineArt.java"
                    if "/casing_" in relative:
                        writer = "MachineTextures.java"
                    asset(relative, "block face/material shared by resolved model slots", name, ART + writer)
            front = image_bytes(local(root, slots["front"]).read_bytes())
            masks[state] = indicator_mask(front)
            state_rows.append({
                "state": state, "model": model_path, "texture_slots": slots,
                "elements": len(model.get("elements", [])),
                "indicator_luma80_mask": masks[state],
            })
        collisions = [[first, second] for index, first in enumerate(block["states"])
                      for second in block["states"][index + 1:] if masks[first] == masks[second]]
        item_model = bind(ASSETS + f"models/item/{name}.json")
        core.require(core.read_json(root / item_model)["parent"] == f"grindless:block/{name}_idle",
                     "A block item no longer inherits its actual 3D block model: " + name)
        block_rows.append({
            **block, "registry_id": "grindless:" + name, "role": ROLES[name],
            "source": JAVA + "registry/BlockCatalogue.java", "blockstate": state_file,
            "item_model": item_model, "blockstate_variants": len(variants), "state_models": state_rows,
            "indicator_shape_collisions": collisions, "human_in_client_review": False,
            "review": "needs human placement/lighting/adjacency/rotations/JEI validation",
        })
    for name in items:
        runtime = ASSETS + f"textures/item/{name}.png"
        asset(runtime, ROLES[name], name, ART + "FormTextures.java")
        bind(ASSETS + f"models/item/{name}.json")
        records[-1]["protected"] = name in PROTECTED
        records[-1]["source_pixel_audit"] = "keep existing sprite: no new source-scale defect demonstrated"
        records[-1]["review_status"] = "protected prior technical selection; human client pending" if name in PROTECTED else "source sheet reviewed; human client pending"
        if name == "multitool":
            records[-1].update(
                source=bind("tools/visual-assets/approved/multitool.png"),
                provenance="retained guided SD1.5 source2; canonical 127b PNG; unchanged proven JDK copy seam",
                provenance_record=bind("tools/visual-assets/approved/multitool.provenance.json"))
            core.require(hashes[runtime] == hashes[records[-1]["source"]], "Canonical Multitool differs.")
    asset(ASSETS + "textures/gui/process_machine.png", "Only the progress arrow UV [176,0,200,17] is blitted by the current screen.",
          "process_machine", ART + "GenerateAssets.java")
    records[-1]["source_pixel_audit"] = "Existing left-pointing triangular arrow; retain pending in-client flow/contrast review. Background pixels are not the live GUI."
    for relative in [JAVA + "registry/BlockCatalogue.java", JAVA + "registry/ModItems.java",
                     JAVA + "source/SourceLogic.java", JAVA + "recipe/BootstrapRecipes.java",
                     JAVA + "recipe/BootstrapProcesses.java", JAVA + "recipe/RenewableProcesses.java",
                     JAVA + "fluid/FluidLogic.java", ART + "BlockModels.java",
                     JAVA + "structure/WorkshopGeometry.java"] + SCREEN_REFS:
        bind(relative)
    port_contract = core.read_json(local(root, "tools/visual-assets/port-assets.json"))
    bind("tools/visual-assets/port-assets.json")
    available_overlays = {}
    for role, resource in port_contract["overlays"].items():
        relative = ASSETS + "textures/" + resource.split(":", 1)[1] + ".png"
        if (root / relative).is_file():
            asset(relative, "shared original " + role + " marking; no casing pixels, glow or animation",
                  "port_" + role, port_contract["writer"])
            available_overlays[role] = {"runtime": relative, "sha256": hashes[relative]}
    port_rows = []
    catalogue_source = text(root, JAVA + "registry/BlockCatalogue.java")
    for contract in port_contract["ports"]:
        name = contract["id"]
        present = bool(re.search(r'new Entry\("' + re.escape(name) + r'"', catalogue_source))
        row = {**contract, "source_catalogue_entry": present, "human_in_client_review": False}
        if present:
            state_path = bind(ASSETS + f"blockstates/{name}.json")
            variants = core.read_json(root / state_path)["variants"]
            core.require(set(variants) == {"facing=" + facing for facing in FACINGS},
                         "Port visual states must be the actual four facings, without invented status.")
            models = {}
            for variant in variants.values():
                core.require(isinstance(variant, dict), "Unexpected port model alternative; review the model contract.")
                model_path = bind(ASSETS + "models/" + variant["model"].split(":", 1)[1] + ".json")
                model = model_data(root, model_path)
                slots = {key: texture_value(model["textures"], value) for key, value in model["textures"].items()}
                for resource in slots.values():
                    if resource.startswith("grindless:"):
                        bind(texture_reference({}, resource))
                models[model_path] = {"texture_resources": slots, "elements": len(model.get("elements", []))}
            row.update(blockstate=state_path, models=models, item_model=bind(ASSETS + f"models/item/{name}.json"),
                       review_status="actual source models present; native formation/appearance remains unproved here")
        else:
            row["review_status"] = "awaiting infrastructure integration; no runtime port claimed from this visual contract"
        port_rows.append(row)
    all_pngs = {}
    for path in sorted((root / ASSETS / "textures").rglob("*.png")):
        relative = path.relative_to(root).as_posix()
        all_pngs[relative] = {
            "sha256": core.file_sha256(path),
            "scope": "T0 resolved" if relative in used else (
                "shared optional material matrix; not a mandatory T0 claim" if "/textures/item/" in relative
                and re.search(r"(raw_|_(dust|ingot|nugget|crushed|oxide|plate|rod|gear|wire|bolt|ring|concentrate|tailings))", path.stem)
                else "registered later/prototype resource; not accepted T0 gameplay"),
        }
    fluids = [{
        "id": "minecraft:water", "role": "T0 consumed/recovered liquid",
        "source": JAVA + "recipe/BootstrapProcesses.java",
        "runtime": "Minecraft water still/flow atlas + native Forge appearance/tint",
        "local_png": None, "review": "native appearance, tank fill and tooltip still need client review",
        "human_visual_approved": False,
    }, {
        "id": "grindless:carbon_monoxide", "role": "Shared tank/external-fluid compatibility hook; not a T0 recipe output",
        "source": JAVA + "fluid/FluidLogic.java",
        "runtime": "native fluid presentation via FluidClientPlatform; missing appearance is explicitly '?'",
        "local_png": None, "review": "source-only; do not invent a gas texture or a T0 production route",
        "human_visual_approved": False,
    }]
    return {
        "schema": "grindless.t0-experience-visual.v1", "source_revision": revision,
        "authorities": "Runtime catalogues/model JSON remain authoritative; this is a reproducible read-only projection.",
        "human_in_client_review": False, "human_art_approval": False,
        "sheet_boundary": "Actual native pixels and CPU model projections, NOT Minecraft screenshots or native render proof.",
        "counts": {
            "blocks": len(block_rows), "standalone_items_including_books": len(items),
            "state_models": sum(len(block["states"]) for block in blocks),
            "blockstate_variants": sum(block["blockstate_variants"] for block in block_rows),
            "resolved_t0_pngs": len(used), "all_distributed_texture_pngs": len(all_pngs),
            "source_catalogued_ports": sum(row["source_catalogue_entry"] for row in port_rows),
            "blocks_including_catalogued_ports": len(block_rows) + sum(row["source_catalogue_entry"] for row in port_rows),
            "state_models_including_ports": sum(len(block["states"]) for block in blocks)
            + sum(len(row.get("models", {})) for row in port_rows),
            "facing_state_variants_including_ports": sum(block["blockstate_variants"] for block in block_rows)
            + 4 * sum(row["source_catalogue_entry"] for row in port_rows),
        },
        "blocks": block_rows, "textures": records, "fluids": fluids,
        "presentation": [
            {"surface": "machine GUI", "source": SCREEN_REFS[0],
             "runtime": "procedural recessed slots, tank and energy; only arrow from process_machine.png",
             "review": "Check localized status wrapping, hover detail, water contrast and inventory at actual GUI scale."},
            {"surface": "Atlas / JEI", "source": [SCREEN_REFS[1], SCREEN_REFS[6]],
             "runtime": "existing item/block icons and native fluid rendering, not a second icon set",
             "review": "Check 16px machine silhouette, ingredient/output identity, water and recipe text."},
            {"surface": "legacy/Heracles quest entry",
             "source": [SCREEN_REFS[2], SCREEN_REFS[5], SCREEN_REFS[7], SCREEN_REFS[12], SCREEN_REFS[13]],
             "runtime": "both actual renderers share live recipe/port teaching and separate T0/T1 chapters; registered item icons",
             "review": "Check six port names, real role/face/slot help and paging. Possession is not formation/operation; the physical F1 claim remains server-owned."},
            {"surface": "legacy Guide", "source": [SCREEN_REFS[3], SCREEN_REFS[11]],
             "runtime": "retained saved item redirects to current quest instruction; protected field_guide inventory icon",
             "review": "Preserve sprite bytes and review the actual harmless redirect, not an obsolete required Guide route."},
            {"surface": "shop icons", "source": [SCREEN_REFS[9], SCREEN_REFS[10]],
             "runtime": "actual material barter menu reuses registered vanilla item/block icons; no minted currency or second icon set",
             "review": "Check real costs, stock, recovery limits, capacity/rejection text and quest-entry navigation at native GUI scale."},
        ],
        "port_hooks": {
            "baseline_registered_ports": [],
            "status": "actual composed catalogue/models projected; native formation, interaction and appearance remain unproved here",
            "ports": port_rows, "available_overlays": available_overlays,
            "visual_contract": "one transparent original role overlay per function/direction over shared family casing, not copied per-machine PNGs",
            "family_sources": {
                "firing": "minecraft:bricks supplied by Minecraft, never copied into Grindless",
                "settling": ASSETS + "textures/block/bootstrap_timber.png",
            },
            "human_in_client_review": False,
        },
        "files_sha256": dict(sorted(hashes.items())), "all_texture_index": all_pngs,
    }


def rotate(point, axis, angle, origin=(8, 8, 8)):
    values = [value - offset for value, offset in zip(point, origin)]
    a, b = {"x": (1, 2), "y": (2, 0), "z": (0, 1)}[axis]
    cosine, sine = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    values[a], values[b] = values[a] * cosine - values[b] * sine, values[a] * sine + values[b] * cosine
    return tuple(value + offset for value, offset in zip(values, origin))


def project(point, size):
    x, y, z = point
    scale = (size - 2) / 24
    return ((x + z - 16) * 0.7071 * scale + size / 2,
            ((x - z) * 0.4082 - (y - 8) * 0.8165) * scale + size / 2,
            x + y - z)


def render_model(root, relative, angle=0, size=16):
    """Orthographic cuboid/UV review aid; no Minecraft display, tint, AO or lighting claim."""
    Image = core.image_module()
    model = model_data(root, relative)
    target = Image.new("RGBA", (size, size))
    depth = [-math.inf] * (size * size)
    faces = {
        "north": ((0, 1, 0), (1, 1, 0), (1, 0, 0), (0, 0, 0)),
        "south": ((1, 1, 1), (0, 1, 1), (0, 0, 1), (1, 0, 1)),
        "west": ((0, 1, 1), (0, 1, 0), (0, 0, 0), (0, 0, 1)),
        "east": ((1, 1, 0), (1, 1, 1), (1, 0, 1), (1, 0, 0)),
        "up": ((0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)),
        "down": ((0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)),
    }
    for element in model.get("elements", []):
        low, high = element["from"], element["to"]
        for face_name, face in element["faces"].items():
            texture = image_bytes(local(root, texture_reference(model["textures"], face["texture"])).read_bytes())
            points = [tuple(high[i] if corner[i] else low[i] for i in range(3)) for corner in faces[face_name]]
            rotation = element.get("rotation")
            if rotation:
                core.require(not rotation.get("rescale"), "Rescaled elements need a qualified projection.")
                points = [rotate(point, rotation["axis"], rotation["angle"], rotation["origin"]) for point in points]
            points = [project(rotate(point, "y", -angle), size) for point in points]
            x0, y0, z0 = low
            x1, y1, z1 = high
            defaults = {
                "down": (x0, 16-z1, x1, 16-z0), "up": (x0, z0, x1, z1),
                "north": (16-x1, 16-y1, 16-x0, 16-y0), "south": (x0, 16-y1, x1, 16-y0),
                "west": (z0, 16-y1, z1, 16-y0), "east": (16-z1, 16-y1, 16-z0, 16-y0),
            }
            u0, v0, u1, v1 = face.get("uv", defaults[face_name])
            uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
            turns = face.get("rotation", 0) // 90
            uvs = uvs[turns:] + uvs[:turns]
            for indices in ((0, 1, 2), (0, 2, 3)):
                vertices = [points[index] for index in indices]
                uv = [uvs[index] for index in indices]
                a, b, c = vertices
                denominator = (b[1]-c[1])*(a[0]-c[0]) + (c[0]-b[0])*(a[1]-c[1])
                if abs(denominator) < 1e-9:
                    continue
                for y in range(max(0, math.floor(min(p[1] for p in vertices))),
                               min(size, math.ceil(max(p[1] for p in vertices)))):
                    for x in range(max(0, math.floor(min(p[0] for p in vertices))),
                                   min(size, math.ceil(max(p[0] for p in vertices)))):
                        wa = ((b[1]-c[1])*(x+.5-c[0]) + (c[0]-b[0])*(y+.5-c[1])) / denominator
                        wb = ((c[1]-a[1])*(x+.5-c[0]) + (a[0]-c[0])*(y+.5-c[1])) / denominator
                        weights = (wa, wb, 1-wa-wb)
                        if min(weights) < -1e-7:
                            continue
                        z = sum(w*p[2] for w, p in zip(weights, vertices))
                        if z < depth[y*size+x]:
                            continue
                        u, v = [sum(w*p[i] for w, p in zip(weights, uv)) for i in (0, 1)]
                        pixel = texture.getpixel((min(15, max(0, int(u))), min(15, max(0, int(v)))))
                        if pixel[3]:
                            target.putpixel((x, y), pixel)
                            depth[y*size+x] = z
    return target


def contact_sheet(rows, title, columns=4):
    Image = core.image_module()
    from PIL import ImageDraw
    cell_width, cell_height = 336, 184
    sheet = Image.new("RGBA", (columns*cell_width, 54 + math.ceil(len(rows)/columns)*cell_height), "#D3D7DC")
    draw = ImageDraw.Draw(sheet)
    draw.text((10, 8), title, fill="#121820")
    draw.text((10, 27), "Actual pixels / native 1x + nearest 8x / light + dark. NOT a Minecraft screenshot.", fill="#121820")
    for index, (label, image) in enumerate(rows):
        core.require(image.size == (16, 16), "Contact cells need native 16px inputs.")
        x, y = (index % columns)*cell_width, 54 + (index // columns)*cell_height
        draw.text((x+5, y+3), label[:51], fill="#121820")
        for offset, background in ((0, "#E0E8F0"), (166, "#20252B")):
            draw.rectangle((x+offset+3, y+20, x+offset+163, y+178), fill=background)
            sheet.alpha_composite(image, (x+offset+6, y+27))
            sheet.alpha_composite(image.resize((128, 128), Image.Resampling.NEAREST), (x+offset+30, y+44))
    return sheet


def write_png(path, image):
    with path.open("xb") as stream:
        image.save(stream, format="PNG", compress_level=9)


def sheets(root, report, directory, prefix):
    directory.mkdir(parents=True, exist_ok=True)
    written = []

    def page(kind, rows):
        for index in range(0, len(rows), 24):
            name = f"{prefix}.{kind}-{index//24+1}.png"
            write_png(directory / name, contact_sheet(rows[index:index+24], prefix + " / " + kind))
            written.append(name)

    item_rows = [(row["owner"], image_bytes(local(root, row["runtime"]).read_bytes()))
                 for row in report["textures"] if "/textures/item/" in row["runtime"]]
    page("items", item_rows)
    face_rows = [(Path(row["runtime"]).stem, image_bytes(local(root, row["runtime"]).read_bytes()))
                 for row in report["textures"] if "/textures/block/" in row["runtime"]]
    page("faces", face_rows)
    first = [row for row in face_rows if row[0].startswith(("quern_front_", "mixer_front_", "settler_front_"))]
    page("critical-states", first)
    page("critical-monochrome", [(label, image.convert("L").convert("RGBA")) for label, image in first])
    overlays = report["port_hooks"]["available_overlays"]
    if overlays:
        rows = []
        Image = core.image_module()
        for role, data in overlays.items():
            overlay = image_bytes(local(root, data["runtime"]).read_bytes())
            rows.append((role + " / transparent original overlay", overlay))
            for name in ("bootstrap_timber", "bootstrap_stone", "firebox_top"):
                body = image_bytes(local(root, ASSETS + "textures/block/" + name + ".png").read_bytes()).copy()
                body.alpha_composite(overlay)
                rows.append((role + " / ORIGINAL " + name + " sample", body))
        page("port-overlays", rows)
    projections = []
    for block in report["blocks"]:
        model = block["state_models"][0]["model"]
        for angle in ((0, 90, 180, 270) if block["facing"] else (0,)):
            projections.append((block["id"] + " / Y=" + str(angle), render_model(root, model, angle)))
    page("rotations", projections)
    gui = image_bytes(local(root, ASSETS + "textures/gui/process_machine.png").read_bytes())
    Image = core.image_module()
    from PIL import ImageDraw
    # The background region is retained for legacy compatibility, but current code blits only this arrow.
    arrow = gui.crop((176, 0, 200, 17))
    preview = Image.new("RGBA", (640, 236), "#D3D7DC")
    draw = ImageDraw.Draw(preview)
    draw.text((10, 8), "Only live GUI texture UV: progress arrow [176,0,200,17]. NOT a GUI screenshot.", fill="black")
    for x, background in ((8, "#E0E8F0"), (324, "#20252B")):
        draw.rectangle((x, 30, x+307, 225), fill=background)
        preview.alpha_composite(arrow, (x+8, 38))
        preview.alpha_composite(arrow.resize((192, 136), Image.Resampling.NEAREST), (x+48, 74))
    gui_name = prefix + ".gui-arrow.png"
    write_png(directory / gui_name, preview)
    written.append(gui_name)
    return {name: core.file_sha256(directory / name) for name in written}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--revision", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--sheets", type=Path)
    parser.add_argument("--prefix", default="t0-experience-visual")
    args = parser.parse_args(argv)
    try:
        core.require(re.fullmatch(r"[a-z0-9.-]+", args.prefix), "Unsafe sheet prefix.")
        report = inventory(args.root, args.revision)
        if args.sheets:
            report["contact_sheets_sha256"] = sheets(args.root, report, args.sheets, args.prefix)
        with args.output.open("xb") as stream:
            stream.write(core.json_bytes(report))
        print(core.json_bytes(report["counts"]).decode("ascii"), end="")
        return 0
    except (core.ContractError, OSError, ValueError, KeyError) as error:
        parser.exit(1, "Visual inventory refused: " + str(error) + "\n")


if __name__ == "__main__":
    raise SystemExit(main())
