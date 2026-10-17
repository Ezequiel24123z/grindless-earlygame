# Active T0 workshop handoff

Updated: 2026-10-17. State: **IMPLEMENTATION IN PROGRESS**.
Branch: `ezequiel24123z-tier-0-rework`. Preserved foundation: `c62de724632cce158988ddce62d4669619398f13`,
fast-forwarded from the published foundation branch without changing that branch or main.
Repository authentication was verified as `Ezequiel24123z`; the exposed Copilot identity remains
`t-ecastano_microsoft`. The worktree was clean at intake and had no unpublished commits.

The current owner-directed assignment supersedes the old no-expansion gate. Implement the
ADR-0129 ceramic workshop, then assembled settling, with current-build checks and checkpoint
commits. Manual client/survival/retained-world acceptance remains a merge condition, not a
feature-development blocker. The owner additionally authorized PR management and merging on
2026-10-17; merge only after all required automated and observed manual conditions pass.
Unrelated T1/T2 work remains outside scope.

Completed checkpoints: `9aea30e` (active contract), `0838e88` (exact Windows/Bash smoke
selection), and `0aad62a` (W1 workshop). The active draft is
[the T0 rework](https://github.com/Ezequiel24123z/grindless-earlygame/pull/54), based on the
preserved foundation. `0838e88` was HEAD when W1's final
working-tree validation began; use `git rev-parse HEAD` for the containing commit, not a
self-referencing guessed SHA.

**W1: AUTOMATEDLY VALIDATED, MANUAL ACCEPTANCE PENDING.** Quern, Clay Mixer, the formed
Firing Chamber, eight F0 routes, three ceramic items, recycling, early transport/plumbing,
complete Bootstrap Atlas routes and non-clipping guide pages are implemented. The shared
engine keeps work identity, wakes for fluids, reserves outputs and retries blocked/idle export.
Registry identities, Matrix ratings, existing menu ordinals and indexed inventory layout remain.

The final unified validator passed environment, full build, **34 suites** and documentation:
`build/validation/20261017T142252076Z/status.tsv`. The final production `workshop` scenario
passed all **19 markers** and natural shutdown: `build/validation/workshop-final-smoke.log`.
Built/loaded jar SHA-256:
`3005B11CFC748FECBE7B542DABB8C16C12590344059A6BC0190659CAD25826DC`.
`states` and the 16-marker Matrix lifecycle also passed during the W1 batch; their exact scope
and the initial workshop timing failure remain in the validation ledger. Full final integration
and observed client/survival/retained-world acceptance are not claimed. No smoke server remains.

The local JDK is restored. Fabric Maven timed out on online resolution, so only dependency
caches were copied from the preserved foundation into this worktree; no world, source,
configuration or old build output was copied. Normal validation then worked. Keep `JAVA_HOME`
on JDK 17 and `GRADLE_USER_HOME` on this worktree's `.gradle/user-home`.
Image keys remain absent; 46 T0 PNGs pass technical QC and the original generator supplies the
functional assets. No AI image generation or final graphical acceptance is claimed.

**Next safe task: T0-W2, assembled settling.** Add the fixed open three-block trough and its
oriented static parts, dirt/clay/gravel/water route, real geometry, quantity/formation checks
and native lifecycle. Then complete the final asset/automation/integration milestone and keep
a draft PR. No unrelated T1/T2 consumer is authorized; merge permission does not waive
the outstanding acceptance conditions.

## Current continuation: sustainability and classified side streams

The owner expanded the active task under ADR-0131. Audit all authored progression resources,
add canonical resource/by-product metadata and deterministic reports/CI gates, expose it in
player views and a repository-driven dashboard, and implement the first missing mod-owned T0
renewable loops. Do not stop gameplay work at schemas or dashboards.

The W2 implementation is now present: four-orientation settling, stateless parts, shared
collision/model boxes and a generated preview. Its fast validation passed at
`build/validation/20261017T151215457Z/status.tsv`. The native batch passed states, the repaired
workshop and book fixtures; settling still fails `TROUGH-WATER-RECOVERED` after producing solids
and retaining exactly 750 mB. Evidence: `build/validation/settling-native-smokes.log`.
That failure is preserved, and ADR-0132 now repairs it: the unified validator and all three
workshop/settling/Matrix scenarios pass in `build/validation/fluid-output-recovery.log`.
The exact volume assertion remains, with input protection, legacy quota and output ownership
reconstruction coverage. This closes the transfer, not upstream resource sustainability.

The dashboard renderer child delivered `e2c2a0a` / `0cea296`, integrated as `40d5260` / `a533888`.
Its code and 26 synthetic browser checks are scoped to rendering; there is no production
`data.json` yet. The inventory worker returned a 315-process default-material projection and
50 authored crafts as source-backed summary, not the requested JSON artifact. Implement the
canonical exporter/auditor directly. The child's README/CHANGELOG ownership has ended.

The initial supply review is [resource sustainability](../docs/RESOURCE-SUSTAINABILITY.md).
Vanilla-derived dirt/wood/carbon and finite ore supply are not certified renewable. The next
code milestone adds real internal sources and an audit, with exact pre-existing exceptions only
if they have a versioned remediation plan. Current gate remains IMPLEMENTATION IN PROGRESS.

## Preserved foundation handoff

Historical evidence only; the restrictions and next action below are superseded by the active
owner-directed handoff above and ADR-0129.

Updated: 2026-10-17. Phase: bounded foundation remediation complete; broad T0 work held.
Repository account: `Ezequiel24123z`. Exposed Copilot context: `t-ecastano_microsoft`.
Branch and remote: `ezequiel24123z-agent-ready-t0-foundation`, `origin` tracks the same branch.
Authorized baseline: `0d798607c66ff7e4b581d2786cfa32dc4a02ded7`.
Last executable HEAD: `e2dfc2c847dd220178572b83e7516a9949bb8b58`.
The commit containing this handoff is a later evidence-only checkpoint; use `git rev-parse HEAD`
for that commit's own identity rather than treating the tested revision as a self-referencing SHA.

## Persisted units

| Commit | Unit |
| --- | --- |
| `e55c518` | Sanitized historical verification integration; original `bcdb162a` / `46eb83fe` remain on the evidence branch. |
| `735daa4` | Gradle-resolved behavior-check classpath; Windows long-path/cache failure repaired. |
| `b07ed16` | Atlas/JEI explicit F1/F2 visibility, stable recipe IDs and rated lookup. |
| `b48e3f6` | Slot-preserving NBT, explicit legacy Assembler migration, Matrix codec, offhand count and full registered-item lifecycle. |
| `0ca31e3` | Production-artifact smoke runtime with natural exit checks; optional dev-launch hang preserved as a limitation. |
| `3eb7351` | Bash report-variable dispatch bug fixed with a real false-green regression fixture. |
| `38f8691` | Pinned research, reuse screening, proposed T0 contract/graph, asset manifest/prompts and exact human checklist; no new gameplay. |
| `9fd6d41` | Unified validation/CI entry, stage ledgers, installer completeness repair and unchanged Shadow version centralization. |
| `e2dfc2c` | Stable Planner item fixture after the transient Linux entity-selector failure. |

## Gate and evidence

**CONDITIONAL GO**, not GO for broad T0 or mechanical Tier acceptance.
Read [the gate](../docs/reviews/go-gate.md),
[current validation](../docs/testing/latest-validation.md) and
[per-scenario results](../docs/testing/integration-smoke-results.md).

- 33 behavior/resource suites, full build and documentation pass. Four Atlas and five inventory
  failures were characterized before their fixes.
- 41/41 distinct Windows production smokes completed. Two aggregates were interrupted after
  23 and nine completed cases; bounded synchronous batches closed the inventory. Never rewrite
  those interruptions as successful aggregate exits.
- Built/loaded jar SHA-256:
  `e0267ee23fdcfde776749df069e29effc72379dfca3b4fb58dc0b0e95a307866`.
- Raw logs: `build/validation/`; server logs: `forge/run/smoke-<scenario>.log`.
  `complete-smoke-inventory.csv` and the committed scenario table preserve exact coverage/hashes.
- 25 existing T0 PNGs pass manifest QC. Isolated regeneration matches 1,377 files; no art was
  replaced. Image-provider keys were absent, so no generated candidates are claimed.
- Independent supplied-excerpt review found one installer completeness issue; it was reproduced
  and repaired. The review had no local-file tools and is not repository-wide independent clearance.
- Linux PR CI `39030528423` at `e2dfc2c` completed successfully: unified verify plus six smoke
  shards. Failed/cancelled runs remain in the ledger. Read live checks before a merge decision.
- No local background check/server remains; smoke port 25599 is closed. Worktree is intended
  clean after this evidence commit; never discard unexpected user changes.

## Limitations and next action

The draft [pull request](https://github.com/Ezequiel24123z/grindless-earlygame/pull/53) is open;
no merge is authorized. Registry IDs and `Rating` remain unchanged. New indexed inventories
prevent future slot loss; legacy compact saves cannot reveal their lost sparse positions.
Back up any real world before migration or downgrade testing.

**Exact next task:** perform and record
[the no-command Bootstrap/client acceptance checklist](../docs/testing/t0-survival-acceptance.md)
with the validated jar, including actual F2 quest interaction, reconnect and retained-world
restart. No command fixture or item packet alone closes that requirement.

The proposed richer T0 specification is not finalized gameplay; timing/expansion decisions
must be reconciled with the current Bootstrap contract after acceptance. Do not add T2
consumers, materials, machines, logistics or a T0 multiblock while the gate is conditional.
The optional development server shutdown hang, two unused missing script twins and deferred
hero art/macOS execution are documented rather than claimed fixed.
