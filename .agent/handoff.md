# Active foundation handoff

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
