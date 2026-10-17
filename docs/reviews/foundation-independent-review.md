# Independent foundation review

Date: 2026-10-17. Reviewed snapshot: supplied source excerpts from `0ca31e3`.
Reviewer: the separate `doublecheck:doublecheck` agent, followed by local reproduction by the
implementation session. **Scope-limited review, not repository-wide independent clearance.**

## Summary

The reviewer initially had no callable local file/Git tools. Its first report therefore
correctly marked the requested claims unverified. Supplying concrete source excerpts enabled
a bounded static adversarial review; it did not authenticate the entire checkout or raw logs.

| Rating / scope | Count | Meaning |
| --- | ---: | --- |
| Concrete static finding | 1 | Installer output declarations did not establish runtime completeness. |
| Other concrete Java defects established | 0 | No counterexample was established in the supplied codec, container, load, inventory or Atlas excerpts. This is not a proof of absence. |
| Independently authenticated execution results | 0 | Build/smoke/hash/port observations remain implementation-session evidence. |

## Finding D1: incomplete installed-runtime tracking

**Source:** `forge/build.gradle`, `installSmokeServer` output declarations.
The initial task declared the two platform argument files, so a missing required library could
leave it up to date. `dependsOn` does not force an up-to-date task to execute:
[Gradle task inputs and outputs](https://docs.gradle.org/8.8/userguide/incremental_build.html#sec:task_inputs_outputs).

**Impact:** a subsequent preparation can fail to repair an incomplete installation, causing
an avoidable smoke failure. The reviewer did not claim stale mod code or a demonstrated false pass.

**Disposition:** addressed under ADR-0128. The task now owns its library outputs and records
an explicit path/size inventory checked before accepting an up-to-date result. A local regression
removed one named generated official Forge jar, ran preparation and verified the restored jar's
SHA-256 equals the original. Merely declaring the directory first did not repair the recorded
installation; that failed attempt is retained, not hidden.

Evidence: `build/validation/installer-repair.log` (failed first approach),
`installer-inventory.log` and `installer-repair-verified.log` under the same directory.
Successful restored SHA-256:
`98a6c0a2318c1788db6f592357eb00d09b15316f054942ba432f71027f074fef`.
This reproduction was performed by the implementation session, not independently by the reviewer.

## Claim/source assessment

| Claim | Assessment | Source / limit |
| --- | --- | --- |
| Explicit integral Matrix ratings and legacy untagged defaults are preserved | Static excerpt reviewed; no defect established | `matrix/ControlMatrixNbt.java`, `item/ControlMatrixItem.java`; actual code/NBT checks separately recorded in the current ledger |
| Process inventories preserve indexed slots and migrate old explicit output positions | Static excerpt reviewed; no defect established | `container/SlottedContainer.java`, `machine/ProcessMachineBlockEntity.java`; compact historical missing positions cannot be recreated |
| Quest evidence counts offhand once and requires a real named rated Matrix | Static excerpt reviewed; no defect established | `quest/QuestInventoryEvidence.java`, menu delegation; no real-player claim independently observed |
| Atlas labels preserve base identity and exact rated matching | Static excerpt reviewed; no defect established | `recipe/AtlasLogic.java`, lookup and JEI excerpts; client screen still requires observation |
| Initial installer declarations guarantee a complete runtime | **DISPUTED** | Finding D1 and the local deletion regression; repaired by explicit inventory |
| All requested automated checks pass on the final revision | Not an independent-review conclusion | Read the revision-bound [execution ledger](../testing/latest-validation.md), not this static report |
| Eight remaining dev-launch threads belong to one proven upstream component | **UNVERIFIED** | Thread dumps show the hang, not exact ownership; unrelated web issue suggestions were not accepted as evidence |
| This work accepts expanded T0 gameplay | **UNVERIFIED / not claimed** | Human survival and client acceptance remain outside the static review |

## Adversarial limits

Only the supplied source was reviewed. No additional private code was sent to public search.
The reviewer did not run builds, inspect real world files, check every datapack command or
authenticate raw execution logs. Historical Linux aggregate successes were explicitly excluded
after the separate dispatch defect was discovered. The current Windows inventory must stand
on its own completed scenario evidence.

This review accelerates human verification; it does not replace it. The same underlying model
family may miss defects. Supporting sources are not a guarantee of correctness, and absence of
a counterexample is not proof. No broad T0 GO or merge authorization follows from this report.
