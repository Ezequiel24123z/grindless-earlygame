package io.github.ezequiel24123z.grindless.item;

import com.mojang.authlib.GameProfile;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One opt-in native capture/stamp and settlement proof, not client or campaign acceptance.
 * Fixtures stay in the harness's fresh diagnostic world; no alternate placement engine is used.
 */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class BlueprintInteractionChecks {

    private static final BlockPos ORIGIN = new BlockPos(1602, 300, 1602);
    private static final BlockPos STAMP = ORIGIN.south(2);
    private static final BlockPos MISSING = ORIGIN.south(4);
    private static final BlockPos BLOCKED = ORIGIN.south(6);
    private static final BlockPos PERMISSION = ORIGIN.south(8);
    private static final BlockPos INVALID = ORIGIN.south(10);
    private static final BlockPos KNOWN_FALSE = ORIGIN.east(6).south(2);
    private static final BlockPos UNLOADED = new BlockPos(20_000_000, 300, 20_000_000);
    private static final GameProfile PROFILE = new GameProfile(
            UUID.fromString("00000000-0000-0000-0000-425052494e54"), "[BlueprintCheck]");
    private static CommandSourceStack pending;
    private static boolean started;

    private final ServerLevel level;
    private FakePlayer player;
    private int assertions;

    private BlueprintInteractionChecks(ServerLevel level) {
        this.level = level;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("grindless_blueprint_checks")
                .requires(source -> source.getEntity() == null && source.hasPermission(4))
                .executes(context -> start(context.getSource())));
    }

    private static int start(CommandSourceStack source) {
        require(source.getEntity() == null && source.hasPermission(4), "Console permission 4 required");
        require(source.getServer().isSameThread(), "Use the actual server thread");
        require("smoke-world".equals(source.getServer().getWorldData().getLevelName()),
                "Use only the harness's disposable smoke-world");
        require(source.getServer().getPlayerList().getPlayers().isEmpty(), "No connected players may be present");
        require(!started && pending == null, "The blueprint fixture cannot be retried in the same process");
        started = true;
        pending = source;
        return 1;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        CommandSourceStack request = pending;
        if (event.phase != TickEvent.Phase.END || request == null
                || request.getServer() != event.getServer() || !request.getLevel().hasChunkAt(ORIGIN)) {
            return;
        }
        pending = null;
        BlueprintInteractionChecks checks = new BlueprintInteractionChecks(request.getLevel());
        try {
            checks.run();
        } catch (RuntimeException failure) {
            Grindless.LOG.error("GRINDLESS_BLUEPRINT_NATIVE_FAIL assertions={}", checks.assertions, failure);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        if (pending != null && pending.getServer() == event.getServer()) {
            pending = null;
        }
    }

    private void run() {
        check("server-only, synchronous probe", !level.isClientSide() && level.getServer().isSameThread());
        check("actual server Overworld is used", level == level.getServer().overworld());
        check("no player joined while the fixture awaited loading",
                level.getServer().getPlayerList().getPlayers().isEmpty());
        check("private profile is not an online player",
                level.getServer().getPlayerList().getPlayer(PROFILE.getId()) == null);
        check("private profile remains non-OP", !level.getServer().getPlayerList().isOp(PROFILE));
        for (BlockPos position : BlockPos.betweenClosed(ORIGIN.offset(0, -1, 0), ORIGIN.offset(12, 0, 12))) {
            check("fixture cells must already be loaded", level.hasChunkAt(position));
            check("fixture cells must be within build bounds and border",
                    level.isInWorldBounds(position) && level.getWorldBorder().isWithinBounds(position));
            check("never overwrite an existing fixture cell",
                    level.getBlockState(position).isAir() && level.getBlockEntity(position) == null);
        }
        player = FakePlayerFactory.get(level, PROFILE);
        check("private player must have a fresh inventory", player.getInventory().isEmpty());
        player.setPos(ORIGIN.getX() + 0.5, ORIGIN.getY(), ORIGIN.getZ() + 0.5);
        for (BlockPos anchor : List.of(ORIGIN, STAMP, MISSING, BLOCKED, PERMISSION, INVALID, KNOWN_FALSE)) {
            putEmpty(anchor.below(), Blocks.DIRT.defaultBlockState());
            putEmpty(anchor.east().below(), anchor.equals(PERMISSION)
                    ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState());
        }
        putEmpty(ORIGIN, Blocks.STONE.defaultBlockState());
        putEmpty(ORIGIN.east(), Blocks.FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.LIT, true));
        FurnaceBlockEntity original = furnace(ORIGIN.east());
        original.setItem(0, new ItemStack(Items.DIAMOND, 7));
        CompoundTag sourceNbt = original.saveWithoutMetadata();

        ItemStack blueprint = capture();
        positive(blueprint, sourceNbt);
        missing(blueprint, sourceNbt);
        blocked(blueprint);
        permissions(blueprint);
        unloaded(blueprint);
        knownFalse();
        malformedAndLimits(blueprint);
        check("source inventory and state remain untouched", sourceNbt.equals(original.saveWithoutMetadata()));
        Grindless.LOG.info("GRINDLESS_BLUEPRINT_NATIVE_PASS blocks=2 capture=1 stamp=1 known_false=1"
                + " blocked=1 unloaded=1 permissions=1 codec=1 limits=1 assertions={}", assertions);
    }

    private ItemStack capture() {
        prepare(new ItemStack(ModItems.BLUEPRINT_TOOL.get()), GameType.SURVIVAL);
        check("actual registered capture item", player.getMainHandItem().getItem() instanceof BlueprintToolItem);
        check("non-OP source interaction is admitted", player.mayInteract(level, ORIGIN));
        check("actual first-corner interaction", use(ORIGIN) == InteractionResult.CONSUME);
        check("actual second-corner interaction", use(ORIGIN.east()) == InteractionResult.CONSUME);
        List<ItemStack> captured = player.getInventory().items.stream()
                .filter(stack -> stack.is(ModItems.BLUEPRINT.get())).toList();
        check("one real blueprint was delivered", captured.size() == 1 && captured.get(0).getCount() == 1);
        ItemStack blueprint = captured.get(0).copy();
        List<BlueprintLogic.Piece> expected = List.of(
                new BlueprintLogic.Piece(0, 0, 0, "minecraft:stone", "minecraft:stone", ""),
                new BlueprintLogic.Piece(1, 0, 0, "minecraft:furnace", "minecraft:furnace", "east"));
        check("actual capture keeps the legacy relative pieces", BlueprintItem.read(blueprint).equals(expected));
        ListTag blocks = blueprint.getTag().getList("Blocks", Tag.TAG_COMPOUND);
        check("stone whitelist is unchanged", blocks.getCompound(0).getAllKeys()
                .equals(Set.of("X", "Y", "Z", "Id", "Item")));
        check("machine whitelist contains facing, never its inventory/status", blocks.getCompound(1).getAllKeys()
                .equals(Set.of("X", "Y", "Z", "Id", "Item", "Facing")));
        ItemStack reloaded = ItemStack.of(blueprint.save(new CompoundTag()));
        check("native item-NBT reload preserves the old carrier", ItemStack.matches(blueprint, reloaded));
        check("native item-NBT reload preserves the old pieces", BlueprintItem.read(reloaded).equals(expected));
        return reloaded;
    }

    private void positive(ItemStack blueprint, CompoundTag sourceNbt) {
        prepare(blueprint, GameType.SURVIVAL);
        stockClean();
        ItemStack protectedStock = protectedFurnace(sourceNbt);
        player.getInventory().items.set(1, protectedStock);
        ItemStack beforeProtected = protectedStock.copy();
        ItemStack beforeClean = player.getInventory().items.get(4).copy();
        ListTag beforeInventory = inventory();
        CompoundTag beforePayload = player.getMainHandItem().getTag().copy();
        BlueprintLogic.Plan diagnosis = plan(STAMP);
        check("real native preflight is ready", diagnosis.ready());
        check("real native preflight retains the entire material bill",
                diagnosis.required().equals(Map.of("minecraft:stone", 1, "minecraft:furnace", 1)));
        check("preflight does not mutate native inventory", beforeInventory.equals(inventory()));
        check("preflight does not mutate blueprint NBT", beforePayload.equals(player.getMainHandItem().getTag()));
        air(STAMP);
        air(STAMP.east());
        check("actual registered stamp interaction", use(STAMP.below()) == InteractionResult.CONSUME);
        check("actual first block is placed", cell(STAMP).is(Blocks.STONE));
        check("actual facing is retained", cell(STAMP.east()).is(Blocks.FURNACE)
                && cell(STAMP.east()).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST);
        check("live machine status is not cloned", !cell(STAMP.east()).getValue(BlockStateProperties.LIT));
        FurnaceBlockEntity stamped = furnace(STAMP.east());
        for (int slot = 0; slot < stamped.getContainerSize(); slot++) {
            check("source inventory is never cloned", stamped.getItem(slot).isEmpty());
        }
        beforeClean.setCount(2);
        check("exact clean main-inventory payment", ItemStack.matches(beforeClean, player.getInventory().items.get(4)));
        check("exact offhand payment", ItemStack.matches(new ItemStack(Items.STONE),
                player.getOffhandItem()));
        check("worn slots are not building material", player.getInventory().armor.get(0).getCount() == 10);
        check("relocated BE stock remains untouched in a real stamp",
                ItemStack.matches(beforeProtected, player.getInventory().items.get(1)));
        check("stamp does not change its payload", beforePayload.equals(player.getMainHandItem().getTag()));
    }

    private void missing(ItemStack blueprint, CompoundTag sourceNbt) {
        prepare(blueprint, GameType.SURVIVAL);
        player.getInventory().items.set(1, protectedFurnace(sourceNbt));
        player.getInventory().offhand.set(0, new ItemStack(Items.STONE, 2));
        ListTag before = inventory();
        check("stored BE data is not eligible construction stock",
                plan(MISSING).missing().equals(Map.of("minecraft:furnace", 1)));
        check("actual missing-stock stamp is refused", use(MISSING.below()) == InteractionResult.FAIL);
        check("missing-stock refusal leaves all native stacks/NBT intact", before.equals(inventory()));
        air(MISSING);
        air(MISSING.east());
    }

    private void blocked(ItemStack blueprint) {
        prepare(blueprint, GameType.SURVIVAL);
        stockClean();
        putEmpty(BLOCKED.east(), Blocks.DIRT.defaultBlockState());
        ListTag before = inventory();
        problem("actual blocked-cell diagnosis", plan(BLOCKED), BlueprintLogic.Failure.BLOCKED);
        check("actual blocked-cell stamp is refused", use(BLOCKED.below()) == InteractionResult.FAIL);
        check("blocked-cell refusal spends nothing", before.equals(inventory()));
        air(BLOCKED);
        check("blocked-cell refusal preserves the obstruction", cell(BLOCKED.east()).is(Blocks.DIRT));
    }

    private void permissions(ItemStack blueprint) {
        prepare(blueprint, GameType.ADVENTURE);
        stockClean();
        ListTag allowedSupport = new ListTag();
        allowedSupport.add(StringTag.valueOf("minecraft:dirt"));
        player.getMainHandItem().getOrCreateTag().put("CanPlaceOn", allowedSupport);
        check("adventure item permits the clicked anchor's dirt support",
                player.mayUseItemAt(PERMISSION, Direction.UP, player.getMainHandItem()));
        check("adventure item denies the later target's stone support",
                !player.mayUseItemAt(PERMISSION.east(), Direction.UP, player.getMainHandItem()));
        ListTag before = inventory();
        BlueprintLogic.Plan diagnosis = plan(PERMISSION);
        problem("actual permission diagnosis", diagnosis, BlueprintLogic.Failure.FORBIDDEN);
        check("permission is evaluated for the later target, not only the clicked anchor",
                diagnosis.problems().size() == 1 && diagnosis.problems().get(0).piece().x() == 1);
        check("actual permission refusal", use(PERMISSION.below()) == InteractionResult.FAIL);
        check("permission refusal spends nothing", before.equals(inventory()));
        air(PERMISSION);
        air(PERMISSION.east());
    }

    private void unloaded(ItemStack blueprint) {
        prepare(blueprint, GameType.SURVIVAL);
        stockClean();
        check("unloaded target is within the actual world bounds",
                level.isInWorldBounds(UNLOADED) && level.getWorldBorder().isWithinBounds(UNLOADED));
        check("unloaded target really is absent", !level.hasChunkAt(UNLOADED));
        ListTag before = inventory();
        problem("actual unloaded-cell diagnosis", plan(UNLOADED), BlueprintLogic.Failure.UNLOADED);
        check("actual unloaded-cell stamp is refused", use(UNLOADED.below()) == InteractionResult.FAIL);
        check("preflight and stamp never force the target chunk", !level.hasChunkAt(UNLOADED));
        check("unloaded-cell refusal spends nothing", before.equals(inventory()));
    }

    private void knownFalse() {
        putEmpty(KNOWN_FALSE.east(), Blocks.GRASS.defaultBlockState());
        check("the real failure cell is replaceable", cell(KNOWN_FALSE.east()).canBeReplaced());
        ItemStack blueprint = new ItemStack(ModItems.BLUEPRINT.get());
        BlueprintItem.write(blueprint, List.of(
                new BlueprintLogic.Piece(0, 0, 0, "minecraft:stone", "minecraft:stone", ""),
                new BlueprintLogic.Piece(1, 0, 0, "minecraft:grass", "minecraft:grass", "")));
        prepare(blueprint, GameType.SURVIVAL);
        ItemStack grass = new ItemStack(Items.GRASS, 2);
        grass.getOrCreateTag().putString("Marker", "native known-false refund");
        player.getInventory().items.set(4, grass);
        player.getInventory().offhand.set(0, new ItemStack(Items.STONE, 2));
        ItemStack beforeGrass = grass.copy();
        BlockState beforeCell = cell(KNOWN_FALSE.east());
        check("identical native setBlock explicitly returns false",
                !level.setBlock(KNOWN_FALSE.east(), beforeCell, 3));
        check("known-false preflight really admits both replaceable cells", plan(KNOWN_FALSE).ready());
        // The second real setBlock receives its existing grass state and returns false.
        // This crosses the same Item.useOn/stamp path, without a fake Level or injected writer.
        check("a real false placement reports failure", use(KNOWN_FALSE.below()) == InteractionResult.FAIL);
        check("the confirmed first placement is not falsely rolled back", cell(KNOWN_FALSE).is(Blocks.STONE));
        check("the failed second cell is unchanged", cell(KNOWN_FALSE.east()) == beforeCell);
        check("the second cell remains replaceable, not an ordinary obstruction",
                cell(KNOWN_FALSE.east()).canBeReplaced());
        check("only the confirmed placement is paid", player.getOffhandItem().getCount() == 1);
        check("the false placement refunds exact original native NBT",
                ItemStack.matches(beforeGrass, player.getInventory().items.get(4)));
    }

    private void malformedAndLimits(ItemStack blueprint) {
        prepare(blueprint, GameType.SURVIVAL);
        stockClean();
        player.getMainHandItem().getTag().getList("Blocks", Tag.TAG_COMPOUND).getCompound(0).remove("X");
        ListTag malformed = inventory();
        problem("typed legacy fields cannot be defaulted", plan(INVALID), BlueprintLogic.Failure.MALFORMED);
        check("malformed native item refuses interaction", use(INVALID.below()) == InteractionResult.FAIL);
        check("malformed native item spends nothing", malformed.equals(inventory()));

        List<BlueprintLogic.Piece> excessive = new ArrayList<>();
        for (int index = 0; index < 513; index++) {
            excessive.add(new BlueprintLogic.Piece(index % 32, 0, index / 32,
                    "minecraft:stone", "minecraft:stone", ""));
        }
        ItemStack oversized = new ItemStack(ModItems.BLUEPRINT.get());
        BlueprintItem.write(oversized, excessive);
        prepare(oversized, GameType.SURVIVAL);
        stockClean();
        ListTag before = inventory();
        problem("registered carrier retains the 512-part bound", plan(INVALID), BlueprintLogic.Failure.TOO_LARGE);
        check("oversized native item refuses interaction", use(INVALID.below()) == InteractionResult.FAIL);
        check("oversized native item spends nothing", before.equals(inventory()));
        air(INVALID);
        air(INVALID.east());

        prepare(new ItemStack(ModItems.BLUEPRINT_TOOL.get()), GameType.SURVIVAL);
        check("legacy constants are unchanged", BlueprintLogic.MAX_EDGE == 32 && BlueprintLogic.MAX_PIECES == 512);
        check("capture first corner for edge refusal", use(ORIGIN) == InteractionResult.CONSUME);
        BlockPos outsideBox = ORIGIN.east(32);
        boolean loadedBefore = level.hasChunkAt(outsideBox);
        check("actual capture refuses a 33-block edge", use(outsideBox) == InteractionResult.FAIL);
        check("edge refusal does not load the far corner", level.hasChunkAt(outsideBox) == loadedBefore);
        check("edge refusal creates no blueprint",
                player.getInventory().items.stream().noneMatch(stack -> stack.is(ModItems.BLUEPRINT.get())));
    }

    private void prepare(ItemStack held, GameType mode) {
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setGameMode(mode);
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, held.copy());
        check("real game mode applies without granting OP", player.gameMode.getGameModeForPlayer() == mode
                && !level.getServer().getPlayerList().isOp(PROFILE));
    }

    private void stockClean() {
        ItemStack furnace = new ItemStack(Items.FURNACE, 3);
        furnace.getOrCreateTag().putString("Marker", "clean native construction stock");
        player.getInventory().items.set(4, furnace);
        player.getInventory().offhand.set(0, new ItemStack(Items.STONE, 2));
        player.getInventory().armor.set(0, new ItemStack(Items.STONE, 10));
    }

    private static ItemStack protectedFurnace(CompoundTag sourceNbt) {
        ItemStack stack = new ItemStack(Items.FURNACE);
        stack.getOrCreateTag().put(BlockItem.BLOCK_ENTITY_TAG, sourceNbt.copy());
        return stack;
    }

    private UseOnContext context(BlockPos clicked) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false));
    }

    private InteractionResult use(BlockPos clicked) {
        return player.getMainHandItem().getItem().useOn(context(clicked));
    }

    private BlueprintLogic.Plan plan(BlockPos anchor) {
        UseOnContext context = context(anchor.below());
        return BlueprintItem.preflight(BlueprintItem.decode(player.getMainHandItem()), anchor,
                BlueprintItem.view(context), BlueprintItem.contents(player.getInventory()),
                player.getAbilities().instabuild).diagnosis();
    }

    private ListTag inventory() {
        return player.getInventory().save(new ListTag());
    }

    private BlockState cell(BlockPos position) {
        check("never read a fixture chunk unless loaded", level.hasChunkAt(position));
        return level.getBlockState(position);
    }

    private void air(BlockPos position) {
        check("refused/preflight cells stay air", cell(position).isAir());
    }

    private FurnaceBlockEntity furnace(BlockPos position) {
        check("native furnace chunk remains loaded", level.hasChunkAt(position));
        check("native furnace entity exists", level.getBlockEntity(position) instanceof FurnaceBlockEntity);
        return (FurnaceBlockEntity) level.getBlockEntity(position);
    }

    private void putEmpty(BlockPos position, BlockState state) {
        check("fixture setup refuses non-air cells", cell(position).isAir());
        check("actual fixture block placement succeeds", level.setBlock(position, state, 3));
    }

    private void problem(String label, BlueprintLogic.Plan plan, BlueprintLogic.Failure expected) {
        check(label, plan.problems().stream().anyMatch(problem -> problem.failure() == expected) && !plan.ready());
    }

    private void check(String label, boolean passed) {
        assertions++;
        require(passed, label);
    }

    private static void require(boolean passed, String label) {
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }
}
