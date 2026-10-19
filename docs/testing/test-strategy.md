# Risk-based validation and proof reuse

Accepted for the active Grindless work under
[ADR-0140](../DECISIONS.md#adr-0140--validate-behavioral-classes-and-reuse-proof-under-explicit-assumptions).
Required technical checks and honest player/migration evidence remain. The delivery
ordering is now [ADR-0144](../DECISIONS.md#adr-0144--ship-superflat-first-playable-tier-releases-with-longitudinal-save-continuity):
technical/product RC readiness precedes the owner's longitudinal human acceptance.
The objective is coverage of relevant properties, not a native scenario per content instance.

## Playable-release proof boundaries

`1389ebb` remains engineering evidence. A policy, sprite or document change does not
invalidate its unchanged mechanisms; changing the starting environment does invalidate
any inference that its ordinary-world finite entry is accessible in the official Superflat
preset. Version that preset and bind its deliberate terminal sources to the model, then
exercise the smallest representative startup/progression path whose accessibility or
generation semantics cannot be established statically. Keep Overworld compatibility.

Before RC delivery, prove the chosen pack's clean import, pins/dependencies/configuration,
startup, expected content/preset, representative questbook and visual/audio behavior,
and absence of known ordinary-play blockers. A contact sheet is visual evidence, not an
in-game render; waveform/licence checks are not listening. Preserve human versus agent
observer identity and never relabel headless/server evidence as player acceptance.
Do not rerun the whole native matrix for each artistic asset or guide sentence.

The owner begins the longitudinal save after playable-RC publication. Its no-command
playthrough and feedback then provide human acceptance, potentially alongside T1 work.
The first release needs an explicit forward-compatibility strategy, not an invented
pre-existing longitudinal save. Each subsequent update validates changed IDs/data/config
and applicable migration on a preserved copy of the actual reference save; keep the
original. Existing codec/transaction proofs are reusable, not a substitute for that
upgrade observation when a compatibility boundary changes.

The [playable-release plan](../../plan/process-t0-playable-release-1.md) names the new
gaps, proof reuse and dependency order. No new unobserved run is supplied by this policy.

## Before an expensive validation

**Cost scales with novelty and risk, not project size or raw change count.** Before
launching a native/client/server smoke, explicitly record what new uncertainty it
resolves that existing evidence does not resolve. Without a concrete answer, do not
run it. Prefer one representative mechanism proof plus broad cheap structural,
contract and progression coverage. Reserve broad integration for actual release
candidates, migrations that affect behavior, changed architecture/mechanisms,
observed regressions or genuinely uncertain boundaries. A chat-context migration
alone changes none of those assumptions.

Ask internally: **What specific property remains unproven, and can an existing proof plus
static evidence already establish it?** If yes, reuse that proof. If not, identify the
smallest missing behavioral class and delegate a targeted check, independently when useful.
Record the property, relevant change, proof references and reuse/rerun decision with the
assignment or evidence. This is a short dispatch decision, not a new approval gate.

Reuse requires the same mechanism or reviewed equivalence, the same relevant assumptions,
and a static check that the new data is within the proven domain. Assumptions include
loader/API/dependencies, storage ownership and side policy, input/output kinds and bounds,
conditions, topology, lifecycle and save format where relevant. A new commit hash, README
change, recipe ID or missing classification alone does not invalidate a mechanism proof.
Conversely, matching class names or a green build alone do not establish equivalence.
Threading, re-entrancy and callback ordering are explicit native-transaction assumptions;
changing them triggers contract review even if public types remain unchanged.

Classify by behavioral effect, not extension: Java-authored recipe/annotation data can be a
data-only change. Recompute reports and data checks for changed inputs, but do not replay an
unchanged audit kernel or executor once per new row. Preserve the original proof's revision,
artifact identity and limitations; cite the new delta/equivalence rather than relabeling an
old execution as a new run.

## Validation classes

| Class | Typical property | Required approach |
| --- | --- | --- |
| Shared mechanism/contract | Recipe execution/state transitions, native fluid transactions, serialization, audit rules, common projections and runner behavior | Focused contract tests with boundaries/failures and representative integration; reuse for instances within the proved domain |
| Static/data-level | Recipe IDs, quantities, arity, tags, ratings/envelopes, classifications, handling/consumer references, tier roots, bootstrap budgets, resources/localization and export schemas | Exhaustive applicable data/model checks against live authorities; no native run merely because another row or unknown exists |
| Representative integration/runtime | Actual loader/registry/capability bindings, native logistics composition, real-data UI integration | Reuse an equivalent representative path; add a targeted case only for an uncovered binding, interaction or assumption |
| Regression | Prior reproduced failures and contracts affected by a change | Retain precise negative/positive oracles; rerun the affected dependency set and appropriate neighboring coverage, not equivalent instances by count |
| Genuinely unique behavior | New execution path, ownership adapter, source/site semantics, migration, external network behavior or actual player interaction | Dedicated proof where static/model evidence and existing contracts cannot establish the property; native/manual only when that observation is necessary |

An ordinary recipe using the same executor, I/O classes, conditions and bounds needs
structural and sustainability/progression validation, not its own costly native scenario.
A new fluid output mode, participant/ownership policy, save layout or scheduling path is
not automatically ordinary just because it uses the same executor class.
Data can also change recipe/tag selection, exceed numerical/arity/capacity bounds or change
persisted-state meaning. Check that effect before calling it an equivalent instance.

## Reusable proof ledger

Detailed attempts and hashes remain in [latest-validation.md](latest-validation.md) and
[pending checks](../../.agent/pending-checks.json); failures and cancellations are not erased.

| Proof | Proven scope | Boundary that must not be broadened |
| --- | --- | --- |
| `S1-kernel` — `639881f`, composed `dcb8010` | Fixed-point/rule/reserve contracts, twelve repaired cases, tag/debt/report identity and deterministic execution; 36-suite gate | Does not supply missing T0 annotations, canonical pack/tag priority or renewable closure; bounded unproved model cases remain unknown |
| `native-completion` — `dcb8010` | Native identity/side/state/quota checks, aliases, boolean/exception compensation of sources and consumer, retry, completed-work retention and current NBT reconstruction | Native synchronous owners only; not foreign handlers, crash-atomic worlds, arbitrary new adapters or a retained-world migration observation |
| `native-binding` — `dcb8010` | Production states 945/945 and settling 19/19, matching built/deployed jar, natural shutdown | Representative registered/native-fluid integration, not every client surface, new loader/API or changed topology |
| `renewable-cycle` — `39baaa0`, followed only by README clarification in `fdce60b` | Ordinary no-override production run, 24 markers, an 800-tick daylight source window with four added cycles of each source, exact stock/FU accounting, native materials and hopper-recovered calibrated core | Finite setup; no pylon only in the source window. Downstream uses Mk1 and command staging; no closed downstream power/routing, night/restart, full construction reserve, independent water-flow or physical five-Matrix craft claim |
| `smoke-budgets` — `ab1f7ac` | 27 native PowerShell and 27 native GNU Bash fake-boot checks; real `.timeout=900` use in `renewable-cycle` | Canonical twin runners, not the legacy shared-world batch utility or every host environment |
| `CI-baseline` — `ab1f7ac` | Completed successful CI run `39081561476`, including verify and six smoke shards | Its actual source/inventory only; the later native fixture has its own source-bound evidence |

The new policy invalidates **none** of these proofs. In particular, do not rerun the composed
S1/C3 gate or renewable cycle simply to adopt this document or clarify their README.

## Outstanding work classification

| Existing work | Classification and next sufficient proof |
| --- | --- |
| S1 core | Shared-contract proof is complete in its stated domain. Use existing regressions when the kernel, resolver, reserve or report mechanism changes; do not reopen it because S2 has more data |
| S2 / production inventory | Static/data first: complete authority coverage, real tag membership/priority, annotations, canonical routes, source provenance and export identities. If a new runtime/pack adapter is needed, verify that binding once and validate all its rows structurally |
| S2 / sustainability and by-products | Run the complete applicable model on T0 roots, handling, construction and exact bootstrap guarantees. Missing metadata, unresolved tags or solver bounds are not a list of native/manual scenarios. They remain visible and non-certifying until resolved; annotations cannot invent a consumer or runtime capability |
| Bootstrap/progression | Validate quantities, finite reserves, source prerequisites and quest optionality in the model and focused quest/recipe checks. Keep one actual no-command T0 path and ADR-0137 physical hand craft as the existing distinct player-acceptance properties, not one run per ingredient |
| C3 ordinary recipes, materials and assets | Reuse executor/conditions/I/O contracts and existing bindings; validate every instance's shape, rates, capacity, tier, tags, resources and localization. Only a new behavior or parameter outside the proved domain needs new contract/runtime proof |
| C3 full factory claims | Reuse the completed source-window and native-material/core evidence. Assess remaining downstream autonomy, power/routing and restart properties first with models/static composition; if insufficient, select one representative missing interaction, not another native run for every ordinary recipe |
| Persistence | Reuse tested unchanged NBT/layout/owner semantics. New layouts, adapters or migrations rerun the affected persistence cases and representative reload; actual retained-world compatibility remains a distinct existing acceptance gate |
| Player communication / dashboard | Validate canonical projection, schema, provenance/privacy, localization and all data instances; reuse common renderer tests. Use representative real-data browser/client interaction for an uncovered UI adapter/layout, not native gameplay for every tooltip |
| Final T0 integration | Aggregate proof coverage and changed assumptions, retain required CI/release checks and genuine manual gaps. Do not require new native evidence for equivalent content already covered by contracts plus data checks |
| Ports / external integrations | Still held by the unchanged task dependencies. Existing native proofs may cover internal pieces, not exclusive multiblock linking/aggregation, foreign or ME storage, disconnection or optional API presence. Verify actual versions/APIs and genuinely new boundary classes when that stage is authorized |

The first real dashboard binding still needs its already-required real-data/browser
integration review. Synthetic renderer tests and schema checks alone do not discharge it;
subsequent equivalent row additions do not require a browser replay per row.

## Change impact and independent review

When a shared mechanism changes, identify which proof assumptions depend on it before
dispatch. Examples:

- Executor, reservation, scheduler or native-owner changes: affected contract and prior
  failure cases, then representative runtime paths whose assumptions changed.
- NBT/layout or lifecycle changes: affected serialization/migration/reload cases; retain
  the actual saved-world gate when applicable.
- Recipe/annotation/tag data changes within the proved domain: exhaustive static/model
  validation and affected regressions; no automatic native matrix expansion.
- New registrations or loader/capability bindings: compile and the existing representative
  registration/state boot, plus a new binding class only if it is not already covered.
- Runner/projection changes: their native fake-boot or component contracts and a relevant
  integration path; do not replay unrelated gameplay contracts.
- Documentation-only changes: document integrity and proof-reference checks, not Minecraft.

Independent QA and architecture reviewers check uncovered behavioral classes, relevance of
assumptions and false reuse claims. They do not redundantly replay equivalent successful
cases. Keep mutable-input validation serialized per worktree while independent work proceeds.
No existing test, threshold, CI requirement or manual gate is disabled by this policy.

## Available execution entry points

The fast deterministic entry point is:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate.ps1 -Root .
```

For the integration gate, opt into the Minecraft EULA and add the isolated smoke inventory:

```powershell
$env:GRINDLESS_ACCEPT_EULA = 'true'
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate.ps1 -Root . -Smokes
```

Linux/macOS:

```bash
bash tools/validate.sh .
GRINDLESS_ACCEPT_EULA=true bash tools/validate.sh . --smokes
```

Prerequisites: Java 17 (`JAVA_HOME`), Git, network access on a cold Gradle/Forge cache, and
PowerShell Core (`pwsh`) for the shared documentation validator on Linux/macOS. Missing tools
fail explicitly. Versions remain in `gradle.properties`; neither script installs globally or
changes credentials. `GRADLE_USER_HOME` may point inside the worktree.

## Scope and evidence

| Layer | Real entry / authority | What it proves |
| --- | --- | --- |
| Environment/build | Committed wrapper, common compilation and full build | Actual Java/Gradle versions, compile/type compatibility and packaged artifacts; Gradle `test NO-SOURCE` is not a passing test suite |
| Behavior/resources | `tools/run-checks.ps1` / `.sh`, Gradle's resolved classpath | Every `Verify*.java` suite, actual NBT/persistence, recipe/quest policy, resource references, graph/asset preparation and platform-runner failure propagation |
| Documentation | `tools/check-links.ps1`, shared by `.sh` through `pwsh` | Tracked/new non-ignored Markdown files, relative anchors and ADR index |
| Integration | `tools/run-smokes.ps1` / `.sh`, `tools/smoke/scenarios.txt` | Fresh production Forge worlds loading the actual remapped jar; each scenario's declared assertions and natural exit 0 |
| Player acceptance | [Exact checklist](t0-survival-acceptance.md) | Real client UI, player claims, saved-world restart and no-command Bootstrap completion |

Each unified run writes `build/validation/<run-id>/source.txt`, stage logs and `status.tsv`
with UTC starts/finishes and actual exit codes. That ignored output is execution evidence;
preserve the meaningful results and tested revision in [latest-validation.md](latest-validation.md)
and [the active handoff](../../.agent/handoff.md). CI retains stage logs as artifacts.

The fast gate explicitly omits smokes unless requested. It is not full integration or Tier
acceptance. The full matrix can be split/resumed with the existing `-Only` / `--only` selectors
but each batch must retain its own revision, selected names, outcome and logs. An interrupted
aggregate remains interrupted even if a subsequent complementary batch closes the inventory.

## Isolation and recovery

Run only one Gradle/generated-output or game-world mutator in a worktree at a time. Use
read-only analysis or documentation while a smoke inventory is running; do not mutate its
runtime or scenario scripts. The server listens on loopback, uses a fresh `smoke-world` and
cleans only that named disposable directory. Failed shutdown is a failure, even if assertions
ran. A forced cleanup is not a clean exit.

Installer-owned libraries are distinct from mutable worlds and staged mod jars. The explicit
runtime inventory causes a missing/truncated library to be repaired, while `remapJar` dependencies
ensure the staged Grindless artifact is current. Hash the staged and built jars when recording
release-gate evidence; do not claim byte-for-byte reproducible builds from a single match.

The optional `:forge:runServer` development path has a separately recorded shutdown hang.
Integration uses the production path under ADR-0126, not reflective worker cleanup or a forced
`System.exit`. macOS runtime execution, installed-pack client behavior and old ambiguous compact
saves are not automatically established by Windows/Linux server passes.
