# T0 free starter supplies

Implementation base: `b29439bf0112a12ee79baa155fd21231f0ddebd7`.
This is the owner-decided generous finite entry, not a new world preset, village
requirement, shop, recipe permission or renewable-resource claim.

## Player contract

`starter_supplies` is the first task in the existing Bootstrap line. Its goal is
explicitly `FREE`: no item possession, payment, location or prerequisite is needed.
Holding the actual Grindless Quest Book is still required by the shared
server-authoritative claim service. Opening a renderer is not a claim.

| Vanilla item | Total | Legal reward stacks |
| --- | ---: | --- |
| Cobblestone | 128 | 64 + 64 |
| Oak Log | 64 | 64 |
| Bread | 64 | 64 |
| Brick | 64 | 64 |
| Clay Ball | 64 | 64 |
| Sand | 64 | 64 |
| Charcoal | 32 | 32 |
| Iron Ingot | 16 | 16 |
| Water Bucket | 2 | 1 + 1 |

The eleven stacks contain no machine, Ceramic Insulator, Data Core, Calibrated
Data Core or Control Matrix. Existing recipes, industrial progression and Matrix
requirements are unchanged. Supplies are finite, generous starting stock, not a
minimum construction bill or a promise of a completed T0 route.

On server login and respawn, an unclaimed player without the book anywhere in
their actual inventory receives one existing book. This includes offhand evidence;
an existing book is not copied. A full main inventory receives a visible explanation
and no dropped item. The server retries deferred delivery once per second until
space exists, the book is present or the starter has been claimed. Logout/server
stop clears only that transient retry set.

The utility-book recovery does not deliver starter supplies or write quest progress.
Old worlds are not forced to claim a kit. After a successful starter claim, automatic
recovery stops; the existing paper/stick book craft remains available for later
replacement. No command, paper, tree or crafting is required for initial access,
in classic Superflat or an ordinary Overworld.

## Claim and save contract

`QuestCatalogue` owns all rewards. The original single-reward constructor/accessor
remain compatible; delivery, presentation and resource export use the complete
`rewards()` list. Every resolved stack is checked against its actual item maximum.

`QuestRewardDelivery` stages copied main-inventory slots using vanilla
`SimpleContainer` stacking. It does not insert into armor or allocate an offhand slot.
The complete batch must fit before the existing claim ledger is changed. Insufficient
capacity returns `INVENTORY_FULL`, leaves every carried item and the claim untouched,
and drops nothing. Freeing enough space and claiming again delivers the batch.
This same bounded loss-prevention rule also protects existing single-stack rewards.

The successful path records the existing per-player Overworld
`grindless_quest_progress` claim and commits the prepared inventory on the server
thread. Reopening, reconnecting, respawning or normally saving/loading does not reset
that claim. Native Heracles completion is only a projection of that ledger; native
rewards, autoclaim and party-sharing cannot mint supplies. No second claim ledger,
progress reset, old-ID reuse or save migration is introduced.

The twenty-five previous IDs, rewards and prerequisites remain unchanged.
`Evidence.FREE` and `Result.INVENTORY_FULL` are appended, not inserted ahead of
existing enum values; status codes retain their old meanings. Catalogue indexes
are transient same-version menu button/data indexes, not saved quest identities.
Client and server must use the same candidate. This does not add a process-crash
transaction across separately saved player inventory and world SavedData.

Both quest interfaces present a free goal rather than `0/0` and retain the complete
reward list. Heracles definitions continue to be generated from the catalogue and
published through the existing owned atomic archive; no hand-maintained second
catalogue is introduced. A normal generated update adds the new stable ID without
removing old definitions or adopting modified/foreign archives.

The resource-authority adapter projects every reward, summing legal split stacks,
and explicitly identifies the `FREE` goal without inventing a physical resource.
The starter route is optional finite aid in the existing conditional T0 model.
That historical witness still chooses to fabricate its own book; it is not a claim
that crafting is necessary for initial player access, nor a new flat-world play pass.

## Verification and remaining qualification

Executed in this child: 40 PowerShell source-only allocation/compatibility/access
assertions pass, including exact old task declarations and unchanged SavedData,
and the existing link checker passes 690 relative links / 158 ADR sections and
index rows. The first auxiliary Python invocation exits 9009 because the local
Python executable is absent; it executes no checks. The native PowerShell check
replaces that auxiliary attempt without installing anything. These results do
not execute Minecraft inventory code or certify client behavior.
The first local checkpoint commit is refused because no Git author is configured;
checkpointing uses a transient Copilot App identity rather than changing global or
personal Git configuration.

The bounded UI worker also executes PowerShell/.NET source checks of JSON/key
scope, both explicit-free render branches, complete reward iteration, the static
eleven-icon geometry and unchanged historical IDs, plus `git diff --check`;
the final invocation exits 0 with six source-check PASS groups, not a numbered
Java assertion count. Its initial source-footprint helper exits 1 with
`DirectoryNotFoundException` because seven paths were joined into one
`ReadAllBytes` argument; corrected enumeration passes. No codec/loader/widget
Java or graphical renderer is executed by these source checks.

The existing closed source-binding check is extended from twelve to sixteen
authorities, including the catalogue and utility-book/reward/event path.
Its bounded worker executes a PowerShell lexical check of 50 exact method profiles,
10 nested declarations and 86 negative-mutation anchors, package/import/delimiter
checks and `git diff --check`. This is source-profile consistency evidence, not an
execution of the Java AST gate or its counterexamples. Existing sender, thread,
physical-book, current-evidence and single-ledger guards remain bound.

The focused existing checks cover the exact allocation and old catalogue
compatibility (`VerifyQuest`), no-book rejection/free claim/idempotence/complete
inventory delivery/deferred book recovery (`VerifyQuestClaims`), binary legacy and
starter SavedData reconstruction (`VerifyPersistence`), actual selected Heracles
definition/loader/projection behavior (`VerifyHeracles`), and the source-bound
authority/resource projection (`VerifySustainabilityRegistry`,
`VerifySustainability`, `VerifySustainabilityT0`, `VerifyResourceGuidance`).

The child does not own a PC/JVM execution lease. Authored Java checks must not be
reported as executed until the coordinator qualifies the frozen checkpoint.
Compile common and Forge, run `:forge:verifyQuestBackend` and the affected existing
behavior suites using the current exported check classpath. The coordinator owns
the combined candidate's build, packaging, publication and deployment.

A representative production boot is justified by the new Forge event-subscriber
binding, not by another instance of an unchanged recipe. A server boot alone does
not exercise first login, respawn or a real quest click. On an isolated classic
Superflat and ordinary Overworld, still observe first-login book access, all eleven
rewards, full-inventory rejection/retry, reopen/reconnect/respawn and a normal
save/reload; preserve existing worlds. Actual client layout/interaction and the
no-command campaign remain human observations, not source-contract passes.

## Integration handoff

The coordinator owns the root ADR, changelog, README plan, active handoff and
release/deployment ledgers. Record the owner's decided finite entry, lossless
capacity deferral, additive stable quest ID and optional utility-book recovery
there when integrating. This child neither publishes nor deploys.

| Required category | Disposition at implementation handoff |
| --- | --- |
| COMPLETED | Free starter, single-ledger claim batch and utility-book access implementation with focused existing regression coverage; use Git for the exact checkpoint. |
| BLOCKED | Local Java/native qualification requires the coordinator's isolated PC/JVM window; no private/default PC inventory is used by this child. |
| NOT EXECUTED | Combined common/Forge build, affected Java suites, native boot, package/deploy and actual quest-click campaign until separately recorded by their owner. |
| NEEDS HUMAN VALIDATION | Real client interaction/layout and no-command campaign, including retained-world observations. |
| NEEDS MY DECISION | None. Entry method and generous counts are decided. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | No new credential or provider; coordinator-controlled existing PC/JDK qualification is required. |
| COST / BILLING RISK | None. No generation, network provider, paid activation or new dependency. |
| DEPLOYED STATE | Unchanged by this child; source implementation is not an installed or accepted release. |

## Integrated03970 executable red

The coordinator reports real executable failures on integrated source
`03970d7efe9b127c6e1d3a71e1b3d109e75c5aaa` in
[CI39367863670](https://github.com/Ezequiel24123z/grindless-earlygame/actions/runs/39367863670),
Foundation artifact `12240248670`. Its closed behavior log retains the two
failures at lines 4585 and 6777. `VerifyResourceGuidance.surfaces` still required
the obsolete no-kit prose, contradicting ADR-0159 and the actual updated guide.
`VerifySustainabilityT0` reaches `VerifyResourceGuidance.checkedAsset`, which
correctly rejects the old committed guidance document against the changed quest
authority. The coordinator separately reports Registry 1106/0; that is not an
all-Foundation pass.

The focused test repair binds the actual first `FREE` quest, no prerequisites,
ready-without-evidence and already-claimed states, a nonempty basic-supplies-only
reward, the actual translated quest/action names and explicit finite per-player
copy. It retains the machine-owned renewable-supply, night/restart and physical
Matrix protections. `checkedAsset` and its full-document/fingerprint equality
remain unchanged. No gameplay quantity, route, classification or historical
proof is altered to make the check pass.

### Existing regeneration path

The sole PC owner executes the already existing command in the latest integrated
owned root with its selected JDK 17 and process-local Gradle home:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\generate-resource-guidance.ps1 -Root .
powershell -ExecutionPolicy Bypass -File .\tools\generate-resource-guidance.ps1 -Root . -CheckOnly
```

The existing SH twin has the same `--root` / `--check-only` actions. Both use
`tools/dashboard/export` with the resource-guidance action, which invokes
`DashboardExport.resourceGuidance` over `canonicalResult(root)` and
`ResourceGuidanceProjection.project(result)`. The writer owns exactly the runtime
asset `common/src/main/resources/assets/grindless/guidance/resources.json` plus
the ignored execution sidecar `build/resource-guidance/provenance.json`.
Do not edit the JSON, declaration fingerprint or authority/class hashes by hand.
Rebuild resources/JAR after writing: classpath preparation may have processed the
old asset before generation. Then rerun the affected guidance/T0 checks on that
actual generated candidate.

Directly related derivatives remain under their existing owners: the Heracles
archive is generated from the catalogue at setup under
`config/heracles/quests/grindless.zip`, not a second checked-in quest catalogue.
Normal dashboard/audit outputs are build-directory artifacts from the canonical
exporter; the guidance action returns before normal dashboard publication.
No other committed quest/guidance derivative is introduced by these seams.
Neither historical reports nor their hashes are refreshed to disguise this red.

The coordinator requested native regeneration within the existing sole PC owner's
phase 2039; this child has communicated the exact command/output and starts no
JVM, third worker or competing publisher.
Executed repair checks: 23 focused PowerShell source/prose/writer assertions
reproduce the obsolete predicate against the actual guide, match the replacement
contracts and prove `checkedAsset` plus the stale JSON remain unchanged. The
existing link checker passes 726 relative links and 159 ADR sections/index rows.
These are not rerun Java suites or a canonical-generation pass.

| Required category | Repair disposition |
| --- | --- |
| COMPLETED | Obsolete contract repaired on exact integrated03970; existing native writer/output identified and handed to the sole PC owner. |
| BLOCKED | Canonical asset remains stale until that owner's real generation; equality is intentionally still strict. |
| NOT EXECUTED | This child does not execute Java, native regeneration, package or deployment. Source checks are not executable-suite greens. |
| NEEDS HUMAN VALIDATION | Existing real quest/client/no-command and retained-world observations remain pending. |
| NEEDS MY DECISION | None. ADR-0159 and the generous counts remain decided. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | Existing coordinator-owned PC/JDK window only; no new credential or provider. |
| COST / BILLING RISK | None. |
| DEPLOYED STATE | Unchanged by this child; the failed integrated run is not a qualified deployment. |
