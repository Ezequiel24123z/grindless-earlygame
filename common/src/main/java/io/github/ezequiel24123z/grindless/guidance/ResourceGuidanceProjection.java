package io.github.ezequiel24123z.grindless.guidance;

import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.sustainability.DashboardProjection;
import io.github.ezequiel24123z.grindless.sustainability.AuditReports;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityAudit;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.Locale;

import static io.github.ezequiel24123z.grindless.guidance.ResourceGuidance.*;
import static io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.*;

/**
 * Build/tool-only projection of the same Result used by the dashboard. No audit verdict,
 * bootstrap bill, recipe quantities or inferred renewability enters the packaged declarations.
 */
public final class ResourceGuidanceProjection {
    private ResourceGuidanceProjection() {
    }

    public static Document project(SustainabilityAudit.Result result) {
        Map<Resource, Entry> resources = new TreeMap<>();
        result.annotations().resources().forEach((resource, metadata) -> {
            if (!result.authorities().resources().contains(resource)) {
                throw new IllegalArgumentException("An annotated guidance resource is absent from the authorities: "
                        + resource.key());
            }
            Map<String, String> sources = new TreeMap<>();
            result.authorities().terminals().values().stream().filter(terminal -> terminal.resource().equals(resource))
                    .forEach(terminal -> sources.put("terminal:" + terminal.id(), terminal.source()));
            reference(result, sources, "canonical", metadata.canonicalRoute());
            if (metadata.handling() != null) {
                reference(result, sources, "treatment", metadata.handling().treatmentRoute());
            }
            if (metadata.recycling() != null) {
                reference(result, sources, "recycling", metadata.recycling().route());
            }
            metadata.futureConsumers().forEach(consumer -> {
                var plan = result.annotations().plans().get(consumer.planReference());
                if (plan == null) {
                    throw new IllegalArgumentException("A declared future consumer has no source plan");
                }
                sources.put("plan:" + plan.reference(), plan.source());
            });
            resources.put(resource, new Entry(metadata, references(result, resource, true),
                    references(result, resource, false), sources));
        });
        Document document = ResourceGuidanceJson.prepare(resources);
        DashboardProjection.requirePublic(JsonParser.parseString(ResourceGuidanceJson.write(document)));
        return document;
    }

    private static List<RouteReference> references(SustainabilityAudit.Result result, Resource resource, boolean producers) {
        return AuditReports.relationships(result.authorities(), resource, producers).stream().map(reference -> {
            String route = (String) reference.get("route");
            var policy = result.annotations().routes().get(route);
            return new RouteReference(route, (String) reference.get("source"), (String) reference.get("via"),
                    ReferenceKind.valueOf(((String) reference.get("kind")).toUpperCase(Locale.ROOT)),
                    Certainty.valueOf(((String) reference.get("certainty")).toUpperCase(Locale.ROOT)),
                    (boolean) reference.get("unconditional"), policy == null ? null : policy.unlockTier(),
                    policy == null ? null : policy.optional(), (boolean) reference.get("discharged"));
        }).toList();
    }

    private static void reference(SustainabilityAudit.Result result, Map<String, String> sources,
                                  String kind, String routeId) {
        if (routeId == null) {
            return;
        }
        Route route = result.authorities().routes().get(routeId);
        if (route == null) {
            throw new IllegalArgumentException("A declared guidance route is absent: " + routeId);
        }
        sources.put(kind + ":" + routeId, route.source());
    }
}
