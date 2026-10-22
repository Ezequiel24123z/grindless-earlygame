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
import java.util.TreeMap;

/**
 * The live process graph: generated from the current tag scan and indexed for lookup (ADR-0043).
 *
 * <p>Replaced wholesale on every material rebuild. A machine caches the recipe it is running and
 * drops that cache when inputs or the published generation change; it never walks the whole list.
 */
public final class ProcessLookup {

    private static volatile Graph GRAPH = Graph.EMPTY;

    private ProcessLookup() {
    }

    /** Rebuilds pure descriptor indexes, without native registry access, next to {@link MaterialRegistry#rebuild}. */
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

    /** Identity of the complete indexed publication; stable until the next rebuild. */
    public static Generation generation() {
        return GRAPH.generation;
    }

    public static List<ProcessRecipe> recipes() {
        return GRAPH.recipes;
    }

    /** Transient identity only; never persisted or constructed outside a complete graph. */
    public static final class Generation {
        private Generation() {
        }
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
        List<String> keys = new ArrayList<>();
        if (inputs.length > 0 && !inputs[0].isEmpty()) {
            inputs[0].getTags().forEach(tag -> keys.add(tag.location().toString()));
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(inputs[0].getItem());
            keys.add("item:" + itemId);
        }
        if (fluid != null && !fluid.isEmpty()) {
            keys.add("fluid:" + fluid.id());
        }
        Optional<ProcessRecipe> dry = Optional.empty();
        for (ProcessRecipe recipe : graph.candidates(family, keys)) {
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

    /** Whether complete item inputs are waiting for their primary fluid, including fluid-only work. */
    public static boolean needsPrimaryFluid(MachineFamily family, ItemStack[] inputs, FluidState fluid) {
        for (ProcessRecipe recipe : GRAPH.byFamily.getOrDefault(family, List.of())) {
            if (!matches(recipe, inputs) || recipe.fluidInputs().isEmpty()) {
                continue;
            }
            IngredientSpec primary = recipe.fluidInputs().get(0);
            if (fluid.isEmpty() || fluid.is(primary.id())
                    && fluid.millibuckets() < primaryFluidTarget(family, inputs, primary.id())) {
                return true;
            }
        }
        return false;
    }

    /** Batch-sized intake; preloading without complete items uses the family's largest matching batch. */
    public static int primaryFluidTarget(MachineFamily family, ItemStack[] inputs, String id) {
        int matched = Integer.MAX_VALUE;
        int preload = 0;
        for (ProcessRecipe recipe : GRAPH.byFamily.getOrDefault(family, List.of())) {
            if (recipe.fluidInputs().isEmpty() || !recipe.fluidInputs().get(0).id().equals(id)) {
                continue;
            }
            int amount = recipe.fluidInputs().get(0).count();
            preload = Math.max(preload, amount);
            if (matches(recipe, inputs)) {
                matched = Math.min(matched, amount);
            }
        }
        return matched == Integer.MAX_VALUE ? preload : matched;
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
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(matrix);
            if (itemId == null || BuiltInRegistries.ITEM.get(itemId) != matrix) {
                return false;
            }
            return matrix.spec(stack).map(value -> spec.acceptsMatrix(itemId.toString(), value))
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

    /** Viewer alternatives from actual registered Matrix items, using the same input matcher. */
    public static List<ItemStack> matrixInputs(IngredientSpec input) {
        if (input.matrixRating() == 0) {
            throw new IllegalArgumentException("Matrix alternatives require a rated input");
        }
        List<ItemStack> alternatives = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ControlMatrixItem matrix) {
                for (FluxTier rating : FluxTier.values()) {
                    if (matrix.architecture().supports(rating)) {
                        ItemStack candidate = matrix.stackFor(rating, input.count());
                        if (matches(input, candidate)) {
                            alternatives.add(candidate);
                        }
                    }
                }
            }
        }
        return List.copyOf(alternatives);
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
        if (recipe.namesPressure()) {
            builder.pressure(recipe.pressureMPa(), recipe.pressureHalfWidthMPa());
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

    private record IndexedRecipe(int order, ProcessRecipe recipe) {
    }

    private record Graph(
            Generation generation,
            List<ProcessRecipe> recipes,
            Map<MachineFamily, List<ProcessRecipe>> byFamily,
            Map<String, List<IndexedRecipe>> byTag,
            Map<MachineFamily, List<IndexedRecipe>> minimumPrimary) {

        static final Graph EMPTY = new Graph(new Generation(), List.of(), Map.of(), Map.of(), Map.of());

        static Graph index(List<ProcessRecipe> recipes) {
            Map<MachineFamily, List<ProcessRecipe>> byFamily = new EnumMap<>(MachineFamily.class);
            Map<String, List<IndexedRecipe>> byTag = new HashMap<>();
            Map<MachineFamily, List<IndexedRecipe>> minimumPrimary = new EnumMap<>(MachineFamily.class);
            for (int order = 0; order < recipes.size(); order++) {
                ProcessRecipe recipe = recipes.get(order);
                IndexedRecipe indexed = new IndexedRecipe(order, recipe);
                byFamily.computeIfAbsent(recipe.family(), ignored -> new ArrayList<>()).add(recipe);
                if (!recipe.itemInputs().isEmpty()) {
                    index(byTag, minimumPrimary, indexed, recipe.itemInputs().get(0));
                } else if (!recipe.catalysts().isEmpty()) {
                    index(byTag, minimumPrimary, indexed, recipe.catalysts().get(0));
                }
                if (!recipe.fluidInputs().isEmpty()) {
                    IngredientSpec fluid = recipe.fluidInputs().get(0);
                    byTag.computeIfAbsent(key(recipe.family(), "fluid:" + fluid.id()),
                            ignored -> new ArrayList<>()).add(indexed);
                }
            }
            Map<MachineFamily, List<ProcessRecipe>> frozenFamily = new EnumMap<>(MachineFamily.class);
            byFamily.forEach((family, list) -> frozenFamily.put(family, List.copyOf(list)));
            Map<String, List<IndexedRecipe>> frozenTags = new HashMap<>();
            byTag.forEach((key, list) -> frozenTags.put(key, List.copyOf(list)));
            Map<MachineFamily, List<IndexedRecipe>> frozenMinimum = new EnumMap<>(MachineFamily.class);
            minimumPrimary.forEach((family, list) -> frozenMinimum.put(family, List.copyOf(list)));
            return new Graph(new Generation(), List.copyOf(recipes), Map.copyOf(frozenFamily),
                    Map.copyOf(frozenTags), Map.copyOf(frozenMinimum));
        }

        /** Graph order owns priority; a row reached through several keys is still one candidate. */
        private List<ProcessRecipe> candidates(MachineFamily family, List<String> keys) {
            Map<Integer, ProcessRecipe> keyed = new TreeMap<>();
            for (String id : keys) {
                for (IndexedRecipe indexed : byTag.getOrDefault(key(family, id), List.of())) {
                    keyed.put(indexed.order(), indexed.recipe());
                }
            }
            if (keyed.isEmpty()) {
                return byFamily.getOrDefault(family, List.of());
            }
            for (IndexedRecipe indexed : minimumPrimary.getOrDefault(family, List.of())) {
                keyed.put(indexed.order(), indexed.recipe());
            }
            return List.copyOf(keyed.values());
        }

        private static void index(Map<String, List<IndexedRecipe>> byTag,
                                  Map<MachineFamily, List<IndexedRecipe>> minimumPrimary, IndexedRecipe indexed,
                                  IngredientSpec primary) {
            ProcessRecipe recipe = indexed.recipe();
            if (primary.isMinimumControl()) {
                minimumPrimary.computeIfAbsent(recipe.family(), ignored -> new ArrayList<>()).add(indexed);
                return;
            }
            String id = IngredientSpec.ITEM.equals(primary.kind())
                    ? "item:" + primary.id()
                    : primary.id();
            byTag.computeIfAbsent(key(recipe.family(), id), ignored -> new ArrayList<>()).add(indexed);
        }
    }
}
