package io.github.ezequiel24123z.grindless.forge.recipe;

import com.mojang.authlib.GameProfile;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.AssemblerWork;
import io.github.ezequiel24123z.grindless.menu.ProcessMachineMenu;
import io.github.ezequiel24123z.grindless.menu.MachineMenuHost.SelectionResult;
import io.github.ezequiel24123z.grindless.menu.MachineMenuHost.SelectionIssue;
import io.github.ezequiel24123z.grindless.material.MaterialRegistry;
import io.github.ezequiel24123z.grindless.container.FluidInsert;
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
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
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
import java.util.UUID;
import java.util.Optional;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

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
        } catch (ReflectiveOperationException | IOException failure) {
            Grindless.LOG.error("GRINDLESS_MATRIX_INPUT_NATIVE_FAIL assertions={}", checks.assertions, failure);
            throw new IllegalStateException("The authored native selection fixture could not execute", failure);
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
        Item thermalCore = ModItems.THERMAL_GENERATOR_CORE.get();
        check(thermalCore.getClass() == Item.class
                        && BuiltInRegistries.ITEM.getKey(thermalCore).toString()
                        .equals(FabricationLogic.THERMAL_GENERATOR_CORE),
                "The ordinary Thermal core is bound to its declared real item registration");
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

    private void capability(ServerLevel level) throws ReflectiveOperationException, IOException {
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
        MenuProbe probe = explicitSelection(level, machine);
        machine.clearContent();
        check(level.removeBlock(POSITION, false), "Remove only the successful empty diagnostic fixture");
        CompoundTag removed = machine.saveWithoutMetadata();
        check(probe.menu().selectRecipe(probe.player(), probe.menu().containerId,
                        probe.menu().synchronizedOpeningToken(), 0, "assemble/chemical_washer") == SelectionResult.CLOSED,
                "A removed actual host cannot receive an old menu selection");
        check(machine.saveWithoutMetadata().equals(removed), "A stale removed-host request preserves all retained state");
        probe.player().containerMenu = probe.player().inventoryMenu;
    }

    private record MenuProbe(ServerPlayer player, ProcessMachineMenu menu) {
    }

    /** Harness materials and Flux prove the selector/transaction, not a no-command Survival supply route. */
    private MenuProbe explicitSelection(ServerLevel level, ProcessMachineBlockEntity machine)
            throws ReflectiveOperationException, IOException {
        machine.clearContent();
        check(machine.network() == null, "This isolated high fixture must not draw another diagnostic network");
        ProcessRecipe washer = recipe("assemble/chemical_washer");
        ProcessRecipe reactor = recipe("assemble/chemical_reactor");
        check(washer.inputs().equals(reactor.inputs()), "Use two real unchanged same-bill construction rows");
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000018501"), "AssemblerProbe"));
        player.setPos(POSITION.getX() + 0.5, POSITION.getY() + 0.5, POSITION.getZ() + 0.5);
        ProcessMachineMenu menu = new ProcessMachineMenu(85, player.getInventory(), machine);
        player.containerMenu = menu;
        long token = menu.synchronizedOpeningToken();
        check(token != 0L, "An actual server menu has its own nonzero opening token");
        int revision = selectionRevision(menu);
        CompoundTag unchanged = machine.saveWithoutMetadata();
        check(menu.selectRecipe(player, menu.containerId + 1, token, revision, washer.id()) == SelectionResult.CLOSED,
                "A wrong actual container ID refuses");
        check(menu.selectRecipe(player, menu.containerId, token + 1, revision, washer.id()) == SelectionResult.STALE,
                "A foreign actual opening refuses");
        check(menu.selectRecipe(player, menu.containerId, token, revision - 1, washer.id()) == SelectionResult.STALE,
                "A stale actual catalogue revision refuses");
        check(menu.selectRecipe(player, menu.containerId, token, revision, "removed/request") == SelectionResult.INVALID_RECIPE,
                "An unknown current ID cannot become a new selected configuration");
        check(menu.selectRecipe(player, menu.containerId, token, revision, "electrolysis/water")
                        == SelectionResult.INVALID_RECIPE,
                "A real recipe of another family cannot become an Assembler target");
        player.getAbilities().mayBuild = false;
        check(menu.selectRecipe(player, menu.containerId, token, revision, washer.id()) == SelectionResult.DENIED,
                "Actual player build permission is enforced before changing the machine");
        player.getAbilities().mayBuild = true;
        check(machine.saveWithoutMetadata().equals(unchanged), "All invalid/unauthorized requests preserve exact state");
        menuSynchronization(level, machine, menu);
        ServerPlayer other = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000018502"), "OtherAssembler"));
        other.setPos(player.getX(), player.getY(), player.getZ());
        other.containerMenu = menu;
        check(menu.selectRecipe(other, menu.containerId, token, revision, washer.id()) == SelectionResult.CLOSED,
                "Another physical player cannot reuse the current menu");
        other.containerMenu = other.inventoryMenu;

        Object items = machine.menuContainer();
        Object energy = machine.energy();
        Object fluids = machine.nativeStore();
        Object config = machine.containerConfig();
        long tick = level.getGameTime() + 1;
        Method work = ProcessMachineBlockEntity.class.getDeclaredMethod("work", long.class, List.class);
        work.setAccessible(true);
        for (ProcessRecipe selected : List.of(washer, reactor)) {
            choose(player, menu, selected.id(), SelectionResult.SELECTED);
            ItemStack[] physical = actualInputs(selected);
            ProcessRecipe auto = ProcessLookup.find(MachineFamily.ASSEMBLER, physical).orElseThrow();
            check(!auto.id().equals(selected.id()), "The actual dry-last ambiguity exists independently of selection");
            supply(machine, physical);
            for (int paid = 0; paid < selected.durationTicks(); paid++) {
                machine.energy().setStored(selected.fuPerTick());
                work.invoke(machine, tick++, List.<FluidInsert.Source>of());
                check(machine.energy().getStored() == 0L, "One actual working tick pays the existing Flux buffer once");
            }
            Item expected = BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(
                    selected.itemOutputs().get(0).id()));
            check(machine.getItem(4).is(expected) && machine.getItem(4).getCount() == 1,
                    "The real BE manufactures exactly the selected registered output, not dry-last");
            for (int slot = 0; slot < selected.itemInputs().size(); slot++) {
                check(machine.getItem(slot).isEmpty(), "The real completion consumes declared input group " + slot);
            }
            check(machine.inspectionProgress() == 0.0, "An exact full cycle has no duplicated work");
            machine.setItem(4, ItemStack.EMPTY);
        }
        choose(player, menu, washer.id(), SelectionResult.SELECTED);
        supply(machine, actualInputs(washer));
        CompoundTag saved = machine.saveWithoutMetadata();
        saved.putDouble("Progress", washer.durationTicks() - 0.75);
        CompoundTag speed = new CompoundTag();
        speed.putInt("SPEED", 1);
        saved.put("Upgrades", speed);
        machine.load(binary(saved));
        machine.energy().setStored(washer.fuPerTick());
        work.invoke(machine, tick++, List.<FluidInsert.Source>of());
        check(machine.getItem(4).is(BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(
                        washer.itemOutputs().get(0).id()))) && machine.inspectionProgress() == 1.25,
                "An actual upgraded registered cycle retains fractional carry after successful settlement");
        machine.setItem(4, ItemStack.EMPTY);
        choose(player, menu, reactor.id(), SelectionResult.SELECTED);
        saved = machine.saveWithoutMetadata();
        saved.putDouble("Progress", 23.375);
        machine.load(binary(saved));
        for (int reopen = 0; reopen < 8; reopen++) {
            choose(player, menu, washer.id(), SelectionResult.SELECTED);
            check(machine.inspectionProgress() == 1.25, "Actual switching restores the exact fractional Washer carry");
            choose(player, menu, reactor.id(), SelectionResult.SELECTED);
            check(machine.inspectionProgress() == 23.375, "Actual switching restores the exact partial Reactor work");
            machine.load(binary(machine.saveWithoutMetadata()));
            check(machine.inspectionProgress() == 23.375, "Actual binary reopen preserves the active paid balance");
            check(machine.menuContainer() == items && machine.energy() == energy
                            && machine.nativeStore() == fluids && machine.containerConfig() == config,
                    "Actual reopening preserves exposed inventory/Flux/fluid/config object identities");
        }

        choose(player, menu, washer.id(), SelectionResult.SELECTED);
        supply(machine, actualInputs(washer));
        machine.setItem(4, new ItemStack(Items.STONE, 64));
        saved = machine.saveWithoutMetadata();
        saved.putDouble("Progress", washer.durationTicks() + 0.25);
        machine.load(binary(saved));
        machine.energy().setStored(64L);
        for (int blocked = 0; blocked < 3; blocked++) {
            work.invoke(machine, tick++, List.<FluidInsert.Source>of());
        }
        check(machine.inspectionProgress() == washer.durationTicks() + 0.25 && machine.energy().getStored() == 64L,
                "Completed native blocked work neither redraws Flux nor loses its fractional overrun");
        check(machine.recipeSelection().orElseThrow().issue() == SelectionIssue.OUTPUT_BLOCKED,
                "The native selected-target diagnosis reports blocked output");
        choose(player, menu, reactor.id(), SelectionResult.SELECTED);
        choose(player, menu, washer.id(), SelectionResult.SELECTED);
        check(machine.inspectionProgress() == washer.durationTicks() + 0.25,
                "Completed blocked native work remains reachable after switching");
        machine.setItem(4, ItemStack.EMPTY);
        work.invoke(machine, tick++, List.<FluidInsert.Source>of());
        check(machine.energy().getStored() == 64L && machine.inspectionProgress() == 0.25
                        && machine.getItem(4).getCount() == 1,
                "Native unblocking settles once without charging completed work again");
        machine.setItem(4, ItemStack.EMPTY);

        supply(machine, actualInputs(washer));
        saved = machine.saveWithoutMetadata();
        saved.putDouble("Progress", washer.durationTicks() + 0.375);
        machine.load(binary(saved));
        CompoundTag beforeSettlement = machine.saveWithoutMetadata();
        boolean[] aborted = {false};
        ((SimpleContainer) machine.menuContainer()).addListener(container -> {
            if (!aborted[0] && machine.getItem(0).isEmpty()) {
                aborted[0] = true;
                CompoundTag tentative = machine.saveWithoutMetadata();
                check(menu.selectRecipe(player, menu.containerId, token, currentRevision(menu), reactor.id())
                                == SelectionResult.BUSY,
                        "The actual server menu refuses reentrant choice during tentative settlement");
                check(machine.saveWithoutMetadata().equals(tentative),
                        "Reentrant menu refusal does not mutate tentative transaction state");
                throw new IllegalStateException("authored native Assembler settlement abort");
            }
        });
        try {
            work.invoke(machine, tick++, List.<FluidInsert.Source>of());
            check(false, "The authored native settlement observer must abort");
        } catch (InvocationTargetException expected) {
            check(expected.getCause() instanceof IllegalStateException
                            && expected.getCause().getMessage().equals("authored native Assembler settlement abort"),
                    "The actual native transaction preserves its original observer exception");
        }
        check(machine.saveWithoutMetadata().equals(beforeSettlement),
                "Native rollback restores full paid work, target, items, fluids, configuration and Flux");

        int oldRevision = selectionRevision(menu);
        ProcessLookup.Generation oldGeneration = ProcessLookup.generation();
        ProcessLookup.rebuild(MaterialRegistry.snapshot());
        check(oldGeneration != ProcessLookup.generation(), "Native reindex replaces the complete current graph");
        CompoundTag beforeReloadRequest = machine.saveWithoutMetadata();
        check(menu.selectRecipe(player, menu.containerId, token, oldRevision, reactor.id()) == SelectionResult.STALE,
                "A native catalogue reload refuses the old menu revision without guessing a new output");
        check(machine.saveWithoutMetadata().equals(beforeReloadRequest), "Reload refusal preserves all paid state");
        choose(player, menu, reactor.id(), SelectionResult.SELECTED);
        saved = machine.saveWithoutMetadata();
        saved.putString(AssemblerWork.TARGET, "removed/native_saved_target");
        saved.putString("WorkRecipe", "removed/native_paid_owner");
        saved.putDouble("Progress", 0.125);
        machine.load(binary(saved));
        check(machine.recipeSelection().orElseThrow().issue() == SelectionIssue.UNAVAILABLE
                        && machine.saveWithoutMetadata().getString(AssemblerWork.TARGET).equals("removed/native_saved_target"),
                "A saved unknown native target stays explicitly unavailable rather than resetting to AUTO");
        choose(player, menu, washer.id(), SelectionResult.SELECTED);
        check(machine.recipeSelection().orElseThrow().suspended().get("removed/native_paid_owner") == 0.125,
                "Unknown native paid work remains preserved and cannot be spent by a known target");
        choose(player, menu, "", SelectionResult.SELECTED);
        check(machine.saveWithoutMetadata().getString(AssemblerWork.TARGET).isEmpty(),
                "The actual server menu can deliberately return to legacy AUTO");

        player.containerMenu = player.inventoryMenu;
        check(menu.selectRecipe(player, menu.containerId, token, currentRevision(menu), washer.id()) == SelectionResult.CLOSED,
                "A closed actual menu cannot change the machine");
        ProcessMachineMenu reopened = new ProcessMachineMenu(86, player.getInventory(), machine);
        player.containerMenu = reopened;
        check(reopened.synchronizedOpeningToken() != token, "Reopening binds a new actual menu identity");
        check(reopened.selectRecipe(player, reopened.containerId, token, selectionRevision(reopened), washer.id())
                        == SelectionResult.STALE,
                "A prior opening's token cannot select through a reopened menu");
        choose(player, reopened, washer.id(), SelectionResult.SELECTED);
        return new MenuProbe(player, reopened);
    }

    /** Actual menu state methods over a vanilla Player carrier, not a graphical or wire-delivery pass. */
    private void menuSynchronization(ServerLevel level, ProcessMachineBlockEntity machine, ProcessMachineMenu server)
            throws ReflectiveOperationException {
        Player viewer = new Player(level, POSITION, 0.0F,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000018503"), "MenuStateCarrier")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        ProcessMachineMenu client = new ProcessMachineMenu(server.containerId, viewer.getInventory(), machine);
        long token = server.synchronizedOpeningToken();
        int revision = selectionRevision(server);
        var state = machine.recipeSelection().orElseThrow();
        List<ProcessMachineMenu.RecipeChoice> catalogue = ProcessLookup.recipes(MachineFamily.ASSEMBLER).stream()
                .map(row -> new ProcessMachineMenu.RecipeChoice(row.id(),
                        ProcessLookup.resolve(row.itemOutputs().get(0)).orElseThrow())).toList();
        client.receiveSelection(token, revision, state, Optional.of(catalogue), false);
        check(client.selection().isEmpty(), "State before native opening data is not an authoritative usable picker");
        for (int part = 0; part < 3; part++) {
            client.setData(ProcessMachineMenu.DATA_SIZE + part, (short) (token >>> (part * 16)));
            check(client.selection().isEmpty(), "A partially delivered native opening identity remains disabled");
        }
        client.setData(ProcessMachineMenu.DATA_SIZE + 3, (short) (token >>> 48));
        check(client.selection().orElseThrow().state().equals(state),
                "All four native data shorts unlock only the matching authoritative state");
        client.setData(ProcessMachineMenu.DATA_SIZE + 4, SelectionResult.DENIED.ordinal());
        check(client.selectionResult() == SelectionResult.DENIED,
                "The native menu exposes a server refusal even when chat is hidden");
        client.setData(ProcessMachineMenu.DATA_SIZE + 4, 32767);
        check(client.selectionResult() == SelectionResult.MALFORMED,
                "An unknown native result value cannot display success");
        client.setData(ProcessMachineMenu.DATA_SIZE + 4, SelectionResult.UNCHANGED.ordinal());
        check(client.selection().orElseThrow().choices().stream().map(ProcessMachineMenu.RecipeChoice::id).toList()
                        .equals(catalogue.stream().map(ProcessMachineMenu.RecipeChoice::id).toList()),
                "Native menu state keeps server stable IDs without consulting another client graph");
        client.receiveSelection(token + 1, revision + 1, state, Optional.of(List.of()), true);
        check(client.selection().orElseThrow().choices().size() == catalogue.size(),
                "A foreign opening snapshot cannot replace a valid native menu catalogue");
        client.receiveSelection(token, revision + 1, state, Optional.empty(), false);
        check(client.selection().orElseThrow().choices().size() == catalogue.size(),
                "A configuration/work-only snapshot preserves the already authoritative catalogue");
        List<ProcessMachineMenu.RecipeChoice> reordered = List.of(
                catalogue.get(catalogue.size() - 1), catalogue.get(0));
        client.receiveSelection(token, revision + 2, state, Optional.of(reordered), false);
        check(client.selection().orElseThrow().choices().get(0).id().equals(reordered.get(0).id()),
                "A replacement catalogue is accepted by exact IDs, never a former transient ordinal");
        client.receiveSelection(token, revision, state, Optional.of(catalogue), false);
        check(client.selection().orElseThrow().revision() == revision + 2
                        && client.selection().orElseThrow().choices().size() == reordered.size(),
                "A stale snapshot cannot undo an authoritative catalogue reload");
        Field pending = ProcessMachineMenu.class.getDeclaredField("selectionPending");
        pending.setAccessible(true);
        pending.setBoolean(client, true);
        client.receiveSelection(token, revision + 2, state, Optional.empty(), false);
        check(client.selectionPending(), "An unrelated state update does not acknowledge a pending choice");
        client.receiveSelection(token, revision + 2, state, Optional.empty(), true);
        check(!client.selectionPending(), "The exact opening's acknowledgement releases the pending native control");
        check(machine.recipeSelection().orElseThrow().equals(state),
                "Client-side display/reload acknowledgement never mutates actual machine configuration or work");
    }

    private void choose(ServerPlayer player, ProcessMachineMenu menu, String id, SelectionResult expected)
            throws ReflectiveOperationException {
        SelectionResult actual = menu.selectRecipe(player, menu.containerId, menu.synchronizedOpeningToken(),
                selectionRevision(menu), id);
        check(actual == expected, "Actual menu selection " + id + " expected=" + expected + " actual=" + actual);
    }

    private static int currentRevision(ProcessMachineMenu menu) {
        try {
            return selectionRevision(menu);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot inspect the actual native menu revision", failure);
        }
    }

    private static int selectionRevision(ProcessMachineMenu menu) throws ReflectiveOperationException {
        Method refresh = ProcessMachineMenu.class.getDeclaredMethod("refreshSelection");
        refresh.setAccessible(true);
        refresh.invoke(menu);
        Field revision = ProcessMachineMenu.class.getDeclaredField("selectionRevision");
        revision.setAccessible(true);
        return revision.getInt(menu);
    }

    private ItemStack[] actualInputs(ProcessRecipe recipe) {
        ItemStack[] inputs = new ItemStack[] {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        for (int slot = 0; slot < recipe.itemInputs().size(); slot++) {
            IngredientSpec input = recipe.itemInputs().get(slot);
            inputs[slot] = ProcessLookup.resolve(new OutputSpec(input.kind(), input.id(), input.count(), false)).orElseThrow();
            check(ProcessLookup.matches(input, inputs[slot]), "Real selected fixture input " + input.qualified());
        }
        return inputs;
    }

    private static void supply(ProcessMachineBlockEntity machine, ItemStack[] inputs) {
        for (int slot = 0; slot < inputs.length; slot++) {
            machine.setItem(slot, inputs[slot].copy());
        }
    }

    private static CompoundTag binary(CompoundTag state) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            NbtIo.write(state, output);
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return NbtIo.read(input);
        }
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
