# Architecture decision records

Why Grindless is built the way it is. The README describes *what* the mod does; this file records
*why* each structural choice was made, and which alternatives were rejected.

Add a record whenever a decision would be non-obvious to someone reading the code cold. See
[`AGENTS.md`](../AGENTS.md) for when and how. Records are append-only: to reverse a decision, add a
new record that supersedes the old one and mark the old one `Superseded by ADR-xxxx`. Never edit
history — the reasoning that was wrong is itself useful information.

**Status values:** `Accepted` · `Superseded by ADR-xxxx` · `Proposed`

| # | Decision | Status |
| --- | --- | --- |
| [0001](#adr-0001--architectury-for-multiloader-support) | Architectury for multiloader support | Superseded by ADR-0039 |
| [0002](#adr-0002--no-neoforge-subproject-on-1201) | No `neoforge` subproject on 1.20.1 | Accepted |
| [0003](#adr-0003--mojang-mappings-layered-with-parchment) | Mojang mappings layered with Parchment | Accepted |
| [0004](#adr-0004--the-material-registry-is-built-from-tags-at-runtime) | Material registry built from tags at runtime | Accepted |
| [0005](#adr-0005--recipes-are-generated-at-runtime-not-shipped-as-json) | Recipes generated at runtime, not shipped as JSON | Accepted |
| [0006](#adr-0006--flux-units-convert-to-fe-at-11-over-a-hand-written-bridge) | Flux Units convert to FE at 1:1 | Accepted |
| [0007](#adr-0007--flux-network-state-lives-in-saveddata-with-a-spatial-index) | Network state in `SavedData` + spatial index | Accepted |
| [0008](#adr-0008--belt-contents-are-lane-data-not-entities) | Belt contents are lane data, not entities | Accepted |
| [0009](#adr-0009--ore-veins-are-derived-from-seed-and-coordinates) | Ore veins derived from seed and coordinates | Accepted |
| [0010](#adr-0010--replication-cost-is-derived-from-the-recipe-graph) | Replication cost derived from the recipe graph | Accepted |
| [0011](#adr-0011--remote-colonies-are-simulated-abstractly-never-force-loaded) | Remote colonies simulated abstractly | Accepted |
| [0012](#adr-0012--planets-come-from-installed-space-mods-and-satellites-have-no-upkeep) | Planets come from installed space mods | Superseded in part by ADR-0088 |
| [0013](#adr-0013--the-readme-is-the-design-source-of-truth) | The README is the design source of truth | Accepted |
| [0014](#adr-0014--consolidate-the-orphaned-session-branches-into-one-history) | Consolidate the orphaned session branches | Accepted |
| [0015](#adr-0015--fluids-are-modelled-as-state-not-as-items) | Fluids are modelled as state, not as items | Accepted |
| [0016](#adr-0016--pipes-early-the-phase-network-late) | Pipes early, the Phase Network late | Accepted |
| [0017](#adr-0017--machines-above-t1-are-manufactured-never-hand-crafted) | Machines above T1 are manufactured, never hand-crafted | Accepted |
| [0018](#adr-0018--one-container-contract-with-auto-void-off-by-default) | One container contract, auto-void off by default | Accepted |
| [0019](#adr-0019--design-machines-before-recipes) | Design machines before recipes | Accepted |
| [0020](#adr-0020--recipes-are-parameterised-by-process-conditions) | Recipes are parameterised by process conditions | Accepted |
| [0021](#adr-0021--complexity-beyond-gregtech-with-no-grind) | Complexity beyond GregTech, with no grind | Superseded in part by ADR-0088 |
| [0022](#adr-0022--parametric-multiblocks-instead-of-fixed-schematics) | Parametric multiblocks instead of fixed schematics | Accepted |
| [0023](#adr-0023--a-native-route-viewer-with-a-ratio-solver) | A native route viewer with a ratio solver | Accepted |
| [0024](#adr-0024--loomplatform-markers-and-the-windows-path-length-limit) | `loom.platform` markers and the Windows path limit | Accepted |
| [0025](#adr-0025--universal-conduits-that-do-not-obsolete-belts-or-pipes) | Universal conduits that do not obsolete belts or pipes | Accepted |
| [0026](#adr-0026--named-conduit-networks-instead-of-coloured-channels) | Named conduit networks instead of coloured channels | Accepted |
| [0027](#adr-0027--chassis-marks-widen-the-condition-envelope) | Chassis marks widen the condition envelope | Accepted |
| [0028](#adr-0028--every-machine-upgrade-is-a-trade) | Every machine upgrade is a trade | Accepted |
| [0029](#adr-0029--operator-drones-imperative-automation-beside-declarative-logistics) | Operator Drones: imperative automation | Accepted |
| [0030](#adr-0030--everything-is-a-chassis-plus-modules) | Everything is a chassis plus modules | Accepted |
| [0031](#adr-0031--construction-drones-exist-so-multiblocks-can-be-massive) | Construction drones exist so multiblocks can be massive | Accepted |
| [0032](#adr-0032--the-item-catalogue-is-a-matrix-not-a-list) | The item catalogue is a matrix, not a list | Accepted |
| [0033](#adr-0033--materials-come-from-tags-reagents-are-grindlesss-own) | Materials come from tags, reagents are Grindless's own | Accepted |
| [0034](#adr-0034--ratios-are-quoted-against-a-canonical-process-unit) | Ratios are quoted against a canonical process unit | Accepted |
| [0035](#adr-0035--routes-compose-from-a-beneficiation-stage-and-a-reduction-stage) | Routes compose from a beneficiation and a reduction stage | Accepted |
| [0036](#adr-0036--every-byproduct-must-have-a-named-sink) | Every byproduct must have a named sink | Accepted |
| [0037](#adr-0037--flux-amounts-are-long-and-clamp-at-the-fe-boundary) | Flux amounts are `long` and clamp at the FE boundary | Accepted |
| [0038](#adr-0038--one-flux-ladder-with-voltage-names-as-aliases) | One Flux ladder, with voltage names as aliases | Superseded in part by ADR-0107 |
| [0039](#adr-0039--forge-1201-is-the-only-build-target) | Forge 1.20.1 is the only build target | Accepted |
| [0040](#adr-0040--running-out-of-band-costs-time-never-yield) | Running out of band costs time, never yield | Accepted |
| [0041](#adr-0041--a-condition-check-returns-a-named-fault-not-a-boolean) | A condition check returns a named fault, not a boolean | Accepted |
| [0042](#adr-0042--machines-subscribe-to-ticking-they-do-not-tick-by-default) | Machines subscribe to ticking; they do not tick by default | Accepted |
| [0043](#adr-0043--recipe-lookup-is-indexed-and-cached-never-a-linear-scan) | Recipe lookup is indexed and cached, never a linear scan | Accepted |
| [0044](#adr-0044--neighbour-lookups-are-cached-against-capability-invalidation) | Neighbour lookups are cached against capability invalidation | Accepted |
| [0045](#adr-0045--capabilities-are-attached-by-event-not-overridden-on-the-block-entity) | Capabilities are attached by event, not overridden on the block entity | Accepted |
| [0046](#adr-0046--the-pylon-index-is-chunk-bucketed-and-brownouts-resolve-in-two-phases) | The pylon index is chunk-bucketed, and brownouts resolve in two phases | Accepted |
| [0047](#adr-0047--veins-are-long-lived-and-deepening-competes-with-relocating) | Veins are long-lived, and deepening competes with relocating | Accepted |
| [0048](#adr-0048--assets-are-generated-from-the-jdk-with-a-named-list-of-what-cannot-be) | Assets are generated from the JDK, with a named list of what cannot be | Accepted |
| [0049](#adr-0049--a-green-build-must-include-a-booted-server) | A green build must include a booted server | Accepted |
| [0050](#adr-0050--grindless-supplies-a-material-only-where-the-pack-has-none) | Grindless supplies a material only where the pack has none | Accepted |
| [0051](#adr-0051--every-block-ships-a-floor-of-assets-generated-from-one-list) | Every block ships a floor of assets generated from one list | Accepted |
| [0052](#adr-0052--each-machine-has-its-own-shape-state-textures-and-effects) | Each machine has its own shape, state textures and effects | Accepted |
| [0053](#adr-0053--the-crude-extractor-is-the-first-consumer) | The Crude Extractor is the first consumer | Accepted |
| [0054](#adr-0054--pylons-are-three-blocks-tall-and-cover-a-factory) | Pylons are three blocks tall and cover a factory | Accepted |
| [0055](#adr-0055--the-multitool-does-not-mine) | The Multitool does not mine | Accepted |
| [0056](#adr-0056--t0-bootstrap-recipes-are-authored-json-using-tags) | T0 bootstrap recipes are authored JSON using tags | Accepted |
| [0057](#adr-0057--the-research-terminal-unlocks-world-scoped-blueprints) | The Research Terminal unlocks world-scoped blueprints | Superseded by ADR-0114 |
| [0058](#adr-0058--build-playable-slices-not-system-layers) | Build playable slices, not system layers | Accepted |
| [0059](#adr-0059--first-iron-is-a-generated-graph-and-a-voltaic-gate) | First iron is a generated graph and a Voltaic gate | Superseded in part by ADR-0114 |
| [0060](#adr-0060--first-factory-is-lane-data-a-survey-and-an-unpowered-belt) | First factory is lane data, a survey and an unpowered belt | Accepted |
| [0061](#adr-0061--the-machine-state-smoke-runs-as-one-function) | The machine-state smoke runs as one function | Accepted |
| [0062](#adr-0062--first-fluids-are-millibuckets-gravity-clay-and-a-named-co-sink) | First fluids are millibuckets, gravity clay and a named CO sink | Accepted |
| [0063](#adr-0063--the-factory-builds-the-factory-at-t1) | The factory builds the factory at T1 | Accepted |
| [0064](#adr-0064--energy-spanning-is-distance-and-storage-not-coverage) | Energy spanning is distance and storage, not coverage | Accepted |
| [0065](#adr-0065--t1-kiln-is-roast-and-so-not-the-acid-line) | T1 Kiln is roast and SO₂, not the acid line | Accepted |
| [0066](#adr-0066--the-t1-atlas-is-a-live-lookup-not-the-solver) | The T1 Atlas is a live lookup, not the solver | Superseded in part by ADR-0115 |
| [0067](#adr-0067--modular-armour-and-the-arc-reactor-are-one-tier) | Modular armour each tier; Arc Reactor is F3 factory and suit | Accepted |
| [0068](#adr-0068--horizon-gates-are-commute-infrastructure-not-mining-dimensions) | Horizon Gates are commute infrastructure, not mining dimensions | Superseded in part by ADR-0107 |
| [0069](#adr-0069--the-multitool-rotates-and-relocates-it-still-does-not-mine) | The Multitool rotates and relocates; it still does not mine | Accepted |
| [0070](#adr-0070--remaining-work-is-the-autonomous-build-out) | Remaining work is the autonomous build-out | Superseded in part by ADR-0088 |
| [0071](#adr-0071--t1-belt-junctions-are-merger-tunnel-and-overflow) | T1 belt junctions are merger, tunnel and overflow | Accepted |
| [0072](#adr-0072--the-t1-sorter-peels-it-does-not-split) | The T1 sorter peels; it does not split | Accepted |
| [0073](#adr-0073--industrial-is-the-second-blueprint-on-the-same-terminal) | Industrial is the second blueprint on the same terminal | Superseded by ADR-0114 |
| [0074](#adr-0074--the-wire-mill-is-t2-and-does-not-wait-for-acid) | The Wire Mill is T2 and does not wait for acid | Accepted |
| [0075](#adr-0075--the-contact-process-is-air-vanadia-and-a-pickle) | The contact process is air, vanadia and a pickle | Accepted |
| [0076](#adr-0076--the-washer-batch-is-eight-crushed-and-the-next-metal) | The washer batch is eight crushed and the next metal | Accepted |
| [0077](#adr-0077--water-splits-in-the-buffer-air-yields-oxygen-only) | Water splits in the buffer; air yields oxygen only | Accepted |
| [0078](#adr-0078--the-fluid-well-is-powered-chunk-water) | The Fluid Well is powered chunk water | Accepted |
| [0079](#adr-0079--melt-is-144-mb-at-1000-c-and-the-arc-still-makes-ingots) | Melt is 144 mB at 1000 °C, and the arc still makes ingots | Accepted |
| [0080](#adr-0080--flotation-is-an-integer-batch-and-the-magnet-splits-by-name) | Flotation is an integer batch, and the magnet splits by name | Accepted |
| [0081](#adr-0081--daylight-is-f1-and-steam-closes-on-the-condenser) | Daylight is F1, and steam closes on the condenser | Accepted |
| [0082](#adr-0082--t2-pipe-holds-steam-and-the-flux-belt-spends-lv) | T2 pipe holds steam, and the flux belt spends LV | Accepted |
| [0083](#adr-0083--a-controller-holds-a-machine-below-five-hundred) | A controller holds a machine below five hundred | Accepted |
| [0084](#adr-0084--the-flux-drill-spends-cells-not-durability) | The Flux Drill spends cells, not durability | Accepted |
| [0085](#adr-0085--a-blueprint-stamps-from-the-inventory) | A blueprint stamps from the inventory | Accepted |
| [0086](#adr-0086--the-planner-marks-and-does-not-pick-up) | The planner marks and does not pick up | Accepted |
| [0087](#adr-0087--a-task-indexed-route-map-updated-with-the-seam) | A task-indexed route map, updated with the seam | Accepted |
| [0088](#adr-0088--grindless-is-a-modpack-scale-progression) | Grindless is a modpack-scale progression | Superseded in part by ADR-0089, ADR-0090 and ADR-0107 |
| [0089](#adr-0089--external-code-may-enter-with-its-own-license) | External code may enter with its own license | Accepted |
| [0090](#adr-0090--electric-arc-steel-is-the-first-line-past-contact) | Electric-arc steel is the first line past contact | Accepted |
| [0091](#adr-0091--refractory-brick-is-one-slag-in-twenty-seconds) | Refractory brick is one slag in twenty seconds | Accepted |
| [0092](#adr-0092--metallurgical-silicon-runs-cold-on-sand-and-quartz) | Metallurgical silicon runs cold on sand and quartz | Accepted |
| [0093](#adr-0093--zone-refining-is-ten-metallurgical-silicon-in-six-hundred-seconds) | Zone refining is ten metallurgical silicon in six hundred seconds | Accepted |
| [0094](#adr-0094--the-first-megastructure-is-the-ground-array) | The first megastructure is the Ground Array | Superseded in part by ADR-0107 |
| [0095](#adr-0095--luna-is-the-first-original-planet) | Luna is the first original planet | Superseded in part by ADR-0107 |
| [0096](#adr-0096--the-first-interstellar-hop-is-the-drift) | The first interstellar hop is the Drift | Superseded in part by ADR-0107 |
| [0097](#adr-0097--local-trips-are-rocket-flights) | Local trips are rocket flights | Superseded in part by ADR-0107 |
| [0098](#adr-0098--the-supraluminal-station-is-the-ride-off-the-star) | The supraluminal station is the ride off the star | Superseded in part by ADR-0107 |
| [0099](#adr-0099--the-galactic-centre-is-a-sealed-chamber) | The galactic centre is a sealed chamber | Superseded in part by ADR-0107 |
| [0100](#adr-0100--the-quest-book-tracks-the-route-and-does-not-gate-it) | The quest book tracks the route and does not gate it | Superseded in part by ADR-0107 |
| [0101](#adr-0101--replication-cost-is-a-graph-walk-and-deconstruction-yields-one-matter) | Replication cost is a graph walk and deconstruction yields one Matter | Accepted |
| [0102](#adr-0102--the-voltaic-harness-has-one-slot-and-no-generator) | The Voltaic Harness has one slot and no generator | Accepted |
| [0103](#adr-0103--the-exosuit-taps-a-pylon-and-does-not-generate) | The exosuit taps a pylon and does not generate | Accepted |
| [0104](#adr-0104--parallel-stacks-reconcile-by-renumbering-the-smaller-reference-set) | Parallel stacks reconcile by renumbering the smaller reference set | Accepted |
| [0105](#adr-0105--smoke-scenarios-are-a-list-sharded-across-parallel-ci-jobs) | Smoke scenarios are a list, sharded across parallel CI jobs | Accepted |
| [0106](#adr-0106--code-is-written-remotely-it-is-validated-on-the-windows-desk) | Code is written remotely; it is validated on the Windows desk | Accepted |
| [0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices) | Sixteen aligned tiers grow through Control Matrices | Accepted |
| [0108](#adr-0108--finish-one-mechanical-tier-before-opening-the-next) | Finish one mechanical tier before opening the next | Accepted |
| [0109](#adr-0109--upstream-material-may-be-reused-under-its-own-licence) | Upstream material may be reused under its own licence | Accepted |
| [0110](#adr-0110--b2-pairs-materials-exactly-without-stack-nbt) | B2 pairs materials exactly without stack NBT | Accepted |
| [0111](#adr-0111--shared-world-smokes-are-the-fast-local-loop) | Shared-world smokes are the fast local loop | Accepted |
| [0112](#adr-0112--the-first-relay-matrix-is-a-four-unit-voltaic-hand-batch) | The first Relay Matrix is a four-unit Voltaic hand batch | Superseded in part by ADR-0114, ADR-0122 |
| [0113](#adr-0113--gregtech-is-the-primary-design-reference) | GregTech is the primary design reference | Accepted |
| [0114](#adr-0114--physical-components-replace-global-research-permissions) | Physical components replace global research permissions | Accepted |
| [0115](#adr-0115--the-atlas-shows-physical-bootstrap-routes) | The Atlas shows physical Bootstrap routes | Accepted |
| [0116](#adr-0116--bootstrap-has-an-explicit-survival-entry-and-exit) | Bootstrap has an explicit survival entry and exit | Accepted |
| [0117](#adr-0117--recurrent-base-materials-gain-industrial-renewal-routes) | Recurrent base materials gain industrial renewal routes | Accepted |
| [0118](#adr-0118--redstone-is-synthesised-from-renewable-silicon-and-water) | Redstone is synthesised from renewable silicon and water | Accepted |
| [0119](#adr-0119--relay-matrices-are-consumed-machine-components) | Relay Matrices are consumed machine components | Accepted |
| [0120](#adr-0120--the-first-client-pack-is-t0-scoped-and-jei-reads-the-live-graph) | The first client pack is T0-scoped and JEI reads the live graph | Accepted |
| [0121](#adr-0121--the-factory-world-is-clean-building-space-not-a-resource-shortcut) | The Factory World is clean building space, not a resource shortcut | Accepted |
| [0122](#adr-0122--the-seed-matrix-batch-funds-the-forming-press) | The seed Matrix batch funds the forming Press | Accepted |
| [0123](#adr-0123--the-voltaic-exit-is-a-rated-physical-f2-relay-matrix) | The Voltaic exit is a rated physical F2 Relay Matrix | Accepted |
| [0124](#adr-0124--atlas-rating-labels-do-not-change-recipe-identity) | Atlas rating labels do not change recipe identity | Accepted |
| [0125](#adr-0125--machine-inventories-persist-slots-with-explicit-legacy-migration) | Machine inventories persist slots with explicit legacy migration | Accepted |
| [0126](#adr-0126--integration-smokes-boot-the-distributable-forge-jar) | Integration smokes boot the distributable Forge jar | Accepted |
| [0127](#adr-0127--smoke-runner-dispatch-is-tested-independently-of-minecraft) | Smoke-runner dispatch is tested independently of Minecraft | Accepted |
| [0128](#adr-0128--validation-records-its-scope-and-repairs-incomplete-server-installations) | Validation records its scope and repairs incomplete server installations | Accepted |
| [0129](#adr-0129--bootstrap-grows-through-a-connected-ceramic-workshop) | Bootstrap grows through a connected ceramic workshop | Accepted |
| [0130](#adr-0130--settling-uses-an-open-oriented-assembly-with-stateless-parts) | Settling uses an open oriented assembly with stateless parts | Accepted |
| [0131](#adr-0131--mandatory-supply-is-mod-owned-renewable-and-explicitly-classified) | Mandatory supply is mod-owned, renewable and explicitly classified | Accepted |
| [0132](#adr-0132--fluid-output-is-owned-volume-not-an-unmarked-input-buffer) | Fluid output is owned volume, not an unmarked input buffer | Accepted |
| [0133](#adr-0133--bootstrap-closes-on-geology-biomass-and-primitive-assembly) | Bootstrap closes on geology, biomass and primitive assembly | Accepted |
| [0134](#adr-0134--astra-orchestrates-bounded-workers-after-an-isolated-cold-start) | Astra orchestrates bounded workers after an isolated cold start | Accepted |
| [0135](#adr-0135--sustainability-is-a-fixed-point-audit-over-live-authorities-and-typed-annotations) | Sustainability is a fixed-point audit over live authorities and typed annotations | Accepted |
| [0136](#adr-0136--c3-integration-keeps-the-two-iron-budget-and-leaves-vanilla-tags-untouched) | C3 integration keeps the two-iron budget and leaves vanilla tags untouched | Accepted |
| [0137](#adr-0137--the-t0-relay-matrix-exit-stays-a-hand-craft-the-bench-does-not-fabricate-it) | The T0 Relay Matrix exit stays a hand craft; the Bench does not fabricate it | Accepted |
| [0138](#adr-0138--a-batch-completes-only-after-its-fluid-inputs-are-re-verified-and-drawn-exactly) | A batch completes only after its fluid inputs are re-verified and drawn exactly | Accepted |
| [0139](#adr-0139--long-native-smokes-carry-their-own-assertion-budget) | Long native smokes carry their own assertion budget | Accepted |
| [0140](#adr-0140--validate-behavioral-classes-and-reuse-proof-under-explicit-assumptions) | Validate behavioral classes and reuse proof under explicit assumptions | Accepted |
| [0141](#adr-0141--canonical-t0-closure-uses-real-authorities-and-conditional-establishment) | Canonical T0 closure uses real authorities and conditional establishment | Accepted |
| [0142](#adr-0142--t0-client-feedback-reuses-native-ui-and-existing-quest-infrastructure) | T0 client feedback reuses native UI and existing quest infrastructure | Accepted |
| [0143](#adr-0143--player-resource-guidance-is-a-generated-view-of-canonical-declarations) | Player resource guidance is a generated view of canonical declarations | Accepted |
| [0144](#adr-0144--ship-superflat-first-playable-tier-releases-with-longitudinal-save-continuity) | Ship Superflat-first playable tier releases with longitudinal save continuity | Accepted |
| [0145](#adr-0145--calibrate-a-small-shared-sound-palette-with-development-only-generation) | Calibrate a small shared sound palette with development-only generation | Accepted |
| [0146](#adr-0146--give-items-material-volume-and-reuse-block-models-for-machine-items) | Give items material volume and reuse block models for machine items | Accepted |
| [0147](#adr-0147--transfer-clean-session-coordination-to-sol-61-without-replaying-evidence) | Transfer clean-session coordination to Sol 6.1 without replaying evidence | Accepted |
| [0148](#adr-0148--integrate-the-approved-quern-sound-with-one-native-client-loop) | Integrate the approved Quern sound with one native client loop | Accepted |
| [0149](#adr-0149--add-a-restricted-playable-profile-with-explicit-public-overrides) | Add a restricted playable profile with explicit public overrides | Accepted |
| [0150](#adr-0150--superflat-retains-virtual-deposits-without-staging-the-terrain) | Superflat retains virtual deposits without staging the terrain | Accepted |
| [0151](#adr-0151--keep-coordination-local-and-execute-through-the-existing-personal-pc-ssh) | Keep coordination local and execute through the existing personal-PC SSH | Accepted |
| [0152](#adr-0152--run-bounded-autonomous-t0-and-creative-work-without-human-acceptance) | Run bounded autonomous T0 and creative work without human acceptance | Accepted |
| [0153](#adr-0153--rebind-generated-recipes-at-atomic-graph-boundaries) | Rebind generated recipes at atomic graph boundaries | Accepted |
| [0154](#adr-0154--stage-one-disabled-optional-pack-for-reload-smokes) | Stage one disabled optional pack for reload smokes | Accepted |
| [0155](#adr-0155--retain-a-diagnostic-smoke-world-only-by-explicit-opt-in) | Retain a diagnostic smoke world only by explicit opt-in | Accepted |
| [0156](#adr-0156--disclose-blockers-and-separate-creative-capability-from-product-acceptance) | Disclose blockers and separate creative capability from product acceptance | Accepted |
| [0157](#adr-0157--prefer-the-existing-local-comfyui-as-a-supervised-asset-backend) | Prefer the existing local ComfyUI as a supervised asset backend | Accepted |
| [0158](#adr-0158--delegate-end-to-end-outcomes-with-proportional-validation) | Delegate end-to-end outcomes with proportional validation | Accepted |
| [0159](#adr-0159--start-t0-with-one-free-generous-starter-quest) | Start T0 with one free generous starter quest | Accepted |
| [0160](#adr-0160--merge-the-testing-ready-t0-candidate-and-close-superseded-prs) | Merge the testing-ready T0 candidate and close superseded PRs | Accepted |
| [0161](#adr-0161--prepare-the-t0-experience-before-longitudinal-human-play) | Prepare the T0 experience before longitudinal human play | Accepted |
| [0162](#adr-0162--quests-own-t0-teaching-and-the-per-player-f1-transition) | Quests own T0 teaching and the per-player F1 transition | Accepted |
| [0163](#adr-0163--use-physical-barter-and-local-evidence-before-economic-rebalancing) | Use physical barter and local evidence before economic rebalancing | Accepted |
| [0164](#adr-0164--extend-native-tank-custody-and-bounded-controller-backed-t0-ports) | Extend native tank custody and bounded controller-backed T0 ports | Accepted |
| [0165](#adr-0165--provide-original-optional-inspection-without-a-new-pack-dependency) | Provide original optional inspection without a new pack dependency | Accepted |
| [0166](#adr-0166--retire-synthesized-distribution-with-physical-families-and-intentional-silence) | Retire synthesized distribution with physical families and intentional silence | Accepted |
| [0167](#adr-0167--make-state-and-port-art-readable-without-color-or-normal-build-generation-services) | Make state and port art readable without color or normal-build generation services | Accepted |
| [0168](#adr-0168--develop-t1-in-parallel-with-owner-t0-testing-without-changing-the-dev-game) | Develop T1 in parallel with owner T0 testing without changing the dev game | Accepted |
| [0169](#adr-0169--make-the-first-t1-factory-a-live-source-contract-not-another-execution-engine) | Make the first T1 factory a live source contract, not another execution engine | Accepted |
| [0170](#adr-0170--develop-the-next-tier-alongside-longitudinal-play-and-prioritize-player-blockers) | Develop the next tier alongside longitudinal play and prioritize player blockers | Accepted |
| [0171](#adr-0171--replace-the-player-blocking-book-with-native-ftb-quests-and-concise-jei-first-content) | Replace the player-blocking book with native FTB Quests and concise JEI-first content | Accepted |
| [0172](#adr-0172--automate-recurrent-t1-casings-and-flux-cells-through-the-existing-assembler) | Automate recurrent T1 casings and Flux Cells through the existing Assembler | Accepted |
| [0173](#adr-0173--develop-later-tiers-in-parallel-with-protected-t0-play) | Develop later tiers in parallel with protected T0 play | Accepted |
| [0174](#adr-0174--grow-tier-complexity-around-basic-machine-families-from-t1) | Grow tier complexity around basic machine families from T1 | Accepted |
| [0175](#adr-0175--carry-optional-absolute-process-pressure-through-existing-contracts) | Carry optional absolute process pressure through existing contracts | Accepted engineering contract; model/native gates separate |
| [0176](#adr-0176--distinguish-exact-matrix-evidence-from-minimum-controller-capacity) | Distinguish exact Matrix evidence from minimum controller capacity | Accepted engineering scope; source integrated, qualification pending |
| [0177](#adr-0177--update-managed-native-quest-definitions-only-through-an-explicit-guarded-transition) | Update managed native quest definitions only through an explicit guarded transition | Accepted engineering scope; source/P2 repair integrated, JVM proof pending |
| [0178](#adr-0178--continue-autonomously-and-deliver-ready-tiers-to-the-longitudinal-game) | Continue autonomously and deliver ready tiers to the longitudinal game | Accepted direct owner operating mandate |
| [0179](#adr-0179--reproduce-four-slot-fit-first-factory-machines-without-repricing-bootstrap) | Reproduce four-slot-fit first-factory machines without repricing bootstrap | Accepted bounded acquisition scope; implementation/proof pending |
| [0180](#adr-0180--reproduce-the-thermal-generator-through-an-honest-two-stage-subassembly) | Reproduce the Thermal Generator through an honest two-stage subassembly | Accepted bounded acquisition scope; implementation/proof pending |
| [0181](#adr-0181--compare-one-generated-platform-shim-by-exact-self-name-equivalence) | Compare one generated platform shim by exact self-name equivalence | Source555dbe9 integrated; upstream/path corroborated, execution pending |

---

## ADR-0001 — Architectury for multiloader support

*2026-09-29 · Superseded by [ADR-0039](#adr-0039--forge-1201-is-the-only-build-target)*

> **Superseded in part.** The multiloader *goal* is withdrawn — Forge 1.20.1 is now the only build
> target. The *structure* this record chose survives: Architectury, a `common/` module holding
> essentially all logic, and `@ExpectPlatform` for loader-specific code. See ADR-0039 for why
> keeping the structure while dropping the second loader is the cheap option rather than a
> contradiction.

**Context.** Grindless has to reach the largest possible modpack audience, which means Fabric,
Forge and NeoForge, across several Minecraft versions. Maintaining parallel codebases per loader
is the usual way such mods die.

**Decision.** Use Architectury with a `common/` module holding essentially all logic, and thin
`fabric/` and `forge/` modules containing only entrypoints and platform bridges. Platform
differences go through `@ExpectPlatform` stubs. Shadow bundles `common` into each platform jar.

**Alternatives rejected.** Separate per-loader repositories (duplicated logic); Forge-only (halves
the audience); a source-templating tool such as Preprocessor (worse IDE experience than a real
multi-module build).

**Consequences.** Ports to new Minecraft versions become mapping and registry churn rather than
rewrites. The cost is that `common/` must stay disciplined: one loader-specific import outside an
`@ExpectPlatform` stub breaks the other platform, usually at runtime rather than compile time.

---

## ADR-0002 — No `neoforge` subproject on 1.20.1

*2026-09-29 · Accepted*

**Context.** NeoForge is normally a distinct build target, so the obvious structure is
`common` + `fabric` + `forge` + `neoforge`. On 1.20.1 that is wrong.

**Decision.** Ship only `fabric` and `forge` on 1.20.1. The Forge jar is also the NeoForge
artifact.

Verified directly against `https://maven.neoforged.net/releases/net/neoforged/`: the modern
`net.neoforged:neoforge` coordinate begins at `20.2.x` — Minecraft 1.20.2 — and has nothing for
1.20.1. NeoForge's 1.20.1 release is `net.neoforged:forge:1.20.1-47.1.x`, a soft-fork of Forge 47
that keeps the `net.minecraftforge` package names and the `META-INF/mods.toml` metadata format.
One Forge-compiled binary therefore loads on both loaders unmodified.

**Consequences.** One less subproject to build, test and publish on 1.20.1. A real `neoforge`
module arrives with the 1.20.2+ port, where both the artifact and Architectury's `neoforge()`
target exist. The `mods.toml` loader range is kept at `[47,)` so the same file satisfies both.

---

## ADR-0003 — Mojang mappings layered with Parchment

*2026-09-29 · Accepted*

**Context.** 1.20.1 mods use either MCP/SRG-era mappings, Yarn, or official Mojang mappings.
A multiloader project needs one mapping set that works on both loaders.

**Decision.** Official Mojang mappings (`officialMojangMappings()`) layered with Parchment
`2023.09.03`, which adds parameter names and javadoc that Mojmap omits.

**Alternatives rejected.** Yarn is Fabric-oriented and awkward on Forge; MCP is legacy.

**Consequences.** Readable, permissively licensed names that are standard on 1.20.1, so code
matches what most 1.20.1 tutorials and other mods' sources show. `silentMojangMappingsLicense()`
is set in the root build to avoid the interactive licence prompt.

---

## ADR-0004 — The material registry is built from tags at runtime

*2026-09-29 · Accepted*

**Context.** The core promise is that Grindless processes *every* material in whatever pack it is
installed into, including mods it has never heard of. A hardcoded material list cannot do that,
and per-pack compat addons are the failure mode of every mod that has tried.

**Decision.** At runtime, read the tags actually present in the loaded pack — `forge:ores/*`,
`forge:ingots/*`, `forge:raw_materials/*`, `forge:dusts/*`, `forge:gems/*` and the Fabric `c:`
conventions — and build the material registry and the whole processing chain from what is found.
Rebuild on datapack reload so pack changes apply without a restart.

**Consequences.** New mods are supported with no patch release and no configuration. Materials
degrade gracefully: a material with an ore but no dust form simply skips the pulverizing step, and
where another mod already provides an item for a material, Grindless uses that item instead of
registering a duplicate. The cost is that nothing about the material set can be assumed at compile
time — every system downstream must be written against a registry discovered at runtime.

---

## ADR-0005 — Recipes are generated at runtime, not shipped as JSON

*2026-09-29 · Accepted*

**Context.** A consequence of ADR-0004: if materials are unknown until the pack loads, their
recipes cannot be authored in advance.

**Decision.** Generate the processing-chain recipes at runtime from the discovered material
registry.

**Consequences.** One implementation covers every mod, present and future, with no shipped recipe
JSON to maintain. Recipe viewers must be fed the generated recipes explicitly, and any bug in
generation affects every material at once rather than one file.

---

## ADR-0006 — Flux Units convert to FE at 1:1 over a hand-written bridge

*2026-09-29 · Accepted*

**Context.** Grindless has its own energy unit, Flux Units, but must interoperate with the pack's
existing energy economy — Forge Energy on Forge, TeamReborn Energy on Fabric. Architectury
provides no unified energy API, so the bridge has to be written by hand either way.

**Decision.** FU converts to FE at exactly 1:1, with a hand-written bridge per platform
(capabilities on Forge, TeamReborn Energy `5.0.0` on Fabric).

**Alternatives rejected.** A non-unit conversion ratio, which accumulates rounding drift across
millions of ticks and turns energy storage into a slow leak; adopting FE directly, which would
give up Grindless-specific tier semantics.

**Consequences.** Conversion is provably lossless and needs no rounding policy. Note that
TeamReborn Energy is hosted on `maven.fabricmc.net`, *not* on `maven.terraformersmc.com` — the
wrong repository here is a confusing build failure.

> **Narrowed by [ADR-0039](#adr-0039--forge-1201-is-the-only-build-target).** The 1:1 FU↔FE rate
> and the hand-written bridge stand exactly as decided. Only the Fabric half is withdrawn: with
> Forge as the sole target there is one bridge, over Forge capabilities, and the TeamReborn
> dependency and its repository caveat no longer apply.

---

## ADR-0007 — Flux Network state lives in `SavedData` with a spatial index

*2026-09-29 · Accepted*

**Context.** The Flux Network is coverage-area power distribution: pylons project a supply area
and machines inside it draw power without cables. The naive implementation stores network
membership on block entities and walks the graph each tick.

**Decision.** Keep network state in a level-wide `SavedData`, indexed spatially over pylons.

**Consequences.** No per-tick graph walk, and the network survives chunk unloads intact —
block-entity-owned state would fragment the moment a chunk containing part of a network unloads.
Coverage lookup is `O(log n)` in the number of pylons rather than `O(n)` in blocks. The cost is
that `SavedData` and world blocks can drift out of sync, so pylon placement and removal must
maintain the index carefully.

---

## ADR-0008 — Belt contents are lane data, not entities

*2026-09-29 · Accepted*

**Context.** Belts are the headline logistics feature and the most likely source of tick lag at
scale.

**Decision.** Represent items on belts as positions in lane data structures, never as one
`ItemEntity` per item.

**Consequences.** Avoids the standard belt-mod performance disaster, where a few thousand items in
transit produce a few thousand ticking entities. Rendering must draw items from lane data
directly, and items must be materialised into real stacks only at belt endpoints.

---

## ADR-0009 — Ore veins are derived from seed and coordinates

*2026-09-29 · Accepted*

**Context.** Resource Genesis gives each chunk its own finite materials, which deplete and push
the player outward. Storing per-chunk vein contents for an effectively unbounded world is a
storage problem.

**Decision.** Derive vein composition deterministically from the world seed plus chunk
coordinates. Persist only what has actually been extracted.

**Consequences.** No stored world data until a vein is used, identical results across reloads, and
the cost of an unexplored chunk is zero. Changing the derivation function after release would
silently alter every unexploited chunk in every existing world, so it must be versioned.

---

## ADR-0010 — Replication cost is derived from the recipe graph

*2026-09-29 · Accepted*

**Context.** Matter Replication produces items that are not ores. Duplication mods that price
items with a flat cost are trivially exploitable: find the item whose crafting inputs are worth
less than its flat replication price and loop it.

**Decision.** Price replication by walking the item's recipe graph down to primitives, so an item
always costs at least what its inputs cost.

**Consequences.** Closes the arbitrage loop generically, for modded items the mod has never seen.
Requires recipe-graph traversal with cycle detection and a sane fallback for items that have no
recipe at all.

---

## ADR-0011 — Remote colonies are simulated abstractly, never force-loaded

*2026-09-29 · Accepted*

**Context.** The planetary layer lets players run industry on worlds they are not standing on.
The obvious implementation force-loads the remote chunks.

**Decision.** Model remote colonies as abstract state machines producing output over time. Never
force-load their chunks.

**Consequences.** Force-loading remote bases is how servers die; abstract simulation lets the mod
*encourage* interplanetary industry instead of quietly discouraging it through lag. Colony
behaviour must therefore be expressible as a rate-based state machine, and visiting a colony in
person has to reconcile abstract state with real blocks.

---

## ADR-0012 — Planets come from installed space mods, and satellites have no upkeep

*2026-09-29 · Superseded in part by [ADR-0088](#adr-0088--grindless-is-a-modpack-scale-progression)*

> **Superseded in part.** The planet source is reversed: Grindless ships original space,
> interstellar play, and planets with unique extractable resources. Satellites still have no
> upkeep. See ADR-0088.

**Context.** Packs that include an orbital layer usually already have a space mod with its own
dimensions. Shipping a competing set of planets fragments the pack.

**Decision.** Detect planets from the space mods actually installed and integrate with them rather
than registering a rival dimension set. Give satellites no maintenance cost — they are paid for in
launch mass and power, once.

**Consequences.** Adding a competing dimension set to a pack that already has one is worse than
adding nothing, so integration keeps Grindless welcome in existing packs. Orbital upkeep would be
a recurring chore, and chores are precisely what this mod exists to delete. The cost is a detection
and mapping layer per supported space mod, plus a sensible fallback when none is installed.

---

## ADR-0013 — The README is the design source of truth

*2026-09-30 · Accepted*

**Context.** The session that wrote the design could not create a `docs/` directory, so the entire
design went into a single very large README. That constraint is gone, but the file now exists and
is coherent.

**Decision.** Keep `README.md` as the authoritative design document and split it into `docs/`
progressively, as each area is implemented. `docs/DESIGN.md` is an earlier standalone note,
superseded by the README and kept only for history.

**Consequences.** One place to look for design, at the cost of a large file. Where the README and
`docs/DESIGN.md` disagree, the README wins. Splitting must move content, never copy it, or the two
copies will diverge.

---

## ADR-0014 — Consolidate the orphaned session branches into one history

*2026-09-30 · Accepted*

**Context.** Two assistant sessions each produced substantial work and each lost its terminal
before committing anything. Their worktrees held complementary and partly conflicting trees:
`ezequiel24123z-fantastic-parakeet` had the multiloader skeleton, the common entrypoint and
`docs/DESIGN.md`; `ezequiel24123z-grindless-mod` had the full README design document, `.gitignore`,
a refined root Gradle setup, and two workaround files written to commit without a working shell.

**Decision.** Consolidate both into a single commit on one branch, taking the newer branch's root
Gradle configuration and the older branch's subproject scripts and sources.

Specifics worth recording, because they are invisible in the result:

- The subproject scripts referenced `rootProject.architectury_version` and
  `rootProject.neoforge_loader_range`; the newer `gradle.properties` named the first
  `architectury_api_version` and omitted the second. Unified on `architectury_api_version` and
  added the missing key, rather than keeping two aliases for one version.
- `COMMIT.bat` and `README.new.md` were excluded. Both existed only to work around the broken
  terminal, and `README.new.md` was explicitly marked a stale scratch file by its own author.
  `SETUP.ps1` was kept, because its bootstrap work is genuinely useful.
- The README's *Current blocker* section was rewritten as *Environment setup*: it described a
  broken terminal as the project's blocker and pointed at the now-removed `COMMIT.bat`.

**Consequences.** One branch carries the whole project and the orphaned branches can be discarded.
`Grindless.java` references registry classes that do not exist yet, so the committed tree does not
compile — this was accepted deliberately, because preserving the real work matters more than a
green build, and the state is documented in the README and the commit message.

This episode is the direct cause of the commit-early rule in [`AGENTS.md`](../AGENTS.md).

---

## ADR-0015 — Fluids are modelled as state, not as items

*2026-09-30 · Accepted*

**Context.** Minecraft tech mods almost universally model a fluid as an item with a different
texture: an amount in a tank, moved by a pipe at a fixed rate. It is simple to implement, it
integrates trivially with existing fluid APIs, and it produces no interesting decisions — a fluid
network is solved once and then copy-pasted forever.

**Decision.** A Grindless fluid stack carries **volume, temperature and pressure**. The same
substance at different points on that curve is a different resource: steam, superheated steam and
supercritical water drive the same turbine at very different outputs. Heat is conserved rather
than ignored, so steam that cools condenses in the pipe and a turbine fed condensate stalls.
Throughput is a function of pipe tier, pressure differential and viscosity; gases need a pump,
liquids can run downhill for free.

**Alternatives rejected.** Flat fluid-as-item, for the reasons above. A full thermodynamic
simulation, which is unplayable and unaffordable at tick rate — the model is deliberately three
numbers and a phase table, not a solver.

**Consequences.** Plumbing becomes a layout puzzle with the same shape as a belt bus: feed, boil,
work, condense, return. It also gives the futuristic tier somewhere physical to go — cryogenics,
supercritical loops, plasma containment — instead of inventing arbitrary new units.

The costs are real. Every fluid-carrying block has to track and display three values rather than
one, interop with the pack's plain fluid API has to pick a sane temperature and pressure for
foreign fluids entering the system, and the UI must make state legible or the whole thing reads as
unexplained failure. Rupture is therefore a loud, visible, repairable vent rather than an
explosion: the model must be safe to learn by experiment.

---

## ADR-0016 — Pipes early, the Phase Network late

*2026-09-30 · Accepted*

**Context.** The mod's founding idea is coverage areas instead of cable spaghetti (ADR-0001's
sibling in design terms): a Flux Pylon powers everything inside its area, so nobody runs wire.
Applying that to fluids immediately would delete the pipe gameplay that ADR-0015 just created.
Never applying it would leave fluids as the one system that stays tedious forever.

**Decision.** Both, in sequence. Real pipes with real pressure carry the early and middle game. At
T3 the **Phase Manifold** arrives: inside its coverage area, registered tanks and machines exchange
any fluid the network holds with no pipes at all.

The phase network is explicitly **not** a strict upgrade. Dematerialising a fluid costs FU per
unit, proportional to distance from ambient, so cryogenics and plasma are expensive to move and a
well-built pipe loop stays cheaper forever.

**Consequences.** This is the same trade the mod already offers between belts and drones: pay in
layout, or pay in power. Players who enjoy plumbing keep plumbing and are rewarded with lower
running costs; players who are finished with plumbing can buy their way out. The transition is
never forced, and no content is invalidated at T3.

The risk to watch in balancing is the phase network's FU price. Too cheap and pipes become
vestigial, taking the ADR-0015 gameplay with them; too expensive and it is a trap option nobody
builds.

---

## ADR-0017 — Machines above T1 are manufactured, never hand-crafted

*2026-09-30 · Accepted*

**Context.** In Factorio the factory builds the factory: you hand-craft the first burner drill and
essentially nothing else. Minecraft tech mods usually discard that loop by letting a player with a
full inventory assemble any machine in a crafting grid, which makes the factory decorative — a
thing that produces items rather than the thing that produces itself.

Grindless has a specific exposure here. It is designed to be installed into large existing packs,
where a player may arrive already holding the pack's mid-game materials.

**Decision.** Past the bootstrap, machines have **no crafting-table recipe at all** — not a hidden
one, not a deliberately expensive one. T2–T3 machines are produced by the **Assembler**, T4 by the
**Quantum Assembler**, T5–T6 by the **Orbital Assembly Bay**. Each consumes a researched blueprint,
fabricated components, power and time.

Components form the intermediate economy — casings, motors, pumps, circuit boards, integrated
circuits, superconductors, quantum cores — and several of them require fluids, so circuits need
etching acid and machines need circuits.

**Consequences.** The reward for building a production line becomes the ability to build the next
one, which is the loop the mod exists to deliver. Research gains teeth: a blueprint is a production
target rather than a note. Fluids become load-bearing exactly once, early, at small scale, rather
than being a system players can ignore entirely.

The danger is obvious — this rule could easily become the grind the mod exists to delete — so the
guard rails are part of the decision, not an afterthought: blueprints are permanent, Assemblers are
cheap and parallelise so the answer to "this is slow" is always "build another one", T0 and T1 stay
hand-craftable forever so there is no softlock path, and the gate is datapack-driven so a pack
author can relax it without a mod patch.

---

## ADR-0018 — One container contract, with auto-void off by default

*2026-09-30 · Accepted*

**Context.** Grindless will ship many storage blocks — logistics crates, four tiers of tank, and
the input and output buffers inside every machine. Tech mods typically give each its own ad-hoc
interface, so the player learns filtering four times and still cannot predict which blocks support
it.

Separately, a system that generates recipes from tags at runtime (ADR-0004, ADR-0005) produces
byproducts for materials the player has never heard of. Unwanted byproducts backing up a line are
the characteristic failure mode of that design, so a discard mechanism is not optional.

**Decision.** Every container — item or fluid, block or machine buffer — implements one shared
contract: per-slot filters that survive the slot emptying, a buffer target, a capacity limit below
physical maximum, configurable auto-void with a threshold and mode, per-face I/O, insertion and
extraction priority, and a fill-level signal output.

Auto-void is built defensively, because it is simultaneously the feature that keeps a base running
unattended and the easiest way for a player to silently destroy something they wanted:

- off by default on every container, always;
- enabling it requires an explicit confirmation;
- a voiding container is visibly marked with particles and a glow, so it is discoverable months
  later in a base you no longer remember building;
- it trims above the threshold and never empties a container;
- it emits a distinct signal while voiding, so alarms can be built;
- it refuses to discard anything on the replication blacklist — creative items, quest rewards,
  pack-unique items.

**Consequences.** One interface to learn and one implementation to maintain, at the cost of a
richer base container class than most blocks strictly need. The defensive defaults mean a player
cannot lose materials to a feature they did not know was on, which is the failure this design is
most exposed to.

The canonical overflow pattern becomes `Overflow Gate` into a voiding `Storage Crate` — the
standard answer to eleven thousand gravel.

---

## ADR-0019 — Design machines before recipes

*2026-10-01 · Accepted*

**Context.** The natural instinct is to design content item-first: decide what the player makes,
then add a machine for each process. Large tech mods that work this way accumulate hundreds of
blocks, most of which host a single recipe type, and the catalogue grows without the gameplay
getting deeper.

**Decision.** Specify the machine layer completely first — what machines exist, what they can do,
how they scale, how multiblocks behave — and only then specify the recipe graph, the item and fluid
catalogue and the concrete routes. The machine design lives in [`MACHINES.md`](MACHINES.md), the
recipe design in `PROCESSES.md`.

Machines are specified as **capabilities**, not as recipe holders, which is what makes the ordering
pay off: see ADR-0020.

**Consequences.** A small machine set can host an enormous recipe space, and adding content later
means adding recipes rather than blocks. The risk is designing a capability nothing ends up needing,
which is mitigated by every machine in `MACHINES.md` naming the processes that motivate it.

---

## ADR-0020 — Recipes are parameterised by process conditions

*2026-10-01 · Accepted*

**Context.** In essentially every Minecraft tech mod a recipe is `inputs → outputs` bound to a
machine type. The machine *is* the process. This makes each recipe a lookup with one answer, and it
forces a new machine for every new kind of transformation.

**Decision.** A Grindless recipe is `inputs + conditions + time → outputs`, where conditions are
physical parameters: temperature, pressure, atmosphere, catalyst, field, agitation. The same inputs
under different conditions produce different outputs.

A machine is then defined by the **condition envelope** it can maintain, and any recipe whose
conditions fall inside an envelope runs in that machine. Efficiency is continuous: inside the
optimal band is full yield, at the edges yield drops or byproducts appear, outside the band the
process fails visibly and informatively.

**Alternatives rejected.** Fixed recipes per machine — simple, and the thing that makes the genre
shallow. A full thermodynamic simulation — unaffordable at tick rate and unplayable.

**Consequences.** This single decision delivers several goals at once: multiple genuine routes to
the same product, a standing optimisation problem that changes as the factory grows, a reason for
the fluid system to carry temperature and pressure (ADR-0015), and a recipe space that is the
*product* of machines and conditions rather than a list.

The costs are real and concentrated in presentation. Recipe lookup is no longer "which machine" but
"which conditions", which is why the route viewer is a first-class feature rather than an
integration (ADR-0023). Out-of-band failure must teach rather than merely fail. And the number of
condition dimensions the UI can carry before configuration becomes work is still an open question —
recorded as such in `MACHINES.md`.

---

## ADR-0021 — Complexity beyond GregTech, with no grind

*2026-10-01 · Superseded in part by [ADR-0088](#adr-0088--grindless-is-a-modpack-scale-progression)*

> **Superseded in part.** The project is no longer a short mod that refuses length. A repeated
> chore is still not a new tier: solving the same problem again by hand is not content. Stopping
> the ladder at T6 because more tiers would take time is retired. See ADR-0088.

**Context.** The brief is a mod deeper than GregTech that nonetheless contains no grind. Those
sound contradictory, because the genre's depth is habitually sold with repetition attached.

**Decision.** Treat complexity and grind as the orthogonal things they are. **Complexity** is the
number of meaningful decisions and how they interact. **Grind** is repeating an action whose outcome
is already known. Grindless maximises the first and refuses the second, under one rule:

> If the player has already solved a problem, never ask them to solve it again by hand. Asking them
> to solve it at a different scale is fair — that is a new problem.

Concretely, depth comes from the recipe graph, condition tuning, route selection and ratio
balancing; and the usual sources of repetition are removed outright. Hand-crafting intermediates is
replaced by fabrication (ADR-0017); per-material ore lines are replaced by the tag-driven registry
(ADR-0004); re-tiering is an in-place upgrade; machine configuration is copied with Process Cards;
and there is no maintenance mechanic at all, because chores are not content.

Scaling is given three axes with different shapes — parallel, overclock, multiblock — where
overclocking is deliberately the *worst* option and never required. In GregTech overclocking is
mandatory and therefore not a decision; making it usually-wrong turns it back into one.

**Consequences.** The mod can be very deep without being long. The risk to watch is that removing
repetition also removes pacing: if everything is instant, tiers blur. The counterweight is that
throughput still has to be built — post-scarcity in materials is never post-scarcity in throughput.

---

## ADR-0022 — Parametric multiblocks instead of fixed schematics

*2026-10-01 · Accepted*

**Context.** The standard multiblock is a fixed schematic: build this exact arrangement and receive
this exact machine. The player looks it up once and copies it forever, which makes a headline
feature into a chore with extra steps.

**Decision.** Grindless multiblocks are parametric. The player chooses dimensions and internal
arrangement; behaviour follows from what was built. Reactor output and heat depend on fuel rod
adjacency and coolant channel layout; distillation tower height sets how many fractions separate;
electrolysis electrode area sets current; accelerator ring circumference sets reachable particle
energy and therefore which transmutations exist at all; fusion coil count and containment strength
set which fuel cycles can ignite.

The reward for a multiblock is **better ratios and new capabilities**, not merely more throughput —
a bigger reactor loses proportionally less heat, a taller column makes fractions a short one cannot.

**Consequences.** Understanding beats copying, and there is no single correct schematic to look up.
Failure modes are deliberately recoverable: an over-hot reactor SCRAMs and needs a restart cycle
rather than detonating, because losing hours of progress to one mistake is not depth.

The cost is validation and feedback complexity — the game must explain *why* a given core design
underperforms, or the design space reads as noise.

---

## ADR-0023 — A native route viewer with a ratio solver

*2026-10-01 · Accepted*

**Context.** Parameterised recipes (ADR-0020) and multiple routes per product (`MACHINES.md`) make
the recipe graph unreadable through a conventional recipe viewer, which answers "what makes this"
with a flat list and cannot express conditions, routes or ratios.

**Decision.** Ship the **Process Atlas** in the mod itself, working with no other mod installed. It
shows every route to a product as a graph, with ratios per unit time at a chosen tier, the
conditions each route needs, a cost overlay in FU and machine count, and reachability marking which
routes are buildable now and naming what is missing from the others. Given a target rate it solves
the line: how many of each machine, at which tier, in what ratio.

JEI, REI and EMI all get full integration on top, with the Atlas reachable from a recipe screen.

**Alternatives rejected.** Relying on JEI alone — it cannot express condition ranges or compare
routes, and a pack that ships no recipe viewer would make the mod unplayable.

**Consequences.** The solver deliberately removes the arithmetic, on the position that arithmetic
is not gameplay while building, placing and feeding a line is. This is the decision most at risk of
going too far: if "press solve, build exactly that" becomes the entire loop, the solver should
propose rather than prescribe. Recorded as an open question in `MACHINES.md`.

---

## ADR-0024 — `loom.platform` markers and the Windows path length limit

*2026-10-01 · Accepted*

**Context.** Two environment problems surfaced on the first real build attempt, and both are the
kind that look like something else entirely.

**Architectury Loom decides a subproject's platform from a `loom.platform` property in that
subproject's own `gradle.properties`.** Without it the module defaults to Fabric, so `forge`
configured as a Fabric project and `dependencies { forge "net.minecraftforge:forge:..." }` failed
with `Could not find method forge()` — which reads as a broken dependency rather than a missing
marker. Both platform modules now carry the file.

**The checkout path is 181 characters**, and the deepest source file reaches 268 — past the Windows
`MAX_PATH` limit of 260. `LongPathsEnabled` is `0` on this machine and setting it needs
administrator rights plus a reboot. Git failed with `Filename too long` when staging.

**Decision.** Commit a one-line `gradle.properties` in each platform module. For path length, set
`core.longpaths = true` in the repository's git config, which makes Git for Windows use the Unicode
path APIs and needs no elevation.

**Alternatives rejected.** Enabling `LongPathsEnabled` system-wide, which needs admin on a machine
where the user is not one; shortening the package name, which would be contorting the project
around a tooling limit; relocating the checkout, which fights the tool that created the worktree.

**Consequences.** Both problems are fixed without elevation. `core.longpaths` is local to the
repository, so a fresh clone must set it again — `SETUP.ps1` should do this, and its `MAX_PATH`
warning is now known to be a real failure on this machine rather than a theoretical one.

Note that the JDK and Gradle are *not* protected by this setting. Nothing has broken so far, but a
build failure that looks like a corrupt file should be treated as a path-length problem first.

---

## ADR-0025 — Universal conduits that do not obsolete belts or pipes

*2026-10-01 · Accepted*

**Context.** By mid-game a base runs four parallel infrastructures that all solve the same problem:
belts for items, pipes for fluids, signal cable for logic, and a steam loop used only to move heat
somewhere. Routing them around each other is busywork. EnderIO's conduits are the genre's answer
and they are the right idea.

The danger is specific and severe: belts are the Factorio soul of this mod, and a universal conduit
that is strictly better deletes them. That is exactly the trap ADR-0016 avoided for fluids.

**Decision.** Ship **Flux Conduits** from T2: one chassis block whose carried types are decided by
**cores** inserted into it — item, fluid, heat, signal and (rarely) Flux. Several cores coexist in
one block, so one run carries ore, coolant, heat and the logic controlling them.

Belts and pipes survive because conduits are deliberately not strictly better:

- **Item cores draw power per item moved; belts move bulk for free.** For the highest-volume line
  in any base — a mine feeding a smelter — belts remain correct forever.
- **A belt is also a buffer.** A full belt is thousands of items of storage in transit; a conduit
  holds almost nothing, so a conduit-fed line stalls the instant production hiccups.
- **Pipes exploit physics for free.** Gravity, head pressure and a condensate loop cost no power.
  A fluid core always does.

The division that emerges is the one real factories have: **belts and pipes move bulk, conduits
move logistics**. At T5 the Singular Conduit genuinely can replace everything, and by then power is
abundant enough that some players will — a legitimate and expensive way to play.

**Consequences.** This is the third instance of one principle — pay in layout, or pay in power —
after belts versus drones and pipes versus the Phase Network. The repetition is intentional: it is
the mod's economic language, and a player who learns it once can predict every later trade.

The heat core is the genuinely new capability. Moving heat *as heat* rather than as hot fluid is
both more realistic and practically useful, since reactor waste heat can reach a distant process
without plumbing a loop there and back.

The balancing risk is the item core's power cost. Too cheap and belts die; too expensive and the
conduit is ornamental.

---

## ADR-0026 — Named conduit networks instead of coloured channels

*2026-10-01 · Accepted*

**Context.** Conduit mods separate logical networks sharing physical space using a fixed palette of
coloured channels, typically sixteen. This works at small scale and fails at large scale: the
colours are a fixed budget, and nobody remembers what purple meant three months later.

**Decision.** Networks are **named**. The player names a network — `iron-bus`,
`reactor-coolant` — and endpoints subscribe by name. Names are unlimited and self-documenting, and
a conduit reports which networks it belongs to when inspected.

**Alternatives rejected.** Coloured channels, for the reasons above. Automatic network inference
from connectivity, which is convenient until two networks accidentally touch and silently merge.

**Consequences.** Readability at scale, which is the whole problem, plus no arbitrary cap on
network count. The costs are a text-entry UI where a colour picker would have done, the need to
handle renames and typos gracefully, and the fact that names are strings and must therefore be
persisted, synchronised and shown in the conduit's tooltip.

---

## ADR-0027 — Chassis marks widen the condition envelope

*2026-10-01 · Accepted*

**Context.** Machine tiering is normally numeric: the same machine with a bigger speed number and a
higher voltage cap. GregTech ships nine of them. The player rebuilds identical infrastructure
repeatedly and nothing new becomes possible, which is the precise definition of grind this project
rejects (ADR-0021).

**Decision.** Machines exist at marks **MK I–MK V**, applied **in place** with a Chassis Upgrade
Kit — the machine keeps its position, contents, configuration and connections, and is never
rebuilt or re-piped.

A mark sets upgrade slot count and maximum Flux tier, but its defining property is that it
**widens the condition envelope**. Since recipes are selected by conditions (ADR-0020), a higher
mark unlocks *recipes*: an MK I Arc Furnace reaches 1800 °C and smelts common metals, an MK V
reaches 3500 °C and runs everything the machine type is physically capable of.

**Consequences.** Upgrading becomes a goal rather than a tax, because it changes what is possible
rather than how fast the same thing happens. The catalogue does not explode, since marks are an
attribute of a machine rather than five separate blocks. And low marks never become invalid — an
MK I still runs every recipe inside its envelope, forever.

This also gives the research tree something concrete to gate that is not a new block.

---

## ADR-0028 — Every machine upgrade is a trade

*2026-10-01 · Accepted*

**Context.** Most upgrade systems offer strict improvements, so the optimal play is to fill every
slot with the best one. The upgrade is then not a decision, it is a tax the player pays once and
forgets. Mekanism's speed upgrades are the canonical case: there is no reason not to max them.

**Decision.** **Every upgrade spends one resource to buy another.** Speed buys cycle time with
superlinear power and waste heat; Efficiency buys power with time; Yield buys output with time and
a catalyst; Precision buys condition accuracy with constant upkeep; Insulation buys cheap heat
retention with slow thermal response; Damping buys Resonance silence with throughput. Three pairs
are mutually exclusive — Speed↔Efficiency, Speed↔Precision, Insulation↔Parallel — because wanting
both is wanting the trade not to exist.

Which upgrade is correct therefore depends on which resource is scarce *now*: Efficiency when
power-limited early, Yield and Recovery when material-limited mid-game, Parallel when throughput-
limited with power to spare, Precision for tight chemistry, Damping when hiding from what the noise
attracts.

**Consequences.** The same machine is configured differently in different parts of the same base
and at different points in the game, which is the definition of a live decision. Notably **Speed is
almost never right**, consistent with the stance on overclocking: the correct way to produce more
is to build wider.

Keeping this from becoming its own chore is part of the decision: upgrades are reusable so
experimenting is free, Process Cards carry the loadout so configuring the tenth machine is a copy,
and the Atlas accounts for upgrades when solving a line so the choice is informed rather than
guessed.

The risk is balance surface area — ten upgrades times five marks times twenty machines is a large
space, and the exclusivity pairs are what keep it from collapsing into one dominant loadout.

---

## ADR-0029 — Operator Drones: imperative automation beside declarative logistics

*2026-10-01 · Accepted*

**Context.** Every automation system in the mod so far is **continuous flow**: belts, pipes,
conduits, and logistics drones that fulfil standing requests. All of them are declarative — you
describe a desired steady state and the system maintains it.

A large class of real factory work cannot be described that way. *"Wait until the autoclave
finishes, take the batch to the press, run it, bring the byproduct back, and swap the catalyst if
it is spent"* is a **sequence**: irregular, conditional, multi-step. No flow system expresses it,
and the player ends up doing it by hand — which is grind by definition.

**Decision.** Add **Operator Drones**: a drone that executes an ordered routine, step by step,
housed and programmed in an **Operator Bay** that is a pylon module like the Drone Bay.

Routines are **built, not typed**. A routine is an ordered strip of physical Instruction Cards —
closer to a player-piano roll than to code — because the README already refuses to make the player
learn a scripting language for the circuit network and the same rule applies here. The vocabulary
is eleven cards: `Go To`, `Take`, `Give`, `Draw`, `Pour`, `Absorb`, `Emit`, `Operate`, `Read`,
`Wait Until`, `If/Else`, `Repeat`, `Signal`, `Return` — of which only two are branches.

Capability comes from **pods**: cargo, fluid (the drone's own tank), thermal, tool arm, sensor and
range extender (ADR-0030).

**Alternatives rejected.** A text or graph scripting language, which is powerful and is exactly the
thing the mod has already decided not to ask of the player. Extending logistics drones with
conditions, which would make a simple declarative system complicated without making it imperative.

**Consequences.** Operator Drones are slow and single-tasking, so they are the wrong answer to
anything high-volume — a belt moves more ore in a second than a drone moves in a minute. They win
only where flow loses, which keeps every other logistics system intact. At T6 the same routines run
on remote colonies, where belts cannot reach at all, which is what turns an off-world base from a
trickle into a factory.

Debuggability is part of the decision rather than a later addition, because a programmable system
without debugging is misery: the drone displays its current instruction in flight, step mode
advances one instruction at a time, failures name themselves (`Step 4: target inventory full`) on
the bay and as a logic signal, and editing draws a holographic preview of the path.

Routine Cards make a finished routine one copyable item, so building the tenth identical setup is a
copy rather than a repeat — the same anti-repetition device as Process Cards.

The risk to watch is scope creep in the card vocabulary. Eleven cards is learnable; thirty is a
programming language with extra steps, and the line should be held.

---

## ADR-0030 — Everything is a chassis plus modules

*2026-10-01 · Accepted*

**Context.** Four systems arrived at the same shape independently: machines take upgrades in slots
(ADR-0028), conduits take cores that decide what they carry (ADR-0025), drones take pods that
decide what they can do (ADR-0029), and pylons take bay modules. This was convergence, not plan,
which usually means the pattern is load-bearing and should be stated before someone breaks it.

**Decision.** Adopt it as a design principle: **capability arrives by slotting a module into a
chassis, never by crafting a different block.** A chassis sets the ceiling — slot count, tier,
envelope — and modules decide what it actually does within that ceiling.

**Consequences.** The player learns one interaction pattern and it applies everywhere, which is a
large reduction in what a complex mod asks them to hold in their head. The block catalogue stays
small, because variation lives in modules rather than in block variants — the alternative is how
tech mods end up shipping nine near-identical machines per type.

It also means upgrading is non-destructive by construction: you slot something in, and the machine
keeps its position, contents, configuration and connections. Nothing is ever rebuilt or re-piped,
which is one of the mod's main anti-grind commitments.

The cost is that modules are items that must be produced, which pushes more load onto the
fabrication economy (ADR-0017), and that every chassis needs a module UI good enough to make the
slots legible. The failure mode to avoid is modules that are strictly mandatory — a module everyone
always installs should simply be part of the chassis.

---

## ADR-0031 — Construction drones exist so multiblocks can be massive

*2026-10-01 · Accepted*

**Context.** Construction Drones were in the design as a convenience — "builds from blueprints" —
and on review that is not a good enough reason to ship them. The Blueprint Tool already captures
layouts, and a mod whose whole premise is removing busywork should be suspicious of a feature whose
only justification is saving clicks.

The useful question is what they *enable*. The answer reframes them entirely.

**The real limit on multiblock scale in every mod is placement tedium, not design.** Nobody ships a
two-thousand-block structure because nobody will place two thousand blocks by hand. So multiblocks
stay at 5×5×5, their design space stays shallow, and "build this shape" becomes a schematic copied
once — which is the exact failure ADR-0022 set out to avoid by making multiblocks parametric.

ADR-0022 is therefore only half-true as written: a parametric multiblock is a real design space
only if the design space is big enough to have interesting answers, and by hand it cannot be.

**Decision.** Construction Drones are an **enabler, not a convenience**, and multiblocks are scaled
up to match. From T3 drones build a blueprint using materials drawn from the logistics network,
which lifts the size ceiling; multiblock scale then grows with the player's ability to build —
roughly 5³ by hand at T2, ~15³ with drones at T3, ~32³ with swarms at T4, and structures measured
in chunks at T5 via the **Assembly Field**, which materialises a blueprint at once.

Two things make this sound rather than merely large:

- **Placing blocks by hand at that scale is grind by the project's own definition** — repeating an
  action whose outcome is already known. Automating the placing while keeping the designing is
  precisely the rule in ADR-0021.
- **Blueprints are validated and simulated before construction.** A design reports legality and
  which part fails, computed output, heat, coolant demand and stability margin, its full bill of
  materials, and a warning when it will run at the edge of its envelope. Trial and error at a cost
  of ten thousand components is punishment, not engineering.

**Consequences.** The Fission Reactor becomes the flagship design problem it was supposed to be:
hundreds of internal positions, fuel clustering traded against coolant routing, moderators and
reflectors making the core's geometry matter rather than just its volume. The same applies to the
accelerator ring, the distillation column and the fusion confinement.

The friction that remains is the right friction — materials still have to be produced, and that
cost is what makes a large multiblock a commitment. What is removed is only the clicking.

Risks: the simulator must agree exactly with the runtime behaviour or it becomes a lie, which makes
it a real implementation constraint rather than a UI nicety. Chunk-scale structures raise genuine
performance questions that the multiblock framework has to answer before T5 content is built. And
early multiblocks must stay hand-placeable, since drones do not exist until T3.

---

## ADR-0032 — The item catalogue is a matrix, not a list

*2026-10-01 · Accepted*

**Context.** `PROCESSES.md` had to specify the item catalogue, and the obvious way to do that is to
enumerate items. That is how most tech mods do it, and it is why their catalogues reach four hundred
entries while still missing the one form a given pack needs.

Enumeration also cannot work here. The material set is not known at build time — it comes from the
runtime tag scan (ADR-0004) and therefore depends on the installed pack. A list cannot be written
for a set that does not exist yet.

**Decision.** The catalogue is defined as **axes, and the items are their product**. Grindless
authors the axes; the materials come from tags; the generator emits the cells.

- **Form × material.** Thirteen formed types (plate, foil, rod, bolt, gear, ring, wire, fine wire,
  coil, nugget, ingot, block, hot ingot) and eight ore-line types, applied to every material.
- **Anion × material.** Six compound families — oxide, sulfide, chloride, sulfate, carbonate,
  hydroxide — which is what gives the chemical layer its breadth.

A cell is emitted only when a process that produces it exists, so a pack never sees salts it has no
way to make.

**Alternatives rejected.** Enumerating items per material (does not survive an unknown material
set, and is unmaintainable at pack scale); a single generic "dust" with NBT material data (breaks
belts, filters, storage mods and every recipe viewer, and NBT-bearing stacks are the usual cause of
tech-mod performance complaints); deferring to another mod's unification (makes Grindless unusable
standalone).

**Consequences.** Adding a form is one authored entry that instantly covers every material in the
pack, which is the same leverage ADR-0005 gets for recipes. Grade becomes a property of the *form*
rather than of the item, which is what lets one generated recipe set cover the whole pack and is
the basis of the composition rule in ADR-0035.

The cost is that the catalogue's size is a pack property rather than a project one, so the project
cannot state how many items it has — only how many axes. Creative-tab organisation and texture
generation both have to be driven by the same matrix or they will drift from it.

---

## ADR-0033 — Materials come from tags, reagents are Grindless's own

*2026-10-01 · Accepted*

**Context.** ADR-0004 resolves materials from tags so Grindless uses the pack's copper rather than
registering a rival one. Writing the chemical core exposed the limit of that rule: it works for
materials and fails for reagents.

`forge:ingots/copper` is a real convention that essentially every mod follows. There is no
equivalent agreement for sulfuric acid. Mods that ship one disagree on its name, its concentration,
its colour and whether it is even a fluid, so a tag lookup would resolve to a different substance in
every pack — or, far worse, resolve to something whose amount means something else, silently
breaking every ratio in `PROCESSES.md`.

**Decision.** Split the rule by kind.

- **Materials** — anything with a `forge:`/`c:` convention — are resolved from tags at runtime and
  never registered by Grindless.
- **Reagents** — acids, bases, industrial gases, leachates, process intermediates — are
  **registered by Grindless**, with its own units and reference states, and are tagged so other
  mods can opt in.

The boundary is testable: if a widely-followed tag convention exists, it is a material; if it does
not, it is a reagent.

**Alternatives rejected.** Tag-resolving reagents too (the ratios in `PROCESSES.md` become
meaningless when the resolved fluid has a different concentration); a config mapping per pack
(pushes an unsolvable problem onto pack authors, and gets it wrong silently); avoiding named
reagents by making chemistry abstract (throws away the entire reason the condition system exists).

**Consequences.** Grindless's acid may coexist with another mod's acid. That is accepted: the
alternative is a wrong conversion rather than a visible duplicate, and a duplicate is something a
pack author can unify deliberately while a wrong ratio is a bug nobody can see.

Every registered reagent carries a reference state (ADR-0015), because a gas volume without one is
not a quantity. Reagents therefore need their own tags published early, so packs can integrate
rather than merely coexist.

---

## ADR-0034 — Ratios are quoted against a canonical process unit

*2026-10-01 · Accepted*

**Context.** `PROCESSES.md` specifies routes that must be *compared* — that is the whole point of
having more than one — and the Atlas's solver has to balance lines across them (ADR-0023). Neither
works if two routes quote their yields on different bases. "Three per operation" and "three per
input" differ by the whole chain length, and chains that look generous per stage routinely multiply
out to a loss.

**Decision.** One basis, fixed for the whole project.

- **u (unit)** is the canonical solid amount: 1 u = 1 ingot = 1 dust = 1 plate = 9 nuggets.
- **1 u as melt = 144 mB**, the established modded convention, so casting needs no conversion table.
- **B** is 1000 mB; **gases are quoted at 20 °C and 0.1 MPa** unless stated, because a fluid is a
  state and not a thing (ADR-0015).
- **Yields are per unit of primary input to the front of the route**, not per stage.
- **Times are for MK I, no upgrades, at the process's base Flux tier.**

**Alternatives rejected.** Per-stage ratios (hide end-to-end losses, which is exactly the number
the player needs); quoting at a "typical" overclock (bakes in the Speed upgrade the design wants
players to reject, ADR-0028); mB-only with no unit concept (every recipe grows a conversion).

**Consequences.** The time baseline is only stable because marks do not change speed (ADR-0027) and
overclocking is a trade rather than an expectation (ADR-0028). Those two decisions are what make a
quoted second meaningful three tiers later; if either is ever revisited, every time in
`PROCESSES.md` has to be requoted.

Throughput therefore has to come from parallel, multiblocks and route choice, which is the intended
shape. The cost is that `PROCESSES.md` cannot quote power: FU cost is deferred until the Flux API
exists, because guessing it before the energy layer is written produces numbers nobody can trust.

---

## ADR-0035 — Routes compose from a beneficiation stage and a reduction stage

*2026-10-01 · Accepted*

**Context.** The design requires several meaningfully different routes to every important product
(ADR-0021, and the route rule in `MACHINES.md`). Written as whole routes, that is quadratic
authoring: five ways to prepare an ore times four ways to reduce it is twenty chains to specify,
balance and maintain, and every new preparation step multiplies the existing work.

Twenty authored chains is also how a recipe viewer becomes unreadable.

**Decision.** An ore route is **two independent stages**, chosen separately:

```
yield (u metal per u raw) = feed grade × reduction factor
```

Beneficiation (B0–B4) sets a **grade** on the output form. Reduction (R1–R4) applies a **factor**.
Nine authored process families produce twenty routes, and adding a sixth preparation adds four
routes for one unit of work.

**Alternatives rejected.** Authoring whole routes (quadratic, and the combinations that nobody
thought to author become the ones players ask for); a single linear upgrade ladder as in most ore
mods (only one axis, so there is only ever one correct answer and no decision); additive bonuses
instead of multiplicative (makes late beneficiation worthless, since a flat bonus on a large number
is noise).

**Consequences.** Grade must be a property of the *form*, not of the stack — which is exactly what
ADR-0032 provides — so composition needs no per-item state and belts stay cheap. The generated
recipe set is per-stage, so the Atlas composes routes at query time rather than enumerating them,
and a "route" is a path through the graph rather than an object.

The risk is legibility: twenty routes are cheap to build and expensive to *present*. The Atlas has
to show five options and four options, never a list of twenty, or this decision trades an authoring
problem for a UI problem. That is recorded as an open question in `PROCESSES.md`.

---

## ADR-0036 — Every byproduct must have a named sink

*2026-10-01 · Accepted*

**Context.** Byproducts are what make routes interact, and that interaction is a stated design goal:
the messy route is often correct precisely because you want its waste. But byproducts are also the
most reliable way a complex mod generates busywork. A stream the player can only delete produces
exactly one behaviour — switch on auto-void and never think about it again — and at that point the
byproduct was not content, it was a config toggle with extra steps.

Worse, it teaches the player to void *everything*, including the streams that mattered.

**Decision.** A process may only emit a byproduct if that byproduct has **at least one named sink
elsewhere in the graph**, documented in the byproduct ledger in `PROCESSES.md`. "Deconstruct it into
Matter" does not count as the named sink: it is the universal backstop and it is always available,
so accepting it would make the rule vacuous.

If no real sink exists, the process does not emit the byproduct at all.

**Alternatives rejected.** Byproducts with no use, as a realism flourish (realism that generates a
chore is a bad trade in a mod defined by ADR-0021); pollution or waste-disposal mechanics (a
maintenance tax, which AGENTS' own stance on chores forbids); making all byproducts universally
valuable (removes the trade that makes route choice interesting).

**Consequences.** The ledger becomes a real constraint on adding processes: a new process that
produces a new residue cannot ship until something consumes it. That is a feature — it keeps the
graph connected, and connectedness is what makes a factory a system rather than a set of lines.

Auto-void stays off by default (ADR-0018) and is now genuinely defensible, because voiding is
always the player discarding something that had a use. The Atlas can therefore flag a voided stream
and say what it was worth, which turns a silent loss into a visible decision.

---

## ADR-0037 — Flux amounts are `long` and clamp at the FE boundary

*2026-10-01 · Accepted*

**Context.** Forge Energy stores and transfers `int` amounts, and the obvious thing is to match it,
since ADR-0006 makes FU and FE the same size. That works until the Flux ladder is taken seriously.

F9 is 2 097 152 FU/t. A buffer sized at a few seconds of throughput for a top-tier machine is in the
hundreds of millions, and a Singularity Reactor or a planetary grid buffer is plainly past
2 147 483 647. An `int` does not overflow politely: it wraps to a negative number, so a full buffer
reads as a debt and the energy is silently created or destroyed. That is the identical failure mode
ADR-0006 rejected a non-unit conversion ratio to avoid, arriving from the other direction.

**Decision.** `FluxStorage` is `long` throughout. Conversion to FE **saturates** at
`Integer.MAX_VALUE` rather than wrapping, and every caller treats a conversion result as a
*request*, believing the amount the other side reports as moved rather than the amount asked for.

**Alternatives rejected.** `int` amounts with a scaling factor on large buffers (reintroduces
rounding, which is exactly what ADR-0006 forbids); `BigInteger` (allocation per transfer, twenty
times a second, on thousands of machines); capping the Flux ladder so `int` suffices (lets a
storage-type choice dictate game design, and the long ladder is a stated goal).

**Consequences.** A single transfer to or from a foreign FE machine cannot exceed about 2.1 billion
FU. That is not a real limitation — no vanilla-shaped machine moves that much in one tick — and it
is correct behaviour rather than a cap: the transfer is simply smaller than requested, and the next
tick moves the rest.

Internal Grindless transfers never clamp, because both sides are `long`. The clamp exists only at
the boundary with another mod's energy system, which is the only place the width difference is
real. Saturation is the specific choice here: it makes an over-large request smaller, never
negative, so a clamp can never manufacture or destroy energy.

---

## ADR-0038 — One Flux ladder, with voltage names as aliases

*2026-10-01 · Superseded in part by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded in part.** There is still one Flux ladder and LV through IV remain display
> aliases. The ladder now runs through F15, and technology tier Tn is deliberately aligned
> with Flux tier Fn. See ADR-0107.

**Context.** The design accumulated two power scales. The README describes **voltage tiers** LV, MV,
HV, EV and IV at 32, 128, 512, 2 048 and 8 192 FU/t, while `MACHINES.md` describes the **Flux tier
ladder** F0–F9, whose F1–F5 are 32, 128, 512, 2 048 and 8 192 FU/t.

They are the same numbers. Implementing both would create two enums that must agree forever, and
enums that must agree forever eventually do not — one gains a tier, a tooltip reads from the wrong
one, and the player sees a machine that needs "HV" next to a cable rated "F3" with no stated
relationship.

**Decision.** There is **one** ladder, `FluxTier` F0–F9. LV through IV are display aliases on F1
through F5, carried as a nullable field on the enum. Tiers above F5 have no alias, which is correct:
the voltage vocabulary was never defined past IV.

**Alternatives rejected.** Two enums with a conversion function (the drift problem, merely delayed);
deleting the voltage names (they are established in the README and in the genre's vocabulary, and a
player reading "MV machine" understands it instantly); renaming the Flux tiers to voltages (stops at
IV and cannot express F6–F9 without inventing names).

**Consequences.** One source of truth for throughput, and the aliases are presentation only. A third
axis still exists and is deliberately separate: research tiers T0–T6 gate what may be *built*, while
Flux tiers gate how much power a built thing may *accept*. Keeping those distinct is what lets a T1
Arc Furnace be fed F5 power without becoming a T5 machine.

Under-volting is implemented here as a smooth curve — half throughput per tier of deficit — rather
than as a hard gate, matching the README. A machine fed too little power always runs, just slowly,
so a power shortfall is never a wall.

---

## ADR-0039 — Forge 1.20.1 is the only build target

*2026-10-01 · Accepted*

**Supersedes [ADR-0001](#adr-0001--architectury-for-multiloader-support) in part, and narrows
[ADR-0006](#adr-0006--flux-units-convert-to-fe-at-11-over-a-hand-written-bridge).**

**Context.** ADR-0001 chose multiloader reach: Fabric and Forge from one `common/` module. The cost
of that reach was not visible while the project was a registry skeleton. It became visible the
moment real platform code arrived.

Writing the Flux energy bridge meant writing it twice, against two unrelated models — Forge
capabilities with `int` amounts, and Team Reborn Energy with `long` amounts and a transaction
system. The Fabric half needed a `SnapshotParticipant` to be correct under transaction rollback,
which is a genuinely subtle piece of code with no Forge counterpart and no way to test it from the
Forge side. And `:fabric:build` was already failing with `Failed to remap 57 mods`, an opaque
toolchain failure that had nothing to do with the mod's own code.

So the second loader was costing double implementation, double review, double failure surface and a
broken build, in return for an audience the project does not yet have — nothing is playable, and
every block registered so far is an inert placeholder.

**Decision.** Target **Forge 1.20.1 only**. Remove the `fabric` subproject, its sources, its loader
metadata and the TeamReborn Energy dependency. `enabled_platforms=forge`.

**Keep the `common/` + `forge/` split and Architectury.** This is the part that looks like a
contradiction and is not: the split costs nothing now that it exists, `common/` already holds ~95%
of the code, and `@ExpectPlatform` is still the mechanism that keeps loader-specific code out of it.
Collapsing the two modules would mean rewriting every registry class against Forge's own
`DeferredRegister` — real work, today, to buy nothing, and it would make re-adding a loader a
rewrite instead of a build-file change.

**Alternatives rejected.** Keeping Fabric and fixing the remap failure (pays the double-implementation
cost indefinitely, for an audience that cannot play the mod yet); dropping Architectury entirely and
merging `common` into `forge` (a rewrite with no benefit, and it forecloses a future port); targeting
Fabric instead (Forge 1.20.1 has the larger modpack ecosystem for this genre, and NeoForge 1.20.1
loads the Forge jar unchanged, so one jar already serves two loaders).

**Consequences.** One bridge, one build, one failure surface. `:forge:build` goes green in about
50 seconds where the two-platform build failed outright. NeoForge 1.20.1 users are unaffected, since
ADR-0002 already established that they load this jar directly — so "Forge only" still means two
loaders in practice.

The cost is real and worth stating plainly: **Fabric support is gone, and restoring it means
rewriting the Fabric energy bridge**, including the transaction-rollback handling that was deleted
with it. That is a deliberate trade of future optionality for present velocity, taken while the
project is small enough that the rewrite would be cheap.

One oddity is left behind. `common/build.gradle` still depends on `fabric-loader` at compile time,
because Architectury Loom builds the common project in a Fabric-shaped environment regardless of
which platforms are enabled. Nothing Fabric ships in the jar. It is commented in place, because it
looks exactly like something a later session would "clean up" and then spend an afternoon
rediscovering.

---

## ADR-0040 — Running out of band costs time, never yield

*2026-10-01 · Accepted*

**Context.** `MACHINES.md` says a recipe run at the edge of its condition band "still works, with
reduced yield, longer time or extra byproducts". That is three options, and the implementation has
to pick one. The obvious pick is reduced yield, because it is what most mods do and it feels like a
natural penalty.

It is the wrong one here, and the reason is ADR-0034. Every ratio in `PROCESSES.md` is quoted per
unit of primary input — B3 × R3 yields 3.12 u of metal per unit of raw ore. If running slightly off
optimum silently produced less metal per ore, then **every one of those numbers becomes conditional
on tuning**, the Atlas's line solver cannot be trusted, and a player comparing two routes is
comparing two numbers that do not mean what they say.

It is also the worse failure to debug. A factory running 10 % slow is visible in a throughput
readout. A factory quietly returning 2.9 u instead of 3.12 u is invisible until someone does the
arithmetic by hand — which is the exact activity this project exists to remove.

**Decision.** A condition mismatch inside the tolerance zone reduces **speed only**. Outputs are
unchanged in both kind and quantity. Efficiency is a multiplier in `[0.25, 1]` applied to process
rate, and `PROCESSES.md`'s ratios hold at every point inside the band.

The remaining two options stay available as *per-recipe* effects rather than as the global rule: a
specific recipe may name an extra byproduct at the edge of its band when that is chemically
meaningful, which is a deliberate authored choice rather than a silent tax on every process.

**Alternatives rejected.** Reduced yield as the global rule (breaks every quoted ratio, and fails
invisibly); outright failure outside the optimal band (turns tuning into a wall, which is the
tedium ADR-0021 forbids, and wastes the tolerance zone entirely); scaling power draw instead of
speed (power is not yet implemented in balance terms, and a slow machine is more legible than an
expensive one).

**Consequences.** Tuning a machine is worth doing and never punishing. The material balance of the
whole graph is independent of how well anything is tuned, so the Atlas can solve a line from the
recipe graph alone and be right.

Efficiency is the **minimum** across the dimensions a recipe names, not their product. Two
dimensions slightly off should not compound into a crawl, and a minimum has a single identifiable
cause the machine can name — which is what ADR-0041 then reports.

The floor is 0.25, not zero: the edge of tolerance is four times slower, which is a real cost
without ever being a stall. A stalled machine that reports "running" is the worst outcome of all.

---

## ADR-0041 — A condition check returns a named fault, not a boolean

*2026-10-01 · Accepted*

**Context.** The natural signature for "can this recipe run here?" is a boolean, and the natural
signature for "how fast?" is a double. Both are easy, and both throw away the only information the
player actually needs when a factory stops.

`MACHINES.md` already identifies this as the single most common failure in complex packs — "my
factory stopped and I do not know why" — and commits to distinguishing blocked, starved and
out-of-band everywhere. A boolean cannot carry that, and neither can an efficiency of `0.0`.

**Decision.** Every condition check returns a `ConditionReport`: a named `ConditionFault`, the
limiting `ConditionDimension`, and the speed multiplier.

Faults carry a **direction**, not just a dimension. `TOO_COLD` and `TOO_HOT` are different faults
because they have different fixes and different in-world tells — too cold and the reaction does not
start, too hot and the product decomposes into something visibly wrong.

`OUTSIDE_ENVELOPE` is deliberately separate from the ordinary faults. The ordinary faults mean the
machine *could* reach the condition and currently is not, so the fix is a dial. `OUTSIDE_ENVELOPE`
means it never can, so the fix is a chassis upgrade or a different machine (ADR-0027). Collapsing
the two would send a player hunting for a setting that does not exist.

When a process runs below full speed, the report names the **one** dimension responsible, which is
what makes "running at 62 % — limited by Temperature" possible instead of an unexplained number.

**Alternatives rejected.** A boolean plus a separate query for the reason (two calls that can
disagree, and the second one gets forgotten at exactly the call sites that needed it); throwing on
mismatch (a mismatch is an ordinary game state, not an error, and this runs every tick); an
efficiency of zero as the failure signal (indistinguishable from a stalled-but-valid process, and
carries no cause).

**Consequences.** Machine status output, the logic signal, the Process Atlas's reachability display
and the multiblock simulator can all be built on one type, so they cannot drift into describing the
same failure three different ways.

The reports are allocated per check and checks run per machine per tick. The record is small and
short-lived, and the optimal case is a shared constant, so this is expected to be fine — but if
machine counts ever make it measurable, the fix is caching the report until an input or a setting
changes, not returning to booleans.

Fault messages live on the enum for now. They move behind translation keys when the client layer
exists; the enum is the single place that has to change.

---

## ADR-0042 — Machines subscribe to ticking; they do not tick by default

*2026-10-01 · Accepted*

**Context.** Grindless is aiming at bases with thousands of machines. The default Minecraft shape —
a `BlockEntityTicker` that runs every tick on every machine — means each one polls its inventory,
looks for a recipe and checks its neighbours sixty times a second whether or not anything has
changed.

The arithmetic is unforgiving. Ten thousand machines at thirty microseconds each is three hundred
milliseconds, or six times the entire 50 ms tick budget, spent on machines that are *doing nothing*.
This is the characteristic way a tech mod ruins a server, and it cannot be fixed later by
optimising the work: the fix has to be not doing it.

**Decision.** A machine holds a list of **tick subscriptions** and runs only those. Work is
subscribed when something makes it necessary and unsubscribed the moment it is not, so an idle
machine's tick is a check that the list is empty.

Subscriptions are re-evaluated from **change notifications** — a buffer gaining its first item, a
side being reconfigured, a neighbour appearing — never by polling, since polling to discover
whether polling is needed defeats the purpose.

Periodic work additionally uses a **per-machine offset** derived from block position, so throttled
work is spread across ticks instead of landing on all of them at once.

**Prior art.** This is GregTech CEu Modern's design, adopted deliberately rather than reinvented.
Their developer documentation states it directly — *"for the sake of performance, our machines are
no longer always in a tickable state. We introduced `ITickSubscription` for managed tick logic"* —
and their machines pair it with inventory change listeners that call an `updateSubscription` method.
Their throttles read `getOffsetTimer() % 5 == 0` rather than using raw game time, which is the
offset idea above.

**Alternatives rejected.** Ticking everything and optimising the work (moves the constant, not the
shape — ten thousand fast no-ops is still ten thousand); throttling with raw game time
(`gameTime % 20 == 0` synchronises *every machine in the world* onto the same tick, so average load
drops twentyfold and the worst tick does not drop at all, which is exactly what players feel);
ticking machines only in loaded chunks (already true, and irrelevant — the loaded base is the
problem).

**Consequences.** An idle base is nearly free, and cost tracks activity rather than machine count,
which is the property that lets the design keep promising large factories.

The cost is a real correctness burden. **A machine that forgets to resubscribe is silently broken**,
and broken in the worst way to diagnose: it does nothing, which is also what it does when correct.
Every input to a subscription decision must notify, so the rule is that a setter which can change
whether work is needed must re-evaluate the subscription. This is more invasive than it sounds and
is the main thing a reviewer should check in machine code.

Subscription work frequently cancels itself — that is how a machine goes idle — so the tick loop
tolerates cancellation and subscription during iteration, deferring list cleanup until the pass
ends.

---

## ADR-0043 — Recipe lookup is indexed and cached, never a linear scan

*2026-10-01 · Accepted*

**Context.** Recipes are generated at runtime from tags (ADR-0005), across every material in the
pack, multiplied by the form and anion matrices (ADR-0032) and the twenty composed ore routes
(ADR-0035). The recipe set will be large — plausibly tens of thousands of entries in a big pack —
and it is not known until the world loads.

"Which recipe matches these inputs?" asked by scanning that list is O(recipes) per machine per
attempt. It is also the question a machine asks most often, because an idle machine with inputs it
cannot use asks it forever.

ADR-0042 removes the cost for machines with *nothing* to do. It does nothing for a machine holding
items that match no recipe, which is a normal and common state.

**Decision.** Three layers, decided now so that step 15 implements them rather than discovering
them:

1. **An index, not a list.** Lookup is keyed by input so a search inspects candidates, not the
   catalogue.
2. **A cached last recipe.** Before searching, retry the recipe this machine ran last and check
   whether it still matches. A machine in steady state — which is nearly all of them, nearly all
   the time — then never searches at all.
3. **A negative result that sticks.** When a search fails, the machine does not retry every tick.
   It waits for an input change, or for a throttled retry on its offset timer.

**Prior art.** Both reference implementations do this, by different routes. GregTech CEu stores
recipes in a trie — `Map<AbstractMapIngredient, Either<GTRecipe, Branch>>`, descended ingredient by
ingredient — and its `RecipeLogic` checks `lastRecipe` before searching, keeps `lastFailedMatches`,
and falls back to `getOffsetTimer() % 5 == 0` for the retry when nothing matched. Mekanism reaches
the same place with typed `InputRecipeCache` classes keyed by item, fluid or chemical, in single,
double and triple input shapes.

That two mature implementations converged independently is the strongest evidence available that
the linear scan is not survivable at this scale.

**Alternatives rejected.** Scanning with an early exit (still O(recipes) in the failing case, which
is the case that repeats); caching only the last recipe without an index (the first search after
any input change is still a full scan, and that is exactly when a player is watching); precomputing
every input combination (combinatorial, and the material set is not known until load).

**Consequences.** The index must be rebuilt when recipes reload — world load and `/reload` — and a
stale index is a wrong-recipe bug rather than a crash, so rebuilding is tied to the recipe manager
rather than done opportunistically.

Condition matching stays **outside** the index. Conditions select which *machine* can run a recipe
(ADR-0020), not which recipe matches the inputs, and they are cheap to evaluate once a candidate
exists. Indexing six dimensions of continuous conditions would be an enormous structure answering a
question nobody asks.

The cached recipe must be invalidated when the machine's conditions or upgrades change, not only
when its inputs do — otherwise retuning a machine leaves it running the recipe it found before, which
would be a genuinely confusing bug.

---

## ADR-0044 — Neighbour lookups are cached against capability invalidation

*2026-10-01 · Accepted*

**Context.** A machine that pushes power or items to an adjacent block has to find that block.
`Level.getBlockEntity(BlockPos)` is a chunk lookup plus a map lookup — negligible once, and not
negligible done six times a tick by thousands of machines to rediscover something that only changes
when a player breaks a block.

The obvious fix is to cache the neighbour in a field. The obvious fix is also a correctness bug:
the cached reference survives the neighbour being broken, and the machine goes on pushing into a
block that no longer exists. This is the well-known cause of *"my machine stopped working until I
broke and replaced it"*.

**Decision.** Cache the neighbour, and invalidate the cache from the platform's own capability
invalidation rather than by re-checking.

Forge hands a capability out as a `LazyOptional` and calls `invalidate()` on it when it stops being
valid; holders register interest with `LazyOptional.addListener`. Grindless resolves a neighbour
once, registers a listener, and drops the cache when it fires.

Two paths are needed, not one. Invalidation covers a capability being **revoked**. It cannot cover
a block **appearing** where there was none, because there was nothing there to invalidate — so the
machine's neighbour-changed event also clears the cache. A cache that only listens to invalidation
never notices a new neighbour and looks exactly as broken as a stale one.

Absence is cached too: a machine facing a wall must not re-ask the wall every tick.

**Alternatives rejected.** Looking the neighbour up every tick (the cost this exists to remove);
caching without invalidation (stale references, and the failure is intermittent and
unreproducible, which is the worst kind of bug report); caching with a time-to-live (picks an
arbitrary number, and is simultaneously too slow to be correct and too fast to be cheap);
validating the cache by checking the block entity is still alive each tick (that check *is* the
lookup, so it saves nothing).

**Consequences.** Neighbour access becomes a field read in the steady state, which is what makes
per-tick pushing affordable at all.

The cost is that cache lifetime is now a real invariant with two independent triggers, and getting
either wrong produces a bug that is invisible in code review and intermittent in play. The
invalidation listener must not assume the block entity still exists when it fires, since the usual
reason it fires is that the block entity is being removed.

NeoForge 1.20.1 loads the Forge jar unchanged (ADR-0002), so this works there as written. Newer
NeoForge replaces this mechanism with `BlockCapabilityCache`, which does the same job with the
invalidation handled for you — a port target rather than a problem.

---

## ADR-0045 — Capabilities are attached by event, not overridden on the block entity

*2026-10-02 · Accepted*

**Context.** Other mods reach a Grindless machine's energy buffer through Forge's
`ForgeCapabilities.ENERGY`. The textbook way to provide one is to override `getCapability` on the
block entity.

That is not available here. Machine block entities live in `common/`, which must not import
loader-specific classes (ADR-0001), and `getCapability` is pure Forge. The constraint is not
theoretical: writing `MachineBlockEntity` produced exactly this mistake one layer down, where
`onLoad()` looked like a vanilla hook and turned out to be a Forge addition. The compiler caught
that one. It would not have caught a design that moved every machine into `forge/`.

**Decision.** Attach the capability from `forge/` with `AttachCapabilitiesEvent<BlockEntity>`.
Forge fires it as each block entity is constructed, so a listener there can attach an
`IEnergyStorage` view of any `MachineBlockEntity` without that class knowing Forge exists.

The attached provider **must** also register through `AttachCapabilitiesEvent.addListener`, which
is the load-bearing half. The chain was read in Forge's source rather than assumed:
`BlockEntity.setRemoved()` calls `invalidateCaps()`; `CapabilityProvider.invalidateCaps()` marks
itself invalid and calls `CapabilityDispatcher.invalidate()`; that runs every `Runnable` registered
by `addListener`; ours invalidates the `LazyOptional`, which fires the listener `NeighbourCache`
put on it (ADR-0044) and clears the stale reference.

Attaching without that listener compiles, works in testing, and leaks stale references in play.

**Alternatives rejected.** Overriding `getCapability` on the block entity (requires a Forge import
in `common/`, which is the rule ADR-0001 exists to enforce); an `@ExpectPlatform` capability hook
(the capability system is shaped around `LazyOptional` and invalidation callbacks, so the stub
would either leak those types into `common/` or be too lossy to invalidate correctly); moving
machine block entities into `forge/` on the grounds that ADR-0039 made the project Forge-only
(ADR-0039 explicitly kept the `common/` + `forge/` split so a future port stays a build change
rather than a rewrite, and block entities are most of what would have to move).

**Consequences.** `common/` stays loader-clean with no `@ExpectPlatform` stub for this, and the
whole Forge-facing surface is one class.

This also forced a latent bug into the open, which is the usual value of making something real.
The Hand Crank Dynamo stored its charge in a field separate from its Flux buffer, so exposing that
buffer would have published an empty load: insertable, never read, and holding none of the power
the dynamo had actually generated. Generation now writes to the buffer itself, and generators
override the buffer's direction — no external insertion, extraction at the rated tier — so the
object the machine fills is the object the world pulls from.

Two limits worth stating. The capability is offered on every face: the container contract can
express per-face energy gating, but honouring it needs the capability re-invalidated whenever a
face is reconfigured, and there is no screen to reconfigure one from yet. And the event fires for
*every* block entity in the game, so the listener must stay a cheap `instanceof` — Forge's own
documentation warns about this, and it is the one place where adding work would be felt
world-wide.

---

## ADR-0046 — The pylon index is chunk-bucketed, and brownouts resolve in two phases

*2026-10-02 · Accepted*

**Context.** ADR-0007 settled *where* network state lives — level-wide `SavedData` with a spatial
index, so there is no per-tick graph walk and an unloaded chunk cannot fragment a network.
Implementing it surfaced four questions that record did not answer.

**Decisions.**

**1. The index is a chunk bucket, not a tree.** Each pylon is filed under every chunk its supply
cube touches; a lookup hashes the query position's chunk once and exact-tests the few pylons filed
there. Supply cubes are small — an MK3's 64-block cube spans at most five chunks a side — so
buckets stay short and cost tracks *local density* rather than world size. A balanced tree would
also satisfy ADR-0007's `O(log n)`, at the price of rebalancing on every placement and far more
code for a worse constant.

The bucket is coarser than the cube, so a pylon can be filed under a chunk it only clips. The
exact per-axis test still runs on every candidate; skipping it is a subtle over-coverage bug that
would mostly work.

**2. Demand is registered before any of it is served.** Machines declare their intended draw, then
the network resolves one satisfaction fraction, then everyone draws at that fraction. Serving
machines as they tick instead would mean the ones that happen to tick early run at full speed
while the rest stop dead — which is exactly the individual starvation the README rejects, where a
random subset of the base stops with no indication why. Two phases is what makes "everything is
visibly sluggish" true rather than aspirational.

**3. Energy follows capacity when a network splits.** A fragment holding a third of the pylons
keeps a third of the charge. Giving it all to one fragment destroys energy; giving each fragment
the full amount manufactures it, and a player who noticed could break and replace a pylon in a
loop. Merging moves energy across for the same reason — if merging lost power, the optimal play
would be to drain a network before linking it, which is absurd.

**4. Where supply areas overlap, the first covering pylon wins.** Splitting a machine's draw
across overlapping networks would make its behaviour depend on index iteration order, which is
unpredictable to a player and untestable in practice. Overlap is a layout the player chose; it
should be stable, not clever.

**Consequences.** Placement and removal are the only expensive operations, and removal is the
expensive one — it recomputes connectivity by flood fill. That is the right place to pay: it
happens when a player breaks a block, not every tick.

Capacity is *derived* from the member pylons rather than stored, so it cannot drift out of step
with them, and a shrinking network clamps its contents rather than holding energy it no longer
has room for.

The saved form stores membership and rebuilds the index on load, so the two cannot disagree. Tier
ordinals are bounds-checked on read, because an unchecked enum ordinal from a future version
throws during world load — which a player experiences as a corrupt save rather than as a mod bug.

---

## ADR-0047 — Veins are long-lived, and deepening competes with relocating

*2026-10-02 · Accepted*

**Context.** Resource Genesis replaces mining with extraction from a chunk that depletes toward a
nonzero floor, and the README frames the resulting pressure as a virtue: a worked vein is slow, so
the efficient move is to expand outward, and the game becomes exploration and layout rather than
tunnelling.

That argument has a failure mode it does not address. If veins run down quickly, "expand outward"
stops meaning exploration and starts meaning **abandon, relocate, rebuild the same layout
somewhere else, repeat**. Rebuilding a layout you have already solved is repeating an action whose
outcome you already know, which is this project's own definition of grind (ADR-0021). The system
designed to delete mining would have reinvented it with extra steps.

The depletion rule was also written without numbers, so "slowly" could have meant anything.

**Decision.** Three changes, all aimed at making an outpost something you build and then stop
thinking about.

1. **Veins are long.** A chunk holds roughly forty hours of continuous T1 extraction before
   reaching its floor. The reserve is sized in hours against a reference rate rather than picked
   as a round number, so the figure stays meaningful when rates are tuned.
2. **The decay curve is gentle early and steep late**, not linear. A quarter of the reserve gone
   still yields 96 % of the original rate and half yields 83 %; the slowdown only becomes obvious
   past three quarters. Linear decay is noticeable from the first hour and makes a player feel
   permanently on a clock, which is the opposite of the intended rhythm.
3. **The floor is 30 %**, and a **Deep Bore** upgrade raises it — 45 % with one, 60 % with two,
   capped at 75 %. A depleting outpost therefore has two answers instead of one: deepen it, or
   found another.

**Richness scales reserve as well as rate**, which makes a vein's lifetime independent of its
richness. A rich chunk is a find rather than a countdown, and the player is never asked to weigh
"rich but short" against "poor but long" — a false choice that adds arithmetic without adding a
decision.

**Alternatives rejected.** A token floor around 10 % (an old outpost becomes a monument rather
than a contributor, and the treadmill returns); no floor at all (every extractor eventually
becomes litter a player has to go and tidy up, which is a chore); infinite veins with no decay
(removes the reason to expand, and the horizontal pressure is the part of this design worth
keeping); making relocation cheap through better build tools (treats the symptom — the problem is
being asked to re-solve a solved layout, not how long it takes).

**Consequences.** The Deep Bore cap is what keeps the decision alive. Without it a stack of
upgrades would make one chunk effectively infinite and expansion would stop being necessary at
all; with it, deepening buys time rather than permanence, and territory still matters.

Deep Bore buys nothing at a fresh vein, which is deliberate — it has to be a mid-game answer to a
mid-game situation rather than something installed reflexively on day one, so it costs an upgrade
slot and constant upkeep for no benefit if fitted early.

The risk to watch is pacing. Forty hours is long enough that a player may never see depletion in a
short playthrough, which makes the Deep Bore upgrade dead content for them. That is the right way
round — far better than the alternative — but if testing shows the depletion curve is never
experienced at all, the reserve comes down rather than the floor.

---

## ADR-0048 — Assets are generated from the JDK, with a named list of what cannot be

*2026-10-02 · Accepted*

**Context.** The README has committed since day one to original, script-generated assets, and gave
good reasons: consistency across a hundred-plus blocks, a palette change as a one-line edit, and
unambiguous provenance. What it never did was establish whether that is actually *achievable*, or
what happens to the parts it cannot cover. Art is the largest unexamined risk in the project — a
mod is judged on its first screenshot long before anyone reads its recipe graph.

This was investigated rather than assumed. The findings changed the plan twice.

**Finding 1: there is no Python and no Node on the toolchain.** A generator written in either
would add a dependency the project does not otherwise have, on a machine that cannot currently run
it.

**Finding 2: the JDK is enough, and that is better anyway.** `javax.imageio` writes PNG and
`javax.sound.sampled` writes WAV with no third-party library at all. A generator in Java uses the
toolchain the project already requires, can run as a Gradle task, and is checked by the same
compiler as the mod.

**Finding 3: audio is generatable, which contradicted the initial assessment.** Sound was written
off as impossible before being tried. A probe produced a seamless one-second machine hum (summed
harmonics at exact multiples of the loop frequency, so the ends join without a click) and a pylon
link chirp (a rising sweep under a decaying envelope). Synthesised industrial sound — hums, clicks,
relays, charge-ups, alarms — is squarely within reach. It is *tonal* sound that is hard, not sound.

**Decision.** Generate assets from a committed Java tool, run by Gradle, covering:

- **the form × material matrix** — ingots, plates, gears, dusts and the rest of ADR-0032's axis,
  across every material the pack provides. This is the bulk of the catalogue by count and the part
  where consistency *is* the quality;
- **machine faces and casings** — composed from a base plate, a recessed panel, vents, rivets and
  an emissive overlay, with active and idle variants;
- **block models and blockstates**, which are declarative JSON;
- **sound effects** of the industrial kind: hums, charge-ups, relay clicks, alarms.

And name, explicitly, what this approach **will not** produce to a shippable standard:

| Not generated | Why |
| --- | --- |
| Hero item sprites — the Multitool, the Research Terminal, the Prospector's Scanner | A script yields something legible, not something with character. These are what a player sees in the first screenshot. |
| Complex models — drone arms, turbines, the Space Elevator | Blockbench work. A generator produces boxes. |
| Entity animation | Keyframes are authored, not derived. |
| Music, and any voiced or recorded sound | Synthesis covers industrial noise, not melody or recording. |
| Particle effects that look *good* | The code is writable; judging the result needs eyes on it running. |

**Alternatives rejected.** Generating everything and accepting the result (the catalogue would be
consistent and characterless, and first impressions are the whole marketing budget of a mod);
hand-authoring everything (hundreds of form × material cells is exactly the repetitive work this
project refuses to inflict on *players*, and it would not survive a palette change); vendoring
another mod's assets or anything under an unclear licence (not negotiable — the README's provenance
claim is a promise, and third-party art would break it).

**Consequences.** The generator is written **early rather than at step 19**. If the approach is
going to fail it should fail at twenty blocks, not at three hundred, and the texture style
constrains block model design — discovering that late would mean redoing both.

The list above is the real output of this record. A future session can now see at a glance that
hero art and complex models are *known gaps with no owner*, rather than assuming assets are
handled because a generator exists. Those gaps are where a human artist would be worth finding;
everything else is deliberately not worth an artist's time.

Any third-party asset that ever enters the repository must be CC0 or equivalent, with its source
recorded. Sound effects sourced rather than synthesised fall under the same rule.


---

## ADR-0049 — A green build must include a booted server

*2026-10-02 · Accepted*

**Context.** Until now "verified" meant two things: `./gradlew build` succeeds, and the behaviour
checks in `tools/checks` pass. Both were true — 309 checks, a 25 second build — while the mod could
not start. `ModCreativeTabs.register()` called `get()` on registry objects during mod construction,
before the registry was populated, and Forge refused to create the mod instance with
`Registry Object not present: grindless:multitool`.

Neither existing gate can see that class of bug, by design. The checks run `common` classes in a
plain JVM, which is why they need no Minecraft; the build only compiles. Nothing instantiates
Forge, so nothing exercises registration order, capability attachment, `@ExpectPlatform`
resolution or anything else that only exists once the loader is running.

A second bug of the same family was found by running the game rather than reading it: a pylon's
network membership was mirrored in a transient `registered` flag, restored by a first-tick hook
that pylons never reach because they have no ticker. After a world reload the flag read false, so
breaking the pylon did nothing and the network kept a ghost. It passed every check, because the
checks never reload a world.

**Decision.** A change is not verified until the dedicated server has booted with the mod loaded.
`tools/smoke-boot.sh` starts `:forge:runServer` headless, waits for `Done`, sends `stop`, and fails
on `Mod Loading has failed` or an early exit. CI runs it after the build and the checks, on Linux.
It writes `eula.txt` only when `GRINDLESS_ACCEPT_EULA=true` or `CI=true`, because that records
acceptance of the Minecraft EULA on the caller's behalf.

State that must survive a reload lives in saved data and nowhere else. A block entity may cache
it, but must be correct with an empty cache, because a block entity is rebuilt on every chunk load
and its first-tick path is not guaranteed to run.

**Alternatives rejected.** Forge GameTests (the better long-term home for reload scenarios, but
they need infrastructure the project does not have yet, and the smoke test catches the failure
that matters today for thirty lines of shell); relying on the client to find it (a person has to
launch a game to learn the mod does not load, which is the situation this record exists to end);
adding `get()` guards at each call site (treats one symptom — registration order — and leaves the
class of bug unobserved).

**Consequences.** CI needs a Linux job with JDK 17 and a few minutes for the first remap. The
script also makes it cheap to run the same check locally on Linux and macOS; on Windows it runs
under WSL or Git Bash. `tools/run-checks.sh` is the Linux counterpart of `tools/run-checks.ps1`, so
the checks are no longer Windows-only. Reload scenarios still need a person or a future GameTest;
the smoke test proves the mod boots, not that every system survives a restart.

---

## ADR-0050 — Grindless supplies a material only where the pack has none

*2026-10-02 · Accepted*

**Context.** Grindless must work beside vanilla and any other mod, not merely tolerate them. Tin
should be the tin a pack already has; copper and iron should be vanilla's; platinum should come
from whichever mod adds it, and from Grindless only when none does. Recipes must be satisfiable by
another mod's equivalent item, and installing Grindless must not overwrite or duplicate anything.

ADR-0004 and ADR-0033 decided that materials are resolved from tags. They left three things open
that this record closes: items must be registered before any tag is read, so "register only if
missing" cannot be literal; the conventions are not equally followed (`forge:ingots/tin` is
universal, `forge:bolts/tin` does not exist); and nothing said how a recipe's output is chosen when
several mods provide the item.

An audit found the shipped assets already broke the rule: the generator produced textures for
vanilla's own iron ingot, gold ingot, gold nugget and copper ingot.

**Decision.** Split by *slot* — one material in one form — and decide each slot at two times.

At registration, a fixed **supply set** (`SupplyCatalogue`) is registered whatever else is
installed. Registry IDs are written into worlds, so they must not depend on the mod list, or
removing a mod would turn every Grindless item in a save into a missing mapping. The set never
includes what vanilla already has: no iron ingot, gold ingot, copper ingot, iron or gold nugget,
or raw iron, copper and gold. Vanilla gets Grindless items only for what it lacks (iron plates,
copper nuggets). Alloys get no raw or crushed form. This is the one hardcoded list in the mod and
it lists *what Grindless can make*, not *what exists*.

At runtime, after every tag load, `MaterialRegistry` scans the tags and records, for each slot,
which items fill it. A supply item is **active** only if no other namespace fills its slot.
Inactive supply items stay registered but are hidden from the creative tab, are never chosen as
output, and (when veins arrive) are never used to make a material mineable.

Four rules follow.

1. **Inputs are tags.** A recipe ingredient that is a material in a form is `forge:ingots/tin`,
   never `othermod:tin_ingot` (`MaterialTags`). `VerifyMaterial` lints every shipped recipe and
   fails on a hard-wired item ID, and has a self-test showing the linter can fail.
2. **Outputs are unified.** One item per slot: vanilla, then namespaces the pack author names,
   then other mods alphabetically, then Grindless last (`Unifier`). Deterministic, so server and
   client agree and the choice is stable across reloads.
3. **Add, never replace.** Every shipped tag is `"replace": false` and lists only Grindless items
   or sibling tags; nothing is shipped into `minecraft:`. Grindless can add to another mod's tag
   but cannot remove from it. Checked in `VerifyMaterial`.
4. **Conventional forms are shared, the rest are ours.** A form is published under `forge:` only if
   other mods agree on it (ore, raw, dust, nugget, ingot, gem, storage block, plate, rod, gear).
   Crushed, purified, foil, bolt, ring, wire and coil have no agreement, so they live under
   `grindless:` — published so others can opt in. This is the boundary ADR-0033 drew for reagents,
   applied to forms. Promoting one is a single flag in `MaterialForm` plus a regeneration.

The generator reads the same `SupplyCatalogue`, so a texture, model or tag can neither exist for an
item that is not registered nor be missing for one that is. Item names come from two lang keys per
item (`form.grindless.ingot` and `material.grindless.tin`), so a new material costs one line of
translation.

**Alternatives rejected.** Registering conditionally on `ModList` (breaks saves when the mod list
changes); registering nothing for materials and only tagging (an exotic material would not exist
with Grindless alone, contradicting the requirement); replacing other mods' recipes or tags with
unified ones (the kind of override this record exists to forbid); a per-pack config mapping
(ADR-0033 rejected it for reagents for the same reasons); one `forge:` tag for every form
(claims tags nobody else fills, so the form is never interchangeable and the claim is a lie).

**Consequences.** With only Grindless installed there are 120 supply items and all are active.
With another mod providing tin ingots and raw platinum, 118 are active and the other two are
hidden; this is demonstrated by booting a server with a datapack standing in for that mod
(`tools/smoke/foreign-providers`, `SMOKE_DATAPACK` in `tools/smoke-boot.sh`).

Known limits. Hiding in recipe viewers needs a JEI/REI integration that does not exist yet.
`c:` tags (Fabric) are not scanned, consistent with ADR-0039. The unifier's namespace preference
has a setter but no config file until `GrindlessConfig` is real. A vanilla-backed material is
listed by hand in `SupplyCatalogue`, so a Minecraft update that adds a vanilla form, such as a
copper nugget, needs a one-line edit and a regeneration; the check `no registered name equals a
vanilla item's name` guards the known cases. A player holding a hidden supply item (from `/give`
or an old chest) still has a working item, since it is tagged, which is intended.

---

## ADR-0051 — Every block ships a floor of assets, generated from one list

*2026-10-02 · Accepted*

**Context.** Booting the server proved the mod loads, not that its blocks work. Auditing the
resources found none of the six registered blocks had a blockstate or a model (the missing-texture
cube), none had a loot table, and none was in a mining tag. Because machines set
`requiresCorrectToolForDrops`, a player could place one and then destroy it for nothing. Three
hand-registered items had no model either, and the Multitool and Data Core had no sprite.

**Decision.** `BlockCatalogue` lists each block, its model shape and its decorated face. The
generator writes, from that list alone, the blockstate, block model, item model, a self-drop loot
table, and the entry in `minecraft:mineable/pickaxe`. Two shapes exist: an orientable casing with
one decorated face, and a column with the decorated face on all four sides. Blocks have no facing
property yet, so the decorated face is always north; that is a placeholder, not a design. The
Multitool and Data Core get generated placeholder sprites, and the Machine Casing item renders as
the casing cube.

`VerifyAssets` reads `ModBlocks` and `ModItems` and fails if a registered block lacks any of those
files, if any model names a texture or parent that does not exist, or if a hand-registered item has
no display name or model. A server-boot scenario (`tools/smoke/blocks.commands`) places every block
and checks that it is in the pickaxe tag and that its loot table drops one item with its real
name.

The pickaxe tag is the only thing Grindless ships into `minecraft:`. It is `"replace": false` and
lists only Grindless blocks, so it can add to vanilla's tag and cannot remove from it. This narrows
rule 3 of ADR-0050 ("nothing is shipped into `minecraft:`") to "nothing except mining tags", and
`VerifyMaterial` now enforces the narrower form. Vanilla has no other mechanism: a block is
mineable with a tool only by being in that tag.

**Alternatives rejected.** Hand-writing the JSON (six blocks now, dozens later, and the
catalogue-versus-registry drift this removes); generating the files from `ModBlocks` itself (it
needs Minecraft and a loaded registry, and the generator deliberately needs only a JDK, ADR-0048);
leaving it to the first person to launch the client (the situation ADR-0049 exists to end).

**Consequences.** Every block renders, drops itself and mines with a pickaxe. Not covered, and
still named gaps under ADR-0048: hero models, facing and active-state variants, block-entity
renderers. Rendering itself was not run: the checks prove the files exist and agree, and the server
proves tags and loot, but only a client shows the pixels. No tool requirement tier is set, so any
pickaxe works; whether the Multitool should mine is a design question this record does not answer.


## ADR-0052 — Each machine has its own shape, state textures and effects

*2026-10-02 · Accepted*

**Context.** ADR-0051 gave every block a floor of assets, but all machines were the same casing cube
with a different face, and none showed what it was doing. `docs/MACHINES.md` already defines five
machine states (running, idle, blocked, starved, out-of-band); nothing exposed them to the player.

**Decision.**

1. *State is a block-state property.* `MachineStatus` (idle, running, blocked, starved,
   out_of_band) is the `status` property; `facing` is added where the shape has a front. The light
   level derives from the status. Changing a block state is a client-visible update, so every
   publisher goes through `StatusDebounce`: entering running or a fault is immediate, leaving
   running waits 40 ticks and recovering from a fault waits 20, so a machine flickering between two
   conditions does not spam updates.
2. *Colours mean the same on every machine; shape and accent colour say which machine it is.*
   Running is cyan, blocked is amber, starved is red. Each geometry (dynamo, extractor, terminal,
   pylon with three tiers) has its own element model and its own painter in `tools/assetgen`.
3. *Effects are client-only, driven by `animateTick`.* `MachineEffects` spawns particles and plays
   one-shot sounds per geometry and status. It references no client class, so `common` stays
   server-safe. Sounds are mono Ogg Vorbis so Minecraft can position them in 3D; the source WAVs
   live in `tools/audio` and `tools/convert-audio.sh` produces the shipped files, so they never
   need hand editing.
4. *The generator stays JDK-only and is driven by `BlockCatalogue`*, which now lists geometry, tier,
   facing and statuses per block. `VerifyAssets` checks every state, rotation and sound.
5. `pack.mcmeta` moves to the `forge` module resources: in a dev run only that module's resources
   form the mod pack, and the old location produced a "failed to load a valid ResourcePackInfo"
   warning.

**Consequences.** The hand-crank dynamo reports running, idle and blocked from its own behaviour,
and flux pylons report the state of their network (the network tick writes a pylon only when its
debounced status is stale). The crude extractor and research terminal are `MachineShellBlock`s:
they have every model, texture and effect, but nothing sets their status yet, because they have no
behaviour. Starved pylon and out-of-band have no producer either; the former needs consumers, the
latter has no visuals. Effects are sparse one-shots, not looping sounds, and a pylon in an unloaded
chunk keeps its last status until the chunk loads. Face lighting is not emissive: only the block
light level changes. A client run (software GL under Xvfb) showed the models and textures
rendering with no model errors in the log; it did not demonstrate the particles over time.


## ADR-0053 — The Crude Extractor is the first consumer

*2026-10-02 · Accepted*

**Context.** The Hand Crank Dynamo generated power and the Flux Network could carry it, but nothing
consumed it. `docs/PROCESSES.md` already specified the T0 extractor: F0, twenty seconds, one unit
of the chunk's material. Veins, depletion and Deep Bore already had checks. The extractor was a
shell with art and no behaviour.

**Decision.** The Crude Extractor is a `MachineBlockEntity` that:

1. Resolves its chunk's vein from the world seed and `MaterialRegistry.snapshot().mineable()`.
2. Draws 8 FU/t from a covering Flux Network, or from its own buffer when an adjacent dynamo
   pushes into it. T0 has no pylons, so adjacency is the bootstrap path.
3. Progresses a 400-tick cycle scaled by `vein.rateAfter` (richness and depletion) and by how much
   of the requested power actually arrived (brownouts slow it, they do not stall it).
4. Outputs the pack's preferred raw form, or the ore if the pack has no raw, via `Unifier`.
5. Holds one output slot that hoppers can pull from and that the machine pushes into neighbouring
   inventories (vanilla `Container` first, Forge item capability otherwise).
6. Publishes status through the same debounce as every other machine: idle with no vein, starved
   with no power, blocked when the product cannot leave, running otherwise. A vein never idles
   from exhaustion — that would contradict ADR-0047.

Work is subscribed while there is a vein and the output is not stuck, and dropped the moment it
is: an extractor next to a full chest costs nothing until a hopper or a neighbour change frees a
slot. `ExtractorLogic` holds the numbers so `VerifyExtractor` can check them without a world.

**Consequences.** The first playable loop exists: crank a dynamo next to an extractor, or cover
both with a pylon, and put a chest on any face. The Terrestrial Extractor (T1, faster, network-only)
is still ahead. The extractor has no menu yet; hoppers and auto-push are the interface.


## ADR-0054 — Pylons are three blocks tall and cover a factory

*2026-10-02 · Accepted*

**Context.** Pylons were a one-block post with a 16/32/64-block supply cube. That is a lamp post,
not a power pylon, and one MK1 could not cover a factory floor. Minecraft JSON models cannot
extend past 32 units on an axis, so a three-block-tall model cannot hang off a single block.

**Decision.**

1. A pylon occupies three blocks of height. The player places the base; two `flux_pylon_shaft`
   blocks occupy y+1 and y+2. The shafts have no item, drop nothing, and breaking one breaks the
   base, which drops the pylon. Only the base is a network member.
2. Each third has its own model, sliced from a 48-unit tower, because JSON cannot represent the
   whole thing on one block. The item uses the tower scaled into 16 units.
3. Shafts copy the base's tier and status so the lights stay in step. Coverage, throughput and
   link range still key off the base position.
4. Supply cubes grow to a factory scale: MK1 48³ / link 64, MK2 80³ / link 112, MK3 128³ / link 192.
   Throughput is unchanged. The shorter of two link ranges still governs, so an MK3 cannot capture
   an MK1 that cannot reach back. The cube-per-axis rule is unchanged; a player can still see the
   coverage by eye.

**Consequences.** An MK1 covers a small factory. An MK3 covers a chunk-scale base. The spatial
index files an MK3 under at most 81 chunk buckets, which is still local density rather than world
size. Existing worlds with 1-block pylons will miss their shafts until the pylon is broken and
replaced; membership is unchanged.


## ADR-0055 — The Multitool does not mine

*2026-10-02 · Accepted*

**Context.** ADR-0051 put every machine in `minecraft:mineable/pickaxe` and left open whether the
Multitool should mine them. The Multitool is the T0 handheld that replaces the stone-tool *phase*,
not the pickaxe.

**Decision.** The Multitool is not a mining tool. Machines drop only for a pickaxe (or another
item in `minecraft:mineable/pickaxe`). The Multitool stays a scanner, configurator and progression
item. Giving it mining would collapse "requires the correct tool" into "the one item does
everything", which is how the early game becomes a single hotbar slot instead of a first factory.

**Consequences.** Players keep a pickaxe. If a later session wants a mining module on the
Multitool, it is an upgrade with a cost, not the default.


## ADR-0056 — T0 bootstrap recipes are authored JSON using tags

*2026-10-02 · Accepted*

**Context.** ADR-0005 says processing-chain recipes are generated at runtime from the material
registry, not shipped as JSON. The T0 loop — dynamo, extractor, multitool, terminal, Data Core —
does not depend on that registry. It is cobblestone, wood and two iron, and it is the only part of
the graph a player hand-crafts (ADR-0017). Leaving it uncraftable meant the "ten minutes, two iron"
claim was creative-mode only.

**Decision.**

1. T0 crafting-table recipes are authored JSON under `data/grindless/recipes/`. They are the
   exception to ADR-0005, not the start of shipping the processing chain as files.
2. Anything that is a material is a tag (`forge:ingots/iron`), never `minecraft:iron_ingot`.
   Stone is `minecraft:stone_crafting_materials`, wood is `minecraft:planks`. Vanilla items that
   are not material forms (stick, glass, redstone) may be named by ID.
3. The iron budget is two: one in the dynamo, one in the extractor. The Multitool is cobble and
   sticks — it replaces the stone-tool *phase*, so it is craftable before the first iron.
4. `BootstrapRecipes` lists the same recipes as data so `VerifyBootstrap` can check the JSON
   against the decision.

**Alternatives rejected.** Generating even these five at runtime (the injector needs mixin or
recipe-manager mutation, and the recipes do not change with the pack); hard-wiring
`minecraft:iron_ingot` (a pack's iron would not craft the bootstrap, contradicting ADR-0050).

**Consequences.** A survival player with a crafting table, cobble, wood and two iron can build the
loop. T1 recipes stay unwritten until those machines exist; they will be gated on the Voltaic
blueprint (ADR-0057). Processing-chain recipes remain generated (ADR-0005).


## ADR-0057 — The Research Terminal unlocks world-scoped blueprints

*2026-10-02 · Superseded by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded.** A world-scoped permission no longer unlocks ordinary machines. Physical
> Control Matrices carry tier progression. The block returns later as a Research Station for
> reusable advanced-process patterns. See ADR-0107.

**Context.** The Research Terminal was a `MachineShellBlock`: art, no block entity, no behaviour.
The README has the player insert Data Cores and Flux to unlock blueprints, and has research as a
*production target* the factory feeds, not a GUI timer. T1 machines do not exist yet, so an unlock
cannot grant a recipe today — but a terminal that does nothing until step 20 would leave T0
without its progression gate.

**Decision.**

1. The terminal is a `MachineBlockEntity` at F0. One Data Core and 600 ticks at full power unlock
   Voltaic, the T1 pack. Brownouts slow it; they do not stall it and they do not consume the core
   early.
2. Unlocks are world-scoped `SavedData` on the overworld, not per-player. A hopper can feed cores,
   which is what "research is a production target" requires. Unlocking twice is a no-op.
3. There is no menu. Right-click with a core inserts it; empty-handed takes it back. Hoppers may
   insert and may not extract — the core is spent, not buffered.
4. Status: idle with no core, starved with no power, running while the cycle advances, blocked
   once Voltaic is already unlocked. The same debounce as every other machine.
5. Voltaic currently gates nothing. That is deliberate: the switch flips now, T1 recipes attach
   later. A flag nobody reads is still a real unlock; a terminal that waits for the recipes is a
   shell.

**Alternatives rejected.** Per-player research (a hopper has no player); a full tech-tree GUI
(step 20; the T0 loop does not need it); consuming the core on insert (a brownout would then
waste the core with no unlock).

**Consequences.** The first playable progression exists: craft a Data Core, power the terminal,
wait thirty seconds, Voltaic is on. T1 crafting will read `ResearchData.isUnlocked(VOLTAIC)`.
Existing worlds have an empty set, which is locked, which is correct.


## ADR-0058 — Build playable slices, not system layers

*2026-10-02 · Accepted*

**Context.** The implementation plan after T0 listed remaining work as *systems in dependency
order*: `ProcessRecipe` (15), all T0/T1 machines and menus (16), fluids (17), JEI (18),
fabrication (20), belts (21), conduits (22), multiblocks (23). Step 11 had already deferred
`ProcessRecipe` until fluids existed, because "half the ingredients are fluid stacks".

That order builds a complete energy API, a complete recipe type, a complete fluid model and a
complete multiblock framework — each of which is empty of a factory that uses it. The 0.1
definition of done is narrower and already written: *a player can go from an empty world to
automated iron in under fifteen minutes without mining it by hand.* T0 currently stops at raw ore
in a chest. The missing piece is a **dry processing line**, not a catalogue.

The T1 floor in `PROCESSES.md` is B0×R1 and B1×R1: raw or crushed into an ingot in an Arc Furnace,
optional Pulverizer doubling, no water, no acid, no hatches. F0 crank power cannot run F1
machines. Pylons already cover a factory; conduits are the named-network *alternative*, not the
way power exists. Every T1 machine is a single block; the first parametric multiblock in
`MACHINES.md` is T3.

**Decision.** Remaining work is scheduled as playable slices. A slice ships when a player can do
a new thing in-world, not when a layer is "complete".

1. **Slice A — First iron (0.1).** `ProcessRecipe` with item I/O now and fluid I/O as an empty
   slot (the type is one union, not a rewrite later). Generate only B0×R1 and B1×R1. Thermal
   Generator (furnace fuel → F1) and a Voltaic-gated Pylon MK1 recipe, so the player can walk
   away from the crank. Pulverizer and Arc Furnace as real consumers. One shared machine menu
   (energy, slots, named fault). Voltaic gates those recipes. Hoppers and chests are the
   logistics. Gaseous byproducts (CO from R1) **vent to atmosphere** until slice C — that is the
   named T1 sink (ADR-0036), not a missing tank. `VerifyRecipes` dumps the generated graph so
   generation is testable without JEI.
2. **Slice B — First factory (0.2).** Belts, splitter, manipulator. Terrestrial Extractor.
   Prospector's Scanner. Kiln can wait until gas is capturable.
3. **Slice C — Fluids.** Volume, temperature, pressure, Clay Conduit, Hand Pump, Basic Tank.
   ProcessRecipe fluid slots go live. Wet pulverizer, gas capture. This is the second line, not
   the first.
4. **Slice D — The factory builds the factory.** Press, Machine Casing, Assembler. Fabrication
   gate for T2+ (ADR-0017). 0.1 does not need an Assembler.
5. **Slice E — Energy spanning.** Flux Conduits, transformers, capacitor banks. Pylons already
   did coverage; this layer is named networks and distance, and must not obsolete pylons
   (ADR-0025).
6. **Slice F — Multiblock kernel.** A `MachineBlockEntity` plus a formed structure and hatches
   that are sided containers. Implemented when the first machine *needs* hatches or size (T2/T3),
   not as a framework waiting for content. Single-block T1 machines stay single blocks. Parametric
   reactors, distillation height and construction drones stay with that content.

The numbered plan rows 15–26 are rewritten to match these slices. Historical rows 1–14b do not
move. CI (old row 26) is already green and is marked done.

**Alternatives rejected.** Finishing every T1 machine before any recipe (shells with nothing to
run — the Research Terminal already taught that lesson); fluids before first iron (the reason
`ProcessRecipe` sat on the shelf, and B1×R1 does not need water); the conduit layer as the energy
route (the route is Thermal Generator + pylons; conduits are a later alternative); the parametric
multiblock framework before a machine that has a size parameter (an unused kernel that T1 machines
would then be forced to pretend to be); reaching the Assembler as part of 0.1 (the definition of
done is automated iron, and the Assembler is the last crafting-table recipe, not the first ingot).

**Consequences.** The next session implements slice A, not "T1 machines" as a set and not
"ProcessRecipe + fluids" as a pair. A successor that starts a multiblock framework or a conduit
network before first iron is reopening this record. `ProcessRecipe` may ship without a fluid stack
implementation; adding fluids later fills the slot, it does not fork the type. Vented gas is
deliberate T1 behaviour and becomes a capture decision in slice C, which is how R2 (roast for
SO₂) starts.


## ADR-0059 — First iron is a generated graph and a Voltaic gate

*2026-10-03 · Accepted*

**Context.** ADR-0058 scheduled slice A: `ProcessRecipe` item-first, B0×R1 and B1×R1, Thermal
Generator, Pulverizer, Arc Furnace, one shared menu, Voltaic-gated T1 crafts including Pylon MK1.
Three implementation questions were still open.

Forge `ICondition` is evaluated when recipes *load*, not against a world. Voltaic is world-scoped
(`ResearchData`). Binding T1 crafts to a datapack condition would unlock them for every world
as soon as anyone researched them in any world in the same JVM, or never, depending on when the
condition ran.

Vanilla `RecipeType` is the usual home for machine recipes. Half of a later process is fluids
(ADR-0015), and slice C has not been written. Putting item-only recipes on `RecipeType` now
would force a rewrite when tanks arrive, which is the pairing ADR-0058 rejected.

**Decision.**

1. Processing recipes are a generated graph (`ProcessGraph` / `ProcessLookup`), rebuilt with
   `MaterialRegistry`, indexed by family and primary input tag (ADR-0005, ADR-0043). They are not
   a vanilla `RecipeType`. Fluid slots exist on the record and stay empty except for vented CO.
2. T1 crafting-table recipes are authored JSON of type `grindless:gated_shaped`. `matches`
   consults `ResearchData` on the server and `ClientResearch` on the client. Unlock is synced on
   login and when the Research Terminal flips the flag. Forge conditions are not used.
3. Carbon is `#grindless:carbon`, currently `#minecraft:coals`. Slag is a Grindless reagent item,
   not a material form (ADR-0033). CO from R1 is a vented fluid output — the named T1 sink
   (ADR-0036), not a missing tank.
4. The Thermal Generator, Pulverizer and Arc Furnace share one menu. Hoppers and neighbouring
   inventories are the logistics. The Arc Furnace holds T 1500 reducing so R1 evaluates optimally
   without a player setting a dial.
5. `VerifyRecipes` dumps the generated graph and checks the gated JSON against `T1Recipes`.

**Alternatives rejected.** Shipping B0×R1 as furnace JSON (it cannot name carbon, slag or CO);
gating with Forge conditions (wrong lifetime); a vanilla `RecipeType` for two families that
would be forked when fluids arrive; a per-machine screen (the point of the shared menu is that
the next machine does not invent a new UI); capturing CO in slice A (there is nowhere to put it).

**Consequences.** Slice B can add belts without touching the graph. Slice C fills the fluid
slots and turns the CO vent into a capture decision. A successor that adds JEI walks
`ProcessLookup.recipes()`, not the vanilla manager. A successor that adds a second blueprint
extends `Blueprint` and writes another gated JSON.


## ADR-0060 — First factory is lane data, a survey and an unpowered belt

*2026-10-03 · Accepted*

**Context.** ADR-0058 scheduled slice B: belts, splitter, manipulator, Terrestrial Extractor,
Prospector's Scanner. Hoppers already move items. The Crude Extractor already pulls a vein
without a survey. Three questions were still open.

Belt mods usually spawn an `ItemEntity` per item. ADR-0008 already forbade that; the
implementation still had to pick a packing and a tick. Factorio's yellow belt is 15 items/s
with four items per tile per lane. Grindless's Conveyor is specified at 8 items/s.

The Terrestrial Extractor is "placed in a surveyed chunk". The Crude Extractor cannot require
that — T0 has no scanner. A survey that is only a chat message would let the T1 extractor
run anywhere, which deletes the scanner.

The splitter's interesting behaviour is filter plus priority. A full GUI is a second menu
family. Sneak-clicking a face with an item is enough to name a filter; a matching filter
already *is* a priority over an open face.

**Decision.**

1. Conveyor contents are two `Lane`s of `{id, count, position}` on the tile. Four slots per
   lane at 8 items/s is one tile per second. The tile ticks once and advances the lanes;
   items become stacks only at an endpoint (ADR-0008). Hoppers insert from above and pull
   from below. A `BeltEndpoint` is how a manipulator or a neighbouring belt talks to one.
2. The Crude Manipulator is unpowered and moves one item a second. The first inserter has
   to work the moment the first belt does.
3. The splitter has one input (the back) and three outputs (front, left, right). Sneak-click
   a face with an item to filter it; empty hand clears. A matching filter beats an open
   face; equal priority round-robins. Merger, tunnel, sorter and overflow wait.
4. The Prospector's Scanner marks a 3×3 of chunks in `SurveyData`. The Terrestrial Extractor
   draws F1, takes five seconds per unit at richness 1 (the rate `VeinGenerator` sizes
   reserve against), and is `OUT_OF_BAND` until its chunk is surveyed. The Crude Extractor
   does not read the set.
5. All five crafts are Voltaic-gated JSON. `VerifyBelt` drives the lanes and the router
   without a world.

**Alternatives rejected.** Entity-per-item belts (ADR-0008); a powered first belt (the
opening would still be a hopper line); requiring a survey on the Crude Extractor (T0 has
no scanner); a survey that does not persist (the T1 extractor would ignore it); a second
menu family for three filters (sneak-click is the whole interesting verb).

**Consequences.** Slice C can add fluids without touching the lanes. A faster belt is
another `BeltLogic` constant and a new block, not a new representation. A successor that
spawns `ItemEntity`s on a belt is reopening ADR-0008.

---

## ADR-0061 — The machine-state smoke runs as one function

*2026-10-03 · Accepted*

**Context.** ADR-0049 requires a booted server. The states scenario then `setblock`s every
facing and status and `say`s `STATE-OK` if the block is still that state. `tools/smoke-boot.sh`
sends each console line a second later so hoppers have time to tick.

A ticking machine does not keep a status the world did not earn. An empty belt publishes
`idle`. A Crude Extractor over a chunk vein with no power publishes `starved`. A Terrestrial
Extractor in an unsurveyed chunk publishes `out_of_band`. After a one-second gap the
`execute if` is testing the live loop, not whether the blockstate exists. GitHub run
37154213057 missed ten `STATE-OK` lines that way: the first idle of each extractor, every
belt `running`, every manipulator `running`. Later idles of the same block type passed only
because `MachineDisplay` would not republish a status it had already shown, so the
`setblock` stuck. That is a debounce accident, not coverage.

**Decision.** `GenerateAssets` writes `tools/smoke/states-pack` with one function that
places every state and `say`s in the same tick, before block entities run. CI enables that
datapack and sends `forceload` then `function grindless:states`. Hopper, belt-feed and
loop smokes stay as one console line per second; they are waiting for the world to work.

**Alternatives rejected.** Dropping the 1s pause for every smoke (hopper inserts would
lose the race the other way); expecting only the block id and ignoring status (a missing
variant would still boot); stopping machines from publishing during smoke (the live loop
is correct; the test was asking the wrong question).

**Consequences.** Adding a machine state is still one `BlockCatalogue` row. A successor
that inlines the 315 console commands to "match the other smokes" is reopening this
record.


## ADR-0062 — First fluids are millibuckets, gravity clay and a named CO sink

*2026-10-03 · Accepted*

**Context.** ADR-0058 scheduled slice C: volume, temperature and pressure, Clay Conduit, Hand
Pump, Basic Tank, live `ProcessRecipe` fluid slots, a wet pulverizer, gas capture. ADR-0015
already forbade modelling fluids as items. Three questions were still open.

`PROCESSES.md` quotes volumes in buckets `B`. Forge and vanilla use millibuckets. Recipe
counts have to pick one unit and stick to it, or wet B1 (0.5 B water) and R1 (1 B CO) will
disagree with a tank that thinks 1 is a bucket.

The Chemical Washer is B2 and T2. A T1 wet mill that produced slurry would have nowhere to
put it (ADR-0036). Dry B1 already doubles raw into crushed. Water at T1 is a fluid-slot
proof, not a new yield.

R1 names 1 B of carbon monoxide. Slice A vented it as the named T1 sink. Slice C has a tank,
so the sink can become a capture *or* a burn. Blocking the Arc Furnace until a tank takes
the gas would stall first iron the moment the player built the second line.

**Decision.**

1. Volume is millibuckets internally. 1000 mB is one `B`. Recipe fluid counts are mB:
   wet B1 takes 500, R1 names 1000 of CO. Temperature and pressure live on the stack
   (`FluidState`). Mix weights both by volume. Empty is zero millibuckets, not a null.
2. The Clay Conduit is unpowered, ambient liquid, level or downhill. Gases need pressure
   a T1 pipe does not have. Horizontal placement is the T1 facing; `toY <= fromY` is the
   rule so a later vertical piece does not invent a new check.
3. The Hand Pump is unpowered. It reads a vanilla water *source* on the face it points at
   or the block below, and does not drain it. 20 mB/t is a bucket every 2.5 s.
4. The Basic Tank holds 16 B at ambient. Anything hotter than 40 °C is refused. Gases at
   0.1 MPa are accepted — that is how captured CO waits to be burnt.
5. Capture is a push. The Arc Furnace offers its 1 B of CO to neighbouring
   `FluidEndpoint`s; whatever they refuse vents. The furnace never blocks on gas. The
   named sink for captured CO is the Thermal Generator: 1 B burns 400 ticks of F1
   (a quarter of a coal). Venting to atmosphere remains a legal sink.
6. Wet B1 is the same crushed yield as dry B1 plus 0.5 B water. No slurry, no byproduct.
   When the mill already holds enough water, the wet recipe wins; otherwise dry runs.
7. Forge `FLUID_HANDLER` is attached by event (ADR-0045). `FluidStack` has no T/P:
   export drops both, import is ambient. Carbon monoxide has no registered vanilla
   `Fluid`; it stays on `FluidEndpoint` and is invisible to other mods' pipes.

**Alternatives rejected.** Recipe counts in buckets (0.5 is not an int); a T1 slurry
(no washer); blocking the furnace on uncaptured CO (first iron would stall); registering
CO as a vanilla fluid in this slice (buckets, stills, textures, and a gas that clay
cannot move); putting T/P on the Forge stack (Forge does not have those fields).

**Consequences.** Slice D can add the Press without touching millibuckets. A successor
that stores fluids as items is reopening ADR-0015. A successor that makes wet B1 a
different yield from dry is inventing the washer a tier early. Kiln / R2 wait until
the player has somewhere to put SO₂ — they now do, so that content is unblocked, not
in this slice.


## ADR-0063 — The factory builds the factory at T1

*2026-10-03 · Accepted*

**Context.** ADR-0017 forbids crafting-table recipes past the bootstrap. ADR-0058 scheduled
slice D as Press, Machine Casing, Assembler, and the T2 fabrication gate. Two documents
disagree about *when* those machines exist.

`MACHINES.md` lists Press and Assembler as T2. `PROCESSES.md` bootstrap chain makes them
the last crafting-table recipes, and says the Assembler is how T2+ is manufactured. Slice D
cannot ship both readings. `PROCESSES.md` also lists coil as a Wire Mill product; the Wire
Mill is T2, so the first Assembler would have no coil if that were the only route.

The first T2 machine has to be something the player already wants. Pylon MK2 is already
in the world as a block; MK1 is the Voltaic-gated craft. Making MK2 the Assembler's first
recipe is the gate, not a new machine shell.

**Decision.**

1. Press and Assembler are T1 crafting-table recipes, Voltaic-gated. `MACHINES.md`'s T2
   row is the industrial workhorse, not the first craft. The Assembler is the last
   crafting-table machine. T2+ has no JSON craft.
2. The Press is 4 s at F1. One ingot and a die. The die is a catalyst: it occupies a
   menu input slot, is not consumed, and hoppers cannot pull it. Plate, rod and gear
   dies cover the forms the casing needs. A coil die on copper makes the `copper_coil`
   reagent. The Wire Mill remains the dedicated T2 route.
3. Machine Casing is a crafting-table recipe: 4 iron plates and 2 iron rods, tags not
   item ids. The 8 s Assembler process in `PROCESSES.md` is the later scale recipe, not
   the bootstrap.
4. The Assembler craft is 1 casing + 1 copper coil + 2 iron gears. Its first generated
   recipe manufactures Pylon MK2 from 1 casing + 4 iron plates + 2 iron gears, 20 s at
   F1. There is no `flux_pylon_mk2.json`.
5. Dies, coil, Press, casing and Assembler are authored `gated_shaped` JSON. Press and
   Assembler recipes are generated into `ProcessRecipe` with a `catalysts` list, so the
   type does not fork.

**Alternatives rejected.** Shipping Press/Assembler as T2-only (the bootstrap could not
reach the Assembler, so ADR-0017 would never fire); a crafting-table recipe for MK2 "just
this once" (the gate would be a comment); consuming the die (hoppers would steal the
catalyst every cycle); waiting for the Wire Mill before the first coil (the Assembler
would be uncraftable); putting the die in a separate inventory that hoppers cannot see
(the shared menu already has input slots, and `canTakeItemThroughFace` already forbids
extracting them).

**Consequences.** Slice E can add Flux Conduits without inventing a fabrication story.
A successor that adds `flux_pylon_mk2.json` is reopening ADR-0017. A successor that
starts the Wire Mill or the Kiln in this slice is ignoring the playable-slice order.
Kiln / R2 remain unblocked by fluids and unstarted.


## ADR-0064 — Energy spanning is distance and storage, not coverage

*2026-10-03 · Accepted*

**Context.** ADR-0058 scheduled slice E as Flux Conduits, transformers and capacitor banks.
Pylons already project supply areas and auto-link within range. Three documents disagree about
what a Flux Conduit *is*.

`README` System 1 and the tools table: a **hand item** that right-clicks two pylons into a
manual link, with no length limit and an upkeep proportional to distance. `docs/DESIGN.md`
says the same: "a hand item, not a block".

`README` *Flux Conduits — one network* and ADR-0025: a **T2 chassis block** whose carried
types are decided by cores (item, fluid, heat, signal, Flux). ADR-0026 names those networks.
Shipping that in slice E would give the player a universal logistics pipe the moment they can
span two pylons, which is exactly the trap ADR-0025 exists to prevent — and it would let a
Flux Core cover spots pylons cannot, which is how pylons die.

Slice E's own sentence is the tie-break: "this layer is named networks and distance, and must
not obsolete pylons". The playable gap after the Assembler is an outpost beyond MK1 link
range, a buffer that is not "place more pylons", and a tap that is not a second supply cube.

**Decision.**

1. The T1 Flux Conduit is the handheld linker. Right-click pylon A, right-click pylon B.
   Clicking a shaft counts as the base. Sneak-click the pair to drop the link; sneak-click
   air to clear a pending mark. The item is not consumed. Manual links have no length limit.
2. Manual edges live in `FluxNetworkData` beside membership (ADR-0007). Flood-fill and
   neighbour-merge follow them as well as automatic range. Breaking a pylon drops its
   manual edges. Unloaded chunks do not. `PylonIndex.linkedTo` stays auto-range only.
3. Upkeep is `ceil(distance / 8)` FU/t, minimum 1. It is demand on the shared pool, paid
   from stored energy each tick. An unpaid link browns the network out; it does not
   silently drop. Auto-range links cost nothing.
4. A Capacitor Bank adds capacity to the **covering** network. It projects no supply cube.
   Uncovered, it is inert. Capacity is 200 ticks of MK1 throughput (102 400 FU). Membership
   of banks is SavedData, same as pylons: place registers, break unregisters, unload does
   not.
5. A Flux Transformer is a covered tap, not a pylon. It exchanges FU with the covering
   network at F1 (32 FU/t) through a local buffer other mods can see as FE. Grindless
   machines still resolve power by `networkCovering` — standing next to a transformer does
   not power them. Per-face F0/F1 gating waits until the energy capability is re-invalidated
   per face; the T1 step is the rate cap, not a second voltage network.
6. Named conduit networks (ADR-0026) and the core-chassis block (ADR-0025) stay T2. Slice E's
   "named network" is the Flux Network identity the linker joins. Item, fluid, heat and
   signal cores are not in this slice.

**Alternatives rejected.** Shipping the T2 chassis in E (obsoletes belts, pipes and pylons
the moment spanning exists); giving the transformer or the bank a supply cube (a cheaper
pylon); letting a Flux Core "for the rare spot a pylon cannot cover" land here (that *is*
obsoleting pylons); colour channels (ADR-0026); dropping the link when upkeep cannot be
paid (a silent topology change); putting manual edges only on the block entity (unloaded
chunks would split the trunk).

**Consequences.** An outpost beyond 64 blocks is a deliberate trunk with a running cost, not
a line of pylons and not a cable. A successor that registers `flux_conduit` as a block with
cores is starting ADR-0025, not extending this slice. A successor that lets
`networkCovering` return a transformer is reopening this record. Kiln / R2 and Slice F are
still unstarted.


## ADR-0065 — T1 Kiln is roast and SO₂, not the acid line

*2026-10-04 · Accepted*

**Context.** ADR-0058 scheduled playable slices, not system layers. ADR-0062 unblocked Kiln / R2
the moment a tank could hold a gas. Slice E (ADR-0064) spanned energy; it did not start roasting.
Three documents still disagree about what the first Kiln ships.

`PROCESSES.md` R2 is roast then reduce: Kiln at `T 700 · atm O2` for 8 s, then Arc Furnace at
`T 1200 · atm reducing` for 10 s, factor **1.15**, byproducts **1 B SO₂** and slag. The sulfur
loop then burns that SO₂ to SO₃ with bottled oxygen and vanadia, and that is sulfuric acid.

The T1 Arc Furnace is locked at 1500 °C reducing so R1 runs at full speed without a dial
(ADR-0059). A 1200 °C point band would put 1500 °C in the tolerance zone and halve R2. T1 has
no atmosphere bottles; `atm O2` is the oxidising envelope, not a fluid slot. Oxide is not a
conventional Forge form. Yield 1.15 is the reason to rebuild the line later, not the reason to
roast now — the reason to roast now is SO₂.

Slice F is the multiblock kernel, and only when a machine needs hatches or size (ADR-0058).
The Kiln does not.

**Decision.**

1. The T1 Kiln is a single-block `ProcessMachine` on the shared menu (1 in, 1 out). Envelope
   `MachineEnvelopes.KILN`. Held conditions are 700 °C oxidising, so roast evaluates optimally
   with no dial. Voltaic-gated crafting-table JSON, like the Pulverizer.
2. Roast is generated per material that has a vein feed and an oxide form. 1 u raw (or ore)
   or 1 u crushed → 1 u oxide + 1 B SO₂, 8 s at F1, `T 700 · OXIDISING`. No oxygen fluid.
   Crushed roast is a second recipe, not a different yield. 1.15 stays deferred.
3. Oxide is a Grindless form: `grindless:oxides/<material>`, not `forge:`. Supply covers
   mineable catalogue materials; alloys have no oxide. Item path is `<material>_oxide`.
4. R2 reduce is an Arc Furnace recipe: 1 u oxide + 1 u carbon → 1 ingot + slag, 10 s at F1.
   It names 1500 °C reducing so the locked T1 furnace runs it at full speed. The PROCESSES
   1200 °C is the envelope floor, not a second held temperature. It does not vent CO —
   PROCESSES names SO₂ and slag only.
5. SO₂ is a gas at ambient, like CO. The Kiln pushes 1 B into neighbouring `FluidEndpoint`s
   and vents the rest. The named T1 sink is the Basic Tank. The Thermal Generator must not
   burn it. Clay Conduit still refuses gases.
6. No sulfuric acid, no SO₃, no vanadia, no bottled oxygen, no Chemical Washer, no Wire Mill,
   no T2 cores, no Slice F kernel. Those wait for a machine that needs them.

**Alternatives rejected.** Shipping the 1.15 yield (fractional stacks and a lie until a later
route exists to justify the extra); a T1 oxygen fluid slot (nothing produces bottled O₂ yet);
naming R2 reduce at 1200 °C (the locked furnace would run it at half speed); burning SO₂ in
the Thermal Generator (that sink belongs to CO, and it would skip the acid line); registering
oxide under `forge:` (no convention); starting the contact-process reactors in this slice
(ADR-0036 would then demand a sulfuric-acid sink that T1 does not have).

**Consequences.** The player roasts because they want a tank of SO₂, not because they want
more metal. A successor that adds `sulfuric_acid` without a named spend is reopening
ADR-0036. A successor that starts Slice F because "the Kiln should be a multiblock" is
ignoring ADR-0058 — this Kiln does not need hatches.


## ADR-0066 — The T1 Atlas is a live lookup, not the solver

*2026-10-04 · Accepted*

**Context.** ADR-0023 ships a native Process Atlas with a ratio solver, because a pack without a
recipe viewer would make parameterised recipes unplayable. README step 16 is `VerifyRecipes` plus
an atlas stub; JEI, REI and EMI wait until 1.0 polish. After Kiln / R2 the live graph is ore line,
roast, press and assembler. The solver — given a rate, emit machine counts — is the feature
ADR-0023 itself flags as most at risk of going too far. Slice F is not next (ADR-0058).

**Decision.**

1. The T1 Atlas is a handheld item. Right-click opens a scrollable list of the live
   `ProcessLookup` graph: family, inputs, catalysts, outputs, named conditions, duration and
   FU/t. Query lives in `AtlasLogic` with no Minecraft imports so `VerifyAtlas` can dump it.
2. It is a lookup, not a solver. No target rate, no machine counts, no FU-per-unit overlay, no
   reachability against research. Those remain ADR-0023.
3. JEI, REI and EMI still wait. Vanilla already shows T0 JSON crafts. The Atlas lists process
   recipes only; a pack that ships no JEI can still see the graph that machines actually run.
4. Voltaic-gated crafting-table JSON, like the Prospector's Scanner. Process recipes only run
   after Voltaic machines exist; T0 table crafts do not need a second viewer.
5. The screen reads `ProcessLookup` on the client after tag sync. No extra S2C. No graph widget
   yet — a list of `AtlasLogic` lines is the stub.

**Alternatives rejected.** Shipping the ratio solver in this step (it would prescribe the factory
before the player has asked); JEI-only (ADR-0023 already rejected that); an always-available T0
craft (the gap is process recipes, which appear with Voltaic); a block terminal (the scanner and
linker are already handheld; a desk would duplicate the Research Terminal).

**Consequences.** A successor that adds `solve(rate)` is finishing ADR-0023, not extending this
stub. A successor that starts JEI integration is 1.0 polish, not a T1 blocker. Slice F is still
only when a machine needs hatches or size.


## ADR-0067 — Modular armour and the Arc Reactor are one tier

*2026-10-04 · Accepted*

**Context.** The README already has a T2 Flux Exosuit with a generic **Portable Reactor** module,
and T4 Fusion as the first *named* compact-power fantasy. Flux F3 is already called **Arc** and
has no generator. The Arc Furnace is a T1 smelter. Construction drones — the thing that makes a
real multiblock playable — arrive at T3 (ADR-0031). The user asked for modular armour *at every
research tier*, with upgrades, and for one of those tiers to carry a **miniature Arc Reactor**
on the suit **and** a fully playable Arc Reactor generation multiblock, with the processing
lines that feed it.

A successor who ships the current T2 "portable reactor" as a free worn generator, or who turns
the Arc Furnace into a power plant, or who drops an unfed trophy core in the world, has missed
the request.

**Decision.**

1. **Armour is a chassis line, not one T2 unlock.** T1–T4 each ship a four-piece modular suit.
   The grid grows. Modules are upgrades: inserting and removing is free, same as machine
   upgrades. T1 is protection plus a cell — no onboard generation. T2 adds the Network Tap and
   mobility (you walk through pylons). T3 is the Arc chassis. T4 is the exotic / orbital suit.
   Vanilla armour stays valid until the T1 chassis exists.
2. **The Arc pair is T3 / F3, together.** The factory **Arc Reactor** and the suit's
   **miniature Arc Reactor** unlock on the same research tier. One without the other is a
   different feature. The worn core burns the same manufactured fuel the multiblock burns, so
   the line you built for the plant also charges the suit.
3. **The factory reactor is a generator you have to run.** Formed structure, hatches, coolant
   or stability as real logistics, and a **named processing line** whose product is the fuel
   (working name: Arc Cell). It produces **FU directly** at F3, not heat into steam — fission
   already owns that route. If the cell line stops, the reactor starves. It is not a trophy
   block (MACHINES.md: every endgame system has a real use; this is the same rule at T3).
4. **It is not the Arc Furnace, not fission, not fusion.** The Furnace stays a smelter. Fission
   stays T3 heat + neighbour-bonus steam. Fusion stays T4 D–T ignition. The Arc Reactor is the
   missing F3 plant: compact, fed, direct FU.
5. **Do not start this in T1.** Slice F exists when *this* machine (or another that actually
   needs hatches) is scheduled — not as an empty kernel (ADR-0058). Equipment waits for the
   0.4 tools slice. Exact Arc Cell chemistry, hatch layout and grid sizes stay open.

**Alternatives rejected.** Shipping a T2 worn reactor that needs no factory (the current
Portable Reactor row); using the Arc Furnace as a generator (name collision and it already
smelts); a trophy core with no feed line; putting the pair at T2 before drones (a processing-line
multiblock you place by hand is a chore); starting Slice F "for later"; merging this into T4
Fusion (then F3 still has no generator and the suit waits until the endgame).

**Consequences.** A successor that adds `portable_reactor` as a T2 module with no cell recipe is
reopening this record. A successor that starts hatches because "armour will need a reactor" is
ignoring ADR-0058 — start F when the Arc Reactor (or another sized machine) is the slice, not
before. The README Exosuit table is the player-facing version of this decision.


## ADR-0068 — Horizon Gates are commute infrastructure, not mining dimensions

*2026-10-04 · Superseded in part by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded in part.** The gate remains a delivered pair and commute infrastructure,
> never a mining dimension. Its tier moves from the old T6 cap into the expanded planetary
> progression. See ADR-0107.

**Context.** The README already rejects mining dimensions as "the same grind, in a different
room." System 9 already has rockets, Mass Driver, telepresence and Colony Cores. ADR-0012
already says planets come from installed space mods. The user asked for futuristic end goals
in the Stargate tradition: gates, dimensions, exotic planets.

A successor who adds a void-miner dimension, a creative teleport, or a second planet pack
beside Ad Astra has missed both the request and the existing records.

**Decision.**

1. **The Horizon Gate is T6 commute infrastructure.** A ring multiblock you dial. Addresses
   come from the planet registry after a Deep Survey, not from a creative list. It is the
   moment the interplanetary commute dies, the same way the Crude Extractor killed mining and
   the Blueprint Tool killed repeating a layout.
2. **Two rings.** The far gate is cargo: you deliver it by rocket, Mass Driver or colony
   package once. Until the pair exists, you still fly. After it exists, you walk. That is the
   factory payoff, not a free portal.
3. **People cheap, bulk expensive.** Players, Proxy Frames and colony packages go through at a
   modest FU cost. Item cargo is allowed but costs more FU per kilogram than the Mass Driver, so
   belts of ore still belong on the driver (principle 10: pay in layout or pay in power).
4. **Not a mining dimension.** A Horizon Gate never opens a world whose only job is "more
   ore." Destinations are planets in the registry — Ad Astra's if present (ADR-0012), otherwise
   the fallback set. Vanilla Nether and End are not auto-registered; a pack author may add them
   by datapack. Pocket "mining dims" and RFTools-style void worlds are out.
5. **Exotic fallback worlds are process envelopes.** When no space mod is installed, the
   existing five stay, and two more exotic worlds join: **Thalassa** (ocean, pressure chemistry)
   and **Helios** (tidally locked heat/cold). Signature resources and hazards differ; the
   material registry does not. Erebus remains the strange-matter world. Do not invent unique
   untaggable ores that only exist there.
6. **Do not start this in T1.** Orbital (0.8) and interplanetary (0.9) stay later. Slice F is
   still only when a machine needs hatches. Exact dial cost, ring size and address format stay
   open.

**Alternatives rejected.** A mining or void dimension (already in the anti-pattern table);
shipping Grindless planets beside Ad Astra (ADR-0012); a one-sided creative teleport (no
factory); routing all cargo through the gate (obsoletes the Mass Driver); calling the block
Stargate (someone else's name); treating the Nether as a planet by default.

**Consequences.** A successor that registers `mining_dim` is reopening this record and the
anti-pattern table. A successor that skips the far-gate delivery is shipping a creative
teleporter. The README System 9 travel table is the player-facing version.


## ADR-0069 — The Multitool rotates and relocates; it still does not mine

*2026-10-04 · Accepted*

**Context.** ADR-0055 forbids the Multitool from mining. The README still promises it rotates
machines and picks them up with contents and settings intact. The registered item is a blank
`Item`: it does neither. That is the empty-the-machine tax the T0 handheld was meant to delete.
Slice F, the Sifter, Resonance, the Ballistic Turret, armour and Horizon Gates are not the next
playable hole.

**Decision.**

1. Right-click a Grindless block with the Multitool: rotate 90° clockwise if it has a horizontal
   facing. The machine GUI, the dynamo crank and the Research Terminal do not open while the
   Multitool is the used item.
2. Sneak-right-click: pick the block up as its BlockItem, with `BlockStateTag` (facing, status)
   and `BlockEntityTag` (buffers, lanes, fluids, filter). `Relocation` is active so
   `onRemove` does not spill the inventory onto the floor. Pylon clicks resolve to the base
   (ADR-0054); shafts are not a separate item.
3. A pickaxe still breaks machines and they still drop empty (ADR-0051, ADR-0055). The Multitool
   is not in `mineable/pickaxe` and has no destroy speed.
4. Splitter face-filtering stays sneak-click *without* the Multitool. With it, sneak is relocate.
5. This is not the Blueprint Tool and not a mining module.

**Alternatives rejected.** Mining with the Multitool (ADR-0055); opening the GUI anyway (then
rotate is unreachable on machines with menus); spilling contents on relocate (that *is* the tax);
a T2 Blueprint-only move (too late for the machine you just placed at T0).

**Consequences.** A successor that gives the Multitool pickaxe behaviour is reopening ADR-0055.
A successor that starts the Sifter or a turret because "T1 is done" is skipping a tool the player
already crafted.


## ADR-0070 — Remaining work is the autonomous build-out

*2026-10-04 · Superseded in part by [ADR-0088](#adr-0088--grindless-is-a-modpack-scale-progression)*

> **Superseded in part.** [`BUILD-OUT.md`](BUILD-OUT.md) remains the history of the slices already
> defined. Anything not started is held. Finishing BB does not finish the mod. The next calendar
> is the modpack expansion, and its first slice is BC. See ADR-0088.

**Context.** Slices A–E, Kiln/R2, the Atlas stub and the Multitool wrench are in. Armour and
Horizon Gates are recorded, not started. The README implementation plan still has four coarse
pending rows (F, T2+ industry, tools, orbit, planets). A session that infers "next" from those
rows restarts Slice F, the Sifter without a graph, or a turret without Resonance — all of which
the records already forbid.

The owner asked for a closed list so a successor can finish the mod without asking.

**Decision.**

1. [`docs/BUILD-OUT.md`](BUILD-OUT.md) is the remaining schedule. The README stays the design
   source of truth (ADR-0013). The build-out is the order.
2. The next playable slice is **G — Belt junctions** (merger, tunnel, overflow), which
   ADR-0060 already parked. Not F. Not the Sifter. Not a turret. Not armour.
3. Slice F still starts when a machine needs hatches. That machine is the Arc Reactor at T3
   (ADR-0067), as build-out **AD/AE**.
4. A session does not stop to ask. Defaults come from the README, `MACHINES.md` and
   `PROCESSES.md`. An ADR is written only when those conflict or are silent, then the slice
   ships.
5. One slice per stacked draft. Do not merge unless the owner asks. Do not squash.

**Alternatives rejected.** Leaving "T2+ industry" as one row (a session dumps the chemical core
into an unused kernel); starting F "so later slices are easier" (ADR-0058); asking the owner
at every T1-named hole (the holes are already classified: Sifter needs a graph ADR, turret
needs Resonance, harness is 0.4).

**Consequences.** A successor that starts hatches, a Sifter shell, or JEI because the build-out
looks long is ignoring this record. Updating the **Next slice** line is part of shipping G
and every slice after it.


## ADR-0071 — T1 belt junctions are merger, tunnel and overflow

*2026-10-04 · Accepted*

**Context.** ADR-0060 shipped the first belt, splitter and manipulator, and parked merger,
tunnel, overflow and sorter. BUILD-OUT slice G is those three junctions. README already
gives the T1 tunnel a range of five and describes overflow as the belt that does not stall
when a chest fills. Three questions were still open.

**Decision.**

1. **Merger.** Three inlets (back, left, right), one outlet (front). Round-robin from the last
   accepted face. No filters; that is the sorter (slice H).
2. **Tunnel Belt.** An entrance/exit pair facing the same way. The gap is one to five empty
   blocks (README range 5). Adjacent tiles are a conveyor, not a tunnel. Items travel only in
   the facing direction. A craft yields two tiles.
3. **Overflow Gate.** Front is preferred. The clockwise side takes the item only when the
   front is backed up. Not a second splitter.
4. Same lane model and BeltEndpoint as the conveyor (ADR-0008). Voltaic-gated crafts.
   `VerifyBelt` dumps pick/range/route. Sorter still waits.

**Alternatives rejected.** Powered T1 tunnels (the first belt is mechanical); opposite-facing
two-way pairs in this slice (a second direction is another block, not this one); overflow as
a splitter with a hidden filter (then two blocks do one job).

**Consequences.** A successor that starts the sorter in this slice is skipping H. A successor
that lets two tunnel tiles pair when they touch is deleting the conveyor.


## ADR-0072 — The T1 sorter peels; it does not split

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice H is the sorter ADR-0060 parked. README calls it an inline
multi-output filter for a mixed ore line. The splitter already has three filtered outlets and
round-robins unfiltered faces. If the sorter copied that, two blocks would do one job. The
remaining question is what "inline" means when a matching lane is full.

**Decision.**

1. One inlet (the back). Front is always the passthrough: unmatched items continue. Left and
   right are optional filters, sneak-clicked like the splitter. An empty filter does not steal.
2. A matching side, if open, takes the item. If that side is backed up, the sorter holds.
   Matching items never dump onto the front. That is the difference from the splitter, whose
   backed-up filter yields to an open face.
3. Two sides that name the same item round-robin. No GUI. Logic Controller still waits.
4. Voltaic-gated craft with a hopper in the middle so the recipe is not the splitter. Same lane
   model and BeltEndpoint (ADR-0008). `VerifyBelt` dumps the route.

**Alternatives rejected.** A second splitter under a new name; a GUI for a list of filters
(sneak-click is enough for two faces); overflowing a full copper lane onto the iron belt (that
deletes the sorter).

**Consequences.** A successor that lets a backed-up filter spill to the front is shipping
overflow on the wrong block. A successor that round-robins unfiltered sides is shipping a
splitter.


## ADR-0073 — Industrial is the second blueprint on the same terminal

*2026-10-04 · Superseded by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded.** Industrial is now reached by manufacturing its physical Control Matrix.
> The Research Terminal no longer owns a global Voltaic/Industrial permission ladder. See
> ADR-0107.

**Context.** BUILD-OUT slice I is the T2 gate: research Industrial, then Pylon MK2 is a real
Assembler craft. The MK2 process already exists (`assemble/pylon_mk2`) and has no crafting-table
JSON (ADR-0063). The terminal only unlocks Voltaic, and the Assembler does not ask whether
Industrial is researched, so a T1 factory already manufactures the T2 pylon.

**Decision.**

1. **Industrial** is the second `Blueprint`. The same Research Terminal unlocks it after Voltaic.
   It still draws F0. The cycle is sixty seconds. The spent item is an **Advanced Data Core**,
   not a basic core. You cannot skip Voltaic.
2. The Advanced Data Core is a Voltaic-gated crafting-table item: one Data Core and four iron
   plates. Processed plates are the "advanced cores from processed ones" line in the README.
3. `assemble/pylon_mk2` names blueprint `industrial`. The Assembler refuses the recipe until
   that blueprint is unlocked. Atlas still lists it. Wire Mill, washer and Slice F still wait.

**Alternatives rejected.** A second research block (the terminal is the production target);
re-rating the terminal to F1 for this slice (the first T2 wait should not demand a new power
tier); a crafting-table MK2 (ADR-0017); unlocking Industrial with a basic core (then the
advanced item is flavour).

**Consequences.** A successor that lets the Assembler build MK2 before Industrial is deleting
the T2 gate. A successor that starts the Wire Mill in this slice is skipping J.


## ADR-0074 — The Wire Mill is T2 and does not wait for acid

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice J is wire, coil and motor. `MACHINES.md` puts the Wire Mill at T2
for wire and coil. `PROCESSES.md` names Motor as `1 casing + 2 u coil + 1 u rod` in 10 s with
no fluid, and the generic T2 machine as `1 casing + 2 Motor + 1 Circuit Board + 4 u plate`.
Circuit Board needs etching acid (slice K). T1 already presses a copper coil with a die so
the Assembler can exist (ADR-0063). Fine wire is MK III+. Pump waits on resin.

**Decision.**

1. **Wire Mill** is a T2 single-block process machine. The Assembler manufactures it once
   Industrial is researched: `1 casing + 2 copper coil + 4 iron plates`, 20 s, F1. No circuit
   board. No crafting-table JSON (ADR-0017). Ambient envelope. Menu is one in, one out.
2. **Wire** is a supplied form for every catalogue material (`grindless:wires/<m>`, not
   conventional). `1 ingot → 2 wire`, 8 s, F1, no named conditions. Fine wire waits.
3. **T2 coil** is mill-only: `2 copper wire → 1 copper_coil`, 8 s, F1, no die. The Press plus
   coil die remains the T1 bootstrap (ADR-0063). `MaterialForm.COIL` is still not supplied.
4. **Motor** is a reagent item `grindless:motor`. Assembler: `1 casing + 2 copper coil +
   1 iron rod`, 10 s, F1, Industrial. No JSON. Pump and the generic T2 machine recipe wait.

**Alternatives rejected.** Using the generic T2 recipe this slice (that is acid); dropping
the Press coil (the first Assembler would have nothing to wind); supplying fine wire
(MK III); a crafting-table mill (ADR-0017); gating every mill recipe on Industrial (owning
the mill is the gate).

**Consequences.** A successor that starts sulfuric acid in this slice is skipping K. A
successor that crafts the mill at a table is deleting the T2 manufacturing gate.


## ADR-0075 — The contact process is air, vanadia and a pickle

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice K is the contact process: Chemical Reactor, SO₂ → SO₃ → sulfuric
acid at the ratios in `PROCESSES.md`, vanadia as catalyst, named sinks, R2 yield 1.15 *may*
go live. Washer and etching wait. Atmospheric Intake and electrolysis wait for M. Generic T2
assembly wants a Circuit Board, which wants etching acid.

`PROCESSES.md` names `0.5 B oxygen` and `T 450` for oxidation, `T 120` for absorption.
`MACHINES.md` gives the Chemical Reactor −20–250 °C. One machine cannot hold both 450 °C and
120 °C. One `FluidBuffer` cannot hold SO₃ and water at once. Arc Furnace R2 already fills
both output slots (ingot + slag), so a 1.15 nugget has nowhere to land.

**Decision.**

1. **Chemical Reactor** is a T2 single-block process machine. The Assembler manufactures it
   once Industrial is researched: `1 casing + 2 motor + 4 iron plates`, 20 s, F1. No circuit
   board. No crafting-table JSON. Menu is one in (catalyst) and one out. Envelope is
   `MachineEnvelopes.CHEMICAL_REACTOR` with the ceiling raised to 500 °C so 450 °C is inside.
   Held conditions are 450 °C oxidising, stirred. Recipes in this slice do not name a
   temperature; both steps run in-band without a dial.
2. **Oxidation.** `1 B SO₂ → 1 B SO₃`, 6 s, F1, vanadia catalyst. Air is the oxidiser
   (held OXIDISING). Bottled oxygen waits for M. SO₃ is a gas at ambient, like SO₂; the tank
   is the buffer.
3. **Absorption.** `1 B SO₃ + 0.2 B water → 1 B sulfuric acid`, 4 s, F1, no catalyst.
   Water is taken from a neighbouring `FluidEndpoint` at finish; it is never pulled into the
   reactor buffer, so a water tank cannot contaminate the sulfur oxide.
4. **Vanadia pellet** is a Voltaic-gated crafting-table reagent: one iron oxide and four
   bricks (ceramic support). Vanadium is not a catalogue material. Not consumed.
5. **Named spend for sulfuric acid** is pickle: `1 iron ingot + 0.1 B sulfuric acid → 1 iron
   plate`, 4 s, F1, no die. The Press remains. Washer and etching wait.
6. **R2 yield 1.15 stays deferred.** A nugget would be a third Arc Furnace output.

**Alternatives rejected.** Bottled oxygen this slice (that is M); a second fluid buffer
(the neighbour pull is enough); naming 450 °C and 120 °C on the recipes (one hold cannot
satisfy both); putting oxidation in the Kiln (700 °C is outside the 450 °C band); shipping
1.15 as a third item; a crafting-table reactor; using the generic T2 recipe (circuit board).

**Consequences.** A successor that starts the washer in this slice is skipping L. A
successor that adds bottled oxygen here is skipping M. A successor that emits sulfuric
acid without pickle (or another spend) is reopening ADR-0036.

---

## ADR-0076 — The washer batch is eight crushed and the next metal

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice L is the Chemical Washer and B2. `PROCESSES.md` quotes the wash
per 1 u raw, after the pulverizer has already made 2 u crushed: `agi stirred`, 0.5 B water,
5 s, 2 u washed crushed and **0.25 u byproduct**, grade still 2.00. The byproduct is "the
secondary material of the vein, resolved from the material's tags". Veins store one material
(ADR-0009). `ProcessRecipe` counts items in whole units. Generic T2 assembly wants a circuit
board, which wants etching acid (slice AI). Flotation, electrolysis and etching wait.

**Decision.**

1. **Chemical Washer** is a T2 single-block process machine. The Assembler manufactures it
   once Industrial is researched: `1 casing + 2 motor + 4 iron plates`, 20 s, F1. No circuit
   board. No crafting-table JSON. Menu is one in and two out. Envelope is 10–100 °C at
   ambient pressure, stirred, so a later 90 °C leach still fits. Held conditions are ambient
   and stirred. Recipes in this slice do not name a temperature or an agitation: the recipe
   record has no agitation field, and one hold cannot also be the acid leach.
2. **Washed crushed** is a supplied non-conventional form,
   `grindless:washed_crushed/<m>`, grade 2.00. The item path is `<m>_washed`.
3. **The integer batch** preserves the per-raw ratio. Four raw are eight crushed:
   `8 crushed + 2 B water → 8 washed crushed + 1 byproduct` in 20 s at F1. Water is the
   primary fluid and fills the 2 B machine buffer. 2 B is four times the documented
   0.5 B; 20 s is four times the documented 5 s; one byproduct is four times 0.25 u.
4. **The byproduct** is one crushed unit of the next material, in generation order, that
   has crushed, washed and an ingot. The list wraps. That material's existing `b1_r1` is
   the named sink (ADR-0036). A pack with fewer than two such materials still washes and
   emits no byproduct: there is no secondary, and washing a metal into itself is not one.
5. **Washed is not a dead end.** `b2_r1` reduces it like crushed (1 + carbon → 1 ingot +
   slag + CO). `roast_washed` feeds the Kiln the same way crushed roast does, so B2×R2
   stays reachable. R2 yield 1.15 stays deferred (ADR-0075).

**Alternatives rejected.** A fractional 0.25 output (the type forbids it); an accumulator
that emits the byproduct every fourth cycle (two machines with the same recipe would not
agree, and a reload would have to persist a credit); a fixed iron→nickel table (a pack
without nickel has no sink); storing a second material on the vein (ADR-0009, and a stack
of crushed does not remember its chunk); a crafting-table washer (ADR-0017); the generic
T2 recipe (circuit board); shipping leachate, flotation or electrolysis in this slice.

**Consequences.** A successor that emits a residue with no Arc Furnace line is reopening
ADR-0036. A successor that starts the Froth Flotation Cell here is skipping P. A successor
that starts the Electrolysis Cell here is skipping M.

---

## ADR-0077 — Water splits in the buffer; air yields oxygen only

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice M is the Electrolysis Cell and the Atmospheric Intake: split
water, take N₂/O₂, and give hydrogen and oxygen a named sink. `PROCESSES.md` quotes
chlor-alkali (`1 B brine → 0.5 B chlorine + 0.5 B hydrogen + 1 B sodium hydroxide`) and
air separation (`10 B compressed air [T −190 · P 0.6 MPa] 10 s → 7.8 B nitrogen + 2.1 B
oxygen + 0.1 B argon`). Water electrolysis is named in the gas table and has no numbered
line. Haber is `1 B nitrogen + 3 B hydrogen [T 450 · P 20 MPa]` on the Autoclave. One
`FluidBuffer` holds 2 B of a single fluid. `canOutput` requires every non-vented fluid
output to fit in that buffer; a vented output skips the buffer and is offered to
neighbours, then lost (ADR-0065, ADR-0075). ADR-0036 forbids emitting a fluid that nothing
in the graph consumes. T1 roast must not start requiring bottled oxygen (ADR-0065).
Brine is the Fluid Well (slice N). The Compressor and the Cryogenic Plant are not this
slice.

**Decision.**

1. **Electrolysis Cell** is a T2 single-block process machine. The Assembler manufactures
   it once Industrial is researched: `1 casing + 2 motor + 4 iron plates`, 20 s, F1. No
   circuit board. No crafting-table JSON. Menu is no item slots: the slice's recipe is
   fluid-only. Envelope is 10–80 °C at ambient pressure, with an electric field, so a
   later 60 °C chlor-alkali still fits and molten-salt electrolysis does not. Held
   conditions are ambient plus electric. Recipes in this slice do not name a temperature:
   the recipe record has no field dimension, and one hold cannot also be chlor-alkali.
2. **Water split** is the integer batch that fills the buffer. Real stoichiometry is
   two volumes of hydrogen per volume of oxygen:
   `2 B water → 2 B hydrogen + 1 B oxygen` in 10 s at F1. Hydrogen is stored. Oxygen is
   vented and captured when a neighbouring tank accepts it. Water is the primary fluid.
3. **Hydrogen's sink** is the Thermal Generator. `1 B hydrogen` burns 400 ticks, the same
   as `1 B CO`. Sulfur oxides, oxygen and nitrogen do not burn.
4. **Oxygen's sink** is oxyhydrogen recombination on the Chemical Reactor:
   `2 B hydrogen + 1 B oxygen → 2 B water` in 8 s at F1. Oxygen is the neighbour fluid,
   as absorption water is (ADR-0075). The recipe does not name a temperature. Roast stays
   free of bottled oxygen.
5. **Atmospheric Intake** is the same Assembler craft and the same empty menu. Envelope
   is −200–40 °C and 0.1–1.0 MPa, so the documented cryogenic separation still fits later.
   Held conditions are ambient. This slice it drinks free air: no compressed-air input,
   no catalyst. `2 B oxygen` in 10 s at F1, stored. That is the buffer, and it is the
   oxygen cut of the documented 10 s separation (2.1 B) rounded to what one buffer can
   hold.
6. **Nitrogen and argon are not emitted.** Neither has a consumer in this graph. Haber
   is 20 MPa on an Autoclave that does not exist, and faking it on the Chemical Reactor
   would erase the envelope lesson (ADR-0075). Argon shielding and liquid nitrogen wait
   with their consumers. The fluid ids exist and are gases, so a later recipe does not
   have to rediscover that clay will not carry them.

**Alternatives rejected.** Chlor-alkali this slice (brine is N, and chlorine and sodium
hydroxide have no sink); three stored gases (the buffer refuses the second id);
fractional 7.8 / 2.1 / 0.1 in one cycle (the buffer is 2 B, and argon would still lack
a sink); inventing a Compressor so the intake can demand compressed air; holding −190 °C
on MK I for a recipe that does not name it; bottled oxygen as a kiln input; a 20 MPa
ammonia recipe on the reactor; burning oxygen or nitrogen for power; a second fluid
buffer.

**Consequences.** A successor that emits nitrogen or argon without a recipe that
consumes it is reopening ADR-0036. A successor that requires oxygen on T1 roast is
reopening ADR-0065. A successor that starts the Fluid Well here is skipping N. A
successor that starts Haber, the Autoclave, or molten-salt electrolysis here is past
this slice.

---

## ADR-0078 — The Fluid Well is powered chunk water

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice N is the Fluid Well: chunk fluids, water, brine and
geothermal, and not a new pipe tier. `MACHINES.md` also names oil-equivalents.
`PROCESSES.md` sends crude hydrocarbon to the Distillation Tower (T3) and brine to
chlor-alkali, whose chlorine and sodium hydroxide still have no sink (ADR-0077).
Geothermal is a heat feed for the Boiler (slice Q). The Basic Tank refuses anything
hotter than 40 °C (ADR-0062). The Hand Pump is the unpowered bootstrap: it needs a
vanilla water source and moves 20 mB/t. Veins stay one material (ADR-0009).

**Decision.**

1. **Fluid Well** is a T2 machine, not a process recipe. The Assembler manufactures it
   once Industrial is researched: `1 casing + 2 motor + 4 iron plates`, 20 s, F1. No
   circuit board. No crafting-table JSON. It draws F1. It does not read a vanilla
   water source. Every chunk is an aquifer.
2. **Rate.** 100 mB/t of ambient water while powered — five times the Hand Pump — into
   a 2 B buffer, pushed out the face it points at. A full buffer with nowhere to go is
   blocked. No power is starved. It stays subscribed for its whole life: the aquifer
   does not turn off, and a tank draining does not notify the well.
3. **Brine, crude hydrocarbon and geothermal are not emitted.** None has a consumer
   the clay line can spend. Hot geothermal would be refused by the only tank. A later
   slice that gains a sink derives the chunk's fluid from the chunk seed under a new
   saved key. It does not bump `VeinGenerator.VERSION`.
4. **No new pipe.** The output is ambient water. Clay carries it.

**Alternatives rejected.** A process-recipe batch (a well is a pump, like the Hand
Pump, not a 10 s cycle); requiring a vanilla source (that is the Hand Pump); emitting
brine or oil with nowhere to put it (ADR-0036); emitting geothermal into a tank that
refuses it; a Pressure Pipe in this slice (that is R); storing a second material on
the vein (ADR-0009).

**Consequences.** A successor that emits brine, oil or geothermal without a recipe
that consumes it is reopening ADR-0036. A successor that changes vein version to
remember a fluid roll is reopening ADR-0009. A successor that starts the Induction
Furnace or the Caster here is skipping O.

---

## ADR-0079 — Melt is 144 mB at 1000 °C, and the arc still makes ingots

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice O is the Induction Furnace and the Caster: a clean melt,
and a way to skip the ingot. `PROCESSES.md` sets 1 u of melt at **144 mB**. Molten metal
is quoted at 1000 °C+. The Basic Tank and the Clay Conduit refuse anything above 40 °C
(ADR-0062). `canOutput` judged every fluid output against that ambient cap, and
`FluidState.of(id, count)` is 20 °C, so a recipe could not emit a hot fluid at all.
The documented caster lines are `1 u molten + ingot mould → 1 u ingot` and
`1 u molten + plate mould → 1 u plate`, 4 s. Tapping the Arc Furnace straight to melt
would replace R1's ingot, slag and CO. The Vacuum Furnace is T5.

**Decision.**

1. **Induction Furnace** and **Caster** are T2 process machines. The Assembler
   manufactures each once Industrial is researched: `1 casing + 2 motor + 4 iron
   plates`, 20 s, F1. No circuit board. No crafting-table JSON. The induction menu
   is one item in. The caster menu is one mould slot and one item out. The mould is
   a catalyst.
2. **Melt** is `1 ingot → 144 mB grindless:molten/<m>` in 8 s at F1, 1000 °C, inert.
   No slag and no CO. The induction holds 1000 °C and inert; its envelope is the
   existing 200–2000 °C. Crushed, raw and oxide do not melt: that would skip
   carbon and emit metal for free.
3. **The fluid is hot.** A molten id is emitted at 1000 °C and 0.1 MPa.
   `canOutput` and capture use the buffer's own rating, not the ambient cap.
   Induction and caster buffers accept up to 1200 °C. The Basic Tank and the Clay
   Conduit stay at 40 °C, so melt moves only between machines that accept it.
   The caster's process envelope stays ambient: the heat is on the fluid, and the
   cast recipes do not name a temperature.
4. **Cast** is `144 mB molten + mould → 1 u` in 4 s at F1. Ingot mould and plate
   mould are Assembler products, four iron plates each, Industrial, not consumed.
   A material with no plate has no plate cast.
5. **R1 is unchanged.** The arc still emits an ingot, slag and CO. The skip is the
   Press: melt a finished ingot and cast a plate. An arc tap that emits melt instead
   of an ingot is a later route, not a replacement.

**Alternatives rejected.** Ambient "molten" that a tank and a clay pipe will carry
(it is not molten); raising the tank's 40 °C cap (ADR-0062); melting crushed or
oxide (free metal); replacing R1 with a tap (slag and CO would disappear);
a vacuum furnace (T5); rod and gear moulds in this slice.

**Consequences.** A successor that stores melt in the Basic Tank is ignoring the
40 °C rating. A successor that melts ore without carbon is inventing a yield.
A successor that deletes R1 in favour of a tap is past this slice. A successor
that starts the Magnetic Separator or the Froth Flotation Cell here is skipping P.

## ADR-0080 — Flotation is an integer batch, and the magnet splits by name

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice P is better separation: a Magnetic Separator, a Froth
Flotation Cell (B3), a surfactant reagent, and a named sink for tailings. Centrifuge
is B4 and T3. The Sifter still needs its own ADR. Veins stay one material (ADR-0009).
`PROCESSES.md` sets B3, per 1 u raw, at 2.4 u concentrate + 0.3 u tailings with
0.05 B surfactant in 8 s, grade 2.40. The magnetic and electrostatic separators are
not a grade row: they split a mixed stream, and on a single-material vein they do
not change the item. Counts on a `ProcessRecipe` are positive integers (ADR-0076).

**Decision.**

1. **Froth Flotation Cell** is a T2 process machine. The Assembler manufactures it
   from 1 casing + 2 motors + 4 iron plates, Industrial, no circuit board. Envelope
   matches the washer (10–100 °C, stirred). Held ambient and stirred. The recipe
   does not name a temperature.
2. **The batch is ten raw.** 20 crushed + 500 mB surfactant → 24 concentrate + 3
   tailings in 80 s at F1. That is the documented line times ten, so 2.4 and 0.3
   and 0.05 B are whole numbers. Concentrate is `grindless:concentrates/<m>`,
   grade 2.40, item `<m>_concentrate`. Tailings are `grindless:tailings/<m>`,
   item `<m>_tailings`, and they are not an ore-line grade.
3. **Concentrate reduces like crushed.** `b3_r1/<m>` is 1 concentrate + 1 carbon →
   1 ingot + slag + 1 B CO on the Arc Furnace, the same 12 s R1. Twenty-four
   ingots from ten raw is the 2.40. Concentrate does not roast: B3 × R2 is T3.
4. **Tailings have an arc sink.** `tailings/r1/<m>` is 10 tailings + 1 carbon →
   1 ingot + slag + 1 B CO. Three tailings from a batch are not an ingot. The
   extra metal is a trace, not a second 2.40. Deconstructor and re-leach wait.
5. **Surfactant is a reactor fluid.** No organics line exists, so black liquor is
   not invented. `reagent/surfactant` is 1 carbon + 1 B water → 1 B
   `grindless:surfactant` in 8 s on the Chemical Reactor, ambient liquid, not a
   fuel. Flotation is the sink.
6. **Magnetic Separator** is a T2 belt block, same Assembler craft, not a process
   recipe and not a grade change. Ferromagnetic item ids — a path token of
   `iron`, `nickel` or `steel` — leave to the left. Everything else continues
   forward. A backed-up magnet holds, it does not dump onto the front. The right
   face is unused. Electrostatic separation is not this slice.

**Alternatives rejected.** Fractional 2.4 and 0.3 outputs; a second vein material
so the magnet has something to split (ADR-0009); treating the magnet as a
pass-through process recipe; roasting concentrate in this slice; recovering one
ingot per three tailings (that would add a tenth of a metal on top of 2.40);
emitting surfactant with no consumer; a crafting-table cell; the Centrifuge; the
Sifter; the Electrostatic Separator.

**Consequences.** A successor that emits 2.4 items is ignoring integer counts.
A successor that roasts concentrate here is in T3. A successor that makes the
magnet change yield is inventing a grade row the document refuses. A successor
that starts the Centrifuge or the Sifter here is past P. A successor that starts
the boiler here is skipping Q.

## ADR-0081 — Daylight is F1, and steam closes on the condenser

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice Q is heat and steam: solar generation, a Boiler and a
Condenser. The Steam Turbine stays with fission. The Arc Reactor is not this slice.
PROCESSES.md sets steam at 150 °C and 0.5 MPa. The Basic Tank and the clay conduit
refuse anything above 40 °C or 0.1 MPa (ADR-0062). The catalogue calls F2 steam and
solar at 128 FU/t, and also lists a T2 Steam Turbine; the calendar says the turbine
waits. One panel at 128 FU/t would retire the Thermal Generator during the day.

**Decision.**

1. **Solar Array** is a T2 generator, Assembler-manufactured (casing, two motors,
   four plates, Industrial). It makes 32 FU/t (F1) while the world is day and the
   block above can see the sky. Night and a cover make 0. Rain does not stop it.
   It stays subscribed, because dawn does not notify the block. One panel matches
   the Thermal Generator; F2 waits for the turbine.
2. **Boiler** is a T2 process machine, same craft. It draws F1. 1 B water becomes
   1 B steam in 10 s. Steam is grindless:steam, emitted at 150 °C and 0.5 MPa.
   The boiler buffer accepts up to 200 °C and 1.0 MPa, so this steam fits and
   superheated steam (450 °C, 6 MPa) does not. The recipe does not name a
   temperature. Geothermal is still not emitted.
3. **Condenser** is a T2 process machine, same craft. 1 B steam becomes 1 B water
   in 4 s at F1. The water is ambient. The condenser is the named steam sink. Its
   buffer has the same ceiling as the boiler, so it can hold the steam the tank
   refuses. No turbine, no superheated steam, no heat exchanger.

**Alternatives rejected.** One panel at F2 (it deletes the reason to burn coal at
night); a fuel-fired boiler that replaces the Thermal Generator; emitting
geothermal from the well in this slice; storing steam in the Basic Tank; a Steam
Turbine (T3, with fission); treating rain as night.

**Consequences.** A successor that rates one panel at 128 FU/t is skipping the
turbine. A successor that puts steam in a clay pipe is ignoring 150 °C. A
successor that builds the turbine here is in slice AF. A successor that starts
the pressure pipe here is skipping R.

---

## ADR-0082 — T2 pipe holds steam, and the flux belt spends LV

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice R is how fluids and stacks move once T2 exists:
Pressure Pipe, Electric Pump, Industrial Tank, Fluid Manipulator, Flux Belt,
Stack Manipulator and Filter Manipulator. Phase Manifold is T3. Steam is
150 °C at 0.5 MPa and molten metal is 1000 °C at 0.1 MPa (ADR-0079, ADR-0081).
The Clay Conduit and the Basic Tank still refuse anything above 40 °C or
0.1 MPa (ADR-0062). The README calls the pressure pipe "MV pressures, hot".
MV is the alias of F2, not a pressure, so a successor who treats it as a
number will invent one. The Industrial Tank row says adjacent blocks merge
into one tank. Slice F, the multiblock kernel, waits for the Arc Reactor
(ADR-0058, ADR-0067). The Flux Belt row is 16 items/s at LV with a tunnel
of 9. The T1 tunnel is 5 (ADR-0071). A Stack Manipulator moves up to 12
items at once. A Filter Manipulator is a whitelist or a blacklist.

**Decision.**

1. **The T2 fluid rating is 1200 °C and 1.0 MPa.** Steam and molten metal
   fit. Superheated steam (6 MPa) does not. The Clay Conduit and the Basic
   Tank keep the ambient cap. Nothing in this slice raises it.
2. **Pressure Pipe** is unpowered and Assembler-manufactured (casing, two
   motors, four plates, Industrial). It pushes any accepted fluid toward the
   face it points at, including gas and including uphill, at 200 mB/t, and
   it holds 2 B. It does not create pressure. Gravity remains the clay rule.
3. **Electric Pump** is the same craft and draws F1 only on a tick that
   moves fluid. It pulls from the face opposite its facing and pushes out
   the facing at 100 mB/t. It does not invent water. The Hand Pump and the
   Fluid Well stay the sources. A full output is blocked. No input is starved.
4. **Industrial Tank** is one block, 64 B, at the T2 rating. Adjacent tanks
   do not merge. The merge is a multiblock and waits for slice F.
5. **Fluid Manipulator** moves 1 B per second from the back face to the front.
   It draws F1 while it has work and stays subscribed, because a tank filling
   behind it does not notify the arm.
6. **Flux Belt** carries 16 items/s. Packing stays two lanes of four, so the
   belt moves two tiles a second. It draws F1 (LV) only while it holds items.
   Without power the items sit. An empty belt draws nothing. The README's
   tunnel range of 9 is recorded and is not a block. The T1 tunnel stays 5.
7. **Stack Manipulator** moves up to 12 items in one 1-second cycle and draws
   F1 while it has work. **Filter Manipulator** moves one item. An empty
   filter passes every item. A set filter is a whitelist of one item id.
   Sneak-use with an item sets it; sneak-use with an empty hand clears it.
   Blacklist is not this slice. The arm reads a belt lane or an inventory
   before it takes, so a rejected item is not picked up and put back.

**Alternatives rejected.** Raising the clay and Basic Tank caps so steam fits
there; a multiblock tank in this slice (that is slice F); one flux belt rated
at MV or at 32 items/s (that is the Mag-Lev); changing the T1 tunnel to 9; a
belt that keeps moving with no power; an electric pump that summons water; a
pipe rated for 6 MPa; a blacklist mode; the Phase Manifold; a booster pump,
relief valve or check valve (not in the slice row).

**Consequences.** A successor that stores steam in clay is ignoring this
record. A successor that merges industrial tanks is starting F. A successor
that retunes the T1 tunnel to 9 is inventing the flux tunnel. A successor
that makes an empty flux belt draw F1 is taxing a line that is doing nothing.
A successor that builds the Phase Manifold here is in slice AJ.

---

## ADR-0083 — A controller holds a machine below five hundred

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice S is the first logic: Signal Cable, Logic
Controller, Redstone Interface. The README's example is *run only while
copper ingots < 500*. Operator Drones are T3. From T2 the README also says
Signal Cable stops being its own block and becomes a signal core inside a
Flux Conduit. That conduit is a later collapse of four networks. This slice
has to be playable before that collapse exists. Signals in the README are
not capped at redstone's 0–15. An Arithmetic Unit and a Network Monitor are
in the same table and are not in the slice row.

**Decision.**

1. **Signal Cable is a block.** It carries one integer in the direction it
   faces, one block per tick, with no 0–15 cap. It is not a signal core.
   The core waits until Flux Conduits gain cores. A value written by a
   neighbour replaces what the tile holds.
2. **Logic Controller** counts one item id in the inventory on its back
   face. The threshold is 500. The machine in front is allowed to run while
   the count is below 500, and held idle at 500 or above. Sneak-use with an
   item sets the id. An empty filter does not hold the machine. The
   controller writes 1 while the machine may run and 0 while it is held,
   into every adjacent signal cable. The hold is not saved: the controller
   sets it every tick and clears it when the controller is removed.
3. **Redstone Interface** faces the redstone and keeps the cable on the
   opposite face. Redstone power on the front writes 0–15 into that cable
   and the interface does not also emit. With no redstone input, a cable
   value above 0 emits `min(15, value)` from the front.
4. All three are Assembler-manufactured (casing, two motors, four plates,
   Industrial). No crafting JSON.

**Alternatives rejected.** A signal core with no conduit to put it in; a
0–15 signal on the cable; a configurable threshold GUI in this slice; an
Arithmetic Unit; a Network Monitor; Operator Drones; saving the hold so a
machine stays stopped after the controller is gone.

**Consequences.** A successor that deletes the cable block and leaves only
a core is skipping the conduit. A successor that caps the cable at 15 is
moving the redstone limit onto the wire. A successor that leaves a machine
held after the controller is mined is inventing a latch. A successor that
builds drones here is in slice AC.

---

## ADR-0084 — The Flux Drill spends cells, not durability

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice T is the Flux Drill: area modes 1×1, 3×3,
vein and tunnel, on cell fuel. The README also names silk and fortune
modules and a portable cell, and it says the tool has no durability, only
charge. The Multitool still does not mine (ADR-0055, ADR-0069). The Flux
Cell is the T1 armour module (slice X), not a fuel item. Silk and fortune
belong on that module grid, not on a second socket invented for one tool.

**Decision.**

1. **Flux Drill** is Assembler-manufactured (casing, two motors, four
   plates, Industrial, 20 s). It is an item, not a block, and it has no
   crafting JSON. Harvest level is diamond. It does not break bedrock, and
   it does not break Grindless blocks, so a 3×3 cannot eat the factory.
   Drops are the block's normal drops. No silk, no fortune, no enchantments.
2. **No durability.** Charge is an integer on the stack. Each broken block
   costs 32 FU, one F1 tick. The drill will not start a break it cannot
   pay for.
3. **Drill Cell** is the fuel, and it is not the Flux Cell. Assembler:
   one copper coil and four iron plates, 4 s, Industrial. One cell is
   3,200 FU. The drill holds 6,400 FU, two cells. A cell is consumed only
   when the whole cell fits. Using the cell charges the drill in the other
   hand.
4. **Modes**, cycled by sneak-use, saved on the stack: single block; the
   3×3 perpendicular to where the player is looking; a face-connected vein
   of the same block, capped at 32; a horizontal 3×3 tunnel, eight blocks
   deep. One cell pays for a full tunnel (72 blocks is 2,304 FU). Extras
   stop when the charge runs out.

**Alternatives rejected.** Mining with the Multitool; naming the fuel Flux
Cell; a charger block; silk touch and fortune in this slice; letting the
drill dismantle Grindless machines; a vertical tunnel.

**Consequences.** A successor that puts durability on the drill is ignoring
this record. A successor that teaches the Multitool to mine is reopening
ADR-0055. A successor that adds silk or fortune here is starting the module
grid early. A successor that spends the armour Flux Cell as fuel is
colliding with slice X.

---

## ADR-0085 — A blueprint stamps from the inventory

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice U is the Blueprint Tool, the T2 checkpoint.
The README says a blueprint is captured, stamped, nameable and shareable,
and that construction drones build it from network items. Drones are slice
AC. Waiting for them would leave U with nothing a player can do.

**Decision.**

1. **Blueprint Tool** is Assembler-manufactured (casing, two motors, four
   plates, Industrial). No crafting JSON. Right-click two corners of a box.
   Sneak-use clears the first corner. The box may be at most 32 blocks on
   an edge and at most 512 non-air blocks. Anything past that is refused.
2. **A Blueprint item** is what the capture produces. It stores block id
   and facing, relative to the minimum corner. Status is not stored, so a
   stamped machine is idle. Block entities are not copied, so a stamp
   cannot duplicate an inventory. Blocks with no item are left out. The
   item stacks to one, so an anvil can name it, and the stack itself is
   how it moves between worlds. There is no file format.
3. **Stamping** spends the player's inventory, not the network. The
   minimum corner lands on the face that was clicked. The stamp is all or
   nothing: missing items or a blocked cell spends nothing. Creative mode
   does not spend. Rotation waits. Drones wait.

**Alternatives rejected.** A stamp that does nothing until drones exist; a
schematic file; copying block-entity contents; rotating the stamp in this
slice; pulling items out of the flux network before the drone bay exists.

**Consequences.** A successor that copies machine inventories into a
blueprint is duplicating items. A successor that pulls from the network
here is starting slice AC. A successor that writes blueprints to disk is
inventing a share format the item already is.

**Source-only qualification note, 2026-10-21.** Checkpoint4550b975 retains this
record's ordinary missing-item/blocked-cell none-spent preflight boundary and
legacy payload/bounds. It also rechecks targets and settles bounded inventory
payment around actual placements. If a later callback fails, already placed
cells are not rolled back. An explicit false result restores the attempted
payment; a throwing callback may already have changed the world, so that attempt
stays paid while unattempted resources are refunded and the error propagates.
This uncompiled source does not establish global rollback, crash atomicity or
universal zero loss. Coordinator review, affected contracts and representative
capture/stamp/load evidence remain pending; the old `first-blueprint` smoke only
observes an item and cannot supply that proof. No drone/network framework is added.

---

## ADR-0086 — The planner marks and does not pick up

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice V is the Deconstruction Planner. It marks a
region to tear down. Returning the items to storage waits until drones
exist (slice AC). Until then the planner may only mark, or the player
picks the block up with the Multitool relocate. A second wrench is
forbidden.

**Decision.** The planner is Assembler-manufactured (casing, two motors,
four plates, Industrial). Right-click two corners. Sneak-use clears the
mark. The box uses the same 32-block edge as a blueprint (ADR-0085). The
mark stays on the item: the two corners and the volume. It does not break
blocks, drop items, or relocate anything. The Multitool relocate is
unchanged. Drones, when they exist, read this mark. They are not in this
slice.

**Alternatives rejected.** A new pickup tool; breaking the region now and
spilling the drops; a second copy of the Multitool relocate; an unmarked
item that does nothing until AC.

**Consequences.** A successor that makes the planner mine or relocate is
inventing the wrench this record refused. A successor that returns items
to a chest here is starting the drone bay.

---

## ADR-0087 — A task-indexed route map, updated with the seam

*2026-10-04 · Accepted*

**Context.** Sessions start from the repository and nothing else. Finding where a
machine, a recipe or a sink actually lives means walking the tree, and the walk is
what gets lost when a session dies mid-way. A hand-maintained catalogue of function
bodies looks like the fix and then rots: the next session stops trusting it, which
is worse than having no catalogue.

**Decision.** [`docs/AGENT-MAP.md`](AGENT-MAP.md) is a task index. Each entry is one
kind of change, the few files to open, and the invariant that is easy to break.
Code remains the source of the functions. Design stays in the README, `MACHINES.md`,
`PROCESSES.md` and these records; the map links to them instead of restating them.
The same commit that changes a seam updates the map, the same way it updates the
changelog.

**Alternatives rejected.** A catalogue of function bodies (it rots, and sessions
stop trusting it). Leaving navigation entirely to search (it works once, and the
invariants the checks enforce are not visible from a file name).

**Consequences.** A successor that pastes method bodies into the map, or that
changes `ProcessGraph`, registration or a check without touching the map, is
reopening this record. `AGENTS.md` points at the map; it does not contain it.

---

## ADR-0088 — Grindless is a modpack-scale progression

*2026-10-04 · Superseded in part by [ADR-0089](#adr-0089--external-code-may-enter-with-its-own-license), [ADR-0090](#adr-0090--electric-arc-steel-is-the-first-line-past-contact) and [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded in part.** The ban on ever changing our license so that copyleft code can be
> copied is lifted. Unconfirmed licenses and All Rights Reserved material stay out. See
> ADR-0089. The schedule that puts the quest book next is withdrawn. The book is last.
> See ADR-0090. ADR-0107 replaces the compressed tier outline and reclassifies the
> implemented route to Sagittarius as prototype infrastructure.

**Context.** The early game is in: the T0 loop, slices A–E, the kiln, the atlas stub, the
Multitool wrench, belt junctions, the sorter, the Industrial gate, the Wire Mill, and the
contact process. The README still introduces the mod as something that deletes the opening
grind and then gets out of the way, and the research ladder stops at T6. That framing and
that cap are the wrong product. A successor who treats T6, or build-out slice BB, as the
finish line will build a convenience mod.

Per-tick Flux rates and stored amounts are a Java `long`: `FluxTier.nominal()`,
`ProcessRecipe.fuPerTick`, and `FluxStorage`. The ladder in code stops at F9,
`FluxTier.MAX_NOMINAL`, 2,097,152 FU/t. The largest rate one `long` can store is
`Long.MAX_VALUE` (9,223,372,036,854,775,807) FU/t.

**Decision.**

1. **The product is a full modpack-scale progression.** The early-game work already built
   stays the foundation. The "remove the early-game grind" framing is retired. So is the
   six-tier cap. T0–T6 remain the specified start. They are not the end.
2. **The ladder keeps going.** Intermediate tiers, endgame tiers, more processing lines,
   more materials. Kardashev scales are the large milestones: Type I, Type II, Type III.
   Megastructures sit along the way. Those names are milestones. They are not a claim that
   a Flux Unit is a physical watt.
3. **Planets are original.** Planetary gameplay has unique extractable resources. Space and
   interstellar gameplay are original to Grindless. This supersedes the planet-source half
   of ADR-0012. Satellites still have no upkeep. Horizon Gates stay commute infrastructure
   (ADR-0068), a milestone on the way, not the victory and not a mining dimension.
4. **The goal is the black hole at the centre of the Milky Way.** Arriving is the victory.
   That default is already chosen. The interior is a finite finale. Its concrete content is
   written when the route exists.
5. **Endgame generation targets the maximum rate a per-tick `long` can name:**
   `Long.MAX_VALUE` FU/t, from one source. F9 is not that ceiling. The `long` can store
   that single rate. It cannot express an economy operating at it:
   - a buffer of two or more ticks overflows (`rate × seconds` does not fit in a `long`,
     and buffers are sized in seconds of throughput);
   - the sum of two such sources does not fit in a `long`;
   - `FluxConversion.toFe` saturates at `Integer.MAX_VALUE` (ADR-0037), so the Forge
     Energy bridge cannot publish the rate;
   - `ProcessLogic.work` divides the `long` amounts in `double`, and not every integer
     above 2^53 is representable, so a partial tick near the ceiling is not exact.
   Any slice that adds those rates together, stores more than one tick of them, or
   publishes them over FE waits until the representation grows. This record does not
   change the type, the ladder, or the bridge.
6. **The quest book is original.** It is in the BetterQuesting style: lines, tasks,
   dependencies, rewards. BetterQuesting's code is not reused. See the license notes.
   Quality-of-life features known from other mods are in scope, built as original work.
7. **Ideas may inform the design. Code and assets from other mods do not enter this
   repository on the strength of this record.** Where a license was not confirmed, the
   default is no reuse.
8. **The schedule.** [`BUILD-OUT.md`](BUILD-OUT.md) keeps every slice already defined.
   G through K stay done. L through BB are **held**: defined, not started, not next.
   The next calendar is the **modpack expansion**. Its first slice is **BC — Original
   quest book**, identified and not implemented here. Later slices of that calendar
   (further tiers, megastructures, planets, the black-hole route) are not sliced yet.

### Licenses

Checked 2026-10-04 against the files named below. Grindless itself is MIT
([`LICENSE`](../LICENSE), copyright 2026 Ezequiel24123z): use, copy, modification, merge,
publication, distribution, sublicense and sale are permitted if that copyright notice and
permission notice are kept.

| Work | What the license allows | Reuse in this MIT repository |
| --- | --- | --- |
| **BetterQuesting** | `Funwayguy/BetterQuesting`, branch `1.12`, file `LICENSE`: MIT, copyright 2019 Funwayguy. That text would allow copying the licensed code into this repository if the Funwayguy copyright and permission notice are kept. The CurseForge page for the same project lists **All Rights Reserved**. File headers, the Standard Expansion, and later ports were not audited. | **Not confirmed.** The two statements contradict. Default applies: no reuse. The quest book is an original implementation only. |
| **GregTech** | GregTech CE Unofficial (`GregTechCEu/GregTech` and `GregTech-Modern`) is LGPL-3.0 on GitHub. LGPL-3.0 allows linking against the library and requires the library's source and relink rights to travel with a combined work. It does not allow copying that source into this tree and offering it under MIT alone. Their README also says use of the name "GregTech" depends on permission. Original GregTech by Gregorius Techneticies, and GregTech assets, were not re-verified file by file. | **No.** LGPL code cannot be vendored into this MIT-only tree. Unverified lines are no reuse. |
| **Ad Astra** | `terrarium-earth/Ad-Astra`, branch `1.20.x`, Terrarium License v1. Files they define as Code (including `.java`, data, shaders, build scripts, markdown and toml) are MIT, so copying those files is allowed if the copyright and MIT notice stay. Everything not defined as Code is All Rights Reserved. | **Code: allowed only with the notice kept. Assets: no.** This record copies neither. Planetary and interstellar content here is original. |
| **Similar mods** | Mekanism, Thermal, Create, and the rest of the genre were not individually confirmed. | **Unsure. No reuse.** Ideas may still inform the design. |

**Alternatives rejected.**

- Staying an early-game convenience mod with six tiers.
- An empty "you arrived" anecdote at the black hole, with no finale.
- An endless second game inside the hole.
- Copying BetterQuesting, GregTech, or Ad Astra code or assets into this repository while
  any license above is unconfirmed or incompatible.
- Implementing the wider energy representation, the quest book, or slice BC in the same
  change as this record.

**Consequences.** A successor who starts the washer, Slice F, or any held row L–BB because
the old **Next slice** line said L is ignoring this record. The next slice is BC, and BC
is not started here. A successor who vendors another mod's sources because "the idea is
the same" is ignoring the license table. A successor who sets the endgame generator to
F9, or who widens `long` inside a feature slice without a record that the representation
grew, is missing the ceiling in point 5. A successor who writes the black-hole interior
before the route exists is early; a successor who makes that interior endless, or empty,
is reopening the rejected alternatives.

---

## ADR-0089 — External code may enter with its own license

*2026-10-04 · Accepted*

**Context.** ADR-0088 kept other mods' code out of this repository. Part of that ban was
ours: GregTech CE Unofficial and GregTech-Modern are LGPL-3.0, and a MIT-only tree cannot
offer those files under MIT alone. The copyright holder has now said they are willing to
change Grindless's license in order to implement external code.

Changing our grant does not create a right in someone else's work. It only lets us accept
obligations we were previously refusing.

**Decision.**

1. **Grindless's own code and documentation stay MIT.** [`LICENSE`](../LICENSE) says so.
   Pack authors can still ship that work.
2. **The copyright holder accepts an extra upstream license when a slice copies code that
   requires one.** The copied files keep their notices. The upstream license text is added
   in that same slice. A file with its own notice is governed by that notice.
3. **LGPL-3.0 code may enter on those terms.** GregTech CE Unofficial
   (`GregTechCEu/GregTech`) and GregTech-Modern are LGPL-3.0. Copying them means those
   files stay LGPL-3.0, the LGPL text ships with the repo, and the mod's distribution
   satisfies LGPL for those parts (source and the ability to replace them). The GregTech
   name is a separate permission, stated in their README, and is not granted here.
4. **MIT code may already enter, with its notice kept.** Ad Astra's Terrarium License v1
   puts Code under MIT. BetterQuesting's GitHub `LICENSE` on branch `1.12` is also MIT.
   Neither needs a change to our MIT grant. Ad Astra's non-code is All Rights Reserved.
   BetterQuesting's CurseForge page lists All Rights Reserved, which contradicts the
   GitHub file. That contradiction is not fixed by editing our license. Both stay unused
   until the grant is one statement. The quest book stays an original implementation.
5. **All Rights Reserved material stays out.** No choice of license for Grindless unlocks
   it. Original GregTech by Gregorius Techneticies, and assets whose license was not
   checked file by file, stay out. Similar mods that were not confirmed stay out.
6. **This record copies nothing.** No `LICENSE` swap to GPL-3.0. The whole project is not
   relicensed in advance. The first slice that copies a file is the slice that adds that
   file's license text.

**Alternatives rejected.**

- Relicensing all of Grindless to GPL-3.0 now, with no file being copied. That is stricter
  than the LGPL code under discussion, and it still does not unlock All Rights Reserved
  assets.
- Treating the owner's willingness as permission to copy BetterQuesting, Ad Astra assets,
  or any unchecked mod.
- Keeping the permanent "MIT-only, so LGPL can never be vendored" ban from ADR-0088.

**Consequences.** A successor who copies GregTech sources into a MIT file, or who drops the
LGPL notice, is ignoring this record. A successor who copies BetterQuesting or an Ad Astra
texture because the license file now mentions upstream terms is ignoring points 4 and 5.
A successor who relicenses the whole tree without a copied file is early.

---

## ADR-0090 — Electric-arc steel is the first line past contact

*2026-10-04 · Accepted*

**Context.** ADR-0088 named a modpack-scale progression and left the steps after the quest
book unidentified. It set the next slice to BC, an original quest book. The owner has
deferred that book until the end of the pack. The urgent calendar, which ADR-0088 names
and does not slice, is intermediate and endgame tiers, more processing lines and more
materials, then megastructures and the Kardashev scales, then original planets and
interstellar travel, then arrival at the galactic-centre black hole. The quest book and
the in-game guide come after that arrival. They are last.

The foundation on this branch ends at the contact process (slice K). Rows L–BB stay held.
A successor who starts the washer, the quest book, or an Autoclave from the old Next line
is guessing.

`PROCESSES.md` opens the materials stratum with alloys, and the first alloy it spells out
is steel, three ways. Steel is already a supplied material, and the Press, rod, gear and
Wire Mill recipes already exist for it once an ingot does. Nothing produces the ingot.
Of the three routes, only electric arc runs on a shipped machine with shipped inputs:
iron ingots and `#grindless:carbon`, in the Arc Furnace, which already holds 1500 °C, a
reducing atmosphere and an electric field (ADR-0059). Oxygen blow needs molten metal and
the Atmospheric Intake (held O and M). Direct reduction needs hydrogen (held M). The
Haber line needs an Autoclave plus nitrogen and hydrogen from those same held machines.
Shipping it either cannot be run, or it resumes M. The washer is L.

**Decision.**

1. **The quest book is last.** BC stays the original quest book. It moves to the end of
   the modpack expansion, after arrival at the black hole, together with the in-game
   guide. It is not the next slice. L–BB stay held. This record copies no BetterQuesting,
   GregTech or Ad Astra code or assets. Grindless's own work stays MIT (ADR-0089).
2. **The next slice is BD — Electric-arc steel.** One material line, on the machine the
   graph already names. That machine is the Arc Furnace. No new block, no new research
   tier, no new blueprint. A second furnace would duplicate an envelope that already
   contains the reaction.
3. **Ratio.** `PROCESSES.md` writes `1 u scrap iron + 0.1 u carbon → 1.0 u` in 14 s.
   A tenth of an item does not exist (ADR-0034: 1 u = 1 ingot). The integer batch that
   keeps both the carbon ratio and the throughput is **10 iron ingots + 1 carbon → 10
   steel ingots in 140 s** (ten times 14 s), at F1, family `ARC_FURNACE`. The named
   temperature is 1600 °C. The furnace's 1500 °C hold sits inside that band (±15 %).
   Scrap is `forge:ingots/iron`, not a new item. The output is `forge:ingots/steel`.
   There is no slag and no CO: the route names neither (ADR-0036). The electric field
   stays the furnace's held field. `ProcessRecipe` does not grow a field slot for one
   host that already applies it.
4. **The other two steel routes wait.** Oxygen blow and direct reduction are not this
   slice. Planets, megastructures and the quest book are not this slice.

**Alternatives rejected.**

- The quest book, or the in-game guide, as the next slice.
- Resuming any held row L–BB, including the washer, electrolysis, the Atmospheric Intake
  and the Induction Furnace.
- Haber, the Autoclave, or nitric acid in this slice. Their feeds are M, or the line
  cannot run.
- A new alloy furnace, or a T7 blueprint, for a reaction the Arc Furnace already holds.
- `1 iron + 1 carbon → 1 steel`, which spends ten times the coal the graph charges.
- `10 iron + 1 carbon` in 14 s, which is ten times the throughput.
- A scrap item, a slag output, or bottled hydrogen.

**Consequences.** A successor who starts BC, the washer, or the Autoclave because an older
Next line said so is ignoring this record. A successor who adds oxygen blow or direct
reduction here is starting held feeds. A successor who changes the batch without keeping
0.1 carbon per ingot and 14 s per ingot is rewriting the graph.

---

## ADR-0091 — Refractory brick is one slag in twenty seconds

*2026-10-04 · Accepted*

**Context.** Slice BE is slag's named sink (ADR-0036). `PROCESSES.md` writes refractory
brick as `alumina + silica [T 1400]` in 20 s, and names no counts. BUILD-OUT forbids that
route, the Kiln, and a new furnace. The Arc Furnace envelope is 1200–3500 °C, so 1400 °C
is inside it. The Kiln envelope stops at 900 °C. A 1400 °C band of ±15 % tolerates down
to 980 °C, which the Kiln still cannot reach. The integer batch is not in the graph.

The furnace holds 1500 °C for R1 and for electric-arc steel (ADR-0090). That hold sits
inside 1400 ±15 % (1190–1610 °C), so a 1400 °C recipe runs at full speed without a second
temperature. Alumina and silica are not items. One reduction already emits one slag.

**Decision.**

1. **The product is a reagent**, `grindless:refractory_brick`. It is the lining. It is not
   a placeable block, and it is not `minecraft:brick`. Vanadia keeps its vanilla-brick
   support. Aggregate and road fill stay later sinks. No new machine, no new blueprint.
2. **Host.** `ceramic/refractory_brick` runs on the Arc Furnace. Family lock is the host.
   The recipe names 1400 °C and nothing else: the ceramics row names no atmosphere and no
   field, and an unwritten condition is not a condition. The held reducing atmosphere and
   electric field stay on the machine. `ProcessRecipe` does not grow a field slot.
3. **Integer batch.** **1 slag → 1 refractory brick in 20 s** at 1400 °C, F1. Twenty
   seconds and 1400 °C are the only numbers the product already has. One unit in and one
   unit out is the canonical unit (ADR-0034). One slag is what one reduction emits, so
   one reduce cycle feeds one brick cycle with nothing left over. Own work stays MIT
   (ADR-0089). This record copies nothing.

**Alternatives rejected.**

- Alumina + silica. Those feeds are not items, and BUILD-OUT forbids the route.
- The Kiln, or a new furnace. 1400 °C is outside the Kiln even at the edge of tolerance.
- Retuning the Arc Furnace from 1500 °C to 1400 °C. The existing hold is already optimal
  for this band, and R1 is written at 1500 °C.
- Naming `REDUCING` or an electric field on the recipe. Naming `OXIDISING` would refuse
  the hold the furnace already has.
- 2 slag → 1 brick, as if the two mineral names were a count.
- 1 slag → 4 bricks, copying vanadia's four vanilla bricks.
- 10 slag → 10 bricks in 200 s, or in 20 s. There is no tenth of an item to clear, so a
  steel-style scale either waits ten times longer or runs ten times faster.
- A placeable brick, an Industrial gate, or spending the brick as a furnace upgrade in
  this slice. The sink is the recipe that consumes slag.

**Consequences.** A successor who adds alumina or silica, moves the recipe to the Kiln, or
retunes the furnace is ignoring this record. A successor who changes the 1:1 batch or the
20 s is inventing a ratio the product did not have. A successor who ships aggregate or
road fill here is starting the next sink.

---

## ADR-0092 — Metallurgical silicon runs cold on sand and quartz

*2026-10-04 · Accepted*

**Context.** Slice BF is the carbothermic line in `PROCESSES.md`:

```
1 u silica + 2 u carbon
  [T 1900 · atm reducing]  14 s
  ──> 1 u metallurgical silicon + 2 B CO             (Arc Furnace, T1)
```

Two facts are not implementable as written.

Silica is not a shipped item. ADR-0091 refused to invent it as a brick feed. This line
names it as the feed, so a recipe that waits for a silica item cannot be run. Sand and
nether quartz are both silica, and both already exist in vanilla. `#minecraft:sand`
also contains red sand, which is not that feed. Glass is silica that has already been
melted. Sandstone is a block of sand, not one unit. A new compound item would be a
reagent with no source.

The Arc Furnace holds 1500 °C, a reducing atmosphere and an electric field. BUILD-OUT
forbids retuning that hold and forbids a new furnace. A relative band at 1900 °C is
optimal from 1615 °C to 2185 °C and tolerated from 1330 °C to 2470 °C. 1500 °C is
inside tolerance and outside the optimal zone, so the line runs and costs time, never
yield (ADR-0040). The furnace envelope is 1200–3500 °C, so 1900 °C is inside the
machine. The Kiln stops at 900 °C and cannot reach the tolerance floor. The counts are
already integers: one silica, two carbon, fourteen seconds, two buckets. There is no
tenth of an item to clear.

Metallurgical silicon is a grade. `PROCESSES.md` says it is useless for circuits.
Putting the output on `forge:ingots/silicon` would let a later circuit recipe accept
99 % metal. Electronic silicon, wafers and the Siemens line are not this slice. The
product is a reagent, the same boundary slag and refractory brick use (ADR-0033).

CO already has a named sink (ADR-0036, ADR-0062): vent, or a neighbouring tank, then
the Thermal Generator. Two buckets is that same fluid, twice.

**Decision.**

1. **Feed.** `#grindless:silica` is the item ids `minecraft:sand` and
   `minecraft:quartz`. No silica item is registered. The values are item ids, not
   `#minecraft:sand`: that tag also contains red sand, and nether quartz has no
   vanilla item tag. ADR-0051 forbids writing one into the `minecraft` namespace.
2. **Product.** `grindless:metallurgical_silicon`, one item. It is not a supplied
   material, so the ore line, plates, rods, gears and wire do not appear. It is not
   `forge:ingots/silicon`.
3. **Host and ratio.** `silicon/metallurgical` runs on the Arc Furnace: 1 silica + 2
   carbon → 1 metallurgical silicon + 2 B vented CO, in 14 s, at F1, at 1900 °C, in a
   reducing atmosphere. The furnace hold stays 1500 °C and reducing. That hold admits
   the band and is not optimal. No new block, no dial, no blueprint. The electric field
   stays on the machine. `ProcessRecipe` does not grow a field slot. Own work stays MIT
   (ADR-0089). This record copies nothing.

**Alternatives rejected.**

- A silica item, glass, red sand, slag, or sandstone as the feed.
- Retuning the Arc Furnace to 1900 °C, or a second furnace that holds 1900 °C.
- Writing the recipe at 1500 °C so the hold is optimal. That rewrites the graph.
- Widening this recipe's band so 1500 °C counts as optimal.
- Scaling to 2 silica + 4 carbon. The graph is already an integer batch.
- `forge:ingots/silicon`, a full supply-catalogue material, plates, wire, electronic
  silicon, or wafers.
- A stored CO item, or a new gas. The sink is the one R1 already uses.
- Naming an electric field on the recipe. The graph names temperature and atmosphere.
- The Kiln. 1900 °C is outside it even at the edge of tolerance.

**Consequences.** A successor who ships a silica item, retunes the furnace, or emits
`forge:ingots/silicon` is ignoring this record. A successor who changes the 1:2:1 batch
or the 14 s is rewriting the graph. A successor who adds electronic silicon, a wafer, or
a Siemens step here is starting the next line. A successor who treats the 1500 °C hold
as a bug and moves it is retuning.

---

## ADR-0093 — Zone refining is ten metallurgical silicon in six hundred seconds

*2026-10-04 · Accepted*

**Context.** Slice BG says the concrete line is chosen when the slice starts. The graph
past metallurgical silicon is the electronic grade: four routes, then a boule and a
wafer.

Vacuum float is a T5 Vacuum Furnace. Orbital growth is the orbital stage. Neither is
this calendar. Siemens is `1 u met-Si + 3 B HCl` at 300 °C, then deposition at 1100 °C
reducing. Nothing shipped produces hydrochloric acid, and chlor-alkali is held row M.
A relative band at 1100 °C tolerates up to 1430 °C, so the Arc Furnace's 1500 °C hold
would not start the deposition. A relative band at 300 °C tolerates up to 390 °C, so
the Chemical Reactor's 450 °C hold would not start the chlorination. Shipping Siemens
either invents an acid or resumes M, and neither step runs on the locked holds.

Zone refining's feed is metallurgical silicon, which exists. `PROCESSES.md` gives it
60 s and **0.70 u** out per unit in. The written conditions are `T 1420 ±5 · atm inert`
on an Induction Furnace MK III. That furnace is held row O, with the Caster. An
absolute ±5 °C band tolerates 1410–1430 °C, so 1500 °C is outside it. Inert does not
match the reducing hold, and atmosphere is an exact match. Putting those conditions on
the Arc Furnace produces a recipe that never runs. ADR-0090 refuses a line that cannot
be run.

A relative band at 1420 °C is optimal from 1207 °C to 1633 °C. The 1500 °C hold sits
inside it. The Kiln stops at 900 °C and cannot reach the tolerance floor at 994 °C.
The Arc Furnace envelope is 1200–3500 °C, so 1420 °C is inside the machine. The 0.30
that does not come out is a yield, not a named byproduct. The boule names inert
atmosphere and static agitation at 1450 °C, and its feed is this product.

**Decision.**

1. **The line is zone refining's ratio, on the Arc Furnace.** `silicon/zone_refining`.
   No new block, no dial, no blueprint. Family lock is the host. The Induction Furnace
   stays held. The ±5 °C band and the inert atmosphere are not written on this recipe:
   either one would refuse the hold. The temperature centre stays 1420 °C, as the
   ordinary relative band. Atmosphere is unnamed. An unwritten condition is not a
   condition, and the held reducing atmosphere stays on the machine. `ProcessRecipe`
   does not grow a half-width field for a band this slice is not applying. Own work
   stays MIT (ADR-0089). This record copies nothing.
2. **Integer batch.** **10 metallurgical silicon → 7 electronic silicon in 600 s** at
   F1. That is 0.70 per unit and 60 s per unit, the smallest integers that keep both.
   The product is `grindless:electronic_silicon`. It is not a supplied material, so
   plates, rods, gears and wire do not appear. It is not `forge:ingots/silicon`. There
   is no slag and no gas.
3. **What waits.** Siemens, trichlorosilane, hydrochloric acid, the boule, the wafer,
   vacuum float and orbital growth. The next slice is BH — megastructures, not started.

**Alternatives rejected.**

- Siemens, or a deposition-only step. The acid is not a fluid, and 1100 °C does not
  admit 1500 °C.
- The Induction Furnace or the Caster. That resumes held row O.
- Writing ±5 or `INERT` on the Arc Furnace recipe. The hold would report out of band.
- Retuning the furnace from 1500 °C to 1420 °C. R1 is written at 1500 °C.
- Moving the centre to 1500 °C. The route's temperature is 1420 °C, and the relative
  band already makes the hold optimal.
- 1 → 1, which erases the 0.30 loss. 10 → 7 in 60 s, which is ten times the throughput.
  100 → 70 in 6000 s, which waits ten times longer for the same ratio.
- A fifth route with a different yield or a different time.
- `forge:ingots/silicon`, a boule, or a wafer.
- A byproduct item for the missing 0.30. The route names none.
- Megastructures, planets, or the quest book.

**Consequences.** A successor who adds ±5 or inert to `silicon/zone_refining` stops the
line. A successor who builds the Induction Furnace here is resuming O. A successor who
changes the 10:7 batch or the 600 s is rewriting the yield or the time. A successor who
ships Siemens, a wafer, or a boule here is starting another line. A successor who treats
the relative band as a mistake and restores ±5 on this furnace is making the recipe
unrunnable. BH is next and is not started.

---

## ADR-0094 — The first megastructure is the Ground Array

*2026-10-04 · Superseded in part by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded in part.** The registered structure and its tested storage behaviour remain
> useful prototype code. Its Industrial survival recipe and position as the next
> megastructure are withdrawn pending placement in the T0-T15 route. See ADR-0107.

**Context.** Slice BH says the concrete structure is chosen when the slice starts. The
ship column then names Kardashev Type I, II and III. Those are the milestones after the
first structure. They are not three more blocks in the same change.

The structures already named cannot be this one.

- The Dyson Collector is orbital-ring solar at F8–F9 and takes a space programme
  (held AO–AR). It is the source that outruns a singularity, which is past the first
  structure.
- The Stellar Forge is necessarily orbital (held AS).
- The Planetary Engine moves a colony. Planets are the following slice. Colonies are
  held AU.
- The Space Elevator is held AX.
- The Singularity Reactor is held AN. Fission, fusion and the Arc Reactor are held
  AF, AK and AE. The README still calls the Arc Reactor the first plant of that tier,
  and that plant is not this calendar.
- Solar generation is held Q. F2 is that row. F3 through F7 are the held plants above.
  Shipping a generator claims one of those tiers.
- A formed multiblock with hatches is the Slice F kernel (held AD), and that kernel is
  for the Arc Reactor. A general shape API here is that row.
- Endgame generation at `Long.MAX_VALUE` FU/t cannot be operated: a multi-tick buffer
  and the sum of two such sources do not fit in a `long` (ADR-0088). This slice does
  not widen the type.

What the factory can already make, and what a structure can already do, is storage.
The capacitor bank holds ten seconds of an MK1 pylon, 200 ticks of 512 FU/t, and only
while a pylon covers it (ADR-0064). It is one block. MK3 is the largest pylon the
factory has, 32,768 FU/t. Ten seconds of that is a buffer the bank cannot be. The
number is the same rule, applied to the ceiling that already ships. It fits in a
`long`.

Refractory brick is the lining (ADR-0091). Steel is the frame the arc just learned to
pour (ADR-0090). Electronic silicon is for circuits, and that line is held AI.
Metallurgical silicon is for solar cells, and solar generation is held Q. Neither
grade is a feed here.

Hand placement at this point in the design is a 3×3 to a 5×5 (MACHINES.md). A 3×3 is
the smallest footprint that is a structure rather than another machine. Minecraft
notifies only the six orthogonal neighbours, so a corner casing has to tell the
controller itself. That is this ring, not a hatched kernel.

**Decision.**

1. **The structure is the Ground Array.** One controller, `grindless:ground_array`,
   and eight `grindless:array_casing` blocks in the horizontal Moore neighbourhood on
   the same Y. The controller is the centre. Seven casings are not a structure. The
   check is this footprint only. There are no hatches, no item or fluid slots, no
   parametric size, no process family and no new research tier. The casing's status
   property lists idle, running and starved, because a property with one value is
   illegal and the dedicated server will not boot. The casing never leaves idle.
   Slice F stays held.
   Own work stays MIT (ADR-0089). This record copies nothing.
2. **What it does.** While the ring is complete and a pylon covers the controller, the
   array adds **6,553,600 FU** to that network. That is 200 ticks of MK3 throughput,
   the same ten seconds the capacitor uses for MK1. It does not generate. It does not
   project a supply cube. Uncovered, or with the ring broken, it adds nothing. A
   casing may sit in more than one ring: layout is the point, and the cost is still
   the blocks. The extra is saved beside the capacitor banks, not as one of them.
3. **How it is built.** Both blocks are Assembler recipes under the Industrial
   blueprint that already exists. T2+ is manufactured (ADR-0017). The casing is
   **4 refractory bricks + 1 steel ingot → 1 casing** in 20 s at F1. The controller is
   **1 machine casing + 4 steel plates + 4 refractory bricks → 1 ground array** in
   20 s at F1. Four bricks line one part. They are not the eight of the ring, which
   would hide the structure inside one item. No crafting-table recipe.
4. **What waits.** Kardashev Type I, II and III. The Dyson Collector, the Stellar
   Forge, the Planetary Engine, the Space Elevator, and every held generator. The
   next slice is **BI — Original planets**, not started.

**Alternatives rejected.**

- Type I, II and III as three further structures in this slice.
- The Dyson Collector, or any F2–F9 generator. Those tiers belong to held rows, and
  F9 is not the `long` ceiling (ADR-0088).
- A single block with the capacity baked in. That is a larger capacitor, not a
  structure the player builds.
- The Slice F kernel, hatches, or a variable rectangle. AD is held, and the Arc
  Reactor is the machine that kernel is for.
- Filling the shaft in for the player, the way a pylon does. A megastructure is the
  blocks the player places.
- Electronic silicon or metallurgical silicon in the recipe. Those grades have named
  uses this slice does not start.
- A new blueprint or a T7 gate. Industrial already gates the Assembler.
- Planets, interstellar travel, the black hole, or the quest book.

**Consequences.** A successor who adds a generator, a supply cube, or a second
footprint here is starting another structure. A successor who changes 6,553,600 FU
without keeping ten seconds of MK3 is inventing a buffer. A successor who moves the
recipe to the crafting table is ignoring ADR-0017. A successor who spends electronic
silicon, or who builds the Dyson Collector, Type I, or the multiblock kernel here, is
outside this record. BI is next and is not started.

---

## ADR-0095 — Luna is the first original planet

*2026-10-04 · Superseded in part by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded in part.** Luna, its regolith and helium-3 remain. The Industrial Lunar Link
> recipe and early survival access are withdrawn; local-system flight belongs to T10. See
> ADR-0107.

**Context.** Slice BI says the concrete world is chosen when the slice starts. The ship
column also names interstellar travel. The owner kept that travel, the black-hole finale
and the quest book out of this change. A successor who builds all seven fallback worlds,
a Horizon Gate, a rocket, or a void full of ore is guessing.

The worlds already named cannot all be this one.

- Luna's signature resource is helium-3, already written in `PROCESSES.md` as the D–³He
  fuel. The other signatures are iron the overworld already has, fusion fuels with the
  same missing reactor, or process envelopes that are not an extractable.
- The fallback set as a whole is the planet registry (held AT) plus the travel that
  this change does not ship.
- A Horizon Gate is held AW, and it is a dialed pair after a far ring is delivered
  (ADR-0068). Rockets are held AO. There is no way to deliver a far ring.
- A dimension whose job is more of the pack's ores is the mining dimension ADR-0068
  and the README both reject.
- Vacuum damage without an exosuit (held X and Y) makes the world unusable.
- Helium-3 as a `forge:raw_materials` entry would enter the overworld vein scan
  (ADR-0004). An ore block in the ground would be mined with a pickaxe, which is the
  tunnel this mod exists to leave behind.
- The extractor outputs items. A fluid would need a tank that extractor does not have
  (ADR-0015). Fusion, the named sink, is held AK.

What the factory can already do is extract a chunk vein and spend stored FU. A capacitor
bank holds ten seconds of an MK1 pylon, 102,400 FU (ADR-0064). That number fits in a
`long`. The Ground Array's larger buffer is storage for the grid, not a toll this planet
has to charge.

**Decision.**

1. **The world is Luna**, dimension `grindless:luna`. One world. The surface is
   `grindless:lunar_regolith` over one layer of bedrock, flat, with features, lakes and
   structure overrides off. The biome spawns nothing and adds no ores. The sky uses the
   vanilla end effect and a fixed night, with enough ambient light to walk. There is no
   vacuum damage and no life-support check. The other fallback worlds wait.
   Own work stays MIT (ADR-0089). This record copies nothing.
2. **The extractable is helium-3.** Every Luna chunk's vein is that material. Richness
   still varies. The overworld pool is unchanged, and `VeinGenerator.VERSION` is not
   bumped. The item is the reagent `grindless:helium_3`, tagged `grindless:helium_3`.
   It is not a supply-catalogue material, not `forge:raw_materials/helium_3`, and not an
   ore block. A Crude or Terrestrial Extractor, and the Prospector's Scanner, use this
   pool when they stand on Luna and the pack pool everywhere else. The sink remains the
   D–³He cycle already named in `PROCESSES.md`. This slice does not build the reactor,
   and it does not add a recipe that consumes the item.
3. **The way there is the Lunar Link**, `grindless:lunar_link`. One block. Right-click
   under a pylon and it draws **102,400 FU** from that network, the capacitor's buffer,
   at whatever throughput the covering pylon can carry. When the charge is full and the
   player is within four blocks, they arrive on the regolith beside a return pad. The
   return sends them to the link they left from and does not draw again. A link that is
   already on Luna only returns. There is no address, no second destination, and no
   cargo tariff. Nether and End are not planets; a link placed there still goes to Luna
   and comes back to where it stood.
4. **How it is built.** Assembler, Industrial, 20 s at F1: **2 array casings + 1 machine
   casing → 1 lunar link**. T2+ is manufactured (ADR-0017). No crafting-table recipe.
   Helium-3 is not an ingredient: the link is how the gas is reached.
5. **What waits.** The other planets. Interstellar travel, which is the next slice and
   is not started. The black-hole interior. The quest book. Kardashev Type I, II and
   III. Horizon Gates, rockets, colonies, telepresence and life support.

**Alternatives rejected.**

- All seven fallback worlds, or Tharsis because iron is already extracted.
- A mining dimension, a stone world, or ore features in the flat generator.
- Registering helium-3 as a pack ore so the overworld can roll it.
- A fluid, a cell crafted on the ground, or a burn recipe in the Thermal Generator.
- The Horizon Gate, a rocket, or a creative item that teleports with no FU cost.
- Charging the Ground Array's 6,553,600 FU. That toll waits on MK3 throughput for a
  trip the capacitor can already pay.
- Vacuum damage, or an exosuit requirement. Those rows are held.
- Interstellar travel, a second star, or the black hole.
- Copying Ad Astra's planets, textures or code. The end sky is vanilla.

**Consequences.** A successor who adds ore to Luna, or who puts helium-3 on the
overworld vein list, is building a mining dimension. A successor who removes the FU
cost is shipping a creative teleport. A successor who makes the return draw power is
stranding a player on a world with no pylon. A successor who starts Tharsis, the
Horizon Gate, fusion, or interstellar travel here is outside this record. BJ is next
and is not started.

---

## ADR-0096 — The first interstellar hop is the Drift

*2026-10-04 · Superseded in part by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded in part.** The Drift and the Starward Link stay. That link is a placeholder.
> Interstellar travel is a station the player rides. A rocket is the local-system flight,
> and it is no longer rejected for that flight. See ADR-0097. The sentence below that
> calls BK next used that id for arrival. BK is now the teleportation orbs and is not
> next. BM, the station, is next. ADR-0107 additionally withdraws the Industrial
> Starward Link recipe and places the Drift in T13.

**Context.** Slice BJ says the concrete route is chosen when the slice starts. The player
can already reach Luna, which is still a moon of the home star. The owner kept the
black-hole interior, the quest book, the Kardashev scales and any further planet out of
this change. A successor who builds a rocket, a Horizon Gate, Tharsis, or the hole itself
is guessing.

The ways already named cannot be this one.

- A rocket is held AO. A Horizon Gate is held AW, and it is a dialed pair after a far
  ring is delivered (ADR-0068). There is still no way to deliver that ring.
- The other fallback worlds are further planets. The row forbids them. A world with its
  own extractable is the planet slice, which already shipped Luna.
- A dimension whose veins are the pack's ores is the mining dimension ADR-0068 rejects.
- Arriving at the black hole is the victory (ADR-0088). The interior is a finite finale
  written when that arrival is the slice. Shipping the hole here finishes the route
  before the arrival slice exists.
- Vacuum damage without an exosuit (held X and Y) makes the stop unusable.
- The lunar toll is one capacitor, 102,400 FU. ADR-0095 refused the Ground Array's
  6,553,600 FU for that trip, because a capacitor can already pay it. Leaving the star
  is the trip that larger buffer was left for. The number fits in a `long`.

**Decision.**

1. **The hop is the Drift**, dimension `grindless:drift`. One stop, on the way from the
   home star toward the galactic centre, and short of the hole. The floor is one layer of
   `grindless:drift_deck` over one layer of bedrock. No features, no lakes, no structures,
   no spawns. The sky is the vanilla end effect, with enough ambient light to walk. There
   is no vacuum damage. The vein list is empty, so an extractor and the scanner find
   nothing. It is not a planet and not a mining dimension.
   Own work stays MIT (ADR-0089). This record copies nothing.
2. **The way there is the Starward Link**, `grindless:starward_link`. One block. Right-click
   under a pylon and it draws **6,553,600 FU** from that network, the Ground Array's
   buffer, at whatever throughput the covering pylon can carry. When the charge is full
   and the player is within four blocks, they arrive on the deck beside a return pad. The
   return sends them to the link they left from and does not draw again. A link that is
   already on the Drift only returns. There is no address, no second star, and no cargo
   tariff. Luna, the Nether and the End are not the Drift; a link placed there still goes
   to the Drift and comes back to where it stood. The lunar return is a different record.
3. **How it is built.** Assembler, Industrial, 20 s at F1: **1 lunar link + 4 array
   casings → 1 starward link**. T2+ is manufactured (ADR-0017). The lunar link is the
   step already built. Four casings are half a ring. No crafting-table recipe. Helium-3
   is not an ingredient.
4. **What waits.** Arrival at the black hole, which is the next slice and is not started.
   Its interior is written then. Further planets. Kardashev Type I, II and III. The quest
   book. Horizon Gates, rockets, colonies, telepresence and life support.

**Alternatives rejected.**

- The black hole, or an empty "you have left the star" with nowhere to stand.
- A chain of named stars. One hop is the route. A catalogue is several slices.
- Tharsis, Vulcan, Kryos, Erebus, Thalassa, Helios, or any world with a signature ore.
- Filling the Drift with the overworld vein pool.
- A rocket, a launch pad, or a Horizon Gate.
- Charging another capacitor. That toll is the moon. This toll is the array.
- Vacuum damage, or an exosuit requirement. Those rows are held.
- Copying Ad Astra's rockets, stations, textures or code. The end sky is vanilla.

**Consequences.** A successor who adds a vein to the Drift is building a planet or a
mining dimension. A successor who removes the FU cost is shipping a creative teleport. A
successor who makes the return draw power is stranding a player on a deck with no pylon.
A successor who opens the black hole, a second star, or another planet here is outside
this record. BK is next and is not started.

---

## ADR-0097 — Local trips are rocket flights

*2026-10-04 · Superseded in part by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded in part.** The survey rocket is unchanged. The station is no longer "next
> and not started": it is the ride off the star. The Starward Link stays registered and
> no longer moves a player. The Lunar Link still does. See ADR-0098. ADR-0107 withdraws
> the prototype recipes and schedules local-system flight for T10.

**Context.** ADR-0088 chose the victory: arriving at the black hole at the centre of the
Milky Way is the win, and the interior is a finite finale, not an empty marker and not an
endless second game. ADR-0096 then shipped the Drift as one deck short of that hole, and
a Starward Link that spends the Ground Array's buffer to stand there. The Lunar Link
does the same kind of thing for Luna, at the capacitor's toll. Both are right-click
teleports. The owner has changed space travel. A third link, from the Drift into the
hole, is the arrival this record refuses.

The ways already named cannot be the new model.

- Held row AO is a launch pad and a rocket inside the six-tier orbital layer, together
  with payload mass. Resuming AO also resumes the held schedule. This flight is the
  modpack expansion, not that row.
- A Horizon Gate is held AW. It is a dialed pair after a far ring is delivered.
- Copying a rocket from another mod is a license question, not a shortcut. Checked
  2026-10-04:
  - **Galacticraft** as shipped by micdoodle8 (`micdoodle8/Galacticraft`, `LICENSE.txt`)
    is a custom non-commercial grant. It is not MIT, and it does not allow this
    repository to offer those files under MIT. Not copied.
  - **TeamGalacticraft/Galacticraft** is MIT, copyright 2019–2025 Team Galacticraft.
    A copy would be allowed if that notice were kept. That tree is a later Minecraft
    generation on Fabric. Dropping it in here would not build. Not copied.
  - **Advanced Rocketry** (`Advanced-Rocketry/AdvancedRocketry`, branch `1.12`) is MIT,
    and the CurseForge page for that project says MIT. **Advanced Rocketry 3** is
    listed All Rights Reserved. The 1.12 sources are a different game version. AR3
    cannot be vendored. Neither is copied. The station this record names is the
    slice that would resemble a warp ship, and that slice is not started.
  - **Ad Astra** stays as ADR-0088 and ADR-0089: code is MIT only with the notice
    kept, textures are All Rights Reserved. Not copied.
- No assets are copied from any of them. Own work stays MIT (ADR-0089).

**Decision.**

1. **Local-system trips are real flights.** The player seats a survey rocket on a
   launch pad. The rocket climbs one block a tick until it reaches the dimension's
   build ceiling, three blocks of clearance under the exclusive limit so the rider's
   head stays inside the world. A landing map then lists the bodies of this star.
   Choosing one lands the rocket. This slice's bodies are the home world
   (`minecraft:overworld`) and Luna (`grindless:luna`). The Nether, the End, the Drift
   and any black-hole id are not on the map, and a rocket there does not launch.
   The shape of the flight — up to the ceiling, then a map — is the Galacticraft
   idea. The code is original.
2. **The toll is the lunar placeholder's, and only on the way out.** Leaving the home
   world draws **102,400 FU** from the pylon that covers the pad, the capacitor's
   buffer (ADR-0095). Leaving Luna draws nothing, so the flight home does not strand
   a player on a world with no pylon. The return remembers the pad they left. Choosing
   the world they are already in sets them back on that pad. The charge is spent once
   the climb starts.
3. **How it is built.** Assembler, Industrial, 20 s at F1. The launch pad is
   **1 machine casing + 4 steel plates**. The survey rocket is **1 machine casing +
   1 motor + 2 steel plates**. T2+ is manufactured (ADR-0017). No crafting-table
   recipe. The motor is the engine. Helium-3 is not an ingredient.
4. **Interstellar trips are a station.** The player rides a station that has a
   superluminal engine. That station is the next slice and is not started. Arrival at
   the galactic centre is that ride, not a link. The interior stays a finite finale
   and is written when that arrival is the slice (ADR-0088). An empty arrival and an
   endless interior stay rejected. The quest book stays last.
5. **The Lunar Link and the Starward Link stay.** Both remain registered and keep
   their behaviour. They are placeholders, marked in the block comments and on the
   item tooltip, to be replaced by the rocket and the station. This pass does not
   delete them and does not add a third link.
6. **What waits.** The station. Arrival at the black hole, by that station. The
   finite interior of the hole. Kardashev Type I, II and III. Further planets. The
   quest book. Held row AO, including payload mass and the Mass Driver.

**Alternatives rejected.**

- Another link that teleports the player into the black hole.
- Deleting the Lunar Link or the Starward Link in the same change that adds the rocket.
- Putting the Drift, or a black hole, on the landing map.
- An empty "you have reached the ceiling" with no choice of body.
- An endless flight with no ceiling and no landing.
- Charging the Ground Array's buffer for a moon trip. That toll stays on the
  placeholder that leaves the star.
- Charging the return from Luna. A world with no pylon would strand the rider.
- Resuming held row AO, or starting the station, in this change.
- Copying Galacticraft, Advanced Rocketry, or Ad Astra code or assets. Where the
  license would have allowed a copy, this slice still does not take one, because
  those trees do not build here or their assets are reserved. The flight is original.

**Consequences.** A successor who adds a Starward-style link to the black hole is
ignoring this record. A successor who deletes either placeholder link without the
slice that replaces it strands a world that still documents that block. A successor
who puts the Drift on the landing map is flying a rocket across stars. A successor
who starts the station, the hole's interior, the quest book, or the teleportation
orbs here is early. Those orbs are row BK: named, ordered before this flight, and
not started. BM, the station, is next and is not started. The quest book stays last.

---

## ADR-0098 — The supraluminal station is the ride off the star

*2026-10-04 · Superseded in part by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded in part.** The ride from the Drift now arrives at the galactic centre, and a
> station there departs for the berth saved on the way out. The climb, the toll, the
> Starward Link registration are unchanged. See ADR-0099. ADR-0107 withdraws the
> Industrial recipes and places the supraluminal station in T13.

**Context.** Slice BM says the player rides a station to another star, and that the
concrete ride is chosen when the slice starts. ADR-0097 already chose the shape: a
station with a superluminal engine, ridden, and the arrival is that ride rather than
a link. The Starward Link was left in place until this slice replaced it. The owner
kept the black-hole interior, the quest book, the Kardashev scales, further planets,
the teleportation orbs and any third teleport link out of this change.

The ways already named cannot be this one.

- The Drift is the only off-star place that exists (ADR-0096). It is one deck between
  the home star and the galactic centre. A new named star would be a further planet,
  which this row forbids. "Another star" in the row is that hop.
- Arriving at the black hole is the victory (ADR-0088). The interior is a finite
  finale written when that arrival is the slice. Shipping the hole here finishes the
  route before the arrival slice exists. The id `grindless:sagittarius` is already
  the name the rocket's checks use for a hole, and a station must not go there.
- A right-click that still fires the Starward Link is the arrival this record
  refuses. Keeping both would mean the link was not replaced.
- Deleting the link block drops it out of worlds that already placed it, and out of
  the states the server smoke places. The rocket slice refused that deletion.
- The launch pad's recipe is one machine casing and four steel plates. The survey
  rocket's is one casing, one motor and two steel plates. A station recipe with
  either of those input lists is the same craft, and the Assembler cannot tell them
  apart.
- Vacuum damage without an exosuit (held X and Y) makes the deck unusable.
- Copying a warp ship is a license question, not a shortcut. Galacticraft, Advanced
  Rocketry and Ad Astra stay as ADR-0097: not copied. Own work stays MIT (ADR-0089).

**Decision.**

1. **The player rides a station.** `grindless:supraluminal_station` is a seat placed
   on `grindless:station_berth`. It climbs one block a tick until the dimension's
   build ceiling, three blocks of clearance under the exclusive limit, so the rider's
   head stays inside the world. Reaching the ceiling is the arrival. There is no
   menu and no link click. The shape of the climb is the rocket's idea of a visible
   flight. The code is original, and it does not open a landing map.
2. **Where it arrives.** Leaving any dimension other than the Drift arrives on the
   Drift, beside a berth, on the column the return link used
   (`StarwardLinkLogic.ARRIVAL_*` and `STAND_*`). Leaving the Drift arrives on the
   berth they left, or the home world's spawn when that point was never written.
   `grindless:sagittarius` does not depart. The Nether, the End and Luna are not new
   worlds: a station there still goes to the Drift, which is what the link used to
   do. The survey rocket's map does not grow, and a rocket still does not launch
   from the Drift.
3. **The toll is the array's buffer, and only on the way out.** Leaving a world that
   is not the Drift draws **6,553,600 FU** from the pylon that covers the berth, the
   same number the link drew (ADR-0096). Leaving the Drift draws nothing, so the
   ride home does not strand a player on a deck with no pylon. The charge is spent
   once the climb starts. A refused dimension has no toll.
4. **How it is built.** Assembler, Industrial, 20 s at F1. The berth is **1 starward
   link + 4 steel plates**. Consuming the link is how the factory replaces the hop.
   The station is **1 machine casing + 1 motor + 2 array casings**. The motor climbs.
   Two casings are the hull's share of the ring that sized the toll. T2+ is
   manufactured (ADR-0017). No crafting-table recipe. Helium-3 is not an ingredient.
   Neither input list is the launch pad's or the survey rocket's.
5. **The Starward Link stays registered and stops being the trip.** Right-click
   tells the player to ride the station. It does not depart and it does not return.
   A saved charge is dropped, so a link that was mid-draw does not finish the old
   hop. When the station lands, a column that is air or a starward link becomes a
   berth. Any other block is left alone. The Lunar Link is unchanged. This is not a
   third teleport link.
6. **What waits.** Arrival at the galactic centre, by riding this station, is the
   next slice and is not started. Its interior is written then. An empty arrival
   and an endless interior stay rejected (ADR-0088). Kardashev Type I, II and III.
   Further planets. The quest book. The teleportation orbs.

**Alternatives rejected.**

- A third link, or a Starward Link that still teleports beside the station.
- Deleting the Starward Link block.
- A map of stars, a new planet, or the black hole.
- An empty ceiling with nowhere to stand.
- Charging the ride home from the Drift.
- Charging a capacitor for this hop. That toll is the moon.
- Sharing an Assembler input list with the launch pad or the survey rocket.
- Copying Galacticraft, Advanced Rocketry, Ad Astra or Draconic Evolution code or
  assets. Draconic Evolution is All Rights Reserved and its assets are CC BY-NC-SA
  4.0. Ad Astra textures are All Rights Reserved. None of them enter this repository.

**Consequences.** A successor who makes the link move a player again is reopening
the hop this record closed. A successor who deletes the link block strands a world
that still has the id. A successor who sends the station to `grindless:sagittarius`,
or who writes the hole's interior here, is starting the next slice. A successor who
adds a planet to the destination is flying further worlds. BN is next and is not
started. The quest book stays last. BK stays named and not started.

---

## ADR-0099 — The galactic centre is a sealed chamber

*2026-10-04 · Superseded in part by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded in part.** The chamber is unchanged. The sentence that leaves the quest book
> unstarted is withdrawn. The book is in. See ADR-0100. ADR-0107 keeps arrival as victory
> but makes it T15 and moves black-hole exploitation into postgame.

**Context.** Slice BN says the player reaches the black hole by riding the station, and
that the interior is written when the slice starts. ADR-0088 already chose the victory:
arriving is the win. The interior is a finite finale. An empty arrival and an endless
second game are both rejected. The way there is the station, not a link.

ADR-0098 left the station one deck short of that hole. Leaving the Drift rides home, and
`grindless:sagittarius` does not depart. That was so this slice could choose the room.
The owner kept the quest book, the teleportation orbs, the Kardashev scales and any
further planet out of this change.

The ways already named cannot be this one.

- A chat line and no room is the empty arrival ADR-0088 rejects.
- Another flat deck, a vein, or a machine is an endless second game, or a planet.
- A link into the hole is the arrival ADR-0097 and ADR-0098 refuse.
- Sending the home-world departure straight to the hole skips the Drift, which is the
  hop already built.
- Charging the ride off the Drift, or the ride out of the chamber, strands a player
  where no pylon stands. Both of those tolls stay zero.
- A breakable shell, or an item that places the mass, lets the room be mined open or
  carried home. The finale would stop being one room.
- Copying a black-hole dimension from another mod is a license question, not a
  shortcut. No such code or texture enters. Own work stays MIT (ADR-0089).

**Decision.**

1. **The ride from the Drift arrives at the centre.** The dimension is
   `grindless:sagittarius`, the id the rocket's checks already use for a hole. Leaving
   the Drift draws nothing. Leaving any other world still arrives on the Drift and
   still draws 6,553,600 FU. This supersedes the Drift-returns-home half of ADR-0098,
   and the sentence there that says the hole does not depart. The Starward Link stays
   registered and still does not move a player. There is no new link and no new recipe.
2. **The interior is one sealed chamber.** The dimension is a solid mass of
   `grindless:horizon_shell`, sixteen blocks tall (`min_y` 0, `height` 16). No
   features, no lakes, no structures, no spawns, no vein, no skylight, not ultrawarm.
   On arrival the station carves a fixed room and nothing else:
   - air where `x` and `z` are in [-3, 3] and `y` is in [2, 6);
   - a station berth at (0, 2, 0);
   - an arrival mark at (0, 2, -3), light level 15, so the room can be seen;
   - a 3×3 shaft of air above the berth, `x` and `z` in [-1, 1], `y` from 6 through
     15, so the ride out climbs through air.
   The floor under that air stays shell. Every other block stays shell. That is 286
   carved cells inside a mass that does not open onto a walkable world. The shell and
   the mark are unbreakable and have no item. Each arrival applies the carve again, so
   the room cannot be kept as a base. An extractor here finds nothing.
3. **Arriving is the victory.** The landing message says so, and the mark is the
   object in the room. Riding the station out of the chamber returns to the berth
   saved when the station left for the Drift, or the home world's spawn when that
   point was never written. That ride draws nothing. The saved point is kept through
   the Drift and forgotten only on the way out, so the chamber is the turnaround and
   not a loop with the Drift. A rocket still does not launch here, and the landing
   map does not list the hole.
4. **What waits.** The original quest book and the in-game guide, which are the next
   slice and are not started. Teleportation orbs stay named and not started.
   Kardashev Type I, II and III. Further planets.

**Alternatives rejected.**

- An empty "you have arrived" with nowhere to stand.
- An endless deck, a second planet, or a vein inside the hole.
- A link, or a Starward Link that moves a player again.
- Skipping the Drift.
- Charging either free ride.
- A breakable shell, or a block item for the mass or the mark.
- Deleting the return, which would strand the player in the chamber.
- Copying third-party code or assets.

**Consequences.** A successor who sends the Drift ride home again is skipping the
victory this record added. A successor who adds a vein, a recipe, or a machine in the
chamber is starting a second game. A successor who adds a link is reopening the
rejected arrival. A successor who makes the shell breakable, or who gives it an item,
is opening the room into a world. BC is next and is not started. BK stays named and
not started. The quest book stays last.

---

## ADR-0100 — The quest book tracks the route and does not gate it

*2026-10-04 · Superseded in part by [ADR-0107](#adr-0107--sixteen-aligned-tiers-grow-through-control-matrices)*

> **Superseded in part.** The book remains a native, per-player route tracker and never
> gates a machine. Its prototype route to the centre is withdrawn until the T0-T15 campaign
> exists; machine research evidence disappears with the old global unlocks. See ADR-0107.

**Context.** Slice BC is the original quest book and the in-game guide. It is last, after
arrival at the galactic centre. ADR-0088 chose a BetterQuesting-style book — lines, tasks,
dependencies, rewards — and kept that mod's code out, because the GitHub `LICENSE` is MIT
and the CurseForge listing says All Rights Reserved. ADR-0089 does not resolve that
contradiction. ADR-0090 moved the book to the end of the calendar. The route it has to
cover now exists: T0 through the sealed chamber (ADR-0099).

Held row BA also names a guide, beside advancements, a config screen and `es_es`. A
successor who treats BC as BA, who vendors BetterQuesting or Patchouli, or who makes the
book a second research gate, builds the wrong slice. A successor who stops the book at
the contact process ignores the route that was built after it.

**Decision.**

1. **The book is native.** No BetterQuesting classes, schema or assets, and no Patchouli.
   Two handhelds, both ungated crafting-table recipes of paper and sticks, so they exist
   at T0. `grindless:quest_book` is how you follow. `grindless:field_guide` is how you
   read. The guide is pages of this route. It is not advancements, not a config screen
   and not a translation. Those stay in held BA.
2. **The book does not gate the factory.** Research still unlocks blueprints (ADR-0057).
   Fabrication still gates everything above T1. A task watches evidence the game already
   has: the named item is in the inventory, the named blueprint is unlocked on the world,
   or the player is standing in the named dimension. Claiming does not consume that
   evidence. A placed machine counts again when the Multitool has picked it back up.
   Claims are stored per player on the overworld. Research is the factory's; the book is
   the player's. One claim does not finish the book for anyone else.
3. **A claim needs its dependencies and its evidence, once.** Dependencies are other
   tasks. A second claim is refused. The reward is an existing item, at most one stack,
   and it is not the evidence the next task asks for. There is no new material and no
   quest token.
4. **The lines are the route already built**, in that order: bootstrap, Voltaic, the
   contact process, the Arc Furnace metals, the Ground Array, then the voyage through
   Luna, the Drift and the sealed chamber. The guide has a page for each, and the chamber
   page is last. Teleportation orbs, the Kardashev scales, the unbuilt fallback worlds
   and every held row are not tasks. The Lunar Link and the Starward Link are mentioned
   as what they already are. They are not quests.
5. **Nothing follows this slice.** BK stays named and not started. L–BB stay held. This
   record does not add a gameplay slice.

**Alternatives rejected.**

- Vendoring BetterQuesting, or taking a dependency on Patchouli.
- Starting BA: advancements, a config screen, `es_es`.
- A book that stops at the contact process.
- Quest completion as a lock on a machine.
- World-scoped claims, so the first player finishes the book for the server.
- A unique reward item, or a reward that satisfies the next task's evidence.
- A quest for the teleportation orbs, or for a world this pack does not have.
- Inventing a slice after BC.

**Consequences.** A successor who copies BetterQuesting is ignoring ADR-0088 and this
record. A successor who locks the Arc Furnace behind a quest is adding a gate the
research terminal already is. A successor who writes only the contact chapter is
skipping the route. A successor who starts BK, resumes L–BB, or opens a new row because
BC is done is inventing work this calendar does not name. Own work stays MIT (ADR-0089).

---

## ADR-0101 — Replication cost is a graph walk and deconstruction yields one Matter

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice W is the Pattern Scanner and the Deconstructor.
The README prices replication by walking the item's recipe graph (ADR-0010)
and says anything can become Matter. The Replicator block is slice AH. A
scanner that stores nothing, or a deconstructor whose yield is the inverse
of the replication cost, would let a later replicator loop Matter back into
the priced item.

**Decision.**

1. **Replication cost** is a world-free walk of item-id outputs. An item
   with no matching line costs 1. The cost of one output is at least 1 and
   at least the inputs shared across that line's output count, so one
   output never costs less than its inputs. Fluids add `ceil(mB / 1000)`,
   and at least 1 when the line has any fluid. A cycle returns 1 for the
   item already on the stack. Tag outputs are not expanded to member
   items; the Replicator (AH) resolves tags.
2. **The Pattern Scanner** is Assembler-manufactured (casing, two motors,
   four plates, Industrial). Right-click with an item. Matter, a blank
   hand, and anything in the item tag `grindless:replication_blacklist`
   are refused and not consumed. A new pattern spends F1 and one item
   (creative does not spend the item) and stores the item id on the
   world, the same scope as research. A pattern already stored is not
   consumed again. Either way the scanner reports the cost. No power
   means the item stays and the machine reads starved.
3. **The Deconstructor** is the same Assembler craft. Right-click spends
   F1 and one item and gives one Matter item. The blacklist does not
   apply: junk is the sink. Matter smashed again is still one Matter, so
   the yield is flat and not the inverse of the cost. The Replicator
   block is not in this slice.

**Alternatives rejected.** Yielding the replication cost as Matter, which
a replicator could loop; expanding tags in this slice; a per-player
pattern list; a hopper-only deconstructor with no right-click; shipping
the Replicator block beside the cost.

**Consequences.** A successor that makes deconstruction pay the graph cost
reopens the loop this record closed. A successor that replicates items
here is starting slice AH. A successor that treats a tag output as every
member item is doing the Replicator's work early.

---

## ADR-0102 — The Voltaic Harness has one slot and no generator

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice X is the Voltaic Harness. ADR-0067 left the
grid size open and said T1 is protection plus a Flux Cell, with no onboard
generation. The Network Tap, which recharges a suit by walking through
pylons, is slice Y. The drill's fuel is a Drill Cell, not this Flux Cell
(ADR-0084).

**Decision.**

1. **Four pieces**, crafted at the crafting table from iron ingots, with
   iron's protection and durability. Vanilla armour still equips. There
   is no research gate and no Assembler recipe: this is T1 equipment, and
   the Assembler is Industrial.
2. **One module slot** on each piece. The only module is the Flux Cell.
   Using the cell on a harness in the other hand moves the cell into the
   slot. Sneak-using a filled piece gives the cell back. A second cell
   does not fit. Inserting and removing spends nothing.
3. **The Flux Cell** is its own item, `grindless:flux_cell`, crafted from
   copper and iron ingots. It stores 6,400 FU. It does not generate.
   Right-clicking a Capacitor Bank or a Flux Transformer, while that
   block is inside a pylon's coverage, moves FU out of that network into
   the cell. Walking through a pylon does not charge it.
4. Nothing in this slice spends the stored FU. Shields, flight and the
   Network Tap wait for Y. The Arc miniature is not a module.

**Alternatives rejected.** A worn generator; charging by walking through
pylons; naming the drill's fuel Flux Cell; an Assembler recipe that waits
for Industrial; disabling vanilla armour; a grid larger than one slot
before a second module exists.

**Consequences.** A successor that recharges the suit by standing in a
pylon area is starting slice Y. A successor that generates FU while the
suit is worn is starting the Arc miniature. A successor that puts
durability on the drill and calls it this cell is reopening ADR-0084.

---

## ADR-0103 — The exosuit taps a pylon and does not generate

*2026-10-04 · Accepted*

**Context.** BUILD-OUT slice Y is the Flux Exosuit. ADR-0067 says the grid
grows, the Network Tap recharges by walking through pylons, and there is
still no worn reactor. Flight, shields and the other T2 modules are named
in the README; this slice only has to make the tap and mobility real.

**Decision.**

1. **Four pieces**, Assembler-manufactured (casing, two motors, four
   plates, Industrial). Protection matches the harness. Each piece has
   **two** slots. The harness stays at one (ADR-0102).
2. **Network Tap** and **Exoskeleton Legs** are the same Assembler craft.
   They install only into an exosuit. A Flux Cell still installs into
   either chassis. The tap does not fit the harness.
3. **While worn**, a tap pulls up to 32 FU/t from the pylon network
   covering the player into Flux Cells on the exosuit. No coverage, no
   pull. Legs add 0.04 to movement speed and spend 1 FU/t from a cell on
   the suit. No charge, no speed. Nothing generates FU.
4. Thrusters, shields, night vision, the magnet and the miniature Arc
   Reactor are not in this slice.

**Alternatives rejected.** A worn generator; letting the tap fit the
one-slot harness; flight in the same slice as the first mobility module;
charging the suit from a capacitor click (that remains the cell's own
use, ADR-0102).

**Consequences.** A successor that makes the worn suit produce FU is
starting the Arc miniature. A successor that puts a third slot here is
growing the grid again before a third module exists.

---

## ADR-0104 — Parallel stacks reconcile by renumbering the smaller reference set

*2026-10-05 · Accepted*

**Context.** Over one weekend two stacks of pull requests were built from the same tip
of `main` by sessions that could not see each other. One shipped build-out slices L
through Y, the T2 foundation. The other shipped the modpack expansion, BD through BN
and then BC. Each allocated ADR-0088, ADR-0089 and ADR-0090 for different decisions,
and the expansion stack put rows L through BB on hold on the strength of its own
ADR-0088 while the other stack was implementing exactly those rows.

An ADR id is a permanent name. Two records with one id is not a merge conflict that
can be resolved by choosing a side: both decisions are live and both are referenced
from code comments, checks and the handoff documents.

**Decision.**

1. When two ranges collide, the stack with **fewer references** is renumbered, and it
   moves **above** the other range rather than into gaps. Here the foundation stack's
   three records carried about thirty references against the expansion stack's three
   hundred, so they became ADR-0101, ADR-0102 and ADR-0103.
2. Renumbering is done on the files that belong to one stack only. In a merged tree
   `ADR-0090` is ambiguous; in a single stack's tree it is not.
3. `DECISIONS.md` is reordered so its sections run in the same order as its index.
   Sections are separated by `---`, which is what every record before this weekend used.
4. Where the two stacks disagree about **status** rather than content, the code wins.
   Rows L through Y are recorded as shipped because they are shipped; only Z through BB
   stay held under ADR-0088.

**Alternatives rejected.** Renumbering the larger stack, which multiplies the chance of
a missed reference by ten. Letting both ranges keep their ids and disambiguating by
prose, which breaks the index check and every `ADR-00xx` link. Reverting one stack and
replaying it, which discards reviewed, green work. Closing the pull requests and
reimplementing, which is the same thing with extra steps.

**Consequences.** A successor reading a commit from before this merge will find
ADR-0088 to ADR-0090 meaning the foundation stack's decisions; in the tree they mean
the expansion stack's. The changelog entries for those three slices name the new ids.
ADR-0087 was a gap until the route-map pull request filled it, so the index is now
contiguous from 0001 to 0104. A successor that opens parallel stacks again should
reserve an ADR range per stack before starting, which is cheaper than this merge was.

---

## ADR-0105 — Smoke scenarios are a list, sharded across parallel CI jobs

*2026-10-05 · Accepted*

**Context.** Every slice added its own `Boot and exercise ...` step to `ci.yml`, each booting
a full dedicated server. The list reached 39 steps in one sequential job against
`timeout-minutes: 45`.

GitHub reports a timed-out job as **cancelled**, not failed, so this never looked like a
broken build. Every run from `cursor/interstellar-travel-e61d` onward was killed at 45.3
minutes, and six pull requests were merged on evidence that did not exist. Consolidating the
two weekend stacks (ADR-0104) made it strictly worse: the merged branch runs the union of
both stacks' scenarios.

The second-order problem is that `ci.yml` *held* the scenario list. Adding a scenario meant
pasting a nine-line block, so the list could only grow, and nothing tied a scenario to the
files it reads.

**Decision.**

1. `tools/smoke/scenarios.txt` is the single list: one line per scenario, the name and what
   booting it proves. A scenario named `<name>` picks up `<name>.commands`, `<name>.expect`
   and `<name>-pack/` when those exist, and is a plain boot when they do not.
2. `tools/run-smokes.ps1` and `tools/run-smokes.sh` run that list, or `--only` a few, or
   `--shard i --of n`. Sharding is round-robin, not contiguous: the slow scenarios are not
   evenly spread through the list, and striping keeps the shards near the same wall clock.
3. CI is two jobs: `verify` (build and the offline checks) and `smoke`, a six-way matrix.
   Adding a scenario is a line in `scenarios.txt`, never a step in `ci.yml`.

**Alternatives rejected.** Raising `timeout-minutes` (the list keeps growing, and a
seventy-minute CI on every push is its own tax). One job per scenario, 39 of them (queueing
and Gradle setup dominate the boot they wrap). Dropping scenarios to fit (they are the only
evidence a block registers and loads, ADR-0049). Generating the matrix from `scenarios.txt`
with a `setup` job that emits JSON (more machinery than a fixed six, and the shard count
rarely changes).

**Consequences.** A successor that adds a `Boot and exercise ...` step to `ci.yml` is
reopening this record. A successor that adds a scenario file without a line in
`scenarios.txt` has written a scenario nothing runs. The shard count lives in two places,
the matrix and the `--of` beside it, and they must change together. A cancelled CI job is
not a passing one: the conclusion to check on a smoke job is `success`.

---

## ADR-0106 — Code is written remotely; it is validated on the Windows desk

*2026-10-05 · Accepted*

**Context.** Work on Grindless happens in two places. Slices are written away from the desk
in Cursor, as stacked pull requests. The Windows desk is where those branches are built,
smoked, fixed and merged. The two weekend stacks are what that looks like when the second
half never happens: 27 pull requests, a duplicated ADR range, and six of them green only
because CI timed out before it could disagree (ADR-0104, ADR-0105).

That division is worth keeping — writing and validating are different jobs, and separating
them is why the collision was caught at all. It only works if the desk can actually run
everything CI runs. It could not: `tools/smoke-boot.sh` feeds the server console through a
FIFO, and a Cygwin FIFO cannot be read by a native Windows `java.exe`. Under Git Bash the
server boots and then every scenario fails at the handshake, which reads as a mod bug and is
not. `VerifyMaterial` compared a path against a forward-slash prefix, so one check was red on
Windows for reasons that had nothing to do with the code. `generate-assets.ps1` ran `javac`
without `-encoding UTF-8` and wrote mojibake into a tracked file.

**Decision.**

1. Authoring is remote and lands as stacked pull requests. Validation, conflict resolution
   and the merge to `main` happen on the desk.
2. Every tool a validation pass needs has a working Windows entry point:
   `run-checks.ps1`, `check-links.ps1`, `generate-assets.ps1`, `run-smokes.ps1` and
   `smoke-boot.ps1`. The `.ps1` is not a courtesy wrapper; where the platforms genuinely
   differ it is a separate implementation, as the Win32 pipe is.
3. A Windows-only failure in a tool is a **bug in the tool** and is fixed there, not worked
   around in the session that trips over it. Three such bugs had been sitting in the
   repository long enough to make the documented pre-commit sequence unusable.
4. CI stays the authority for merge. The desk is the faster loop, not a replacement: it
   catches the failure in minutes instead of waiting forty-five for a job to be cancelled.

**Alternatives rejected.** Running the shell scripts under WSL (it boots a Linux Java, so it
stops being evidence about the machine the developer builds on, and it hides exactly the
class of bug listed above). Making the desk authoritative and dropping CI (one machine, one
operating system, no record). Writing code on the desk too (the split is what made the
duplicate ADR range visible before it reached `main`).

**Consequences.** A successor on Windows that cannot run a check should expect the tool to be
at fault and fix it. A successor that adds a `tools/*.sh` without its `.ps1` twin has made
the desk unable to validate that step, which is the thing this record exists to prevent. A
pull request authored remotely is not finished when CI is green; it is finished when it has
been through the desk.

---

## ADR-0107 — Sixteen aligned tiers grow through Control Matrices

*2026-10-06 · Accepted*

**Context.** The consolidated branch has a complete, locally validated prototype path from
the hand crank to the galactic centre. That path is not a viable campaign: the Ground Array,
rocket and supraluminal station are all small Industrial Assembler recipes, so the nominal
victory bypasses the retained T3-T6 factory and every Kardashev milestone.

The old progression also has two gates for the same fact. The Research Terminal stores a
world-scoped permission saying a blueprint may run, while the Assembler recipe is supposed
to prove that the factory can physically manufacture the machine. A permission is invisible
in the production graph, and extending it through a long campaign would create a second tree
that can drift away from the real material requirements.

The current T3 and T4 descriptions compress pressure chemistry, lithography, fission,
cryogenics, fusion, particle acceleration, quantum fabrication and singularity power into
two tiers. Later tiers need more content, more interacting routes and a larger spatial scale
than earlier ones, not a faster repetition of the same recipes. The player also wants many
recognisable tiers, but F30 is too many labels merely to preserve the existing fourfold power
step up to the numerical ceiling.

**Decision.**

1. **Technology, controller and power use one aligned T0/F0-T15/F15 ladder.** Tn names
   what can be built, Fn is its nominal Flux rate, and a Control Matrix rated Tn is the
   physical controller in every Tn machine. A higher-rated matrix may substitute, but doing
   so wastes a more valuable component. Chassis marks remain a separate physical-envelope
   axis: a controller cannot make a chamber hold a temperature, pressure or field that its
   chassis cannot survive.

   | Tier | Name | Nominal FU/t | Main transition |
   | --- | --- | ---: | --- |
   | T0 / F0 | Bootstrap | 8 | hand work to assisted extraction |
   | T1 / F1 | Voltaic | 32 | first powered factory |
   | T2 / F2 | Industrial | 128 | fluids, steel and closed chemical loops |
   | T3 / F3 | Arc | 512 | fed direct power and construction drones |
   | T4 / F4 | Precision | 2,048 | high-pressure chemistry, purity and lithography |
   | T5 / F5 | Nuclear | 8,192 | isotope cascades and fission |
   | T6 / F6 | Cryogenic | 32,768 | deep cooling and superconducting infrastructure |
   | T7 / F7 | Fusion | 131,072 | breeding blankets and sustained plasma |
   | T8 / F8 | Particle | 524,288 | accelerator isotopes and exotic precursors |
   | T9 / F9 | Quantum | 2,097,152 | quantum control and programmable matter |
   | T10 / F10 | Orbital | 8,388,608 | a factory split between ground and orbit |
   | T11 / F11 | Planetary | 33,554,432 | colonies, telepresence and Type I scale |
   | T12 / F12 | Stellar | 134,217,728 | stellar processing and Type II scale |
   | T13 / F13 | Interstellar | 536,870,912 | multiple star systems and the Drift |
   | T14 / F14 | Galactic | 2,147,483,648 | abstract expansion and Type III scale |
   | T15 / F15 | Event Horizon | 8,589,934,592 | the route to the galactic centre |

   The exact rule is `F(n) = 8 * 4^n FU/t`. It preserves every existing F0-F9 value.
   F14 is the first nominal rate above the Forge Energy `int` boundary, so external transfer
   saturates there while internal Flux remains wider. F15 is the last named tier, not the
   numerical ceiling. After arrival, T15 black-hole infrastructure scales from its nominal
   rate to `Long.MAX_VALUE` FU/t as postgame progression without another fifteen matrix names.
   The representation must grow before a network sums or buffers those rates.

2. **Control Matrices replace global machine unlocks.** Recipes are visible from the start;
   the Atlas reports the physical inputs and missing infrastructure. The first T1 matrix is
   a deliberately small hand-crafted batch so even the first factory has a controller.
   Every later matrix is manufactured. The first route to rating Tn depends on a Tn-1 matrix,
   so a tier cannot be skipped, but this is not one rigid recursive recipe forever.

   Matrices have both a **rating** and an **architecture**:

   | Architecture | Frontier ratings | Character |
   | --- | --- | --- |
   | **Relay** | T1-T3 | wire, coils, ceramic insulation and discrete logic |
   | **Integrated** | T4-T6 | silicon, etchant, polymer and fine interconnects |
   | **Superconducting** | T7-T9 | cryogenic films, coherent control and exotic dopants |
   | **Photonic** | T10-T12 | zero-g crystals, lasers and optical routing |
   | **Causal** | T13-T15 | exotic matter, distributed clocks and relativistic control |

   A new architecture opens cheaper, faster or larger-batch routes to older ratings. The
   original route remains the low-infrastructure option. This keeps old matrix factories
   useful, makes later factories better at producing their own foundations, and provides
   route choice instead of fifteen ceremonial reskins.

3. **The Research Terminal no longer grants ordinary machine permissions.** It is redesigned
   at T4 Precision as the **Research Station**. It analyses samples and Data Cores to produce
   reusable physical patterns for genuinely advanced components and process modes. Patterns
   are catalysts, not world-scoped booleans and not a second tier ladder. Normal machines,
   matrices and visible recipes never require a hidden research flag. Existing Data Cores
   become pattern media rather than tier currency.

4. **Every tier is a braided factory, not four independent checklists.** Its four persistent
   domains are materials, chemistry, computation/control and energy. They exchange reagents,
   catalysts, byproducts, condition hardware and intermediate components throughout the tier,
   then converge again in the frontier Control Matrix. From T2 onward, no domain's headline
   output may be completed without at least one other domain.

   Each later tier is approximately longer and richer than the one before it: more process
   families, deeper routes, more cross-domain loops, a larger logistics problem and a larger
   signature project. Length comes from new decisions, not inflated stack counts or timers.
   Every increase in scale ships with proportional leverage: Assembler automation, reusable
   Process Cards, construction drones, the Assembly Field, abstract colonies and eventually
   self-replicating infrastructure. Solving a larger problem is progression; repeating a
   solved action is grind.

5. **The late campaign is decompressed.** Arc, Precision, Nuclear, Cryogenic, Fusion,
   Particle and Quantum each get their own tier. Rockets and orbital production start at
   T10. Planetary industry and Horizon Gates belong to T11. Stellar industry and Type II
   belong to T12. The supraluminal station and the Drift belong to T13. Galactic expansion
   and Type III belong to T14. The final route to Sagittarius belongs to T15.

   Arrival at the galactic centre remains the victory. Exploiting the supermassive black hole
   is postgame T15: accretion resources, rotational-energy extraction and the climb toward the
   absolute Flux rate. The existing chamber is therefore a prototype destination, not the
   final amount of gameplay at the centre.

6. **The validated space implementation is retained as test infrastructure but withdrawn
   from survival progression now.** The Ground Array, Lunar and Starward Links, launch pad,
   survey rocket, station berth, supraluminal station, Luna, Drift and Sagittarius remain
   registered so their code, worlds and smoke scenarios are not discarded. Their Industrial
   recipes and the quest path that advertises the shortcut are removed in the next slice.
   They return to survival only when their assigned tier has its real routes and matrix.
   No black-hole recipe is authored before T15.

**Alternatives rejected.**

- Keeping a Research Terminal permission in addition to physical fabrication. Two gates for
  one capability drift and make the recipe graph lie.
- One recursive matrix item with no alternative architectures. It keeps every old line busy
  but never rewards later technology with better manufacturing.
- Independent technology and Flux ladders. That distinction served a six-tier outline; the
  matrix now gives each named technology a physical power rating without making undervolting
  a hard wall.
- F0-F30 solely to reach `Long.MAX_VALUE` at a fourfold step, or changing the fourfold step
  so F15 lands there. Thirty named tiers dilute their identity; a variable ratio makes the
  ladder harder to predict and invalidates the proven early balance.
- Making later tiers longer through larger ingredient counts, longer timers or manual
  multiblock placement. That is repetition, not content.
- Leaving the Industrial recipes as a convenient vertical slice. A survival victory at T2
  makes the actual campaign optional.
- Making black-hole extraction the victory condition. Arrival remains the clear campaign
  finish; exploitation is an open-ended postgame with practical rewards.

**Consequences.** `FluxTier`, machine recipes, chassis documentation, the quest catalogue and
the current research save data still implement the old model until their named slices change
them. Migration must preserve placed blocks and old worlds; unused research flags may be read
and ignored rather than deleting saves. The next slice is the narrow survival-boundary change:
remove prototype endgame recipes and quests while keeping registrations and smokes. The slice
after that introduces matrix rating/architecture data and the recoverable T1 Relay Matrix route;
the following slice moves T2 fabrication off the Voltaic/Industrial permission checks. Later
tiers are specified in route-sized increments before their code is released; this record does
not pretend that one table is fifteen finished recipe sets.

---

## ADR-0108 — Finish one mechanical tier before opening the next

*2026-10-07 · Accepted*

**Context.** The repository contains a wide validated foundation and several later prototypes,
but "implemented" has sometimes meant that a block registers, a pure policy check passes or an
isolated smoke can place it. That is necessary evidence, not proof that a player can enter a tier,
build every required line, recover from normal failure states and reach the next frontier without
commands or creative-only gaps.

Continuing to add slices across many tiers would make those gaps compound. Conversely, requiring
finished hero art, bespoke animation, final sound and every locale before mechanics settle would
front-load work that is likely to be redone. Running all historical smoke scenarios after every
recipe or guide change has the same problem: it spends many server boots proving untouched
registrations while the actual tier route is still incomplete.

**Decision.**

1. **Only one tier is mechanically active.** Work may be split into small reviewable slices, but
   the next tier stays locked until the active tier passes the completion gate in
   `BUILD-OUT.md`. Existing later-tier code may remain registered for compatibility and tests; it
   is not evidence that its tier is accepted and must not provide a survival shortcut.
2. **A tier is accepted by an end-to-end survival route.** From a documented entry state, a player
   can obtain its resources, power every required machine, run all mandatory material, chemical,
   control and energy lines, automate their transport, handle every byproduct, recover from blocked
   or starved states and produce the frontier Control Matrix or terminal objective. No command,
   hidden world permission, creative item or unexplained external-mod assumption may bridge a gap.
3. **Mechanical usability is part of correctness.** Required recipes are discoverable, machine
   states name actionable faults, inventories and capabilities automate, state survives reload,
   and the quest/guide route matches what survival can actually do. A system that only works by
   reading source code is not complete.
4. **Final art is not a tier gate.** Hero textures and models, animation, polished sound, visual
   effects and complete localization are deferred to the final art/polish pass. Functional
   placeholder assets, readable names, tooltips, menus and state feedback remain mandatory because
   invisible or uninterpretable mechanics are not playable.
5. **Validation follows risk.** Pure arithmetic and policy stay in fast behaviour checks. Every
   tier closes with compile, all behaviour checks and a full build. A targeted boot or interaction
   smoke is added or run when the tier changes registration, loader wiring, menus, persistence or
   another Minecraft-only seam. The full historical smoke matrix is an integration/merge check,
   not the inner loop for data-only changes. This complements ADR-0049; it does not permit a tier
   release that has never booted.
6. **Acceptance is recorded.** `BUILD-OUT.md` owns the gate and tier status, the README records the
   playable frontier, and the changelog records each closed tier. The next tier becomes active only
   in the commit that accepts its predecessor.

**Alternatives rejected.** Continuing feature-by-feature across the whole ladder (maximises breadth
while leaving uncertain player routes behind); one enormous pull request per tier (too much
uncommitted and unreviewable work, contrary to the repository's checkpoint rule); final art as part
of every tier (slows mechanical iteration and invites rework); no in-world acceptance, only unit
checks (cannot prove reachability or capability wiring); all 39+ historical smokes after every
change (slow evidence about mostly untouched systems rather than the active tier).

**Consequences.** T0 Bootstrap is the active audit. Its accepted endpoint is a recoverable first
T1 Relay Matrix batch, so BP is T0 completion work rather than permission to move on. T1 remains
locked until the T0 route is mechanically complete. The same gate then repeats through T15. Art
debt stays explicit and deferred, but placeholder assets and mechanical feedback remain maintained.

---

## ADR-0109 — Upstream material may be reused under its own licence

*2026-10-07 · Accepted · Supersedes the external-asset restriction in ADR-0048*

**Context.** Grindless is intentionally designed to coexist with and learn from established
technology mods. The owner explicitly permits copying or adapting their code, textures, sounds
and other material where the upstream licence allows the intended use. The prior asset provenance
rule only allowed CC0-equivalent third-party assets, which unnecessarily ruled out legitimate
reuse and incorrectly made originality an absolute project claim.

**Decision.** Grindless may reuse or adapt upstream material only after a per-component licence
review. `THIRD_PARTY_NOTICES.md` records the upstream project and URL, exact release or commit,
licence, imported scope, required attribution and the distribution consequence. Required notices
and licence text travel with the distribution. The repository's MIT licence continues to apply to
Grindless-authored material only; a compatible licence change is made before distribution if an
import requires one. Inspiration that copies no protected expression needs no entry, but any
copied or adapted source or asset does.

**Alternatives rejected.** An all-original-only rule (needlessly discards usable compatible work);
an unqualified permission to copy from any mod (a repository licence cannot grant rights held by
an upstream author); deciding a new project-wide licence now (there is no imported component whose
terms require it yet).

**Consequences.** Future sessions may use compliant upstream work, including code and visual
assets, but must document provenance before committing it. An asset is no longer required to be
CC0 solely because it is external. No third-party component is introduced by this decision.

---

## ADR-0110 — B2 pairs materials exactly without stack NBT

*2026-10-07 · Accepted · Supersedes ADR-0076's secondary-output contract*

**Context.** The original B2 batch used a crushed secondary selected from the washer's eligible
cycle. That made the documented 0.25 u trace depend on the primary material's processing route,
and item stacks could not identify their source vein without adding NBT provenance. The precise
contract needs a deterministic, pack-derived output with an ordinary item stack.

**Decision.** The Washer scales B2 by four: `8 crushed + 2 B water → 8 washed crushed + 1
secondary ingot` in 20 seconds at F1, stirred. Eligible secondaries have a mineable feed and an
ingot; their stable name order forms a wrapping ring. A one-material pool has no B2 recipe.
Washed crushed uses `grindless:washed_crushed_materials/<material>`. The secondary ingot enters
that material's existing wire and forming routes, so it has a named sink without new NBT, random
output or fractional stacks.

**Alternatives rejected.** Random output (breaks exact short-run behaviour); nugget rounding
(does not represent 0.25 u); storing a secondary on item stacks (splits stacks and complicates
logistics); keeping the previous crushed output (couples the trace to an unrelated ore route).

**Consequences.** The B2 graph is deterministic and visible in the Atlas. Generated assets use
the renamed washed-crushed form, and the stale former `*_washed` assets are removed.

---

## ADR-0111 — Shared-world smokes are the fast local loop

*2026-10-07 · Accepted*

**Context.** A Forge startup takes a material fraction of every smoke scenario. The isolated
39-scenario matrix is the correct integration evidence, but it turns small local changes into a
long wait even when most scenarios have not changed.

**Decision.** `tools/run-smokes-batch.ps1` boots one temporary Forge world and runs the ordinary
scenario command files consecutively, checking each scenario's existing expected markers. It is
the fast local feedback loop, not a replacement for isolated validation. `foreign-providers` and
`states` stay out of a batch because they require their own datapack at world creation; they run
through `tools/run-smokes.ps1`, as does the full historical matrix before integration.

**Alternatives rejected.** Rebooting the server for every local edit (sound isolation but poor
iteration); loading all datapacks into one world (changes the provider and state guarantees being
tested); calling the shared-world batch merge evidence (state leaked by an earlier scenario could
mask a regression).

**Consequences.** Developers can exercise broad Minecraft seams quickly while retaining a clear,
stronger isolated gate for branch integration. A batch failure is actionable; a batch pass only
authorizes faster iteration, not merge acceptance.

---

## ADR-0112 — The first Relay Matrix is a four-unit Voltaic hand batch

*2026-10-07 · Accepted*

**Context.** The physical T1 Relay Matrix existed as an item, but a fresh player had no survival
recipe for the first matrices. Making it require a T1 machine would be circular; making it free or
available before the terminal would bypass the material, control and manual-power bootstrap.

**Decision.** After Voltaic research, a crafting-table recipe turns two glass, two tagged copper
ingots, two redstone and one Data Core into four T1 Relay Matrices. The recipe is Voltaic-gated,
but needs no T1 machine and consumes none of the two iron ingots that seed the dynamo and crude
extractor. Its small batch supplies the first factory and preserves a local recovery path.

**Alternatives rejected.** A single matrix per craft (turns initial expansion into repetition);
a free ungated matrix (does not prove the bootstrap route); a Press or Assembler recipe (requires
the factory the matrix is supposed to start); consuming the original two bootstrap iron (can
softlock recovery when those blocks are lost).

**Consequences.** T0 now has a concrete frontier item after the Research Terminal's first cycle.
The subsequent BP work can migrate legacy blueprint state and make the Atlas, guide and quest route
describe this batch before T0 acceptance is considered.

---

## ADR-0113 — GregTech is the primary design reference

*2026-10-07 · Accepted*

**Context.** Grindless aims for a coherent industrial progression rather than an unconnected set
of machines. Several automation games and technology mods offer useful lessons, but without a
reference hierarchy their sometimes-conflicting conventions would dilute the project.

**Decision.** GregTech is the primary reference for industrial coherence: processing chains,
material forms, machine roles, gating and progression scale. Factorio and Satisfactory are
secondary references for readable factory flow, throughput feedback, automation ergonomics and
spatial scale. This is design inspiration, not an import of protected expression. Any copied or
adapted code, texture, sound or other asset still requires the per-component licence review and
provenance record mandated by ADR-0109.

**Alternatives rejected.** Treating all references as equivalent (weakens a clear industrial
identity); treating inspiration as permission to copy (does not grant upstream rights); refusing
all external influence (discards established design knowledge without improving originality).

**Consequences.** New mechanics should first be evaluated against the GregTech-style industrial
chain and then use the secondary references to make that chain legible and pleasant to automate.
`THIRD_PARTY_NOTICES.md` remains empty until protected upstream material is actually imported.

---

## ADR-0114 — Physical components replace global research permissions

*2026-10-07 · Accepted · Supersedes the gated-recipe portion of ADR-0057, ADR-0073 and ADR-0112; batch count revised by ADR-0122*

**Context.** The original Research Terminal stored Voltaic and Industrial unlocks in world-scoped
saved data. Both crafting-table and process recipes consulted that invisible state. This made a
recipe unavailable even when a player held every visible ingredient, and it obscured what the
factory had actually achieved.

**Decision.** Remove the global blueprint state, client synchronisation, recipe serializer and
all runtime checks of it. Every ordinary recipe matches from its visible ingredients alone. The
T0 Research Terminal remains a physical F0 machine: it consumes a Data Core over thirty seconds
at 8 FU/t and leaves a Calibrated Data Core in its output slot. The first five Relay Matrices
consume that output, alongside glass, copper and redstone. The terminal output can be recovered
by hand or hopper. Existing `grindless_research` saved data is deliberately left unread: it is
inert legacy data, rather than a migration source for a permission that no longer exists.

**Alternatives rejected.** Retaining the old unlock purely as a convenience flag (still hides
the requirement); automatically granting the result to worlds with saved unlocks (preserves an
invisible entitlement); removing the terminal entirely (would bypass the first powered machine
and make the matrix route less tangible).

**Consequences.** Quests observe physical cores without gating anything. Existing terminals keep
their saved stack so a player can retrieve it; an older Advanced Data Core is not destroyed.
Process graph milestone labels remain design metadata only until a future physical matrix route
replaces them. No gameplay path reads a world- or player-scoped research permission.

---

## ADR-0115 — The Atlas shows physical Bootstrap routes

*2026-10-08 · Accepted · Supersedes part of ADR-0066*

**Context.** The Process Atlas originally listed only generated machine-process rows. The first
Relay Matrix route is instead a physical terminal calibration followed by an ordinary crafting
table recipe. Leaving those two transformations outside the Atlas would make the Bootstrap
frontier less discoverable precisely when the player needs to find it, despite both routes being
live game data.

**Decision.** The Atlas includes two kinds of supplemental live route: the Research Terminal's
Data Core calibration and every authored Grindless shaped crafting recipe. They use explicit
`RESEARCH_TERMINAL` and `CRAFTING_TABLE` stations alongside the existing machine family names.
The route view derives its ingredient multiplicities from the same data that writes and verifies
the recipe JSON. It remains a sorted lookup: it does not gain reachability checks, target-rate
solving, vanilla-recipe enumeration, or recipe-viewer integration.

**Alternatives rejected.** A guide-only explanation (does not let the player inspect the actual
recipe); showing only the Relay Matrix (leaves the other real hand crafts inconsistent); treating
the calibration as an invisible exception (reintroduces an unexplained gate); scanning every
vanilla recipe (is outside the mod's route graph and makes the handheld unusable).

**Consequences.** The fresh T0 player can see the complete physical core-to-matrix route in the
native handheld. The Atlas remains bounded to Grindless-owned data and stays testable without a
Minecraft runtime. The later solver and third-party viewer integrations remain deferred.

---

## ADR-0116 — Bootstrap has an explicit survival entry and exit

*2026-10-08 · Accepted*

**Context.** The previous “two iron” wording correctly described the mod-specific metal spend,
but it was too easy to misread as the whole inventory for the first Matrix. The actual physical
route also visibly needs a normal Overworld source of stone, wood, sand or another glass source,
redstone and copper. The tier gate requires an explicit entry, exit, scale and target playtime;
without those, a later audit can claim success from isolated recipes rather than a route a player
can follow.

**Decision.** T0 starts in ordinary Overworld survival once the player has a crafting table and
can gather wood, stone, sand or another glass source, redstone, copper and exactly two tagged iron
ingots for Grindless machinery. The two iron pay only for the Hand Crank Dynamo and Crude
Extractor; the Multitool, Terminal and Data Core do not spend them. The player powers both F0
machines, uses the extractor at least once, calibrates a Data Core, and crafts five T1 Relay
Matrices from the calibrated core, two glass, two copper and two redstone. That is the T0 exit.
The target is roughly twenty minutes from this vanilla-material entry, with no repeated hand craft
after the recoverable five-matrix batch.

**Alternatives rejected.** Calling two iron the entire inventory (hides real visible inputs);
granting copper, redstone or glass through a hidden starter state (makes the route less legible);
requiring a particular chunk material from the extractor (turns seed variation into a softlock);
accepting a collection of unit tests without one route contract (does not meet the mechanical
tier gate).

**Consequences.** `VerifyBootstrap` must pin both the two-iron machine budget and the physical
Matrix inputs/output. The field guide and Atlas name the same route. A local smoke proves the
machine seam, while an end-to-end survival playthrough remains the final acceptance evidence
before T1 opens.

---

## ADR-0117 — Recurrent base materials gain industrial renewal routes

*2026-10-08 · Accepted*

**Context.** The intended experience is GregTech-style production-chain complexity without the
corresponding requirement to repeat manual gathering for ingredients such as glass or redstone.
T0 correctly begins from visible Overworld resources, but a resource that stays finite and manual
after it becomes a continuous machine input turns scale into grind. A universal free-source block
would solve that problem by deleting the industrial decisions the mod is meant to create.

**Decision.** Every recurring base material has two explicit stages: a natural or pack-provided
seed route for the opening, then an indefinitely renewable industrial route before sustained
consumption makes manual supply a chore. The second route must name its feed, machine, energy or
logistics cost, and any byproduct; it is never an invisible permission, creative supply or one
universal generator.

The first implementation is glass. A vanilla cobblestone generator feeds one cobblestone to the
Pulverizer for one sand in six seconds at F1; the Arc Furnace vitrifies one sand into one glass in
ten seconds at F1 and 1500 °C. Sand remains valid `#grindless:silica`, so this single line also
makes the existing metallurgical-silicon feed renewable. T0 remains the short physical bootstrap:
the line is a Voltaic factory capability and does not itself accept T1 or replace its completion
gate.

**Alternatives rejected.** Keeping glass as a permanent furnace-and-shovel chore (contradicts
the no-grind goal); making glass directly from power (removes the material/logistics puzzle);
adding every future renewable material in one broad slice (creates untested empty routes and
obscures their proper tier); moving industrial self-supply into T0 (blurs the intentional
transition from assisted extraction to a factory).

**Consequences.** The Process Atlas exposes `renew/sand` and `renew/glass`, and the Field Guide
names their place immediately after the first Matrix batch. The design records the remaining
contract openly: renewable metal recovery, redstone recovery/synthesis and ceramic feed must each
gain a named industrial route before their demand becomes a sustained bottleneck. The Bootstrap
audit remains active until its survival playthrough is complete.

---

## ADR-0118 — Redstone is synthesised from renewable silicon and water

*2026-10-08 · Accepted*

**Context.** Redstone is a recurring control ingredient from the first Matrix onward. Mining it is
an appropriate Bootstrap seed, but it becomes grind if every later relay, pylon and controller
requires more cave exploration. The newly renewable sand line already provides silica, while a
tree/charcoal loop supplies the carbon that makes metallurgical silicon. The Hand Pump supplies
renewable water before the faster Fluid Well exists, and the Thermal Generator can burn charcoal
to power the line.

**Decision.** The Industrial Chemical Reactor synthesises four redstone dust from one
metallurgical silicon and 500 mB water in twenty seconds at F1, 450 °C, oxidising atmosphere and
stirred agitation. The complete renewable source stack is therefore: a cobblestone generator to
the Pulverizer for sand, renewable wood to charcoal/carbon, the Arc Furnace for metallurgical
silicon, a Hand Pump for water, and the Chemical Reactor for redstone. A Hand Crank can commission
the starter machines; a charcoal-fed Thermal Generator is the first unattended F1 source.

The tree source is deliberately an open vanilla/pack integration: any automatically replanted
tree farm supplies logs, and an automatically fed vanilla furnace supplies charcoal. Grindless
does not add a duplicate early planter before the factory needs a distinct agricultural process.
The source contract records the later dedicated biomass expansion instead.

**Alternatives rejected.** A free redstone generator (removes the control-resource problem);
turning cobblestone directly into redstone (hides both carbon and water infrastructure); requiring
nether-only ingredients (would make an early control material dimension-dependent); adding a
Grindless tree-farm block that only duplicates a simple vanilla automation line.

**Consequences.** Redstone has a fully renewable automated route before it becomes a sustained
factory bottleneck. The source chain deliberately spans material, chemistry, control and energy:
it gives the first small factory a reason to connect its coal/charcoal, water and silica lines.
Copper/iron recovery and mineral ceramics remain separate future routes; they must not be silently
substituted by this control-material loop.

---

## ADR-0119 — Relay Matrices are consumed machine components

*2026-10-08 · Accepted*

**Context.** A T1 Relay Matrix already had a physical item, calibration route and manual batch,
but the T1 machine recipes did not consume it. That made the Matrix a one-off milestone rather
than the primary machine-control component intended by the progression lattice, and it left the
first Assembler unable to reproduce its own controller.

**Decision.** The seed Matrix batch is allocated to the first factory's active machines. Its final
count is five — Thermal Generator, Pulverizer, Arc Furnace, Press and Assembler — under
ADR-0122. The Assembler repeats the exact physical batch — two glass, two copper ingots, two
redstone and one Calibrated Data Core — into five Relay Matrices in twenty seconds at F1. Press,
Kiln and Terrestrial Extractor each consume one Matrix when crafted.
Passive grids, storage, transport and hand tools do not consume a Matrix because they do not host
an autonomous process controller. The Assembler gains a fourth input slot so this route neither
omits an ingredient nor invents an intermediate solely to fit a UI limit.

Existing Assemblers save their old three input slots and output at slots 0–3. On loading legacy
data, the former output at slot 3 moves to the new output slot 4 before the inventory is read.

**Alternatives rejected.** Leaving matrices out of machine recipes (turns the frontier into a
token); adding a separate, artificial contact item just to preserve a three-slot Assembler
(creates a manual or circular extra step); dropping glass, redstone or the calibrated core from
automated production (makes the factory route materially different from the physical Bootstrap
route); charging passive belts, pylons or tanks (adds a controller tax where no controller exists).

**Consequences.** The Matrix behaves like a GregTech-style circuit: a consumed, scalable
component that carries progression through recipe inputs rather than a permission check. The
first batch remains recoverable, but continued T1 expansion is automated once its five seed
machines exist. Future T2+ machine recipes must consume their matching physical Matrix as part of
their own frontier slice; they are not silently converted by this T1 change.

---

## ADR-0120 — The first client pack is T0-scoped and JEI reads the live graph

*2026-10-08 · Accepted*

**Context.** Grindless is intended to be played as a modpack, but an early pack needs quality of
life and recipe discoverability without silently adding an alternate technology tree. The project
has generated machine processes and a physical Research Terminal calibration that neither vanilla
JSON recipes nor a static exported JEI list can describe. Applied Energistics 2 is desired for the
campaign, but its unmodified ore, device and autocrafting route would be available at Bootstrap if
it were simply installed now.

**Decision.** Ship a reproducible local CurseForge-format **client-pack candidate** for Forge
1.20.1 with JEI, Jade, Sophisticated Backpacks, FTB Essentials, FTB Chunks/Teams/Library and the
ModernFix/FerriteCore/Embeddium performance baseline. The tracked manifest pins each launcher
file; the packaging scripts build the local Grindless jar and add it as an override instead of
committing a binary.

The Forge-only optional JEI bridge reads `ProcessLookup` at registration time, one category per
machine family, and registers the physical calibration page independently. Ordinary data recipes
remain JEI's vanilla responsibility. Until Grindless state fluids are registered Forge fluids, JEI
shows their names and millibucket amounts as explicit process-condition text rather than
misrepresenting them as fillable fluid ingredients.

Reserve AE2 for the T4 Precision frontier. That slice must establish recipe replacement, material
bridges, Flux/AE energy behaviour, a storage/pattern/autocrafting route, and a survival proof in
one release. It is not enough to hide an AE2 recipe or name a tier in a document.

**Alternatives rejected.** Including unmodified AE2 in T0 (bypasses the intended early
frontier); producing a hand-maintained JEI recipe copy (it drifts from generated processes);
waiting for a perfect server distribution before players can exercise the local pack (delays the
useful client proof); treating ordinary crafting JSON as Grindless custom JEI pages (duplicates
vanilla's viewer behaviour).

**Consequences.** A local archive can be created consistently and all currently authored
Grindless recipes/processes are visible in JEI. This improves testability and discoverability but
does **not** close the T0 mechanical gate: an end-to-end normal-survival playthrough is still the
acceptance evidence. Server-pack assembly and AE2 integration are future scoped work, not hidden
claims of this first client candidate.

---

## ADR-0121 — The Factory World is clean building space, not a resource shortcut

*2026-10-09 · Accepted*

**Context.** A large automated base is more legible and enjoyable on a controlled plane than in a
terrain-cleared overworld. Players should be able to make that choice early enough to build their
first serious factory there. However, a visually empty dimension that silently supplies infinite
materials would contradict the no-grind promise: resource acquisition must stay an automation
problem with finite fields and relocation decisions.

**Decision.** A Factory Portal, crafted after the first Relay Matrix, sends a player to
`grindless:factory` and returns that player to the particular gate from which they left. The
dimension has one bedrock layer, three dirt layers and a grass surface; no lakes, structures,
terrain features, rain or hostile spawns; a normal sky fixed at Minecraft midday. The factory
dimension retains the installed material pool through the existing `ChunkVein` model. Its patches
are surveyed, extracted and depleted like overworld patches, but no ore terrain interrupts the
level building surface.

**Alternatives rejected.** Requiring the player to clear an overworld biome (reintroduces a
terrain chore); free resource generators in an otherwise empty building world (trivialises the
material route); a separate visible-ore generator (duplicates the mod's established surveyed-vein
contract); tying the gate to orbital travel or a campaign tier (makes practical factory layout wait
for an unrelated milestone).

**Consequences.** Solar arrays receive uninterrupted daylight there once they are legitimately
available, while all ordinary resource/energy/logistics decisions remain. The portal is a compact
two-way access block rather than a replacement for Luna, orbital or interstellar campaign travel.
`VerifyFactory` pins the physical craft, fixed-noon world data and finite-field policy.

---

## ADR-0122 — The seed Matrix batch funds the forming Press

*2026-10-09 · Accepted*

**Context.** The first Relay Matrix batch contained four controllers for the Thermal Generator,
Pulverizer, Arc Furnace and Assembler. That allocation was not mechanically reachable: the
Assembler needs a casing, gears and a copper coil, and those parts require the Matrix-consuming
Press. Asking the player to calibrate another core and hand-craft a second batch before the
Assembler exists would make the advertised automation transition depend on a repeated hidden
bootstrap.

**Decision.** The manual and automated recipe both produce **five** Relay Matrices from the same
two glass, two copper, two redstone and one Calibrated Data Core. The initial allocation is one
each to Thermal Generator, Pulverizer, Arc Furnace, Press and Assembler. The native Quest Book and
Field Guide follow this exact route through Voltaic power, surveyed extraction, renewable glass,
basic logistics and the optional Factory Portal. They remain non-gating evidence: holding an item
unlocks only a claim and a small reward, never a machine.

**Alternatives rejected.** A second manual seed batch before the Assembler (repetition at the
moment automation should begin); removing the Matrix from the Press (makes the primary machine
component inconsistent); free pre-pressed parts (hides the forming route); keeping a four-Matrix
book that claims an unreachable factory (misleads the player).

**Consequences.** The T0 exit becomes a recoverable five-Matrix batch while retaining its
two-iron machinery budget and single calibration ingredient list. T1 can now begin as a coherent
factory route, but it is not accepted until its physical T2 Matrix frontier and end-to-end
survival playthrough are complete. `VerifyBootstrap`, `VerifyRecipes` and `VerifyQuest` pin the
allocation and the expanded in-game route.

---

## ADR-0123 — The Voltaic exit is a rated physical F2 Relay Matrix

*2026-10-09 · Accepted*

**Context.** T1 can reproduce its F1 Relay Matrix batch, but that alone cannot prove that the
factory has reached Industrial. A world flag, a quest claim or an untagged copy of the same item
would recreate the permission system removed by ADR-0114.

**Decision.** The Assembler consumes an F1 Relay Matrix, Machine Casing, four renewable glass and
a Flux Cell in forty seconds at F1, and emits one Relay Matrix carrying `Rating = F2`. The former
Matrix is consumed and supplies both the required prior rating and the control component; casing,
glass and cell are the structural, dielectric and power-coupling components. `IngredientSpec` and
`OutputSpec` carry matrix ratings, so lookup verifies F1 and machine output writes F2 NBT. The
Quest Book counts only F2 stacks for its terminal task.

**Consequences.** T1 now has a visible physical exit without unlocking an F2 machine early. The
next slice can attach T2 consumers to this rating; it must not add a global permission.

---

## ADR-0124 — Atlas rating labels do not change recipe identity

*2026-10-16 · Accepted*

**Context.** ADR-0123 records an exact F1-to-F2 conversion, but the Atlas prints only the item
identifier. The same Relay item therefore appears on both sides without its different rating.
`qualified()` is also used as stable recipe identity by other consumers; decorating that shared
value would mix presentation with graph identity.

**Decision.** Decorate explicitly rated Atlas inputs, outputs and catalysts with `[F<n>]` only
inside `AtlasLogic`. Preserve the unqualified registry and recipe identifiers, ordinary item
labels, quantities and the existing menu protocol. Bare item lookup still finds every rating;
an explicitly rated lookup must match that rating. JEI resolves rated item ingredients through
the same stack resolver used for rated outputs, rather than reconstructing an untagged stack.

The F2 manufacturing recipe continues to consume exactly F1. This is not the more permissive
`canControl` rule for installing a stronger controller: accepting F2 as its own upgrade feed
would spend a finished controller for no progress. No T2 consumer is introduced.

**Alternatives rejected.** Changing `qualified()` everywhere (unnecessary identity/API churn);
adding separate F1 and F2 registry items (saved-world migration for a display defect); displaying
only the recipe suffix (hides the physical input distinction); treating ordinary unqualified
ingredients as exactly F1 (they do not currently enforce a rating).

**Consequences.** The handheld's existing input/output detail lines carry the distinction in text,
not color alone. Existing untagged bootstrap stacks remain F1 under the existing item contract.
No NBT format, packet, registry identifier, recipe balance or survival gate changes here.

---

## ADR-0125 — Machine inventories persist slots with explicit legacy migration

*2026-10-16 · Accepted*

**Context.** F2 lifecycle characterization reproduced five failures with the vanilla
`SimpleContainer.createTag/fromTag` methods used by process machines. They save compact item
lists, then merge items into the first available slots. A lone output in Assembler slot 4
therefore reloads into input 0. The previous output-slot migration edited `Slot`, but the
vanilla compact reader ignored that field. A build or completed recipe cannot prove persistence.

**Decision.** Process machines use a slot-preserving container built on vanilla
`ContainerHelper` NBT serialization. `Items` remains the list key; `ItemLayout = 3` identifies
new slot-indexed saves. All item IDs, rating tags, counts, menu slots and automation sides stay
unchanged. Loading does not publish intermediate partially restored inventories.

Migration is explicit and non-speculative:

- Slot-indexed saves retain their exact positions. An Assembler layout older than 2 moves
  its explicit former output slot 3 to current output slot 4, once.
- Legacy compact lists have no record of empty slots. Retain their item order and payload,
  logging that original positions are unavailable. For an old three-input Assembler, a fourth
  compact entry necessarily occupied its old output and moves to slot 4.
- Do not infer missing historical positions from item type or the last recipe. A player can
  recover an ambiguous legacy stack through the ordinary menu; no item or rating is fabricated.

The Matrix codec retains `Rating` and the untagged architecture-frontier default. Existing
lossless integral numeric tags remain readable; fractional, wrapped, non-finite, wrong-type
or unsupported ratings are invalid rather than silently becoming an F1/F2 controller. The
existing invalid-Matrix name/tooltip remains the recovery feedback.

**Alternatives rejected.** Keeping compact saves and changing only tests (leaves output
reconstruction broken); renaming the Matrix item or deleting old data (breaks worlds);
guessing an old output from its type (a player may have placed that item in an input);
claiming a paper carrier's packet check proves a registered Matrix's whole lifecycle (the
dedicated Forge smoke must also produce, consume, block, recover and reconstruct that item).

**Consequences.** A new save keeps sparse inputs and output ratings through binary NBT and
reconstruction. Valid old stacks are retained, but missing historical slot information cannot be
recreated honestly. Back up a world before testing a downgrade: an older jar will once again
compact inventories. Player UI, quest interaction and no-command survival acceptance remain
separate evidence from headless NBT checks.

---

## ADR-0126 — Integration smokes boot the distributable Forge jar

*2026-10-16 · Accepted*

**Context.** Enforcing clean shutdown exposed a hang after every dimension had saved. It
reproduced on a plain `:forge:runServer` boot as well as the new Matrix scenario. Thread dumps
of the probe's own processes showed the game main/server threads gone and eight non-daemon
executor workers still waiting. Closing stdin did not fix it. The precise upstream worker
owner was not proven, and killing the process must not be labelled a successful exit.

The development launch also tests transformed development classes, not the remapped jar a
player installs. That is a weaker integration boundary than the artifact the build produces.

**Decision.** `:forge:prepareSmokeServer` installs the official Forge server matching
`forge_version` into ignored `forge/run/smoke-server`, then stages the actual `remapJar`
artifact and the pinned Architectury runtime dependency. All coordinates remain in
`gradle.properties`. Both smoke boot scripts launch Java using Forge's generated platform
argument file, with a fresh scenario world and loopback-only binding. The existing scenario
manifest and assertion protocol remain authoritative.

Installer libraries and jars are generated development outputs, never committed or added to a
distribution notice as newly copied source. Stable staged filenames prevent old Grindless
versions from accumulating. Optional JEI is absent in this minimal server, which also verifies
that the standalone jar does not require the viewer.

**Alternatives rejected.** Forced `System.exit`, reflective executor shutdown or suppressing the
new exit failure (hides a broken lifecycle); speculative Forge/Loom version upgrades (no verified
fix and unnecessary dependency churn); treating a development-class boot as proof of the packaged
jar (misses packaging/remapping failures).

**Consequences.** A plain production boot and the full Matrix lifecycle both exit 0 without
forced cleanup. The loaded and built mod jars have identical SHA-256. The upstream dev-launch
hang remains a documented limitation of the optional development path, not a claimed upstream
repair. First preparation needs official Forge/Mojang artifact access; subsequent runs reuse the
installed libraries. The full isolated inventory still runs before integration acceptance.

---

## ADR-0127 — Smoke-runner dispatch is tested independently of Minecraft

*2026-10-17 · Accepted*

**Context.** Linux CI run `38968479378` printed `ATLAS-OK` while nominally running blocks,
fluids and other scenarios. `write_report` in `run-smokes.sh` iterated the global `name`
variable, leaving it set to the final selected scenario before the caller chose its command,
expectation, datapack and log files. A green report could therefore represent repeated runs
of the last scenario instead of the stated inventory.

**Decision.** Reporting loop variables are function-local. `VerifySmokeHarness` exercises the
actual platform runner in a disposable fixture with three fake boots, one deliberately failing.
It asserts exact dispatch order, nonzero failure propagation and the report's two-pass/one-fail
result. It does not launch Minecraft or replace any real smoke. Windows runs the PowerShell
twin; Linux/macOS run Bash. An explicit `--bash <executable>` also tests Bash on Windows.

The pre-fix Bash fixture executed `gamma, gamma, gamma`, missed the failing `beta`, and reported
all three passed. With the fix, Bash and PowerShell execute `alpha, beta, gamma` and report the
failure correctly. These are actual process results, not source-pattern assertions.

**Alternatives rejected.** Trusting a report's labels or aggregate exit alone (both lied
together); removing the failing Atlas scenario (would conceal the dispatch error); adding a
second scenario list in CI (duplicates the source of truth).

**Consequences.** Historical Linux aggregate smoke successes are not full-inventory evidence.
The Windows production inventory remains independent. New Linux integration evidence must come
from the corrected runner. This changes no game data or runtime behavior.

---

## ADR-0128 — Validation records its scope and repairs incomplete server installations

*2026-10-17 · Accepted*

**Context.** Independent review of supplied source excerpts identified that two generated Forge
argument files do not establish a complete installed runtime. A removed library can coexist with
an up-to-date preparation task. A local deletion/recovery regression confirmed the weakness;
declaring the libraries directory alone did not repair the already-recorded installation.
The full Windows run was also interrupted after 23 scenarios, reinforcing that start/finish,
exit status and scope must be recorded instead of inferred.

**Decision.** The installer owns its libraries and writes an explicit inventory of runtime jar
and argument-file paths and sizes. Preparation is up to date only while every recorded file
exists at its recorded size. A missing/truncated file reruns the official installer. This is a
completeness check, not a cryptographic corruption audit. Mutable worlds and staged mod jars
remain outside the installer's ownership.

`tools/validate.ps1` and `.sh` provide one fast foundation entry point: environment, compile/
full build, all behavior/resource checks and documentation. `-Smokes` / `--smokes` adds the
complete isolated inventory. Every stage records UTC times, its actual exit code and log in
ignored `build/validation/<run-id>/status.tsv`; the source snapshot records HEAD and dirty
paths. Failure stops the sequence, and an unrequested smoke stage is explicitly not acceptance.
CI uses the same fast entry point and retains its logs; the existing six smoke shards remain.

The Bash documentation entry calls the same PowerShell implementation through PowerShell Core
(`pwsh`), a documented prerequisite available on the CI runner. No second anchor algorithm is
maintained. The shared checker includes non-ignored new Markdown as well as tracked files.

**Alternatives rejected.** Assuming argument files imply intact libraries (the reviewed
counterexample reproduced); silently falling back to another installation; installing globally;
duplicating every check in CI; treating a cancelled run or an omitted smoke stage as green;
reimplementing the established Markdown anchor rules independently on another platform.

**Consequences.** The deletion regression restores the official Forge jar with the same SHA-256.
Fast validation passes all 33 current suites and links. PowerShell Core is required for local
Linux/macOS documentation validation; absence is a failure, not a skipped check. The existing
Shadow version moves to `gradle.properties` without a version change. None of these tooling
changes authorizes a new gameplay tier.

---

## ADR-0129 — Bootstrap grows through a connected ceramic workshop

*2026-10-17 · Accepted*

**Context.** The current owner-directed rework explicitly authorizes continued T0 implementation,
including useful primitive processes, automation and formed machinery. The foundation at
`c62de72` preserves the inventory, Matrix, viewer and production-smoke corrections above main.
Its earlier conditional gate was written for a narrower assignment; it must not silently veto
the new one. Nor may new authorization be mistaken for an observed survival or client pass.

**Decision.** Keep T0/F0 as the only active frontier and the five-F1-Relay-Matrix exit. Expand
the material route into a small ceramic workshop, with quantities owned by the live Java
recipe catalogues:

- A Quern mills gravel into sand and fired ceramic into reusable grog. It never doubles ores.
- A Clay Mixer combines two clay balls, one sand or grog, and 250 mB water into three unfired
  ceramic pieces. Grog reduces processing time, not material cost or yield.
- A Firing Chamber consumes three unfired pieces and one carbon fuel to make two ceramic
  insulators and one grog. Fuel supplies the process heat; F0 powers handling/control.
  A controller and seven ordinary brick blocks form its fixed 2x2x2 chamber. The controller is
  the front-left bottom corner; all item/fluid/power interfaces remain on the controller.
- Ceramic insulators replace stone in newly crafted Data Cores and supply primitive plumbing
  and item handling. Remove early quest rewards that would bypass their manufacture.
  Existing cores, matrices, machines, registry IDs, saved quest IDs and rating data remain valid.
- A later Settling Trough separates two dirt and 1000 mB water into two clay balls, one gravel
  and 750 mB recovered water. Its controller, channel and screen form a three-block-long open
  assembly, with clear space above and matching orientations. The gravel feeds the Quern;
  clay feeds the Mixer. This is a deliberately simplified game resource route, not a scientific
  mass-balance claim.

Every mandatory transformation has a visible Atlas/JEI/guide route. Every new intermediate
has a process or construction sink. Recovering grog requires fresh clay, water, fuel and power;
no cycle manufactures its own net solid input. Quests guide and reward but never grant machine
permissions. Copper/wood/ceramic recipes bring existing unpowered plumbing and handling into
T0 without new transport engines or a second energy system. The Dynamo/Extractor still require
two iron in total; optional vanilla hoppers, tools and later infrastructure are not included
in that narrow budget.

The fixed workshop structures share only the bounded local-cell validation needed by these
two concrete consumers. A broken, obstructed, misoriented or unloaded cell pauses work before
energy/input consumption. Formation is derived from the world, not trusted from saved NBT.
Progress and indexed inventories survive repair/reload. This is not the future parametric
Arc Reactor kernel, does not move that reactor out of T3, and never force-loads chunks.

**Scope and gates.** This supersedes the twenty-minute minimal material route of ADR-0116 and
the T0 structure exclusion of ADR-0108/ADR-0067 only for these process-driven workshop machines.
The working target is 35-60 minutes from the stated material entry, not an observed playtime
or a timer requirement. Manual graphical, no-command survival and retained-world acceptance
are **merge conditions**, not blockers to further validated feature-branch development.
Use IMPLEMENTATION IN PROGRESS, AUTOMATEDLY VALIDATED, MANUAL ACCEPTANCE PENDING, MERGE READY,
or BLOCKED precisely. No merge or unrelated T1/T2 expansion is authorized here.

**Research and alternatives.** The pinned mod screening remains conceptual only. Digitalfire's
[grog reference](https://digitalfire.com/material/grog), inspected 2026-10-17, supports reusing
crushed fired ceramic as a body aggregate and its drying/shrinkage role; its text/images are
not imported. Exact game counts and timings are original balancing choices. A second ore
doubler, cosmetic recolored furnaces, fuel-free heat, mandatory repeated handcrafting, and a
general multiblock framework without consumers are rejected. So is preserving the old
stone-to-core bypass while calling ceramics a meaningful progression rework.

**Consequences.** Changes ship as separately committed playable units on the dedicated T0
branch, based on the preserved foundation. Automated checks cover live graph quantities,
resource loops, inventory/recipe changes, fluid wakeup, structure rotation/invalidation and
the packaged Forge lifecycle. Old validation is historical evidence, never proof of a new jar.
Assets remain original deterministic geometry/textures unless a configured image provider
produces candidates that pass both technical and visual review.

---

## ADR-0130 — Settling uses an open oriented assembly with stateless parts

*2026-10-17 · Accepted*

**Context.** The ceramic workshop is now implemented and headlessly validated. Its clay and
aggregate inputs need a useful supplied route, and ADR-0129 calls for an assembled 3D T0 process
rather than a concept image or a decorative large machine.

**Decision.** Add a Settling Trough controller, one channel behind it and one screen behind
the channel. All three face the same horizontal direction. The channel and screen each require
air above; the controller's top deliberately remains available to an input hopper. This
clarifies ADR-0129's open-space rule without obstructing automation. The footprint is 1x3 blocks
with one block of overhead clearance over the working channel.

Two dirt and 1000 mB water yield two clay balls, one gravel and 750 mB recovered water in
160 ticks at 4 FU/t. The Quern consumes gravel; the Mixer consumes clay and water. The controller
owns all item, fluid, power, progress and NBT interfaces. Existing process side behavior remains
unchanged. Missing parts, wrong orientation, overhead obstruction or an unloaded cell stop work
before consumption; repair resumes the retained cycle.

The two structure parts have only horizontal facing, no inventory, ticking block entity,
artificial machine status or saved owner. A single visual state in `BlockCatalogue` therefore
means no `status` property; model generation, blockstate keys and the state smoke use that same
rule. `WorkshopGeometry` supplies the same original boxes to collision, model generation and
the assembled preview; controller/channel/screen segments share continuous bed/rail boundaries.
The controller supplies the active-state indication and process effects.

Extend only the existing bounded `WorkshopStructure` cell checker with observed block identity
and facing. Do not create a graph of hatches, ownership services or a generic multiblock kernel.
The layout's roles and orientation prevent another overlapping controller from treating the
same part sequence as its own.

**Research and balance.** The
[USGS sediment reference](https://www.usgs.gov/water-science-school/science/sediment-and-suspended-sediment),
inspected 2026-10-17, supports the general role of slower water allowing suspended solids to
settle. The game counts and times are original abstractions, not measured physical yields.
No source text, diagram, image or code is imported. The bounded economy check assigns two
potential units to dirt and one to each ceramic/aggregate item; every F0 process is
non-increasing under that potential. This is neither physical mass nor a whole-mod economy proof.

**Alternatives rejected.** Three independent processing block entities (duplicated inventory and
ambiguous ownership); an oversized cube with no process role; a single-state Minecraft enum
property (invalid/useless state); requiring air above the input controller (blocks its hopper);
copying another mod's model or asset; and storing a trusted formation flag across world changes.

**Consequences.** New registry IDs are additive. The menu kind and fault values append to their
existing ordinals. Deterministic checks exercise all four rotations, every required part,
clearance and orientation. A production Forge scenario must prove real separation, water
recovery, input preservation, reconstruction and repair before this unit is considered validated.

---

## ADR-0131 — Mandatory supply is mod-owned, renewable and explicitly classified

*2026-10-17 · Accepted*

**Context.** The owner established a project-wide invariant: every mandatory basic material and
consumable must ultimately have an unlimited, automatable, mod-owned supply. The existing
cobblestone-generator/tree-charcoal assumptions do not satisfy it. Nor does a repeating final
recipe whose upstream ingredients remain finite. The owner also rejects forced usefulness for
every residue and requires canonical classifications across gameplay and a factual dashboard.

**Decision.** Audit the complete authored production/progression graph recursively, including
machine construction, tools, fuels, fluids, catalysts, physical objectives and side streams.
Keep recipes and quantities in their existing live Java catalogues; canonical annotation data
adds source provenance, resource/by-product classes, handling, tier roles and bootstrap guarantees.
Generated audit and dashboard outputs are projections of those authorities, not parallel recipes.

Resource classes are `bootstrap`, `renewable`, `closed_loop`, `catalytic`, `lossy_loop`,
`finite_optional` and `forbidden_dependency`. A renewable claim needs a validated internal route
whose consumables recursively close. Bootstrap quantities must cover producer construction and
recovery without an unseeded cycle. Optional vanilla inputs or emergency shortcuts may remain,
but never the permanent canonical farm, exploit, loot or manual-mining dependency.

By-product classes are `byproduct_current`, `byproduct_future`, `byproduct_optional`,
`waste_recoverable`, `waste_hazardous`, `waste_inert`, `process_loss`, `emission`,
`catalyst_spent` and `potential_future_use`. Validate their compatibility and evidence:
current consumers exist; future consumers name a real plan; spent catalysts name their origin;
hazards have implemented handling; inert waste is not mandatory; potential use is not promised.
This supersedes ADR-0036 only where it implied every side stream must gain a material consumer.
A truthful disposal, emission or loss policy is a valid handling destination.

The same metadata supplies human-readable classifications, localization, handling advice,
tooltips, supported recipe-viewer/quest integration and the repository-driven dashboard.
Missing or unresolved metadata stays visible. Planned tiers and unobserved manual acceptance
must not acquire invented progress percentages or passing status.

CI rejects new unresolved mandatory violations, unseeded circular dependencies, finite/vanilla
canonical dependencies, contradictory classifications, unsafe handling, bootstrap starvation,
resource-positive recycling and stale generated projections. Pre-existing exceptions, if needed,
are exact versioned findings with concrete remediation; they remain reported failures/debt,
never a blanket waiver for new resources or a claim that a tier is sustainable.

**First implementation direction.** Preserve and repair the current ceramic/settling machinery,
then close foundational supply with original geological collection, biological biomass and
automatic low-tier power. Model environmental energy/material inputs explicitly; do not invent
matter without an industrial, biological, geological or other coherent source. Vanilla farms
are not the fallback implementation. Later tiers improve these routes rather than undoing them.

**Alternatives rejected.** Renaming finite extraction "renewable"; assuming tree/cobblestone
farms are internal machines; treating recycling as a source without a seed or replacement for
losses; making every waste valuable; hard-coding different tag meanings in each UI; and drawing
a dashboard from hand-maintained completion claims.

**Consequences.** T0 self-sustaining supply, audit/metadata and the first dashboard increment
are active implementation, not documentation-only work. The owner permits PR management and
merging when ready, but automated and manual acceptance requirements remain.

---

## ADR-0132 — Fluid output is owned volume, not an unmarked input buffer

*2026-10-17 · Accepted*

**Context.** The settling smoke produced the correct 750 mB, but it remained in the controller.
Clay Conduits forward what a source pushes; they do not pull from a machine. Simply pumping
every stored fluid would drain the incoming water before the process could run, and could
return it to its source tank instead of the intended output line.

**Decision.** Retain the existing single `Fluid` buffer and add `FluidOutputQuota`, the volume
known to have been produced by completed non-vented outputs. A bounded subscription pushes only
that quota through the controller's front, respecting extraction configuration. Consumption or
external extraction debits produced volume first. Incoming water never increases the quota.
The menu reports the output-ready portion separately from total stored volume. A ready recipe
reserves its primary batch against automatic export, and a process claims at most one work
callback per world tick so subscription changes cannot overclock it.

Intake is limited to the applicable primary-fluid batch instead of topping up continuously
during work. Pulls reserve actual acceptance before extracting from a neighbor; Forge fill
simulation uses the endpoint's same acceptance contract. Secondary-fluid availability counts
only neighbors that really permit extraction from that face. Vented gas handling is unchanged.

**Migration.** Existing `Fluid`, inventory layout and recipe keys are unchanged. Missing quota
means legacy unmarked stock, kept intact and not automatically exported. It may be reused in
a later batch or extracted through the existing capability. Do not guess whether an old buffer
contained input or output. Invalid explicit quotas are logged and ignored, never converted into
permission to drain unproven input.

**Alternatives rejected.** Teaching every gravity pipe to steal its upstream machine's input;
exporting the whole buffer; discarding excess/legacy fluid; silently moving an ambiguous saved
buffer into a new tank; or treating a 750 mB readout as proof of actual fluid logistics.

**Consequences.** Put process fluid outputs at the front and supply input on another free face.
The settling fixture must preserve input while unformed, reconstruct both fluid and output
ownership, then recover exactly 750 mB through a real conduit. Existing input/output quantities
and manual extraction remain valid.

---

## ADR-0133 — Bootstrap closes on geology, biomass and primitive assembly

*2026-10-17 · Accepted*

**Context.** ADR-0131 forbids treating vanilla tree/cobblestone farms and finite veins as the
canonical industrial base. The ceramic workshop needs renewable aggregate, clay, construction
wood, carbon, basic metals, redstone, glass and power before T0 can be called self-sustaining.

**Decision.** Add four concrete F0 roles on existing machine contracts:

- A Geological Collector consumes 250 mB water and 200 ticks at 4 FU/t to collect one mineral
  aggregate and one clay ball. It taps an explicitly abstracted terrestrial geological reservoir
  through a valid rock bed; it does not label the existing finite chunk vein renewable or operate
  in an airless/spatial prototype dimension.
- A sunlit Biocultivator consumes one aggregate and 250 mB water over 160 ticks at 4 FU/t to grow
  four biomass. Ambient carbon, water, mineral nutrients and sunlight explain the source.
  No vanilla tree/crop growth, mob drops or consumable seed is required.
- A Biomass Dynamo burns the registered 200-tick biomass fuel at 8 FU/t. The existing Thermal
  Generator remains the stronger F1 conversion. Stored energy still exports after fuel runs out;
  a full buffer does not consume more fuel. Gas probes validate fuel identity/volume before
  extracting rather than discarding an unsuccessful refund.
- An Assembly Bench automates the limited primitive construction catalogue at F0. The T1
  Assembler retains the advanced fabrication domain and faster repeat routes. Recipes are
  projected from the existing crafting catalogues, not independently duplicated quantities.

The aggregate has three real uses: dry stone/gravel recovery, wet recovery of basic iron/copper/
redstone fractions, and biological nutrients. Biomass supplies fuel, charcoal, paper and
fibreboard. Fibreboard is an original placeable plank-compatible material, not a vanilla tree
farm. Settling's expanded output slots append after its old outputs, preserving indexed saves.

Remove unnecessary metal/redstone bottlenecks from primitive pump, mixing, grinding, settling
and firing-control crafts. The existing two-iron Dynamo/Extractor remains an optional quicker
bootstrap, not the only way to recover. A reusable wood/stone Crank Handle can supply bounded
manual work to F0 process machines when a metal bootstrap was lost. It is a bootstrap/emergency
action, never the canonical automated energy route.

Firing recipes name their controlled fuel-backed operating band: ceramics, biomass
carbonization, glass and low-grade iron/copper reduction. The chamber holds the selected band
within its declared ceiling; inputs supply the thermal fuel. Initial chamber bricks may use
the ordinary furnace as finite bootstrap fabrication, but repeated industrial bricks/fuel/glass
have mod-owned routes. Later machines improve throughput, yield, conditions and conversion.

**Audit boundary.** Environmental geological/biological inputs are declared and checked,
not hidden zero-input recipes. Exact counts are original game abstractions, not measured
chemical mass yields. The audit must distinguish an energy-producing loop fed by sunlight/
geology from a resource-positive recycling exploit, prove a metal-free bootstrap path and
show any unresolved later-tier sources rather than claiming universal closure.

**Alternatives rejected.** Renaming depleted veins renewable; farming vanilla trees by another
name; requiring a one-off seed or the producer's own metal before recovery is possible;
free material recipes with no physical source; and moving basic supply beyond the active tier.

**Consequences.** Existing registry IDs remain. New source, material and bench IDs are additive.
F0 recipe/field state, output reservations, persistence, tooltips, audit and native full-cycle
checks accompany the sources. T0 acceptance remains pending until its complete source closure
and required observed playthrough are established.

---

## ADR-0134 — Astra orchestrates bounded workers after an isolated cold start

*2026-10-17 · Accepted*

**Context.** The owner stopped the loaded conversation and requested a new clean session,
carrying all work/planning, with Astra leading orchestration and implementation performed
through subagents. C3 source/assets are incomplete and must survive the transition without
being mislabeled as a validated product.

**Decision.** Preserve all WIP and an explicit handoff/JSON plan in Git. Start a genuinely new
isolated-worktree Astra coordinator at the published preservation checkpoint, not a fork of
the chat or a branch session in the main checkout. Transfer original requests as attachments
and private environment hints separately. Verify receipt before resuming bounded work.

Astra owns architecture, product/Tiers, sustainability/production graphs, machine families,
standard and assembled multiblocks, migrations, integration and final review. Delegate
application implementation and test execution; prefer Sol 6.1 Max for normal coding and
Luna for simple/repetitive work. Keep architectural decisions with Astra rather than
downgrading them for latency. Each worker has an input revision, outcome, owned files,
acceptance criteria and stop condition. Use few workers and serialize shared-file edits.

The coordinator may investigate, design, review code/evidence, maintain handoffs and integrate
Git; it must not quietly resume solo application coding. Report missing worker capabilities.
This is repository execution policy, not a rule inferred for unrelated projects.

Preserve draft PR54 and its base PR53. The new app branch does not inherit PR identity:
maintain the existing PR explicitly, with a single new-coordinator publisher and reviewed
fast-forward continuation after live-state/ancestry checks. No duplicate PR, retarget or history
rewrite is part of migration. The retiring session stops product writes and its merger remains
off; preserve its worktree/evidence. Conditional merge authority never substitutes for
automated or observed manual acceptance.

**Alternatives rejected.** Copying the entire overloaded chat; switching the main checkout;
discarding incomplete code/assets; replacing the review with another PR; coordinator solo
coding; unbounded worker pools; and claiming old checks validate a new revision.

**Consequences.** The [handoff](../.agent/handoff.md) and [plan](../.agent/work-plan.json)
are reloadable without session history. C3/audit work resumes first; functional ports follow
the current integration milestone. Migration neither accepts a Tier nor creates a new manual
approval gate before authorized feature development.

---

## ADR-0135 — Sustainability is a fixed-point audit over live authorities and typed annotations

*2026-10-17 · Accepted*

**Context.** ADR-0131 requires recursive proof that mandatory supply is renewable, mod-owned and
honestly classified, shared by validation, tooltips, recipe viewers, the quest book and the
dashboard. The live authorities are scattered: `ProcessGraph.generate` over a material view
(including `RenewableProcesses`, `BootstrapProcesses` and `BootstrapAssembly`), the shipped
crafting JSON checked against `BootstrapRecipes`/`T1Recipes`, generator fuel tables, quest
rewards and local tags. No classification code exists, and a parallel metadata file that copies
quantities would drift. Graph reachability alone would certify unseeded cycles as renewable.

**Decision.** *Authority.* Canonical metadata is typed Java data in a new loader-free package,
`io.github.ezequiel24123z.grindless.sustainability`, with no Minecraft or Forge imports in its
model or auditor. The game UI, checks and exporters share it. Annotations never copy a recipe
quantity; adapters read the live authorities. The canonical audit pack is standalone
Grindless + vanilla + Forge: the graph generated over `SupplyCatalogue`'s materials. Other packs
may add optional routes, never canonical ones.

*Graph.* Resource nodes are items, tags, fluids, the `energy` pseudo-resource in FU, and
environmental terminals. Route nodes come from `ProcessGraph`, shipped crafting JSON, generator
fuel tables, one-shot quest rewards, declared bootstrap sources and an explicit allowlist of
vanilla transformation recipes that must exist on the resolved classpath. A route has consumed
inputs, non-consumed catalysts, a machine requirement (family to constructible block or
multiblock), energy demand and outputs. An input that the same route returns counts only as its
net consumption. Tags resolve through local tag JSON, then through vanilla/Forge tag resources
on the classpath; an unresolved tag is a finding, never silently satisfied.

*Annotations, declared.* Each resource can declare:
- resource and side-stream classes, and first-use and first-renewable tiers;
- a canonical route, required when several non-optional producers exist;
- future consumers {tier, system, plan reference};
- handling {action, discard allowed, hazardous, containment, treatment route};
- recycling {route, planned tier} and, for spent catalysts, the originating catalyst.

Declarations outside a single resource:
- closed loops {carrier, member routes, make-up source};
- the environmental justification of each route whose matter or energy has no item input;
- optional per-route output-role overrides;
- each machine family's unlock tier;
- a T0-T15 ledger with roots and explicit unknowns.

*Annotations, derived and never declared.* Current consumers, the mandatory flag, producers,
vanilla/finite dependency and the conventional description key.

*Terminals.* Environmental sources are geology at a rock bed, sunlight or culture, a world water
body through a collector and atmosphere through an intake, each with its `SourceSite` and whether
the collector is automatable. Bootstrap sources are:
- quest rewards, with exact quantities derived from `QuestCatalogue`;
- manual world gathering, declared per resource with a justification;
- manual labour such as the crank.

World generation, mob drops, loot, tree/crop growth, block generators and trading are never
renewable terminals.

*Analysis.* Mandatory roots are the ledger's tier roots (for T0, the non-optional quest subjects
and the exit objective). Their closure follows canonical routes: consumed inputs, catalysts,
energy and machine-construction ingredients.
- **Availability** A(t) is the least fixed point over all terminals and the routes/machines
  unlocked at or below tier t.
- **Renewability** R(t) is the least fixed point over automatable environmental terminals only.
  Consumed inputs and energy must be R; catalysts and machines need A; manual sources never
  qualify. A declared closed loop must net its carrier to zero or less across its member routes
  and replace losses from R.
- Least fixed points reject unseeded cycles and self-constructing producers by construction.
- **Construction reserve.** For every exact bootstrap guarantee, the establishment set (the
  machines first needed to reach R for the closure) must not exceed it, and the margin is
  reported. Manual-world bootstrap is reported as manual dependence and never counts as
  guaranteed.
- **Recycling.** A declared recycling route may not return more of any resource than its
  canonical forward route consumed. Other positive-gain cycles without an environmental input
  are reported for review.
- The ADR-0131 compatibility and handling rules hold per tier: hazardous output needs handling
  available at its first production tier, future feedstock is never required before its tier,
  and potential future use is never required.

*Findings and ratchet.* A finding has a stable fingerprint `rule:subject[:detail]` and a
severity of error, warning or unknown.
- `tools/sustainability/debt.json` holds exact versioned fingerprints, each with its origin,
  concrete remediation and owning task.
- An unlisted error fails the check. A listed one is reported as `waived_debt`, which is still
  a failure in every report and never a pass.
- A stale entry that matches nothing also fails, so debt only shrinks.
- Unauthored T1-T15 detail is reported as unknown, not as failure or closure.

*Outputs.* `tools/checks/VerifySustainability.java` runs through `run-checks` and CI. It writes
`build/sustainability/audit.json` (audit schema 1) and a human `audit.md`. The dashboard
`data.json` (schema 1) is a generated, commit-bound projection, never committed. UIs render
projections and never reclassify.

**Implementation order.**
- S1: model, adapters, fixed points, rules, ratchet and synthetic rule fixtures.
- S2: T0 annotations and the debt baseline. Classification is Astra's decision; drafting may be
  delegated.
- S3: the dashboard projection.
- Player communication follows. Checks compile together, so S1 starts only after the
  concurrent C3 validation finishes.

**Alternatives rejected.**
- Runtime-parsed YAML/JSON metadata, which is untyped and off-convention.
- Copying quantities into metadata.
- Renewability by reachability.
- Per-route roles as the default.
- Treating every quest as mandatory.
- Committing generated dashboard data.
- Blanket or open-ended waivers.

**Consequences.** Until S2 lands, the report shows unknowns rather than classifications. The
pinned inventory extraction checks adapter coverage but is not the auditor. ADR-0120's AE2 scope
is unaffected.

## ADR-0136 — C3 integration keeps the two-iron budget and leaves vanilla tags untouched

**Status.** Accepted, 2026-10-17. Refines the C3 integration under ADR-0129 and ADR-0131.

**Context.** The preserved C3 work-in-progress fails 15 behaviour assertions, and Forge does
not compile. The JEI catalyst switch does not cover the new machine families. Three failures
are product questions, not stale numbers:
- The Biomass Dynamo spends an iron ingot, raising `ironBudget` to three. The two-iron
  contract is settled in [ADR-0056](#adr-0056--t0-bootstrap-recipes-are-authored-json-using-tags)
  and [`docs/tiers/t0.md`](tiers/t0.md).
- `data/minecraft/tags/items/planks.json` adds Fibreboard to the vanilla planks tag. That makes
  Fibreboard a substitute in every vanilla and third-party planks recipe. It also breaks
  [ADR-0051](#adr-0051--every-block-ships-a-floor-of-assets-generated-from-one-list), under
  which the only thing in the `minecraft` namespace is a mining tag.
- Fibreboard is axe-mineable, but the asset check expects every block to take a pickaxe.

**Decision.**
1. **Two iron.** The iron budget stays at two: the Hand Crank Dynamo and the Crude Extractor.
   The Biomass Dynamo uses one tagged copper ingot in place of iron. Copper is already a
   declared T0 entry input.
2. **No vanilla tag inject.** Grindless recipes that accept boards use a Grindless-owned item
   tag containing `#minecraft:planks` and `grindless:fibreboard`.
   - `BootstrapRecipes.PLANKS` and the authored JSON reference that tag.
   - The vanilla planks file is deleted.
   - The renewable Fibreboard route therefore satisfies Grindless construction without
     changing vanilla crafting.
3. **Declared tool.** Each block's harvest tool comes from the single asset list and is
   checked against it. Fibreboard is the axe exception; stone-like machines stay on the
   pickaxe.
4. **Counts with attribution.** Changed Atlas and recipe counts are accepted only when every
   added route is attributed to a live `RenewableProcesses`, `BootstrapAssembly` or crafting
   source. An unexplained route is a defect, not a new expectation. Each assertion keeps its
   semantic intent; renaming a "one route" assertion to a larger number needs a stated reason.

**Alternatives rejected.**
- Accepting a third iron because it is small. It silently breaks a documented entry contract
  and the sustainability construction reserve.
- Keeping the vanilla tag inject because it makes boards renewable everywhere. It changes other
  mods' and vanilla recipes behind the player's back.
- Weakening the asset check to "pickaxe or axe" for every block.
- Updating expected counts to whatever the code now produces.

**Consequences.** One crafting recipe and one catalogue entry change, and seven Grindless
recipe files move to the owned board tag. The Bench's physical Relay Matrix exit, the asset
quality review and the secondary-fluid reservation for foreign rate-limited endpoints remain
separate, visible C3 follow-ups.

---

## ADR-0137 — The T0 Relay Matrix exit stays a hand craft; the Bench does not fabricate it

**Status.** Accepted, 2026-10-17. Resolves the "Bench physical Matrix exit" C3 follow-up of
[ADR-0136](#adr-0136--c3-integration-keeps-the-two-iron-budget-and-leaves-vanilla-tags-untouched).

**Context.** The Assembly Bench projects the Bootstrap crafting catalogue and four vanilla
basics. The T0 exit, one five-unit F1 Relay Matrix batch, is the `T1Recipes` shaped craft.
The integration review asked for proof of "automated physical exit fabrication" rather than
assumed coverage. The ambiguity is whether T0 must automate that craft.

The settled design says it does not:
- [`docs/tiers/t0.md`](tiers/t0.md) defines the exit as physically calibrating a core and
  recovering one five-Matrix batch through the shaped craft.
- The README's T1 row says that the first batch builds the T1 machines, and that the Assembler
  then automates the same batch.
- The T1 acceptance objective in the build-out is that the factory "no longer depends on hand
  fabrication".
- [ADR-0133](#adr-0133--bootstrap-closes-on-geology-biomass-and-primitive-assembly) gives the
  Bench only the limited primitive construction catalogue and keeps advanced fabrication in
  the T1 Assembler.

**Decision.**
1. The T0 exit stays a hand craft at a crafting table. The Bench must not project
   `T1Recipes` or any Relay Matrix route.
2. "Physical exit" means three things:
   - the calibrated core is produced by the physical Research Terminal;
   - the core's glass, copper and redstone inputs come from T0 machine routes;
   - the five-unit batch is recoverable.
   It does not mean automated fabrication.
3. The follow-up becomes one evidence requirement for the C3 native renewable cycle and the
   sustainability audit: the exit objective is a T0 mandatory root, and its consumed inputs
   reach mod-owned renewable sources. The no-command played route to the exit remains a
   manual acceptance gate.

**Alternatives rejected.**
- *Letting the Bench project the Matrix craft.* That moves T1's defining milestone (removing
  hand fabrication) into T0 and blurs the tier boundary.
- *A Bench-only Matrix recipe.* It is a second quantity authority for the same item.
- *Dropping the requirement.* The exit inputs still need recursive supply proof.

**Consequences.**
- No recipe, machine or count changes.
- A check guards the boundary: Bench routes never output a Relay Matrix.
- The work plan's renewable acceptance criterion changes from "automated physical Matrix
  exit" to the exit-root supply evidence above (plan amendment PA-0002).

---

## ADR-0138 — A batch completes only after its fluid inputs are re-verified and drawn exactly

**Status.** Correctness requirements accepted 2026-10-17; source-backed native-store design
accepted 2026-10-18. Native implementation and exception correction reviewed and verified
on composed source `dcb8010`. This is not full C3 acceptance under
[ADR-0136](#adr-0136--c3-integration-keeps-the-two-iron-budget-and-leaves-vanilla-tags-untouched).

**Context.** A process recipe takes its first fluid input from the machine's own buffer. Any
further fluid input is a *secondary*, drawn from neighbours when the batch completes. Today
only two Chemical Reactor recipes have one: `contact/acid` (sulfur trioxide plus water) and
`recombine/water` (hydrogen plus oxygen).

The initial review attributed a possible partial payment to a stale cached match, claiming
that completion did not re-check fluid availability. Reinspection of `b08deeaa` corrected
that claim: `finish()` calls `canOutput()`, which already calls `ProcessLookup.matchesFluid`.
The claimed stale-neighbour duplication is therefore withdrawn, not treated as a reproduced
bug. Missing inputs are nevertheless conflated with full outputs, and the returned amount
from `FluidInsert.takeFromNeighbours` is not checked at consumption.

The bounded implementation investigation established:
- Every in-mod `FluidEndpoint` extracts exactly `min(requested, contents)` from a face that
  permits extraction in the inspected buffer implementations.
- `takeFromNeighbours` only draws from in-mod endpoints; a foreign Forge handler is never a
  secondary source.
- An exact drain does not imply a reversible drain. `HandPumpBlockEntity` and
  `FluidWellBlockEntity` reject insertion, while a process machine can expose an extract-only
  face or reject the drained fluid as a new input.
- If an earlier source delivers 100 mB and a later source violates its drain contract,
  ordinary `insert` cannot guarantee returning the earlier volume. Logging that rejection
  is not loss prevention. No observed in-mod rate limit establishes such a violation; this
  is a counterexample to the proposed rollback guarantee.

**Decision.**
1. Completion verifies before it mutates. In the completion tick, before any item, fluid or
   output changes, the machine checks:
   - the primary fluid against its own buffer;
   - every secondary against the extractable neighbour volume, computed with exactly the same
     face and identity filter that will draw it.
2. A short input means no mutation. The batch keeps its recipe and completed progress and
   waits at completion. While waiting it draws no power, gains no progress and reports the
   missing input. It completes once the input returns. Changes to the machine's own inventory
   still re-resolve the recipe as they do today.
3. A batch must not produce unpaid outputs or lose already drawn fluid on a failed take.
   The original proposal to restore every source through ordinary `insert` is withdrawn.
   Simulation is not a reservation, and insertion permissions are not a restoration
   contract.
4. Use the existing native `FluidBuffer` as the owned storage boundary. A bounded internal
   batch operation plans exact withdrawals from identified native stores, checks identities,
   current contents and face permissions again, and commits against those stores without
   calling arbitrary external extraction handlers. Duplicate backing stores cannot be
   counted twice. Native participants own any snapshot/receipt and restoration operation;
   callers must not gain an unrestricted buffer setter.
5. All participating native stores must support exact abort/restoration independently of
   their external insertion rules. Preserve complete `FluidState` metadata and output-volume
   accounting, not only an amount. Keep notifications, synchronization and subscription
   changes out of the tentative mutation phase; notify only after the batch and the
   machine's own input/output commit are settled. An aborted operation leaves no committed
   item, primary-fluid, output or source-accounting mutation.
   This includes synchronous exceptions during validation, withdrawal or consumer settlement,
   not only boolean rejection. The consumer supplies explicit compensation even when it has
   no fluid withdrawal. Restore attempted native receipts and deep consumer state before
   propagating the original unexpected exception; retain compensation diagnostics without
   skipping remaining restores. Fatal VM errors and world-crash atomicity remain excluded.
6. Adapt every currently supported native source, including output-only pumps and wells;
   do not silently restrict recipes to refillable tanks. External sided capabilities retain
   their existing restrictions and cannot obtain internal restoration access. Unknown
   implementations do not receive an optimistic contents-based transaction fallback.
7. This is a synchronous server-thread, native-store guarantee, not an arbitrary-handler,
   cross-thread or crash-atomic world transaction. No new persistent escrow, saved key,
   port or Forge/AE2 integration is authorized. If the bounded contract cannot preserve
   existing native sources without such changes, escalate before implementation.
8. `FluidEndpoint` documents its actual exact-extraction and non-reversible-insertion
   contract. Behavioural evidence must cover missing input, split sources, extract-only
   sources, partial execution and restoration before a stronger guarantee is accepted.
9. A general reserve-and-commit model for foreign, rate-limited or port-backed sources is
   *not* built here. That includes Forge handlers, hatches and AE2. It belongs to the held
   `multiblock-ports` task's PA-0001 criterion that recipes simulate, reserve, process and
   commit atomically. Until that exists, secondary sources stay restricted to in-mod
   endpoints.

**Solved-problem-first evidence.** Three parallel read-only investigations inspected these
exact upstream implementations; no runtime failure-injection result is claimed:

| Reference and pin | Verified semantics relevant here | Reuse boundary |
| --- | --- | --- |
| [GregTech CEu 2.8.10](https://github.com/GregTechCEu/GregTech/blob/9fe140febe8747bbe2f06dfd570421331ec06f4b/src/main/java/gregtech/api/util/GTTransferUtils.java#L34-L98), Minecraft 1.12.2 | Simulated drain/fill followed by sequential execution; the ordinary transfer ignores final acceptance. [Recipe input execution](https://github.com/GregTechCEu/GregTech/blob/9fe140febe8747bbe2f06dfd570421331ec06f4b/src/main/java/gregtech/api/recipes/Recipe.java#L192-L242) is not a general reversible batch. | [LGPLv3](https://github.com/GregTechCEu/GregTech/blob/9fe140febe8747bbe2f06dfd570421331ec06f4b/LICENSE); different loader/API generation, not MIT-only copyable code. |
| [GregTech Modern 7.5.3-1.20.1](https://github.com/GregTechCEu/GregTech-Modern/blob/91a79b8a7a2b62ec6277423e6c0ded4af89a831e/src/main/java/com/gregtechceu/gtceu/common/machine/multiblock/part/FluidHatchPartMachine.java#L200-L223), Forge 47.3.0 | Hatches eagerly stage inputs in owned tanks. [Recipe allocation](https://github.com/GregTechCEu/GregTech-Modern/blob/91a79b8a7a2b62ec6277423e6c0ded4af89a831e/src/main/java/com/gregtechceu/gtceu/api/recipe/RecipeRunner.java#L174-L239) simulates then executes; no general source rollback is established. | [LGPLv3](https://github.com/GregTechCEu/GregTech-Modern/blob/91a79b8a7a2b62ec6277423e6c0ded4af89a831e/LICENSE); direct reuse also brings its Forge/LDLib storage contracts. |
| [Mekanism 10.4.16.80](https://github.com/mekanism/Mekanism/blob/95b03e838aa94128596db42154a1527048372999/src/main/java/mekanism/common/capabilities/fluid/BasicFluidTank.java#L155-L228), Minecraft 1.20.1, Forge build 47.1.76, API 10.4.0 | Separates external insertion policy from owner-controlled storage access. [Multi-input processing](https://github.com/mekanism/Mekanism/blob/95b03e838aa94128596db42154a1527048372999/src/api/java/mekanism/api/recipes/cache/PressurizedReactionCachedRecipe.java#L54-L120) still relies on owned-handler behavior, not arbitrary-source compensation. | [MIT](https://github.com/mekanism/Mekanism/blob/95b03e838aa94128596db42154a1527048372999/LICENSE), with its copyright/permission notice required for copied portions; Forge-specific code is not a loader-free drop-in. |

The selected precedent is **owner-controlled storage separated from public access policy**,
adapted to Grindless's existing native buffer. None of the inspected implementations proves
the stronger arbitrary-handler rollback guarantee, so their unchecked-result behavior is
not copied. Adding eager multi-tank intake would change saved state and input custody, and
introducing foreign storage libraries would widen this bounded repair unnecessarily.
The implementation reuses Grindless's existing storage primitives and independently applies
the ownership pattern. No upstream code or assets have been imported. Any later direct
reuse requires exact source, licence and notices in `THIRD_PARTY_NOTICES.md` before copying;
the reference table is not a blanket permission to reuse third-party material.

**Alternatives not authorized.**
- *Ordinary insertion as rollback.* An extract-only source is a concrete counterexample.
- *Rejecting buffering or restoration contracts before inspecting existing solutions.*
  The initial dismissal of escrow was premature. After comparison, transient native-store
  operations preserve the existing storage layout; persistent staging remains outside this fix.
- *Invalidating the cache on every neighbour change as a transaction guarantee.*
  Notification alone does not define exact execution or restoration.
- *Building the generic transaction seam now.* It is ports work, and the port audit must
  design it first.
- *Throwing on underpayment.* That crashes the server tick over a recoverable condition.

**Consequences.**
- No recipe, quantity, saved key or tier change is authorized by this investigation.
- The independent Bench boundary check and unrelated sustainability work may continue.
  `NativeFluidStore`, `FluidInsert` and the private process `Completion` implement the
  native-only contract. A first delivery handled boolean rejection but missed exceptions;
  red fixtures in `b26ee8a` exposed 15 failures, and `fa3e708` repaired them before integration.
  The same reviewer closed that P1. Composed S1/native source `dcb8010` subsequently
  passed 36 suites plus production `states,settling`, with matching built/deployed hashes.
- The common-machine fix preserves the two Chemical Reactor recipes without opening new
  T1 or T2 content. A targeted process fixture is not a played-survival or retained-world pass.
- The C3 acceptance on partial extraction now includes verified secondary completion
  (plan amendment PA-0002).

---

## ADR-0139 — Long native smokes carry their own assertion budget

**Status.** Accepted, 2026-10-18. Twin-runner support passes its native fake-boot harness;
ordinary execution of the long native-cycle fixture is still pending.

**Context.** The renewable T0 smoke executes real machine work. Even Bench fabrication and
Terminal calibration together need 1,200 powered ticks, before source and downstream cycles.
The ordinary assertion deadline is 120 seconds. Passing only with a manually supplied
`SMOKE_EXPECT_TIMEOUT` would leave the normal runner and CI broken for the same fixture.

**Decision.** A scenario may add a sibling `tools/smoke/<name>.timeout` containing one
positive integer assertion budget in seconds. The existing explicit `SMOKE_EXPECT_TIMEOUT`
override wins, then the scenario budget, then the unchanged 120-second boot-helper default.
Server-startup `SMOKE_TIMEOUT` is separate and unchanged. The PowerShell and Bash runners
must forward identical values, reject malformed selected metadata before booting, and never
leak one scenario's budget into another or into the caller's environment.
Sidecars are BOM-free decimal digits with optional ASCII whitespace, bounded to
1--2,147,483,647 seconds; leading zeroes normalize to decimal. Selected malformed metadata
fails even when an environment override exists. Listing and unselected scenarios retain
their existing behavior.

Keep `scenarios.txt` as the single scenario list, with its existing syntax. No per-scenario
CI step, global timeout increase, machine-speed override or skipped assertion is added.
Fake-boot harness checks cover selection, invalid budgets, explicit override, consecutive
scenarios and platform parity. The real long scenario must also pass through the ordinary
runner with no environment override; a special invocation alone does not close integration.

**Alternatives rejected.** Raising every scenario's deadline would delay unrelated failures.
Changing recipe durations or injecting outputs would invalidate the production proof.
Adding a second scenario list or bespoke runner would bypass the existing shard/selection
contract. A small optional fixture file preserves that contract and local/CI parity.

**Implementation boundary.** `run-smokes.ps1` and `run-smokes.sh`, including the CI shard
path, are covered by 27 native fake-boot checks each. The legacy
`run-smokes-batch.ps1` is an unchanged shared-world developer utility: it clears
`SMOKE_DATAPACK` and already cannot execute the normal per-scenario datapack isolation.
It is not an acceptance path for this fixture or its deadline contract. Use the canonical
twin runners; extending or retiring that pre-existing batch utility is separate work.

---

## ADR-0140 — Validate behavioral classes and reuse proof under explicit assumptions

**Status.** Accepted, 2026-10-18; owner-directed validation policy for active Grindless work.

**Context.** S1's audit contracts, C3 native completion and its exceptional-abort repair,
representative production bindings and the bounded renewable cycle now have source-bound
evidence. Repeating expensive native runs for each ordinary content instance would add
latency without necessarily covering a new behavior. Conversely, an unchanged class name
does not prove unchanged ownership, conditions, persistence or external assumptions.

**Decision.** Classify remaining validation as shared-contract, static/data, representative
integration, regression or genuinely unique behavior. Before an expensive run, identify
the specific unproved property and ask whether an existing proof plus static evidence
already establishes it. Reuse when the mechanism and relevant assumptions match; otherwise
run the smallest targeted proof and the affected regression set.

Keep evidence bound to its actual revision/artifacts and limits. Data authored in Java is
classified by its effect, not its extension. New data requires renewed data/model validation,
not an automatic replay of the unchanged kernel or one native scenario per recipe. Missing
metadata, tag resolution or an unproved model bound is not automatically a native/manual
behavior gap, and it is never silently promoted to a pass.

Changes to shared mechanisms invalidate only proofs depending on the changed assumptions.
Independent QA and architecture review focus on uncovered behavioral classes and invalid
reuse claims. ADR-0137/0138 boundaries, transaction failures, bootstrap, persistence,
external integrations and genuinely different execution paths retain the proof they need.
Existing CI requirements, thresholds, tests and real manual acceptance gates are not removed.

**Current effect.** The successful S1/C3 composed gate and renewable-cycle evidence remain
valid within their stated scopes. No runtime rerun is required merely to adopt this policy
or to clarify the fixture README. Product task order, dependencies and held ports do not change.
The [validation strategy](testing/test-strategy.md) records reusable proofs, remaining work
classes and impact triggers; the execution plan and check ledger reference it.

**Alternatives rejected.** A native scenario for every unknown or every recipe; treating
all instances as equivalent despite different assumptions; discarding source-bound passes
when only unrelated content changes; and using proof reuse to hide missing evidence or
weaken acceptance. Coverage of behavioral classes, not executed-check count, is the objective.

---

## ADR-0141 — Canonical T0 closure uses real authorities and conditional establishment

**Status.** Accepted, 2026-10-18; S2 implementation and its source-bound closure are in progress.

**Context.** The owner prioritizes coherent T0 acceptance over new tier breadth. S1 supplies a
verified audit kernel, but canonical pack membership, real source annotations, full construction
and an honest finite establishment witness are still needed. Existing shared/native proofs are
reused under ADR-0140; unclassified data is not a native scenario per entry.

**Decision.**
- Keep the full implemented inventory, separating active T0 facts from unknown/unaccepted later
  placement. Do not erase routes, invent tiers or waive mandatory findings to make the audit green.
- Author renewable-at-T0 claims only for the approved source/workshop material chains and
  actual automatic projections; the audit must prove those claims. Construction needs
  availability, not an invented automated recipe for every registered block. Finite entry
  supplies remain bootstrap assumptions, and first use is not inferred from registration.
  Slag is a potential future-use side stream in the active T0 contract: its refractory-brick
  prototype consumer exists, but an accepted later-use tier is not established here. Keep
  that uncertainty and first-use tier explicit, with no fabricated future-consumer tier.
  Its renewable-at-T0 claim may follow the real copper-reduction by-product route; it is
  not a required T0 input. Ordinary item disposal is permitted under current non-hazardous
  item behavior, but neither this advice nor `ContainerConfig` establishes an automated
  Storage Crate/Overflow Gate sink. Do not force retention or invent a consumer.
- Export actual Gradle-resolved Forge resource inputs through a small build-task seam. Verify
  `TagEntry`/`TagLoader` membership, required/optional, nested, removal and replacement semantics
  against the actual Minecraft/Forge implementation. Use demonstrated layer precedence or prove
  that the concrete composition is order-independent; filenames, hashes and namespace guesses
  are not registration or precedence evidence. Runtime registry access stays outside the pure
  model/auditor, and absolute cache paths stay out of public projections.
  A route may have real environmental evidence while its progression tier is unknown:
  nullable `RoutePolicy.unlockTier` means not unlocked and retains `route_tier_unknown`,
  never an implicit T0. Physical atmosphere/light terminals do not unlock a prototype.
  `OPEN_SKY_DAYLIGHT` describes the solar producer's actual day/open-sky predicate without
  importing the culture site's terrestrial restriction. The current intake's absent
  site guard is reported as such, not as verified atmospheric simulation in every dimension.
  A direct, positive, declared manual-labour input may justify a non-automatic physical
  route without becoming an environmental source or renewable credit. That guard alone
  supplies no budget, new profile or guarantee; the optional Hand Crank Dynamo's known T0
  placement is separate from an unproved finite labour witness.
- Give `Route` an explicit `automatable` fact, independent of ownership. Manual crafting
  provides availability but cannot enter renewable supply or a renewable `ClosedLoop`.
  Adapters must derive the fact from the actual mechanism: a physical producer is not
  automatically an automated producer. Preserve the old constructor for S1 compatibility,
  not as evidence for canonical adapters; include automation in fingerprints and exports.
- Treat `TierPlan.exitObjective` as a finite production/availability goal, not recurring
  demand for the manual Matrix itself. Its canonical route must execute even when assumed
  stock or a gift already contains the goal, with batch output and objective count derived
  from the real authorities. Consumed exit ingredients still require renewable supply;
  construction and non-consumed requirements retain their availability obligations.
  Do not add the Relay quest's Matrix subject back as a recurring root, weaken unrelated
  recurring roots, or make the Assembly Bench fabricate the five-unit manual batch.
  This refines the audit meaning of ADR-0137's mandatory exit root without changing gameplay.
- Reuse non-consumed route requirements for formation. Keep the controller in
  `Machine.construction`, derive non-air pieces from `WorkshopStructure.cells`, and share the
  existing family-to-structure link with the runtime instead of copying counts into annotations.
  Current T0 cells exclude the controller. The witness covers one installation per family,
  not simultaneous reuse of physical pieces across installations; structural pieces must not
  be mislabeled as chemical catalysts. No fictitious registered assembly item is introduced.
- The existing `ClosedLoop` representation can describe the source-production circuit with
  aggregate as a zero-net carrier. Its normalizer needs a conservative unit-vector candidate
  when that carrier closes exactly and other internal balances are nonnegative. Retain the
  exact normalizer fallback and all environmental, loss, independent-feed and executable-startup
  checks. A positive net output does not pay its own earlier prefix, and finite carrier stock
  does not become unlimited supply. No new general solver or production-system API is required.
- Represent manual entry requirements separately from real guarantees. A sufficient finite bill
  is derived from real recipe/formation/order requirements and then checked by replaying the
  witness with that finite budget, without unlimited manual replenishment. Report guaranteed,
  player-provided, required and remaining amounts separately, together with the conditional
  profile and its identity. Do not claim a globally minimal bill, a gifted kit or guaranteed
  world loot. `MANUAL_WORLD` and `MANUAL_LABOUR` retain no guaranteed quantity.
- Respect physical bootstrap interfaces. Account for the reusable tool, and do not let abstract
  manual FU imply arbitrary transport: the portable Crank Handle applies to F0 process machines,
  not the Research Terminal. Keep its conditional work allowance separate from transferable
  `ENERGY` stock: it may pay only an eligible process batch and never enters renewability.
  Eligibility is derived from the real interface/rating contract. Actual generator construction
  and the canonical source route remain requirements rather than being bypassed by a generic
  energy credit.
- `quern` follows `multitool`; `crank` and `extractor` remain optional with existing IDs and
  rewards. `calibrate` still follows `ceramics`, and `relay` follows `calibrate`. Quests check
  possession/claim state, not provenance or the fabrication of every intermediate. The physical
  recipe/Terminal route, not a redundant quest permission, defines production.
- Deliver portable client preparation separately from acceptance. A source-pinned Prism-
  importable ZIP may precede final S2 closure, but it must carry the actual production
  remapped artifact and honest provenance, not a post-S2 label. The reference client uses
  Grindless, its required Architectury dependency and JEI for recipe discovery. Preserve
  the existing expanded client pack separately: its utilities, extra content and mixins
  are not part of the standalone canonical audit assumption. Launcher-managed dependencies
  retain verified upstream pins and licences. A supplied world must actually be generated
  by Minecraft without fixtures, commands or injected stock; otherwise create a fresh
  Survival world on the capable host. Do not fabricate NBT or repair an unsupported VM
  graphics environment merely to avoid honest host-side observation.
  Normal exploratory play and crafting in that new world need not wait for the auditor.
  Preserve the real source/settings and observations without an imposed starter bill or
  injected resources. A later unchanged-behavior proof may be reused; the formal whole-path
  acceptance still needs its agreed candidate/requirements. Reopening this new save under
  the same binary is continuity, not proof of compatibility across an update.

**Validation and consequences.** Static/model checks cover the new authority/profile data and
the affected normalizer, reserve, membership, formation, automation, exit and quest contracts. A genuinely new
runtime binding needs one appropriate proof, not one world per tag or recipe. Existing S1,
native restoration and bounded daylight-cycle evidence remains valid within its assumptions.
The conditional entry witness must later be confronted with an actually acquired survival
inventory. Neither these decisions nor a diagnostic client/world accept T0, a retained world,
the physical five-Matrix playthrough or any T1--T15 expansion.

**S2 refinements.** Canonical recurring production does not prohibit a real manual first
construction. The finite availability witness may fall back to verified, unlocked,
non-optional, non-automatic and non-one-shot crafting authorities, retaining actual input
costs and completed sublots. It never substitutes that fallback for renewable production
or the mandatory canonical exit batch. `establishmentBatches` exposes completed replay
batches separately from the canonical route and goal result.
The existing handheld quest UI is also a physical prerequisite: reward claims require one
non-consumed Quest Book, acquired before the first claim. A later renewable book, a creative
registration or a proposed future backend keybind cannot finance access retrospectively.
The same book is reusable across claims; do not consume or reserve one copy per quest.

The sufficient entry bill must fund the actual shared construction/claim witness and the
required independently replayed startup proofs. Componentwise maxima between alternative
proof bills are conservative allowances, not a sum of their execution steps, a minimal kit
or a replacement for joint feasibility. Optional rewards require explicit selection.

The active T0 conditional gate is separate from global sustainability and physical acceptance.
It fails relevant global integrity/unknown/positive-cycle findings as well as T0-context
findings; relevance is not just a string in the detail or an SCC's representative vertex.
Unknown first use of a non-mandatory output does not waive its class, route, R0 or handling
proof. Flint is optional in the active T0 contract, with ordinary storage/discard and
unplaced first use, not an invented mandatory consumer.

A unit-positive potential may certify the same no-positive-gain predicate checked by
`positiveCycles` when every member's complete non-energy net sum and net energy are both
nonpositive, using exact integer arithmetic. Non-consumed requirements contribute no spent
input. This only removes a genuinely discharged uncertainty; it preserves warnings and
existing searches, does not assert physical mass conservation and does not establish supply,
startup or acceptance on its own.

## ADR-0142 — T0 client feedback reuses native UI and existing quest infrastructure

**Status.** Accepted, 2026-10-18; client engineering is integrated and the selected quest
adapter remains under integration review.

**Context.** Real-player observations on the preparation client expose an invisible slot grid,
overlapping Inventory/status text, textual JEI fluid lists and repetitive non-working-state
audio. The owner wants a clearer native interface, red non-consumed catalyst text, real fluid
visuals and an existing compatible quest system instead of more bespoke infrastructure.
The placed machine models are positively received. The owner explicitly parks investigation
of the three T1 recipe-output screenshots and unconfirmed T1 crafting-complexity changes.

**Decision.**
- Repair actual machine/player slots and bounded status layout immediately, independently of
  S2 and backend research. Preserve physical slot indices, I/O restrictions and saved item layout.
  Share a small physical layout description between the existing menu/screen and JEI; do not
  create a general UI framework. Native Minecraft/JEI primitives already solve this problem.
- Actual `recipe.catalysts` use the non-consumed ingredient role and a localized red
  `Not consumed` tooltip. A machine recipe-catalyst icon is a different JEI concept; consumed
  inputs and structural requirements must not be relabeled as chemical catalysts.
- Render current T0 water with its real native texture/tint, amount and capacity. The output
  quota is part of the same store, not extra volume or a second tank. JEI keeps complete
  machine-specific I/O positions, including empty slots and all process conditions. Internal
  fluid IDs without Forge registration retain a visible diagnostic cell/role/quantity, not
  a fake FluidStack, guessed chemical color, hidden input or new fluid registration.
- Recipe restrictions alone do not establish a restored buffer's identity. Preserve loaded
  contents and append a server-confirmed water flag to the existing menu payload (old fields
  unchanged). Unconfirmed nonempty data retains volume/capacity/quota with an unknown
  diagnostic; it is not erased, treated as empty, or painted as water. This is derived
  synchronization, not a save migration. Client and server must use the same candidate.
- Ordinary non-working states retain visual feedback without repetitive relay/beep noise.
  Keep legitimate working effects and the distinct pylon brownout alert. New sound selection
  is independent: verify each asset's source/licence, not just the upstream code licence,
  before import. A compatible shortlist is not an import or an auditory acceptance result.
- Evaluate an existing Forge 1.20.1 quest backend against the actual stable catalogue,
  per-player claimed state, prerequisite/reward semantics and actual physical-book access.
  Preserve existing book registration and old saved data; an item can bridge to a new UI.
  Generate definitions from `QuestCatalogue`, rather than hand-maintaining a second catalogue.
  No wholesale authority replacement, team-sharing behavior, raw save overwrite, duplicate
  reward or new recipe permission is authorized by a product name or research summary.

The exact-source GregTech Modern/LDLib study confirms a shared widget tree and native
Minecraft/Forge/JEI rendering underneath. Native APIs suffice here without copying or adopting
the much larger external widget engine. The study is a pattern/API reference, not a licence
grant or proof that every target patch-build API is identical; compilation uses the project's
resolved dependency. No upstream code, texture or sound is imported in these checkpoints.

**Selected quest adaptation.** Use Heracles' compatible Forge 1.20.1 UI/task framework with
one registered Grindless claim-task/widget adapter. The selected published binary exposes
the required task/widget APIs; no mixin, fork or invented requirement registry is needed.
Native task completion represents an already recorded legacy claim, not cached possession:
the backend's completed-parent relation thereby preserves the catalogue's claimed-parent
relation. Current evidence, the physical book, per-player idempotency and rewards remain
server-validated through the shared Grindless claim service. Disable native/autoclaim
reward delivery for these generated quests; reconstruction never grants items.

Keep `QuestCatalogue` and `grindless_quest_progress` authoritative and preserve the registered
book/menu. Generate stable, owned backend definitions and reconcile only their projection;
do not replace another pack's quests or raw progress file. Set individual progress and
retain the legacy fallback when the optional backend is absent. This is a narrow gameplay
adapter to an existing framework, not a replacement custom quest engine. Dependency/API,
projection and reward/migration checks precede activation and packaging.

**Definition publication and recovery.** Publish generated definitions and their ownership
proof as one owned archive through atomic replacement, not a sequence of loose JSON writes
followed by a manifest. A failure before publication must leave either no published bundle
or the prior complete bundle; retry must not adopt partial or foreign data. Refuse unsupported
atomic replacement rather than silently falling back to a non-atomic move. Check collisions,
member hashes and the real selected loader's filename/ZIP rules; temporary files are not quests.
The loose25-JSON layout from unpublished local `f106397` exists only as development evidence:
do not convert, adopt or delete it automatically. Preserve/refuse legacy collisions visibly,
use fresh configuration for the new format and retain existing diagnostic files. This is not
a user-save migration or permission to overwrite third-party quests.

Under ADR-0140, the unchanged f106 registration/lifecycle boot may be reused only after the
new format passes the published loader plus Quest.CODEC and real failure/retry tests.
Codec-only serialization or file counts do not establish loader integration. Retained human
worlds and client behavior remain independent gates.

The selected artifact is Heracles 1.1.13, CurseForge845831/file5406935, with ResourcefulLib
2.1.29/file5659871. Inspection of the actual jar establishes bundled Hermes1.6.0 and
MixinExtras0.2.1; a moving source branch or `latest.release` is not the artifact pin.
The provenance ledger distinguishes upstream-managed dependencies from imported material.

**Validation and delivery.** Run focused geometry, role, capacity, tooltip, platform-bridge and
audio-policy checks. Reuse unchanged item interaction, executor, native fluid and save-layout
proofs. Keep source-scoped human evidence separate from global satisfaction or a prior version.
The owner authorizes autonomous work and a future merge **after T0 is genuinely ready**.
That does not certify missing checks, override branch protection or permit an early merge.
Preserve the live PR stack, reviewed Git checkpoints and a portable, truthful handoff so a
later session/device can continue without private paths, lost worktrees or invented passes.

---

## ADR-0143 — Player resource guidance is a generated view of canonical declarations

**Status.** Accepted direction, 2026-10-18; implementation and client acceptance are pending.

**Context.** S2 now supplies canonical resource classes, side-stream roles, handling,
source routes and explicit unknown tiers, but the installed client cannot run its
repository/pack-source audit. `AuthorityAdapters.live/canonical` require a source-root path;
calling them from hover or rendering would be both incorrect and expensive. Existing guide
prose also sends the player toward the optional finite extractor and vanilla farms rather
than the mod-owned recurring route. A second handwritten tooltip catalogue would drift
from the same metadata the dashboard and validator use.

**Prior art.** GregTech Modern v1.4.6 (Minecraft1.20.1), commit
`aa41aba133f31dd3928bea816d6fd032cd1d90b6`, separates registered material definitions from
thin presenters. Its
[material builder](https://github.com/GregTechCEu/GregTech-Modern/blob/aa41aba133f31dd3928bea816d6fd032cd1d90b6/src/main/java/com/gregtechceu/gtceu/api/data/chemical/material/Material.java#L1212-L1234)
registers typed properties; the
[tooltip formatter](https://github.com/GregTechCEu/GregTech-Modern/blob/aa41aba133f31dd3928bea816d6fd032cd1d90b6/src/main/java/com/gregtechceu/gtceu/client/TooltipsHandler.java#L43-L110)
checks property presence, and its
[JEI adapter](https://github.com/GregTechCEu/GregTech-Modern/blob/aa41aba133f31dd3928bea816d6fd032cd1d90b6/src/main/java/com/gregtechceu/gtceu/core/mixins/jei/FluidHelperMixin.java#L18-L24)
passes the actual fluid amount through. Its
[guide recipe view](https://github.com/GregTechCEu/GregTech-Modern/blob/aa41aba133f31dd3928bea816d6fd032cd1d90b6/src/main/java/com/gregtechceu/gtceu/api/gui/compass/GTRecipeViewCreator.java#L19-L47)
uses a runtime recipe, not a second quantity table. This LGPL-3.0 source is a pattern
reference; no upstream code, assets, mixin or widget engine is imported. Its material
property validation is not a sustainability proof or a universal guide-prose generator.

**Decision.**
- Retain S2's typed declarations as the authority. Generate a compact, versioned,
  read-only runtime resource from the same canonical Result, with a declaration-content
  fingerprint and exhaustive parity/drift checks. Treat it as a generated packaged asset,
  not an independently edited catalogue. Do not generate a placeholder from fixtures when
  the real audit cannot run.
- Project classes, known/unknown tiers, canonical route, handling/recycling, uncertainty
  and public source references. Exclude copied recipe quantities, private paths, the finite
  entry bill, player state and global/conditional acceptance verdicts. Fingerprint the
  projected content rather than introducing a self-referential commit/source hash.
- Load/cache that prepared declaration in the installed mod without repository/JDK
  inspection. Invalid or missing data is explicitly unavailable with normal diagnostics;
  unknown is not an empty success, default class, renewable certification or Tier unlock.
  The declaration describes authored policy, not live operating readiness in every world.
- Reuse one native formatter across item/fluid hover, JEI, machine/Atlas details and
  quest/guide context. Keep actual stack/fluid amounts and recipe I/O in their current
  catalogues/views. Resource side-stream class does not determine an output's recipe role.
  Preserve slots, packet fields, fluid identity, saved data and claim behavior.
- Keep grog/water reuse, slag's unplaced potential future use and ordinary discard, and
  flint's optional storage/discard honest. Do not invent a consumer, mandatory hoard,
  containment system, automatic void or accepted future tier.
- Correct the current T0 guide toward geological/biological supply, biomass power,
  natural water and ceramic processing. The iron/extractor route is optional and finite;
  vanilla farms are not the permanent canonical source. Use live recipe references rather
  than duplicated fixed quantities. Preserve the calibrated-core/manual five-Matrix exit
  and do not present the model's sufficient entry assumptions as a minimum player kit.

**Why not adopt the upstream registry directly?** Grindless already has accepted typed
sustainability declarations derived with its canonical source binding. GT's material
property registry neither supplies those declarations nor makes the source-root audit
available inside an installed client. The generated view preserves that one authority
while applying the proven prepared-definition/presentation boundary.

**Ordinary and rated Matrix identity.** An `Evidence.ITEM` quest keeps its ordinary,
unrated resource identity; rating0 there means unspecified, not a physical F0 Matrix.
`Evidence.MATRIX` keeps the explicitly required rating. Never change those authorities or
invent rated declarations to satisfy a presentation test.
For a validated physical Matrix, retain its actual identity and prefer an exact rated
declaration. Only when it is the same item's real native default specification may its
ordinary declaration be used as presentation context. Derive that default through the
existing `ControlMatrixItem`/`ControlMatrixNbt` and architecture-first-frontier semantics;
do not flatten ratings or mutate NBT. Relay's untagged/default and explicit valid F1 can
share that default context; F2+ and malformed/incompatible ratings cannot inherit the T0
declaration by fallback. A non-default rating without its own declaration remains unknown.
This is a lookup distinction, not a new recipe, quest requirement or sustainability policy.

**Validation.** Exhaustively compare generated fields with the actual Result, including
null/unknown and handling cases; test fingerprint/drift/error behavior and shared
presentation without duplicating recipe amounts. Keep the new helper/model loader-free
where required and use native UI checks for the changed surfaces. A generated resource,
successful audit or source test is not real client, Survival, retained-world or Tier
acceptance. Existing ADR-0129/0140 gates remain in force.

---

## ADR-0144 — Ship Superflat-first playable tier releases with longitudinal save continuity

**Status.** Accepted owner-directed product and architecture policy, 2026-10-18.
Implementation of the first Playable Release Candidate remains in progress.

**Source.** The complete 549-line owner directive is preserved in the session Library.
Its SHA-256 is `7a5a04b1f2709536f1187ecc70958829fb7383bf95135820735f352b91a1e317`.
PA-0007 in the [reloadable plan](../.agent/work-plan.json) records the change in
requirements and sequencing. Private attachment and storage paths are not project inputs.

**Context.** `1389ebb` is a verified **T0 engineering candidate**, not the release on
which the owner will start the longitudinal world. Its minimal preparation ZIP, model
proofs, generated resources and repaired aliases do not establish a polished modpack,
an accessible Superflat bootstrap, an adequate sound pass or human acceptance.
The product must be playable during development, not only after T15.

**Product and release lifecycle.**

Grindless is a complete playable modpack centered on the Grindless mod. Every tier follows:
design -> implementation -> integration -> technical verification -> polish -> modpack
build -> Release Candidate -> real play -> feedback alongside the next tier.
The product is **Superflat-first and Overworld-compatible**.

Track `implemented`, `technically_verified`, `integrated`, `packaged`,
`visually_reviewed`, `human_accepted` and `release_ready` separately, with source,
artifact, observer and scope where applicable. A deterministic asset, successful
server or large assertion count does not supply another state's evidence.

The first T0 Playable Release Candidate must satisfy all of these conditions:

- T0 implementation and mandatory progression have deliberate, reasonable routes in
  the versioned official Superflat preset; normal Overworld play remains compatible.
- The complete selected pack, Grindless version, dependency versions, configurations,
  preset and required datapacks/resource packs/scripts are pinned and documented.
- Questbook and guidance agree with the canonical progression and preserve actual
  claim/reward authority.
- T0 receives a real sprite/texture/visual pass and a real sound pass, not merely
  inventory, alias validation or a change to fault-beep dispatch.
- Every included external asset has traceable origin, credited author where required,
  compatible redistribution/derivative rights, attribution and derived-file history.
- QoL does not unintentionally bypass resources, processing, energy, transport,
  storage, logistics, machines, quests, automation or progression restrictions.
- Relevant CI, installation/import, startup, content/config/preset checks and
  representative client, visual/audio and initial Superflat progression QA pass.
- The distributable is reproducible and installable, its version lock and provenance
  are sufficient, forward-compatibility strategy is explicit, and no known technical
  blocker invalidates ordinary T0 play.
- Remaining uncertainty is disclosed. No unobserved human result is marked passed.

**Acceptance and tier sequencing.** Delivering this RC starts human acceptance.
The owner's completed no-command playthrough is not a prerequisite to receiving it.
After the T0 RC is technically ready, safely published and the owner has actually
started its longitudinal save, T1 design/implementation may proceed alongside T0
feedback without waiting for the player to finish T0, unless a concrete technical
dependency prevents it. None of those conditions holds merely because `1389ebb`
exists. **Do not start T1 now.** Do not start the held functional-port work merely
because an existing model has a port-like texture.

This supersedes the conflicting *ordering* in ADR-0108 and the earlier active
ADR-0129/0140 acceptance checklists: full human acceptance is not the RC delivery
gate or the only way to permit the next tier after real play begins. It does not
erase unfinished observations, weaken technical tests or declare a tier human-accepted.
Live PR state, required checks/reviews and branch protection still govern integration.
The prior conditional merge authorization does not make the current draft merge-ready.

**Superflat contract.** Select and version an explicit recommended preset and its
installation/world-creation path. Trace every mandatory initial and recurring input,
including stone/geology, wood/biomass, water/lava, farming, mobs/drops, structures,
biomes, vanilla materials and relevant dimensions or external generation assumptions.
Normal terrain, optional exploration, lucky structures and an unrecorded starter kit
cannot conceal a missing official-flat route. The Factory World is not automatically
the starting Superflat preset. Rebind the environmental assumptions of the existing
conditional model; do not discard its unchanged execution/reserve mechanisms or
claim its old finite bill proves accessibility in a different world profile.
ADR-0131's mod-owned renewable permanence and ADR-0137's manual Matrix exit remain.

**Longitudinal save contract.** No new longitudinal world is started from the engineering
candidate by this policy. Once the owner begins the first playable release, that save
is the longitudinal reference. Upgrades normally preserve the same world across tiers.
Protect registry/block/item IDs, inventories, fluids, machine and multiblock state,
NBT/components, placed/carried objects, quest progress/claims/rewards and persistent
configuration. Internal refactoring alone does not justify incompatibility.
Before a genuinely necessary incompatible update, detect it, design an explicit
migration, test a preserved copy, keep the original and document the outcome. Never
claim demonstrated compatibility without the applicable evidence or alter tests to
hide a real incompatibility. Fresh fixtures are not an authentic save upgrade.

**Questbook and pack composition.** Review current Heracles and the real identity,
version/API support and experience of BetterQB before replacing anything. Preserve
Grindless's single server-side authority, current evidence, physical-book requirements
and unique rewards. A remembered item or native sticky completion must not become
an unintended claim. Retain/polish Heracles if the evidence makes it the better fit;
do not force a product name or introduce parallel progression truth.
Select a small reasoned set of auxiliary/QoL/performance/UI/accessibility mods.
For each material candidate record usefulness, overlap, maintenance, exact compatible
version, dependencies, licence/distribution method, performance, progression/bypass
impact and required configuration. Configure, integrate or exclude risky candidates.
Version coordinates remain in `gradle.properties`, with locked publisher references
in the existing pack metadata rather than floating `latest` selections.

**Art and sound.** Preserve good incumbent work and the industrial/futuristic identity.
Prioritize item/material/component/byproduct/Matrix sprites and textures, then machine
family/state readability, casings, real interfaces, models, fluid presentation and
feedback. Improve deterministic generation or adopt a maintainable source pipeline
where it actually improves craft; do not substitute file count for visual review.
Prefer believable mechanical, industrial, electrical, pneumatic, hydraulic or
environmental recordings to generic synth sounds. Review loops, relevant starts/stops,
liquids/actions/alerts, family distinction, volume, repetition and aggregate factory
audibility. Metadata and waveform checks are not listening acceptance. Only assets
with evidenced redistribution rights enter the release; record them in
[the notices](../THIRD_PARTY_NOTICES.md) before import.

**Distribution and storage.** Keep source, development environment, generated output,
mod jar, modpack, dependency downloads, caches, logs, QA saves and personal saves
distinct. The release includes required public configuration and content, never
credentials, user data, personal worlds, caches or irrelevant development files.
Use launcher-managed references when direct third-party jar redistribution is not
permitted. Produce a versioned GitHub release/prerelease only when the actual workflow
and verified artifact allow safe publication; do not relabel earlier preparation ZIPs.

The new work disk is an operational resource, not a guessed path in committed data.
Inspect ownership, sizes, persistent versus regenerable content, open users and
absolute-path dependencies before relocating anything. Prefer supported per-process
cache/output settings to breaking app-managed worktree identity. Any move needs a
reversible plan, verified copy/content identity, preserved originals until verification,
and checks that tooling/builds/workers use the intended storage. No broad cleanup,
foreign-cache deletion, world move or OS repartitioning follows implicitly.

**Execution and validation.** Astra owns product/architecture, integration and final
review; bounded workers implement, research and validate. Ordinary reversible steps
proceed autonomously. Stop for a genuinely irreversible/destructive operation,
unresolvable licence question or subjective product choice requiring the owner.
Reuse ADR-0140 proof within its assumptions; refresh only changed profile, data,
mechanism or integration boundaries. Record failures and skipped/unexecuted work
honestly. Keep compatibility-critical fixes distinct from polish and balance changes
while the next tier is developed.

**Rejected alternatives.** Waiting until T15 to package; relabeling `1389ebb` as polished;
requiring a completed human playthrough before delivering the first RC; substituting
ordinary Overworld resources for an incomplete flat route; changing worlds every tier;
rebuilding working T0 engines without an invalidated assumption; forcing BetterQB
without compatibility evidence; expanding QoL by mod count; importing unlicensed
sounds; and moving app-managed repositories by an unverified filesystem operation.

**Implementation.** The [T0 playable-release plan](../plan/process-t0-playable-release-1.md)
contains the concrete gaps, ownership, dependencies and verification boundaries.
Accepting this ADR starts that work; it does not claim any of those gaps is closed.

---

## ADR-0145 — Calibrate a small shared sound palette with development-only generation

**Status.** Accepted owner direction, 2026-10-18; initial generation and artistic
selection remain pending. The 434-line sound directive is preserved in Library with
SHA-256 `ef79a5ccbfdbfb7dfabd242019451352a50ba29f9b08efc29f7fed1de72250b2`.

**Decision.** Audit actual T0 callers before generation and record the minimal
[sound design manifest](assets/t0-sound-design.json). Prefer a few physical family
sounds and reusable interactions to one sound per machine or logical state. Ordinary
idle, missing-input, blocked and invalid-condition states remain silent; retain only
alerts with specific informational value. Preserve good existing behavior and current
registered identities until a concrete integration need justifies changing them.

ElevenLabs is an optional development tool, never a runtime or normal-build dependency.
The first calibration represents the Quern's stone/granular grinding mechanism using
at most three candidates with explicit prompts/parameters and no automatic rerolls.
Do not assume an electric or manual drive from an F0 energy rating. Perform technical
QC and present the candidates for the owner's initial artistic choice. Only after that
choice derive the sound style guide and expand the pass. API retries after interruption
must not spend credits twice: preserve request/outcome state and stop on ambiguous
delivery rather than resending automatically.

Keep credentials process-local and outside source, logs, manifests, candidates, build
outputs and release artifacts. Read the selected secret programmatically only when a
specifically authorized provider request needs it; never display its contents or copy it
into the project. Generation provenance records prompt, model, date, parameters, output
hashes, transformations, final use and applicable rights, not the credential or account.

The [official API](https://elevenlabs.io/docs/api-reference/text-to-sound-effects/convert)
returns binary audio. Use the reviewed request contract, not an invented format/seed or
web-playground batch. Verify generation-time entitlement and
[service-specific terms](https://elevenlabs.io/creative-terms) before release use.
Those terms distinguish incorporation into games from isolated audio/library
distribution; do not label outputs CC0, publish a standalone sound library or infer
rights from a generic paid-plan label. Internal audition is not release approval.

Current machine effects use randomized positional one-shots, not managed continuous
loops. A loopable source does not change that runtime behavior. Review duration,
stop latency, gain, distance, pitch, repetition and multiple nearby machines before
integration. Reuse existing APIs and conversion tools; add infrastructure only for an
observed behavior gap. Approved local assets must survive generation and build offline.

**Rejected alternatives.** Per-state/per-machine error files, endgame synth identity
for T0, unbounded generation, generic ambient tracks per block, overwriting generated
WAVs without a maintained source path, and a new general audio framework.

**Evidence boundary.** The initial manifest/request/example-grant survived an app
crash as drafts. A runnable pipeline, paid generation, codec/QC results and listening
approval are not established by preserving those files.

---

## ADR-0146 — Give items material volume and reuse block models for machine items

**Status.** Accepted owner clarification, 2026-10-18; implementation and visual
acceptance remain pending.

**Context.** The owner reviewed the source contact sheet and explicitly likes the
current placed machine/block identity. The remaining principal art gap is flat-looking
items, not a mandate to redesign all blocks.

**Decision.** Preserve the good block geometry, machine textures and family identity.
Refine items as Minecraft-compatible pixel art with implied physical volume and
material: meaningful silhouette, surfaces, depth, coherent lighting/shadows/highlights,
appropriate irregularity and readable internal detail. Default to consistent upper-left
lighting where the shape permits it. Do not merely add outlines, a flat fill and one
highlight, or increase detail until inventory-scale readability is lost.

Stone, ceramic, biomass, metal, residue and technological components must differ in
surface and shape as well as hue. Give Data Core, Calibrated Data Core and Relay Matrix
particular attention: preserve recognizable family/color semantics while making them
physical, interesting progression objects. A Relay Matrix should feel like the earned
T0 technology milestone. Preserve registry IDs, ratings, recipe identities and NBT.

For machine items, first inspect the actual Minecraft/Forge1.20.1 BlockItem model chain.
Prefer the same block model hierarchy and native item display transforms in inventory,
JEI and quests. A contact sheet of front textures is not evidence that the current
BlockItem is rendered flat. Fix only demonstrated model/transform gaps; do not maintain
fake isometric PNGs or a second independent visual authority when the real model can
be reused. Mature technical-mod implementations are source references, not copied art.

Maintain the generator/source-of-truth path so regeneration cannot erase approved
sprites. Review native-size items, enlarged pixels and representative game contexts
separately. Existing alias/determinism checks remain reusable structural evidence,
not the art pass. No image generation, new block design or completed visual review
is implied by accepting this direction.

---

## ADR-0147 — Transfer clean-session coordination to Sol 6.1 without replaying evidence

**Status.** Accepted owner direction, 2026-10-19; the successor session has not been
created or launched by the retiring coordinator.

**Decision.** The next genuinely clean, isolated session uses **Sol 6.1
(`gpt-6.1-sol`)** as leader and orchestrator instead of Astra. It inherits architecture,
product/Tier, sustainability, migration, integration and final-review authority, plus
the single-publisher responsibility after verifying the checkpoint and live refs.
This supersedes the leader-model choice in ADR-0134, not bounded delegation, use of
appropriate workers, proof reuse or preservation. No reasoning-effort override for
the new leader is inferred beyond the owner's stated model.

Preserve the current coherent work in Git before retirement. Transfer a compact
source-bound handoff and detailed startup prompt, not the entire conversation via
fork. The successor must use its own isolated worktree starting from the published
preservation commit; no reuse of the main checkout or writes to retired worktrees.
Do not create another PR for a context migration or silently reactivate old sessions,
event subscriptions, automation or Agent merge.

At this handoff no product worker/JVM remains active, the coordinator subscription is
paused and no session automation is attached. The current visible session list no
longer exposes most former children; an explicit app notification archived the C3
completion-safety child. Do not assume a missing child's raw workspace is accessible.
Use committed/captured evidence, and ask for restoration only if unavailable context
is genuinely required. Historical agent IDs are not a resumable worker pool.

The preservation includes two focused CI repairs: bounded Blueprint item observation
and UTC JSON string preservation in the sound tool. Their recorded targeted tests
remain separate from the new checkpoint's pending hosted CI. No game smoke, codec
replay or charged generation is required just to migrate context. The successor
names the new uncertainty before any expensive run and reuses valid proofs.

Retain real approval accurately: the owner approved all three Quern preview WAVs,
not the complete soundscape, a release or the art batch. The original three-request
grant is exhausted and its receipts are immutable. Existing tooling encodes the
historical `Astra` grant authority; before a different generation scope under Sol,
review that authorization seam explicitly rather than forging the old name, changing
spent receipts or resending approved sounds. No credential is transferred in Git or
chat; local secret references remain private operational context.

**Rejected alternatives.** Forking the long conversation, losing uncommitted worker
fixes, treating migration as acceptance, forcing history, copying build/world evidence
as a new proof, returning to an Astra-only leader requirement after the owner's change,
or automatically resuming every former worker.

**Handoff.** [Compact execution state](../.agent/sol-migration-handoff.md),
[task graph](../.agent/work-plan.json) and
[release plan](../plan/process-t0-playable-release-1.md).

---

## ADR-0148 — Integrate the approved Quern sound with one native client loop

**Status.** Accepted coordinator integration decision, 2026-10-19; implementation
and native listening remain pending.

**Context and prior art.** ADR-0145 calibration is complete: the owner approved all
three previews and A is the preserved first integration reference. Its exact source,
preview, derived Ogg and provenance hashes match the retained files. Existing
generation-time entitlement attestation and the reviewed
[ElevenCreative terms](https://elevenlabs.io/creative-terms), Sound Effects exhibit,
support incorporation into the game, not standalone sound-library distribution.
Do not reopen account/subscription verification, request more proof or preserve account
screenshots in Git. Runtime acceptance is still a separate unobserved property.

The preserved source investigation of native Minecraft1.20.1 audio and
[GT Modern AutoReleasedSound](https://github.com/GregTechCEu/GregTech-Modern/blob/aa41aba133f31dd3928bea816d6fd032cd1d90b6/src/main/java/com/gregtechceu/gtceu/api/sound/AutoReleasedSound.java)
already establishes the relevant lifecycle pattern. Use native mechanisms rather than
importing LGPL code or inventing a general audio framework. Random `animateTick`
one-shots do not provide authoritative onset/ownership for a continuous working bed.

**Decision.** Import only the existing candidate A Ogg as the `t0_grinding` family
after the provenance notice is committed. Preserve raw/preview/receipt bytes and their
historical hashes; never run generation or reset the exhausted grant. Use one
client-owned `AbstractTickableSoundInstance` per eligible running Quern position,
native `SoundManager`, BLOCKS category, mono and positional attenuation.

Discover eligible machines from bounded, already-loaded nearby client chunks during
the existing Architectury client tick lifecycle; do not force-load or scan every block
or the whole world. Use immutable position keys and one current-world owner, not a
static map retaining old worlds. Remove/stop ownership when state is no longer RUNNING,
the block/chunk disappears, the world/player changes, or the source leaves audible range.
Respect pause, reload/stopAll and cancelled/replaced sound-engine instances without
duplicate voices or an every-tick restart storm. Verify the actual selected API/binary
semantics where these details cannot be established from the recorded research.

Remove only the Quern heavy-hum bursts replaced by this loop, preserving its particles
and every unrelated consumer. Do not change pumps/extractors, gameplay, machine state
synchronization, save layouts or ordinary-fault silence. Introduce no per-error sounds,
mandatory variants or unique palette for each logical state.

**Scope and proof.** Bounded workers implement the client lifecycle, event/resource
binding and focused contracts. The coordinator alone integrates/publishes. Static asset
and lifecycle tests plus compilation cover what they can actually observe. Registration
changes need the representative server binding proof; the new client onset/stop/reload/
distance behavior and one/eight-machine mix need actual client qualification. The
headless environment cannot supply listening or native rendering by implication.
Do not rerun unchanged S1/S2/executor/content proofs or the paid calibration.

**Rejected alternatives.** Rechecking the owner's account, regenerating approved clips,
shipping all candidates by default, random callbacks as continuous-loop authority,
world-retaining caches, unbounded discovery and a new general audio subsystem.

---

## ADR-0149 — Add a restricted playable profile with explicit public overrides

**Status.** Accepted coordinator pack-composition decision,2026-10-19; configured
package, import/startup and actual client qualification remain pending.

**Decision.** Add an opt-in `playable` profile beside unchanged `expanded` and
`preparation` profiles. Select the existing pinned Architectury, JEI, Heracles and
ResourcefulLib, plus ModernFix, FerriteCore and client Embeddium: seven upstream files.
Reuse the exact publisher IDs/hashes already in the single manifest/metadata authority;
introduce no new or floating versions. Retain Heracles and its proved Grindless
claim authority instead of replacing a working backend for its name.

This is a deliberate small composition, not a claim of final interoperability:
runtime/platform, recipe discovery and quests are essential; the three existing
performance pins address memory/rendering pressure without adding progression items.
Use their normal defaults until an actual defect requires a version-specific setting.
Do not invent unverified config keys or ship personal options to demonstrate curation.
Record that decision and distinguish explicit public overrides from upstream-generated
defaults in provenance.

Do not include Jade in this first restricted selection: JEI/native machine feedback
cover essential discovery, and Jade's corrected noncommercial source terms should not
be silently treated as LGPL. Do not include Backpacks, Essentials or Chunks/Teams/Library:
their storage/processing, teleportation and loading behavior needs an intentional
progression design. These omissions apply only to the new profile; never remove their
content from an existing instance or save. Extra unpinned QoL is not required to close T0.

**Packaging.** Extend the existing Gradle pack resolver, provenance and paired shell/
PowerShell entry points rather than creating another builder or dependency list.
Keep the old selectors and archive identities working. Give the new profile a
distinct source-bound archive name/status; it is not a published RC merely because
it builds. Include required third-party notices with the modpack.

Support only explicitly enumerated public override files from a dedicated committed
pack source root. Reject unknown profiles, traversal/absolute paths, escaping symlinks,
duplicate destinations and missing/untracked inputs. No recursive copy of a launcher,
home, dev config, cache, log or world directory. Record each included public override's
relative destination/hash and preserve the exact input-profile selection in provenance.
An explicit empty override list is valid for the default-config composition; do not
claim that it supplies the still-unimplemented official preset or final public content.
The preset itself belongs to versioned mod worldgen resources, not a private save.

**Proof and limits.** Extend existing positive/negative metadata/selector and archive
whitelist checks, including default-profile preservation and invalid override inputs.
Serialize actual Gradle execution with the Quern worker. Source/pin/config selection,
structural packaging and client import/startup are different properties. The new pack
needs final integrated source, world-preset access, compatibility contract and actual
installation/client QA before it can become the playable RC.

**Rejected alternatives.** Mod-count expansion, changing historical profiles, treating
unconfigured teleportation/storage/loading as neutral QoL, forcing BetterQB, switching
publisher identity, guessing config keys and copying an arbitrary overrides directory.

---

## ADR-0150 — Superflat retains virtual deposits without staging the terrain

**Status.** Accepted owner clarification,2026-10-19. The owner explicitly requires
virtual underground rock/resource deposits in Superflat, extractable by multiblocks.

**Decision.** Superflat terrain must retain the same concept of chunk-local deposits
as ordinary worlds, without physically placing ore or rock blocks beneath its surface.
Use the existing seeded Resource Genesis foundation; terrain generation is not the
deposit authority. Preserve survey identity, richness, reserves, extraction history
and versioned derivation across updates. Adding basic resource families must not
silently remap existing surveyed mineral chunks.

The coordinator's proposed landscaped spawn, pond, exposed quarry, planted trees and
food/refuge provision were not selected and will not be implemented as a solution to
Superflat resource access. Ordinary mobs are not the reported problem. The actual
product gap is deliberate renewable acquisition of necessary materials, including
sand/clay and other missing basic inputs, through the game's production systems.

**Existing implementation boundary.** `VeinGenerator` derives the material/richness/
reserve from seed and chunk coordinates. The current extractors use that data and
`VeinData`, not physical ore scans; `PlanetCatalogue.veins` does not exclude flat
Overworld generation. This source evidence supports reuse, not a new native flat
execution or a claim that all basic rocks and multiblock routes are implemented.
Do not change that established mechanism solely to adopt this clarification.

The owner proposed a special T0 generating multiblock and a quest-reward currency/
shop for basic or intermediate materials. Preserve both as concrete product-design
leads, not delivered content or a forced quest-backend switch. Detailed machine/site/
cost/renewability and currency/catalogue/bootstrap rules remain to be settled against
the existing source, custody and quest authorities. The requirement for extraction
multiblocks does not open unrelated held T1+ ports/framework work.

**Proof.** Reuse seed/derivation, persistence, extraction and processing contracts
within their existing assumptions. Verify the actual flat-world binding and each new
source/resource behavior only when changed or unproved. A shop with finite quest
rewards is not itself proof of unlimited automated supply. No native test, new
terrain, machine, currency or manual playthrough is supplied by this decision.

---

## ADR-0151 — Keep coordination local and execute through the existing personal-PC SSH

**Status.** Accepted owner-directed operating model, 2026-10-19. This supersedes the
cloud-execution freeze as the current destination, not its preserved source/evidence.

**Decision.** Keep the Copilot conversation local. Use ordinary SSH to the personal
Windows PC for heavy builds, focused native checks, packaging and dedicated Prism
development deployment. GitHub is the synchronization boundary. Do not convert or bind
this conversation to a Remote Host, reactivate retired workers, or start from old main.
The received source is `70901eca2da386cd54fc769ba8a2a59be10eb76b`, including the original
Quern and restricted-pack histories.

Inspect first and preserve the working authentication. PC-login SSH, personal-PC Git SSH,
commit identity and GitHub API authentication are independent. The existing personal Git
SSH alias successfully reads the continuation branch even though the PC's `gh` token is
invalid. Use that Git alias for PC synchronization and the already verified personal
GitHub account for local publication; do not change accounts, remotes, keys or global
identity to repair an unrelated API login.

Use an isolated detached execution worktree and process-local Gradle home on the
high-capacity work volume. Preserve existing clones, caches, app worktrees and worlds.
Serialize JVM ownership while allowing source/tooling/documentation work in parallel.
Only committed source is built/deployed, with exact source/runtime and artifact hashes.
Personal addresses, machine paths and launcher/account configuration stay outside Git.

Deploy into a separately marked **engineering development instance**, not the existing
preparation instance or a longitudinal save. Reuse verified launcher component metadata
and hash-matched dependency files where available. Resolve missing selected dependencies
from the publisher CDN against the existing pins; introduce no new version authority.
Never clone saves, accounts or private configuration. Refuse unowned/running targets,
retain replaced owned jars outside the loadable mods directory, and record deployment
identity. Do not auto-launch Minecraft, log in, or claim human playtest results.

**Alternatives rejected.** Rebinding the conversation to Remote Host/cloud (contrary
to the explicit operating model); rebuilding on the smaller coordination machine;
reauthenticating `gh` to fix already-working Git SSH; overwriting the player's old
instance; treating a successful deployment as an RC or Superflat accessibility proof.

**Evidence boundary.** The completed original T0 audit/action plan target `0d79860`.
Read them, but use the later integrated T0 contract and ADR-0144/0150 where they supersede
that baseline. Do not rerun the audit or reopen already-proved mechanisms merely because
execution moved. The missing native sound binding, actual client rendering/listening,
flat-world entry, final configured RC and longitudinal compatibility retain distinct
proof obligations. See [the operating runbook](operations/ssh-prism-workflow.md).

---

## ADR-0152 — Run bounded autonomous T0 and creative work without human acceptance

**Status.** Accepted owner-directed unattended programme, 2026-10-19.

**Decision.** Continue local coordination and ordinary personal-PC SSH while the owner
is unavailable. Prioritize stable traceable build/deployment, verified T0 defects and
nonredundant regressions, then independent GREEN foundations. Run genuinely independent
technical, deterministic visual and SFX work in isolated child worktrees. The coordinator
owns architecture, integration, resource reservations, publication and final evidence.
Do not leave useful safe work idle because a different lane needs human judgment.

Keep T0 unaccepted/unfrozen and T1 inactive. GREEN preparation is not a new T1 frontier.
Preserve the current product/flat-world decisions: no guessed starter kit, staged terrain,
quest-shop economy, final balance or RED content. Reconcile the original audit's old
baseline with current evidence instead of redoing its completed review or inventing defects.
Human pacing, enjoyment, visual quality and listening remain explicit manual observations.

All PC work is reversible and project-scoped. Existing player instances/saves, unrelated
software, OS/security configuration and authentication remain untouched. Only the coordinator
updates the shared execution worktree/development instance; children request a separate
execution surface and a bounded JVM window before builds. Preserve every coherent contribution
in Git and maintain [the return handoff](../RETURN-HANDOFF.md).

**Creative scope.** Preserve the approved placed-machine identity and reviewed Core/Matrix
family. Prefer the existing deterministic generator for confidently weak encountered T0
sprites; no image-provider key is currently configured on the coordinator and no onboarding
question blocks local art work. Static/pixel review is not an observed in-game art pass.

The owner newly authorizes appropriate ElevenLabs work, not reuse of the exhausted original
three-request grant. First locate an already configured secure credential without printing,
copying, reconstructing or re-requesting it. Existing entitlement/account review stays closed.
This programme permits **at most two new provider requests for one selected T0 sound purpose**,
only after the coordinator records its purpose, prompts/parameters, new authorization/ledger
and rights/provenance boundary. No automatic retry after an ambiguous charge, no reset of old
receipts, no impersonation of their historical author, and no API dependency in normal builds.
If the credential or honest authorization tooling is unavailable, record that blocker and
continue source/offline validation or another GREEN lane. Do not expand the sound budget.

**Alternatives rejected.** Waiting for the absent owner on ordinary engineering choices;
silently deciding subjective/product questions; activating T1 because a jar deploys; exhaustive
duplicate native tests; unconstrained paid generation; competing shared-worktree publishers.

---

## ADR-0153 — Rebind generated recipes at atomic graph boundaries

**Status.** Accepted bounded A03 remediation, 2026-10-19. The regression is prepared;
red execution, implementation and its affected proofs are not credited by this decision.

**Current counterexample.** `MaterialRegistry.rebuild` publishes a new generated lookup,
but a loaded process machine can retain its previous cached recipe. Removing only an
ingot form withdraws a generated row while its raw input/crushed output still resolve,
so ingredient re-matching alone can still spend Flux and emit from a withdrawn definition.
Existing paid-work ownership and fluid receipts solve other boundaries, not membership.
An idle/miss machine also needs a wake path when a reload adds a usable recipe.

**Prior art and limits.** Read-only source review at GregTech-Modern
`aa41aba133f31dd3928bea816d6fd032cd1d90b6` inspected
[RecipeLogic](https://github.com/GregTechCEu/GregTech-Modern/blob/aa41aba133f31dd3928bea816d6fd032cd1d90b6/src/main/java/com/gregtechceu/gtceu/api/machine/trait/RecipeLogic.java),
[RecipeManagerMixin](https://github.com/GregTechCEu/GregTech-Modern/blob/aa41aba133f31dd3928bea816d6fd032cd1d90b6/src/main/java/com/gregtechceu/gtceu/core/mixins/RecipeManagerMixin.java)
and [GTRecipeType](https://github.com/GregTechCEu/GregTech-Modern/blob/aa41aba133f31dd3928bea816d6fd032cd1d90b6/src/main/java/com/gregtechceu/gtceu/api/recipe/GTRecipeType.java).
Those paths re-match cached recipes, rebuild a mutable index during synchronous reload,
and unsubscribe idle work pending content notifications. They do not establish the
graph-membership and reload-wakeup guarantees this generated, subscription-driven graph
requires. Targeted negative searches are not proof that every upstream path is absent.
No source is imported; LGPL-3.0 code remains reference-only. Vanilla caching was not
source-verified in that investigation and is not used as supporting proof.

**Decision.** Preserve the existing graph and scheduler. Publish the complete immutable
recipe list and indexes together through the existing graph holder, with atomic/volatile
visibility. Use one stable typed identity from that published snapshot, not an independently
published epoch and graph. The identity is unchanged between rebuilds and fresh for a new
publication, including equivalent/empty contents; reading it allocates nothing.

Cached selection checks that identity before reuse. On change, discard only the transient
selection and resolve against the current graph. Same `WorkRecipe` keeps paid `Progress`;
different recipe IDs retain the existing reset policy. A withdrawn definition may wait
with its paid work, but cannot consume another input/Flux or emit. Already committed
output/receipt ownership is not discarded.

Add one bounded pre-subscription hook to the already invoked base server ticker.
Only process machines compare the identity, O(1), and refresh subscriptions once when
it changes. The existing ticker runs even with empty subscription lists; explicit
logistics hold stays held, and release revalidates before work. No world scan, global
machine registry, forced chunk load, polling recipe lookup every tick or cross-thread
tag callback may touch machines. Preserve the existing once-per-world-tick work guard.

No NBT keys, paid-work identity, recipe balance, Matrix gate or player permission changes.
A reload/unload reconstructs transient selection from current data; no saved epoch is trusted.

**Validation.** First execute the real tag-binding/material/graph counterexample on the
frozen pre-fix source. Then cover withdrawn active selection with still-resolvable I/O,
same-identity paid progress, reconstruction/current provider, a previously idle miss
gaining a row without neighbor mutation, and no double work/Flux on refresh. Reuse unaffected
inventory/fluid/conservation proofs and run the affected selected checks/build. The changed
BE/ticker seam needs representative native evidence, not another run for every material.

**Alternatives rejected.** Guard only active `recipe()` calls (leaves idle misses asleep);
direct tag-event callbacks (unsafe client/server ownership); scan every loaded machine on
reload; keep a separate mutable generation counter; clear paid progress on every rebuild;
copy the upstream mutable lookup/cache behavior or replace the whole execution engine.

---

## ADR-0154 — Stage one disabled optional pack for reload smokes

**Status.** Accepted narrow validation-tool extension, 2026-10-19. Native qualification
is still pending.

**Need.** ADR-0153's Java tests prove cache/paid-work behavior without a real Level.
Its distinct native obligation is an already loaded, charged, unsubscribed process
machine waking on a genuine tag/graph rebuild without another inventory/neighbor wake.
The existing smoke harness stages only one initially enabled pack. A base pack can
provide raw/crushed tags while one initially disabled ingot pack supplies the missing
form only when the scenario enables it. An always-enabled fixture would be a false proxy.

**Decision.** Extend the existing paired PS/SH harness, not create another server runner.
Support at most one `SMOKE_OPTIONAL_DATAPACK`, copied using the same validated path/
long-path rules as the principal pack. A selected scenario may discover its one explicitly
named optional directory. Keep it out of the enabled selection and explicitly disabled
at fresh-world creation using verified1.20.1 pack-selection semantics; the commands then
enable that exact pack and exercise the real reload. If those semantics are unverified,
fail the setup rather than assuming the pack starts disabled.

No optional pack means byte/behavior-compatible existing defaults. Reject missing,
ambiguous or colliding selected pack paths; reset/restore per-scenario environment.
The reload case requires a fresh diagnostic world, not leakage into an ordinary shared
batch. Inspect both batch twins and either route this case through isolation or reject/
exclude it explicitly. Do not change other scenarios or add a bespoke CI step; add
the scenario-list row only after actual native qualification.

**Oracle.** Before reload, demonstrate a stable charged machine with valid I/O but no
work/output. Then enable the ingot form without mutating its inventory/energy/neighbor.
One normal B1 batch must consume one gravel, emit two sand and debit its actual3840FU
across machine/source buffers once. A full receiver must not receive a positive transfer/
callback before work begins. Use the actual production buffer/NBT/transfer contract;
do not inject Progress or infer wakeup from placement or a final item alone.

**Limits.** Test-only tags/world/source, no Prism or real saves. Preserve existing
handshake/budgets/cleanup and paired platform parity. This is a specific reload fixture,
not a generic datapack manager, content rewrite or human survival evidence.

---

## ADR-0155 — Retain a diagnostic smoke world only by explicit opt-in

**Status.** Accepted bounded validation-tool extension, 2026-10-19. Source
implementation `444409f` is independently reviewed with37 PS/43 Windows Git-MSYS
contracts; native qualification remains pending.

**Need.** A01's extractor regression must check the real, cleanly saved
`VeinData` extraction counts, not infer them from exported items. Both existing
smoke twins remove their diagnostic world on exit. The authored command that
reads `grindless_veins.dat` after the harness returns therefore cannot work.
The production recovery review found no other high-confidence blocker, but this
missing accounting evidence still prevents qualification.

**Decision.** Add the same minimal `SMOKE_KEEP_WORLD` opt-in to both existing
smoke-boot twins. Absent or `0` retains their normal cleanup; `1` preserves only
the existing isolated diagnostic world and reports that retention explicitly.
Reject any other value before Gradle or filesystem mutation. All ordinary path,
linked-ancestor, overlap, EULA, budget, assertion and shutdown guards still apply.
Do not add an arbitrary post-stop command, another runner or a general export API.

The caller must require successful scenario assertions and natural successful
server shutdown before using the retained data as evidence. Retention itself is
neither a PASS nor proof of a clean save. Run the source-bound SavedData oracle
before another boot resets the world, preserve its receipt, then explicitly clean
up that one owned diagnostic world. The caller scopes and restores the opt-in;
the harness must not change the caller's environment. Normal CI and unrelated
smokes do not opt in.

**Alternatives rejected.** Copying the file from another process before cleanup
is a race with shutdown and deletion. Reading live data does not prove the
clean-stop persistence boundary. Item counts alone cannot prove vein accounting.
An arbitrary shell callback or replacement server harness adds execution and
quoting boundaries that this one evidence file does not need.

**Limits.** No Prism/player save, migration, crash-atomicity, actual restart or
longitudinal-play claim. Keep the canonical server heap and all existing timeouts.
Offline contracts establish default/opt-in/invalid-value cleanup behavior and
environment preservation; only the real native run can qualify saved accounting.

---

## ADR-0156 — Disclose blockers and separate creative capability from product acceptance

**Status.** Accepted owner-directed follow-up, 2026-10-20; PA-0014. The previous
qualified559 deployment is preserved, not relabelled as a finished T0 campaign.
ADR-0158 subsequently replaces the initial micro-approval/cap execution model
with broader end-to-end authority; the historical budgets below are not current
permanent project restrictions.

**Need.** The engineering handoff did not prominently disclose that new SFX
generation had not occurred. Documentation alone is insufficient disclosure.
The owner supplies the existing audio credential location, requires visible
blocker reporting and requests parallel visual-capability discovery, coherent
SFX families, objective client preparation and a minimal fresh-Superflat proposal.

**Decision.** Every wave completion and handback explicitly reports COMPLETED,
BLOCKED, NOT EXECUTED, NEEDS HUMAN VALIDATION, NEEDS MY DECISION, NEEDS CREDENTIAL /
EXTERNAL RESOURCE, COST / BILLING RISK and DEPLOYED STATE, both in chat and durable
handoff. A zero-entry category says None. Partial engineering completion does not
erase unexecuted creative/product work or claim manual acceptance.

Use four isolated workstreams with the coordinator owning product, integration,
publication and the single PC execution lease. Image discovery must distinguish
Copilot usage, GitHub Models image input versus actual image output, actual account
access, included/free usage, separate billing and local feasibility. No paid
activation, new subscription, broad secret search or costly model download is
authorized. At most one small visual proof may use a verified no-cost or already
authorized route without replacing the accepted asset.

For SFX, preserve the approved Quern resource and original spent receipts.
Historical Quern/three and prior-wave/two request limits apply to their runs, not
all future audio. This wave initially budgets at most two candidates per selected
family, at most two justified families/four provider requests in total, beginning
with Clay Mixer wet work. This is a coordinator-selected finite execution cap,
not a new permanent project limit. Before any generation, review the actual
machine audit, prompts, parameters, rights, new purpose-specific authorization
and reservation-before-HTTP ledger. Reuse existing sounds/families where adequate.
Ambiguous delivery consumes its reservation and is not retried automatically.
No new account/entitlement review is needed.

The supplied external credential is verified by one body-suppressed authenticated
read, HTTP200, with no generation or secret output/copy. It remains at the existing
private location; values and access paths never enter Git, manifests or logs.
Objective audio processing, format/path/registration/lifecycle validation and
technical selection may proceed; perceptual quality stays pending owner listening.

Trace fresh-Superflat entry and all consumed dependencies against the real
catalogues. Present viable alternatives and a minimal recommendation before any
material progression/preset/economy choice. Diagnostic stock is not survival.
Automate objective client/resource checks and prepare a separate review surface
without touching player worlds. No T0 acceptance/freeze before the owner's manual
campaign; no T1 activation by implication.

**Rejected alternatives.** Treat a missing credential as a universal absence
without naming the searched scope; reset an old spend ledger; turn Copilot credits
into assumed image credits; generate one sound for every trivial event; stall
independent branches on a subjective choice; silently implement a starter kit,
world landscape or quest shop to make a diagnostic route appear playable.

---

## ADR-0157 — Prefer the existing local ComfyUI as a supervised asset backend

**Status.** Accepted owner-directed execution route, 2026-10-20. The owner identifies
an existing personal-PC ComfyUI installation and authorizes necessary reversible
configuration, extensions and appropriately licensed additions. Qualification and
the first end-to-end proof are still pending.

**Decision.** Use a specialized nested asset worker for this local route while
external-provider research, audio, CI regressions, Superflat proposals and client
preparation continue independently. Inspect the real installation, launch/runtime,
models/checkpoints/VAEs/LoRAs/conditioning, custom nodes, workflows, input/output
locations, hardware and API before changing anything. Hardware alone never proves
that a model/backend runs. Keep host paths in private operational configuration.

Never reboot the PC. Preserve existing workflows, models, outputs and queued user
jobs; do not restructure the installation unnecessarily. Prefer a logically isolated
Grindless profile/workflow/output prefix and existing compatible resources. Do not
expose ComfyUI to the Internet, weaken firewall/security or create new cloud billing.
A necessary node/tool/model addition requires a concrete role, pinned provenance,
licence check, bounded disk/compute estimate and rollback. Avoid duplicate or multiple
large model families. Serialize heavy GPU/RAM execution against other PC leases
when actual headroom requires it, not all metadata/configuration work.

The public project contract is structured asset specification -> versioned ComfyUI
workflow/seed/model settings -> retained source candidate -> deterministic crop/
background/palette/pixel adaptation -> Minecraft technical validation -> reviewed
integration -> owned stopped Prism deployment -> human in-game visual review.
ComfyUI supplies source material, not Minecraft correctness or artistic acceptance.
Keep generation and deterministic transformation identities separate; a fixed seed
does not promise cross-driver bitwise output. Never mistake a cache hit for a second
independent inference.

Derive shared palette, silhouette, material, mechanical and tier/family rules from
existing Grindless design/assets. Prefer reusable item/material, machine concept/
texture, state variant, book and UI/icon templates rather than unrelated graphs.
Begin with one justified weak T0 asset, at most two source candidates unless a
concrete defect requires a reviewed adjustment. Preserve its predecessor, integrate
only a technically valid scoped candidate and mark NEEDS HUMAN VISUAL VALIDATION.
No mass replacement or T0 acceptance/freeze follows from this proof.

Record model and workflow versions/hashes, original structured prompt/specification,
seed/settings, own source output, deterministic transforms and resulting asset.
Do not copy unrelated personal workflows/prompts into Git. External image options
remain comparative research and require explicit paid authorization before use.

**Reporting.** In addition to ADR-0156's eight categories, the useful ComfyUI
checkpoint explicitly reports COMFYUI ENVIRONMENT FOUND, REUSED COMPONENTS,
INSTALLED/CHANGED COMPONENTS, MODEL/WORKFLOW SELECTED, AUTOMATION METHOD,
POC ASSETS GENERATED, ASSETS INTEGRATED, NEEDS HUMAN VISUAL VALIDATION, BLOCKERS,
DISK/COMPUTE IMPACT, LICENSING/PROVENANCE NOTES and RECOMMENDED NEXT ASSET WAVE.
Do not bury failed nodes, missing models, unsupported execution or absent images
inside the decision document.

---

## ADR-0158 — Delegate end-to-end outcomes with proportional validation

**Status.** Accepted owner correction, 2026-10-20. Excessively narrow assignments
and approval stops delayed real generation despite available access and subscriptions.

**Decision.** Give each existing worker a broad, coherent outcome through ordinary
implementation, generation, technical selection, processing, integration and tests.
Routine iteration, review-marker/grant preparation under delegated authority and
repair of concrete defects do not require a new coordinator permission at each step.
Prefer a real bounded execution and relevant checks over another custom carrier,
full replay or preparation-only handoff. Use independent review at meaningful risk
boundaries, not as an automatic gate for every file or invocation.

The sound owner may now mark the delivered real wet-work contract/manifest reviewed,
commit it, issue its fresh private source-bound grant and execute the accepted two
Mixer requests immediately. Small justified iterations/families may use up to12
SFX requests in this wave, without exhausting that ceiling merely for activity.
The Comfy owner may finish the selected Multitool pipeline and use up to8 additional
local candidates only if useful. Each run still has a finite recorded budget; old
spent receipts are never reset and uncertain delivery is not blindly resubmitted.
These are operating allowances for delivery, not permanent project limits.

Keep meaningful safeguards: secrets never exposed/copied, exact source/output
provenance, no PC reboot/public Comfy exposure or player-world damage, and honest
human visual/listening/campaign gates. Ordinary transformations, codec QC and
source preservation are worker-owned. Coordinator publication and shared final
build/deploy remain serialized; actual PC resource handoffs are scheduling, not
another design-approval ceremony. Independent lightweight work continues.

Material progression alternatives, new external paid-image billing and genuine
security/account/OS changes still need the appropriate owner decision. Existing
subscriptions and the expressly authorized local route are for use, not perpetual
access/entitlement rediscovery. Record actual costs and material failures visibly
under ADR-0156. Neither this acceleration nor a technical pass accepts/freezes T0.

---

## ADR-0159 — Start T0 with one free generous starter quest

**Status.** Accepted owner decision, 2026-10-20. Implement directly; the earlier
Superflat alternatives are historical proposal, not an outstanding choice.

**Decision.** The first quest is immediately available and free, providing a
generous finite construction/food/basic-material bridge. Do not require paper,
a pre-existing tool, currency, a village or diagnostic commands to reach it.
Use the existing server-authoritative quest claim/reward/persistence mechanisms,
with stable old quest IDs and one claim per player/world across rejoin/respawn.
Make the existing quest book available from initial entry; do not create a new
shop, world generator, primitive production framework or second reward ledger.

The coordinator's ordinary allocation is 128 cobblestone, 64 oak logs, 64 bread,
64 brick items, 64 clay balls, 64 sand, 32 charcoal, 16 iron ingots and 2 filled water
buckets, delivered as legal stacks. This intentionally generous finite bootstrap
is not a permanent resource farm. No finished machines, insulators, Data Cores,
Calibrated Cores, Matrices or advanced components are given.

Expose the complete reward and free/ready state through the legacy book and
Heracles projection. Preserve explicit player claim, safe overflow and old save/
quest progress; never reset claims or silently grant the whole kit to existing
inventories. Normal Superflat works without selected-village/terrain/loot reliance.
Human first-quest interaction, pacing and the campaign remain validation, not
another initial-kit design approval or a reason to defer implementation.

**Delivery and capacity.** Reuse vanilla `SimpleContainer` stacking over copies
of the 36 main inventory slots. Resolve and stage every legal reward before
advancing the same claim ledger, then commit the prepared inventory once on the
server thread. A full inventory rejects the whole claim with a localized message;
nothing is dropped or partially granted. This intentionally also defers older
single-stack quests when full, replacing their previous remainder-drop behavior.
The utility book alone is recoverable on login/respawn while the starter claim
is still unused; an initially full inventory retries book delivery every 20 ticks.
No new persistence schema, second ledger or Heracles-owned reward is introduced.

---

## ADR-0160 — Merge the testing-ready T0 candidate and close superseded PRs

**Status.** Accepted owner instruction, 2026-10-20.

**Decision.** Once the integrated T0 candidate is technically ready for testing,
merge its consolidated pull request into `main` and clean up superseded open PRs.
Readiness includes the implemented free starter path, applicable executable
contracts, common/Forge build, source-bound package/registration verification,
the same verified development deployment, and applicable live GitHub checks.
Do not bypass required checks, reviews, branch protection or an unresolved defect.

Human appearance/listening, real quest interaction, no-command campaign and
longitudinal observations are the testing that follows this delivery, not fictional
pre-merge passes. This instruction does not accept/freeze T0, declare the restricted
engineering pack a release candidate, or activate T1.

Preserve the complete checkpoint history with an allowed merge commit. Before
closing an old PR, re-read its live state/head and establish that the exact head
is included in merged `main`; preserve and report any unique or changed work.
Keep existing branches/worktrees and their evidence. Closing superseded PRs is
not permission to delete unknown work, rewrite owned branches or modify repository
protection. The inspected older PR chains have no native stack metadata; do not
invent or dissolve a stack to clean them up.

---

## ADR-0161 — Prepare the T0 experience before longitudinal human play

**Status.** Accepted owner master prompt, 2026-10-20.

**Decision.** The next work is the explicit T0 experience programme in
[the executable plan](../plan/feature-t0-experience-1.md), not another claim that
engineering testing readiness already satisfies the longitudinal gate. It includes
quest-led instruction, harmless Guide migration, recovery barter, balance evidence,
usable tanks, functional interfaces for the two actual T0 structures, optional
inspection, physical audio and reusable visual production.

This scope explicitly opens bounded T0 ports/shop work previously held in the
route map. It does not open unrestricted T1 production or replace the physical
five-F1-Matrix exit. Preserve old IDs/claims/contents/default builds and provenance.
Keep independent feature ownership, one new integration surface and one shared
mutating PC execution owner. Do not reuse retired workers as an unbounded pool.

The new sound purpose permits at most8 new POSTs across at most4 justified
families, initially6 requests/3 families. The new local visual purpose permits
at most6 candidates/3 demonstrated weak targets. These are ceilings, not targets
or resets of historical ledgers. Existing selected Quern/Mixer/Multitool sources
remain preserved; paid images, account changes and model downloads are not selected.

Required client launch/world/interaction/save smoke is distinct from headless
server/fixture proof. The status `TECHNICALLY PREPARED FOR LONGITUDINAL HUMAN
VALIDATION` requires the owner's complete gate, not just successful compilation.
No normal longitudinal campaign, final T0/RC release/tag, acceptance/freeze or
unrestricted T1 development follows from this authorization. A coherent bounded
slice may be preserved if blocked, but required gate items are not silently deferred.

---

## ADR-0162 — Quests own T0 teaching and the per-player F1 transition

**Status.** Accepted implementation direction under ADR-0161.

**Decision.** Extend the canonical quest catalogue with typed reward-free teaching
lessons and concise machine/process/placement/troubleshooting content. Preserve
all26 old IDs and rewards; old claims remain in the same player/Overworld ledger.
An explicit lesson can record acknowledgment without pretending item possession
proves successful operation or inventing material reward fallbacks.

New `relay` claims require five valid physical Relay Matrices rated F1 through
the existing rated-item evidence. The claim unlocks a distinct actual T1 chapter
in both native and Heracles presentation, not an unrelated menu/item or global
machine permission. Historical relay claims remain authoritative: their past
rating evidence was not stored and cannot honestly be reconstructed or reset.
Keep later prototype IDs/progress, but never label them completed T0 content.

Retain `field_guide` as a registered harmless deprecated compatibility item whose
use routes to current quest instruction. Remove its normal recipe/required role,
not the saved ID. Process Atlas remains the authoritative recipe viewer.
Recover only the utility Quest Book on normal entry/respawn when absent, including
after the starter was used; never duplicate the valuable starter or other claims.

Both quest renderers request the one shop network entry. The shop service owns
server validation and barter; do not create a second hook/packet/economy ledger.
Shared recipe details must derive from canonical definitions; text is instruction,
not another quantity catalogue.

---

## ADR-0163 — Use physical barter and local evidence before economic rebalancing

**Status.** Accepted reversible direction under ADR-0161.

**Decision.** The recovery shop uses direct in-kind exchanges of existing items.
Do not add a currency item, mint, wallet, rare-structure dependency or mandatory
economy mod. This meets the requested production-surplus/recovery role without
the fundamentally new currency that would require an owner decision.

One machine-readable offer catalogue owns input/output quantities, prerequisite
claims, role and risk metadata. Server-side native inventory staging must prove
the complete input withdrawal and complete output capacity before a single commit.
Reject stale/replayed menu actions; never debit partially, drop valuable remainder
or trust a client-selected price. Include process conversions in cycle/arbitrage
checks rather than considering only a buy/sell pair.

Emergency/convenience/terrain/food/container/fuel offers are optional. No shop
offer sells a machine, Core, Calibrated Core, Relay Matrix or advanced critical
component. The production route must work without the shop. Preserve the generous
existing starter until measured evidence justifies a separate balance change.

Export live recipe bills, sufficient construction witness and assumptions
separately; do not call a sufficient witness a globally minimal route or turn
configured ticks into observed playtime. Opt-in local developer telemetry records
only aggregate causal process/manual/claim/shop/recovery/state events and relative
times. It is off by default and records no names, UUIDs, coordinates, credentials
or external analytics. Manual accepted work and committed processes count, not
attempts or repeated polling.

---

## ADR-0164 — Extend native tank custody and bounded controller-backed T0 ports

**Status.** Accepted under the owner's explicit ADR-0161 infrastructure scope.

**Decision.** Extend the existing Basic Tank, `FluidEndpoint` and
`NativeFluidStore` authority rather than replace their16B/`Fluid` save contract.
Stage real container handlers on copies and settle the whole native fluid/item
exchange with owner-controlled compensation. Preserve fluid custody through
breaking/rotation/relocation; reject hot/unrepresentable/tagged/invalid/stale
Forge operations rather than silently treating them as cold valid input.

Implement only justified item/fluid roles in the two existing T0 shapes:
Firing candidate cells(1,0,0)/(1,1,0) accept at most one item input/output each
while at least five ordinary bricks remain. Trough channel(0,0,1) accepts one
item/fluid input variant; screen(0,0,2) accepts one item/fluid output variant.
Keep original geometry, rotation and open-air requirements, with exposed relative
sides and understandable role/fault markings. Default old builds remain valid
and direct controller interfaces remain available.

Ports own zero independent contents. Every operation validates loaded world,
current formed shape, compatible role/face and unique controller block-entity
identity; captured accesses to an old controller/store must fail after replacement
at the same position. Rebind on each reform, invalidate on every retirement path,
and never force-load chunks or create cross-dimensional links.

Output quota is a real visibility/withdrawal bound, not just a displayed amount.
Native reservations must respect that bound across aliases and recheck it at
commit. Reuse the same backing store identity; do not manufacture a second store
or copied tank to hide reserved input stock. Limit any shared API extension to
this boundary and rerun its affected transaction/rollback proofs.

[Pinned mature-source research](design/t0-external-references.md) supports
role/count predicates, zero-storage primitive proxies, relative sides and identity
retirement. Their generic kernels solve variable-scale systems unnecessary for
seven known cells/two known parts; no GT/Mekanism implementation is copied.
Controller ownership is structural, not a new player UUID/security system.

---

## ADR-0165 — Provide original optional inspection without a new pack dependency

**Status.** Accepted under ADR-0161; Jade pack inclusion remains deferred.

**Decision.** Add immutable bounded loaded-target inspection snapshots that read
the real current process/progress/energy/fluid/environment/structure authorities.
Use descriptive display reasons without reordering serialized machine-state
enums. Show unknown/absent when a native fact is unavailable; do not invent a
specific starvation cause from an ambiguous status.

Present those snapshots through a small original optional native aimed-machine
overlay using existing networking/client-rendering patterns. Keep client-only
render classes out of dedicated-server initialization. Validate target, distance,
rate and payload bounds on the server; no world scans, forced chunks, retained
world references or personal logging. Text/shape must accompany color cues.
Offer a clear client control and avoid duplicate overlays when another provider
is present.

Exact Jade11.13.3 API research establishes a compatibility target, not distribution
permission. Its pinned CC-BY-NC-SA licence and ADR-0149 omission must not become
an unreviewed mandatory dependency or a silently changed playable pack. Deliver
the original native equivalent first; no Jade jar/API class/implementation is
copied or shaded. Owner-approved integration or alternative overlay selection can
extend the neutral snapshot later without replacing machine execution.

---

## ADR-0166 — Retire synthesized distribution with physical families and intentional silence

**Status.** Accepted T0 experience strategy; implementation/runtime/client
qualification and human listening remain separate.

**Decision.** Remove the five legacy synthesized Oggs from distribution and their
periodic hum/click/chirp fallback callers. Preserve their seven existing registered
event identities: unused hums are deliberately silent, legitimate interaction
events resolve to native physical game sounds. Do not copy vanilla sound assets.
Keep Quern A and Mixer A byte-identical; add only justified physical families.

The new finite six-request purpose produces six HTTP200 sources without retries
or ambiguity. Water B and extraction B pass technical decoding/normalization and
are selected. Neither generated fire candidate is suitable; preserve the actual
failed adaptation and use native furnace crackle rather than reroll for sunk cost.
About720 credits is an estimate, not an account charge or guaranteed USD quote.
The spent6/6 ledger and rejected sources remain immutable/private as appropriate.

Use one existing tracker and one loaded-nearby-chunk scan, with at most eight
nearest managed sources and short process-appropriate new-family ranges/gains.
Retain identity/kind/family separation, clean retirement, pause/mute/reload/
dimension/removal handling and bounded observation of late-start sources.
No independent second loop manager or unbounded queue is introduced.

Biology, Bench, calibration, storage, structure parts and ordinary logistics can
be intentionally quiet; a material or block does not require a permanent sound.
New ports are six structural silence rows, not aliases of their controller's
working family. Ten event definitions/aliases must resolve or explicitly represent
unused silence, with no synthesized runtime resurrection by normal conversion.

Source/codec/lifecycle and one/eight/mixed-workshop listening are different proofs.
No artistic approval, audible native lifecycle, fatigue or final mix follows from
technical selection. Preserve the raw generation/audition worktree until its
private evidence has been retained; never archive it merely because source is clean.

---

## ADR-0167 — Make state and port art readable without color or normal-build generation services

**Status.** Accepted deterministic visual direction under ADR-0161.

**Decision.** Preserve the incumbent original T0 material/geometry world. Fix the
observed shared blocked/out-of-band mask by giving the five machine states distinct
shape marks, not just different colors. Apply the justified change to62 T0 fronts;
retain the other519 baseline PNGs, protected nine material/core sprites, three
books and canonical Multitool. Four original role overlays serve six port variants
without a duplicated texture set per orientation/casing.

A rear surface only two model units high cannot carry an entire16px role glyph
legibly. Retain it as a secondary cue and add the same role texture on the existing
open bed's readable top area before the screen bars. The approved planar bounds/
UV crop preserve base/collision geometry and do not open an automation face.
Human in-world distance/light/readability still requires real client review.

Comfy source generation remains optional development-only work. Extend reusable
item/face/family/palette/mask/contact-sheet presets and their reservation/model/spec
bindings, but do not spend diffusion to fix a deterministic mark defect. This
wave needs no new inference/model download/paid image service. The old Multitool
2/2 generation record remains untouched.

Normal runtime artwork generation stays JDK-only and deterministic; approved
source bytes are validated/copied before output. Python/Pillow tools inventory
and process candidates, not the normal mod build. Contact sheets explicitly show
actual source pixels/native1x/nearest8x/light-dark, not Minecraft screenshots.
Refresh the manifest only after composed models exist; do not relabel an old
19-block projection as the new25-block/port model result.

---

## ADR-0168 — Develop T1 in parallel with owner T0 testing without changing the dev game

**Status.** Accepted direct owner instruction, 2026-10-21.

**Decision.** The owner explicitly requests T1 development and documentation of
reusable systems/production lines while testing the current T0 dev build. Open
that development now through [the first-factory plan](../plan/feature-t1-first-factory-1.md).
This later instruction supersedes the old T1-development hold in ADR-0144 and
the T0 experience programme only for concurrent development; do not infer a
completed graphical smoke, longitudinal start, T0 acceptance/freeze or RC release.

Use isolated branches depending on the actual current T0 experience source
a64d44f/PR56, not older main. Preserve the pending T0 review and all source-bound
proofs. Leave the installed F298988d/95bf47c4/3e5ad1fb candidate, launcher,
player worlds/options and running game untouched. No T1 deployment, Prism restart,
release/tag or main merge is authorized by "begin development".

Build-only SSH work may proceed on a separate explicit execution root if fresh
headroom and shared-cache ownership permit, with gameplay taking priority.
A legitimate player-client JVM is not permission to kill it or a reason to
pretend an unknown build is safe. No competing agent Gradle process or source
switch under a running build is allowed. If capacity is insufficient, continue
independent source/docs and use applicable hosted proof rather than lowering guards.

Keep the T0 graphical/human feedback lane distinct from T1 code progress.
T0 fixes remain integration dependencies when owner feedback changes a shared
assumption; do not re-label a T1 test as a retroactive T0 player pass.

---

## ADR-0169 — Make the first T1 factory a live source contract, not another execution engine

**Status.** Accepted first-increment direction under ADR-0168.

**Decision.** T1 Voltaic is the existing first-factory chain made explicit and
usable: F1 thermal power/distribution; milling/reduction and renewable glass;
Press/dies/coils/casings; Assembler reproduction of the five-F1 batch; and the
physical rated F2 Relay Matrix. Select actual craft/process/fuel authorities by
stable identity and dependency, supplying no duplicate quantities or new global
machine permission.

Implement one loader-neutral line contract consumed by both runtime quest
teaching and a source-derived exporter/checks. Separate mandatory lines, optional
branches and the exit. Finite extraction, optional logistics/storage, Kiln and
Factory World cannot become an unmentioned renewable supply guarantee or an
unrelated required F2 gate. Preserve their existing IDs/claims and useful optional
roles. New first-factory teaching replaces generic preview text only for the
implemented selected routes, not every registered later-tier prototype.

The physical F2 recipe remains `assemble/relay_matrix_f2` in the real Assembler;
its current F1 Matrix/casing/glass/Flux Cell inputs are read from live definitions.
The final `industrial_matrix` claim still requires valid physical F2 evidence,
but no longer requires optional Factory World access. Preserve old claim/reward
records and the exact T0/F1 chapter boundary.

Recurring inputs inherit the mod-owned T0 base or retain explicit honest
dependency findings. Do not author a T1 renewable label solely because a recipe
is repeatable or a finite vein exists. Reuse `SustainabilityAudit`,
`AuthorityAdapters`, construction reserves and source projections where applicable;
an early source contract is not a claim of complete T1 closure or optimal pacing.

Document the shared machine/process/condition/power, transaction/persistence,
capability/interface, quest/guidance, inspection, telemetry and asset/proof
mechanisms with actual authority/invariant/extension seams. The project's
already-proven infrastructure is the solved-problem-first choice; there is no
new need for a generic processor, transfer rollback engine, multiblock kernel,
quest ledger, currency or content-copying dependency in this first increment.

---

## ADR-0170 — Develop the next tier alongside longitudinal play and prioritize player blockers

**Status.** Accepted direct owner instruction, 2026-10-21.

**Context.** The owner clarifies that the installed T0 game is intended to be
longitudinal, not an indefinitely disposable test. After the source-bound
[readiness investigation](testing/t0-longitudinal-readiness.md), the owner
explicitly chooses to begin that game and asks the agent to continue T1 research,
brainstorming and development while play proceeds.

**Decision.** Adopt this continuing project loop:

1. The owner plays the current T0 candidate as the intended reference game.
   Record actual observations/feedback when supplied; the statement of intent
   is not a completed launch, save/reopen, campaign or human-acceptance pass.
2. Develop the next authorized tier, currently T1, on isolated dependent branches.
   Cover actual routes, machines, components, processing lines, renewable inputs,
   automation and reusable systems. Inspect mature implementations before new
   infrastructure design. Research and brainstorm proposals are not automatically
   accepted gameplay, quantities or additional tiers.
3. A reported issue that interrupts play has priority over new T1 work. Reproduce
   the actual defect, make a coherent fix and run its affected proof set. Preserve
   the original save and previous artifact. Use the existing stopped-instance
   deployment/update contract; no agent may stop or restart the player's game.
4. When the owner reaches the end of T0, incorporate the reported conclusions to
   fix and finish T0 polish. Do not equate development progress or an agent's
   judgement with the owner's acceptance. Continue the same feedback/development
   pattern for later tiers when their development is authorized.

This instruction supersedes the temporary T1 investigation pause and earlier
disposable-only start restriction for the owner's manual play. It does not
relabel unobserved client/retained-world checks as passed, publish an RC, merge
main, deploy T1, open T2 or guarantee an untested future migration.

T0's installed F artifact and the owner's running instance/worlds remain stable
during T1 work. Build-only execution has one explicit owner, fresh headroom and
worktree/cache locks, and yields to gameplay. A legitimate client JVM is not a
reason to terminate it or falsify a global-zero-Java requirement. Source, docs
and useful research continue if heavy execution must wait.

Before an update, compare changed IDs, persisted meanings, world derivation,
recipes/configuration and dependencies; test applicable compatibility on a
verified copy while preserving the reference original. The identified unused
vein-version pin remains explicit work before a conflicting resource-generation
update, not a claimed repair or reason to remap old surveys silently.

**Alternatives rejected.** Waiting for the owner to finish T0 before useful T1
work contradicts the instruction. Repeatedly demanding a disposable playthrough
or invented prior human acceptance defeats longitudinal play. Updating the live
instance continuously, skipping copy-based compatibility, inventing completed
passes, or spending an unlimited generation allowance is not authorized.

---

## ADR-0171 — Replace the player-blocking book with native FTB Quests and concise JEI-first content

**Status.** Accepted direct owner feedback; implementation/qualification pending,
2026-10-21.

**Context.** The owner cannot continue playing with the delivered questbook.
They explicitly requested an established BetterQuesting or FTB Quests integration,
not an original book or a large custom teaching widget. The owner also rejects
the exhaustive, impersonal guide prose and asks for brief explanations when JEI
already shows the recipe. This is a real play blocker under ADR-0170.

**Framework decision.** Use the supported Forge1.20.1 FTB Quests native experience:
chapter/quest graph, normal task/reward presentation, navigation, completion and
claim interaction. Verify exact published artifacts, required dependency closure,
licences and actual APIs before pinning them. Dependency use is not permission
to copy third-party source, assets or another pack's quest writing.

This supersedes the native-book/Heracles-only presentation and permanently empty
native-reward choices in ADR-0088/0100/0142/0149/0162 for this replacement. Do not
port the same custom guide, buttons, giant recipe panel or alternate navigation
into an FTB container and call that framework adoption.

Keep the registered Grindless Quest Book and legacy Guide identities safe for
existing saves, but route normal use to the genuine framework. Standalone or
missing-backend failures must be explicit; the official pack must not silently
fall back to the rejected screen.

**Content decision.** Ordinary quests have a useful title and one or two short,
natural English sentences: the immediate goal and, where useful, why it matters.
Use native task/reward item displays and JEI rather than duplicating ingredients,
energy, timing or every operating rule in prose. Preserve indispensable unusual
facts such as source-site placement, a non-obvious structure/side, renewable versus
finite supply and physical Matrix rating. A little in-world personality is welcome;
forced jokes, generic motivational prose and developer audit disclaimers are not.

The initial copy lane preserves all46 logical task IDs, rewards, prerequisites,
evidence and other locale values. It changes presentation, not recipes, starter
quantities or resource balance. Native definitions still derive from canonical
catalogue data instead of becoming a second handwritten recipe/quest authority.

**Progress and reward ownership.** The old Overworld per-player claim set is paid
history, not a list of newly claimable rewards. Import native individual reward
receipts before admitting native claims; opening the new book must not pay the
starter or any old reward again. Preserve partial/historical claim sets, including
claims whose current prerequisites are absent. Reconstruction grants no items.

Use one live grant authority. Once native ownership is established, delayed legacy
packets cannot continue minting through the old engine. Keep legacy paid-history
separate from derived native completion: completing an objective is not proof that
its reward has already been collected. Login/retry must not overwrite newer native
progress with a frozen legacy snapshot or reset another pack's quests.

Prefer supported native progress/reward APIs and ordinary tasks. A narrow native
task or compatibility adapter is justified for exact Matrix NBT or once-only import,
not another quest engine or UI. Do not preserve incidental requirements such as
holding the custom book at reward time merely because the old handler did so.

Native individual reward claims may replace the former single batch button.
Define and test exact delivered/claimed/pending outcomes, including full inventory
and the two non-stackable buckets. Do not advertise atomic no-drop refusal if the
chosen native operation actually uses overflow delivery. Team progress and
individual paid receipts are different: preserve individual reward history, do
not create parties automatically, and document/test actual native team behavior
rather than silently asserting the old projection has identical semantics.

**Migration and delivery.** Preserve the actual reference world and previous
artifact. Do not adopt the separate old exploratory save or rewrite raw framework
progress files to simulate a migration. Use an exact stopped-world backup/copy,
supported loading/import APIs and idempotent retry/failure tests. New dependency
pins/configuration must pass the existing package/ownership protections; if the
pin change requires a new isolated instance, retain the original and migrate a
verified copy rather than weakening the deployment guard.

Backend/API/pack work, content editing and independent verification run in
parallel on owned surfaces. Shared build/cache, source changes during execution
and final deployment remain serialized. Qualify the actual native definitions,
old-claim import, individual reward receipts, Matrix evidence, affected tooling
and one representative changed loader/binding. No recipe-per-instance smoke or
new custom validation framework is justified. Client presentation and the owner's
ability to resume remain distinct from a headless/server pass.

**Alternatives rejected.** Another original questbook, a re-skinned custom
Heracles widget, unchanged guide prose inside FTB, a second reward-paying ledger,
discarded progress, manual pin guesses, or an untested in-place dependency
replacement do not address the feedback.

---

## ADR-0172 — Automate recurrent T1 casings and Flux Cells through the existing Assembler

**Status.** Accepted coordinator implementation decision within the owner's
authorized T1 scope; source2f4eee1 authored, execution qualification pending,
2026-10-21.

**Evidence.** The sourceef9d670 route inventory identifies two actual recurrent
F2 component gaps: Machine Casing and Flux Cell have manual crafts but no
automatic process producer. This does not prevent the first manually assisted
F2. Their materials already have declared inherited supply boundaries, and both
batches fit the existing four-input Assembler.

**Decision.** Add ordinary `assemble/machine_casing` and `assemble/flux_cell`
data routes using the existing Assembler executor, conditions, output reservation
and save format. Match the actual manual catalogue/JSON material batches and
outputs; preserve both manual crafts for first construction and compatibility.
Do not add charged-Cell requirements, new catalysts, motors, Wire Mill, machines,
ports, slots, inventory formats or a crafting engine.

Use the existing `FabricationLogic.ASSEMBLE_TICKS` and `FU_PER_TICK` conventions
for both processes rather than another timing/configuration authority. At the
reviewed source these are400ticks and F1 nominal32FU/t. The old eight-second
casing design prose did not describe an implemented automatic route; reconcile
it with this explicit ordinary-Assembler decision rather than silently borrowing
an unproved prototype value.

Keep manual casing construction before the first Assembler and place automatic
reproduction after assembly exists. Do not create a forming/assembly bootstrap
cycle or change physical F1/F2 quest evidence. Select/explain the new recurrent
routes through the existing T1 contract/report; implemented repeatability is not
by itself a global renewable-establishment, power/autonomy or minimum-stock proof.

Validate the two exact batches against live catalogues/JSON, unique IDs, family,
arity, existing timing/FU constants and Atlas projection. Keep manual F1 five-unit
bootstrap and the existing F2 recipe/rating unchanged. Reuse unchanged executor/
NBT/native binding proof; ordinary item recipes need no native run per recipe.
Canonical generation and affected checks still precede a qualified delivery.

This independent source lane does not compete with the urgent ADR-0171 PC/cache
owner and must not deploy T1 into the owner's T0 game. Thermal's five distinct
construction ingredients exceed the current four-input Assembler and are not
an excuse to expand the engine or auto-project every `T1Recipes` entry here.

---

## ADR-0173 — Develop later tiers in parallel with protected T0 play

**Status.** Accepted direct owner development instruction, 2026-10-21.

**Context.** While the native questbook is finished and prepared in Prism, the
owner explicitly asks to continue all agreed T1 work and start work on the other
tiers. The accepted aligned T0/F0-T15/F15 campaign already defines their themes
and terminal frontiers; it does not make the registered later prototypes
survival-ready. The questbook remains the current play blocker.

**Decision.** Open isolated source development, research and dependency-backed
implementation for T2-T15 alongside T1. This supersedes ADR-0108's sequential
development hold and the T1-only limits of ADR-0168/0170, not sequential tier
acceptance, real-play feedback, release or protected deployment requirements.
Do not wait for T0 campaign acceptance to investigate or implement later content.
Do not infer that every tier is implemented, accepted or deployable from this grant.

Keep the coordinator responsible for architecture, product, route/sustainability
graphs, migrations and final integration. Assign coherent implementation and
research outcomes on isolated dependent branches. Current bounded work is:

- Integrate the qualified native FTB baseline into the existing T1 factory/
  component branch, retaining both automatic routes,50 quests, old IDs/rewards
  and the normal canonical writers.
- Begin physical T2 manufacturing with the existing Wire Mill/Assembler seam:
  one F2 Matrix is the fourth grouped construction operand, not a new machine,
  executor, slot expansion or acid requirement. Motor's existing component
  recipe and the T1 Press-coil/bootstrap routes remain unchanged.
- Ground T3-T15 work in actual source authorities/dependencies and choose useful
  T3/T4 and cross-tier mechanism increments; use pinned mature-mod source as
  precedent, not a copied balance calendar or licence to import foreign code.

The source trace finds no implicit F2 injection: the existing Wire Mill bill has
three groups. It also exposes a separate compatibility gap: `IngredientSpec`
and `ProcessLookup.sameItem` currently match matrix ratings exactly, although
`ControlMatrixSpec.canControl` and intended README policy allow stronger control.
The bounded source-only F2 operand preserves the current matcher; it does not
claim stronger-rating ingredient substitution works. Resolve that shared
mechanism/role distinction explicitly before accepting the completed T2 frontier,
without relaxing exact quest/upgrade receipt predicates or changing global
matching under cover of one recipe.

The native46-to50 configuration transition is also a distinct future boundary.
Current `FtbQuestFiles.ensure/preflight` deliberately rejects a managed existing
chapter missing the new quest/task IDs. Preserve that fail-closed behavior.
A later stopped, hash-bound managed-definition update must preserve all old IDs,
paid receipts/progress and edited-file refusals; the four reward-free lessons
start incomplete. Never implement it as raw native world-data replacement.

**Operational limits.** Finish the protected T0 questbook delivery first; report
and fix further play blockers before taking its execution capacity. PC/cache/
build/source synchronization remains single-owner with fresh guards and a new
explicit lease. Source parallelism is not concurrent JVM or deployment authority.
Preserve the original game and backup; never deploy T1+ into the owner's T0
instance as an incidental integration step. No new generation allowance, paid
service, credentials, release, main merge or acceptance is implied.

**Rejected alternatives.** Waiting for every earlier human campaign before any
later source work contradicts the direct request. Activating every registered
prototype, inventing fifteen independent engines, balancing unreviewed routes
or treating a proposal/fixture as accepted content does not satisfy it either.

---

## ADR-0174 — Grow tier complexity around basic machine families from T1

**Status.** Accepted direct owner design clarification, 2026-10-21.

**Context.** The owner reiterates that tiers must become exponentially more
complex, with more processing lines and content; T1 is where the basic machines
are made, in GregTech's family-based spirit. Later upgrades enable further
recipes, alongside specialized multiblocks. The owner also explicitly expects
research to lead to actual external reuse/adaptation when permitted.

**Decision.** T1 introduces the baseline ordinary machine families. The initial
five-machine factory remains a coherent implementation increment, not the whole
T1 tier. Do not distribute every first basic machine over later tiers merely to
fill a roadmap. Later tiers broaden the production network, introduce genuinely
new domains and specialized multiblocks, and improve existing families through
real chassis, control, condition, tooling, efficiency and logistics capabilities.
Ordinary baseline availability does not make every advanced process available
at T1; the later duty still needs its actual material/condition/control/energy
requirements.

This supersedes old first-introduction placement where it contradicts the owner
clarification, including a Wire Mill available only as a first T2 basic machine.
It preserves the acid-free first coil/mill intent and the functioning early Press
route. Source e265/5ff is retained as an actual bounded checkpoint, not discarded,
but a fourth F2 ingredient alone does not prove an upgraded Wire Mill: its output
must carry and use a real changed capability. Select baseline-F1 versus upgrade
construction only after tracing existing shared chassis/persistence seams.
Do not silently change every ingredient rating, recipe bill or saved machine.

The subsequent source trace confirms the emitted item and placed kind remain
F1/MK I; no upgraded capability follows from the F2 ingredient. The next bounded
implementation therefore makes the **baseline T1 Wire Mill** consume one F1
Matrix as its fourth grouped operand, keeping its casing/coil/plate bill,
400ticks/32FU, normal output and acid-free entry. Connect optional wire forming
after the first Assembler without adding a prerequisite to the initial F2 batch.
Do not change matching/storage or advertise MK II. Actual higher-capability
upgrades remain a separate shared-mechanism implementation, not this recipe.

Use the already accepted two-dial chassis-mark/modules design in MACHINES and
ADR-0027/0028/0030. Upgrade in place with stable block/save identity and preserve
stored items/fluids, configuration, links and ownership; specify treatment of an
active process rather than resetting it. Widen only a family's genuine physical
capabilities. Do not turn an ordinary furnace into a fusion chamber, create
fifteen executors or implement upgrades as an unexplained power-cost multiplier.
Whether each target seam already implements these requirements remains a source/
execution question, not a declaration of completion.

**Content growth.** Every tier proposal must show its increased set of useful
lines, alternative routes, interacting material/chemistry/control/energy domains,
conditions, byproduct/recovery decisions, logistics and signature projects against
the previous tier. The owner requires exponential growth in meaningful complexity
and content. No arbitrary numeric multiplier, recipe-count quota or timer/cost
inflation is selected by this clarification. Better automation and compression
of earlier work must keep manual burden from growing with the network.

**External work.** Prefer established implementations/public APIs over recreating
mature infrastructure. Reuse or adapt compatible work when it actually meets the
requirements, with exact source/version/licence/scope, required notices and
distribution consequences recorded before import. Pattern-only references are
not a promise of implementation; code availability is not a licence grant.
FTB's public binary/API integration remains valid reuse, not permission to copy
its visible-source implementation. No new paid service or generation allowance
follows, and no foreign balance calendar is authoritative.

**Boundaries.** Keep T0 native questbook delivery and original-save preservation
first. Broad later-tier development is still authorized by ADR-0173, not
acceptance/release/deployment. The source-only first-factory/F2 checkpoints,
unexecuted canonical writers, exact-versus-capacity matching gap and managed
native46-to50 migration stay explicit; none proves complete T1 or T2.

---

## ADR-0175 — Carry optional absolute process pressure through existing contracts

**Status.** Accepted coordinator engineering contract for source541cf6c9,
2026-10-21. Direct review confirms the optional pair/legacy constructors and
strict schema boundary; actual integrated426 CI compiles and passes the affected
noncanonical contracts. Normal model generation and the changed Blueprint native
boundary remain separate. This is not accepted gameplay, a production pressure
recipe, a tier upgrade or another PC execution grant.

**Context.** `ProcessRecipe` previously carried temperature, atmosphere and
agitation while the existing `ProcessConditions` / `ConditionBand` mechanism
already evaluates pressure. The source increment needs to carry a declared
process-pressure requirement through that same mechanism and its read-only
projections without changing existing recipes.

`ConditionBand.relative(0)` explicitly rejects a zero-width band. Zero absolute
MPa is nevertheless a meaningful optimum, distinct from no requirement.
Substituting ambient pressure or an epsilon would invent a physical requirement
or conceal that distinction. The inspection transport also needs to distinguish
a new required-pressure band from schema1's existing observed/tank pressure.

**Decision.**

1. Carry optional `pressureMPa` and `pressureHalfWidthMPa` in `ProcessRecipe`.
   Both are `NaN` when unnamed; all four old constructor signatures retain that
   absence. A named optimum is finite/nonnegative, its explicit half-width is
   finite/positive, and the resulting tolerance bounds must remain finite.
   Zero is never an absence sentinel.
2. Convert once through `ProcessLookup.conditionsOf` and the existing
   `ProcessConditions.Builder.pressure(optimum, halfWidth)` /
   `ConditionBand.absolute`. Reuse existing eligibility, speed and named faults;
   do not add another evaluator or change machine envelopes/executors.
3. Project the same values through Atlas, `AuthorityAdapters`, machine inspection
   and JEI. No existing production recipe starts requiring pressure, and no
   machine capability, Matrix matcher, work owner or item/fluid save format changes.
4. Write inspection schema2. Read strict schema1 with no required process-pressure
   band while preserving its existing observed/tank pressure fields. Reject
   unknown versions, orphan optimum/half-width and required-pressure fields
   mislabeled as schema1. Keep the existing protocol's explicit `MALFORMED`
   outcome instead of retaining an old successful observation.

**Alternatives not used by the source.** A relative band around zero cannot
represent the intended boundary. Ambient/epsilon defaults alter old recipes.
Another condition engine duplicates the existing mechanism. Silently discarding
new required fields under schema1 hides malformed data; schema2 without strict
schema1 reading needlessly loses old pressure-absent observations. None requires
foreign code, a dependency import or another persisted machine schema.

**Qualification boundary.** Source541cf6c9 is normally integrated in42654eb and
published M0a676fe. Exact CI39674226306 compiles/runs the affected contracts,
retaining three then-stale canonical failures. Actual426 NORMAL writers and
independent CheckOnly subsequently close those projections; A85069 carries only
the four generated outputs into M without retargeting writer provenance.
The separate Blueprint eligibility/probe change passes its new contracts at
e260 and one real native interaction at M; unchanged pressure proofs are reused.
The [validation ledger](testing/latest-validation.md#integrated-development-source)
records those distinct sources, exact-M full CI success and the duplicate
DRILL-OK observation failure without concealing it. All PC/cache leases are
returned. No production pressure recipe, native/client pressure presentation,
actual save compatibility or human acceptance is inferred from this contract.

---

## ADR-0176 — Distinguish exact Matrix evidence from minimum controller capacity

**Status.** Accepted coordinator engineering scope under the owner's continued
development instruction and ADR-0173/0174, 2026-10-22. Source1ee2e73 is normally
integrated; independent review and changed executable proofs remain pending.
No recipe balance, playable chassis upgrade or deployment
is accepted by this record.

**Context.** README permits a higher physical Matrix to replace lower controller
capacity. `ControlMatrixSpec.canControl` already expresses that across legitimate
architectures, but `IngredientSpec.matrix` / `ProcessLookup.sameItem` require an
exact item/rating. The F2 predecessor, explicit output and native exact-F1 quest
are different roles and must not silently become minimum-capacity tests.

**Decision.** Introduce an explicit typed minimum-control input alongside the
existing exact contract. Preserve old constructor/factory semantics, count rules
and exact item/rating identities. Only the existing `assemble/wire_mill` F1
control operand selects the minimum role in this increment. Leave the F2 Matrix
predecessor, all outputs and quest/reward evidence exact.

Validate genuine `ControlMatrixItem` data through the existing strict NBT/spec
mechanism and `canControl`. Wrong/ordinary items, lower/invalid ratings, malformed
or spoofed data cannot satisfy the role. Hopper admission may ignore batch count;
execution/consumption still requires and debits the real full batch.
Propagate the distinction consistently through Atlas/JEI, inspection/teaching
where affected, audit authorities and exporters. Preserve canonical qualified
resource identities; a human-readable minimum label is not a new catalogue ID.
A canonical lower-rating supply remains one legitimate accepted input, not a
claim that every alternate architecture has the same production route.
Only Relay Matrix items are currently registered. Exercise RelayF1-F3 in real
registered-item proofs and other architectures in pure spec contracts; do not
introduce fake registrations or advertise those contracts as native coverage.

**Source realization, not executed acceptance.** `IngredientSpec.MatrixRole`
keeps old factories/constructors EXACT; `minimumControl` selects the new role.
Registered viewer alternatives share the actual matcher. Atlas/teaching and
inspection distinguish F1+ without inventing resource IDs. Inspection writes
schema3, reads strict schema1/2 with EXACT by absence, preserves schema2 pressure
and rejects old-version role fields/invalid roles/minimum outputs.
The audit records canonical F1 as one accepted supply option, not every
alternate architecture's supply. T1 exporter provenance binds role/spec/adapter
sources and classes. Normal writers are still required; old426 outputs/proofs
are not relabeled. The extended existing matrix-lifecycle fixture retains its
old cases/budgets and has no observed new marker until actual execution.

**Reuse and alternatives.** Reuse the project's existing physical spec,
ingredient selection and executor. The pinned GT exact-circuit versus capacity
precedent supports explicit roles, not importing GT's ingredient/voltage engine
or globally relaxing equality. A recipe-ID special case would hide semantics
from viewers/audits. Replacing exact quest predicates would break progression
evidence. A new higher ingredient bill producing the same block is not a real
chassis upgrade.

**Limits.** Actual in-place family upgrades remain a separate shared-state
implementation. Current processor kinds expose their existing full envelopes;
silently narrowing them to construct an apparent mark ladder risks legacy
capability/save behavior. Do not invent new kit bills or envelope numbers here.
The closed426/e260/M proofs retain their sources; run focused changed-role,
projection and registered-item proofs, not an unrelated Blueprint/pressure replay.

---

## ADR-0177 — Update managed native quest definitions only through an explicit guarded transition

**Status.** Accepted coordinator engineering scope under ADR-0171/0173 and the
owner's continued development instruction, 2026-10-22. Source93ce975 is normally
integrated; unchanged-input Windows786/103 is actual file/tool evidence.
Independent provenance P2 closes in source only. Subsequent G3 managed44/
stale-class contract passes and actual fresh compiler/remap receipt are real;
old publication fails on a generated Architectury payload difference.
Corrected publication/plan-consumption qualification remains pending; no
player instance update is authorized.

**Context.** The installed native baseline has46 logical quests; current source
has50 with four reward-free lessons. `FtbQuestFiles.ensure/preflight` deliberately
refuses existing chapters missing required IDs and otherwise preserves compatible
user files. Normal startup must not become an implicit overwrite merely because
the source adds definitions.

**Decision.** Provide an explicit stopped-target, hash-bound managed-definition
update through the existing production publisher/deployment/file mechanisms.
Retain append-only/fail-closed startup. Management must derive from verifiable
known source/version/hash ownership, never filenames, ID counts or an agent's
guess. Unknown or edited/malformed/unsafe files are refused before mutation;
an existing unreceipted baseline needs demonstrated known provenance or refusal,
not silent adoption.

Complete preflight for the entire owned definition set before writing.
Preserve and verify previous bytes/metadata in a bounded backup, stage new
known bytes and record an interrupted/partial operation explicitly. Failure
recovery must not overwrite foreign concurrent changes or return success while
the transition is incomplete. Repeated successful update is a no-op. Existing
compatible user-file preservation is not replaced by unconditional byte management.

All historical logical/native quest/task/reward IDs stay stable. Definitions
may add the four reward-free lessons, not infer their completion or mint a new
starter. Native team/progress/claim SavedData, owner worlds and the legacy-paid
minting fence are never rewritten by this file transition. Reuse supported FTB
and ordinary safe-file/package primitives, with existing Windows/shell twins.

**Source realization and provenance correction.** Normal93ce975 preserves the
explicit UpdateQuestDefinitions/RecoverQuestDefinitions host actions, current
package-before-definition ordering, full preflight, backups/journal and startup
refusal while pending. Reference publication schema2 requires a fresh normal
Gradle JavaCompile/remap receipt, source/input hashes, actual loaded class images
and all selected-JAR payloads. A clean checkout, correct selected JAR and
historical classes merely located below the root are not sufficient. Source-only
independent review closes that concrete P2. G3 subsequently executes both fresh
compilers/remap and emits the authentic receipt, but selected-JAR equality
rejects one309-byte generated Architectury class's changed name/payload.
The2272 other common payloads match; this does not authorize ignoring a class
or assuming semantic equivalence. Investigate the exact pinned generator and
actual bytes before selecting a narrow fail-closed repair. Corrected export
and real-plan execution remain separate unproved properties. Preserve the
successful compilation for input-bound reuse rather than repeating it merely
for a comparator change. The prior real786/103 filesystem evidence is reused
only because its inputs are exactly unchanged.

**Rejected alternatives and proof.** Startup overwrite, accepting a filename as
ownership, raw progress replacement and ignoring user edits all violate the
save/paid-state contract. A private one-off migration script does not deliver
the repeatable production boundary. Verify actual filesystem46-to50/idempotence,
full-preflight refusal, drift and partial-failure recovery; use native execution
only for a newly uncovered loader/state property. Old native paid-history proofs
remain reusable when their implementation/assumptions are unchanged. No blanket
claim of crash atomicity, whole-T1 readiness, client acceptance or release follows.

---

## ADR-0178 — Continue autonomously and deliver ready tiers to the longitudinal game

**Status.** Accepted direct owner operating mandate, 2026-10-22.

**Context.** After requesting continued development, the owner explicitly
clarifies that work should proceed independently, stopping only for a serious
blocker or an urgently needed owner decision. Whenever a new tier is ready, it
should be incorporated into the longitudinal game so play can continue.
Stopping after each bounded engineering increment would not meet that direction.

**Decision.** Keep the development, integration, risk-based qualification and
delivery loop moving autonomously. Ordinary implementation choices, coherent
checkpoints and new sole-owner validation assignments are coordinator decisions,
not repetitive owner approvals. Escalate serious blockers or genuinely urgent
owner-only decisions; a temporary resource wait or a running player game does
not stop independent source work.

When a whole next tier is genuinely ready as a coherent playable candidate,
incorporate it into the existing longitudinal game. Readiness includes actual
content/lines/family progression, supply and bootstrap/recovery, useful teaching
and discovery, suitable polish, pinned installation and applicable changed-
mechanism/forward-save qualification under ADR-0144/0174. A partial first factory,
registered prototype, generated report or green compile is not a ready tier.
Do not freeze/accept a tier or fabricate a completed campaign to justify delivery.

Use the owned stopped-instance workflow, fresh ownership/resource guards,
verified backup and applicable copy-based compatibility checks. Preserve the
actual world identity, contents, existing paid quest progress and the original
backup. Managed-definition updates follow ADR-0177; do not substitute a newly
staged world, reset progress or overwrite raw native claim data. Verify installed
runtime/pins and leave launcher/client interaction to the owner.

If the player game is running, wait to deploy without stopping/restarting it.
Continue unrelated development during that wait. An unknown or unsafe target,
failed backup/compatibility/recovery or unreconciled source change blocks that
delivery and must be reported honestly, not bypassed to preserve momentum.

**Precedence and limits.** This later explicit mandate supersedes blanket
development-only/no-later-tier-deployment holds for genuinely ready candidates.
It does not waive technical/manual/save gates, authorize premature partial-tier
updates, infer human acceptance, grant new paid/provider generation or credentials,
or independently request a public release/main merge. Existing isolated lanes,
single PC/cache owner and serial publisher rules remain. Preserve historical
receipts and earlier authority labels; apply this mandate prospectively.

---

## ADR-0179 — Reproduce four-slot-fit first-factory machines without repricing bootstrap

**Status.** Accepted bounded acquisition scope under ADR-0174/0178, 2026-10-22.
Implementation and qualification are pending; not all first-factory machinery
or the whole T1 tier is declared automated or ready.

**Context.** The first manual factory is intended to reproduce useful machines,
not require repeated hand crafting forever. Existing T1 shaped catalogues and
JSON already define the material bills. The Assembler has four grouped input
slots; the Thermal Generator's manual bill has five distinct groups.

**Decision.** Select ordinary Assembler reproduction of the existing Pulverizer,
Arc Furnace, Press and Assembler only. Derive exact material multiplicities and
ordinary outputs from their existing manual source/JSON; retain all manual
bootstrap recipes. Reuse the existing fabrication cycle/FU and grouping helper
with an explicit whitelist, not automatic projection of every future craft.

Each new machine route declares its F1 controller as minimum capacity through
ADR-0176's existing typed API. This is a new explicit opt-in for those four
routes, not relaxation of old exact constructors, F2 predecessors, outputs or
quest/reward evidence. Higher valid stock still yields the same ordinary
baseline machine; it does not upgrade a chassis or its saved state.

An optional reproduction line follows assembly. Manual first machines seed
that line; it cannot become a mandatory bootstrap/first-F2 cycle. Existing die
reproduction and retained catalysts remain intact. New route IDs are unique,
and Atlas/JEI/audit use the shared role/projection rather than a parallel engine.
Trace construction inputs honestly; an unresolved input is not a renewable-
establishment certificate.

**Not selected.** Keep Thermal reproduction separate because its five groups
do not fit. Do not enlarge slots, invent a compressed ingredient/intermediate,
discount a bill or claim all five machines automatically reproduced. Broader
families, genuine capability upgrades and complete tier readiness remain work.
Ordinary new rows reuse the proven executor/role mechanism: exhaustive data and
affected normal projections, not one native smoke per machine.

---

## ADR-0180 — Reproduce the Thermal Generator through an honest two-stage subassembly

**Status.** Accepted separate bounded acquisition scope under ADR-0178/0179,
2026-10-22. Implementation and qualification are pending; no whole-tier readiness
or player update is implied.

**Context.** ADR-0179 deliberately selects only four manual machines whose
material groups fit the Assembler. Thermal has five groups, so simply adding
it to that whitelist would either exceed slots or conceal/drop an input.
The owner's ongoing programme calls for completing useful factory automation,
not making that initial exclusion permanent.

**Decision.** Add one ordinary Thermal Generator Core component and two real
Assembler operations. The core consumes the furnace, carbon and iron groups
from the existing manual recipe. Final assembly consumes exactly one core,
the remaining cobblestone and one explicitly minimum-F1 controller. Derive
multiplicities from the live manual catalogue/JSON and prove composed parity:
the current five cobblestone, furnace, carbon, iron and Relay Matrix are neither
discounted nor duplicated. Each stage fits three groups within the existing four.

Both stages pay the existing ordinary fabrication cycle/FU. Their combined
cost is two operations, not a claim of one operation's energy/time. Preserve
the manual first generator as the factory seed. Extend optional reproduction
after assembly; no new mandatory first-F2 edge, world gate or bootstrap cycle.
The final output remains the ordinary unchanged F1 generator, not a chassis
upgrade, new fuel rule or modified saved inventory.

Register the one ordinary item with proper existing asset/discovery and
resource metadata. Prefer existing owned/vanilla resource references or the
project's deterministic asset mechanism; no third-party asset copying or paid
generation is authorized. Source-backed component production/consumption is
not automatic proof of global renewable establishment or artistic acceptance.

**Rejected shortcuts and proof.** Do not widen slots, hide an ingredient in a
matcher exception, price the machine differently or automatically project every
future manual craft. This is an explicit productive subassembly, not a general
recipe engine. Verify exact cancellation of the intermediate, counts, roles,
bulk-input selection against generic Assembler recipes, optional acyclicity,
registry/model validity and unchanged manual/claim/F2 contracts. Reuse the shared
execution/Matrix mechanism proof; add only genuinely uncovered registration
or adapter observations, not repeated native cycles for ordinary content.

---

## ADR-0181 — Compare one generated platform shim by exact self-name equivalence

**Status.** Accepted bounded source-repair contract under ADR-0177/0178,
2026-10-22. Upstream source independently corroborates the measured class
structure and path-dependent name; two exact declared devlibs path strings
produce both observed stamps without PC access or path-variant guessing.
Source555dbe9 is normally integrated; corrected Java/publisher execution
remain pending. The original G3 publication failure stays real;
no player update or reference publication follows from this decision.

**Context.** G3 genuinely compiles/remaps the preserved dd0 source in a fresh
normal enclave. Receipt5f911a77 is authentic. Selected JAR29bcce43 and fresh
JAR8a7af1e8 each have2273 entries;2272 common payloads are byte-identical.
The remaining309-byte Architectury PlatformMethods class differs in its entry
and internal self-name. Both compiler successes must not be repeated merely
to test a changed comparator.

The source owner inspects both actual classes without a JVM: ClassFile52.0,
flags0x0011, constant-pool count10, self UTF8 at#1 and Class#1 at#2/this_class,
Object superclass, no interfaces/fields. The only method is public static
getCurrentTarget()Ljava/lang/String;, max_stack1/max_locals0, exactly ldc#8
(`forge`)/areturn, with no exceptions/other attributes. Only self UTF8 differs;
every other byte matches. The reported SELF-representation SHA-256 is
`0a84a9c64d4531a5942f7fab6de29d225f189424344d81b42e4642cb468923d6`.
Upstream source independently corroborates the generator below. The source
owner also confirms both actual stamps from the two declared input paths
locally; private path values stay outside Git and no PC reexecution is inferred.

**Decision.** Preserve ordinary exact comparison first. Only for one unmatched
pair may the publisher accept this exact recognized class format/fingerprint
with internal name equal to its JAR entry. Preserve the module prefix,32-hex
identity and artifact suffix across the pair; permit only the64-hex component
of the self-name to differ. Validate/hash the fixed structure and all remaining
bytes, not merely its method's answer. All other JAR names/payloads remain exact.

An extra shim, missing/extra other resource, inconsistent entry/internal name,
changed instruction, flags, descriptor, constant pool or other byte fails closed.
This is a comparison of one concrete generated structure, not prefix exclusion,
arbitrary class normalization, a parser/compiler/remapper framework or blanket
binary reproducibility. Keep selected-JAR and fresh-compilation identities
distinct, and expose the applied comparison/names/fingerprint in the existing
publication evidence. Source/input/recipe/loaded-image/compilation hashes,
reference schema2 and stale-class rejection remain mandatory.

**Established implementation and scope.** Running pins are plugin3.4.164 and
Loom1.7.423. The plugin's [project identifier implementation][181-project]
persists a32-hex UUID in the checkout's ignored `.gradle` cache; it is not
versioned project source. Its [generated package function][181-package]
combines that identifier with SHA256 of the UTF8 input-JAR path string and
the legalized filename. The [Forge wiring][181-forge] uses that function.
An isolated build directory therefore changes the path-derived64-hex segment,
not necessarily any application code or the checkout UUID.

The transformer's [platform-method generator][181-transformer] corroborates
the exact52.0/public-final/single constant-return method measured above.
These cited implementations are MIT-licensed; no code/assets are imported.
The plugin release-to-source-commit association is date-correlated evidence,
not an attested binary build mapping. Keep actual artifact versions and cited
source commits distinct; exact observed bytes/fingerprint still bound the
exception. No claim about every possible Gradle task override is needed.

Changing the compiler recipe to force a historical absolute path/namespace
would invalidate the successful fresh receipt and risk old output custody.
Compare the already-proved narrow generated structure instead. A different
checkout UUID, changed filename identity or unknown generator shape remains a
visible refusal, not a general historical-JAR recovery guarantee.

**Reuse and limits.** The existing eight-argument source-file publisher already
accepts the authentic compilation receipt and validates its immutable inputs.
Reconsume that receipt/output after source repair if those bindings still match;
do not add an adoption flag, rebuild the legacy source by assumption or alter
historical outputs. Exact and recognized-rename positives plus changed-body,
identity, additional-entry and other-resource negatives belong in the existing
contract. Retain the upstream corroboration and qualify the corrected publisher
on the actual preserved pair. A source-only
fixture is not that real export or managed-plan consumption.

[181-project]: https://github.com/architectury/architectury-plugin/blob/3b9bbb8bbf2cd7507193a0ca447a80afc4f9039f/src/main/kotlin/dev/architectury/plugin/TransformingTask.kt#L121-L155
[181-package]: https://github.com/architectury/architectury-plugin/blob/3b9bbb8bbf2cd7507193a0ca447a80afc4f9039f/src/main/kotlin/dev/architectury/plugin/ArchitecturyPluginExtension.kt#L517-L539
[181-forge]: https://github.com/architectury/architectury-plugin/blob/3b9bbb8bbf2cd7507193a0ca447a80afc4f9039f/src/main/kotlin/dev/architectury/plugin/ModLoader.kt#L65-L89
[181-transformer]: https://github.com/architectury/architectury-transformer/blob/2698278e63d0afea4969b5fd6c96a68fb64432f2/src/main/java/dev/architectury/transformer/transformers/TransformExpectPlatform.java#L59-L87
