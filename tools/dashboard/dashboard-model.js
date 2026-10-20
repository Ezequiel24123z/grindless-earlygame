(function (global) {
  "use strict";

  const ARRAY_FIELDS = ["classifications", "tiers", "resources", "routes", "machines", "quests", "findings"];
  const COMPLETE = new Set(["passed", "pass", "success", "complete", "completed", "done", "accepted"]);
  const BAD = new Set(["failed", "failure", "error", "blocked", "critical", "high", "no-go"]);
  const WARNING = new Set(["pending", "planned", "not_run", "not run", "cancelled", "canceled", "skipped", "waived", "medium", "in_progress", "running"]);
  const list = value => Array.isArray(value) ? value : [];
  const object = value => value !== null && typeof value === "object" && !Array.isArray(value) ? value : {};
  const reported = value => value !== null && value !== undefined && value !== "";
  const keyOf = value => {
    if (typeof value === "string" || typeof value === "number") return String(value);
    const entry = object(value);
    return String(entry.resource_id || entry.route_id || entry.machine_id || entry.quest_id || entry.id || "");
  };
  const ordered = (a, b) => a < b ? -1 : a > b ? 1 : 0;

  function text(value, fallback) {
    if (!reported(value)) return fallback === undefined ? "Not reported" : fallback;
    if (typeof value !== "string" && typeof value !== "number" && typeof value !== "boolean") return "Structured data";
    return String(value)
      .replace(/\b[A-Za-z]:[\\/][^\s<>"']*/g, "[local path omitted]")
      .replace(/\\\\[^\s<>"']+/g, "[local path omitted]")
      .replace(/\/(?:home|Users|private|mnt)\/[^\s<>"']*/g, "[local path omitted]")
      .replace(/\b(?:gh[pousr]_[A-Za-z0-9_]+|github_pat_[A-Za-z0-9_]+|sk-[A-Za-z0-9_-]{16,})\b/g, "[credential omitted]")
      .replace(/\bBearer\s+[A-Za-z0-9_.~+/-]+=*/gi, "[credential omitted]")
      .replace(/\bAuthorization\s*:\s*Basic\s+\S+/gi, "[credential omitted]")
      .replace(/\b(password|token|secret|api[_ -]?key)\s*[:=]\s*\S+/gi, "$1=[credential omitted]")
      .replace(/https?:\/\/[^/\s]+@/gi, "https://[credentials omitted]@")
      .replace(/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi, "[contact omitted]");
  }

  function narrative(value) {
    if (typeof value === "string" && (value.split(/\r?\n/).length > 8 || /(?:^|\n)\s*(?:at [\w.$]+\(|Caused by:|\[\d{2}:\d{2}:\d{2}\])/.test(value))) {
      return "Raw log omitted. Use the canonical evidence reference.";
    }
    return text(value);
  }

  function label(value) {
    return text(value).replace(/_/g, " ");
  }

  function status(value) {
    const raw = typeof value === "string" ? value.toLowerCase().trim() : "";
    return { label: label(value), tone: COMPLETE.has(raw) ? "good" : BAD.has(raw) ? "bad" : WARNING.has(raw) ? "warning" : "neutral" };
  }

  function repositoryPath(value) {
    if (typeof value !== "string" || !value.trim()) return null;
    const path = value.replace(/\\/g, "/");
    if (/^(?:[A-Za-z]:|\/)|(?:^|\/)\.\.?(?:\/|$)|[?\u0000-\u001f]/.test(path)) return null;
    if (!/^[A-Za-z0-9_@./ -]+(?:#[A-Za-z0-9_.:-]+)?$/.test(path)) return null;
    if (/(?:^|\/)(?:\.env(?:\.|$)|id_rsa|credentials(?:\.|$)|secrets?(?:\.|$))/i.test(path)) return null;
    return path;
  }

  function httpsUrl(value) {
    if (typeof value !== "string") return null;
    try {
      const url = new URL(value);
      if (url.protocol !== "https:" || url.username || url.password || url.search) return null;
      if (/^(?:localhost|127\.|0\.|192\.168\.|10\.|\[?::1\]?)/i.test(url.hostname)) return null;
      if (text(value) !== value) return null;
      return url.href;
    } catch (_) {
      return null;
    }
  }

  function sourceUrl(context, path) {
    const safePath = repositoryPath(path);
    const base = httpsUrl(object(context).repository_url);
    const commit = object(context).commit;
    if (!safePath || !base || typeof commit !== "string" || !/^[0-9a-f]{7,40}$/i.test(commit)) return null;
    const repository = new URL(base);
    if (!/^\/[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+\/?$/.test(repository.pathname)) return null;
    const pieces = safePath.split("#");
    const encodedPath = pieces[0].split("/").map(encodeURIComponent).join("/");
    return base.replace(/\/$/, "").replace(/\.git$/, "") + "/blob/" + encodeURIComponent(commit) + "/" + encodedPath + (pieces[1] ? "#" + encodeURIComponent(pieces[1]) : "");
  }

  function index(entries, name) {
    const result = new Map();
    for (const entry of entries) {
      if (!entry || typeof entry !== "object" || Array.isArray(entry) || !reported(entry.id)) {
        throw new Error(name + " contains an entry without an id.");
      }
      const id = String(entry.id);
      if (result.has(id)) throw new Error(name + " contains duplicate ids.");
      result.set(id, entry);
    }
    return result;
  }

  function snapshot(raw) {
    if (!raw || typeof raw !== "object" || Array.isArray(raw) || raw.schema_version !== 1) {
      throw new Error("Expected dashboard schema_version 1.");
    }
    if (!raw.context || typeof raw.context !== "object" || Array.isArray(raw.context)) {
      throw new Error("The snapshot context is missing or invalid.");
    }
    const model = { context: raw.context, reported: {}, schema_version: 1 };
    for (const field of ARRAY_FIELDS) {
      if (reported(raw[field]) && !Array.isArray(raw[field])) throw new Error(field + " must be an array or null.");
      model[field] = list(raw[field]);
      model.reported[field] = Array.isArray(raw[field]);
    }
    for (const field of ["assets", "validation"]) {
      if (reported(raw[field]) && (typeof raw[field] !== "object" || Array.isArray(raw[field]))) throw new Error(field + " must be an object or null.");
      model[field] = object(raw[field]);
      model.reported[field] = reported(raw[field]);
    }
    for (const [parent, fields] of [["assets", ["entries", "generation_queue", "qc_queue", "checks"]], ["validation", ["checks", "ci", "manual_acceptance", "required_deliverables"]]]) {
      for (const field of fields) {
        if (reported(model[parent][field]) && !Array.isArray(model[parent][field])) throw new Error(parent + "." + field + " must be an array or null.");
      }
    }
    model.resourceIndex = index(model.resources, "resources");
    model.routeIndex = index(model.routes, "routes");
    model.machineIndex = index(model.machines, "machines");
    model.questIndex = index(model.quests, "quests");
    model.findingIndex = index(model.findings, "findings");
    model.classificationIndex = index(model.classifications, "classifications");
    model.tierIndex = index(model.tiers, "tiers");
    for (const tier of model.tiers) {
      if (!Number.isInteger(tier.id) || tier.id < 0 || tier.id > 15) throw new Error("Tier ids must be integers from 0 through 15.");
    }
    return model;
  }

  function tier(model, id) {
    return model.tierIndex.get(String(id)) || { id, name: "Name not reported", implementation_state: null, unreported: true };
  }

  function classifications(resource, domain) {
    return list(object(resource)[domain === "byproduct" ? "byproduct_classes" : "resource_classes"]).map(keyOf);
  }

  function wasteResources(model) {
    const sideStreams = new Set();
    for (const route of model.routes) {
      for (const output of list(route.outputs)) {
        if (["byproduct", "emission", "loss"].includes(object(output).role)) sideStreams.add(keyOf(output));
      }
    }
    return model.resources.filter(resource => classifications(resource, "byproduct").length > 0 || sideStreams.has(String(resource.id)) || object(resource.handling).hazardous === true);
  }

  function matchesWaste(resource, filter) {
    const classes = new Set(classifications(resource, "byproduct"));
    const handling = object(resource.handling);
    switch (filter) {
      case "current": return list(resource.current_consumers).length > 0 || classes.has("byproduct_current");
      case "future": return list(resource.future_consumers).length > 0 || classes.has("byproduct_future");
      case "potential": return classes.has("potential_future_use");
      case "recyclable": return list(resource.recycling_routes).length > 0 || classes.has("waste_recoverable");
      case "hazardous": return handling.hazardous === true || classes.has("waste_hazardous");
      case "inert": return classes.has("waste_inert");
      case "disposable": return handling.discard_allowed === true;
      case "spent": return classes.has("catalyst_spent");
      case "mandatory": return resource.mandatory_for_progression === true;
      case "unresolved": return list(resource.finding_ids).length > 0 || !reported(resource.validation_status) || ["unresolved", "failed", "blocked", "unknown", "not_reported"].includes(resource.validation_status);
      default: return true;
    }
  }

  function filterResources(resources, filters) {
    const query = String(filters.q || "").trim().toLowerCase();
    return resources.filter(resource => {
      const tiers = [resource.first_use_tier, resource.first_renewable_tier].filter(reported).map(String);
      const ids = classifications(resource, "resource").concat(classifications(resource, "byproduct"));
      return (!query || [resource.id, resource.name, resource.kind, resource.description].filter(reported).join(" ").toLowerCase().includes(query))
        && (!filters.tier || tiers.includes(filters.tier))
        && (!filters.classification || ids.includes(filters.classification))
        && (!filters.mandatory || resource.mandatory_for_progression === true)
        && matchesWaste(resource, filters.waste);
    });
  }

  function deliverables(model) {
    const entries = list(model.validation.required_deliverables);
    const commit = model.context.commit;
    const current = entry => COMPLETE.has(String(object(entry).status || "").toLowerCase()) && typeof commit === "string" && /^[0-9a-f]{7,40}$/i.test(commit) && object(entry).source_commit === commit;
    return {
      numerator: entries.filter(current).length,
      denominator: entries.length,
      reported: Array.isArray(model.validation.required_deliverables),
      entries
    };
  }

  function graph(model, kind) {
    const nodes = new Map();
    const edges = [];
    function addNode(type, id, entry) {
      const key = type + ":" + id;
      if (!nodes.has(key)) nodes.set(key, { id: key, entityId: String(id), type, name: entry ? text(entry.name || entry.id) : text(id) + " (not in snapshot)", entry: entry || null });
      return key;
    }
    if (kind === "quests") {
      for (const quest of model.quests) addNode("quest", quest.id, quest);
      for (const quest of model.quests) {
        list(quest.requires).forEach((required, i) => {
          const id = keyOf(required);
          if (!id) return;
          edges.push({ id: quest.id + ":requires:" + i, from: addNode("quest", id, model.questIndex.get(id)), to: "quest:" + quest.id, label: "Quest evidence dependency", amount: null, optional: quest.optional === true, catalytic: false, recycling: false });
        });
      }
    } else {
      for (const resource of model.resources) addNode("resource", resource.id, resource);
      for (const route of model.routes) {
        const routeNode = addNode("route", route.id, route);
        list(route.inputs).forEach((input, i) => {
          const item = object(input);
          const id = keyOf(input);
          if (!id) return;
          edges.push({
            id: route.id + ":in:" + i, from: addNode("resource", id, model.resourceIndex.get(id)), to: routeNode,
            label: item.consumed === false ? "Catalyst (not consumed)" : item.consumed === true ? "Consumed input" : "Input; consumption not reported",
            amount: item.amount, optional: route.canonical === false, catalytic: item.consumed === false, recycling: route.origin === "recycle"
          });
        });
        list(route.outputs).forEach((output, i) => {
          const item = object(output);
          const id = keyOf(output);
          if (!id) return;
          edges.push({
            id: route.id + ":out:" + i, from: routeNode, to: addNode("resource", id, model.resourceIndex.get(id)),
            label: reported(item.role) ? "Output: " + label(item.role) : "Output role not reported",
            amount: item.amount, optional: route.canonical === false, catalytic: false, recycling: route.origin === "recycle"
          });
        });
      }
      for (const resource of model.resources) {
        list(resource.input_dependencies).forEach((dependency, i) => {
          const id = keyOf(dependency);
          if (!id) return;
          const item = object(dependency);
          edges.push({ id: resource.id + ":dependency:" + i, from: addNode("resource", id, model.resourceIndex.get(id)), to: "resource:" + resource.id, label: "Declared resource dependency", amount: item.amount, optional: item.optional === true, catalytic: item.consumed === false, recycling: item.recycling === true });
        });
      }
    }
    return { nodes, edges };
  }

  function neighborhood(network, center, options) {
    const settings = Object.assign({ depth: 1, limit: 48, optional: true, catalytic: true, recycling: true }, options);
    const eligible = network.edges.filter(edge => (settings.optional || !edge.optional) && (settings.catalytic || !edge.catalytic) && (settings.recycling || !edge.recycling));
    const adjacency = new Map();
    for (const edge of eligible) {
      for (const key of [edge.from, edge.to]) {
        if (!adjacency.has(key)) adjacency.set(key, []);
        adjacency.get(key).push(edge);
      }
    }
    const levels = new Map();
    if (network.nodes.has(center)) levels.set(center, 0);
    const queue = levels.size ? [{ id: center, depth: 0 }] : [];
    let truncated = false;
    for (let cursor = 0; cursor < queue.length; cursor++) {
      const current = queue[cursor];
      if (current.depth >= Math.max(0, Math.min(3, Number(settings.depth) || 0))) continue;
      const neighbors = list(adjacency.get(current.id)).map(edge => ({ edge, id: edge.from === current.id ? edge.to : edge.from })).sort((a, b) => ordered(a.id, b.id));
      for (const neighbor of neighbors) {
        if (levels.has(neighbor.id)) continue;
        if (levels.size >= settings.limit) { truncated = true; continue; }
        const direction = neighbor.edge.to === current.id ? -1 : 1;
        levels.set(neighbor.id, levels.get(current.id) + direction);
        queue.push({ id: neighbor.id, depth: current.depth + 1 });
      }
    }
    return {
      nodes: Array.from(levels.keys()).map(id => Object.assign({}, network.nodes.get(id), { level: levels.get(id) })),
      edges: eligible.filter(edge => levels.has(edge.from) && levels.has(edge.to)),
      totalNodes: network.nodes.size,
      totalEdges: network.edges.length,
      eligibleEdges: eligible.length,
      truncated
    };
  }

  global.GrindlessDashboardModel = Object.freeze({
    list, object, reported, keyOf, ordered, text, narrative, label, status, repositoryPath, httpsUrl, sourceUrl,
    snapshot, tier, classifications, wasteResources, matchesWaste, filterResources, deliverables, graph, neighborhood
  });
})(window);
