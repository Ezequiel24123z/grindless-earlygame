# Image-generation discovery and visual asset pipeline

Observed 2026-10-20 against source
`269965b1b0b8c1becda74459a853f245105649ee`, inherited exactly in an isolated
worktree. This implements the discovery boundary in
[ADR-0156](../DECISIONS.md#adr-0156--disclose-blockers-and-separate-creative-capability-from-product-acceptance),
not a new provider grant, art acceptance or Tier decision.

## Decision

**Authorized and operational image generation: NOT VERIFIED.**
Keep the existing deterministic JDK pipeline as Grindless's production authority.
The owner subsequently identified an existing personal-PC ComfyUI installation;
**qualifying that local route now takes priority over introducing an external
paid dependency**. A separate specialized worker owns its inspection, workflow,
PoC and any asset changes. This workstream did not inspect that installation and
established no verified no-cost or already-authorized image-output route itself.
Its image PoC is NOT EXECUTED: zero image-inference requests, zero new images and
no replacement of an accepted resource. External-provider discovery remains
research, not the default implementation, and does not block the other lanes.

The important current finding is stronger than a missing Models credential:
**GitHub Models was fully retired on 2026-07-30.** Its catalogue, playground,
inference API and BYOK are no longer available to any customer, and it was separate
from Copilot ([GitHub notice](https://docs.github.com/en/github-models),
[retirement changelog](https://github.blog/changelog/2026-07-01-github-models-is-being-fully-retired-on-july-30-2026/)).
Do not build this pipeline around old Models tutorials or try a new token to
revive that service. Copilot model names, vision support and credits do not
establish a provider image-generation entitlement.

## CONFIRMED actual capabilities and limits

These are observations of this session, not an exhaustive account or disk audit.
Only public technical queries and display-safe metadata were used.

| Surface | Observed result | What it does not prove |
| --- | --- | --- |
| Existing local GitHub CLI | `gh 2.102.0`; `gh api user` succeeds under the existing personal `User` identity; `gh extension list` is empty. | No image-provider subscription, allowance, organization access or `gh models` extension is established. Authentication was not changed. |
| Live Models catalogue | Both existing-CLI GET forms return literal `OK`, not JSON. A separate unauthenticated GET of `https://models.github.ai/catalog/models` confirms HTTP 200, `Content-Type: text/plain`, four response characters, trimmed body `OK`. | HTTP/CLI success is not a usable catalogue or inference permission. The retirement finding comes from official documentation, not an inferred list of supported/unsupported models in this body. |
| App session tools | Settings list six MCP servers: three connected and three failed with missing executables. None of the connected tools exposes image synthesis. The `generate-image` skill is present and was inspected. | An enabled skill is instructions, not a connected renderer or a provider credential. Failed optional MCPs were not repaired or installed. |
| App-bundled organization catalogues | Two read-only catalogue diagnostics report `repository_not_visible` under the existing personal identity. | Personal repository access does not grant another organization's catalogue access. No account switch, login or permission request was attempted; private catalogue references are intentionally omitted. |
| Named local key-presence checks | `SKILL_IMAGE_GEN_OPENAI_KEY`, `SKILL_IMAGE_GEN_GEMINI_KEY`, `OPENAI_API_KEY`, `GEMINI_API_KEY` and `GOOGLE_API_KEY` are absent in the current process. Only Boolean presence was output. | This says nothing about other processes, secret stores, browser subscriptions or unsearched machines. No credential value, hash, profile or personal content was read. |
| Production assets | Existing catalogues, JDK generator, runtime PNGs and focused checks are available in the received source. | Structural availability and prior source-pixel review are not a new native-client or human art pass. |

An initial CLI `--jq` expression failed because of shell quoting. Subsequent
JSON parsing failed on the actual `OK` body. These are preserved discovery/
observer failures, not image calls or evidence that a model denied this account.
The existing external audio credential is not an image credential or grant.

### GitHub Models, Copilot vision and billing are different contracts

The historical Models API announced `GET /catalog/models`, chat-completion
inference and organization-scoped chat-completion inference, not an Images API
([historical API announcement](https://github.blog/changelog/2025-05-15-github-models-api-now-available/)).
It has **no current authentication, API/CLI, free tier, quota or paid-activation
route to qualify** after retirement. Historical limits/scopes are not current access.

Copilot's [supported-model list](https://docs.github.com/en/copilot/reference/ai-models/supported-models)
and [pricing table](https://docs.github.com/en/enterprise-cloud@latest/copilot/reference/copilot-billing/models-and-pricing)
do not document an image-output model/service for this task.
[Copilot vision](https://github.blog/changelog/2026-07-01-copilot-vision-is-generally-available/)
and the SDK's [image-input contract](https://docs.github.com/en/copilot/how-tos/copilot-sdk/features/image-input)
describe attaching images/PDFs for understanding. A `vision` capability is an
**input** capability, not a PNG-generation endpoint. Drawing original pixels with
ordinary code remains possible; that is deterministic asset work, not image-model output.

Current [Copilot credit billing](https://docs.github.com/en/copilot/concepts/billing-and-usage/individuals/billing)
and [legacy premium-request billing](https://docs.github.com/en/copilot/reference/copilot-billing/request-based-billing-legacy/copilot-requests)
cover their documented Copilot features. Neither establishes that those balances
can fund `api.openai.com` or Google's image API. [Local BYOK](https://docs.github.com/en/copilot/concepts/models/bring-your-own-key)
removes dependency on GitHub's Copilot API; a provider's own key, contract and
billing still apply. BYOK chat support also does not automatically wire an Images
API tool. Organization policy can restrict
[model access](https://docs.github.com/en/copilot/how-tos/copilot-on-github/set-up-copilot/configure-access-to-ai-models)
and BYOK; no corporate identity, entitlement or credits were borrowed.
Copilot capacity/rate limits are separate from a spending allowance and are not
an image-generation quota.

No Spark account, asset-export workflow or entitlement was inspected. The result
is **no verified usable route here**, not a universal claim about every GitHub
feature, subscription, future model or unsearched provider.

### Real external image APIs: documented, not authorized here

| Route | Actual output/API/authentication | Included/free, rate and billing boundary |
| --- | --- | --- |
| OpenAI GPT Image | The [GPT-Image-2 model card](https://developers.openai.com/api/docs/models/gpt-image-2) explicitly declares image output and `/v1/images/generations` plus `/v1/images/edits`. The [current guide](https://developers.openai.com/api/docs/guides/image-generation) also documents newer image models. Calls require separate OpenAI API authentication; organization verification may be required. | No working key, free image quota, provider credit or cost grant was established. Use [current image pricing](https://developers.openai.com/api/docs/pricing#image-generation), not a Copilot allowance. [Rate limits](https://developers.openai.com/api/docs/guides/rate-limits) depend on model/project/organization, including IPM; none was checked for an image account. |
| Gemini Developer API | The [image guide](https://ai.google.dev/gemini-api/docs/image-generation) documents actual image output, currently including `gemini-nano-banana-2.1`. The Google GenAI SDK/Interactions API uses a separately authorized [API key](https://ai.google.dev/gemini-api/docs/api-key); the installed skill's older `generateContent` recipe is not current-access evidence. | The inspected [pricing tables](https://ai.google.dev/gemini-api/docs/pricing) for Nano Banana 2.1, 3.1 Flash Image, 3.1 Flash Lite Image and 3 Pro Image say **Free Tier: Not available**. For example, 2.1 lists USD0.0336 per 1K output image, excluding input/other charges; this is not a quote or spend permission. General free text access or an AI Studio UI trial is not free API image generation. |
| Existing local ComfyUI | Owner-reported installation, not a Copilot-hosted provider. Hardware-only/PATH inspection did not qualify its configuration, working inference or weights; an independent assigned worker now owns that trace. | Preferred next evaluation, not a retroactive runtime pass. No provider image API fee need apply to an offline workflow, but setup, downloads, disk, power and maintenance are real costs. This workstream may not install/download or configure it. |

Gemini [rate limits](https://ai.google.dev/gemini-api/docs/rate-limits) are per
project/model/tier, not a fresh allowance per API key; available quota was not
inspected. [Paid setup](https://ai.google.dev/gemini-api/docs/billing) links billing
and may involve prepayment/automatic reload or postpay. No such setup, balance
lookup, payment method, budget edit or login was performed.

The installed skill's generic Gemini "free tier" onboarding is insufficient.
Its `gemini-2.5-flash-image` target also has conflicting official lifecycle evidence:
pricing warns of a 2026-10-02 shutdown, whereas the
[deprecation table](https://ai.google.dev/gemini-api/docs/deprecations) says
2027-03-15 and the [model page](https://ai.google.dev/gemini-api/docs/models/gemini-2.5-flash-image)
still lists it as stable. Do not claim a confirmed retirement or select that
recipe without resolving the discrepancy. No provider request was used to test it.
The conflict is recorded, not reconciled by further probing; it does not change
the absent authorized credential/free-quota proof or permit spend.

### Public catalogue discovery is not installation or entitlement

Two unauthenticated [Agent Finder](https://agentfinder.github.com/api/v1/search)
searches were made. The broad search surfaced a public
[Gemini API skill](https://github.com/google/skills/tree/730939d15d6f026cbd29f7ea707d66aa320fb93a/skills/cloud/gemini-api),
not authenticated image access. The MCP-only search returned:

1. **HTML/CSS to Image API** - `application/mcp-server+json`, score92,
   [registry metadata](https://api.mcp.github.com/v0.1/servers/io.github.htmlcsstoimage%2Fhtml-css-to-image/versions/1.1.0).
   Inspected: hosted screenshot/HTML rendering with separate OAuth, not an included image-model grant.
2. **ImageSorcery MCP** - `application/mcp-server+json`, score91,
   [registry metadata](https://api.mcp.github.com/v0.1/servers/sunriseapps%2Fimagesorcery-mcp/versions/1.0.0).
   Inspected: local recognition/editing, not a verified text-to-image backend; installation is unapproved.
3. **Glif Media Generation Agent** - `application/mcp-server+json`, score70,
   [registry metadata](https://api.mcp.github.com/v0.1/servers/app.glif%2Fglif/versions/1.0.1).
   Inspected: hosted generation requires a separate Glif OAuth/API key; its account credits and rights are unverified.
4. **EmpirioLabs AI MCP Server** - `application/mcp-server+json`, score65,
   [registry metadata](https://api.mcp.github.com/v0.1/servers/ai.empiriolabs%2Fmcp/versions/1.2.0).
   Discovery only; capability, authorization, terms and price were not qualified.
5. **HasData Google Images** - `application/mcp-server+json`, score52,
   [registry metadata](https://api.mcp.github.com/v0.1/servers/com.hasdata%2Fgoogle-images/versions/1.0.3).
   Discovery only; not an evidenced generation route or permission to reuse found artwork.

Scores are relevance only, **not trust/safety ratings**. Catalogue descriptions
are untrusted vendor claims; no result was installed, connected, enabled or run.
None solves the missing authorization/cost proof merely by being listed.

## Read-only personal-PC feasibility

The existing strict, noninteractive SSH connection succeeded without credential
changes. Only hardware properties and named command availability were inspected;
host addresses, usernames, private paths and access references are omitted.

| Observed metadata | Result and limitation |
| --- | --- |
| Windows | Windows 11 Pro N, build26200; GPU driver `32.0.21045.5002`. OS activation/licensing and driver compatibility were not audited or changed. |
| GPU/actual memory | AMD Radeon RX6700XT. The accessible single-adapter `HardwareInformation.qwMemorySize` value is12868124672 bytes, **11.9844 GiB**, a12GiB-class device. This is registry-reported dedicated memory, not measured free VRAM. |
| WMI limitation | `Win32_VideoController.AdapterRAM` reports4293918720 bytes,3.9990GiB. Its32-bit field underreports this device; do not mistake that for actual4GiB capacity. `nvidia-smi` is not installed and was not installed/run. |
| RAM |34278801408 bytes,31.925GiB usable physical;9.327GiB free at the successful snapshot. This is not a reserved execution window. |
| Storage | System volume8.679GiB free; work volume161.402GiB; other fixed volume28.926GiB. These are transient snapshots, not installation/storage permission or model-size guarantees. |
| Named PATH checks | `python` and `git` resolve as commands; `py`, `ffmpeg`, `magick`, `ollama`, `comfy`, `uv`, `openai`, `gemini` and `nvidia-smi` do not. Python execution, packages, portable installs and model directories were not inspected. |

The first hardware probe printed useful data but ended nonzero; a broader
display-registry enumeration then hit access denied before the proposed software/
listener inspection. Neither is hidden as a pass. A final narrow metadata probe
and single accessible adapter key succeeded with exit0, without elevation or
reading the denied registry subtree. **No completed uninstall-entry, port,
Python-package or model-file inventory is claimed.**

Use a mature engine rather than writing diffusion infrastructure. The inspected,
[pinned ComfyUI source](https://github.com/Comfy-Org/ComfyUI/blob/e3bee7ffcb7d6547ed0bcd15221792f3b8646b17/README.md)
documents Windows AMD/ROCm support, offline execution and saved workflows, but
that is not qualification of this installed driver/GPU. Its
[GPLv3 licence](https://github.com/Comfy-Org/ComfyUI/blob/e3bee7ffcb7d6547ed0bcd15221792f3b8646b17/LICENSE)
does not license model weights, LoRAs, reference images or generated derivatives.
AMD's inspected [7.2.1 Windows matrix](https://rocm.docs.amd.com/projects/radeon-ryzen/en/latest/docs/compatibility/compatibilityrad/windows/windows_compatibility.html)
does not list this GPU; the newer [10.0 matrix](https://rocm.docs.amd.com/en/docs-10.0.0/compatibility/compatibility-matrix.html)
also does not certify this exact card. Conversely, TheRock's
[development-status table](https://github.com/ROCm/TheRock/blob/main/SUPPORTED_GPUS.md)
marks Windows `gfx1031` release-ready while explicitly distinguishing development
status from released compatibility. Do not turn either omission into universal
impossibility, or a development/package claim into a passed device runtime.

**Local verdict: hardware-only feasibility, not backend qualification.** The
owner's later identification of an existing ComfyUI installation supersedes any
inference of absence from PATH checks, but does not establish usability. Its
inspection/configuration is assigned elsewhere; private installation paths stay
out of Git. No CUDA assumption, driver update, OS/WSL
change, software/model download, inference, JVM, Gradle or Prism write occurred.
Later evaluation needs an approved isolated directory/environment, exact
GPU/driver/backend qualification, independently licensed weights and fresh
RAM/VRAM/storage headroom. Prefer the work volume, preserve originals and
coordinate with the PC execution owner. Power/thermal load and interference with
client play/builds remain unmeasured operational costs.

### Standalone fallback PoC reference - NOT SELECTED / NOT RUN

This is a bounded option for the coordinator/new local worker, not a claim that
generation already works. **Reuse and inspect the identified installation and
existing licensed weights first.** The standalone engine/model pins below are a
fallback candidate, not permission to reinstall, replace or download anything.

At integration, ADR-0157 separately authorizes the existing local ComfyUI route.
The installation owner derives actual reuse and safe resource admission from that
environment. The hypothetical standalone ceilings below are not a replacement
for that authority or a claim that all local work still lacks owner permission.

| Boundary | Proposed finite experiment and stop condition |
| --- | --- |
| Backend | First qualify the existing ComfyUI revision/environment without modification. The pinned upstream source above is a fallback comparison, not an upgrade target. Use a core-only offline workflow; no partner/API nodes, cloud, new custom nodes or manager. Exact package/driver qualification precedes approval; no system-wide Python, driver or OS changes. |
| Model / licence | `stable-diffusion-v1-5/stable-diffusion-v1-5`, public revision `451f4fe16113bff5a5d2269ed5ad43b0592e9a14`, `v1-5-pruned-emaonly.safetensors`. Public [metadata](https://huggingface.co/api/models/stable-diffusion-v1-5/stable-diffusion-v1-5?blobs=true) declares `creativeml-openrail-m`; inspect the exact [model card](https://huggingface.co/stable-diffusion-v1-5/stable-diffusion-v1-5/blob/451f4fe16113bff5a5d2269ed5ad43b0592e9a14/README.md) and linked use restrictions before downloading. This is not MIT or a blanket output-rights guarantee. Do not redistribute weights with Grindless. |
| Download / integrity | Prefer zero new downloads if existing licensed weights are suitable. The fallback weights alone are 4265146304 bytes, approximately 4.27 GB; LFS SHA-256 `6ce0161689b3853acaa03779ec93eafe75a02f4ced659bee03f50797806fa2fa`. Only metadata was read here. An exact hash/size list for any missing runtime wheels remains unqualified; approve it before downloading, within a proposed 20 GiB total transfer ceiling, or stop. |
| Space / resource lease | One explicitly owned work-volume directory outside repositories, Prism, shared caches and personal data; proposed30GiB total staging ceiling and40GiB free disk before starting. Conservative proposed admission:12GiB free physical RAM and6GiB free dedicated VRAM, rechecked with the PC owner. The observed9.327GiB free RAM does not satisfy that proposal; free VRAM is unknown. No automatic process termination or storage migration. |
| One output | One original Multitool silhouette/material reference, one512x512 PNG, fixed seed156, Euler20steps/CFG7, batch1 and one sampler execution, at most10minutes. No tiny text, copied art, private references or runtime overwrite. A device/package/model failure stops the experiment; no retry, CPU fallback or model substitution without a new decision. |
| Validation / evidence | Confirm the exact model hash, expected local device, offline/no-provider execution and output count/dimensions/decodability. Record workflow, package/driver identities, seed, elapsed time, resource peak and output hash in private/session staging. Assess usefulness against the existing16px sprite; do not call the512px reference a runtime asset or a proven improvement. |
| Rollback | Preserve the existing ComfyUI installation/configuration/models. Preserve the one output/receipt if requested, stop only an explicitly owned process, release only its listener and remove only approved new named PoC files/environment. Never stop an existing owner server or remove its files as cleanup. No global settings, shared inventory, accepted pixels, launcher instance, world or game process is changed. |

This option has no intended external inference bill, but download bandwidth,
disk, power, setup time and a new software/model licence review are real resource
decisions. Package/driver qualification, permission to acquire/run software and
the execution lease remain prerequisites, not inferred consent.

## Actual Grindless surfaces

The [source-bound inventory](t0-asset-manifest.json) remains an inventory, not a
new catalogue: 19 T0 blocks and 14 standalone T0 icons. Read-only PNG-header
measurement of this source finds **230 item 16x16, 346 block 16x16, one GUI 256x256
and four armour 64x32 textures: 581 total, zero 32x32 PNGs**. These totals span
implemented assets, not 581 accepted T0 artworks. A 32px item pipeline is a
possible separately approved change, not the existing runtime or a reason to
upscale everything.

| Surface / existing authority | Deterministic production value | Bounded generative value, if later authorized |
| --- | --- | --- |
|16px standalone items, materials, components and by-products - `FormTextures`, `HeightField`, `Palette`, `SupplyCatalogue` | Native silhouettes, material relief, upper-left lighting, controlled palette/alpha and repeatable form-by-material variants. Preserve honest canonical resource/by-product meanings. | Explore one genuinely weak silhouette/material idea; redraw/encode it at native resolution. A large image or downsample alone is not a finished sprite. |
| Core/Matrix milestone and three books | Preserve the protected nine-item batch and Atlas/Quest Book/Field Guide pixels, names, family accents and reproducible writers. | No replacement in this workstream. Concepts must not erase the established Core/Matrix identity or invent quest progress. |
| Blocks, machine faces, casings and state variants - `BlockCatalogue`, `MachineArt`, `MachineTextures`, `BlockModels`, `WorkshopGeometry` | Reuse accepted geometry and real BlockItem model/display inheritance; compose common bodies with coherent state faces. | Family mood/material reference only. Do not independently generate every face/state or replace3D items with fake-isometric PNGs. |
| Overlays, effects and fluids - client renderers, `SurveyOverlay`, `MachineEffects`, actual fluid adapter | Keep status, quantities and faults bound to real data and existing text/geometry/texture contracts. Future masks can be authored deterministically. | Concepts cannot create emissive/animation behavior, supported ports, fluid identities or state semantics. Those need implementation approval. |
| Machine UI, Atlas, quests, guides and icons - `ProcessMachineScreen`, other screens and existing GUI writer | Pixel-aligned slots/progress/controls and localized native text. The256px machine sheet is not a book illustration; several other screens use fills/text, not background PNGs. | Optional design reference only; never bake dynamic/localized text, claims or recipes into generated imagery. |
| Future material/machine families | Extend existing catalogue-driven templates only when that Tier/family is authorized; reuse forms, parts, palettes and state mechanisms. | One approved concept per materially new family, then deterministic variants, not a paid call per recipe/material/state. No T1 work is opened here. |
| Concept/promo illustration | Separate documentation/promotion outputs from runtime resources and label them honestly. | Potentially useful at larger sizes, but not an inventory sprite, real gameplay screenshot, native-rendering proof or RC acceptance. |

The relevant source paths are indexed by [the asset route map](../AGENT-MAP.md).
In particular,
[GenerateAssets](../../tools/assetgen/io/github/ezequiel24123z/grindless/assetgen/GenerateAssets.java)
owns catalogue-driven writers, the selective guide mode and the256px GUI;
[FormTextures](../../tools/assetgen/io/github/ezequiel24123z/grindless/assetgen/FormTextures.java):759-772
still calls the Multitool a placeholder. **The next defensible deterministic
candidate is therefore the existing `multitool`, not another accepted book or
Core/Matrix redraw.** This is a source-backed opportunity, not a fresh human
judgment that its current rendering is unacceptable. No candidate image was made.
Without a native/client counterexample, **no additional visual correction is
established as necessary**. Retaining the nine/three reviewed sprites is valid
work; do not change them merely to produce activity.

## Recommended scalable pipeline and approval boundary

1. Start from the real catalogue/model chain and an observed native-size need.
   Preserve the twelve protected sprites, placed-machine identity, registry IDs,
   UVs, recipes, ratings and NBT. Keep source/art ownership explicit; do not run
   full generation while another lane owns resources.
2. Prefer a local `FormTextures`/material refinement for the Multitool when the
   coordinator assigns that slice. Keep the existing lighting/palette mechanism;
   assess actual16px silhouette, grip/head separation and material, not added
   detail for its own sake. Production-generator/resource edits are outside this
   document's ownership. Existing accepted guide output is not a new task.
3. First let the separately owned existing-ComfyUI qualification establish what
   local generation can actually do. Consider an external dependency only if
   deterministic/local work cannot meet the justified need. Then propose a named
   provider, current image-output model/version, single-component purpose, price/
   quota proof, data/rights terms and **finite request and currency ceilings**.
   Obtain explicit approval for any spend, login, installation, new credential or
   billing/resource activation. A key or pre-existing paid balance alone is not
   authorization. Notify the coordinator before any generation.
4. A future approved external PoC is at most **one request, one image, one weak T0
   component**, in ignored/session staging, not a batch or accepted-resource
   overwrite. Reserve the allowance before HTTP; ambiguous delivery consumes it
   and must not auto-retry. Reject an unpriced/unknown route rather than silently
   falling back to a paid provider. No such reservation/request occurred here.
5. Treat the result as reference until pixel-authored/encoded into a maintainable
   source path. Record provider/model/version, sanitized prompt, parameters,
   request accounting, rights/source notices and output hashes. Keep credentials,
   private access paths, signed URLs and personal content out of Git and logs;
   do not upload project code or private references. Local engine/model/custom-node
   licences are separate reviews. Clear imports in
   [THIRD_PARTY_NOTICES](../../THIRD_PARTY_NOTICES.md) before distribution.
6. Reuse [the existing asset checks/workflow](t0-asset-workflow.md): native-size
   alpha/margins/palette/silhouette contracts, model/alias/state bindings, protected
   hashes and two identical deterministic stages. Review1x/enlarged light/dark
   sheets separately from native inventory/hand/JEI/quest/client observation.
   Provider outputs and GPU workflows are not promised byte-reproducible merely
   by pinning a seed; preserve the actual accepted source/output. Reuse unchanged
   engine proofs; new client behavior/bindings need their own targeted evidence.

For subsequent tiers, keep templates and technical checks broad while spending
art review/concepts on genuinely new silhouettes/material families. Do not create
a second material/progression database or a bespoke renderer/provider framework.
Human appearance/UX judgment remains a separate release gate.

## Eight-category handoff

| Category | Actual result |
| --- | --- |
| COMPLETED | Source/ownership verified; installed tooling and safe account metadata examined; public provider/catalogue research and read-only hardware/PNG dimensions completed; deterministic-first pipeline and Multitool opportunity documented. |
| BLOCKED | No external authorized image-output/free-quota route verified; GitHub Models is retired, not waiting for a new token. The owner-identified existing ComfyUI installation is independently being qualified, not absent or accepted by this workstream. |
| NOT EXECUTED | Image PoC/generation, new asset pixels, installs/model downloads, billing changes, application edits, builds, smokes and client/Survival runs. |
| NEEDS HUMAN VALIDATION | Native appearance/readability, artistic quality and the owner's manual campaign; prior source-pixel approval is not that acceptance. |
| NEEDS MY DECISION | The standalone fallback above is not selected. Existing-local implementation is separately authorized under ADR-0157 and its owner qualifies actual reuse/resources. A paid external image route would still need specific approval. No decision is needed to retain current art safely. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | A later external image call needs authorized provider identity, secure credential injection and model/quota/rights evidence. Local qualification needs the separately owned execution/resource lease, not an assumed paid API. The existing deterministic pipeline needs no external credential; audio access is unrelated. |
| COST / BILLING RISK | Zero image-inference requests or new provider charges initiated. No paid activation. Normal Copilot research usage is separate; possible future provider, download/storage and electricity costs are not assumed zero or covered by Copilot. |
| DEPLOYED STATE | Unchanged by this workstream. The coordinator's recorded qualified559 JAR remains the deployment authority; no fresh deployment inspection, RC publication, human T0 acceptance/freeze or T1 activation is claimed. |

## Coordinator integration notes

Only this document belongs to the workstream checkpoint; no push, PR or deploy.
The coordinator owns root changelog/README/ADRs/handoffs and publication.

- Changelog: record actual discovery, Models retirement, scoped absent credentials,
  corrected64-bit GPU memory, no image PoC/spend and the deterministic recommendation.
- README step51/active handoff: mark discovery complete while image generation and
  native/human art acceptance remain unexecuted by this workstream; preserve the
  existing deployment and hand existing-ComfyUI qualification to its assigned owner.
- Route map: link this decision beside the existing asset workflow if useful.
  ADR-0156 already authorizes the boundary; no new product/architecture acceptance
  or generator seam change is introduced.

Documentation validation uses the existing link/ADR checker; application/runtime
inputs did not change, so no build, behavior suite or native replay is warranted.
