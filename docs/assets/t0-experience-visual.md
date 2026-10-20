# T0 experience visual production

This unit implements the global visual workstream over exact baseline
`b8bd5e885aef598bf888d7e390b297a34cbb7d52`. It does not accept T0, launch Minecraft,
change a save or redefine a machine. Root changelog/map/handoff integration is
coordinator-owned.

## Inventory and demonstrated target

The actual baseline contains 19 T0 blocks, 65 state models, 257 facing/state
variants, 14 standalone icons including the three books, and 104 resolved T0
PNGs. All 581 distributed texture PNGs are indexed, including the optional
material matrix and later/prototype resources rather than silently calling all
registered content accepted T0 gameplay. Catalogues, registered item models,
resolved texture aliases, actual PNG pixels and source hashes are inspected.

The first real light/dark 1x/nearest-8x sheet proves that `blocked` and
`out_of_band` use the same four-pixel alternating mask, with only hue/luminance
changed. Quern, Mixer and Settler expose it particularly clearly. This is a
shared readability defect, not a reason to regenerate 14 working item sprites
or request AI concepts. T0 alone receives distinct neutral high-contrast marks:
idle dash, running activity mark, blocked pause, starved empty box and out-of-band cross.
The existing color vocabulary remains a secondary side cue. Only the 10x3
indicator region changes; existing mechanisms, materials, geometry and UVs stay
unchanged. A tank's state is not a dynamic fill measurement or a new animation.

All nine reviewed materials/cores, all three reviewed book sprites and canonical
Multitool `127b662a124c30490c3d6737dc29ce8175d37b201e45bdad18bc1ea7360c7d38`
remain protected. No source or runtime audio is in this unit's write set.

Contact sheets are **source-pixel evidence, not Minecraft screenshots**. The
CPU cuboid/UV projections show actual model elements and four blockstate
rotations, but do not simulate Minecraft's inventory display transforms,
ambient occlusion, resource-pack overrides, tint, lighting or GUI scale.
Actual placement, adjacent machines, hotbar, JEI, quests and lighting still
require a client review.

The final [machine-readable projection](t0-experience-visual.json) is bound to
`981552b9709dac4d083a418b69636e4bb1491710`: 108 T0-resolved PNGs including four
new shared overlays, all 585 texture PNGs indexed, and exact source/runtime
hashes. Six port IDs are declared by the infrastructure contract but **zero are
registered in this branch**; their actual models/capabilities belong to the
other lane. Re-run the same projection on the coordinator's merged source to
bind the real port models; never infer gameplay from this contract.

| Source-pixel review surface | Actual sheets |
| --- | --- |
| All 14 item/book sprites, 1x/8x light/dark | [Items](t0-experience-visual.items-1.png) |
| Every T0 face/material/state plus four markings | [1](t0-experience-visual.faces-1.png), [2](t0-experience-visual.faces-2.png), [3](t0-experience-visual.faces-3.png), [4](t0-experience-visual.faces-4.png) |
| Real state ambiguity before/after | [Before](t0-experience-visual.before.critical-states-1.png), [after](t0-experience-visual.critical-states-1.png) |
| State review without hue | [Before](t0-experience-visual.before.critical-monochrome-1.png), [after](t0-experience-visual.critical-monochrome-1.png) |
| Actual cuboid/UV projections at four rotations | [1](t0-experience-visual.rotations-1.png), [2](t0-experience-visual.rotations-2.png), [3](t0-experience-visual.rotations-3.png), [4](t0-experience-visual.rotations-4.png) |
| Shared markings alone and on original sample materials | [Ports](t0-experience-visual.port-overlays-1.png) |
| Only the GUI arrow UV used by the live screen | [Arrow](t0-experience-visual.gui-arrow.png) |

All sheets were actually inspected. The existing GUI arrow is a left-pointing
triangle, and generic dark casing remains on several primitive mechanisms;
record these for in-client flow/contrast/identity review rather than silently
redesigning working machines from a software projection. No extra regeneration
or subjective pass follows. The port samples use only Grindless's original
timber/stone/firebox art, **not** copied Minecraft brick pixels.

## Reusable workflows

[`presets.json`](../../tools/visual-assets/presets.json) defines four original
presets over the existing eight-core-node conditioned graph and pinned existing
SD1.5 model: item concept, block face, controlled machine-family variation and
masked overlay. No new node, model, image provider, account or paid image
entitlement is required.

[`visual_presets.py`](../../tools/visual-assets/visual_presets.py) prepares a new
immutable directory from an actual original 16px guide. It retains that source,
locks the shared palette, materializes the exact 512px nearest-neighbour guide,
and records canonical preset/spec/guide/model settings and two seeds before any
possible submit. Existing `comfy_asset.py` generation/reservation/collection
protects the same owned graph, model, queue, prompt history and ambiguous-submit
boundaries. Preparing a spec is not authorization to generate.

The item processor reuses the existing tested edge-connected key, bounded
component cleanup, margin, nearest sampling and hard-alpha contract. Block faces
and family variations preserve the entire UV frame: no auto-crop, rotation,
mirror or background key. Native masks use exact black/white pixels, preserve
every outside-mask pixel, and preserve an overlay's alpha footprint. This is
deterministic masking, **not** an unsupported diffusion-inpainting claim.
Raw concepts are never runtime resources; selection, deterministic processing,
technical checks, JDK integration and human in-client review remain separate.

PowerShell entrypoints, including embedded Python with an isolated `_pth`:

```powershell
.\tools\visual-assets\visual-assets.ps1 -Python <existing-python> -Tool visual_presets `
  --preset block-face --asset-id quern_front_idle --registry-id grindless:quern `
  --source-revision <exact-source> --guide-source <original-16px.png> --output <new-owned-directory>
.\tools\visual-assets\visual-assets.ps1 -Python <existing-python> `
  plan --spec <new-owned-directory>\spec.json
.\tools\visual-assets\visual-assets.ps1 -Python <existing-python> `
  process --spec <new-owned-directory>\spec.json --source <retained-source.png> `
  --source-sha256 <exact-sha256> --output <new-processing-directory>
```

Use `--preset item-concept`, `machine-family` or `masked-overlay` for the other
presets; the last requires `--mask <native-black-white.png>`. The Bash twin
selects the same tool with `--tool visual_presets`; otherwise it defaults to
the existing `comfy_asset` CLI. Neither entrypoint installs anything, starts
a server, cancels a queue or deploys resources.

The new [purpose ledger](t0-experience-visual.ledger.json) has a ceiling of six
local candidates across at most three demonstrated critical targets, two each.
No target or request is reserved: this wave needs **zero diffusion** to fix its
actual defect. The historical spent Multitool 2/2 ledger is untouched.

## Shared port markings and deterministic integration

[`port-assets.json`](../../tools/visual-assets/port-assets.json) binds four
original transparent role/direction overlays to six infrastructure-lane IDs.
Square and droplet distinguish item/fluid without hue; the arrow points into or
away from the role symbol. Cream ink and dark outline work over a separately
selected structural family. One PNG per role is shared, not copied for every
casing/state/rotation combination. Minecraft supplies its own brick texture;
no external texture is copied. Ports have no glow, animation or sound.
Registration, capabilities, family geometry and model hooks belong to the
infrastructure lane, not this visual contract.

`WorkshopPortTextures` is a JDK-only original writer. Normal `GenerateAssets`
calls that same routine; `--t0-visuals-only` writes exactly the 62 T0 state
fronts and four overlays, without deleting/rebuilding other resources, writing
models, audio, tags, localization or provenance side outputs. Existing
`--t0-guides-only` behavior and the approved Multitool copy remain independent.

## Verification and current disclosure

The baseline CPU inventory/contact-sheet run exited 0. Initial intake found
the baseline Git object absent; fetching the existing personal origin resolved
it without moving the original clone. Initial embedded-Python import failed
because its `_pth` excludes the script directory; a process-local import path
resolved it without changing/installing Python. Both failures remain history.

Exact source `1969138a3bca424ed87f69966d8cd9b6d657db24` now has actual javac 0,
two selective stages 0 and **32,773 pixel/writer checks on each**. Both 66-file
stages are byte-identical; the real baseline fails the intended glyph contract.
All 1,590 source resources/581 PNGs remain untouched during that proof. The
existing 12 Multitool/guide checks pass, and two invalid invocations exit 2
without output. Only those 62 generated fronts and four new overlays are
installed, not an unrelated full regeneration. The JDK lease is released with
clean source and zero Java before the next lane executes.

The same freeze passes the actual existing 28 Python contracts plus 16 new
preset/inventory cases, 44 total in 8.805s. A WinPS stderr-capture failure before
the first result remains recorded; read-only recovery found no Python process
and an empty log before the corrected capture. No success is inferred from that
observer failure. Later model-lock/spec binding and optional port-inventory
projection changes have a distinct receipt, not a relabelled 44-case pass.
The later exact `981552b` run passes **45 cases**, 28 existing
and 17 new, in 12.165s. The actual PowerShell entrypoint exports the final
manifest and 13 native-scale contact sheets with clean source and zero Python;
all exported hashes and all 66 installed stage PNG hashes match after transfer.
All 519 other baseline PNGs, including the 13 protected sprites, remain unchanged.
See the [portable result record](t0-experience-visual.validation.json).

The final default-entrypoint probe found a new PowerShell positional-binding
regression: bare `plan` bound to the newly added `Tool` parameter. Make `Tool`
named-only while preserving the positional Python argument. The explicit
`-Tool t0_visuals` inventory invocation above really passed; it did not cover
this backward-compatible default. The initial inline Bash observer also failed
in nested quoting before reaching the script; preserve both logs. The fixed
default PowerShell/Bash smoke is pending at this documentation checkpoint.

No Gradle, native server, Prism, GPU or new inference is used. The regular
generator remains JDK-only and calls the same checked face/overlay writers;
there is no need to repeat the old full 1,602-file proof just for more counters.

| Category | Current entry |
| --- | --- |
| COMPLETED | Complete global source/pixel inventory and 13 final sheets plus two baseline comparisons; reusable presets; 62 checked T0 state fronts/four shared overlays; actual 45 Python cases, two equal 66-file JDK stages and existing Multitool/guide regressions. |
| BLOCKED | None. |
| NOT EXECUTED | Comfy/GPU inference (unnecessary), native client, port registration/integration in this branch, combined build/package/deployment. |
| NEEDS HUMAN VALIDATION | In-client lighting/placement/state/rotations, hotbar/JEI/quests, GUI comfort and artistic quality. |
| NEEDS MY DECISION | None. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None; reuse the existing local tools/model only if needed. |
| COST / BILLING RISK | Zero image requests, zero paid images, zero model downloads; finite purpose ceiling is not a spending target. |
| DEPLOYED STATE | Unchanged qualified747 owned development instance; this unit does not deploy. |
