package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.guidance.ResourceGuidance;
import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceRuntime;
import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixNbt;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixSpec;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityAnnotations.SideStreamClass;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Kind;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Resource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** One native, localized presenter over cached declarations for every player-facing surface. */
public final class ResourceGuidanceClient {
    private static final String PREFIX = "tooltip.grindless.resource.";
    private enum Presentation { HOVER, PLAYER, TECHNICAL }

    private ResourceGuidanceClient() {
    }

    /** A matched declaration key is not a replacement for the physical stack/resource identity. */
    public record DeclarationMatch(Resource declaration, ResourceGuidance.Entry entry) {
    }

    /** Matrix NBT is read through its existing validator, never changed or silently downgraded. */
    public static Optional<Resource> item(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.getItem() instanceof ControlMatrixItem matrix) {
            return matrixItem(id, matrix.spec(stack));
        }
        return Optional.of(new Resource(Kind.ITEM, id));
    }

    /** Converts only an already validated spec; invalid explicit NBT never becomes an ordinary item. */
    public static Optional<Resource> matrixItem(String id, Optional<ControlMatrixSpec> matrix) {
        return matrix.map(spec -> new Resource(Kind.ITEM, id, spec.rating().ordinal(), "items"));
    }

    public static Optional<DeclarationMatch> declaration(ResourceGuidanceRuntime.Loaded loaded, Resource physical) {
        return declaration(loaded, physical, ResourceGuidanceClient::matrixArchitecture);
    }

    /**
     * Only the registry edge is supplied separately for checks. Default selection always uses the
     * existing NBT contract; no new ratings, item aliases or declarations are generated.
     */
    public static Optional<DeclarationMatch> declaration(ResourceGuidanceRuntime.Loaded loaded, Resource physical,
                                                         Function<String, Optional<MatrixArchitecture>> architectures) {
        if (!loaded.available() || physical == null) {
            return Optional.empty();
        }
        ResourceGuidance.Entry exact = loaded.document().resources().get(physical);
        if (exact != null) {
            return Optional.of(new DeclarationMatch(physical, exact));
        }
        if (physical.kind() != Kind.ITEM || !physical.registry().equals("items") || physical.rating() <= 0) {
            return Optional.empty();
        }
        Optional<ControlMatrixSpec> defaultSpec = architectures.apply(physical.id())
                .flatMap(architecture -> ControlMatrixNbt.load(architecture, null));
        if (defaultSpec.isEmpty() || defaultSpec.get().rating().ordinal() != physical.rating()) {
            return Optional.empty();
        }
        Resource ordinary = new Resource(Kind.ITEM, physical.id(), 0, physical.registry());
        return Optional.ofNullable(loaded.document().resources().get(ordinary))
                .map(entry -> new DeclarationMatch(ordinary, entry));
    }

    private static Optional<MatrixArchitecture> matrixArchitecture(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.get(location) instanceof ControlMatrixItem matrix
                ? Optional.of(matrix.architecture()) : Optional.empty();
    }

    /** Quiet ordinary hover: only annotated resources appear; explicit contexts use required=true. */
    public static void append(List<Component> tooltip, Resource resource, boolean required, boolean expanded) {
        List<Component> prepared = lines(ResourceGuidanceRuntime.current(), resource, required, expanded);
        if (!tooltip.containsAll(prepared)) {
            tooltip.addAll(prepared);
        }
    }

    public static List<Component> lines(Resource resource, boolean required, boolean expanded) {
        return lines(ResourceGuidanceRuntime.current(), resource, required, expanded);
    }

    /** Reading surfaces do not have a Shift interaction or need source-code provenance. */
    public static List<Component> playerContext(Resource resource) {
        return playerContext(ResourceGuidanceRuntime.current(), resource);
    }

    public static List<Component> playerContext(ResourceGuidanceRuntime.Loaded loaded, Resource resource) {
        return present(loaded, resource, true, Presentation.PLAYER);
    }

    /** Explicit state makes null/unknown handling independently testable without a client/world. */
    public static List<Component> lines(ResourceGuidanceRuntime.Loaded loaded, Resource resource,
                                         boolean required, boolean expanded) {
        return present(loaded, resource, required, expanded ? Presentation.TECHNICAL : Presentation.HOVER);
    }

    private static List<Component> present(ResourceGuidanceRuntime.Loaded loaded, Resource resource,
                                            boolean required, Presentation presentation) {
        boolean player = presentation == Presentation.PLAYER;
        if (!loaded.available()) {
            return List.of(Component.translatable(PREFIX + (player ? "player_unavailable" : "unavailable"))
                    .withStyle(ChatFormatting.RED));
        }
        Optional<DeclarationMatch> match = declaration(loaded, resource);
        if (match.isEmpty()) {
            return required ? List.of(Component.translatable(PREFIX + (player ? "player_unknown" : "unknown"))
                    .withStyle(ChatFormatting.GRAY)) : List.of();
        }
        ResourceGuidance.Entry entry = match.get().entry();
        var metadata = entry.metadata();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(PREFIX + (player ? "player_scope" : "declaration")).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable(PREFIX + "supply", label("class", metadata.resourceClass())));
        if (metadata.sideStreamClass() != null) {
            lines.add(Component.translatable(PREFIX + "side_stream", label("side", metadata.sideStreamClass())));
        }
        lines.add(Component.translatable(PREFIX + (player ? "player_tiers" : "tiers"), tier(metadata.firstUseTier()),
                tier(metadata.firstRenewableTier())));
        lines.add(Component.translatable(PREFIX + (player ? "player_source" : "route"),
                player ? recipeDescription(metadata.canonicalRoute(), true) : known(metadata.canonicalRoute())));
        var handling = metadata.handling();
        if (handling != null) {
            lines.add(Component.translatable(PREFIX + "handling", label("action", handling.action()),
                    Component.translatable(PREFIX + (handling.discardAllowed() ? "discard_allowed" : "discard_not_allowed"))));
            if (handling.hazardous()) {
                lines.add(Component.translatable(PREFIX + "hazardous").withStyle(ChatFormatting.RED));
            }
        }
        if (metadata.uncertainty() != null) {
            Resource declared = match.get().declaration();
            String key = player ? playerUncertaintyKey(declared) : ResourceGuidance.uncertaintyKey(declared);
            lines.add(Language.getInstance().has(key) ? Component.translatable(PREFIX + "uncertainty", Component.translatable(key))
                    : Component.translatable(PREFIX + (player ? "player_uncertainty_unavailable" : "uncertainty_unavailable"))
                            .withStyle(ChatFormatting.RED));
        }
        if (presentation == Presentation.HOVER) {
            if (!entry.consumers().isEmpty()) {
                lines.add(reference(consumer(entry), false, false));
            }
            lines.add(Component.translatable(PREFIX + "more").withStyle(ChatFormatting.DARK_GRAY));
            return List.copyOf(lines);
        }
        if (handling == null) {
            lines.add(Component.translatable(PREFIX + "handling_unknown"));
        } else {
            if (handling.containment() != null || handling.hazardous()) {
                lines.add(Component.translatable(PREFIX + "containment",
                        known(handling.containment() == null ? null : handling.containment().key())));
            }
            if (handling.treatmentRoute() != null || handling.hazardous()) {
                lines.add(Component.translatable(PREFIX + "treatment", player
                        ? recipeDescription(handling.treatmentRoute(), true) : known(handling.treatmentRoute())));
            }
        }
        var recycling = metadata.recycling();
        String recyclingRoute = recycling == null ? null : recycling.route();
        lines.add(Component.translatable(PREFIX + "recycling", player
                        ? recipeDescription(recyclingRoute, true) : known(recyclingRoute),
                tier(recycling == null ? null : recycling.plannedTier())));
        if (metadata.originatingCatalyst() != null) {
            lines.add(Component.translatable(PREFIX + "originating_catalyst", metadata.originatingCatalyst().key()));
        }
        if (player) {
            if (!entry.consumers().isEmpty()) {
                lines.add(playerUse(consumer(entry)));
            } else {
                lines.add(Component.translatable(PREFIX + "player_use_unknown"));
            }
            if (metadata.futureConsumers().isEmpty()) {
                if (metadata.sideStreamClass() == SideStreamClass.POTENTIAL_FUTURE_USE
                        || metadata.sideStreamClass() == SideStreamClass.BYPRODUCT_FUTURE) {
                    lines.add(Component.translatable(PREFIX + "player_future_unknown"));
                }
            } else {
                metadata.futureConsumers().forEach(use -> lines.add(Component.translatable(PREFIX + "player_future",
                        use.system(), tier(use.tier()))));
            }
            return List.copyOf(lines);
        }
        if (metadata.futureConsumers().isEmpty()) {
            if (metadata.sideStreamClass() != null) {
                lines.add(Component.translatable(PREFIX + "future_unknown"));
            }
        } else {
            metadata.futureConsumers().stream().limit(3).forEach(consumer -> lines.add(Component.translatable(PREFIX + "future",
                    consumer.system(), tier(consumer.tier()), consumer.planReference())));
            more(lines, metadata.futureConsumers().size() - 3);
        }
        if (entry.sources().isEmpty()) {
            lines.add(Component.translatable(PREFIX + "source_unknown"));
        } else {
            var source = entry.sources().entrySet().iterator().next();
            lines.add(Component.translatable(PREFIX + "source", source.getKey(), source.getValue())
                    .withStyle(ChatFormatting.DARK_GRAY));
            more(lines, entry.sources().size() - 1);
        }
        if (!entry.producers().isEmpty()) {
            var producer = entry.producers().stream().filter(reference -> reference.route().equals(metadata.canonicalRoute()))
                    .findFirst().orElse(entry.producers().get(0));
            lines.add(reference(producer, true, true));
            more(lines, entry.producers().size() - 1);
        }
        if (!entry.consumers().isEmpty()) {
            lines.add(reference(consumer(entry), false, true));
            more(lines, entry.consumers().size() - 1);
        }
        return List.copyOf(lines);
    }

    public static String playerUncertaintyKey(Resource resource) {
        return ResourceGuidance.uncertaintyKey(resource).replace(PREFIX + "uncertainty.", PREFIX + "player_uncertainty.");
    }

    private static ResourceGuidance.RouteReference consumer(ResourceGuidance.Entry entry) {
        return entry.consumers().stream().filter(reference -> reference.unconditional()
                && Integer.valueOf(0).equals(reference.unlockTier())
                && !Boolean.TRUE.equals(reference.optional())).findFirst().orElse(entry.consumers().get(0));
    }

    private static void more(List<Component> lines, int count) {
        if (count > 0) {
            lines.add(Component.translatable(PREFIX + "more_references", count).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Component reference(ResourceGuidance.RouteReference reference, boolean producer, boolean expanded) {
        Component description = expanded ? recipeDescription(reference.route(), false) : Component.literal(reference.route());
        Component line = Component.translatable(PREFIX + (producer ? "producer" : "consumer"),
                tier(reference.unlockTier()), description);
        if (!reference.unconditional()) {
            line = Component.translatable(PREFIX + "candidate_reference", line);
        }
        if (reference.unlockTier() == null) {
            line = Component.translatable(PREFIX + "unassessed_reference", line);
        }
        if (reference.kind() == ResourceGuidance.ReferenceKind.CATALYST) {
            line = Component.translatable(PREFIX + "catalyst_reference", line);
        }
        if (reference.discharged()) {
            line = Component.translatable(PREFIX + "discharged_reference", line);
        }
        if (Boolean.TRUE.equals(reference.optional())) {
            line = Component.translatable(PREFIX + "optional_reference", line);
        }
        return expanded ? Component.translatable(PREFIX + "reference_source", line, reference.via(), reference.source()) : line;
    }

    private static Component playerUse(ResourceGuidance.RouteReference use) {
        Component line = Component.translatable(PREFIX + "player_use", tier(use.unlockTier()), recipeDescription(use.route(), true));
        if (!use.unconditional()) {
            line = Component.translatable(PREFIX + "player_candidate_use", line);
        }
        if (use.unlockTier() == null) {
            line = Component.translatable(PREFIX + "player_unassessed_use", line);
        }
        if (use.kind() == ResourceGuidance.ReferenceKind.CATALYST) {
            line = Component.translatable(PREFIX + "catalyst_reference", line);
        }
        if (Boolean.TRUE.equals(use.optional())) {
            line = Component.translatable(PREFIX + "optional_reference", line);
        }
        return line;
    }

    private static Component recipeDescription(String route, boolean player) {
        if (route == null) {
            return known(null);
        }
        Optional<AtlasLogic.Entry> nativeRecipe = ProcessLookup.recipes().stream()
                .filter(recipe -> recipe.id().equals(route)).findFirst().map(AtlasLogic::entry);
        if (nativeRecipe.isEmpty()) {
            nativeRecipe = AtlasLogic.allEntries(List.of()).stream().filter(recipe -> recipe.id().equals(route)).findFirst();
        }
        return nativeRecipe.<Component>map(recipe -> {
            Object station = player ? Component.translatable(recipe.station().equals("CRAFTING_TABLE")
                    ? "block.minecraft.crafting_table" : "block.grindless." + recipe.station().toLowerCase(Locale.ROOT))
                    : recipe.id();
            Component summary = Component.translatable(PREFIX + "native_recipe", station,
                    AtlasLogic.describe(recipe.inputs()), AtlasLogic.describe(recipe.outputs()));
            return recipe.catalysts().isEmpty() ? summary : Component.translatable(PREFIX + "native_recipe_catalysts",
                    summary, AtlasLogic.describe(recipe.catalysts()));
        }).orElseGet(() -> Component.literal(route));
    }

    public static void renderTooltip(GuiGraphics graphics, Font font, List<Component> text, int mouseX, int mouseY) {
        int width = Math.max(40, Math.min(300, graphics.guiWidth() - 24));
        graphics.renderTooltip(font, text.stream().flatMap(line -> font.split(line, width).stream()).toList(),
                mouseX, mouseY);
    }

    public static Component name(Resource resource) {
        if (resource.kind() == Kind.ITEM && resource.registry().equals("items")) {
            ResourceLocation id = ResourceLocation.tryParse(resource.id());
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                return BuiltInRegistries.ITEM.get(id).getDescription();
            }
        }
        if (resource.kind() == Kind.FLUID) {
            return FluidClientPlatform.name(resource.id());
        }
        return Component.literal(resource.key());
    }

    private static Component label(String domain, Enum<?> value) {
        return value == null ? Component.translatable(PREFIX + "not_declared")
                : Component.translatable(PREFIX + domain + "." + value.name().toLowerCase(Locale.ROOT));
    }

    private static Component tier(Integer tier) {
        return tier == null ? Component.translatable(PREFIX + "not_declared")
                : Component.translatable(PREFIX + "tier", tier);
    }

    private static Component known(String value) {
        return value == null ? Component.translatable(PREFIX + "not_declared") : Component.literal(value);
    }
}
