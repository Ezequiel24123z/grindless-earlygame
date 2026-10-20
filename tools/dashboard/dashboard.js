(function () {
  "use strict";

  const M = window.GrindlessDashboardModel;
  const main = document.getElementById("main-content");
  const detail = document.getElementById("detail-panel");
  const refresh = document.getElementById("refresh");
  const announcement = document.getElementById("announcement");
  const state = { model: null, path: "overview", params: new URLSearchParams(), loading: false };
  const VIEWS = new Set(["overview", "resources", "byproducts", "production", "machines", "logistics", "quests", "assets", "validation"]);
  const RESOURCE_FIELDS = [
    ["Description", "description"], ["Kind", "kind"], ["Mandatory for progression", "mandatory_for_progression"],
    ["First use tier", "first_use_tier", "tier"], ["First renewable tier", "first_renewable_tier", "tier"],
    ["Bootstrap source", "bootstrap_source"], ["Renewable routes", "renewable_routes", "route"],
    ["Input dependencies", "input_dependencies", "resource"], ["Output rate", "output_rate"],
    ["Current consumers", "current_consumers", "route"], ["Future consumers / committed plans", "future_consumers"],
    ["Recycling routes", "recycling_routes", "route"], ["Catalyst behavior", "catalyst_behavior"],
    ["Automation", "automation"], ["Vanilla dependency", "vanilla_dependency"], ["Deadlock risk", "deadlock_risk"],
    ["Handling", "handling"], ["Validation status", "validation_status"], ["Findings", "finding_ids", "finding"], ["Icon reference", "icon"]
  ];
  const DETAIL_FIELDS = {
    route: [
      ["Tier", "tier", "tier"], ["Origin", "origin"], ["Canonical route", "canonical"], ["Machine", "machine_id", "machine"],
      ["Multiblock", "multiblock_id", "machine"], ["Inputs (amount / consumed)", "inputs", "resource"],
      ["Outputs (amount / role)", "outputs", "resource"], ["Duration (ticks)", "duration_ticks"], ["Power (FU/t)", "fu_per_tick"],
      ["Conditions", "conditions"], ["Validation status", "validation_status"]
    ],
    machine: [
      ["Family", "family"], ["Tier", "tier", "tier"], ["Kind", "kind"], ["Recipes", "recipe_ids", "route"],
      ["Resources", "resource_ids", "resource"], ["Conditions", "conditions"], ["Automation", "automation"],
      ["Persistence", "persistence"], ["Preview reference", "preview"], ["Validation status", "validation_status"]
    ],
    quest: [
      ["Line", "line"], ["Evidence dependencies (not machine permissions)", "requires", "quest"],
      ["Physical evidence", "subject", "resource"], ["Evidence count", "count"], ["Optional quest", "optional"],
      ["Reward", "reward", "resource"], ["Teaches", "teaches"], ["Validation status", "validation_status"]
    ],
    finding: [
      ["Rule", "rule"], ["Severity", "severity"], ["Resource", "resource_id", "resource"], ["Tier", "tier", "tier"],
      ["Message", "message"], ["Waived (still visible debt)", "waived"], ["Remediation", "remediation"]
    ]
  };

  function element(tag, className, content) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (content !== undefined) append(node, content);
    return node;
  }

  function append(parent, content) {
    if (Array.isArray(content)) {
      content.forEach(child => append(parent, child));
    } else if (content instanceof Node) {
      parent.append(content);
    } else if (content !== undefined && content !== null) {
      parent.append(document.createTextNode(M.text(content)));
    }
    return parent;
  }

  function paragraph(content, className) {
    return element("p", className, content);
  }

  function badge(value, forcedTone) {
    const info = M.status(value);
    const node = element("span", "badge", info.label);
    node.dataset.tone = forcedTone || info.tone;
    return node;
  }

  function boolean(value) {
    return value === true ? "Yes" : value === false ? "No" : "Not reported";
  }

  function href(path, values) {
    const params = values instanceof URLSearchParams ? values : new URLSearchParams(values || {});
    return "#/" + path + (params.size ? "?" + params.toString() : "");
  }

  function link(label, url, className) {
    const node = element("a", className, label);
    node.href = url;
    return node;
  }

  function external(label, url) {
    const safe = M.httpsUrl(url);
    if (!safe) return element("span", "muted", label);
    const node = link(label, safe);
    node.target = "_blank";
    node.rel = "noopener noreferrer";
    return node;
  }

  function entityLink(type, id, title) {
    if (!M.reported(id)) return element("span", "muted", "Not reported");
    const key = String(id);
    if (type === "tier") return link(title || "T" + key, href("tier/" + encodeURIComponent(key)));
    const params = new URLSearchParams(state.params);
    params.set("item", type + ":" + key);
    const node = link(title || entityName(type, key), href(state.path, params));
    if (state.params.get("item") === type + ":" + key) node.setAttribute("aria-current", "true");
    return node;
  }

  function getEntity(type, id) {
    const model = state.model;
    const indices = { resource: model.resourceIndex, route: model.routeIndex, machine: model.machineIndex, quest: model.questIndex, finding: model.findingIndex };
    if (indices[type]) return indices[type].get(String(id)) || null;
    const arrays = {
      asset: model.assets.entries, generation: model.assets.generation_queue, qc: model.assets.qc_queue, assetcheck: model.assets.checks,
      check: model.validation.checks, ci: model.validation.ci, manual: model.validation.manual_acceptance,
      deliverable: model.validation.required_deliverables, milestone: model.context.milestones
    };
    if (!Object.prototype.hasOwnProperty.call(arrays, type) || !/^\d+$/.test(id)) return null;
    return M.list(arrays[type])[Number(id)] ?? null;
  }

  function entityName(type, id) {
    const entry = getEntity(type, id);
    return entry ? M.text(M.object(entry).name || M.object(entry).title || M.object(entry).id || (typeof entry === "string" ? entry : id)) : M.text(id);
  }

  function sourceReference(path) {
    const safe = M.repositoryPath(path);
    if (!safe) return element("span", "source-ref muted", M.reported(path) ? "Source reference unavailable (not a safe repository path)." : "Source path not reported.");
    const url = M.sourceUrl(state.model.context, safe);
    const node = url ? external(safe, url) : element("span", "", safe);
    node.classList.add("source-ref", "mono");
    return node;
  }

  function evidence(value) {
    if (!M.reported(value)) return element("span", "muted", "Not reported");
    if (Array.isArray(value)) return valueList(value, "evidence");
    if (typeof value === "object") {
      const record = M.object(value);
      const reference = record.source_path || record.path || record.url || record.reference;
      const wrapper = element("div");
      if (reference) wrapper.append(evidence(reference));
      if (M.reported(record.summary)) wrapper.append(paragraph(M.narrative(record.summary)));
      if (!reference && !M.reported(record.summary)) wrapper.append(element("span", "muted", "Evidence reference not reported."));
      return wrapper;
    }
    const safeUrl = M.httpsUrl(value);
    if (safeUrl) return external(value, safeUrl);
    if (typeof value === "string" && /[/.]/.test(value) && M.repositoryPath(value) && !/\s/.test(value)) return sourceReference(value);
    return element("span", "muted", "Raw evidence is not displayed. A repository path or HTTPS reference is required.");
  }

  function dataValue(value, preferredType, depth) {
    const level = depth || 0;
    if (!M.reported(value)) return element("span", "muted", "Not reported");
    if (preferredType === "evidence") return evidence(value);
    if (Array.isArray(value)) return valueList(value, preferredType, level);
    if (typeof value === "boolean") return element("span", "", boolean(value));
    if (typeof value !== "object") {
      if (preferredType === "tier") return entityLink("tier", value);
      if (preferredType) return entityLink(preferredType, value);
      if (M.httpsUrl(value)) return external(value, value);
      return element("span", "", M.narrative(value));
    }
    if (level > 5) return element("span", "muted", "Additional nested data is not displayed.");
    const record = M.object(value);
    const fields = Object.entries(record);
    if (!fields.length) return element("span", "muted", "No fields reported");
    const dl = element("dl", "nested-data");
    fields.forEach(([key, child]) => {
      if (/(?:^|_)(?:secret|password|token|credential|api_key|authorization|auth|raw_log|logs?)(?:s)?$/i.test(key)) return;
      const type = key === "resource_id" ? "resource" : key === "route_id" || key === "recipe_id" ? "route" : key === "machine_id" || key === "multiblock_id" ? "machine" : key === "quest_id" ? "quest" : key === "tier" ? "tier" : key === "id" ? preferredType : null;
      dl.append(element("dt", "", M.label(key)));
      dl.append(element("dd", "", key === "source_path" || key === "path" ? sourceReference(child) : key === "evidence" ? evidence(child) : dataValue(child, type, level + 1)));
    });
    if (!dl.children.length) return element("span", "muted", "Sensitive fields are not displayed.");
    return dl;
  }

  function valueList(value, type, depth) {
    if (!Array.isArray(value)) return element("span", "muted", "Not reported");
    if (!value.length) return element("span", "muted", "None reported");
    const ul = element("ul");
    value.forEach(entry => ul.append(element("li", "", dataValue(entry, type, (depth || 0) + 1))));
    return ul;
  }

  function classificationBadges(resource, domain) {
    const ids = M.classifications(resource, domain);
    const wrapper = element("span");
    if (!ids.length) return element("span", "muted", "Not reported");
    ids.forEach(id => {
      const entry = state.model.classificationIndex.get(id);
      if (!entry || entry.domain !== domain) {
        wrapper.append(badge("Classification not reported: " + id, "neutral"));
        return;
      }
      const node = element("span", "badge", entry.label || "Label not reported");
      node.title = M.text(entry.description);
      if (typeof entry.color === "string" && /^#[0-9a-f]{3}(?:[0-9a-f]{3})?$/i.test(entry.color)) node.style.setProperty("--badge-mark", entry.color);
      if (M.reported(entry.icon)) {
        const icon = element("span", "", M.text(entry.icon).slice(0, 12));
        icon.setAttribute("aria-hidden", "true");
        node.prepend(icon);
      }
      wrapper.append(node);
    });
    return wrapper;
  }

  function table(columns, rows, caption) {
    if (!rows.length) return paragraph("No entries reported for this scope.", "empty-state");
    const container = element("div", "table-wrap");
    container.tabIndex = 0;
    container.setAttribute("role", "region");
    container.setAttribute("aria-label", caption + ". Scroll horizontally for all columns.");
    const node = element("table");
    node.append(element("caption", "sr-only", caption));
    const head = element("tr");
    columns.forEach(column => {
      const th = element("th", "", column.title);
      th.scope = "col";
      head.append(th);
    });
    node.append(element("thead", "", head));
    const body = element("tbody");
    rows.forEach(row => {
      const tr = element("tr");
      columns.forEach(column => tr.append(element("td", "", column.render(row))));
      body.append(tr);
    });
    node.append(body);
    container.append(node);
    return container;
  }

  function title(name, description) {
    const header = element("header", "view-header");
    const h1 = element("h1", "", name);
    h1.id = "view-title";
    h1.tabIndex = -1;
    header.append(h1);
    if (description) header.append(paragraph(description));
    main.append(header);
  }

  function cellTitle(type, entry) {
    return element("div", "", [
      entityLink(type, entry.id, M.text(entry.name || entry.id)),
      element("span", "cell-note mono", entry.id)
    ]);
  }

  function fieldPair(firstLabel, first, secondLabel, second) {
    return element("div", "", [
      element("span", "", [firstLabel + ": ", first]),
      element("span", "cell-note", [secondLabel + ": ", second])
    ]);
  }

  function tierValue(value) {
    return M.reported(value) ? entityLink("tier", value) : element("span", "muted", "Not reported");
  }

  function form() {
    const node = element("form", "filters");
    node.setAttribute("aria-label", "Filter this view");
    node.addEventListener("submit", event => event.preventDefault());
    return node;
  }

  function saveFilter(key, value) {
    if (value) state.params.set(key, value); else state.params.delete(key);
    history.replaceState(null, "", href(state.path, state.params));
  }

  function selectFilter(parent, key, name, options, update, defaultValue) {
    const label = element("label", "filter", name);
    const select = element("select");
    select.id = "filter-" + key;
    options.forEach(([value, text]) => {
      const option = element("option", "", text);
      option.value = value;
      select.append(option);
    });
    select.value = state.params.get(key) || defaultValue || "";
    select.addEventListener("change", () => { saveFilter(key, select.value); update(); });
    label.append(select);
    parent.append(label);
    return select;
  }

  function searchFilter(parent, update) {
    const label = element("label", "filter search", "Search name or identifier");
    const input = element("input");
    input.id = "filter-q";
    input.type = "search";
    input.placeholder = "Name, identifier or description";
    input.value = state.params.get("q") || "";
    input.addEventListener("input", () => { saveFilter("q", input.value); update(); });
    label.append(input);
    parent.append(label);
    return input;
  }

  function checkboxFilter(parent, key, name, update, checkedByDefault) {
    const label = element("label", "check-filter");
    const input = element("input");
    input.id = "filter-" + key;
    input.type = "checkbox";
    input.checked = checkedByDefault ? state.params.get(key) !== "0" : state.params.get(key) === "1";
    input.addEventListener("change", () => { saveFilter(key, checkedByDefault ? input.checked ? "" : "0" : input.checked ? "1" : ""); update(); });
    label.append(input, document.createTextNode(name));
    parent.append(label);
    return input;
  }

  function tierOptions() {
    return [["", "All tiers"]].concat(Array.from({ length: 16 }, (_, id) => [String(id), "T" + id + " - " + M.text(M.tier(state.model, id).name)]));
  }

  function filterValues() {
    return {
      q: state.params.get("q"), tier: state.params.get("tier"), classification: state.params.get("classification"),
      mandatory: state.params.get("mandatory") === "1", waste: state.params.get("waste")
    };
  }

  function count(shown, total, noun) {
    const node = paragraph(shown + " / " + total + " " + noun + " shown", "results-count");
    node.setAttribute("role", "status");
    node.setAttribute("aria-live", "polite");
    return node;
  }

  function deliverableSummary(parent) {
    const progress = M.deliverables(state.model);
    parent.append(element("h2", "", "Required deliverables"));
    if (!progress.reported || !progress.denominator) {
      parent.append(paragraph("Required deliverables are not reported. No completion measure can be calculated.", "notice"));
      return;
    }
    parent.append(paragraph([
      element("span", "fraction", progress.numerator + " / " + progress.denominator),
      " explicit deliverables reported complete at this exact source commit."
    ]));
    parent.append(paragraph("Only explicit completion states with an exactly matching source_commit enter the numerator. Historical, skipped, cancelled, waived and unreported evidence does not.", "muted"));
    parent.append(checkTable(progress.entries, "deliverable", "Required deliverable evidence"));
  }

  function renderOverview() {
    title("Repository overview", "A generated, revision-bound view of progression, supply and observed evidence. Counts describe this snapshot, not subjective completion.");
    const context = state.model.context;
    main.append(paragraph("Tier acceptance, no-command survival, client interaction and saved-world compatibility require their own reported evidence. A renderer or an automated pass cannot grant acceptance.", "notice warning"));
    const facts = element("dl", "facts");
    [["Active tier", tierValue(context.active_tier)], ["Active slice", M.narrative(context.active_slice)], ["Project state", badge(context.status)], ["Validation state", badge(state.model.validation.status)]].forEach(([name, value]) => facts.append(element("dt", "", name), element("dd", "", value)));
    main.append(facts);
    deliverableSummary(main);
    main.append(element("h2", "", "Tier ledger"));
    const tiers = Array.from({ length: 16 }, (_, id) => M.tier(state.model, id));
    main.append(table([
      { title: "Tier", render: row => entityLink("tier", row.id, "T" + row.id + " - " + M.text(row.name)) },
      { title: "Implementation", render: row => badge(row.implementation_state) },
      { title: "Purpose / exit", render: row => fieldPair("Purpose", M.narrative(row.purpose), "Exit", M.narrative(row.exit)) }
    ], tiers, "T0 through T15, including unreported tiers"));
    main.append(element("h2", "", "Findings and visible debt"));
    const findings = state.model.findings;
    main.append(count(Math.min(8, findings.length), findings.length, "findings"));
    main.append(findingTable(findings.slice(0, 8)));
    main.append(paragraph(link("Inspect all findings, tests and manual acceptance", href("validation"))));
    main.append(element("h2", "", "Milestones"));
    const milestones = M.list(context.milestones);
    main.append(table([
      { title: "Milestone", render: (row) => entityLink("milestone", row.index, M.text(row.entry.title || row.entry.id)) },
      { title: "Reported state", render: row => badge(row.entry.status) },
      { title: "Commit", render: row => element("code", "", M.text(row.entry.commit)) }
    ], milestones.map((entry, index) => ({ entry: M.object(entry), index })), "Reported project milestones"));
    main.append(element("h2", "", "Recent commits in the generated snapshot"));
    main.append(table([
      { title: "Commit", render: row => external(M.text(row.sha), row.url) },
      { title: "Subject", render: row => M.text(row.subject) }
    ], M.list(context.recent_commits), "Snapshot commit history; not a live activity feed"));
  }

  function tierSection(name, value, type) {
    const section = element("section");
    section.append(element("h2", "", name), valueList(value, type));
    return section;
  }

  function renderTier(id) {
    const tier = M.tier(state.model, id);
    title("T" + id + " - " + M.text(tier.name), M.narrative(tier.purpose));
    main.append(badge(tier.implementation_state), sourceReference(tier.source_path));
    if (tier.unreported) main.append(paragraph("This tier has no entry in the snapshot. Its plan, implementation and acceptance are not reported.", "notice warning"));
    const facts = element("dl", "facts");
    [["Entry", tier.entry], ["Exit", tier.exit]].forEach(([name, value]) => facts.append(element("dt", "", name), element("dd", "", dataValue(value))));
    main.append(facts);
    const sections = element("div", "tier-sections");
    [
      ["Bootstrap resources", "bootstrap_resources", "resource"], ["Renewable routes", "renewable_routes", "route"],
      ["Improved resources", "improved_resources", "resource"], ["Recycling routes", "recycling_routes", "route"],
      ["Useful by-products", "useful_byproducts", "resource"], ["Vanilla dependencies eliminated", "vanilla_dependencies_eliminated"],
      ["Finite optional supplies", "finite_optional", "resource"], ["Deadlock protection", "deadlock_protection"],
      ["Blockers", "blockers"], ["Manual checks (reported evidence only)", "manual_checks"]
    ].forEach(([name, field, type]) => sections.append(tierSection(name, tier[field], type)));
    main.append(sections);
    main.append(element("h2", "", "Tier-linked resources"), valueList(tier.resource_ids, "resource"));
    main.append(element("h2", "", "Tier-linked machines / multiblocks"), valueList(tier.machine_ids, "machine"));
    main.append(element("h2", "", "Tier-linked quests"), valueList(tier.quest_ids, "quest"));
    main.append(element("h2", "", "Routes authored at this tier"));
    main.append(routeTable(state.model.routes.filter(route => route.tier === id)));
    main.append(element("h2", "", "Findings at this tier"));
    main.append(findingTable(state.model.findings.filter(finding => finding.tier === id)));
    const pager = element("nav", "tier-pager");
    pager.setAttribute("aria-label", "Adjacent tiers");
    if (id > 0) pager.append(link("Previous: T" + (id - 1), href("tier/" + (id - 1)), "button"));
    if (id < 15) pager.append(link("Next: T" + (id + 1), href("tier/" + (id + 1)), "button"));
    main.append(pager);
  }

  function classificationLegend(domain) {
    const disclosure = element("details");
    disclosure.append(element("summary", "", "Classification dictionary (canonical labels and meanings)"));
    disclosure.append(table([
      { title: "Label", render: row => row.label },
      { title: "Meaning", render: row => M.narrative(row.description) },
      { title: "Identifier / localization", render: row => fieldPair("Id", element("code", "", row.id), "Key", element("code", "", M.text(row.localization_key))) }
    ], state.model.classifications.filter(entry => !domain || entry.domain === domain), "Canonical classification dictionary"));
    return disclosure;
  }

  function resourceTable(rows, waste) {
    return table(waste ? [
      { title: "By-product / waste", render: row => cellTitle("resource", row) },
      { title: "Classification", render: row => classificationBadges(row, "byproduct") },
      { title: "Tiers / mandatory", render: row => element("div", "", [fieldPair("First use", tierValue(row.first_use_tier), "Renewal", tierValue(row.first_renewable_tier)), element("span", "cell-note", "Mandatory: " + boolean(row.mandatory_for_progression))]) },
      { title: "Current / future consumers", render: row => element("div", "", [valueList(row.current_consumers, "route"), element("span", "cell-note", "Future plans"), valueList(row.future_consumers)]) },
      { title: "Handling", render: row => fieldPair("Action", M.narrative(M.object(row.handling).current_action), "Discard allowed", boolean(M.object(row.handling).discard_allowed)) },
      { title: "Evidence / debt", render: row => element("div", "", [badge(row.validation_status), valueList(row.finding_ids, "finding")]) }
    ] : [
      { title: "Resource", render: row => cellTitle("resource", row) },
      { title: "Resource classification", render: row => classificationBadges(row, "resource") },
      { title: "First use / renewal", render: row => fieldPair("Use", tierValue(row.first_use_tier), "Renewal", tierValue(row.first_renewable_tier)) },
      { title: "Mandatory", render: row => boolean(row.mandatory_for_progression) },
      { title: "Renewable routes", render: row => valueList(row.renewable_routes, "route") },
      { title: "Validation / findings", render: row => element("div", "", [badge(row.validation_status), valueList(row.finding_ids, "finding")]) }
    ], rows, waste ? "Reported side streams and handling; unknown disposal is not safe disposal" : "Resource sustainability from canonical annotations");
  }

  function renderResources(waste) {
    title(waste ? "By-products & waste" : "Resource sustainability", waste ? "Reported side streams, present and future consumers, and actual handling. Discard is permitted only when explicitly true; unknown handling is not safe disposal." : "Supply classifications and routes are reported by the generator. A repeating route is not independently certified renewable by this renderer.");
    const all = waste ? M.wasteResources(state.model) : state.model.resources;
    if (!state.model.reported.resources) main.append(paragraph("The resources collection is not reported in this snapshot.", "notice warning"));
    const controls = form();
    const results = element("section");
    results.id = "resource-results";
    const update = () => {
      const rows = M.filterResources(all, filterValues());
      results.replaceChildren(count(rows.length, all.length, waste ? "side streams" : "resources"));
      results.append(rows.length ? resourceTable(rows, waste) : paragraph("No entries match these filters. Clear the search or choose a broader scope.", "empty-state"));
    };
    searchFilter(controls, update);
    selectFilter(controls, "tier", "First-use or renewable tier", tierOptions(), update);
    selectFilter(controls, "classification", "Canonical classification", [["", "All classifications"]].concat(state.model.classifications.filter(entry => !waste || entry.domain === "byproduct").map(entry => [String(entry.id), M.text(entry.label)])), update);
    if (waste) selectFilter(controls, "waste", "Side-stream scope", [
      ["", "All side streams"], ["current", "Current consumer / class"], ["future", "Future plan / class"],
      ["potential", "Potential future use class"], ["recyclable", "Recycling route / class"], ["hazardous", "Hazardous"],
      ["inert", "Inert class"], ["disposable", "Explicitly disposable"], ["spent", "Spent catalyst class"],
      ["mandatory", "Mandatory for progression"], ["unresolved", "Unresolved / unreported evidence"]
    ], update);
    checkboxFilter(controls, "mandatory", "Mandatory only", update, false);
    const clear = element("button", "", "Clear filters");
    clear.type = "button";
    clear.addEventListener("click", () => { location.hash = href(state.path); });
    controls.append(clear);
    main.append(controls, results, classificationLegend(waste ? "byproduct" : null));
    update();
  }

  function routeTable(rows) {
    return table([
      { title: "Route", render: row => cellTitle("route", row) },
      { title: "Tier / origin", render: row => fieldPair("Tier", tierValue(row.tier), "Origin", M.label(row.origin)) },
      { title: "Machine", render: row => entityLink("machine", row.machine_id || row.multiblock_id) },
      { title: "Inputs / outputs", render: row => fieldPair("Inputs", valueList(row.inputs, "resource"), "Outputs", valueList(row.outputs, "resource")) },
      { title: "Validation", render: row => badge(row.validation_status) }
    ], rows, "Authored production routes");
  }

  function networkView(kind) {
    const network = M.graph(state.model, kind);
    const controls = form();
    controls.setAttribute("aria-label", kind === "quests" ? "Quest dependency neighborhood controls" : "Production dependency neighborhood controls");
    const results = element("section");
    const entries = Array.from(network.nodes.values()).sort((a, b) => M.ordered(a.id, b.id));
    if (!entries.length) return paragraph("No graph nodes are reported in this snapshot.", "empty-state");
    const requested = state.params.get("root");
    const initial = requested || entries[0].id;
    const update = () => {
      const center = state.params.get("root") || initial;
      const neighborhood = M.neighborhood(network, center, {
        depth: state.params.get("depth") || "1", limit: 48,
        optional: state.params.get("optional") !== "0", catalytic: state.params.get("catalytic") !== "0", recycling: state.params.get("recycling") !== "0"
      });
      results.replaceChildren();
      const message = "Filtered neighborhood: " + neighborhood.nodes.length + " / " + neighborhood.totalNodes + " nodes; " + neighborhood.edges.length + " / " + neighborhood.totalEdges + " edges shown. " + neighborhood.eligibleEdges + " edges eligible after edge filters.";
      results.append(paragraph(message, "results-count"));
      if (neighborhood.truncated) results.append(paragraph("The 48-node display limit was reached. Choose another center or a smaller depth to inspect the remaining graph.", "notice warning"));
      if (!neighborhood.nodes.length) {
        results.append(paragraph("The selected node is not in this snapshot. Choose a node from the center selector.", "empty-state"));
        return;
      }
      results.append(drawNetwork(neighborhood, center, kind));
      results.append(element("h3", "", "Shown edges (text equivalent)"));
      results.append(table([
        { title: "From", render: row => nodeLink(network.nodes.get(row.from)) },
        { title: "To", render: row => nodeLink(network.nodes.get(row.to)) },
        { title: "Relationship / amount", render: row => fieldPair("Relation", row.label, "Amount", M.text(row.amount)) },
        { title: "Edge semantics", render: row => [row.optional ? "Optional / non-canonical" : "No optional flag reported", row.catalytic ? "Catalytic" : "", row.recycling ? "Recycling" : ""].filter(Boolean).join("; ") }
      ], neighborhood.edges, "Text equivalent of every shown dependency edge"));
    };
    const choices = entries.map(node => [node.id, M.text(node.name) + " [" + node.type + "]"]);
    if (requested && !network.nodes.has(requested)) choices.unshift([requested, "Not in snapshot: " + M.text(requested)]);
    selectFilter(controls, "root", "Center (any node)", choices, update, initial);
    selectFilter(controls, "depth", "Neighborhood depth", [["1", "1 connection"], ["2", "2 connections"], ["3", "3 connections"]], update, "1");
    checkboxFilter(controls, "optional", kind === "quests" ? "Include optional quest edges" : "Include optional / non-canonical routes", update, true);
    if (kind !== "quests") {
      checkboxFilter(controls, "catalytic", "Include catalytic edges", update, true);
      checkboxFilter(controls, "recycling", "Include recycling edges", update, true);
    }
    const section = element("section");
    section.append(controls, paragraph("Select a node for its detail and canonical source. Change the center to inspect another neighborhood. Cycles are retained, not unfolded into an invented production order.", "muted"));
    const legend = element("div", "legend");
    [["", "Dependency / flow"], ["optional", "Optional"], ["catalytic", "Catalytic"], ["recycling", "Recycling"]].forEach(([type, label]) => {
      if (kind === "quests" && ["catalytic", "recycling"].includes(type)) return;
      legend.append(element("span", "", [element("i", "edge-swatch " + type), label]));
    });
    section.append(legend, results);
    update();
    return section;
  }

  function nodeLink(node) {
    return entityLink(node.type, node.entityId, node.name);
  }

  function drawNetwork(neighborhood, center, kind) {
    const region = element("div", "graph-scroll");
    region.tabIndex = 0;
    region.setAttribute("role", "region");
    region.setAttribute("aria-label", "Filtered dependency diagram. Scroll horizontally; use the edge table for all relationship labels.");
    const stage = element("div", "network-stage");
    const levels = Array.from(new Set(neighborhood.nodes.map(node => node.level))).sort((a, b) => a - b);
    const positions = new Map();
    let maxRows = 1;
    levels.forEach((level, column) => {
      const nodes = neighborhood.nodes.filter(node => node.level === level).sort((a, b) => M.ordered(a.id, b.id));
      maxRows = Math.max(maxRows, nodes.length);
      nodes.forEach((node, row) => positions.set(node.id, { x: 24 + column * 250, y: 26 + row * 106 }));
    });
    const width = Math.max(500, levels.length * 250 + 24);
    const height = Math.max(150, maxRows * 106 + 26);
    stage.style.width = width + "px";
    stage.style.height = height + "px";
    const ns = "http://www.w3.org/2000/svg";
    const svg = document.createElementNS(ns, "svg");
    svg.setAttribute("width", String(width));
    svg.setAttribute("height", String(height));
    svg.setAttribute("aria-hidden", "true");
    const defs = document.createElementNS(ns, "defs");
    const marker = document.createElementNS(ns, "marker");
    const markerId = kind + "-edge-arrow";
    [["id", markerId], ["viewBox", "0 0 10 10"], ["refX", "9"], ["refY", "5"], ["markerWidth", "6"], ["markerHeight", "6"], ["orient", "auto-start-reverse"]].forEach(([key, value]) => marker.setAttribute(key, value));
    const arrow = document.createElementNS(ns, "path");
    arrow.setAttribute("d", "M 0 0 L 10 5 L 0 10 z");
    arrow.setAttribute("fill", "#a8b7c5");
    marker.append(arrow);
    defs.append(marker);
    svg.append(defs);
    neighborhood.edges.forEach(edge => {
      const from = positions.get(edge.from);
      const to = positions.get(edge.to);
      const forward = to.x > from.x;
      const x1 = from.x + (forward ? 198 : 0);
      const x2 = to.x + (forward ? 0 : 198);
      const y1 = from.y + 38;
      const y2 = to.y + 38;
      const path = document.createElementNS(ns, "path");
      const bend = forward ? 28 : -28;
      path.setAttribute("d", "M " + x1 + " " + y1 + " C " + (x1 + bend) + " " + y1 + ", " + (x2 - bend) + " " + y2 + ", " + x2 + " " + y2);
      path.setAttribute("class", "graph-edge" + (edge.optional ? " optional" : "") + (edge.catalytic ? " catalytic" : "") + (edge.recycling ? " recycling" : ""));
      path.setAttribute("marker-end", "url(#" + markerId + ")");
      svg.append(path);
    });
    stage.append(svg);
    neighborhood.nodes.forEach(node => {
      const position = positions.get(node.id);
      const anchor = nodeLink(node);
      anchor.className = "graph-node";
      anchor.style.left = position.x + "px";
      anchor.style.top = position.y + "px";
      anchor.dataset.center = String(node.id === center);
      anchor.dataset.missing = String(!node.entry);
      anchor.title = M.text(node.name);
      anchor.setAttribute("aria-label", M.text(node.name) + " - " + node.type + " detail");
      anchor.replaceChildren(element("span", "node-name", node.name), element("span", "node-kind", node.type + (node.id === center ? " / center" : "") + (!node.entry ? " / not reported" : "")));
      stage.append(anchor);
    });
    region.append(stage);
    return region;
  }

  function renderProduction() {
    title("Production dependencies", "Resource and route nodes include environmental sources and tags. Amounts, consumed flags, output roles and route origin remain as authored; no balance or renewable claim is inferred.");
    main.append(networkView("production"));
    const disclosure = element("details");
    disclosure.append(element("summary", "", "All authored routes (" + state.model.routes.length + ")"), routeTable(state.model.routes));
    main.append(disclosure);
  }

  function renderMachines(logistics) {
    title(logistics ? "Logistics & automation" : "Machines & multiblocks", logistics ? "Logistics/storage entries and machines with reported automation metadata. Interfaces, persistence and conditions are reported, not inferred from machine names." : "Ordinary machines, generators, parts, standard multiblocks and assembled multiblocks remain distinct kinds.");
    const all = state.model.machines.filter(machine => !logistics || ["logistics", "storage", "logistics_storage", "logistics/storage"].includes(machine.kind) || M.reported(machine.automation));
    const controls = form();
    const results = element("section");
    const update = () => {
      const q = (state.params.get("q") || "").trim().toLowerCase();
      const rows = all.filter(row => (!q || [row.id, row.name, row.family].join(" ").toLowerCase().includes(q)) && (!state.params.get("tier") || String(row.tier) === state.params.get("tier")) && (!state.params.get("kind") || row.kind === state.params.get("kind")));
      results.replaceChildren(count(rows.length, all.length, "entries"));
      results.append(table([
        { title: "Machine / structure", render: row => cellTitle("machine", row) },
        { title: "Kind / family", render: row => fieldPair("Kind", M.label(row.kind), "Family", M.label(row.family)) },
        { title: "Tier", render: row => tierValue(row.tier) },
        { title: "Automation", render: row => dataValue(row.automation) },
        { title: "Persistence / validation", render: row => element("div", "", [dataValue(row.persistence), badge(row.validation_status)]) }
      ], rows, logistics ? "Reported logistics, storage and automation" : "Machines and both multiblock kinds"));
    };
    searchFilter(controls, update);
    selectFilter(controls, "tier", "Tier", tierOptions(), update);
    selectFilter(controls, "kind", "Machine kind", [["", "All kinds"]].concat(Array.from(new Set(all.map(row => row.kind).filter(M.reported))).sort(M.ordered).map(kind => [kind, M.label(kind)])), update);
    main.append(controls, results);
    update();
  }

  function renderQuests() {
    title("Questbook", "Dependencies describe physical evidence and the teaching sequence. Quest claims do not consume evidence, grant machine permissions or replace the survival route.");
    const controls = form();
    const results = element("section");
    const update = () => {
      const q = (state.params.get("q") || "").trim().toLowerCase();
      const optional = state.params.get("quest-scope");
      const rows = state.model.quests.filter(row => (!q || [row.id, row.name, row.line, ...M.list(row.teaches)].join(" ").toLowerCase().includes(q)) && (!state.params.get("line") || row.line === state.params.get("line")) && (!optional || (optional === "optional" ? row.optional === true : optional === "required" ? row.optional === false : !M.reported(row.optional))));
      results.replaceChildren(count(rows.length, state.model.quests.length, "quests"));
      results.append(table([
        { title: "Quest / line", render: row => element("div", "", [cellTitle("quest", row), element("span", "cell-note", M.text(row.line))]) },
        { title: "Dependencies", render: row => valueList(row.requires, "quest") },
        { title: "Physical evidence", render: row => fieldPair("Subject", dataValue(row.subject, "resource"), "Count", M.text(row.count)) },
        { title: "Reward / optional", render: row => fieldPair("Reward", dataValue(row.reward, "resource"), "Optional", boolean(row.optional)) },
        { title: "Teaches", render: row => valueList(row.teaches) }
      ], rows, "Full questbook evidence, dependencies, rewards and education"));
    };
    searchFilter(controls, update);
    selectFilter(controls, "line", "Quest line", [["", "All lines"]].concat(Array.from(new Set(state.model.quests.map(row => row.line).filter(M.reported))).sort(M.ordered).map(line => [line, M.text(line)])), update);
    selectFilter(controls, "quest-scope", "Quest scope", [["", "All quests"], ["required", "Reported non-optional"], ["optional", "Reported optional"], ["unknown", "Optional state not reported"]], update);
    main.append(controls, results);
    update();
    main.append(element("h2", "", "Quest evidence dependencies"), networkView("quests"));
  }

  function namedRows(entries) {
    return M.list(entries).map((entry, index) => ({ entry: M.object(entry), value: entry, index }));
  }

  function namedLabel(row) {
    return M.text(row.entry.name || row.entry.title || row.entry.id || (typeof row.value === "string" ? row.value : "Entry " + (row.index + 1)));
  }

  function assetTable(entries, type, name) {
    return table([
      { title: "Entry", render: row => entityLink(type, row.index, namedLabel(row)) },
      { title: "Kind / state", render: row => fieldPair("Kind", M.label(row.entry.kind || row.entry.type), "State", badge(row.entry.status || row.entry.validation_status)) },
      { title: "Source / reference", render: row => sourceReference(row.entry.source_path || row.entry.path) },
      { title: "Reported details", render: row => M.narrative(row.entry.description || row.entry.summary || row.entry.details) }
    ], namedRows(entries), name);
  }

  function renderAssets() {
    title("Assets", "Generated entries, generation and quality-control queues, and observed checks from the snapshot. A queue entry is not an approved asset.");
    [["Asset entries", "entries", "asset"], ["Generation queue", "generation_queue", "generation"], ["Quality-control queue", "qc_queue", "qc"], ["Asset checks", "checks", "assetcheck"]].forEach(([name, key, type]) => {
      main.append(element("h2", "", name), count(M.list(state.model.assets[key]).length, M.list(state.model.assets[key]).length, "reported entries"));
      main.append(assetTable(state.model.assets[key], type, name));
      if (!Array.isArray(state.model.assets[key])) main.append(paragraph(name + " are not reported.", "muted"));
    });
  }

  function commitEvidence(entry) {
    const source = entry.source_commit;
    const same = typeof source === "string" && /^[0-9a-f]{7,40}$/i.test(source) && source === state.model.context.commit;
    return element("div", "", [element("code", "", M.text(source)), element("span", "cell-note", !M.reported(source) ? "Revision not reported" : same ? "Matches snapshot commit" : "Different or unresolved revision; not current acceptance")]);
  }

  function checkTable(entries, type, name) {
    return table([
      { title: "Check / scope", render: row => entityLink(type, row.index, namedLabel(row)) },
      { title: "Observed state", render: row => badge(row.entry.status) },
      { title: "Source commit", render: row => commitEvidence(row.entry) },
      { title: "Evidence", render: row => evidence(row.entry.evidence) },
      { title: "Details", render: row => M.narrative(row.entry.details || row.entry.summary) }
    ], namedRows(entries), name);
  }

  function findingTable(entries) {
    return table([
      { title: "Finding / rule", render: row => element("div", "", [entityLink("finding", row.id, row.id), element("span", "cell-note", M.text(row.rule))]) },
      { title: "Severity / debt", render: row => element("div", "", [badge(row.severity), row.waived === true ? badge("Waived - visible debt", "warning") : element("span", "cell-note", row.waived === false ? "Not waived" : "Waiver not reported")]) },
      { title: "Resource / tier", render: row => fieldPair("Resource", entityLink("resource", row.resource_id), "Tier", tierValue(row.tier)) },
      { title: "Finding / remediation", render: row => fieldPair("Finding", M.narrative(row.message), "Remediation", M.narrative(row.remediation)) }
    ], entries, "Findings; waived violations remain visible debt");
  }

  function renderValidation() {
    title("Tests, blockers & manual acceptance", "Only reported observations are shown. Historical passes, cancelled checks and unobserved player acceptance are not promoted to this revision.");
    main.append(badge(state.model.validation.status));
    deliverableSummary(main);
    [["Repository checks", "checks", "check"], ["CI observations", "ci", "ci"], ["Manual acceptance", "manual_acceptance", "manual"]].forEach(([name, key, type]) => {
      main.append(element("h2", "", name), checkTable(state.model.validation[key], type, name));
      if (!Array.isArray(state.model.validation[key]) || !state.model.validation[key].length) {
        main.append(paragraph(name === "Manual acceptance" ? "Manual acceptance: not reported / not run. No-command survival, client interaction and saved-world compatibility are not established." : name + ": not reported / not run.", "notice warning"));
      }
    });
    main.append(element("h2", "", "Findings and waivers"));
    const controls = form();
    const results = element("section");
    const update = () => {
      const rows = state.model.findings.filter(row => (!state.params.get("tier") || String(row.tier) === state.params.get("tier")) && (!state.params.get("severity") || row.severity === state.params.get("severity")) && (!state.params.get("debt") || row.waived === true));
      results.replaceChildren(count(rows.length, state.model.findings.length, "findings"), findingTable(rows));
    };
    selectFilter(controls, "tier", "Tier", tierOptions(), update);
    selectFilter(controls, "severity", "Severity", [["", "All severities"]].concat(Array.from(new Set(state.model.findings.map(row => row.severity).filter(M.reported))).sort(M.ordered).map(value => [value, M.label(value)])), update);
    checkboxFilter(controls, "debt", "Waived debt only", update, false);
    main.append(controls, results);
    update();
    main.append(element("h2", "", "Tier blockers and manual checks"));
    const rows = state.model.tiers.filter(tier => M.list(tier.blockers).length || M.list(tier.manual_checks).length);
    main.append(table([
      { title: "Tier", render: row => entityLink("tier", row.id, "T" + row.id + " - " + M.text(row.name)) },
      { title: "Blockers", render: row => valueList(row.blockers) },
      { title: "Manual checks", render: row => valueList(row.manual_checks) }
    ], rows, "Reported tier blockers and manual checks"));
  }

  function renderDetail(focus) {
    detail.replaceChildren();
    const titleNode = element("h2");
    titleNode.id = "detail-title";
    titleNode.tabIndex = -1;
    detail.append(titleNode);
    const selected = state.params.get("item");
    if (!selected) {
      titleNode.textContent = "Inspect an entry";
      detail.append(paragraph("Select an entry to read its exact metadata, evidence and canonical source."));
      detail.append(paragraph("Unknown fields remain unreported. This view does not certify sustainability or Tier acceptance.", "muted"));
      return;
    }
    const split = selected.indexOf(":");
    const type = selected.slice(0, split);
    const id = selected.slice(split + 1);
    const entry = split > 0 ? getEntity(type, id) : null;
    const params = new URLSearchParams(state.params);
    params.delete("item");
    const controls = element("div", "detail-controls", link("Close detail", href(state.path, params), "button"));
    detail.append(controls);
    if (!entry) {
      titleNode.textContent = M.text(id || selected);
      detail.append(paragraph("This referenced entry is not reported in the snapshot. Its source and state are unknown.", "notice warning"));
      if (focus) titleNode.focus();
      return;
    }
    const record = M.object(entry);
    titleNode.textContent = entityName(type, id);
    detail.append(paragraph(M.label(type) + " / " + M.text(record.id || id), "detail-kind mono"));
    detail.append(sourceReference(record.source_path || record.path));
    if (["resource", "route", "quest"].includes(type)) {
      const graphParams = new URLSearchParams({ root: type + ":" + id });
      controls.append(link("Explore dependencies", href(type === "quest" ? "quests" : "production", graphParams), "button"));
    }
    if (state.path === "production" || state.path === "quests") {
      if (["resource", "route", "quest"].includes(type)) {
        const centerParams = new URLSearchParams(state.params);
        centerParams.set("root", type + ":" + id);
        controls.append(link("Center graph here", href(state.path, centerParams), "button"));
      }
    }
    if (type === "resource") {
      detail.append(element("h3", "", "Resource classes"), classificationBadges(record, "resource"));
      detail.append(element("h3", "", "By-product classes"), classificationBadges(record, "byproduct"));
      if (record.handling?.discard_allowed !== true) detail.append(paragraph(record.handling?.discard_allowed === false ? "Discard is explicitly not allowed." : "Discard permission is not reported. Do not assume safe disposal.", "notice warning"));
    }
    if (type === "finding" && record.waived === true) detail.append(paragraph("Waived findings remain debt. This waiver is not a passing sustainability badge.", "notice warning"));
    if (type === "quest") detail.append(paragraph("Evidence tracking only. This quest grants no machine permission.", "notice"));
    const fields = type === "resource" ? RESOURCE_FIELDS : DETAIL_FIELDS[type] || [
      ["Reported state", "status"], ["Validation state", "validation_status"], ["Kind", "kind"],
      ["Description", "description"], ["Source commit", "source_commit"], ["Evidence", "evidence", "evidence"],
      ["Details", "details"], ["Summary", "summary"], ["Remediation", "remediation"],
      ["Format", "format"], ["Dimensions", "dimensions"], ["Width", "width"], ["Height", "height"]
    ];
    const dl = element("dl");
    fields.forEach(([name, key, preferred]) => {
      const value = key === "preview" && typeof record[key] === "string" ? sourceReference(record[key]) : dataValue(record[key], preferred);
      dl.append(element("dt", "", name), element("dd", "", value));
    });
    if (typeof entry === "string") dl.append(element("dt", "", "Reported entry"), element("dd", "", M.narrative(entry)));
    detail.append(dl);
    if (focus) titleNode.focus();
  }

  function renderIdentity() {
    const context = state.model.context;
    const identity = document.getElementById("identity");
    identity.replaceChildren();
    const modeLabel = context.mode === "stable" ? "Stable / generated snapshot" : context.mode === "pr" ? "PR / provisional" : context.mode === "local" ? "Local / provisional" : "Mode not reported / provisional";
    identity.append(element("strong", "", modeLabel));
    [
      ["Repository", external(M.text(context.repository), context.repository_url)],
      ["Branch", element("code", "", M.text(context.branch))],
      ["Commit", element("code", "", M.text(context.commit))],
      ["Generated", M.text(context.generated_at)],
      ["State", badge(context.status)]
    ].forEach(([name, value]) => identity.append(element("span", "", [element("span", "identity-label", name), value])));
    if (M.reported(context.pull_request_url)) identity.append(element("span", "", [external("Pull request", context.pull_request_url), badge(context.pull_request_state)]));
    document.getElementById("snapshot-notice").textContent = context.mode === "local"
      ? "Provisional local projection. Refresh refetches data.json; it does not run a build, audit, CI job or acceptance check."
      : "Fixed generated snapshot, not live activity. Refresh only reloads published data.json. Missing evidence remains not reported / not run.";
    document.getElementById("footer-revision").textContent = "Schema 1 / " + M.text(context.commit);
    refresh.textContent = context.mode === "local" ? "Refetch local data" : "Reload snapshot";
  }

  function renderNavigation() {
    const tiers = document.getElementById("tier-navigation");
    tiers.replaceChildren();
    for (let id = 0; id < 16; id++) {
      const node = link("T" + id, href("tier/" + id));
      const tier = state.model ? M.tier(state.model, id) : null;
      node.title = "T" + id + " - " + (tier ? M.text(tier.name) + " / " + M.label(tier.implementation_state) : "Not reported");
      if (state.model?.context.active_tier === id) node.title += " / active tier";
      node.setAttribute("aria-label", node.title);
      node.dataset.active = String(state.model?.context.active_tier === id);
      tiers.append(node);
    }
    document.querySelectorAll("#navigation a").forEach(node => {
      const selected = node.getAttribute("href").split("?")[0] === href(state.path);
      if (selected) node.setAttribute("aria-current", "page"); else node.removeAttribute("aria-current");
    });
  }

  function render(focus) {
    if (!state.model) return;
    main.replaceChildren();
    renderNavigation();
    const tierMatch = /^tier\/(\d+)$/.exec(state.path);
    if (tierMatch && Number(tierMatch[1]) <= 15) {
      renderTier(Number(tierMatch[1]));
    } else if (!VIEWS.has(state.path)) {
      title("View not found", "This route is not a dashboard view.");
      main.append(paragraph(link("Return to the repository overview", href("overview")), "empty-state"));
    } else {
      const renderers = {
        overview: renderOverview, resources: () => renderResources(false), byproducts: () => renderResources(true),
        production: renderProduction, machines: () => renderMachines(false), logistics: () => renderMachines(true),
        quests: renderQuests, assets: renderAssets, validation: renderValidation
      };
      renderers[state.path]();
    }
    renderDetail(focus === "detail");
    main.setAttribute("aria-busy", "false");
    const heading = document.getElementById("view-title");
    if (focus === "view" && heading) heading.focus({ preventScroll: true });
    document.title = "Grindless | " + (heading ? heading.textContent : "Repository dashboard");
  }

  function readRoute() {
    const raw = location.hash.startsWith("#/") ? location.hash.slice(2) : "overview";
    const split = raw.indexOf("?");
    return { path: split < 0 ? raw : raw.slice(0, split), params: new URLSearchParams(split < 0 ? "" : raw.slice(split + 1)) };
  }

  function handleRoute() {
    const previousPath = state.path;
    const previousItem = state.params.get("item");
    const route = readRoute();
    state.path = route.path;
    state.params = route.params;
    render(previousPath !== state.path ? "view" : previousItem !== state.params.get("item") ? state.params.has("item") ? "detail" : "view" : null);
  }

  async function load() {
    if (state.loading) return;
    state.loading = true;
    refresh.disabled = true;
    refresh.textContent = "Reading data.json...";
    main.setAttribute("aria-busy", "true");
    announcement.textContent = "Loading repository snapshot.";
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 12000);
    let stage = "fetch";
    try {
      const response = await fetch(new URL("data.json", document.baseURI), { cache: "no-store", credentials: "same-origin", signal: controller.signal });
      if (!response.ok) throw new Error("Could not load data.json (HTTP " + response.status + ").");
      stage = "json";
      const raw = await response.json();
      stage = "schema";
      const model = M.snapshot(raw);
      state.model = model;
      renderIdentity();
      handleRoute();
      announcement.textContent = "Snapshot loaded. Source commit: " + M.text(model.context.commit) + ".";
    } catch (error) {
      state.model = null;
      const message = error.name === "AbortError" ? "Loading data.json timed out." : stage === "json" ? "data.json is not valid JSON." : stage === "schema" ? M.text(error.message) : error instanceof TypeError ? "data.json could not be fetched. Serve this directory over HTTP." : M.text(error.message);
      main.replaceChildren();
      title("Repository data unavailable", "No cached or demonstration snapshot is substituted.");
      const alert = paragraph(message + " Generate a valid schema-1 data.json beside index.html, serve both files from the same origin, then use Retry loading data.", "notice error");
      alert.setAttribute("role", "alert");
      main.append(alert);
      detail.replaceChildren(element("h2", "", "No snapshot to inspect"), paragraph("No resource, classification or validation state is available."));
      detail.firstElementChild.id = "detail-title";
      document.getElementById("identity").replaceChildren(element("strong", "", "Snapshot unavailable"), badge("Not reported", "neutral"));
      document.getElementById("snapshot-notice").textContent = "Loading failed. Previous data is not shown as current evidence.";
      document.getElementById("footer-revision").textContent = "Schema 1 / revision not available";
      refresh.textContent = "Retry loading data";
      renderNavigation();
      main.setAttribute("aria-busy", "false");
      announcement.textContent = "Snapshot unavailable. No fallback data loaded.";
    } finally {
      clearTimeout(timeout);
      state.loading = false;
      refresh.disabled = false;
    }
  }

  refresh.addEventListener("click", load);
  document.querySelector(".skip-link").addEventListener("click", event => {
    event.preventDefault();
    main.focus();
    main.scrollIntoView({ block: "start" });
  });
  window.addEventListener("hashchange", handleRoute);
  renderNavigation();
  load();
})();
