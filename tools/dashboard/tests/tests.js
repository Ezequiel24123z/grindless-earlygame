(async function () {
  "use strict";

  const M = window.GrindlessDashboardModel;
  const results = [];
  const list = document.getElementById("test-results");
  const status = document.getElementById("test-status");
  const desktop = document.getElementById("desktop");
  const mobile = document.getElementById("mobile");
  const original = await (await fetch("fixtures/full.json", { cache: "no-store" })).json();
  const clone = value => JSON.parse(JSON.stringify(value));
  const model = M.snapshot(clone(original));

  function assert(condition, message) {
    if (!condition) throw new Error(message || "Assertion failed");
  }

  function equal(actual, expected, message) {
    assert(actual === expected, (message || "Unexpected value") + ": expected " + expected + ", got " + actual);
  }

  async function test(name, callback) {
    try {
      await callback();
      results.push({ name, passed: true });
    } catch (error) {
      results.push({ name, passed: false, error: error.message });
    }
    const result = results[results.length - 1];
    const li = document.createElement("li");
    li.dataset.result = result.passed ? "pass" : "fail";
    li.textContent = (result.passed ? "PASS: " : "FAIL: ") + name + (result.error ? " - " + result.error : "");
    list.append(li);
    status.textContent = results.filter(result => result.passed).length + " / " + results.length + " checks passed so far.";
  }

  async function until(predicate, message) {
    const deadline = performance.now() + 6000;
    while (performance.now() < deadline) {
      if (predicate()) return;
      await new Promise(resolve => setTimeout(resolve, 20));
    }
    throw new Error(message || "Timed out waiting for renderer");
  }

  async function load(frame, name, route) {
    frame.src = "/cases/" + name + "/index.html#/" + (route || "overview");
    await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error("Frame load timed out")), 6000);
      frame.onload = () => { clearTimeout(timer); resolve(); };
    });
    await until(() => frame.contentDocument.querySelector("main")?.getAttribute("aria-busy") === "false");
    return frame.contentDocument;
  }

  async function route(frame, name) {
    const win = frame.contentWindow;
    const hash = "#/" + name;
    if (win.location.hash !== hash) {
      await new Promise(resolve => {
        win.addEventListener("hashchange", resolve, { once: true });
        win.location.hash = hash;
      });
    }
    await until(() => frame.contentDocument.getElementById("view-title")?.textContent === ({
      resources: "Resource sustainability", byproducts: "By-products & waste", machines: "Machines & multiblocks",
      logistics: "Logistics & automation", production: "Production dependencies", quests: "Questbook",
      assets: "Assets", validation: "Tests, blockers & manual acceptance", overview: "Repository overview"
    }[name] || name), "Route did not render: " + name);
    return frame.contentDocument;
  }

  function choose(doc, id, value) {
    const node = doc.getElementById(id);
    assert(node, "Missing control: " + id);
    node.value = value;
    node.dispatchEvent(new doc.defaultView.Event("change", { bubbles: true }));
  }

  function search(doc, value) {
    const node = doc.getElementById("filter-q");
    node.value = value;
    node.dispatchEvent(new doc.defaultView.Event("input", { bubbles: true }));
  }

  function throws(callback) {
    let threw = false;
    try { callback(); } catch (_) { threw = true; }
    assert(threw, "Invalid snapshot was accepted");
  }

  await test("schema 1 rejects mismatches, malformed collections and duplicate catalogue ids", () => {
    throws(() => M.snapshot({ schema_version: 2, context: {} }));
    throws(() => M.snapshot({ schema_version: 1, context: {}, resources: {} }));
    const duplicate = clone(original);
    duplicate.resources.push(clone(duplicate.resources[0]));
    throws(() => M.snapshot(duplicate));
  });
  await test("null tiers are not T0 and unknown statuses never become passing badges", () => {
    equal(M.status("a_future_status").tone, "neutral");
    equal(M.status("cancelled").tone, "warning");
    equal(M.status("skipped").tone, "warning");
    equal(M.status(null).label, "Not reported");
    assert(M.filterResources(model.resources, { tier: "0" }).every(resource => resource.first_use_tier === 0 || resource.first_renewable_tier === 0));
    assert(!M.filterResources(model.resources, { tier: "0" }).some(resource => resource.id === "test:potential"));
  });
  await test("progress exposes 1 / 4 explicit deliverables bound to the exact commit", () => {
    const result = M.deliverables(model);
    equal(result.numerator, 1);
    equal(result.denominator, 4);
    const noRevision = clone(original);
    noRevision.context.commit = null;
    equal(M.deliverables(M.snapshot(noRevision)).numerator, 0);
  });
  await test("all requested side-stream scopes use explicit data, never safe-disposal defaults", () => {
    const ids = filter => M.filterResources(M.wasteResources(model), { waste: filter }).map(resource => resource.id);
    equal(ids("current").join(), "test:grog");
    equal(ids("future").join(), "test:future");
    equal(ids("potential").join(), "test:potential");
    equal(ids("recyclable").join(), "test:grog");
    equal(ids("hazardous").join(), "test:hazard");
    equal(ids("inert").join(), "test:inert");
    equal(ids("disposable").join(), "test:inert");
    equal(ids("spent").join(), "test:spent");
    equal(ids("mandatory").join(), "test:hazard");
    assert(ids("unresolved").includes("test:hazard"));
    assert(!ids("disposable").includes("test:unknown-handling"));
    assert(M.wasteResources(model).some(resource => resource.id === "test:loss"));
    assert(M.wasteResources(model).some(resource => resource.id === "test:emission"));
  });
  await test("graph retains cycles, catalysts, optional routes, recycling and unresolved references", () => {
    const network = M.graph(model, "production");
    assert(network.nodes.has("resource:test:not-in-snapshot"));
    assert(network.edges.some(edge => edge.catalytic));
    assert(network.edges.some(edge => edge.optional));
    assert(network.edges.some(edge => edge.recycling));
    const area = M.neighborhood(network, "resource:test:clay", { depth: 3 });
    assert(new Set(area.nodes.map(node => node.id)).size === area.nodes.length);
    assert(area.edges.some(edge => edge.recycling));
    const filtered = M.neighborhood(network, "resource:test:clay", { depth: 3, catalytic: false, optional: false, recycling: false });
    assert(filtered.edges.every(edge => !edge.catalytic && !edge.optional && !edge.recycling));
    const quests = M.graph(model, "quests");
    equal(M.neighborhood(quests, "quest:test:cycle-a", { depth: 3 }).nodes.length, 2);
  });
  await test("large graph uses a deterministic 48-node bound and honest shown/total counts", () => {
    const large = clone(original);
    large.resources = [{ id: "test:hub", name: "Synthetic hub" }];
    large.routes = [];
    for (let i = 0; i < 90; i++) {
      large.routes.push({ id: "test:route-" + String(i).padStart(3, "0"), inputs: [{ resource_id: "test:hub", consumed: true }], outputs: [] });
    }
    const network = M.graph(M.snapshot(large), "production");
    const first = M.neighborhood(network, "resource:test:hub", { depth: 3 });
    const second = M.neighborhood(network, "resource:test:hub", { depth: 3 });
    equal(first.nodes.length, 48);
    equal(first.totalNodes, 91);
    equal(first.totalEdges, 90);
    assert(first.truncated);
    equal(first.nodes.map(node => node.id).join(), second.nodes.map(node => node.id).join());
  });
  await test("unsafe URLs, local paths, credentials and raw stack traces are not rendered", () => {
    equal(M.httpsUrl("javascript:alert(1)"), null);
    equal(M.httpsUrl("https://user:password@example.com/repo"), null);
    equal(M.httpsUrl("https://example.com/repo?token=test"), null);
    equal(M.repositoryPath("../private.json"), null);
    equal(M.repositoryPath("C:\\Users\\Synthetic\\secret.txt"), null);
    equal(M.repositoryPath("/home/synthetic/secret.txt"), null);
    assert(!M.text("C:\\Users\\Synthetic\\secret.txt").includes("Synthetic"));
    assert(!M.text("ghp_syntheticFixtureTokenOnly1234").includes("syntheticFixture"));
    assert(!M.text("Authorization: Basic c3ludGhldGljOnRlc3Q=").includes("c3ludGhldGlj"));
    assert(!M.narrative("Caused by: synthetic\n at fake.Method(Fake.java:1)").includes("fake.Method"));
    assert(M.sourceUrl(model.context, "tests/catalogue.json").includes("/blob/" + original.context.commit + "/tests/catalogue.json"));
  });

  let doc = await load(desktop, "full", "overview");
  await test("real overview shows identity, an exact fraction and sixteen navigable tier routes", () => {
    assert(doc.getElementById("identity").textContent.includes(original.context.commit));
    assert(doc.getElementById("identity").textContent.includes("provisional"));
    assert(doc.querySelector(".fraction").textContent === "1 / 4");
    equal(doc.querySelectorAll("#tier-navigation a").length, 16);
    assert(!doc.querySelector("main").textContent.includes("%"));
  });
  await test("tier navigation preserves planned and absent-tier uncertainty, including T15", async () => {
    doc.querySelector('#tier-navigation a[href="#/tier/15"]').click();
    await until(() => doc.querySelector("h1")?.textContent.startsWith("T15"));
    assert(doc.querySelector("main").textContent.includes("no entry in the snapshot"));
    equal(doc.querySelector('#tier-navigation a[aria-current="page"]').getAttribute("href"), "#/tier/15");
    doc.querySelector('#tier-navigation a[href="#/tier/1"]').click();
    await until(() => doc.querySelector("h1")?.textContent.startsWith("T1 -"));
    assert(doc.querySelector("main").textContent.includes("planned"));
    doc.querySelector('#tier-navigation a[href="#/tier/0"]').click();
    await until(() => doc.querySelector("h1")?.textContent.startsWith("T0 -"));
    assert(doc.querySelector("main").textContent.includes("Synthetic physical evidence"));
  });
  await test("real resource filters preserve search focus and safely display hostile/long names", async () => {
    doc = await route(desktop, "resources");
    assert(doc.querySelector("#resource-results").textContent.includes("Test renewable label"));
    assert(!doc.querySelector("#resource-results img"));
    assert(!desktop.contentWindow.syntheticInjection);
    search(doc, "test:html");
    equal(doc.querySelectorAll("#resource-results tbody tr").length, 1);
    assert(doc.querySelector("#resource-results").textContent.includes("<img"));
    const field = doc.getElementById("filter-q");
    field.focus();
    search(doc, "Synthetic long resource");
    equal(doc.activeElement, field);
    equal(doc.querySelectorAll("#resource-results tbody tr").length, 1);
    doc.querySelector("#resource-results td a").click();
    await until(() => doc.getElementById("detail-title").textContent.startsWith("Synthetic long resource"));
    assert(doc.getElementById("detail-title").textContent.endsWith("detail panel"));
    equal(doc.activeElement.id, "detail-title");
    assert(doc.querySelector("#detail-panel a.source-ref").href.includes("/blob/" + original.context.commit + "/"));
  });
  await test("by-product table excludes unknown discard permission and exposes current/future filters", async () => {
    doc = await route(desktop, "byproducts");
    choose(doc, "filter-waste", "disposable");
    equal(doc.querySelectorAll("#resource-results tbody tr").length, 1);
    assert(doc.querySelector("#resource-results").textContent.includes("Synthetic disposable inert"));
    choose(doc, "filter-waste", "hazardous");
    equal(doc.querySelectorAll("#resource-results tbody tr").length, 1);
    assert(doc.querySelector("#resource-results").textContent.includes("Discard allowed: No"));
    choose(doc, "filter-waste", "future");
    assert(doc.querySelector("#resource-results").textContent.includes("Synthetic named committed plan"));
  });
  await test("production graph can select every node and exposes edge labels and node details", async () => {
    doc = await route(desktop, "production");
    const select = doc.getElementById("filter-root");
    equal(select.options.length, M.graph(model, "production").nodes.size);
    choose(doc, "filter-root", "route:test:mix");
    choose(doc, "filter-depth", "3");
    assert(doc.querySelector("main").textContent.includes("Filtered neighborhood:"));
    assert(doc.querySelector("main").textContent.includes("Catalyst (not consumed)"));
    assert(doc.querySelector("main").textContent.includes("Output: emission"));
    assert(doc.querySelector(".graph-node[data-center='true']").textContent.includes("Synthetic mixing"));
    doc.querySelector(".graph-node[data-center='true']").click();
    await until(() => doc.getElementById("detail-title").textContent === "Synthetic mixing route");
    assert(doc.querySelector("#detail-panel").textContent.includes("Duration (ticks)"));
  });
  await test("real large-graph rendering exposes 48 / 91 nodes without hiding its bound", async () => {
    doc = await route(desktop, "production");
    const win = desktop.contentWindow;
    const fetch = win.fetch;
    const large = clone(original);
    large.resources = [{ id: "test:hub", name: "Synthetic hub" }];
    large.routes = Array.from({ length: 90 }, (_, i) => ({ id: "test:large-" + String(i).padStart(3, "0"), inputs: [{ resource_id: "test:hub", consumed: true }], outputs: [] }));
    win.fetch = async () => new win.Response(JSON.stringify(large), { status: 200, headers: { "Content-Type": "application/json" } });
    doc.getElementById("refresh").click();
    await until(() => doc.querySelectorAll(".graph-node").length === 48);
    assert(doc.querySelector("main").textContent.includes("48 / 91 nodes"));
    assert(doc.querySelector("main").textContent.includes("48-node display limit"));
    equal(doc.getElementById("filter-root").options.length, 91);
    win.fetch = fetch;
    doc.getElementById("refresh").click();
    await until(() => doc.getElementById("filter-root").options.length === M.graph(model, "production").nodes.size);
  });
  await test("a persisted graph center missing from a new snapshot remains explicitly unresolved", async () => {
    desktop.contentWindow.location.hash = "#/production?root=resource%3Atest%3Ano-longer-present";
    await until(() => doc.querySelector("main").textContent.includes("The selected node is not in this snapshot"));
    equal(doc.getElementById("filter-root").selectedOptions[0].textContent, "Not in snapshot: resource:test:no-longer-present");
    equal(doc.querySelectorAll(".graph-node").length, 0);
    choose(doc, "filter-root", "resource:test:water");
    assert(doc.querySelectorAll(".graph-node").length > 0);
  });
  await test("machine kinds, logistics, quest physical evidence and all asset queues are reachable", async () => {
    doc = await route(desktop, "machines");
    choose(doc, "filter-kind", "standard_multiblock");
    equal(doc.querySelectorAll("main tbody tr").length, 1);
    choose(doc, "filter-kind", "assembled_multiblock");
    equal(doc.querySelectorAll("main tbody tr").length, 1);
    doc = await route(desktop, "logistics");
    assert(doc.querySelector("main").textContent.includes("Synthetic storage"));
    doc = await route(desktop, "quests");
    assert(doc.querySelector("main").textContent.includes("grant machine permissions"));
    assert(doc.querySelector("main").textContent.includes("Synthetic handling lesson"));
    assert(doc.querySelector("main").textContent.includes("Quest evidence dependency"));
    doc = await route(desktop, "assets");
    assert(doc.querySelector("main").textContent.includes("Synthetic generation request"));
    assert(doc.querySelector("main").textContent.includes("Synthetic unreviewed asset"));
    assert(doc.querySelector("main").textContent.includes("Synthetic dimensions check"));
  });
  await test("validation preserves cancelled checks, old revisions, waived debt and not-run manual work", async () => {
    doc = await route(desktop, "validation");
    const text = doc.querySelector("main").textContent;
    assert(text.includes("cancelled"));
    assert(text.includes("Different or unresolved revision"));
    assert(text.includes("Waived - visible debt"));
    assert(text.includes("not run"));
    assert(text.includes("Synthetic blocker"));
    doc.querySelector('a[href*="item=finding%3Atest%3Adebt"]').click();
    await until(() => doc.getElementById("detail-title").textContent === "test:debt");
    assert(doc.querySelector("#detail-panel").textContent.includes("not a passing sustainability badge"));
  });
  await test("keyboard targets use native links/controls, labels, visible focus and a skip target", async () => {
    doc = await route(desktop, "resources");
    const navigation = doc.querySelector("#navigation a");
    navigation.focus();
    equal(doc.activeElement, navigation);
    assert(navigation.matches("a[href]"));
    const input = doc.getElementById("filter-q");
    input.focus();
    equal(doc.activeElement, input);
    assert(input.closest("label"));
    const select = doc.getElementById("filter-tier");
    select.focus();
    equal(doc.activeElement, select);
    assert(select.closest("label"));
    const graphLink = doc.querySelector("#resource-results td a");
    graphLink.focus();
    equal(doc.activeElement, graphLink);
    assert(doc.querySelector('a.skip-link[href="#main-content"]'));
    assert(doc.getElementById("main-content").tabIndex === -1);
    assert(doc.querySelector('link[href="dashboard.css"]'));
  });
  await test("skip navigation focuses the current main region without replacing the selected view", async () => {
    doc = await route(desktop, "resources");
    const previous = desktop.contentWindow.location.hash;
    doc.querySelector(".skip-link").click();
    equal(doc.activeElement.id, "main-content");
    equal(desktop.contentWindow.location.hash, previous);
    equal(doc.querySelector("h1").textContent, "Resource sustainability");
  });
  await test("refresh failure clears stale data and retry restores only a successful fetch", async () => {
    const win = desktop.contentWindow;
    const fetch = win.fetch;
    win.fetch = async () => new win.Response("", { status: 404 });
    doc.getElementById("refresh").click();
    await until(() => doc.querySelector("h1")?.textContent === "Repository data unavailable");
    assert(!doc.querySelector("main").textContent.includes("Synthetic water"));
    assert(doc.getElementById("snapshot-notice").textContent.includes("Previous data is not shown"));
    equal(doc.getElementById("refresh").textContent, "Retry loading data");
    win.fetch = fetch;
    doc.getElementById("refresh").click();
    await until(() => doc.querySelector("h1")?.textContent === "Resource sustainability");
    assert(doc.querySelector("main").textContent.includes("Synthetic water"));
  });
  await test("PR/stable snapshot wording never invents live activity", async () => {
    const win = desktop.contentWindow;
    const fetch = win.fetch;
    for (const mode of ["pr", "stable"]) {
      const payload = clone(original);
      payload.context.mode = mode;
      win.fetch = async () => new win.Response(JSON.stringify(payload), { status: 200, headers: { "Content-Type": "application/json" } });
      doc.getElementById("refresh").click();
      await until(() => doc.getElementById("identity").textContent.includes(mode === "pr" ? "PR / provisional" : "Stable / generated"));
      assert(doc.getElementById("snapshot-notice").textContent.includes("not live activity"));
    }
    win.fetch = fetch;
    doc.getElementById("refresh").click();
    await until(() => doc.getElementById("identity").textContent.includes("Local / provisional"));
  });

  for (const [name, message] of [["missing", "HTTP 404"], ["schema", "schema_version 1"], ["invalid", "not valid JSON"]]) {
    await test(name + " data shows an actionable error without synthetic fallback", async () => {
      const frame = document.createElement("iframe");
      document.getElementById("case-mount").append(frame);
      const errorDoc = await load(frame, name);
      equal(errorDoc.querySelector("h1").textContent, "Repository data unavailable");
      assert(errorDoc.querySelector("main").textContent.includes(message));
      assert(errorDoc.querySelector("main").textContent.includes("No cached or demonstration"));
      frame.remove();
    });
  }
  await test("null fields produce unreported state in every view, not manufactured passes", async () => {
    const frame = document.createElement("iframe");
    document.getElementById("case-mount").append(frame);
    const nullDoc = await load(frame, "null");
    assert(nullDoc.querySelector("main").textContent.includes("No completion measure"));
    assert(nullDoc.getElementById("identity").textContent.includes("unrecognized future state"));
    for (const name of ["resources", "byproducts", "production", "machines", "logistics", "quests", "assets", "validation"]) {
      await route(frame, name);
      assert(!nullDoc.querySelector("main").textContent.includes("NaN"));
      assert(!nullDoc.querySelector("main").textContent.includes("[object Object]"));
    }
    assert(nullDoc.querySelector("main").textContent.includes("not reported / not run"));
    frame.remove();
  });
  await test("desktop real renderer contains horizontal scrolling inside tables/graphs, not the page", async () => {
    doc = await route(desktop, "resources");
    assert(doc.documentElement.scrollWidth <= desktop.contentWindow.innerWidth + 1, "Desktop page overflows horizontally");
    await route(desktop, "production");
    assert(doc.documentElement.scrollWidth <= desktop.contentWindow.innerWidth + 1, "Desktop graph page overflows horizontally");
  });
  await test("mobile real renderer stays within 390px with readable long-name detail and native filters", async () => {
    const mobileDoc = await load(mobile, "full", "resources?item=resource%3Atest%3Aclay");
    equal(mobile.contentWindow.innerWidth, 390);
    assert(mobileDoc.documentElement.scrollWidth <= 391, "Mobile page overflows horizontally");
    assert(mobileDoc.getElementById("detail-title").textContent.endsWith("detail panel"));
    const bodyText = mobileDoc.getElementById("detail-title");
    assert(bodyText.getBoundingClientRect().width <= 390);
    await route(mobile, "byproducts");
    choose(mobileDoc, "filter-waste", "hazardous");
    equal(mobileDoc.querySelectorAll("#resource-results tbody tr").length, 1);
    await route(mobile, "production");
    assert(mobileDoc.documentElement.scrollWidth <= 391, "Mobile graph controls overflow horizontally");
    await route(mobile, "resources");
  });

  await route(desktop, "byproducts");
  const passed = results.filter(result => result.passed).length;
  const failed = results.length - passed;
  window.dashboardTestResults = { passed, failed, tests: results };
  document.documentElement.dataset.tests = failed ? "failed" : "passed";
  status.textContent = passed + " / " + results.length + " checks passed; " + failed + " failed. Synthetic renderer checks only, not gameplay acceptance.";
})().catch(error => {
  const message = window.GrindlessDashboardModel.text(error.message);
  document.getElementById("test-status").textContent = "Test harness failed before completion: " + message;
  document.documentElement.dataset.tests = "failed";
  window.dashboardTestResults = { passed: 0, failed: 1, tests: [{ name: "Harness execution", passed: false, error: message }] };
});
