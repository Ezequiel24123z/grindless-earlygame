# Supervised local ComfyUI asset workflow

ADR-0157 makes ComfyUI a development-only **source candidate** backend, not the
Minecraft asset authority. No model, ComfyUI dependency, external image account,
Python dependency or network operation is added to the normal mod build.
Private host paths, personal workflows and generation endpoints stay outside Git.

## Current bounded target and visual contract

The coordinator selected `grindless:multitool` after inspection of the actual
source PNG and `FormTextures.multitool`, which is explicitly a placeholder.
`MultitoolItem` rotates and relocates owned blocks; it does not mine. This item
is outside the nine protected material/core sprites and three reviewed guides.
Preserve its warm `#C9A227` brass identity, manual-tool silhouette, implied
material volume, upper-left lighting and T0 readability. Do not add powered
cyan, a battery, a mining head, tiny text or future-tier semantics.

The original [structured specification](../../tools/visual-assets/specs/multitool.json)
records materials, silhouette, motif, wear, static state, tier readability,
orientation, palette, source authorities, prompt and two fixed seeds. These
requirements decorate the existing item, not a second gameplay catalogue.
Only the shared item/material behavioral class is implemented. Machines, state
variants, books and UI need their own evidence before sharing this template.

## Inspection checkpoint

Source intake is exact `7e58f92e846f7580968393be0a2e0a99ed883e2c`.
Strict SSH inspected only the owner-identified installation and relevant model
references. No unrelated workflow prompts, private images, account logs,
credentials or process command lines were read.

Two installations exist: portable AMD at ComfyUI commit
`c2bcbecd82ec5ae66594340b395c24ef0217b238` and ROCm at
`a52921417de6232527bdddd1f325e693be88b448`, reporting version `0.32.0`.
The portable runtime has torch `2.9.1+rocm7.2.1` and Pillow `12.3.0`;
the ROCm runtime has torch `2.12.0+rocm7.15.0a20260728` and Pillow `12.2.0`.
Package metadata is not a backend or inference pass.

The portable model directories contain only eight tiny approximation models,
not a checkpoint. ROCm contains one 2,132,696,762-byte SD1.5 fp16 checkpoint,
Anima, Qwen conditioning/VAE and three LoRAs. Only the existing SD1.5 is selected;
no Anima/Qwen/LoRA or custom node is needed. The tensor header names Stable
Diffusion v1.5, and the actual checkpoint SHA-256 is
`e9476a13728cd75d8279f6ec8bad753a66a1957ca375a1464dc63b37db6e3916`.
The [model lock](../../tools/visual-assets/sd15-model.json) binds the actual
length/hash to Comfy-Org's immutable archive upload
`4fddeb7f9096623f1b77f4708feb96126a08a0cf`. Its git-LFS content identifier matches
the independently computed local hash exactly. The archive describes this file
as an FP16 conversion plus metadata header, not the original FP32 bytes.
CreativeML OpenRAIL-M section 6 claims no rights in generated output but forbids
output uses contravening its provisions, including use-based restrictions.
Do not use an unrelated mirror's MIT tag to relicense these weights.
No weights are downloaded, copied or distributed; no ComfyUI GPL source is
copied into this original API client or graph. This is not legal assurance.

Initial observation: zero Python/ComfyUI/Java processes and no listeners on
8188/8189; loopback `/queue` on 8188 is unavailable. Seven existing ROCm workflow
files were counted without reading their contents. No installation/workflow,
queue, model, driver, global Python setting, firewall or player instance was
modified by inspection. An auxiliary PowerShell metadata serializer emitted
oversized object metadata and was stopped; a bounded explicit-string collection
recovered the inventory. That observation failure is not a backend failure.
Local `python` is a Microsoft Store alias and exits 9009; the pipeline therefore
uses the already installed ComfyUI Python, not an automatic installation.

Under the coordinator's exclusive lease `ade517967975e2fff7438d942a2c5676248277f8`,
the existing ROCm Python 3.12.9 imports torch/HIP7.15.26290 successfully.
The actual device reports RX6700XT/gfx1031, 12,868,124,672 bytes total VRAM and
12,720,144,384 free before a 16-element native GPU kernel. Its real sum is16.0;
synchronization succeeds and allocated/reserved memory returns to zero.
No CPU fallback or runtime installation was used. This kernel is not diffusion
inference. A fresh preflight measured12.301GiB physical/29.394GiB commit and
157.846GiB work disk, with0Java/Python/listeners before that probe.

The separately owned server then becomes ready on loopback only. Its API confirms
ComfyUI0.32.0, the same torch build, the actual RX6700XT and the real core
`CLIPTextEncode` node. Both queue counts are zero; the listener belongs only to
the recorded owned Python PID. The private launch-argument digest is
`ccf8ba89859fbe866d77737a4c07cfd43d7eaf223426ad78b64f2c7c8085758c`.
No personal launcher, custom node, Manager, provider node or auto-updater is used.
This readiness is not an inference pass.

The frozen client `11b7f1ef3f6ca08a1928ec2bf3fe48b5cbad0a41ada2148354d76f8f29a8d5ef`
and test file `58cd8e30c1d31d50fd45e63430859b84d416efbaf8875581f783bb771848f611`
pass17 offline contracts in4.505s under the existing PC Python/Pillow, exit0.
The retained stderr log hashes to
`b8a55767001d1a58d19b2d8075bf2ad5d3367ce08c75e1b18ff5cf70ffc02936`.
Those use synthetic PNGs, a synthetic checkpoint and a loopback fake API, not
diffusion inference. Refusals cover changed model/spec, budget/slot exhaustion,
ambiguous submission, timed-out collection/resumption, duplicate sources, foreign
outputs, occupied queues, stale locks, invalid native pixels and source hashes.
The initial successful17-case run exposed Pillow's `getdata` deprecation warning;
the implementation now uses its existing supported flattened-data method, with
the duplicate-output disposition also explicit in the ledger. No real source
request was made by either test run.

## Reproducible generation and retained evidence

The original [API graph](../../tools/visual-assets/item-workflow.json) uses only
checkpoint loader, positive/negative CLIP, one 512-square latent, Euler/normal
KSampler at 20 steps/CFG7, VAE decode and SaveImage. Batch is one.
There are at most two sequential source reservations for this purpose.
Fixed seeds reproduce settings, not bitwise results across GPU/driver/runtime
changes. A cache hit or duplicate output is not independent inference evidence.

Run `visual-assets.ps1 -Python <existing-python> <action> ...`, or set `PYTHON`
and use the Bash twin. Both call the same original Python implementation.
No automatic package/model/node install, server launch, queue cancellation,
deployment or GPU fallback is hidden in either entrypoint.

1. Obtain a coordinator resource lease and refresh physical/commit/disk/process
   guards. Qualify the existing GPU runtime; never present CPU fallback as GPU proof.
2. Launch only an owned loopback server with custom/provider nodes and Manager
   disabled, browser launch disabled, and owned input/output/user/temp directories.
   Preserve the launcher digest, own PID, runtime code identities, raw failures,
   readiness and actual GPU memory observations outside Git.
3. `plan --spec <spec>` resolves the pinned model and original graph without
   submitting anything. `generate --allow-generation --spec <spec> --checkpoint
   <existing-file> --run-dir <owned-run> --slot 1` checks the model length/hash,
   queue emptiness and core node metadata before reserving a slot durably.
4. Reservation precedes the one `/prompt` POST. Transport ambiguity spends that
   slot; do not retry. `collect` resumes only a known recorded prompt through its
   exact `/history/<id>` and `/view` output. Never fetch another user's history,
   dump `/queue` bodies or call interrupt/delete APIs.
5. Retain the original source PNG, resolved workflow, spec, runtime metadata and
   request ledger. The client refuses duplicate sources, foreign output paths,
   redirects, proxy routing and non-loopback endpoints.
6. Run `process --spec <spec> --source <retained-png> --source-sha256 <hash>
   --output <new-directory>`. Processing never overwrites the source or a prior
   output directory. Review source pixels and the actual 16px result separately.

The finite reservation ledger protects one declared run, not an authorization
to create unlimited new runs. Parent approval still controls the purpose and
total allowance. A failed/unknown submit, timeout or execution remains visible.
The client never cancels a job. Stop only its positively identified server PID,
then report the free port, zero remaining owned processes and released resources.
Never reboot the PC.

## Deterministic transformation and integration boundary

The pipeline accepts only the graph's single-frame 512px PNG. It removes only
edge-connected near-white/transparent background, bounds foreground coverage,
and refuses detached substantial objects. The declared tiny dust allowance is
recorded, not silently generalized into object removal. It crops the retained
object, preserves orientation without rotation/mirroring, fits a 14px envelope
using nearest-neighbor sampling, quantizes to the declared material palette and
writes canonical zero-RGB transparency and hard alpha.

Checks cover 16px dimensions, format/frame, palette membership, transparent
one-pixel margin, connected silhouette, material-tone separation and hashes.
These are technical checks, not proof of correct wrench semantics, good art,
upper-left lighting, originality or actual inventory/JEI rendering.
The processing receipt records Python/Pillow versions, source/spec/template
hashes, crop, dust, transform identity and both RGBA/PNG hashes.

The coordinator approved a narrow normal-generator seam: after technical
selection and simultaneous parent-owned notices, an approved 16px source under
`tools/visual-assets/approved/` is copied with native JDK validation to the one
existing runtime path. ComfyUI and Python are not normal-generation dependencies.
Until that seam and selected PNG actually exist, the current placeholder remains
the only integrated asset. Keep predecessor/source/provenance and demonstrate
stage repeatability plus preservation of all other assets.

## Checkpoint disclosure

| Category | Actual state at source preparation |
| --- | --- |
| COMPLETED | Safe inventory, source-role review, exact model provenance, actual GPU kernel, isolated API readiness and17 offline pipeline contracts. |
| BLOCKED | None at this source checkpoint; the leased real-inference phase is next. |
| NOT EXECUTED | Source generation, native source selection/integration, normal-generator replay, build/deployment. |
| NEEDS HUMAN VALIDATION | All appearance, readability and actual client contexts. |
| NEEDS MY DECISION | None; coordinator selected Multitool and approved the narrow generator seam. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | None; existing SSH and local runtime are available. |
| COST / BILLING RISK | No paid service or download; bounded local energy/RAM/VRAM, at most two sources. |
| DEPLOYED STATE | Qualified559 remains unchanged; this source preparation is not deployed. |

| ComfyUI category | Actual state |
| --- | --- |
| COMFYUI ENVIRONMENT FOUND | Two installations; no API initially, then an isolated owned loopback API qualified separately. |
| REUSED COMPONENTS | Existing SD1.5 fp16, incorporated CLIP/VAE and existing Python/Pillow; no model copy. |
| INSTALLED/CHANGED COMPONENTS | Original project tooling only; no user-installation change. |
| MODEL/WORKFLOW SELECTED | Exact hash-qualified Comfy-Org SD1.5 FP16 plus original seven-node item/material workflow. |
| AUTOMATION METHOD | Loopback prompt/history/view; original deterministic offline processor. |
| POC ASSETS GENERATED | 0 at this checkpoint. |
| ASSETS INTEGRATED | 0 at this checkpoint. |
| NEEDS HUMAN VISUAL VALIDATION | All future source/native candidates. |
| BLOCKERS | None; real inference remains unexecuted, not a capability failure or pass. |
| DISK/COMPUTE IMPACT | Zero model downloads/copies; existing weights are about 1.99 GiB. Owned evidence expected below 32 MiB. |
| LICENSING/PROVENANCE NOTES | Comfy-Org FP16 hash matches; CreativeML OpenRAIL-M is recorded in the model lock. No third-party implementation copied. Parent notices precede asset import. |
| RECOMMENDED NEXT ASSET WAVE | Finish only this Multitool proof before proposing further families. |
