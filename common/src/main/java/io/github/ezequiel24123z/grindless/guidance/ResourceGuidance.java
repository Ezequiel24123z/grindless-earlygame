package io.github.ezequiel24123z.grindless.guidance;

import io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.ResourceMetadata;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Resource;

import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Comparator;

/**
 * Read-only resource declarations prepared outside the game. Neither a recipe catalogue nor
 * certification of the current world, operational readiness or an accepted progression tier.
 */
public final class ResourceGuidance {
    public static final int SCHEMA_VERSION = 1;
    public static final String ASSET = "assets/grindless/guidance/resources.json";

    private ResourceGuidance() {
    }

    public static String uncertaintyKey(Resource resource) {
        return "tooltip.grindless.resource.uncertainty." + resource.descriptionKey()
                + (resource.rating() == 0 ? "" : ".f" + resource.rating());
    }

    /** Source references retain authority names, never machine-local source-root paths. */
    public enum ReferenceKind { OUTPUT, CONSUMED_INPUT, CATALYST }
    public enum Certainty {
        EXACT_RESOURCE, INCOMPLETE_TAG, COMPLETE_SINGLETON_TAG, CONDITIONAL_TAG_MEMBER, COMPLETE_TAG_MEMBER
    }

    /** A route reference is not its quantities, a new output-role assignment or a tier acceptance. */
    public record RouteReference(String route, String source, String via, ReferenceKind kind, Certainty certainty,
                                 boolean unconditional, Integer unlockTier, Boolean optional, boolean discharged) {
        public RouteReference {
            if (route == null || route.isBlank() || source == null || source.isBlank()
                    || via == null || via.isBlank() || unlockTier != null && (unlockTier < 0 || unlockTier > 15)) {
                throw new IllegalArgumentException("Invalid guidance route reference");
            }
            Objects.requireNonNull(kind, "reference kind");
            Objects.requireNonNull(certainty, "reference certainty");
        }
    }

    public record Entry(ResourceMetadata metadata, List<RouteReference> producers,
                        List<RouteReference> consumers, Map<String, String> sources) {
        public Entry {
            Objects.requireNonNull(metadata, "resource declarations");
            Comparator<RouteReference> order = Comparator.comparing(RouteReference::route)
                    .thenComparing(RouteReference::via).thenComparing(reference -> reference.kind().name());
            producers = producers.stream().sorted(order).toList();
            consumers = consumers.stream().sorted(order).toList();
            TreeMap<String, String> sorted = new TreeMap<>(sources);
            sorted.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null || value.isBlank()) {
                    throw new IllegalArgumentException("A guidance source needs an identity and reference");
                }
            });
            sources = Collections.unmodifiableMap(sorted);
        }
    }

    /** The fingerprint covers the complete ordered declaration payload, including explicit nulls. */
    public record Document(int schemaVersion, String contentSha256, Map<Resource, Entry> resources) {
        public Document {
            if (schemaVersion != SCHEMA_VERSION || contentSha256 == null
                    || !contentSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Unsupported resource guidance schema or fingerprint");
            }
            resources = Collections.unmodifiableMap(new TreeMap<>(resources));
        }
    }
}
