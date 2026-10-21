# T0 native FTB questbook qualification

Engineering evidence for ADR-0171. The replacement uses native FTB graph,
tasks/rewards and the client screen, not the old custom guide widget. This record
does not declare client rendering, actual recipe clicks, owner-world upgrade,
manual play or T0 acceptance passed.

## Source and actual execution

| Boundary | Source / observed result |
| --- | --- |
| Initial application |173673690d5df3720d77d0e4295c48587fa2b1e9: real Forge compile red, two static calls to an instance Matrix method and an EventActor return |
| Corrective application |a181b9e: actual main Forge compile succeeds; a separate tool-file abstract-method fixture fails and is retained |
| Current native proof source |`ff610a7a02e3f66edd120b8c719f599e3fc48857` |
| Common contracts at ff610 | Claims336, Quest all, registry1330/0 with22 authorities/mutations, guidance25594, ports2093 and smoke harness pass; Windows explicitly skips the inapplicable POSIX inaccessible-file case |
| Parse-only performance repair | The prior owned check is CANCELLED after14minutes with captured classpath-canonicalization stack; empty parse paths retain all authorities/mutations/equivalence and complete in about23seconds. No cancelled run becomes a pass |
| Native standalone data contracts | Later tool-onlyd73175f:864 production-definition assertions plus a vanilla surrogate mechanism total2352 pass. Surrogates are not production mod-item registration or client evidence |
| Pack data contracts |152 assertions pass; final artifact/package delivery remains separate |

The original transcript did not retain every native compiler stdout. Its actual
SDK result and the failed post-build daemon-settlement guard remain distinct.
The follow-up inspects state without replaying successful compilation; no
stronger log is reconstructed from an empty transcript.

## Real registered Forge and disk restart

The actual production server loads the exact Grindless/Architectury/JEI and four
FTB jars. The executor checks complete mod inventory before both starts.

| Actual server phase | Result |
| --- | --- |
| Registered native graph/API/filter/import | `GRINDLESS_FTB_NATIVE_PASS quests=46 rewards=36 legacy=46 assertions=1099` |
| Persistent seed | `GRINDLESS_FTB_NATIVE_SEED_PASS legacy=2 receipts=14 advance=3 awaiting_stop=1 assertions=56` |
| First close | Unmodified canonical harness exits0; server saves/stops naturally with `SMOKE_KEEP_WORLD=1` |
| Second process, same diagnostic world | `GRINDLESS_FTB_NATIVE_REOPEN_PASS legacy=2 receipts=14 advance=3 assertions=58` |
| Second close | Harness/server exit0 and natural save/stop; no owned JVM remains |

There are26 logical material-reward tasks but36 native ItemReward objects because
the starter's eleven stacks have separate native receipts. This is not another
set of extra rewards. Individual receipts, shared completion, pending versus paid
states and the old mint fence are checked separately.

The seed uses actual `ServerQuestFile`/`TeamData` and native save APIs. It makes
additional progress **after** the explicit save, so natural server shutdown must
persist the later dirty value. The reopen command refuses same-JVM reuse and
requires the native file loaded by real server startup; it checks all disk state
**before** import can repair anything, then proves repeated import changes none
of those native dates/progress/pending/paid receipts. It does not silently reseed.

The saved diagnostic identity is not a real player team; it is deliberately
isolated and cannot become the owner's reference world. These are real native
disk/lifecycle observations, not human login/team UI or physical reward delivery.

## Evidence binding

The core retains closed raw files under its ignored relative run directory
`build/ftb-validation/20261021T204938314-Native/`.
The parent reads the complete local transport and observes the actual seed/reopen
markers plus the structured PASSED result. Direct copied server-file custody
verification remains part of the final handoff, not inferred from the summary.

| Artifact | SHA-256 |
| --- | --- |
| Native transport | `4ebf5e364f08dfa8afc92bf7fdae4a1089c064b4009ff1950a09c8d1a563b383` |
| Closed receipt reported by core | `e66197aca60b8168ec255f416da4407a3cfda3c6efd5522df1def03f441c1824` |
| First actual server log | `77af16f2b32cbb7d902f5e376f543fb91f70106f0f343957bf89dd0a001e5dbe` |
| Reopened actual server log | `bc245d51a6c20e32a96e40fc1fa31276f448576eef532c4334cac3a7b858f484` |
| Retained origin receipt | `a58fa239e448a2601f8dd3d3b8f2ebc4f57215209c9ab01c0a6104e06a6e8862` |
| Canonical first harness | `a30d5945b8e39c9f347e400bef92e6c838ce616f54a7c7b6a63e6a38f0d86389` |
| Private bounded reopen harness | `e99d59af507df844e2e4138381e6a8f1c8d582a78808c76c3665156d838c2962` |
| Actual staged Grindless JAR | `29bcce43372016719fe1202b8859a18c93147562c04a2ef1daf163fddf7cc54f` |

The private reopen helper validates the first actual successful stop, source,
seven jars, retained world and native data files before admitting phase2. It
replaces only the canonical top-level world reset with exact retained-origin
validation; startup/assertions/timeouts/handshake/shutdown/KEEP_WORLD behavior
remain intact. It is ignored evidence, not a new public runner flag.

Exact FTB binaries and the required native JEI provider are recorded in
[the integration reference](../design/ftb-questbook-integration.md). Corrected
Sync/shared-batch tooling has source closure plus actual mock/twin results,
not a substituted full Forge proof.

## Existing failures and remaining boundaries

The first compiler, fixture/registered-item boundary, old prose, smoke inventory/
dispatch, parse-performance cancellation and daemon-custody failures remain
recorded. The completed ff610 Linux run also finds the obsolete exact
`Players`-only persistence-root oracle after the intentional native-owner schema
extension. Update the exact expected schema and retain the original claim/reload
invariants; do not disable the assertion or call that CI green.

Before player delivery: close that targeted persistence oracle, package the exact
qualified runtime and public data, inspect hashes, return the exclusive build
lease, and use the stopped-world backup/copy/update boundary. Load a separately
admitted verified F-world clone if needed to establish changed-mod registry/
compatibility behavior. The synthetic diagnostic origin is not an owner-clone
origin and must never be loosened/relabelled to pretend it is one.

The actual DevF world, old preparation world and launcher settings have not been
modified by these tests. Real GUI/JEI clicks, player inventory submission/delivery,
login/party interactions and final subjective readability remain owner/client
observations. No normal campaign, release, merge or T0 acceptance is supplied here.
