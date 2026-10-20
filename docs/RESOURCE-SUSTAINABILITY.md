# Resource sustainability review

Status: **implementation in progress**, under ADR-0131. This is the initial concrete review,
not a claim that the full graph is already closed or that a dashboard mockup is production data.

| Current seam | Finding | Required correction |
| --- | --- | --- |
| T0 clay/ceramics | Settling consumes dirt; milling and firing can repeat, but their upstream natural stock is finite | Add a mod-owned geological supply and trace every input through it |
| Wood and carbon | Tree charcoal is currently assumed for repeated fuel and construction | Add biological production and automatic mod-owned energy; vanilla trees remain bootstrap/optional only |
| Iron/copper | Chunk veins deplete; assisted extraction is not unlimited renewal | Supply an original internal mineral route; retain finite veins as an optional acquisition route |
| Renewable glass/redstone claims | Existing process rows still depend on vanilla cobblestone generation and carbon supply | Audit recursively; a downstream repeating recipe cannot certify its terminal inputs |
| Recovered water | Corrected under ADR-0132: produced-volume ownership and front export recover exactly 750 mB through the conduit | Retain the exact volume, legacy-stock and output-reconstruction checks |
| Side streams | Grog, slag, gases and losses lack one canonical classification/handling source shared by player views | Add metadata, validation and factual use/store/recycle/discard guidance |
| Progression data | No complete sustainability/deadlock report exists | Generate the machine-readable graph and human report from authoritative catalogues |
| Observability | The smoke report is not a Tier/resource dashboard | Generate a provisional dashboard from real audit, quest, asset and validation data |

The new settling implementation compiles and passes the 34 behavior/resource suites, including
56 T0 PNGs, exact geometry continuity and the schematic assembly preview. Native execution
already produces the expected solids in all four orientations and preserves covered work,
energy, input and reconstructed fluid. The subsequent output-ownership correction passes the
full settling recovery scenario and its workshop/Matrix regressions. This closes fluid handling,
not the still-finite upstream dirt supply.

The first W1 CI had a fixed-time `WORKSHOP-IN-PROGRESS` failure and a transient item-entity
`BOOK-OK` failure; a sibling green run was not accepted as a repair. The workshop now waits
boundedly for observed progress, and the book fixture uses exact stationary inventory stacks.
These corrections stay under validation with the settling unit.

## Classification and disclosure

Use the canonical classes from ADR-0131. A resource's current route may be unresolved even when
its recipe exists. Report finite sources, vanilla assumptions, bootstrap margins, source tiers,
recycling losses, handling gaps and explicit exceptions rather than hiding them behind a label.
No hazardous mechanic, future consumer, player acceptance or stable deployment is claimed merely
because a field can be added to a JSON document.

The planned geological/biological/power systems above are implementation directions, not
implemented renewable producers yet. Their code, source justification, automation and exact
validation must exist before they can close an audit finding.
