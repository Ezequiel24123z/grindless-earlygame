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
idle dash, running arrow, blocked pause, starved empty box and out-of-band cross.
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

At this source checkpoint, the new Python contracts and JDK selective stages
are **NOT EXECUTED**. The coordinator granted only CPU/Pillow and a separate
bounded 256MiB/CPU2/SerialGC JDK window on this unit's isolated surface; no
Gradle, native server, Prism or GPU launch is authorized here. Exact executed
results and native resource identities will be added after the frozen source
is exercised, not inferred from compilation or a prior Multitool pass.

| Category | Current entry |
| --- | --- |
| COMPLETED | Baseline global source/pixel inventory and first native-scale contact sheets; reusable source tooling and deterministic writer prepared. |
| BLOCKED | None. |
| NOT EXECUTED | New Python/JDK contracts, generated integration, native client, combined build/package/deployment. |
| NEEDS HUMAN VALIDATION | In-client lighting/placement/state/rotations, hotbar/JEI/quests, GUI comfort and artistic quality. |
| NEEDS MY DECISION | None. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None; reuse the existing local tools/model only if needed. |
| COST / BILLING RISK | Zero image requests, zero paid images, zero model downloads; finite purpose ceiling is not a spending target. |
| DEPLOYED STATE | Unchanged qualified747 owned development instance; this unit does not deploy. |
