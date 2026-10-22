package io.github.ezequiel24123z.grindless.forge.recipe;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixNbt;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestInventoryEvidence;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.OutputSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Existing-harness probe over real registrations and capability admission, never a registry bootstrap. */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class ControlMatrixInputChecks {
    private static final BlockPos POSITION = new BlockPos(8, 300, 8);
    private static boolean started;
    private int assertions;

    private ControlMatrixInputChecks() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("grindless_matrix_input_checks")
                .requires(source -> source.getEntity() == null && source.hasPermission(4))
                .executes(context -> run(context.getSource())));
    }

    private static int run(CommandSourceStack source) {
        require(source.getEntity() == null && source.hasPermission(4), "Console permission 4 required");
        require(source.getServer().isSameThread(), "Use the actual server thread");
        require("smoke-world".equals(source.getServer().getWorldData().getLevelName()),
                "Use only the harness's disposable smoke-world");
        require(source.getServer().getPlayerList().getPlayers().isEmpty(), "No connected players may be present");
        require(!started, "The Matrix probe cannot be retried in the same process");
        started = true;
        ControlMatrixInputChecks checks = new ControlMatrixInputChecks();
        try {
            checks.check(source.getLevel() == source.getServer().overworld(), "Use the actual Overworld");
            checks.registered();
            checks.primaryControl();
            checks.capability(source.getLevel());
            Grindless.LOG.info("GRINDLESS_MATRIX_INPUT_NATIVE_PASS assertions={} scope=actual_registered_items_only",
                    checks.assertions);
            return 1;
        } catch (RuntimeException failure) {
            Grindless.LOG.error("GRINDLESS_MATRIX_INPUT_NATIVE_FAIL assertions={}", checks.assertions, failure);
            throw failure;
        }
    }

    private void primaryControl() {
        ProcessRecipe press = recipe("assemble/press");
        check(press.itemInputs().get(0).isMinimumControl(), "The real Press reproduction has minimum control first");
        ProcessRecipe predecessor = recipe("assemble/relay_matrix_f2");
        check(!predecessor.itemInputs().get(0).isMinimumControl()
                        && predecessor.itemInputs().get(0).id().equals(FabricationLogic.RELAY_MATRIX),
                "A nonempty ordinary Relay item index competes with the minimum-primary role");
        ControlMatrixItem relay = (ControlMatrixItem) ModItems.RELAY_MATRIX.get();
        ItemStack[] inputs = new ItemStack[press.itemInputs().size()];
        ItemStack high = relay.stackFor(FluxTier.F3, press.itemInputs().get(0).count());
        inputs[0] = high;
        for (int slot = 1; slot < inputs.length; slot++) {
            IngredientSpec input = press.itemInputs().get(slot);
            inputs[slot] = IngredientSpec.TAG.equals(input.kind())
                    && input.id().equals("minecraft:stone_crafting_materials")
                    ? new ItemStack(Items.COBBLESTONE, input.count())
                    : ProcessLookup.resolve(new OutputSpec(input.kind(), input.id(), input.count(), false))
                            .orElseThrow();
            check(ProcessLookup.matches(input, inputs[slot]),
                    "Actual Press fixture input " + input.qualified() + " count=" + input.count());
        }
        check(!ProcessLookup.matches(predecessor.itemInputs().get(0), high),
                "The existing exact F2 predecessor still rejects F3");
        check(ProcessLookup.accepts(MachineFamily.ASSEMBLER, 0, high.copyWithCount(1)),
                "Minimum-primary hopper admission accepts the first real F3 controller");
        check(ProcessLookup.find(MachineFamily.ASSEMBLER, inputs).orElseThrow().id().equals(press.id()),
                "A populated ordinary item index cannot hide the real F3 minimum-primary recipe");
        inputs[0] = ItemStack.EMPTY;
        check(ProcessLookup.find(MachineFamily.ASSEMBLER, inputs).filter(row -> row.id().equals(press.id())).isEmpty(),
                "Missing primary control cannot match through the pure role index");
        inputs[0] = new ItemStack(Items.PAPER, high.getCount());
        inputs[0].setTag(high.getTag().copy());
        check(ProcessLookup.find(MachineFamily.ASSEMBLER, inputs).filter(row -> row.id().equals(press.id())).isEmpty(),
                "An ordinary item with copied F3 NBT cannot match the indexed minimum role");
        inputs[0] = new ItemStack(relay, high.getCount());
        inputs[0].getOrCreateTag().putString(ControlMatrixNbt.RATING, "3");
        check(ProcessLookup.find(MachineFamily.ASSEMBLER, inputs).filter(row -> row.id().equals(press.id())).isEmpty(),
                "Malformed primary Matrix NBT cannot match through the pure role index");
        inputs[0] = high;
        inputs[1] = inputs[1].copyWithCount(inputs[1].getCount() - 1);
        check(ProcessLookup.find(MachineFamily.ASSEMBLER, inputs).filter(row -> row.id().equals(press.id())).isEmpty(),
                "A minimum-primary index still requires the entire actual material batch");
    }

    private void registered() {
        IngredientSpec minimum = IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 2);
        IngredientSpec f2 = IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F2, 2);
        IngredientSpec exact = new IngredientSpec(IngredientSpec.ITEM, FabricationLogic.RELAY_MATRIX, 2, 1);
        IngredientSpec live = wireMill().itemInputs().get(3);
        IngredientSpec predecessor = recipe("assemble/relay_matrix_f2").itemInputs().get(0);
        Set<MatrixArchitecture> architectures = EnumSet.noneOf(MatrixArchitecture.class);
        int variants = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ControlMatrixItem matrix)) {
                continue;
            }
            architectures.add(matrix.architecture());
            ItemStack legacy = new ItemStack(item, 2);
            check(ProcessLookup.matches(minimum, legacy), "Real legacy frontier has sufficient F1 capacity");
            CompoundTag unrelated = new CompoundTag();
            unrelated.putString("Unrelated", "preserve");
            legacy.setTag(unrelated.copy());
            check(ProcessLookup.matches(minimum, legacy), "Unrelated legacy NBT keeps its frontier default");
            check(legacy.getTag().equals(unrelated), "Reading a legacy Matrix cannot rewrite its NBT");
            for (FluxTier rating : FluxTier.values()) {
                if (!matrix.architecture().supports(rating)) {
                    continue;
                }
                variants++;
                ItemStack stack = matrix.stackFor(rating, 2);
                ItemStack before = stack.copy();
                check(ProcessLookup.matches(minimum, stack), "Real supported capacity satisfies the minimum");
                check(ProcessLookup.matches(f2, stack) == (rating.ordinal() >= 2),
                        "Lower capacity is rejected independently of architecture");
                boolean relayF1 = item == ModItems.RELAY_MATRIX.get() && rating == FluxTier.F1;
                check(ProcessLookup.matches(exact, stack) == relayF1, "Old constructor stays exact");
                check(ProcessLookup.matches(predecessor, stack) == relayF1, "No substitution in the F2 predecessor");
                check(ProcessLookup.accepts(MachineFamily.ASSEMBLER, 3, stack.copyWithCount(1)),
                        "Count-ignoring live admission accepts the first controller");
                check(!ProcessLookup.matches(minimum, stack.copyWithCount(1)), "Full batch count is still required");
                check(!ProcessLookup.matches(minimum, stack.copyWithCount(0)), "Zero count is not physical input");
                check(!ProcessLookup.matches(minimum, stack.copyWithCount(-1)), "Negative count is not physical input");
                ItemStack[] inputs = wireInputs(stack);
                check(ProcessLookup.matches(wireMill(), inputs), "The real selected Wire Mill recipe matches");
                check(ProcessLookup.find(MachineFamily.ASSEMBLER, inputs).orElseThrow().id().equals("assemble/wire_mill"),
                        "The real graph selects the Wire Mill with stronger stock");
                inputs[1].setCount(1);
                check(!ProcessLookup.matches(wireMill(), inputs), "A partial coil batch cannot start");
                check(ProcessLookup.accepts(MachineFamily.ASSEMBLER, 1, inputs[1]),
                        "A hopper may preload the first of two coils");
                SimpleContainer inventory = new SimpleContainer(1);
                inventory.setItem(0, stack.copy());
                check(QuestInventoryEvidence.count(inventory, QuestCatalogue.byId("relay")) == (relayF1 ? 2 : 0),
                        "Quest evidence remains exact F1 rather than minimum control");
                check(ItemStack.matches(before, stack), "Selection and evidence cannot mutate a Matrix");
            }
        }
        check(architectures.contains(MatrixArchitecture.RELAY), "The actual Relay registration must be present");
        List<ItemStack> alternatives = ProcessLookup.matrixInputs(minimum);
        check(alternatives.size() == variants, "Viewer alternatives enumerate only actual registered capacities");
        check(alternatives.stream().allMatch(stack -> stack.getCount() == 2 && ProcessLookup.matches(minimum, stack)),
                "Every viewer alternative matches the same full-count runtime contract");
        check(ProcessLookup.matrixInputs(exact).size() == 1, "Exact viewer evidence has only its exact physical input");
        ControlMatrixItem relay = (ControlMatrixItem) ModItems.RELAY_MATRIX.get();
        check(!ProcessLookup.matches(IngredientSpec.matrix("minecraft:paper", FluxTier.F1, 1),
                relay.stackFor(FluxTier.F1, 1)), "A false declared item identity cannot borrow a real Matrix");
        ItemStack ordinary = new ItemStack(Items.PAPER, 2);
        ordinary.setTag(relay.stackFor(FluxTier.F3, 2).getTag().copy());
        check(!ProcessLookup.matches(live, ordinary), "An ordinary item cannot spoof physical Matrix capacity");
        List<Consumer<CompoundTag>> malformed = List.of(
                tag -> tag.putInt(ControlMatrixNbt.RATING, 0),
                tag -> tag.putInt(ControlMatrixNbt.RATING, -1),
                tag -> tag.putInt(ControlMatrixNbt.RATING, 4),
                tag -> tag.putInt(ControlMatrixNbt.RATING, 16),
                tag -> tag.putString(ControlMatrixNbt.RATING, "3"),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, 1.5),
                tag -> tag.putLong(ControlMatrixNbt.RATING, 4_294_967_297L),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, Double.NaN),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, Double.POSITIVE_INFINITY),
                tag -> tag.put(ControlMatrixNbt.RATING, new CompoundTag()),
                tag -> tag.put(ControlMatrixNbt.RATING, new ListTag()));
        for (Consumer<CompoundTag> mutation : malformed) {
            ItemStack invalid = new ItemStack(relay, 2);
            CompoundTag tag = new CompoundTag();
            mutation.accept(tag);
            invalid.setTag(tag);
            check(!ProcessLookup.matches(live, invalid), "Malformed or partial physical NBT is not admission");
        }
        Grindless.LOG.info("Matrix probe actual architectures={} rated_variants={}; absent architectures not tested natively",
                architectures, variants);
    }

    private void capability(ServerLevel level) {
        check(level.hasChunkAt(POSITION) && level.isInWorldBounds(POSITION)
                && level.getWorldBorder().isWithinBounds(POSITION), "Fixture position must already be loaded and legal");
        check(level.getBlockState(POSITION).isAir() && level.getBlockEntity(POSITION) == null,
                "Never replace an existing diagnostic cell");
        check(level.setBlock(POSITION, ModBlocks.ASSEMBLER.get().defaultBlockState(), 3),
                "Place the actual registered Assembler");
        check(level.getBlockEntity(POSITION) instanceof ProcessMachineBlockEntity,
                "The actual registered process block entity must be present");
        ProcessMachineBlockEntity machine = (ProcessMachineBlockEntity) level.getBlockEntity(POSITION);
        IItemHandler handler = machine.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElseThrow();
        check(handler.getSlots() == 4, "The real sided handler exposes the four unchanged input slots");
        ControlMatrixItem relay = (ControlMatrixItem) ModItems.RELAY_MATRIX.get();
        ItemStack high = relay.stackFor(FluxTier.F3, 2);
        check(machine.canPlaceItemThroughFace(3, high.copyWithCount(1), Direction.UP),
                "Vanilla sided admission accepts the first higher controller");
        check(handler.insertItem(3, high.copyWithCount(1), true).isEmpty() && machine.getItem(3).isEmpty(),
                "Forge hopper simulation admits without spending");
        check(handler.insertItem(3, high.copyWithCount(1), false).isEmpty() && machine.getItem(3).getCount() == 1,
                "Actual Forge hopper admission inserts the first higher controller");
        check(handler.insertItem(3, high.copyWithCount(1), false).isEmpty() && machine.getItem(3).getCount() == 2,
                "Actual Forge hopper admission accumulates rather than requiring a full batch");
        ItemStack[] inputs = wireInputs(high);
        for (int slot = 0; slot < 3; slot++) {
            check(handler.insertItem(slot, inputs[slot], false).isEmpty(), "Actual material admission for input " + slot);
        }
        check(machine.saveWithoutMetadata().getString("Recipe").equals("assemble/wire_mill"),
                "Actual capability insertion selects the named machine route");
        machine.clearContent();
        check(level.removeBlock(POSITION, false), "Remove only the successful empty diagnostic fixture");
    }

    private static ItemStack[] wireInputs(ItemStack matrix) {
        ItemStack[] inputs = new ItemStack[4];
        for (int index = 0; index < 3; index++) {
            IngredientSpec input = wireMill().itemInputs().get(index);
            inputs[index] = ProcessLookup.resolve(new OutputSpec(input.kind(), input.id(), input.count(), false))
                    .orElseThrow();
        }
        inputs[3] = matrix.copy();
        return inputs;
    }

    private static ProcessRecipe wireMill() {
        return recipe("assemble/wire_mill");
    }

    private static ProcessRecipe recipe(String id) {
        return ProcessLookup.recipes().stream().filter(recipe -> recipe.id().equals(id)).findFirst().orElseThrow();
    }

    private void check(boolean condition, String message) {
        assertions++;
        require(condition, message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
