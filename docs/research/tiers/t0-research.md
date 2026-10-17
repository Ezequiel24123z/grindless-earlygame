# T0 research: candidate screening, not permission to import

## Workshop decision, 2026-10-17

ADR-0129 activates a connected ceramic workshop without importing upstream material.
The inspected [Digitalfire grog reference](https://digitalfire.com/material/grog) describes
ground fired ceramics used as aggregate and the effect on drying/shrinkage. That supports
the role of a reusable fired byproduct, not a copied recipe or an exact game yield.
The reference text and images have no reuse permission established here and are not copied.
Classification: **conceptual inspiration only**, no dependency, no integration cost beyond
original implementation, low originality risk when retaining only the general material role.

The active design uses original counts and times. Dirt settling is an explicitly simplified
resource route; no precise scientific mass-balance claim is made. A charcoal-retort route was
considered but not selected: it would require a meaningful condensate sink before adding new
gas/fluid types. No speculative retort or copied industrial recipe is shipped.

The screening below is historical preparation. Its former no-expansion gate is superseded
for this workshop by the current owner-directed scope, not by the research itself.

Retrieved 2026-10-16; recorded 2026-10-17. Scope: conceptual preparation while the
[foundation gate](../../reviews/go-gate.md) remains closed. No upstream code, assets, recipes,
names, text or source layout were copied. No dependency was added for T0.

The README and accepted ADRs remain the design authority. Research does not override the
short two-iron Bootstrap, physical five-Matrix exit, or the first designed multiblock at T3.
This is a component-screening record, not a legal opinion or blanket redistribution clearance.

## Pinned sources and licence scope

The SHA is the inspected branch snapshot, not necessarily the commit of a separately mentioned
release tag. Asset exceptions and bundled components require their own review before import.

| Source | Inspected snapshot | Primary licence / notices | Observed scope and attribution boundary |
| --- | --- | --- | --- |
| [GTCEu Modern](https://github.com/GregTechCEu/GregTech-Modern) | `1.20.1`, `b3b0ce37989c2dd0b28c6780ec71e20231c57662` | [LICENSE](https://github.com/GregTechCEu/GregTech-Modern/blob/b3b0ce37989c2dd0b28c6780ec71e20231c57662/LICENSE), [credited works](https://github.com/GregTechCEu/GregTech-Modern/blob/b3b0ce37989c2dd0b28c6780ec71e20231c57662/README.md) | LGPL-3.0 code declaration; credited assets come from several projects. Their individual rights are unverified, not presumed covered or presumed absent. Literal code reuse requires applicable LGPL notices/source obligations; every asset needs exact provenance. |
| [Create](https://github.com/Creators-of-Create/Create) | `mc1.20.1/dev`, `588124239141029c5321afd38d20c2eaf872b32b` | [LICENSE.md](https://github.com/Creators-of-Create/Create/blob/588124239141029c5321afd38d20c2eaf872b32b/LICENSE.md) | The file distinguishes MIT code from All Rights Reserved material under `src/main/resources/assets`. MIT notices do not authorize copying those assets. Independently bundled Catnip provenance was not resolved in this pass. |
| [Immersive Engineering](https://github.com/BluSunrize/ImmersiveEngineering) | `1.20.1`, `e63e4824800945eccf3684200a2c2270e2e1cdf2` | [custom licence](https://github.com/BluSunrize/ImmersiveEngineering/blob/e63e4824800945eccf3684200a2c2270e2e1cdf2/LICENSE), [notices](https://github.com/BluSunrize/ImmersiveEngineering/blob/e63e4824800945eccf3684200a2c2270e2e1cdf2/README.md) | Custom source-visibility/credit and redistribution terms, with an addon-specific asset-remix clause. Grindless is not an IE addon. Credited sounds and derived textures have separate terms. No literal reuse is cleared. |
| [Modern Industrialization](https://github.com/AztechMC/Modern-Industrialization) | Legacy `1.20.1`, `338473051c912016d1efe487bfc250bbb2340ff1` | [LICENSE.java](https://github.com/AztechMC/Modern-Industrialization/blob/338473051c912016d1efe487bfc250bbb2340ff1/LICENSE.java), [asset declaration](https://github.com/AztechMC/Modern-Industrialization/blob/338473051c912016d1efe487bfc250bbb2340ff1/README.md) | MIT code; README declares CC0 assets. Preserve MIT notices for code. This is the most promising licence-screened candidate, but still verify the exact files, authors and supplied notices before copying anything. |
| [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) | `forge/1.20.1`, `1c2f96e70ca6b61e8120482923e9775d772fd9e7` | [LICENSE](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/1c2f96e70ca6b61e8120482923e9775d772fd9e7/LICENSE), [scope declarations](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/1c2f96e70ca6b61e8120482923e9775d772fd9e7/README.md) | Separate API MIT, core LGPL-3.0, texture/model CC BY-NC-SA 3.0, text/translation CC0 and credited sound terms are declared. Never infer an API licence covers art. NonCommercial and ShareAlike material requires an explicit distribution decision; no import is cleared. |
| [CraftTweaker](https://github.com/CraftTweaker/CraftTweaker) | `1.20.1`, `f63201946a95c0944571fb92e9172f63196d7c4d` | [LICENSE](https://github.com/CraftTweaker/CraftTweaker/blob/f63201946a95c0944571fb92e9172f63196d7c4d/LICENSE) | MIT code declaration. Branding/CDN images are outside this review. A recipe-manager example was read on the current default branch, not proven at this 1.20.1 snapshot; do not treat its API as a verified 1.20.1 integration contract. |
| [Factorio](https://www.factorio.com/) | Live pages retrieved 2026-10-16; no immutable source snapshot | [terms](https://www.factorio.com/terms-of-service), [research reference](https://wiki.factorio.com/Research) | Proprietary game, not a code/asset source for this project. Its own mod ecosystem permissions do not establish rights for an unrelated Minecraft project. No assets or distinctive progression are to be reproduced. |

## Technical fit, tradeoffs and decisions

| Candidate | Relevant idea / problem solved | Strengths | Weaknesses and T0 fit | Integration cost / originality risk / classification |
| --- | --- | --- | --- | --- |
| GTCEu Modern | Coherent material forms, process families, conditions and physical control/research items organize a large industrial graph. | Strong primary reference for industrial roles and cross-dependent chains. | Its scale and detailed progression are not a twenty-minute Bootstrap. Do not clone its scanner/data-stick route or introduce later chemistry by resemblance. | High integration and maintenance cost. High originality risk if expression or progression is copied. **Conceptual inspiration only.** |
| Create | A bounded manual kinetic action makes power visibly causal; transport and mechanisms communicate work. | Clear movement, cause/effect and reasons to automate. | Distinctive art and exact crank costs/timings must not become Grindless defaults. A second energy system would duplicate F0. | No dependency justified. MIT code is an **adaptation candidate**, but actual use is conceptual only; assets not cleared. |
| Immersive Engineering | Separate source, conversion and obstruction constraints give generators physical roles; structures communicate process scale. | Readable machine purpose and environmental constraints. | Custom terms and high-fidelity multiblocks are poor candidates for literal T0 import. T3 owns the first designed multiblock here. | High coupling and licence-review cost. **Conceptual inspiration only; explicit decision before reuse.** |
| Modern Industrialization | Declarative material parts/properties and queued recipe generation reduce hand-maintained duplication. | Good fit for reviewing Grindless's existing catalogue/generator seam. | The inspected 1.20.1 line is an older snapshot, not current-project capability evidence; runtime/platform integration is not established. | Medium-to-high port cost. **Code-reuse/adaptation candidate** and licence-screened **asset-reuse candidate**, not selected. Study the general pattern and preserve Grindless's own catalogues. |
| Applied Energistics 2 | Grid/node/service separation supports large logistics networks. | Useful later storage and autocrafting reference. | Wrong frontier for T0; ADR-0120 explicitly reserves complete integration for T4. API paths were identified, not an exhaustive behavior review. | High product/integration cost and asset-licence constraints. **Requires explicit decision; held for T4.** |
| CraftTweaker | Explicit, reversible recipe actions suggest auditable data changes instead of hidden mutation. | Useful for diagnostics and reload boundaries. | No scripting dependency is needed to implement the current deterministic graph. The observed API example is not pinned to 1.20.1. | Low conceptual cost, unestimated implementation cost until an API spike. **Adaptation candidate**, actual use conceptual only. |
| Factorio | Visible flow, constrained throughput and a reason to replace repeated hand work. | Strong secondary reference for factory readability. | Science-pack queues are not permission to restore the removed global research gate or copy a tech tree. | Zero dependency cost; high originality risk for copied presentation. **Conceptual inspiration only.** |

The general requirements extracted are original: make state explain the next action, declare
materials once, preserve quantities and identities across views, expose bottlenecks, and make
automation remove repeated handling rather than merely shorten an inflated timer.

## Scientific grounding, with explicit limits

| Source | Relevant fact / fit | Limit and decision |
| --- | --- | --- |
| [NIST metrology](https://www.nist.gov/metrology), retrieved 2026-10-16 | Measurement and traceability support explaining why a physical calibration step exists. | A general reference, not validation of a thirty-second game recipe or a hand-crank design. Cite the idea; do not import figures or assume all hosted/coauthored material shares one licence. |
| [USGS Mineral Resources Program](https://www.usgs.gov/programs/mineral-resources-program/about), retrieved 2026-10-16 | Resource occurrence, extraction, use and recycling form a connected material lifecycle. | The mission page does not establish a specific leaching process, yield or early-game chemical recipe. A candidate technical report URL did not resolve; no detailed hydrometallurgical recipe is justified by this research. |

## Reuse boundary and next action

The [external reuse register](../../compliance/external-reuse-register.md) records screening
status. [THIRD_PARTY_NOTICES.md](../../../THIRD_PARTY_NOTICES.md) remains the authority for
actual imports; no entry is added there merely for inspiration.

Before any future import: choose exact files, confirm the licence at the chosen revision,
inspect per-file/asset exceptions and bundled notices, establish modification/redistribution
rights for the intended distribution, and commit the required notices with the material.
Unverified is not permission. No source in this report is a requirement to add a dependency.

The [T0 preparation specification](../../tiers/t0.md) may use these general principles.
Its gameplay proposals stay draft until the GO gate and existing frontier constraints are resolved.
