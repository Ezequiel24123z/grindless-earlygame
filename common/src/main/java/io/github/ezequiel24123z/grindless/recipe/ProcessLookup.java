package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.material.Material;
import io.github.ezequiel24123z.grindless.material.MaterialForm;
import io.github.ezequiel24123z.grindless.material.MaterialRegistry;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.material.MaterialSnapshot;
import io.github.ezequiel24123z.grindless.process.Agitation;
import io.github.ezequiel24123z.grindless.process.Atmosphere;
import io.github.ezequiel24123z.grindless.process.ProcessConditions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The live process graph: generated from the current tag scan and indexed for lookup (ADR-0043).
 *
 * <p>Replaced wholesale on every material rebuild. A machine caches the recipe it is running and
 * drops that cache when inputs change; it never walks the whole list.
 */
public final class ProcessLookup {

    private static volatile Graph GRAPH = Graph.EMPTY;

    private ProcessLookup() {
    }

    /** Rebuilds from the current material snapshot. Called next to {@link MaterialRegistry#rebuild}. */
    public static void rebuild(MaterialSnapshot snapshot) {
        List<ProcessGraph.MaterialView> views = new ArrayList<>();
        for (Material material : snapshot.materials()) {
            views.add(new ProcessGraph.MaterialView(
                    material.name(),
                    material.has(MaterialForm.RAW),
                    material.has(MaterialForm.ORE),
                    material.has(MaterialForm.CRUSHED),
                    material.has(MaterialForm.OXIDE),
                    material.has(MaterialForm.INGOT),
                    material.has(MaterialForm.PLATE),
                    material.has(MaterialForm.ROD),
                    material.has(MaterialForm.GEAR),
                    material.has(MaterialForm.WASHED_CRUSHED)));
        }
        List<ProcessRecipe> recipes = ProcessGraph.generate(views);
        GRAPH = Graph.index(recipes);
        Grindless.LOG.info("[{}] {} process recipes (ore line / roast / press / mill / contact / wash / gas / assembler)",
                Grindless.MOD_NAME, recipes.size());
    }

    public static List<ProcessRecipe> recipes() {
        return GRAPH.recipes;
    }

    /**
     * The recipe this family should run for {@code inputs}, or empty when none matches.
     *
     * <p>Indexed by the primary input's tags so a full scan is the miss path, not the hit path.
     */
    public static Optional<ProcessRecipe> find(MachineFamily family, ItemStack[] inputs) {
        return find(family, inputs, FluidState.EMPTY);
    }

    /**
     * The recipe this family should run. A wet mill wins over dry B1 when the fluid buffer
     * already holds enough water (ADR-0062).
     */
    public static Optional<ProcessRecipe> find(MachineFamily family, ItemStack[] inputs,
                                               FluidState fluid) {
        return find(family, inputs, fluid, List.of());
    }

    /**
     * The recipe this family should run. Extra fluids (absorption water) come from neighbouring
     * tanks and are never stored in the machine buffer (ADR-0075).
     */
    public static Optional<ProcessRecipe> find(MachineFamily family, ItemStack[] inputs,
                                               FluidState fluid, List<FluidState> neighbours) {
        Graph graph = GRAPH;
        List<ProcessRecipe> keyed = new ArrayList<>();
        if (inputs.length > 0 && !inputs[0].isEmpty()) {
            inputs[0].getTags().forEach(tag -> {
                List<ProcessRecipe> found = graph.byTag.get(key(family, tag.location().toString()));
                if (found != null) {
                    keyed.addAll(found);
                }
            });
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(inputs[0].getItem());
            List<ProcessRecipe> byItem = graph.byTag.get(key(family, "item:" + itemId));
            if (byItem != null) {
                keyed.addAll(byItem);
            }
        }
        if (fluid != null && !fluid.isEmpty()) {
            List<ProcessRecipe> byFluid = graph.byTag.get(key(family, "fluid:" + fluid.id()));
            if (byFluid != null) {
                keyed.addAll(byFluid);
            }
        }
        if (keyed.isEmpty()) {
            keyed.addAll(graph.byFamily.getOrDefault(family, List.of()));
        }
        Optional<ProcessRecipe> dry = Optional.empty();
        for (ProcessRecipe recipe : keyed) {
            if (recipe.family() != family || !matches(recipe, inputs)
                    || !matchesFluid(recipe, fluid, neighbours)) {
                continue;
            }
            if (!recipe.fluidInputs().isEmpty()) {
                return Optional.of(recipe);
            }
            dry = Optional.of(recipe);
        }
        return dry;
    }

    public static boolean matchesFluid(ProcessRecipe recipe, FluidState fluid) {
        return matchesFluid(recipe, fluid, List.of());
    }

    public static boolean matchesFluid(ProcessRecipe recipe, FluidState fluid,
                                       List<FluidState> neighbours) {
        List<IngredientSpec> needed = recipe.fluidInputs();
        if (needed.isEmpty()) {
            return true;
        }
        IngredientSpec primary = needed.get(0);
        if (fluid == null || fluid.isEmpty()
                || !fluid.is(primary.id()) || fluid.millibuckets() < primary.count()) {
            return false;
        }
        for (int i = 1; i < needed.size(); i++) {
            IngredientSpec spec = needed.get(i);
            if (!FluidLogic.hasAtLeast(neighbours, spec.id(), spec.count())) {
                return false;
            }
        }
        return true;
    }

    /** Whether this family has a recipe whose primary fluid is {@code id}. */
    public static boolean isPrimaryFluid(MachineFamily family, String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        for (ProcessRecipe recipe : GRAPH.byFamily.getOrDefault(family, List.of())) {
            List<IngredientSpec> fluids = recipe.fluidInputs();
            if (!fluids.isEmpty() && id.equals(fluids.get(0).id())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code stack} is a legal insert for this family's input {@code slot}.
     *
     * <p>Count is ignored. A hopper moves one item at a time, so a wash that wants eight
     * crushed must accept the first one. The recipe still waits until the count is met.
     */
    public static boolean accepts(MachineFamily family, int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Graph graph = GRAPH;
        List<ProcessRecipe> recipes = graph.byFamily.getOrDefault(family, List.of());
        for (ProcessRecipe recipe : recipes) {
            List<IngredientSpec> inputs = recipe.itemInputs();
            if (slot < inputs.size() && sameItem(inputs.get(slot), stack)) {
                return true;
            }
            int catalyst = slot - inputs.size();
            if (catalyst >= 0 && catalyst < recipe.catalysts().size()
                    && sameItem(recipe.catalysts().get(catalyst), stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matches(ProcessRecipe recipe, ItemStack[] inputs) {
        List<IngredientSpec> needed = recipe.itemInputs();
        List<IngredientSpec> catalysts = recipe.catalysts();
        if (inputs.length < needed.size() + catalysts.size()) {
            return false;
        }
        for (int i = 0; i < needed.size(); i++) {
            if (!matches(needed.get(i), inputs[i])) {
                return false;
            }
        }
        for (int i = 0; i < catalysts.size(); i++) {
            if (!matches(catalysts.get(i), inputs[needed.size() + i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean matches(IngredientSpec spec, ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() >= spec.count() && sameItem(spec, stack);
    }

    /**
     * Item identity, ignoring count.
     *
     * <p>A hopper moves one item per tick. Requiring the whole batch here would refuse
     * the first insert of steel and of zone refining, so the line could never fill.
     * The count is enforced when the recipe actually starts.
     */
    private static boolean sameItem(IngredientSpec spec, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (spec.matrixRating() > 0) {
            if (!(stack.getItem() instanceof ControlMatrixItem matrix)) {
                return false;
            }
            return matrix.spec(stack).map(value -> value.rating().ordinal() == spec.matrixRating())
                    .orElse(false);
        }
        if (IngredientSpec.ITEM.equals(spec.kind())) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(spec.id());
        }
        if (IngredientSpec.TAG.equals(spec.kind())) {
            return stack.is(TagKey.create(Registries.ITEM, new ResourceLocation(spec.id())));
        }
        return false;
    }

    /** Resolves an item output to a stack, honouring the pack's preferred provider. */
    public static Optional<ItemStack> resolve(OutputSpec output) {
        if (output.vented() || output.isFluid()) {
            return Optional.empty();
        }
        if (IngredientSpec.ITEM.equals(output.kind())) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(output.id()));
            if (item == null) {
                return Optional.empty();
            }
            if (output.matrixRating() > 0) {
                if (!(item instanceof ControlMatrixItem matrix)) {
                    return Optional.empty();
                }
                return Optional.of(matrix.stackFor(FluxTier.values()[output.matrixRating()], output.count()));
            }
            return Optional.of(new ItemStack(item, output.count()));
        }
        ResourceLocation tag = new ResourceLocation(output.id());
        int slash = tag.getPath().lastIndexOf('/');
        if (slash < 0) {
            return Optional.empty();
        }
        String formPath = tag.getPath().substring(0, slash);
        String material = tag.getPath().substring(slash + 1);
        MaterialForm form = MaterialForm.byTagPath(formPath);
        if (form == null) {
            return Optional.empty();
        }
        return MaterialRegistry.output(material, form)
                .map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id), output.count()));
    }

    public static ProcessConditions conditionsOf(ProcessRecipe recipe) {
        ProcessConditions.Builder builder = ProcessConditions.builder();
        if (recipe.namesTemperature()) {
            builder.temperature(recipe.temperatureC());
        }
        if (recipe.namesAtmosphere()) {
            Atmosphere atmosphere = Atmosphere.valueOf(recipe.atmosphere());
            builder.atmosphere(atmosphere);
        }
        if (recipe.namesAgitation()) {
            builder.agitation(Agitation.valueOf(recipe.agitation()));
        }
        return builder.build();
    }

    private static String key(MachineFamily family, String id) {
        return family.name() + "|" + id;
    }

    private record Graph(
            List<ProcessRecipe> recipes,
            Map<MachineFamily, List<ProcessRecipe>> byFamily,
            Map<String, List<ProcessRecipe>> byTag) {

        static final Graph EMPTY = new Graph(List.of(), Map.of(), Map.of());

        static Graph index(List<ProcessRecipe> recipes) {
            Map<MachineFamily, List<ProcessRecipe>> byFamily = new EnumMap<>(MachineFamily.class);
            Map<String, List<ProcessRecipe>> byTag = new HashMap<>();
            for (ProcessRecipe recipe : recipes) {
                byFamily.computeIfAbsent(recipe.family(), ignored -> new ArrayList<>()).add(recipe);
                if (!recipe.itemInputs().isEmpty()) {
                    index(byTag, recipe, recipe.itemInputs().get(0));
                } else if (!recipe.catalysts().isEmpty()) {
                    index(byTag, recipe, recipe.catalysts().get(0));
                }
                if (!recipe.fluidInputs().isEmpty()) {
                    IngredientSpec fluid = recipe.fluidInputs().get(0);
                    byTag.computeIfAbsent(key(recipe.family(), "fluid:" + fluid.id()),
                            ignored -> new ArrayList<>()).add(recipe);
                }
            }
            Map<MachineFamily, List<ProcessRecipe>> frozenFamily = new EnumMap<>(MachineFamily.class);
            byFamily.forEach((family, list) -> frozenFamily.put(family, List.copyOf(list)));
            Map<String, List<ProcessRecipe>> frozenTags = new HashMap<>();
            byTag.forEach((key, list) -> frozenTags.put(key, List.copyOf(list)));
            return new Graph(List.copyOf(recipes), Map.copyOf(frozenFamily), Map.copyOf(frozenTags));
        }

        private static void index(Map<String, List<ProcessRecipe>> byTag, ProcessRecipe recipe,
                                  IngredientSpec primary) {
            String id = IngredientSpec.ITEM.equals(primary.kind())
                    ? "item:" + primary.id()
                    : primary.id();
            byTag.computeIfAbsent(key(recipe.family(), id), ignored -> new ArrayList<>()).add(recipe);
        }
    }
}
