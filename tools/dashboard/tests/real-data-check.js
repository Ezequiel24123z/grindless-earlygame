/* Run in the browser page context of an actual generated artifact, not the synthetic harness. */
async function checkCanonicalDashboard() {
  "use strict";
  const M = window.GrindlessDashboardModel;
  const response = await fetch("data.json", { cache: "no-store" });
  if (!response.ok) throw new Error("The real generated dataset is unavailable.");
  const data = await response.json();
  const model = M.snapshot(data);
  const checks = [];
  const check = (name, pass, observed) => {
    checks.push({ name, pass: Boolean(pass), observed });
    if (!pass) throw new Error(name + ": " + JSON.stringify(observed));
  };
  const main = document.getElementById("main-content");
  const detail = document.getElementById("detail-panel");
  const wait = async predicate => {
    const deadline = performance.now() + 8000;
    while (!predicate()) {
      if (performance.now() >= deadline) throw new Error("Timed out waiting for the actual dashboard view.");
      await new Promise(resolve => setTimeout(resolve, 25));
    }
  };
  const view = async (path, params, heading) => {
    location.hash = "#/" + path + (params ? "?" + new URLSearchParams(params).toString() : "");
    await wait(() => main.querySelector("h1")?.textContent.includes(heading));
    await new Promise(resolve => setTimeout(resolve, 40));
  };
  const field = name => Array.from(detail.querySelectorAll("dt"))
    .find(node => node.textContent === name)?.nextElementSibling?.textContent;
  await view("overview", null, "Repository overview");
  check("full commit is visible", document.getElementById("identity").textContent.includes(data.context.commit), data.context.commit);
  check("all sixteen tier routes remain available", document.querySelectorAll("#tier-navigation a").length === 16,
    document.querySelectorAll("#tier-navigation a").length);
  check("unknown deliverables do not become a percentage", data.validation.required_deliverables === null
    && main.textContent.includes("No completion measure can be calculated"), data.validation.required_deliverables);
  check("canonical counts survive model loading", model.resources.length === data.summary.resources
    && model.routes.length === data.summary.routes && model.quests.length === data.summary.objectives,
  { resources: model.resources.length, routes: model.routes.length, tags: data.tags.length, quests: model.quests.length });

  const unknown = model.resources.find(entry => entry.annotation === null);
  if (!unknown) throw new Error("The real inventory has no unknown-resource case.");
  await view("resources", { q: unknown.id, item: "resource:" + unknown.id }, "Resource sustainability");
  check("unknown resource is selectable", detail.querySelector("#detail-title").textContent === unknown.name, unknown.id);
  check("unknown first use stays unreported", field("First use tier") === "Not reported", field("First use tier"));
  check("unknown disposal is not permission", detail.textContent.includes("Discard permission is not reported"), unknown.handling);
  const slag = model.resourceIndex.get("item:grindless:slag");
  if (!slag) throw new Error("The actual canonical slag resource is missing.");
  await view("byproducts", { q: slag.id, item: "resource:" + slag.id }, "By-products");
  check("side stream retains the declared class", M.classifications(slag, "byproduct").includes("potential_future_use")
    && detail.textContent.includes("Potential future use"), slag.byproduct_classes);
  check("unplaced slag use stays unreported", field("First use tier") === "Not reported", field("First use tier"));
  check("declared handling is visible", field("Handling")?.includes("Discard allowed") && field("Handling")?.includes("Yes"), field("Handling"));

  const future = model.tiers.find(entry => entry.id > 0 && entry.unknown !== null);
  if (!future) throw new Error("The real dataset has no unassessed future-tier case.");
  await view("tier/" + future.id, null, "T" + future.id);
  check("future tier does not inherit T0 entry", future.entry === null
    && main.querySelector(".facts")?.textContent.includes("Not reported"), future.entry);
  check("unassessed future diagnostics are scoped", main.textContent.includes("Unassessed ledger"), future.unknown);

  const route = model.routeIndex.get("bootstrap/insulators");
  if (!route) throw new Error("The actual canonical multi-output route is missing.");
  await view("production", { root: "route:" + route.id, item: "route:" + route.id }, "Production dependencies");
  const network = M.graph(model, "production");
  const neighborhood = M.neighborhood(network, "route:" + route.id, { depth: 1, limit: 48 });
  check("real production graph includes canonical nodes", network.nodes.size === model.resources.length + model.routes.length,
    { nodes: network.nodes.size, edges: network.edges.length });
  check("real graph renders the exact bounded neighborhood", neighborhood.nodes.length <= 48
    && main.querySelectorAll(".network-stage .graph-node").length === neighborhood.nodes.length,
  { expectedShown: neighborhood.nodes.length, rendered: main.querySelectorAll(".network-stage .graph-node").length });
  check("actual batch amounts remain visible", route.outputs.every(output => field("Outputs (amount / role)")?.includes(String(output.amount))),
    route.outputs.map(output => ({ resource_id: output.resource_id, amount: output.amount, role: output.role })));
  check("undeclared output roles are not invented", route.outputs.every(output => output.declared_role !== null || output.role === null),
    route.outputs.map(output => output.role));
  const large = model.routes.find(entry => entry.inputs.concat(entry.outputs)
    .some(amount => typeof amount.amount === "string" && /^-?\d+$/.test(amount.amount)));
  if (large) {
    await view("production", { root: "route:" + large.id, item: "route:" + large.id }, "Production dependencies");
    const amounts = large.inputs.concat(large.outputs).filter(amount => typeof amount.amount === "string");
    check("actual large native quantities retain every decimal digit", amounts.every(amount => detail.textContent.includes(amount.amount)),
      amounts.map(amount => ({ resource_id: amount.resource_id, amount: amount.amount })));
  }

  await view("validation", null, "Tests");
  check("actual conditional gate is displayed", main.textContent.includes(data.active_t0_conditional_gate.status.replace(/_/g, " ")),
    data.active_t0_conditional_gate.status);
  const originalFetch = window.fetch;
  const requests = [];
  window.fetch = function (...args) {
    requests.push(String(args[0]));
    return originalFetch.apply(this, args);
  };
  try {
    document.getElementById("refresh").click();
    await wait(() => requests.length > 0 && !document.getElementById("refresh").disabled);
    check("refresh fetches real data exactly once", requests.length === 1 && requests[0].includes("data.json"), requests);
    check("refresh preserves the actual revision", document.getElementById("identity").textContent.includes(data.context.commit), data.context.commit);
    check("refresh preserves the selected view", main.querySelector("h1").textContent.includes("Tests"), main.querySelector("h1").textContent);
  } finally {
    window.fetch = originalFetch;
  }
  await view("overview", null, "Repository overview");
  return {
    source_commit: data.context.commit,
    generated_at: data.context.generated_at,
    page_url: location.href,
    collections: { resources: model.resources.length, routes: model.routes.length, tags: data.tags.length,
      quests: model.quests.length, machines: model.machines.length, findings: model.findings.length,
      tiers: model.tiers.length, asset_entries: M.list(model.assets.entries).length },
    active_t0_conditional_gate: { pass: data.active_t0_conditional_gate.pass, status: data.active_t0_conditional_gate.status,
      scope: data.active_t0_conditional_gate.scope },
    assertions: checks,
    scope: "Real-data browser binding only; no gameplay, global sustainability, Tier or merge acceptance."
  };
}
