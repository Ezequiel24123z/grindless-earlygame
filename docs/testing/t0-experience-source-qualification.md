# Integrated T0 source qualification — Phase A

Executed on 2026-10-20. **Compilation, normal generation and the complete
standalone integration gate pass. Native/client/Prism qualification is not
implied. The required graphical gate remains BLOCKED, not waived.**

## Exact source and execution boundary

- Published intake: `26ac1eb465fc7ad1a8f6235f5b92190ed454a9b4`.
- Repaired source at the full common gate:
  `1101313cadcb3965cf5900af3740a21c322b6bef`, plus its actual normal generated
  outputs, subsequently committed without rewriting their bytes.
- Complete generated checkpoint and final backend/clean-PC source:
  `db5a8e91678cad5d474e12c7902f9c5a418f1bdd`.
- The containing handoff commit changes proof/context only. Reuse the completed
  executions for identical executable/data inputs under ADR-0140; do not replay
  them merely to relabel a receipt with the documentation commit.

One new detached execution surface was used. The original clean70901 clone,
personal origin/authentication and independent economy surface were not changed
as execution targets. No reset, clean, execution-root deletion, shared-history
rewrite, push, PR/main merge, package deployment, Prism/world access or GUI simulation
was performed. All owned source/build/raw evidence remains available.

Main Gradle stages used3GiB, CPU2, one worker, no parallelism/daemon and SerialGC,
after fresh zero-Java and6GiB physical/8GiB commit/8GiB work-and-cache guards.
Compiler/check JVMs were bounded to768MiB/CPU2; the existing guidance/economy
wrappers retained their768MiB/two-worker Gradle and256MiB compiler settings.
Strict version probes used the appropriate process-local Java environment,
not relaxed validation.

## Actual automated result

The **one** post-repair `tools/run-checks.ps1 -Root .` execution runs all
**52 autodiscovered suites**, exit0, from19:57:09Z to20:05:55Z
(526.065seconds). There is no second full PC run.

| Actual check | Result |
| --- | --- |
| Common/Forge compilation and resolved check classpath | exit0 |
| Final `:forge:verifyQuestBackend` | exit0; **776** real Heracles contracts |
| Basic Tank | **166**, zero failures; not native Forge bucket proof |
| Workshop ports/models/orphan/shared teaching | **2075**, zero failures |
| Legacy Guide owner/input selection | **14**, zero failures; registered settlement is native-pending |
| Generic quest claims/inventory/lessons/ledger | **279**, zero failures; positive registered F1 is native-pending |
| Quest catalogue, Bootstrap, Recipes, Atlas, persistence and full Assets | all pass |
| Shop | **33230**, zero failures |
| Economy | **417**, zero failures |
| Development telemetry | **29**, zero failures |
| Machine inspection | **409**, zero failures |
| Quern/shared physical audio mechanisms | **101**, zero failures |
| Resource guidance | **25500**, zero failures |
| Registry/source binding | **1164**, zero failures |
| Conditional T0 sustainability | **2834**, zero failures; not Tier acceptance |
| Actual dashboard projection/counterexamples | **76991**, exit0; no browser/artifact claim |
| Actual cross-root exporter selection with javac | **44**, exit0 |
| Windows physical-audio source/resources and negative controls | **47 + 30**, exit0 |
| Affected Python visual `InventoryContracts` | **10 cases**, exit0 |

The runner verifies the composed fixture counts, not a guessed model:
**199 generated recipes /74 Assembler /255 Atlas /28 shaped Bootstrap /
33 primitive construction projections per recipient**. These are the checked
material-fixture/catalogue counts, not a promise about every external tag pack.

The final Gradle command, on clean generated source, is:

```powershell
cmd.exe /d /s /c 'gradlew.bat :common:compileJava :forge:compileJava :common:processResources :forge:processResources :common:writeCheckClasspath :forge:verifyQuestBackend --no-daemon --no-parallel --max-workers=1 --console=plain "-Dorg.gradle.jvmargs=-Xmx3G -XX:ActiveProcessorCount=2 -XX:+UseSerialGC"'
```

It exits0 at20:16:35Z. The whole JVM property is quoted; no malformed
`.gradle.parallel=false` task or Forge replacement/stub is used.

## Normal generation and exact bytes

Actual existing entrypoints:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/generate-assets.ps1 -Root .
powershell -NoProfile -ExecutionPolicy Bypass -File tools/generate-resource-guidance.ps1 -Root .
powershell -NoProfile -ExecutionPolicy Bypass -File tools/generate-resource-guidance.ps1 -Root . -CheckOnly
powershell -NoProfile -ExecutionPolicy Bypass -File tools/economy/export.ps1 -Root .
powershell -NoProfile -ExecutionPolicy Bypass -File tools/economy/export.ps1 -Root . -CheckOnly
```

The JDK-only writer generates**1548 assets**, including six real port
blockstates, block/item models, loot, mining and state-smoke output. The normal
`WorkshopPreview.write` output is preserved too. No model/loot JSON is authored
by hand. Guidance writer and independent CheckOnly JVMs both pass
**73 declarations /5179 comparisons**; economy writer/check both pass
**416 checks /17 offers** on the actual46-quest/six-port composition.

The existing embedded-Python wrapper runs `t0_visuals`, with an exact source
revision and owned output/sheets directory. It reads actual generated port
models/metadata and reports**25 blocks /71 models /281 variants** including
six ports; legacy19/65/257 remains separately identified. It also reports585
distributed texture PNGs,108 resolved T0 PNGs and14 standalone icons.
The manifest now binds the actual barter screen/menu, harmless Guide redirect
and shared native/Heracles teaching sources, not obsolete baseline placeholders.
All human/client approval flags remain false.

**45 allowlisted normal files** are transferred with a raw-hash manifest,
verified before and after import. Thirteen contact sheets are byte-identical;
the updated720x256 workshop diagram is a JDK projection, not Minecraft imagery.
No private helper/sidecar enters Git and no old5002 cost JSON is adopted as the
new authority. Canonical generated JSON keeps LF through a real command-scoped
Windows `core.autocrlf=true/core.eol=crlf` checkout test and matches raw Git blobs.

| Normal output | Bytes | Raw SHA-256 |
| --- | --- | --- |
| Runtime guidance | 586067 | `536976d859a0adf787b57f415a14d8a2f86a0e4f27b0a40fe57d1cad9b53ed56` |
| Guidance content identity | — | `d6949e94ec1592739f9e3ce250433d9bf7cf9a76306cc7429a0f1844f4d13619` |
| Canonical economy | 178854 | `d42310134a8148c9cdd6c381de91fd47336c96886e2b0e3d09b7838268c2d127` |
| Actual visual inventory | 363560 | `f2459595c45cec5fe1dc6b5f1870846f1ed2e257362360f53501b93a78cc329a` |
| Normal workshop diagram | 8196 | `bb5570215d544f67aeaa98fab950bc4b4da7bc034cc8ee4de58c8b901360bdbb` |
| Complete45-file manifest | — | `1b2eef30d802ce86d8a1412c3c0d49d70197ef6dbe1e03d6baf62e597c24143c` |
| Complete normal-output archive | — | `827a18060e0d832dd814c65366c8deabe9480921d278b489e032e101f6c4fdcd` |

All nine material/core sprites, three books and Multitool remain unchanged.
The four protected Oggs retain their exact raw hashes:

| Ogg | SHA-256 |
| --- | --- |
| Quern | `811b29471e3fa6a51ce76db2241259d02adfc64e244eadc096ef376d1d1970d5` |
| Mixer | `2c052cd678775f237fbf1a939037006ed26d05588e99a4ff29bbffcd66599f45` |
| Water | `5fbfa5c1906fcc98ac310f3feaa9959fd7838ef5f4f6143dc293d37eadc6aeb1` |
| Extraction | `a634f9a17c5f5c71e7fd20da8f79ec5453715d125b1084c557de54bb380ba9fb` |

All five retired synthetic runtime Oggs remain absent. No provider, inference,
codec regeneration, model download or installer is used by this qualification.

## Repairs, retained reds and proof reuse

- Exact26ac hosted CI39482095570/artifact12292750450 has a real seven-suite
  behavior red. Its raw log is retained, SHA-256
  `9e393ca5375c1d6ba279c99f44a20672c448836aa03fbc7fee0e77aa887b8b2a`.
  Reuse that diagnostic instead of running another known pre-generation full red.
- The PC guidance writer exposes the stale ProcessMachine crank AST too.
  The repair retains its exact F0/server/clamp body and actual positive-delta
  telemetry after admission, and rejects ten concrete source mutations.
  No assertion/guard is skipped.
- New intrusive Items/Blocks cannot be constructed after vanilla's freeze;
  the attempted before-bootstrap ordering also really fails `Not bootstrapped`.
  These failures are not rewritten as successful fixture runs. Registered-object
  assertions move to the existing real Forge probe below. Generic claims,
  vanilla NBT/input selection and orphan behavior remain executable in common.
- The next focal orphan check rejects Stone's missing horizontal-facing
  property. An existing registered Furnace state fixes that unbound BE fixture,
  not a fake port. The complete focal2075 and subsequent full gate pass.
- Direct embedded-Python invocation rejects the missing module search path;
  the existing wrapper resolves it without installation. The first composed
  presentation regression rejects guessed shop package paths; actual
  `shop/client/ShopScreen` and `shop/ShopMenu` paths repair it, followed by10/0.
- The first compile collector records a null exit and a transient closing JVM,
  despite real `BUILD SUCCESSFUL`/Heracles776. Its original receipt is kept;
  it is not labeled exit0. The final bounded compiler/backend gate has actual0.
- Auxiliary ZIP assembly/type and output-allowlist refusals are retained.
  The omitted diagram is attributed to the normal JDK writer before import.
  Fast-forward succeeds without reset/clean;929 residual stat entries are
  refreshed only after **every Git blob matches exactly**, with no source-byte
  change or staged content. No successful full JVM gate is replayed for observers.
- Offline sound695 WinPS/Core assertions are reused from source1789ce9 after
  **17 exact Git-blob input comparisons**. The current47+30 Windows contracts
  and101 Java audio checks are fresh. A new Linux/SH execution is not inferred.
- Batch tooling's three new cases/98 assertions and unchanged old48-case proof
  are reused after the three runner/test blobs match3564fe7 exactly.
  The historical full**33/51 timeout FAIL remains FAIL**, with no15/60/120-second
  deadline increase. Fresh complete hosted CI after publication remains required.

## Native properties are required, not disabled

The same isolated [`workshop-ports` scenario](../../tools/smoke/workshop-ports.expect)
now requires**26 markers**, preserving every original23 predicate and adding:

- `WORKSHOP-INTERFACES-PORT-GEOMETRY-OK`: six actual registered block variants,
  facing-only state and every rotated native collision.
- `WORKSHOP-INTERFACES-LEGACY-GUIDE-OK`: actual registered Bench/Assembler,
  exact retired paid owner, blocked/no-loss settlement, one real Guide,
  exact input debit, cleared oversized progress/owners and no second retired start.
- `WORKSHOP-INTERFACES-QUEST-F1-OK`: actual registered Matrix/Quest Book,
  malformed/wrong/split/untagged evidence, actual public server-thread claim,
  four-versus-five F1, held-book requirement, unchanged evidence, two flint once,
  historical/per-player chapters and the real ledger serialization contract.

These use the existing console-only non-OP Forge probe after normal deferred
registration, not intrusive-holder flags, Unsafe, fake items or a new framework.
The original synchronous P2 callback/rebinding and raw750/quota750 predicates
remain required; old Tank120 does not prove them. Native execution is
**NOT EXECUTED in Phase A**. The real projector/Atlas/localization/resource
checks and AST hooks to both quest renderers pass, but do not render a GUI.

Phase B must build/package and run this same-G isolated scenario before the
dedicated Prism deployment/Inspect/idempotency/preservation checks. No old
A01/A03/full native-matrix replay is requested here. Required controlled
graphical launch/world/interaction/save observations remain BLOCKED until an
authorized actual graphical path exists; no longitudinal campaign or release
acceptance follows from this source gate.

## Closed evidence and custody

Private relative evidence root: `build/validation/phasea/`.
All23 command receipts and46 raw stdout/stderr files are closed and their
hashes verified again after transfer. Both the initial44-file archive and the
complete45-file supplement remain, alongside all real reds and source bundles.

| Closed evidence | SHA-256 |
| --- | --- |
| `22-full-common-checks.receipt.json` | `8b12c6322fc26e5e520e5dde68e3bf96180ee7350e4c88dd29bf6476c8a66b52` |
| `22-full-common-checks.stdout.log` | `0cff37028f5d3c36ddb60f1fae8ef291995c43a522969de80e722aed150fde4d` |
| `23-backend-final.receipt.json` | `49f66a7fd63033bf08d1b957fd67fc1f2ccba3ebfd5ffe5d9d0dd12d46f7b4fe` |
| `closed-evidence.json` | `684a051509e4e6108c29d21fa146bcd69ba8911b313c8b4be37ad85105bd490f` |
| `closed-evidence.zip` | `e94ce169bac25fde682620836bbe5e004675165f1a6b349f4174eb76581c0555` |
| `proof-reuse.receipt.json` | `35a1b990fcf98c261fe7f946bc90c8488f2e081df6f28dad4ed5bcfb67e18e4d` |
| `protected-and-checkout.receipt.json` | `fdde5aa7bd7c96188d86485724329eec8f10333bed2e75e2796d078bc8a80bf0` |

Closeout observes**0 Java /0 Python /0 root-cache locks**, clean generated
source and original clean70901. The independent economy root remains present
and unmodified by this task. Retained outputs include585 common classes,
53 Forge classes and82 focused check classes covering all52 check entrypoints.
Official wrappers keep their existing temporary-directory cleanup; the retained
execution/build/source/class/evidence roots are never manually removed.

| Mandatory disclosure | Phase A handoff |
| --- | --- |
| COMPLETED | Actual compile/generation/full52/backend776 and affected projections; committed normal source and raw-hash receipts. |
| BLOCKED | Required real graphical gate: no authorized path; not waived. |
| NOT EXECUTED | Same-G26-marker native probe, fresh hosted/SH CI, package/Prism deployment/Inspect, normal campaign, release/tag/main merge. |
| NEEDS HUMAN VALIDATION | Actual client interaction/save, appearance, sound/listening and pacing; no human approval claimed. |
| NEEDS MY DECISION | None for this bounded source outcome. |
| NEEDS CREDENTIAL / EXTERNAL RESOURCE | Authorized graphical access; no new secret discovery, installation or provider grant. |
| COST / BILLING RISK | No new provider/image/audio request or model download; historical spend remains separate. |
| DEPLOYED STATE | Unchanged by Phase A. Prior747/c08 deployment is not a new-G Prism verification. |
