# Repository dashboard renderer

This is the **static renderer**, not a second catalogue, audit engine or acceptance authority.
The canonical generator supplies `data.json` beside
[`index.html`](../../tools/dashboard/index.html). No production snapshot is included in this
renderer checkpoint. Missing data produces an actionable error, never a synthetic fallback.
Integration of the generator and audit belongs to the active T0 work; gameplay development is
not blocked on dashboard polish.

The governing rules are
[ADR-0131](../DECISIONS.md#adr-0131--mandatory-supply-is-mod-owned-renewable-and-explicitly-classified),
the [active handoff](../../.agent/handoff.md) and the
[development/merge gate](../reviews/go-gate.md). This renderer neither changes their status nor
claims that current automated checks establish no-command survival, client interaction or
saved-world compatibility.

## Serve and inspect

Serve `tools/dashboard/` over HTTP from a single origin after the generator writes `data.json`.
Open `index.html`; opening it as a local file cannot reliably fetch JSON. The four runtime files
are ordinary HTML, CSS and JavaScript: no Node, package installation, external library, font,
build framework, network API or deployment service is required.

The page explicitly displays repository, branch, complete source commit, generation timestamp,
mode, project status and reported PR identity. Local mode says **provisional**; PR mode also
retains that qualification. Stable mode says **generated snapshot**, not live monitoring.
Refresh performs one uncached same-origin fetch. It does not run a generator, audit, build,
CI check or manual test. Failure clears the previous visible snapshot rather than presenting
old evidence as current. There is no timer, invented live feed or browser storage fallback.

## Schema-1 integration contract

The agreed top-level collections are `context`, `classifications`, `tiers`, `resources`,
`routes`, `machines`, `quests`, `assets`, `validation` and `findings`, with `schema_version: 1`.
The renderer consumes the coordinator's resource, route, machine, quest and Tier fields without
reclassifying their meaning or changing quantities.

- `context` is required. A wrong schema, non-array catalogue, missing catalogue entry id,
  duplicate catalogue id or out-of-range Tier id is an explicit load error.
- An absent/null collection is **not reported**, not proof that the project has no such entries.
  Empty arrays say **none reported**. Unknown status strings remain visible with a neutral badge.
  Null tiers and unknown booleans are never coerced to T0 or false.
- Tier ids are integers from 0 through 15. All sixteen Tier routes remain navigable; an absent
  Tier has an explicitly unreported name, plan, implementation and acceptance state.
- Classification badge labels, descriptions, localization keys, colors and icons come from the
  supplied dictionary and matching `resource` or `byproduct` domain. An unknown id stays visibly
  unreported. The renderer never certifies a renewable route by graph reachability.
- References use their supplied ids. Input/output objects retain `resource_id`, `amount`,
  `consumed` and `role`. Nested records such as handling, conditions, automation, future plans
  and persistence are rendered as labeled data, not interpreted as a new gameplay rule.
- `assets.entries`, `generation_queue`, `qc_queue` and `checks` accept objects with
  `id`, `name` or `title`, reported `status` / `validation_status`, `kind` / `type`,
  `source_path` / `path`, and description/summary/details. Optional dimensions remain in detail.
- `validation.checks`, `ci`, `manual_acceptance` and `required_deliverables` display named
  objects with `id`, `name` / `title`, `status`, `source_commit`, `evidence` and `details`.
  A primitive entry is visible but has no invented state, revision or evidence.
- Evidence accepts a repository-relative path, an HTTPS URL, an array of references, or
  `{source_path|path|url|reference, summary}`. Raw logs are not embedded. The generator remains
  responsible for public, sanitized summaries and an exact evidence revision.

### Deliverables, not subjective percentages

The only completion-like value is **numerator / denominator** for explicitly supplied
`validation.required_deliverables`. The denominator is the number of supplied entries. The
numerator includes only objects with one of the exact case-insensitive statuses `pass`,
`passed`, `success`, `complete`, `completed`, `done`, `accepted`, and a `source_commit`
exactly equal to the snapshot's hexadecimal commit identity. A missing/empty list yields
**no completion measure**, not a zero-percent completion claim.

This is explicitly **reported completion at this commit**, not independent acceptance.
Historical/unbound passes, skipped/cancelled checks and waived debt do not enter the numerator.
Every required entry and its evidence remains inspectable. No percentage, subjective phase
weight, estimate or hand-maintained status is added.

## Views and navigation

| View | Reported content |
| --- | --- |
| Overview | Revision identity, required deliverables, T0-T15 ledger, findings/debt, milestones and supplied recent commits |
| Each Tier | Purpose, entry/exit, implementation, bootstrap, renewal/improvements, recycling, useful side streams, eliminated vanilla dependencies, finite optional sources, deadlock protection, linked resources/machines/quests, blockers and manual checks |
| Resource sustainability | Search, first-use/renewal tier, dictionary class and mandatory filters; complete resource detail |
| By-products and waste | Dedicated side-stream table; current/future/potential/recyclable/hazardous/inert/disposable/spent/mandatory/unresolved scopes; current/future consumers and handling |
| Production dependencies | Resource/route graph including tags, environmental sources, declared resource dependencies and exact route input/output roles |
| Machines and multiblocks | Kind/tier/search filters; ordinary machines, generators, storage/logistics, parts, standard and assembled multiblocks remain distinct |
| Logistics and automation | Explicit logistics/storage kinds plus entries with reported automation; interface/conditions/persistence detail |
| Questbook | All dependencies, physical subject/count, reward, optional state and lessons; separate dependency graph with non-gating semantics |
| Assets | Reported entries, generation queue, QC queue and asset checks |
| Tests and acceptance | Required deliverables, repository checks, CI, manual acceptance, Tier blockers, findings and waived debt |

Filters and the selected detail are shareable hash state; a reload retains them. Filtering does
not steal focus from the control. Native links, inputs, checkboxes and selects support keyboard
operation. View changes focus the heading; selecting an entry focuses its detail heading.
The skip link targets the main region. Tables and diagrams scroll inside labeled keyboard
regions rather than widening the entire mobile page.

### Graph boundaries

The diagram is explicitly a **filtered neighborhood**, not the complete factory. Any reported
or unresolved referenced node can be selected as its center. Depth is one to three connections;
the display limit is 48 nodes. Shown/total node and edge counts, eligible-edge counts and a
limit warning expose what is omitted. A complete route table and full quest table remain
available alongside the bounded diagrams.

`canonical: false` marks optional/non-canonical route edges; `consumed: false` marks catalysts;
`origin: recycle` marks recycling. Output `primary`, `byproduct`, `emission` and `loss` roles
remain explicit edge text. Declared resource dependencies retain explicit optional,
non-consumed and recycling flags when supplied. No missing consumed flag becomes a catalyst.
Cycles use visited-node traversal, remain visible and do not imply a seeded or sustainable
loop. The edge table is the accessible textual equivalent, including amounts and edge flags.

The waste table includes resources with by-product classes, explicit hazardous handling or a
route's `byproduct`/`emission`/`loss` output role. Scope predicates reference the agreed
canonical class ids and reported fields; they do not add a new classification dictionary.
Only `handling.discard_allowed === true` enters the disposable scope. Waived findings stay
visible debt. A future plan is not a current consumer and potential usefulness is not a promise.

### Source and safety boundary

Every entity selection shows its reported canonical source path. When the context has a safe
repository URL and hexadecimal commit, the source link is pinned to that commit, not the
moving branch. Missing source/context or unresolved graph references say so explicitly.
Machine preview references are reported data, not arbitrary remote image requests.

All JSON text is inserted using `textContent` / DOM text nodes. No JSON becomes HTML,
JavaScript, CSS selectors or executable URLs. Classification color is restricted to a hex
color used on a small marker, never low-contrast label text. External references permit HTTPS
without credentials or query strings; source references reject absolute paths, traversal and
common secret-file names. Obvious local user paths, credential patterns, contacts and raw
stack/log bodies are suppressed. This is defense in depth, not a substitute for sanitizing
the generated public projection. No raw snapshot inspector or raw-log viewer is provided.

## Renderer tests

The fixtures under [`tests/`](../../tools/dashboard/tests/index.html) are labeled
**SYNTHETIC TEST DATA ONLY**. They are neither a gameplay catalogue nor validation/acceptance
evidence for the mod. Runtime code never imports them.

With JDK 17 selected:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\dashboard\tests\serve.ps1 -Port 8877
```

The paired Bash command is:

```bash
bash tools/dashboard/tests/serve.sh 8877
```

Open `http://127.0.0.1:8877/tests/index.html`. The loopback-only JDK source-file server mounts
synthetic JSON **only** at explicit `/cases/full/`, `/cases/null/`, `/cases/schema/` and
`/cases/invalid/` URLs; `/cases/missing/` returns 404 for data. Ordinary `/index.html` still
uses the real `data.json` or fails honestly. No fixture is a production fallback.

The browser runner exercises the real renderer in desktop and mobile iframes, tests model
invariants, missing/malformed/null data, unknown states, long/hostile names, filters, all tiers,
large/cyclic graphs, revision-bound deliverables, disposal uncertainty, source URLs,
focus/native keyboard semantics, refresh failure/retry, and PR/stable wording. Results appear
on the page and in `window.dashboardTestResults`. It is a browser test harness, not a claim
that the repository's CI already runs it.

Stop the test server when the bounded inspection is finished. No Forge smoke is required for
these static renderer-only changes; they do not touch registration, loaders, game state or saves.

## Checkpoint evidence and integration limits

The initial renderer checkpoint adds implementation, synthetic fixtures and the executable
browser harness. Browser results are not claimed until the subsequent validation record.
The canonical generator/audit, CI wiring, live classifications, gameplay sources and final
integration review remain coordinator-owned. No independent PR, deployment, merge, accepted
Tier status or repair of the parent's settling-water failure is included.
