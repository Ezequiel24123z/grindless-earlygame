package io.github.ezequiel24123z.grindless.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ezequiel24123z.grindless.sustainability.AuditReports;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityAudit;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Deterministic machine-readable prices, graph, risks and costs from the live authorities. */
public final class EconomyProjection {
    private static final Gson JSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();
    public static final String BASELINE = "b8bd5e885aef598bf888d7e390b297a34cbb7d52";

    private EconomyProjection() {
    }

    public static Map<String, Object> document(SustainabilityAudit.Result audit) {
        Map<String, Object> document = new TreeMap<>();
        document.put("schema_version", 1);
        document.put("baseline", BASELINE);
        document.put("authority_snapshot_sha256", AuditReports.snapshotHash(audit.authorities()));
        document.put("mechanism", "direct_in_kind_barter_no_currency_wallet_or_dependency");
        document.put("claim_ledger", "Existing QuestProgress, read-only shop prerequisites; no shop claim ledger");
        document.put("shop_required_for_t0", false);
        document.put("starter_allocation", ProgressionCostModel.starterAllocation());
        List<Object> offers = new ArrayList<>();
        for (ShopCatalogue.Offer offer : ShopCatalogue.offers()) {
            Map<String, Object> row = new TreeMap<>();
            row.put("id", offer.id());
            row.put("unlock_claims", new TreeSet<>(offer.requires()));
            row.put("price", offer.payment());
            row.put("goods", offer.goods());
            row.put("role", offer.role());
            row.put("initial_acquisition_source_class", offer.paymentSource());
            row.put("initial_acquisition_provenance", offer.surplusSource());
            row.put("repeatable_exchange", true);
            row.put("same_payment_accepts_later_production", true);
            row.put("production_alternative", offer.productionAlternative());
            row.put("progression_risk", offer.progressionRisk());
            row.put("max_batches_per_request", 1);
            row.put("output_capacity_policy", "complete_native_main_inventory_settlement_or_no_change");
            row.put("paid_inputs_status", paymentStatus(audit, offer));
            row.put("canonical_production_replacement", false);
            row.put("renewable_scope", offer.paymentSource() == ShopCatalogue.PaymentSource.MANUAL_FINITE_COMPATIBILITY
                    ? "Initially hand-gathered finite compatibility recovery. Dirt can later come from mod-owned "
                    + "wet_aggregate; the exchange stays optional and does not replace canonical production"
                    : "Actual canonical classifications and conditional model membership are exported per payment; shop is optional");
            offers.add(row);
        }
        document.put("offers", offers);
        List<Object> graph = new ArrayList<>();
        for (EconomyGraph.Row route : EconomyGraph.rows()) {
            Map<String, Object> row = new TreeMap<>();
            row.put("id", route.id());
            row.put("inputs", route.inputs());
            row.put("outputs", route.outputs());
            row.put("source", route.source());
            row.put("family", route.family());
            row.put("duration_ticks", route.ticks());
            row.put("fu_per_tick", route.fuPerTick());
            row.put("boundary", route.boundary());
            row.put("prerequisites", new TreeSet<>(route.requires()));
            row.put("max_executions_per_player_world", route.boundary() == EconomyGraph.Boundary.FINITE_QUEST ? 1 : null);
            graph.add(row);
        }
        document.put("exploit_graph", graph);
        document.put("cycle_certificate", EconomyCycleProof.check());
        document.put("cycle_proof_scope", "Positive material-potential certificate over all projected T0 conversions, "
                + "including shop, crafting, ceramic/wood/container returns and finite quest budgets; "
                + "environmental sources are explicit boundaries. Not an energy, arbitrary datapack or whole-mod proof.");
        document.put("minimum_exit_boundary", ProgressionCostModel.exitMinimum());
        document.put("minimum_scope", "Exact final craft and core/calibration material lower bound; construction, "
                + "raw processing, energy generation and optional branches are not included in this lower bound.");
        document.put("canonical_conditional_witness", ProgressionCostModel.witness(audit));
        document.put("witness_scope", "Reuse of the existing finite canonical conditional-entry replay, not a new "
                + "optimal planner, a globally minimum bill, an observed starter-kit route or an elapsed playtime.");
        document.put("balance_ledger", balanceLedger());
        document.put("telemetry", Map.of("default", "off", "control", "/grindless telemetry start|snapshot|stop",
                "destination", "local_game_log_only", "identity", "none", "clock", "relative_game_ticks",
                "scope", "server_aggregate_not_per_player", "automatic_restart", false));
        document.put("unmeasured", List.of("prices_and_usefulness", "human_milestone_times", "overall_pacing",
                "client_ergonomics", "no_command_campaign", "longitudinal_save_compatibility"));
        return Collections.unmodifiableMap(document);
    }

    public static String json(SustainabilityAudit.Result audit) {
        return canonicalJson(JSON.toJsonTree(document(audit)));
    }

    /** Canonical object order across JVMs; arrays retain their authoritative execution/display order. */
    public static String canonicalJson(JsonElement document) {
        return JSON.toJson(ordered(document)) + "\n";
    }

    private static JsonElement ordered(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject result = new JsonObject();
            for (String key : new TreeSet<>(value.getAsJsonObject().keySet())) {
                result.add(key, ordered(value.getAsJsonObject().get(key)));
            }
            return result;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray();
            value.getAsJsonArray().forEach(element -> result.add(ordered(element)));
            return result;
        }
        return value.deepCopy();
    }

    private static Map<String, Object> paymentStatus(SustainabilityAudit.Result audit, ShopCatalogue.Offer offer) {
        Map<String, Object> status = new TreeMap<>();
        for (String payment : offer.payment().keySet()) {
            SustainabilityModel.Resource resource = new SustainabilityModel.Resource(SustainabilityModel.Kind.ITEM, payment);
            var declaration = audit.annotations().resources().get(resource);
            var tier = audit.tiers().get(0);
            Map<String, Object> item = new TreeMap<>();
            item.put("classification", declaration == null ? "UNDECLARED" : declaration.resourceClass().name());
            item.put("canonical_route", declaration == null ? null : declaration.canonicalRoute());
            item.put("proved_in_conditional_t0", tier != null && tier.renewable().contains(resource));
            item.put("initial_acquisition_scope", offer.paymentSource());
            item.put("resource_classification_overridden", false);
            status.put(payment, item);
        }
        return status;
    }

    private static List<Object> balanceLedger() {
        List<Object> ledger = new ArrayList<>();
        ledger.add(Map.of("surface", "shop", "old", "No implementation in baseline", "new", "Explicit paid recovery",
                "problem", "Ordinary shortages and pre-producer losses had no exchange route",
                "risk", "Usefulness and prices need human measurement"));
        ledger.add(Map.of("surface", "pre_producer_recovery", "old", "Surplus-only barter would require water and an established base",
                "new", "Manual dirt restart before industrial stock; only old starter claim required",
                "problem", "Loss of both water buckets or first construction stock could cut every normal payment route",
                "risk", "Finite manual emergency compatibility, never the normal renewable factory"));
        ledger.add(Map.of("surface", "starter_and_existing_rewards", "old", "Generous finite one-time canonical allocation",
                "new", "Preserved", "problem", "Review is not authorization to nerf or reset claims",
                "risk", "Actual starter pacing remains unmeasured"));
        ledger.add(Map.of("surface", "production_rates_and_batches", "old", "Existing live recipe/source/research definitions",
                "new", "No production change by this lane; exporter compares baseline source blobs",
                "problem", "No measured friction supports blanket inflation or arbitrary recipe changes",
                "risk", "Observe causal counts and relative milestones before any production rebalance"));
        ledger.add(Map.of("surface", "fuel_recovery", "old", "Native biomass/biocarbon route and finite starter fuel",
                "new", "One costly paid charcoal at a time",
                "problem", "A local fuel shortage should not force replacement of the whole production chain",
                "risk", "Native biocarbon stays more efficient; paid food/fuel is not free recurring income"));
        return List.copyOf(ledger);
    }
}
