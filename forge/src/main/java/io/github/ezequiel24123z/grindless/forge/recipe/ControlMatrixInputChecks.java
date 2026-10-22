package io.github.ezequiel24123z.grindless.forge.recipe;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.AssemblerWork;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
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
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
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

/** Existing-harness manual-ingredient and permitted-tool/menu probe, never a registry bootstrap or client proof. */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class ControlMatrixInputChecks {
    private static final BlockPos POSITION = new BlockPos(8, 300, 8);
    private static final String BLUEPRINT = "assemble/blueprint_tool";
    private static final String PLANNER = "assemble/deconstruction_planner";
    private static final String RETIRED = "assemble/chemical_washer";
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
            checks.manualCrafting(source.getLevel());
            checks.capability(source.getLevel());
            checks.benchHeldWork(source.getLevel());
            Grindless.LOG.info("GRINDLESS_MATRIX_INPUT_NATIVE_PASS assertions={} scope=registered_manual_ingredient_permitted_tool_menu_and_held_bench_work",
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

    /** One real loaded shaped recipe, not a fake process row or an exhaustive per-machine runtime matrix. */
    private void manualCrafting(ServerLevel level) {
        ResourceLocation id = new ResourceLocation(Grindless.MOD_ID, "pulverizer");
        var loaded = level.getRecipeManager().byKey(id).orElseThrow();
        check(loaded instanceof ShapedRecipe, "RecipeManager must load the real grindless:pulverizer shaped recipe");
        ShapedRecipe shaped = (ShapedRecipe) loaded;
        check(shaped.getWidth() > 0 && shaped.getWidth() <= 3 && shaped.getHeight() > 0 && shaped.getHeight() <= 3,
                "The representative manual recipe fits the native crafting table");
        List<Ingredient> controls = shaped.getIngredients().stream()
                .filter(ControlMatrixInputChecks::minimumControlJson).toList();
        check(controls.size() == 1, "The loaded manual bill has exactly one serialized minimum-control cell");
        Ingredient control = controls.get(0);
        check(control.toJson().getAsJsonObject().get("tier").getAsString().equals(FluxTier.F1.name()),
                "The real manual constructor requires baseline F1 control");
        manualIngredient(control);
        ItemStack baseline = new ItemStack(ModBlocks.PULVERIZER.get());
        check(ItemStack.matches(shaped.getResultItem(level.registryAccess()), baseline),
                "Higher control must still produce the unchanged baseline Pulverizer, not a tier upgrade");

        check(level.hasChunkAt(POSITION) && level.isInWorldBounds(POSITION)
                        && level.getWorldBorder().isWithinBounds(POSITION),
                "The manual carrier reuses the already loaded legal diagnostic cell");
        check(level.getBlockState(POSITION).isAir() && level.getBlockEntity(POSITION) == null,
                "Never replace an existing block for the manual carrier");
        check(level.setBlock(POSITION, Blocks.CRAFTING_TABLE.defaultBlockState(), 3),
                "Place one actual vanilla crafting table in the existing disposable carrier");
        // Forge's public FakePlayer supplies a no-delivery connection for vanilla result updates.
        // It is not a connected client, packet-delivery, rendered-GUI or Survival proof.
        FakePlayer player = new FakePlayer(level,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000018801"), "ManualCraftProbe"));
        player.setPos(POSITION.getX() + 0.5, POSITION.getY() + 0.5, POSITION.getZ() + 0.5);
        check(player.getInventory().isEmpty(), "The manual carrier begins with no invented finished output");
        CraftingMenu menu = new CraftingMenu(88, player.getInventory(), ContainerLevelAccess.create(level, POSITION));
        player.containerMenu = menu;
        check(menu.stillValid(player), "The actual native crafting menu is bound to the nearby real table");
        CraftingContainer grid = (CraftingContainer) menu.getSlot(1).container;
        int controlCell = -1;
        for (int row = 0; row < shaped.getHeight(); row++) {
            for (int col = 0; col < shaped.getWidth(); col++) {
                Ingredient ingredient = shaped.getIngredients().get(row * shaped.getWidth() + col);
                if (ingredient.isEmpty()) {
                    continue;
                }
                ItemStack[] candidates = ingredient.getItems();
                check(candidates.length > 0, "Each real shaped material cell has a loaded physical candidate");
                int cell = row * grid.getWidth() + col;
                ItemStack physical = candidates[0].copyWithCount(2);
                if (minimumControlJson(ingredient)) {
                    controlCell = cell;
                    physical = ((ControlMatrixItem) ModItems.RELAY_MATRIX.get()).stackFor(FluxTier.F3, 2);
                }
                check(ingredient.test(physical), "The representative bill derives each input from its loaded Ingredient");
                grid.setItem(cell, physical);
            }
        }
        check(controlCell >= 0, "The actual manual grid contains its declared minimum-control cell");
        ControlMatrixItem relay = (ControlMatrixItem) ModItems.RELAY_MATRIX.get();
        grid.setItem(controlCell, new ItemStack(relay, 2));
        manualResult(level, shaped, grid, menu, baseline, true, "untagged registered default F1");
        for (Consumer<CompoundTag> mutation : malformedRatings()) {
            ItemStack invalid = new ItemStack(relay, 2);
            CompoundTag tag = new CompoundTag();
            mutation.accept(tag);
            invalid.setTag(tag);
            grid.setItem(controlCell, invalid);
            manualResult(level, shaped, grid, menu, baseline, false, "malformed/unsupported physical control");
        }
        ItemStack unrelated = new ItemStack(Items.PAPER, 2);
        unrelated.setTag(relay.stackFor(FluxTier.F3, 2).getTag().copy());
        grid.setItem(controlCell, unrelated);
        manualResult(level, shaped, grid, menu, baseline, false, "unrelated item with copied F3 NBT");
        grid.setItem(controlCell, unknownMatrix(relay));
        manualResult(level, shaped, grid, menu, baseline, false, "unregistered encoded item identity");
        grid.setItem(controlCell, ItemStack.EMPTY);
        manualResult(level, shaped, grid, menu, baseline, false, "missing control");
        grid.setItem(controlCell, relay.stackFor(FluxTier.F3, 2));
        manualResult(level, shaped, grid, menu, baseline, true, "registered higher F3");
        List<ItemStack> before = java.util.stream.IntStream.range(0, grid.getContainerSize())
                .mapToObj(cell -> grid.getItem(cell).copy()).toList();
        check(ItemStack.matches(menu.quickMoveStack(player, 0), baseline),
                "One native result-slot transfer settles exactly one baseline output into inventory");
        check(player.getInventory().countItem(baseline.getItem()) == 1,
                "The real result transfer owns one output, not the next preview or a repeated craft");
        for (int cell = 0; cell < grid.getContainerSize(); cell++) {
            ItemStack prior = before.get(cell);
            ItemStack remaining = grid.getItem(cell);
            check(prior.isEmpty() ? remaining.isEmpty()
                            : ItemStack.isSameItemSameTags(prior, remaining) && remaining.getCount() == prior.getCount() - 1,
                    "Native settlement consumes each actual shaped cell exactly once: " + cell);
        }
        check(ItemStack.matches(grid.getItem(controlCell), relay.stackFor(FluxTier.F3, 1)),
                "Native settlement consumes one real higher Matrix without altering its remaining rating");
        menu.slotsChanged(grid);
        check(player.getInventory().countItem(baseline.getItem()) == 1,
                "Recomputing the next result cannot grant or settle a second output");
        // This no-delivery FakePlayer is offline. Move real leftovers through the native input slots
        // before close, rather than assuming vanilla's disconnected-player close returns them to inventory.
        for (int cell = 0; cell < grid.getContainerSize(); cell++) {
            ItemStack leftover = grid.getItem(cell).copy();
            if (!leftover.isEmpty()) {
                check(ItemStack.matches(menu.quickMoveStack(player, cell + 1), leftover) && grid.getItem(cell).isEmpty(),
                        "Native input-slot transfer returns only the exact unconsumed stock: " + cell);
            }
        }
        menu.removed(player);
        player.containerMenu = player.inventoryMenu;
        check(player.getInventory().countItem(baseline.getItem()) == 1
                        && player.getInventory().countItem(relay) == 1 && grid.isEmpty(),
                "Native transfers/close retain one unconsumed controller and one crafted output, with no extra grant");
        check(level.removeBlock(POSITION, false), "Remove only the successful empty manual diagnostic table");
    }

    private static boolean minimumControlJson(Ingredient ingredient) {
        var json = ingredient.toJson();
        return json.isJsonObject() && json.getAsJsonObject().has("type")
                && json.getAsJsonObject().get("type").getAsString().equals("grindless:minimum_control");
    }

    private void manualResult(ServerLevel level, ShapedRecipe shaped, CraftingContainer grid, CraftingMenu menu,
                              ItemStack baseline, boolean accepted, String label) {
        check(shaped.matches(grid, level) == accepted, "Loaded shaped matching: " + label);
        var current = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        check(accepted ? current.isPresent() && current.get().getId().equals(shaped.getId()) : current.isEmpty(),
                "Actual RecipeManager authority, with no fallback: " + label);
        check(accepted ? ItemStack.matches(menu.getSlot(0).getItem(), baseline) : menu.getSlot(0).getItem().isEmpty(),
                "Actual native result-slot admission: " + label);
    }

    private void manualIngredient(Ingredient loaded) {
        JsonObject contract = new JsonObject();
        contract.addProperty("type", "grindless:minimum_control");
        contract.addProperty("tier", FluxTier.F1.name());
        check(loaded.toJson().equals(contract), "The loaded manual Ingredient retains the agreed serializer JSON");
        Ingredient json = Ingredient.fromJson(loaded.toJson());
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        Ingredient network;
        try {
            loaded.toNetwork(data);
            network = Ingredient.fromNetwork(data);
            check(!data.isReadable(), "The actual custom Ingredient network roundtrip consumes its exact payload");
        } finally {
            data.release();
        }
        ControlMatrixItem relay = (ControlMatrixItem) ModItems.RELAY_MATRIX.get();
        ItemStack high = relay.stackFor(FluxTier.F3, 1);
        ItemStack unrelated = new ItemStack(Items.PAPER);
        unrelated.setTag(high.getTag().copy());
        ItemStack unknown = unknownMatrix(relay);
        int rating = FluxTier.F3.ordinal();
        List<Consumer<CompoundTag>> wholeNumbers = List.of(
                tag -> tag.putByte(ControlMatrixNbt.RATING, (byte) rating),
                tag -> tag.putShort(ControlMatrixNbt.RATING, (short) rating),
                tag -> tag.putInt(ControlMatrixNbt.RATING, rating),
                tag -> tag.putLong(ControlMatrixNbt.RATING, rating),
                tag -> tag.putFloat(ControlMatrixNbt.RATING, rating),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, rating));
        for (Ingredient ingredient : List.of(loaded, json, network)) {
            check(ingredient.toJson().equals(contract) && !ingredient.isSimple(),
                    "Loaded/JSON/network variants retain NBT-sensitive semantics, never simple item expansion");
            check(ingredient.test(new ItemStack(relay)) && ingredient.test(high),
                    "Loaded/JSON/network matching accepts real untagged F1 and factory-built higher F3");
            check(!ingredient.test(ItemStack.EMPTY) && !ingredient.test(unrelated) && !ingredient.test(unknown),
                    "Loaded/JSON/network matching refuses absent, unrelated and unregistered encoded inputs");
            List<ItemStack> display = List.of(ingredient.getItems());
            check(display.size() == 3 && display.stream().allMatch(ingredient::test)
                            && display.stream().allMatch(stack -> stack.getItem() == relay)
                            && display.stream().map(stack -> relay.spec(stack).orElseThrow().rating())
                            .collect(java.util.stream.Collectors.toSet()).equals(Set.of(FluxTier.F1, FluxTier.F2, FluxTier.F3)),
                    "Display candidates are the three real registered Relay capacities, not fictitious architectures");
            for (Consumer<CompoundTag> write : wholeNumbers) {
                ItemStack valid = new ItemStack(relay);
                CompoundTag tag = new CompoundTag();
                write.accept(tag);
                valid.setTag(tag.copy());
                check(ingredient.test(valid) && valid.getTag().equals(tag),
                        "Whole-number legacy NBT types remain valid without rewriting the physical stack");
            }
            for (Consumer<CompoundTag> write : malformedRatings()) {
                ItemStack invalid = new ItemStack(relay);
                CompoundTag tag = new CompoundTag();
                write.accept(tag);
                invalid.setTag(tag);
                check(!ingredient.test(invalid), "Loaded/JSON/network matching refuses malformed physical NBT");
            }
        }
    }

    /** Decode an unknown saved ID; never construct/register an intrusive native Item after registry freeze. */
    private ItemStack unknownMatrix(ControlMatrixItem relay) {
        ResourceLocation unknown = new ResourceLocation(Grindless.MOD_ID, "unregistered_matrix_fixture");
        check(!BuiltInRegistries.ITEM.containsKey(unknown), "The unknown encoded identity must actually be unregistered");
        CompoundTag encoded = relay.stackFor(FluxTier.F3, 1).save(new CompoundTag());
        encoded.putString("id", unknown.toString());
        ItemStack decoded = ItemStack.of(encoded);
        check(decoded.isEmpty(), "Vanilla decoding cannot invent physical stock for an unregistered item ID");
        return decoded;
    }

    private void registered() {
        Item thermalCore = ModItems.THERMAL_GENERATOR_CORE.get();
        check(thermalCore.getClass() == Item.class
                        && BuiltInRegistries.ITEM.getKey(thermalCore).toString()
                        .equals(FabricationLogic.THERMAL_GENERATOR_CORE),
                "The ordinary Thermal core is bound to its declared real item registration");
        IngredientSpec minimum = IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 2);
        IngredientSpec f2 = IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F2, 2);
        IngredientSpec exact = IngredientSpec.matrix(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 2);
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
                check(ProcessLookup.matches(f2, stack) == (rating.ordinal() >= FluxTier.F2.ordinal()),
                        "Lower capacity is rejected independently of architecture");
                boolean relayF1 = item == ModItems.RELAY_MATRIX.get() && rating == FluxTier.F1;
                check(ProcessLookup.matches(exact, stack) == relayF1, "Old constructor stays exact");
                check(ProcessLookup.matches(predecessor, stack) == relayF1, "No substitution in the F2 predecessor");
                check(!ProcessLookup.matches(minimum, stack.copyWithCount(1)), "Full batch count is still required");
                check(!ProcessLookup.matches(minimum, stack.copyWithCount(0)), "Zero count is not physical input");
                check(!ProcessLookup.matches(minimum, stack.copyWithCount(-1)), "Negative count is not physical input");
                SimpleContainer inventory = new SimpleContainer(1);
                inventory.setItem(0, stack.copy());
                check(QuestInventoryEvidence.count(inventory, QuestCatalogue.byId("relay")) == (relayF1 ? 2 : 0),
                        "Quest evidence remains exact F1 rather than minimum control");
                check(ItemStack.matches(before, stack), "Selection and evidence cannot mutate a Matrix");
            }
        }
        check(architectures.equals(EnumSet.of(MatrixArchitecture.RELAY)),
                "Only Relay is really registered; absent architectures remain pure contracts, not native fixtures");
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
        check(!ProcessLookup.matches(minimum, ordinary), "An ordinary item cannot spoof physical Matrix capacity");
        for (Consumer<CompoundTag> mutation : malformedRatings()) {
            ItemStack invalid = new ItemStack(relay, 2);
            CompoundTag tag = new CompoundTag();
            mutation.accept(tag);
            invalid.setTag(tag);
            check(!ProcessLookup.matches(minimum, invalid), "Malformed or partial physical NBT is not admission");
        }
        Grindless.LOG.info("Matrix probe actual architectures={} rated_variants={}; absent architectures not tested natively",
                architectures, variants);
    }

    private static List<Consumer<CompoundTag>> malformedRatings() {
        return List.of(
                tag -> tag.putInt(ControlMatrixNbt.RATING, 0),
                tag -> tag.putInt(ControlMatrixNbt.RATING, -1),
                tag -> tag.putInt(ControlMatrixNbt.RATING, FluxTier.F4.ordinal()),
                tag -> tag.putInt(ControlMatrixNbt.RATING, FluxTier.values().length),
                tag -> tag.putString(ControlMatrixNbt.RATING, Integer.toString(FluxTier.F3.ordinal())),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, FluxTier.F1.ordinal() + 0.5),
                tag -> tag.putFloat(ControlMatrixNbt.RATING, FluxTier.F1.ordinal() + 0.5f),
                tag -> tag.putLong(ControlMatrixNbt.RATING, 4_294_967_297L),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, Double.NaN),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, Double.POSITIVE_INFINITY),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, Double.NEGATIVE_INFINITY),
                tag -> tag.put(ControlMatrixNbt.RATING, new CompoundTag()),
                tag -> tag.put(ControlMatrixNbt.RATING, new ListTag()));
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
        ProcessRecipe tool = recipe(BLUEPRINT);
        ItemStack[] inputs = actualInputs(tool);
        int grouped = tool.itemInputs().size();
        check(grouped > 0 && grouped <= handler.getSlots(), "Use the actual permitted tool's grouped bill");
        int accumulated = java.util.stream.IntStream.range(0, grouped)
                .filter(slot -> inputs[slot].getCount() > 1).findFirst().orElseThrow();
        ItemStack first = inputs[accumulated].copyWithCount(1);
        check(machine.canPlaceItemThroughFace(accumulated, first, Direction.UP),
                "Vanilla sided admission accepts the first unit of a real grouped tool input");
        check(handler.insertItem(accumulated, first.copy(), true).isEmpty() && machine.getItem(accumulated).isEmpty(),
                "Forge hopper simulation admits without spending");
        check(handler.insertItem(accumulated, first.copy(), false).isEmpty()
                        && machine.getItem(accumulated).getCount() == 1,
                "Actual Forge hopper admission inserts the first grouped material");
        check(machine.saveWithoutMetadata().getString("Recipe").isEmpty(),
                "One preloaded unit cannot start a partial grouped bill");
        check(handler.insertItem(accumulated, first.copy(), false).isEmpty()
                        && machine.getItem(accumulated).getCount() == 2,
                "Actual Forge hopper admission accumulates rather than requiring a full batch");
        for (int slot = 0; slot < grouped; slot++) {
            int remaining = inputs[slot].getCount() - (slot == accumulated ? 2 : 0);
            if (remaining > 0) {
                check(handler.insertItem(slot, inputs[slot].copyWithCount(remaining), false).isEmpty(),
                        "Actual material admission for input " + slot);
            }
        }
        ProcessRecipe expectedAuto = ProcessLookup.find(MachineFamily.ASSEMBLER, inputs).orElseThrow();
        check(machine.saveWithoutMetadata().getString("Recipe").equals(expectedAuto.id()),
                "Actual capability insertion keeps the live lookup-derived AUTO choice: " + expectedAuto.id());
        MenuProbe probe = explicitSelection(level, machine);
        machine.clearContent();
        check(level.removeBlock(POSITION, false), "Remove only the successful empty diagnostic fixture");
        CompoundTag removed = machine.saveWithoutMetadata();
        check(probe.menu().selectRecipe(probe.player(), probe.menu().containerId,
                        probe.menu().synchronizedOpeningToken(), 0, BLUEPRINT) == SelectionResult.CLOSED,
                "A removed actual host cannot receive an old menu selection");
        check(machine.saveWithoutMetadata().equals(removed), "A stale removed-host request preserves all retained state");
        probe.player().containerMenu = probe.player().inventoryMenu;
    }

    /** One actual Bench/retired owner/current competitor in the same command-gated aggregate and diagnostic cell. */
    private void benchHeldWork(ServerLevel level) throws ReflectiveOperationException, IOException {
        String retired = "bench/furnace";
        double paid = SourceLogic.BENCH_TICKS + 0.375;
        check(level.hasChunkAt(POSITION) && level.isInWorldBounds(POSITION)
                        && level.getWorldBorder().isWithinBounds(POSITION)
                        && level.getBlockState(POSITION).isAir() && level.getBlockEntity(POSITION) == null,
                "Reuse only the successful previous probe's empty loaded diagnostic cell");
        check(ProcessLookup.byId(MachineFamily.ASSEMBLY_BENCH, retired).isEmpty(),
                "The real native catalogue must have retired the normal Furnace constructor");
        ProcessRecipe permitted = ProcessLookup.byId(MachineFamily.ASSEMBLY_BENCH, "bench/brick_block").orElseThrow();
        check(level.setBlock(POSITION, ModBlocks.ASSEMBLY_BENCH.get().defaultBlockState(), 3),
                "Place the actual registered Bench, not a fake process registration");
        check(level.getBlockEntity(POSITION) instanceof ProcessMachineBlockEntity,
                "The actual Bench block-entity registration must bind");
        ProcessMachineBlockEntity bench = (ProcessMachineBlockEntity) level.getBlockEntity(POSITION);
        check(bench.kind() == ProcessMachineKind.ASSEMBLY_BENCH && bench.menuKind().inputs() == 4,
                "The representative carrier uses the unchanged real Bench inventory");
        ItemStack[] physical = actualInputs(permitted);
        supply(bench, physical);
        bench.energy().setStored(bench.energy().getCapacity());
        long fuel = bench.energy().getStored();
        CompoundTag legacy = bench.saveWithoutMetadata();
        legacy.putString("Recipe", retired);
        legacy.remove("WorkRecipe");
        legacy.putDouble("Progress", paid);
        bench.load(binary(legacy));
        nativeHeldBenchState(bench, retired, paid, fuel);
        bench.serverTick();
        nativeHeldBenchState(bench, retired, paid, fuel);
        CompoundTag conserved = bench.saveWithoutMetadata();
        Object inventory = bench.menuContainer();
        Object energy = bench.energy();
        Object fluids = bench.nativeStore();
        Object config = bench.containerConfig();
        Method matching = ProcessMachineBlockEntity.class.getDeclaredMethod("matchingRecipe", List.class);
        matching.setAccessible(true);
        Method work = ProcessMachineBlockEntity.class.getDeclaredMethod("work", long.class, List.class);
        work.setAccessible(true);
        long tick = level.getGameTime() + 1;
        for (int reopen = 0; reopen < 3; reopen++) {
            check(ProcessLookup.find(MachineFamily.ASSEMBLY_BENCH, physical).orElseThrow().id().equals(permitted.id()),
                    "A genuine current competitor still matches the actual physical bill");
            CompoundTag beforeReads = bench.saveWithoutMetadata();
            check(bench.inspectionRecipe().isEmpty()
                            && ((Optional<?>) matching.invoke(bench, List.<FluidInsert.Source>of())).isEmpty(),
                    "Actual Bench inspection/shared matching cannot expose or adopt that competitor");
            check(bench.saveWithoutMetadata().equals(beforeReads),
                    "Actual native read-only matching/inspection cannot change raw ownership or stores");
            work.invoke(bench, tick++, List.<FluidInsert.Source>of());
            bench.serverTick();
            nativeHeldBenchState(bench, retired, paid, fuel);
            check(bench.saveWithoutMetadata().equals(conserved),
                    "Native paid Bench ticks preserve the complete saved state, not just Progress");
            if (reopen == 0) {
                ProcessLookup.Generation previous = ProcessLookup.generation();
                ProcessLookup.rebuild(MaterialRegistry.snapshot());
                check(previous != ProcessLookup.generation(), "Actual native material reindex replaces the generation");
                check(bench.inspectionRecipe().isEmpty(), "Generation changes cannot expose a stale Bench recipe");
            }
            bench.load(binary(bench.saveWithoutMetadata()));
            nativeHeldBenchState(bench, retired, paid, fuel);
            check(bench.saveWithoutMetadata().equals(conserved)
                            && bench.menuContainer() == inventory && bench.energy() == energy
                            && bench.nativeStore() == fluids && bench.containerConfig() == config,
                    "Native binary reopen conserves paid work, stores and exposed store identities");
        }
        // Keep this held diagnostic Bench and its real inputs/paid NBT in the disposable world for save-stop evidence.
        // This is not a disk/chunk restart, actual client transport/rendering, Survival or owner-world migration pass.
    }

    private void nativeHeldBenchState(ProcessMachineBlockEntity bench, String retired, double paid, long fuel) {
        CompoundTag raw = bench.saveWithoutMetadata();
        check(raw.getString("WorkRecipe").equals(retired)
                        && Double.doubleToRawLongBits(raw.getDouble("Progress")) == Double.doubleToRawLongBits(paid),
                "Actual legacy Bench owner and exact positive paid amount remain retained");
        check(bench.energy().getStored() == fuel && bench.getItem(bench.menuKind().inputs()).isEmpty(),
                "Unavailable native Bench work cannot draw Flux or invent a completed output");
        check(bench.menuStatus() == MachineStatus.OUT_OF_BAND
                        && bench.menuData().get(ProcessMachineMenu.DATA_STATUS) == MachineStatus.OUT_OF_BAND.ordinal()
                        && bench.menuFault().equals(Component.translatable("gui.grindless.bench.retained_work",
                        retired, Double.toString(paid))),
                "The actual menu data/fault path reports retained unavailable work, never success-shaped idle");
        check(!raw.contains(AssemblerWork.TARGET) && !raw.contains(AssemblerWork.SUSPENDED),
                "Native Bench preservation adds no Assembler selection, job ledger or save schema");
    }

    private record MenuProbe(ServerPlayer player, ProcessMachineMenu menu) {
    }

    /** Harness materials and Flux prove the selector/transaction, not a no-command Survival supply route. */
    private MenuProbe explicitSelection(ServerLevel level, ProcessMachineBlockEntity machine)
            throws ReflectiveOperationException, IOException {
        machine.clearContent();
        check(machine.network() == null, "This isolated high fixture must not draw another diagnostic network");
        ProcessRecipe blueprint = recipe(BLUEPRINT);
        ProcessRecipe planner = recipe(PLANNER);
        check(blueprint.inputs().equals(planner.inputs()), "Use two real unchanged same-bill permitted handheld tools");
        check(blueprint.itemOutputs().get(0).id().equals(FabricationLogic.BLUEPRINT_TOOL)
                        && planner.itemOutputs().get(0).id().equals(FabricationLogic.DECONSTRUCTION_PLANNER),
                "The colliding rows produce genuine stock-consuming tools, never complete normal machine blocks");
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000018501"), "AssemblerProbe"));
        player.setPos(POSITION.getX() + 0.5, POSITION.getY() + 0.5, POSITION.getZ() + 0.5);
        ProcessMachineMenu menu = new ProcessMachineMenu(85, player.getInventory(), machine);
        player.containerMenu = menu;
        long token = menu.synchronizedOpeningToken();
        check(token != 0L, "An actual server menu has its own nonzero opening token");
        int revision = selectionRevision(menu);
        CompoundTag unchanged = machine.saveWithoutMetadata();
        check(menu.selectRecipe(player, menu.containerId + 1, token, revision, blueprint.id()) == SelectionResult.CLOSED,
                "A wrong actual container ID refuses");
        check(menu.selectRecipe(player, menu.containerId, token + 1, revision, blueprint.id()) == SelectionResult.STALE,
                "A foreign actual opening refuses");
        check(menu.selectRecipe(player, menu.containerId, token, revision - 1, blueprint.id()) == SelectionResult.STALE,
                "A stale actual catalogue revision refuses");
        check(menu.selectRecipe(player, menu.containerId, token, revision, "removed/request") == SelectionResult.INVALID_RECIPE,
                "An unknown current ID cannot become a new selected configuration");
        check(menu.selectRecipe(player, menu.containerId, token, revision, "electrolysis/water")
                        == SelectionResult.INVALID_RECIPE,
                "A real recipe of another family cannot become an Assembler target");
        for (String retired : List.of(RETIRED, "assemble/press")) {
            check(ProcessLookup.byId(MachineFamily.ASSEMBLER, retired).isEmpty(),
                    "The actual native current catalogue has retired normal-machine target " + retired);
            check(menu.selectRecipe(player, menu.containerId, token, revision, retired) == SelectionResult.INVALID_RECIPE,
                    "An explicitly requested retired normal target refuses rather than falling back");
        }
        player.getAbilities().mayBuild = false;
        check(menu.selectRecipe(player, menu.containerId, token, revision, blueprint.id()) == SelectionResult.DENIED,
                "Actual player build permission is enforced before changing the machine");
        player.getAbilities().mayBuild = true;
        check(machine.saveWithoutMetadata().equals(unchanged), "All invalid/unauthorized requests preserve exact state");
        menuSynchronization(level, machine, menu);
        ServerPlayer other = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000018502"), "OtherAssembler"));
        other.setPos(player.getX(), player.getY(), player.getZ());
        other.containerMenu = menu;
        check(menu.selectRecipe(other, menu.containerId, token, revision, blueprint.id()) == SelectionResult.CLOSED,
                "Another physical player cannot reuse the current menu");
        other.containerMenu = other.inventoryMenu;

        Object items = machine.menuContainer();
        Object energy = machine.energy();
        Object fluids = machine.nativeStore();
        Object config = machine.containerConfig();
        long tick = level.getGameTime() + 1;
        Method work = ProcessMachineBlockEntity.class.getDeclaredMethod("work", long.class, List.class);
        work.setAccessible(true);
        tick = retiredWork(machine, player, menu, blueprint, planner, work, tick);
        ItemStack[] collision = actualInputs(blueprint);
        ProcessRecipe expectedAuto = ProcessLookup.find(MachineFamily.ASSEMBLER, collision).orElseThrow();
        check(ProcessLookup.matches(planner, collision)
                        && List.of(blueprint, planner).stream().anyMatch(row -> !row.id().equals(expectedAuto.id())),
                "A real permitted collision discriminates exact selection from actual AUTO=" + expectedAuto.id());
        for (ProcessRecipe selected : List.of(blueprint, planner)) {
            choose(player, menu, selected.id(), SelectionResult.SELECTED);
            ItemStack[] physical = actualInputs(selected);
            ProcessRecipe auto = ProcessLookup.find(MachineFamily.ASSEMBLER, physical).orElseThrow();
            check(auto.id().equals(expectedAuto.id())
                            && ProcessLookup.findSelected(MachineFamily.ASSEMBLER, selected.id(), physical,
                            io.github.ezequiel24123z.grindless.fluid.FluidState.EMPTY, List.of()).orElseThrow() == selected,
                    "Exact stable-ID matching is independent of the lookup-derived AUTO choice");
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
        choose(player, menu, blueprint.id(), SelectionResult.SELECTED);
        supply(machine, actualInputs(blueprint));
        CompoundTag saved = machine.saveWithoutMetadata();
        saved.putDouble("Progress", blueprint.durationTicks() - 0.75);
        CompoundTag speed = new CompoundTag();
        speed.putInt("SPEED", 1);
        saved.put("Upgrades", speed);
        machine.load(binary(saved));
        machine.energy().setStored(blueprint.fuPerTick());
        work.invoke(machine, tick++, List.<FluidInsert.Source>of());
        check(machine.getItem(4).is(BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(
                        blueprint.itemOutputs().get(0).id()))) && machine.inspectionProgress() == 1.25,
                "An actual upgraded registered cycle retains fractional carry after successful settlement");
        machine.setItem(4, ItemStack.EMPTY);
        choose(player, menu, planner.id(), SelectionResult.SELECTED);
        saved = machine.saveWithoutMetadata();
        saved.putDouble("Progress", 23.375);
        machine.load(binary(saved));
        for (int reopen = 0; reopen < 8; reopen++) {
            choose(player, menu, blueprint.id(), SelectionResult.SELECTED);
            check(machine.inspectionProgress() == 1.25, "Actual switching restores the exact fractional Blueprint carry");
            choose(player, menu, planner.id(), SelectionResult.SELECTED);
            check(machine.inspectionProgress() == 23.375, "Actual switching restores the exact partial Planner work");
            machine.load(binary(machine.saveWithoutMetadata()));
            check(machine.inspectionProgress() == 23.375, "Actual binary reopen preserves the active paid balance");
            check(machine.menuContainer() == items && machine.energy() == energy
                            && machine.nativeStore() == fluids && machine.containerConfig() == config,
                    "Actual reopening preserves exposed inventory/Flux/fluid/config object identities");
        }

        choose(player, menu, blueprint.id(), SelectionResult.SELECTED);
        supply(machine, actualInputs(blueprint));
        machine.setItem(4, new ItemStack(Items.STONE, 64));
        saved = machine.saveWithoutMetadata();
        saved.putDouble("Progress", blueprint.durationTicks() + 0.25);
        machine.load(binary(saved));
        machine.energy().setStored(64L);
        for (int blocked = 0; blocked < 3; blocked++) {
            work.invoke(machine, tick++, List.<FluidInsert.Source>of());
        }
        check(machine.inspectionProgress() == blueprint.durationTicks() + 0.25 && machine.energy().getStored() == 64L,
                "Completed native blocked work neither redraws Flux nor loses its fractional overrun");
        check(machine.recipeSelection().orElseThrow().issue() == SelectionIssue.OUTPUT_BLOCKED,
                "The native selected-target diagnosis reports blocked output");
        choose(player, menu, planner.id(), SelectionResult.SELECTED);
        choose(player, menu, blueprint.id(), SelectionResult.SELECTED);
        check(machine.inspectionProgress() == blueprint.durationTicks() + 0.25,
                "Completed blocked native work remains reachable after switching");
        machine.setItem(4, ItemStack.EMPTY);
        work.invoke(machine, tick++, List.<FluidInsert.Source>of());
        check(machine.energy().getStored() == 64L && machine.inspectionProgress() == 0.25
                        && machine.getItem(4).getCount() == 1,
                "Native unblocking settles once without charging completed work again");
        machine.setItem(4, ItemStack.EMPTY);

        supply(machine, actualInputs(blueprint));
        saved = machine.saveWithoutMetadata();
        saved.putDouble("Progress", blueprint.durationTicks() + 0.375);
        machine.load(binary(saved));
        CompoundTag beforeSettlement = machine.saveWithoutMetadata();
        boolean[] aborted = {false};
        ((SimpleContainer) machine.menuContainer()).addListener(container -> {
            if (!aborted[0] && machine.getItem(0).isEmpty()) {
                aborted[0] = true;
                CompoundTag tentative = machine.saveWithoutMetadata();
                check(menu.selectRecipe(player, menu.containerId, token, currentRevision(menu), planner.id())
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
        check(menu.selectRecipe(player, menu.containerId, token, oldRevision, planner.id()) == SelectionResult.STALE,
                "A native catalogue reload refuses the old menu revision without guessing a new output");
        check(machine.saveWithoutMetadata().equals(beforeReloadRequest), "Reload refusal preserves all paid state");
        choose(player, menu, planner.id(), SelectionResult.SELECTED);
        saved = machine.saveWithoutMetadata();
        saved.putString(AssemblerWork.TARGET, "removed/native_saved_target");
        saved.putString("WorkRecipe", "removed/native_paid_owner");
        saved.putDouble("Progress", 0.125);
        machine.load(binary(saved));
        check(machine.recipeSelection().orElseThrow().issue() == SelectionIssue.UNAVAILABLE
                        && machine.saveWithoutMetadata().getString(AssemblerWork.TARGET).equals("removed/native_saved_target"),
                "A saved unknown native target stays explicitly unavailable rather than resetting to AUTO");
        choose(player, menu, blueprint.id(), SelectionResult.SELECTED);
        check(machine.recipeSelection().orElseThrow().suspended().get("removed/native_paid_owner") == 0.125,
                "Unknown native paid work remains preserved and cannot be spent by a known target");
        choose(player, menu, "", SelectionResult.SELECTED);
        check(machine.saveWithoutMetadata().getString(AssemblerWork.TARGET).isEmpty(),
                "The actual server menu can deliberately return to legacy AUTO");
        check(ProcessLookup.findSelected(MachineFamily.ASSEMBLER, RETIRED, actualInputs(recipe(BLUEPRINT)),
                        machine.contents(), List.of()).isEmpty()
                        && machine.recipeSelection().orElseThrow().suspended().get(RETIRED) == 1600.375,
                "Later current catalogue and menu transitions cannot spend or erase the retired balance");

        player.containerMenu = player.inventoryMenu;
        check(menu.selectRecipe(player, menu.containerId, token, currentRevision(menu), blueprint.id()) == SelectionResult.CLOSED,
                "A closed actual menu cannot change the machine");
        ProcessMachineMenu reopened = new ProcessMachineMenu(86, player.getInventory(), machine);
        player.containerMenu = reopened;
        check(reopened.synchronizedOpeningToken() != token, "Reopening binds a new actual menu identity");
        check(reopened.selectRecipe(player, reopened.containerId, token, selectionRevision(reopened), blueprint.id())
                        == SelectionResult.STALE,
                "A prior opening's token cannot select through a reopened menu");
        choose(player, reopened, blueprint.id(), SelectionResult.SELECTED);
        return new MenuProbe(player, reopened);
    }

    private long retiredWork(ProcessMachineBlockEntity machine, ServerPlayer player, ProcessMachineMenu menu,
                             ProcessRecipe blueprint, ProcessRecipe planner, Method work, long tick)
            throws ReflectiveOperationException, IOException {
        supply(machine, actualInputs(blueprint));
        machine.energy().setStored(64L);
        CompoundTag saved = machine.saveWithoutMetadata();
        saved.putString("Recipe", RETIRED);
        saved.putString("WorkRecipe", RETIRED);
        saved.putString(AssemblerWork.TARGET, RETIRED);
        saved.putDouble("Progress", 1600.375);
        machine.load(binary(saved));
        var physical = saved.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND).copy();
        Method resolve = ProcessMachineBlockEntity.class.getDeclaredMethod("recipe", List.class);
        resolve.setAccessible(true);
        for (int reopen = 0; reopen < 3; reopen++) {
            check(resolve.invoke(machine, List.<FluidInsert.Source>of()) == null
                            && machine.recipeSelection().orElseThrow().issue() == SelectionIssue.UNAVAILABLE,
                    "A saved retired normal target is unavailable with no permitted same-bill fallback");
            work.invoke(machine, tick++, List.<FluidInsert.Source>of());
            CompoundTag raw = machine.saveWithoutMetadata();
            check(raw.getString(AssemblerWork.TARGET).equals(RETIRED)
                            && raw.getString("WorkRecipe").equals(RETIRED)
                            && Double.doubleToRawLongBits(raw.getDouble("Progress"))
                            == Double.doubleToRawLongBits(1600.375),
                    "Native load/work retains the exact raw retired owner, target and fractional paid balance");
            check(machine.energy().getStored() == 64L && machine.getItem(4).isEmpty()
                            && raw.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND).equals(physical),
                    "Retired paid work cannot draw, complete, refund or mutate physical supplies");
            machine.load(binary(raw));
        }
        for (ProcessRecipe permitted : List.of(blueprint, planner)) {
            choose(player, menu, permitted.id(), SelectionResult.SELECTED);
            check(machine.inspectionProgress() == 0.0
                            && machine.recipeSelection().orElseThrow().suspended().get(RETIRED) == 1600.375,
                    "Each permitted native target starts with its own zero credit, retaining the unspendable old job");
        }
        choose(player, menu, "", SelectionResult.SELECTED);
        ProcessRecipe expectedAuto = ProcessLookup.find(MachineFamily.ASSEMBLER, actualInputs(blueprint)).orElseThrow();
        ProcessRecipe actualAuto = (ProcessRecipe) resolve.invoke(machine, List.<FluidInsert.Source>of());
        check(actualAuto != null && actualAuto.id().equals(expectedAuto.id()) && machine.inspectionProgress() == 0.0
                        && machine.recipeSelection().orElseThrow().suspended().get(RETIRED) == 1600.375,
                "Legacy AUTO derives its actual live choice without borrowing retired credit: " + expectedAuto.id());
        return tick;
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
        Set<String> currentIds = catalogue.stream().map(ProcessMachineMenu.RecipeChoice::id)
                .collect(java.util.stream.Collectors.toSet());
        check(currentIds.containsAll(Set.of(BLUEPRINT, PLANNER))
                        && !currentIds.contains(RETIRED) && !currentIds.contains("assemble/press"),
                "The actual server catalogue advertises both permitted tools and neither retired normal target");
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
