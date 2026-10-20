package io.github.ezequiel24123z.grindless.fluid.forge;

import com.mojang.authlib.GameProfile;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.fluid.BasicTankBlockEntity;
import io.github.ezequiel24123z.grindless.fluid.ClayConduitBlockEntity;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidNbt;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.fluid.NativeFluidStore;
import io.github.ezequiel24123z.grindless.item.Relocation;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * Opt-in console probe for the existing workshop-ports smoke's disposable world.
 * Controlled stock is a mechanism fixture, not earned process work or player acceptance.
 */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID)
public final class WorkshopInterfaceChecks {

    // Private to this probe, not Forge's shared Minecraft fake player or an owner's profile.
    private static final GameProfile PROFILE = new GameProfile(
            UUID.fromString("00000000-0000-0000-0000-4752494e444c"), "[WorkshopChecks]");

    private WorkshopInterfaceChecks() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("grindless_workshop_checks")
                .requires(source -> source.getEntity() == null && source.hasPermission(4))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> run(context.getSource(),
                                BlockPosArgument.getLoadedBlockPos(context, "pos")))));
    }

    private static int run(CommandSourceStack source, BlockPos origin) {
        require(source.getEntity() == null && source.hasPermission(4), "Console permission 4 required");
        ServerLevel level = source.getLevel();
        BlockPos controllerPos = origin.offset(6, 0, -3);
        FakePlayer player = null;
        try {
            preflight(level, origin, controllerPos);
            require(level.getServer().getPlayerList().getPlayer(PROFILE.getId()) == null,
                    "Probe profile must not belong to an online player");
            require(!level.getServer().getPlayerList().isOp(PROFILE), "Probe profile must remain non-OP");
            player = FakePlayerFactory.get(level, PROFILE);
            player.setPos(origin.getX() + 2.5, origin.getY(), origin.getZ() + 0.5);
            player.setYRot(0.0F);
            require(player.mayInteract(level, origin) && player.mayInteract(level, origin.below()),
                    "Real prerequisite denied: non-OP probe mayInteract (check disposable-server protection)");

            place(level, origin.below(), Blocks.STONE.defaultBlockState());
            place(level, origin, ModBlocks.BASIC_TANK.get().defaultBlockState());
            BasicTankBlockEntity tank = entity(level, origin, BasicTankBlockEntity.class);
            buckets(level, player, tank);
            indivisibleBuckets(level, player, tank);
            fluidGuards(tank);
            creativeAndFullInventory(level, player, tank);
            dropAndReplace(level, player, tank);
            ports(level, controllerPos);
            Grindless.LOG.info("WORKSHOP-INTERFACES-COMPLETE");
            return 1;
        } catch (RuntimeException failure) {
            Grindless.LOG.error("Workshop interface checks failed at {}", origin, failure);
            throw failure;
        } finally {
            if (player != null) {
                // Only this dedicated probe's inventory is ever cleared. Leave failed world evidence.
                player.getInventory().clearContent();
            }
        }
    }

    private static void preflight(ServerLevel level, BlockPos origin, BlockPos controllerPos) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 1, 1))) {
            emptyLoaded(level, pos);
        }
        for (int behind = 0; behind <= 2; behind++) {
            emptyLoaded(level, controllerPos.offset(0, 0, behind));
            emptyLoaded(level, controllerPos.offset(0, 1, behind));
        }
        emptyLoaded(level, controllerPos.south().east());
        require(level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS),
                "Real prerequisite denied: doTileDrops must already be true");
        require(level.getEntitiesOfClass(ItemEntity.class, dropBox(origin)).isEmpty(),
                "Tank drop area already contains unowned items");
    }

    private static void emptyLoaded(ServerLevel level, BlockPos pos) {
        require(!level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos),
                "Reserved position must already be loaded and within build height: " + pos);
        require(level.getWorldBorder().isWithinBounds(pos), "Reserved position is outside the world border: " + pos);
        require(level.getBlockState(pos).isAir() && level.getBlockEntity(pos) == null,
                "Refusing to overwrite a reserved position that is not empty: " + pos);
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        emptyLoaded(level, pos);
        require(level.setBlock(pos, state, 3), "Could not create owned probe block: " + pos);
    }

    private static <T extends BlockEntity> T entity(ServerLevel level, BlockPos pos, Class<T> type) {
        require(level.hasChunkAt(pos), "Probe entity's chunk is no longer loaded: " + pos);
        BlockEntity entity = level.getBlockEntity(pos);
        require(type.isInstance(entity), "Expected registered " + type.getSimpleName() + " at " + pos);
        return type.cast(entity);
    }

    private static void prepare(FakePlayer player, ItemStack held, boolean creative) {
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        require(player.isCreative() == creative && player.getAbilities().instabuild == creative,
                "Real prerequisite denied: probe game-mode change did not take effect");
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
    }

    private static void useBucket(ServerLevel level, FakePlayer player, BasicTankBlockEntity tank) {
        BlockPos pos = tank.getBlockPos();
        require(level.hasChunkAt(pos) && level.getBlockEntity(pos) == tank && !tank.isRemoved(),
                "Bucket target is not the current registered tank");
        require(player.mayInteract(level, pos),
                "Real prerequisite denied: non-OP probe mayInteract at tank");
        IFluidHandlerItem item = FluidUtil.getFluidHandler(player.getItemInHand(InteractionHand.MAIN_HAND))
                .orElseThrow(() -> new IllegalStateException("Held bucket has no real Forge item fluid capability"));
        require(item.getTanks() == 1, "Expected the real one-tank bucket capability");
        // Deliberately crosses common Block.use and the packaged @ExpectPlatform Forge binder.
        InteractionResult result = level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
        require(result == InteractionResult.CONSUME, "Tank Block.use did not handle the Forge bucket");
    }

    private static void held(FakePlayer player, Item item, int count) {
        require(ItemStack.matches(player.getItemInHand(InteractionHand.MAIN_HAND), new ItemStack(item, count)),
                "Unexpected probe hand; expected " + count + " " + item);
    }

    private static void stock(BasicTankBlockEntity tank, FluidState state) {
        CompoundTag tag = tank.saveWithoutMetadata();
        tag.put("Fluid", FluidNbt.save(state));
        tank.load(tag);
        require(tank.contents().equals(state), "Controlled tank stock did not load exactly");
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static boolean isWater(FluidStack stack, int amount) {
        return !stack.isEmpty() && stack.getFluid() == Fluids.WATER && stack.getAmount() == amount
                && !stack.hasTag();
    }

    private static boolean isWater(FluidState state, int amount) {
        // Bucket imports use Forge's intrinsic temperature; exact native metadata is checked on the drop.
        return state.is(FluidLogic.WATER) && state.millibuckets() == amount;
    }

    private static IFluidHandler handler(BlockEntity entity, Direction side) {
        return entity.getCapability(ForgeCapabilities.FLUID_HANDLER, side)
                .orElseThrow(() -> new IllegalStateException("Missing real Forge fluid capability on " + side));
    }

    private static void buckets(ServerLevel level, FakePlayer player, BasicTankBlockEntity tank) {
        stock(tank, FluidState.EMPTY);
        prepare(player, new ItemStack(Items.WATER_BUCKET), false);
        useBucket(level, player, tank);
        require(isWater(tank.contents(), 1000), "Whole bucket did not fill 1000 mB");
        held(player, Items.BUCKET, 1);
        useBucket(level, player, tank);
        require(tank.contents().isEmpty(), "Empty bucket did not extract the whole 1000 mB");
        held(player, Items.WATER_BUCKET, 1);
        require(level.getEntitiesOfClass(ItemEntity.class, dropBox(tank.getBlockPos())).isEmpty(),
                "Bucket exchange must not drop items");
        Grindless.LOG.info("WORKSHOP-INTERFACES-BUCKETS-OK");
    }

    private static void indivisibleBuckets(ServerLevel level, FakePlayer player, BasicTankBlockEntity tank) {
        FluidState nearlyFull = FluidState.of(FluidLogic.WATER, 15500);
        stock(tank, nearlyFull);
        prepare(player, new ItemStack(Items.WATER_BUCKET), false);
        useBucket(level, player, tank);
        require(tank.contents().equals(nearlyFull), "500 mB room consumed an indivisible water bucket");
        held(player, Items.WATER_BUCKET, 1);
        FluidState partial = FluidState.of(FluidLogic.WATER, 500);
        stock(tank, partial);
        prepare(player, new ItemStack(Items.BUCKET), false);
        useBucket(level, player, tank);
        require(tank.contents().equals(partial), "An empty bucket consumed a partial 500 mB");
        held(player, Items.BUCKET, 1);
        Grindless.LOG.info("WORKSHOP-INTERFACES-WHOLE-BUCKETS-OK");
    }

    private static void fluidGuards(BasicTankBlockEntity tank) {
        stock(tank, FluidState.EMPTY);
        IFluidHandler capability = handler(tank, Direction.NORTH);
        require(capability.fill(water(1000), FluidAction.SIMULATE) == 1000 && tank.contents().isEmpty(),
                "SIMULATE fill mutated the actual tank or misreported acceptance");
        stock(tank, FluidState.of(FluidLogic.WATER, 1000));
        FluidState before = tank.contents();
        require(isWater(capability.drain(501, FluidAction.SIMULATE), 501)
                        && isWater(capability.drain(water(502), FluidAction.SIMULATE), 502)
                        && tank.contents().equals(before), "SIMULATE drain mutated the actual tank");
        for (int index : new int[]{Integer.MIN_VALUE, -1, 1, Integer.MAX_VALUE}) {
            require(capability.getFluidInTank(index).isEmpty() && capability.getTankCapacity(index) == 0
                            && !capability.isFluidValid(index, water(1)), "Invalid tank index was exposed: " + index);
        }
        require(capability.drain(0, FluidAction.EXECUTE).isEmpty()
                        && capability.drain(-1, FluidAction.EXECUTE).isEmpty()
                        && tank.contents().equals(before), "Non-positive drain changed stock");
        stock(tank, FluidState.EMPTY);
        require(capability.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE) == 0
                        && tank.contents().isEmpty(), "Ambient Basic Tank accepted hot lava");
        FluidStack tagged = water(1000);
        CompoundTag extra = new CompoundTag();
        extra.putString("WorkshopProbe", "must-not-be-lost");
        tagged.setTag(extra);
        require(capability.fill(tagged, FluidAction.EXECUTE) == 0 && !capability.isFluidValid(0, tagged)
                        && tank.contents().isEmpty(), "Tagged water was accepted without representable metadata");
        FluidState gas = FluidState.of(FluidLogic.CARBON_MONOXIDE, 1000);
        require(tank.insert(Direction.NORTH, gas).isEmpty() && tank.contents().equals(gas),
                "Native carbon monoxide fixture was not accepted");
        require(capability.getFluidInTank(0).isEmpty()
                        && capability.drain(1000, FluidAction.EXECUTE).isEmpty()
                        && capability.drain(water(1000), FluidAction.EXECUTE).isEmpty()
                        && tank.contents().equals(gas), "Unregistered native gas was consumed to an empty Forge result");
        Grindless.LOG.info("WORKSHOP-INTERFACES-FLUID-GUARDS-OK");
    }

    private static void creativeAndFullInventory(ServerLevel level, FakePlayer player, BasicTankBlockEntity tank) {
        stock(tank, FluidState.EMPTY);
        prepare(player, new ItemStack(Items.WATER_BUCKET), true);
        useBucket(level, player, tank);
        require(isWater(tank.contents(), 1000), "Creative input did not fill tank");
        held(player, Items.WATER_BUCKET, 1);
        prepare(player, new ItemStack(Items.BUCKET), true);
        useBucket(level, player, tank);
        require(tank.contents().isEmpty(), "Creative extraction did not empty tank");
        held(player, Items.WATER_BUCKET, 1);
        for (boolean creative : new boolean[]{false, true}) {
            FluidState before = FluidState.of(FluidLogic.WATER, 1000);
            stock(tank, before);
            prepare(player, new ItemStack(Items.BUCKET, 16), creative);
            for (int slot = 1; slot < player.getInventory().items.size(); slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            useBucket(level, player, tank);
            require(tank.contents().equals(before), "Full inventory consumed fluid; creative=" + creative);
            held(player, Items.BUCKET, 16);
            for (int slot = 1; slot < player.getInventory().items.size(); slot++) {
                require(ItemStack.matches(player.getInventory().getItem(slot), new ItemStack(Items.COBBLESTONE, 64)),
                        "Full-inventory refusal changed slot " + slot);
            }
            require(level.getEntitiesOfClass(ItemEntity.class, dropBox(tank.getBlockPos())).isEmpty(),
                    "Full-inventory refusal dropped a container");
        }
        Grindless.LOG.info("WORKSHOP-INTERFACES-CREATIVE-DELIVERY-OK");
    }

    private static AABB dropBox(BlockPos pos) {
        return new AABB(pos).inflate(1.0);
    }

    private static void retired(IFluidHandler capability) {
        require(capability.fill(water(1), FluidAction.EXECUTE) == 0
                        && capability.drain(1, FluidAction.EXECUTE).isEmpty()
                        && capability.drain(water(1), FluidAction.EXECUTE).isEmpty()
                        && capability.getFluidInTank(0).isEmpty()
                        && capability.getTankCapacity(0) == 0 && !capability.isFluidValid(0, water(1)),
                "Retained handler still exposes or mutates a retired owner");
    }

    private static void dropAndReplace(ServerLevel level, FakePlayer player, BasicTankBlockEntity tank) {
        prepare(player, ItemStack.EMPTY, false);
        FluidState saved = FluidState.of(FluidLogic.WATER, 1234, 31.5, 0.1);
        stock(tank, saved);
        BlockPos pos = tank.getBlockPos();
        NativeFluidStore oldStore = tank.nativeStore();
        require(oldStore != null, "Registered tank has no native store");
        LazyOptional<IFluidHandler> oldOptional = tank.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH);
        IFluidHandler oldHandler = oldOptional.orElseThrow(() -> new IllegalStateException("Missing old tank handler"));
        require(level.getBlockEntity(pos) == tank && level.setBlock(pos,
                        level.getBlockState(pos).setValue(MachineProperties.FACING, Direction.EAST), 3),
                "Could not rotate the owned tank using the world block-state path");
        require(level.getBlockEntity(pos) == tank && tank.contents().equals(saved)
                        && isWater(oldHandler.getFluidInTank(0), 1234), "Same-block rotation lost tank stock");
        require(level.destroyBlock(pos, true) && level.getBlockState(pos).isAir(), "Owned tank was not destroyed");
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, dropBox(pos));
        require(drops.size() == 1, "Expected exactly one local filled tank drop, got " + drops.size());
        ItemEntity dropped = drops.get(0);
        ItemStack actualDrop = dropped.getItem();
        CompoundTag blockTag = actualDrop.getTagElement(BlockItem.BLOCK_ENTITY_TAG);
        require(actualDrop.is(ModBlocks.BASIC_TANK.get().asItem()) && actualDrop.getCount() == 1
                        && blockTag != null && blockTag.getCompound("Fluid").equals(FluidNbt.save(saved)),
                "Actual dropped tank did not retain the exact Fluid metadata");
        require(!oldOptional.isPresent(), "Destroyed tank's LazyOptional was not invalidated");
        retired(oldHandler);
        require(oldStore.contents().equals(saved), "Retired tank handler changed the old native store");

        // Transfer the actual entity stack once, with no copied item remaining in the world.
        player.setItemInHand(InteractionHand.MAIN_HAND, actualDrop);
        dropped.setItem(ItemStack.EMPTY);
        dropped.discard();
        require(player.getItemInHand(InteractionHand.MAIN_HAND) == actualDrop && dropped.isRemoved()
                        && level.getEntitiesOfClass(ItemEntity.class, dropBox(pos)).isEmpty(),
                "Owned dropped stack was not transferred exclusively to the probe hand");
        require(actualDrop.getItem() instanceof BlockItem, "Dropped tank is not a real BlockItem");
        BlockPos support = pos.below();
        require(level.getBlockState(support).is(Blocks.STONE) && player.mayInteract(level, support),
                "Real prerequisite denied: probe cannot use its owned stone support");
        InteractionResult placed = ((BlockItem) actualDrop.getItem()).useOn(new UseOnContext(
                player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(support).add(0.0, 0.5, 0.0), Direction.UP, support, false)));
        require(placed.consumesAction() && player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "Actual dropped BlockItem placement failed or did not consume its single owned item");
        BasicTankBlockEntity replacement = entity(level, pos, BasicTankBlockEntity.class);
        require(replacement != tank && replacement.nativeStore() != oldStore && replacement.contents().equals(saved),
                "Actual item placement did not restore stock into a fresh native owner");
        retired(oldHandler);
        IFluidHandler fresh = handler(replacement, Direction.NORTH);
        require(isWater(fresh.getFluidInTank(0), 1234) && isWater(fresh.drain(1234, FluidAction.SIMULATE), 1234)
                        && replacement.contents().equals(saved) && oldStore.contents().equals(saved),
                "Fresh placed tank's capability is not independent of its retired predecessor");
        Grindless.LOG.info("WORKSHOP-INTERFACES-TANK-DROP-REPLACE-OK");
    }

    private static void ownerStock(ProcessMachineBlockEntity owner) {
        ownerStock(owner, 1750, 750);
    }

    private static void ownerStock(ProcessMachineBlockEntity owner, int amount, int quota) {
        owner.setItem(0, new ItemStack(Items.DIRT, 2));
        CompoundTag tag = owner.saveWithoutMetadata();
        tag.put("Fluid", FluidNbt.save(FluidState.of(FluidLogic.WATER, amount)));
        tag.putInt("FluidOutputQuota", quota);
        owner.load(tag);
        ownerStockIs(owner, amount, quota);
    }

    private static void ownerStockIs(ProcessMachineBlockEntity owner, int amount, int quota) {
        require(owner.contents().equals(FluidState.of(FluidLogic.WATER, amount))
                        && owner.saveWithoutMetadata().getInt("FluidOutputQuota") == quota
                        && ItemStack.matches(owner.getItem(0), new ItemStack(Items.DIRT, 2)),
                "Controlled controller stock, produced quota or two dirt inputs changed unexpectedly");
    }

    private static NativeFluidStore.Access access(WorkshopPortBlockEntity port, Direction face) {
        require(port.permitsFace(face), "Native fixture face is not currently allowed");
        long revision = port.capabilityRevision();
        NativeFluidStore store = port.nativeStore();
        require(store != null, "Actual formed port has no controller-backed native store");
        return new NativeFluidStore.Access(store,
                () -> port.permitsFace(face) && port.capabilityRevision() == revision && port.nativeStore() == store,
                () -> port.contents().millibuckets());
    }

    private static NativeFluidStore.Request request(int amount, NativeFluidStore.Access... accesses) {
        return new NativeFluidStore.Request(FluidLogic.WATER, amount, List.of(accesses));
    }

    private static void ports(ServerLevel level, BlockPos controllerPos) {
        BlockPos inputPos = controllerPos.south();
        BlockPos outputPos = controllerPos.south(2);
        place(level, controllerPos, ModBlocks.SETTLING_TROUGH.get().defaultBlockState()
                .setValue(MachineProperties.FACING, Direction.NORTH));
        place(level, inputPos, ModBlocks.SETTLING_FLUID_INPUT.get().defaultBlockState()
                .setValue(MachineProperties.FACING, Direction.NORTH));
        place(level, outputPos, ModBlocks.SETTLING_FLUID_OUTPUT.get().defaultBlockState()
                .setValue(MachineProperties.FACING, Direction.NORTH));
        ProcessMachineBlockEntity owner = entity(level, controllerPos, ProcessMachineBlockEntity.class);
        WorkshopPortBlockEntity input = entity(level, inputPos, WorkshopPortBlockEntity.class);
        WorkshopPortBlockEntity output = entity(level, outputPos, WorkshopPortBlockEntity.class);
        CompoundTag recovered = owner.saveWithoutMetadata();
        recovered.put("Fluid", FluidNbt.save(FluidState.of(FluidLogic.WATER, 750)));
        recovered.putInt("FluidOutputQuota", 750);
        owner.load(recovered);
        require(owner.isEmpty() && output.contents().equals(FluidState.of(FluidLogic.WATER, 750))
                        && isWater(handler(output, Direction.EAST).drain(751, FluidAction.SIMULATE), 750),
                "A completed batch with no remaining item inputs hid its actual 750 mB output");
        recovered.put("Fluid", FluidNbt.save(FluidState.of(FluidLogic.WATER, 1000)));
        owner.load(recovered);
        require(owner.isEmpty() && output.contents().equals(FluidState.of(FluidLogic.WATER, 750)),
                "The family's preload target was mistaken for a selected primary reservation");
        ownerStock(owner);
        require(owner.inspectionStructure().formed() && input.inspection().formed() && output.inspection().formed()
                        && controllerPos.equals(output.inspection().ownerPos()), "Actual trough geometry did not form");
        require(!output.getCapability(ForgeCapabilities.FLUID_HANDLER, null).isPresent()
                        && !output.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).isPresent()
                        && !output.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.EAST).isPresent(),
                "Output port exposed an unsided, forbidden-face or wrong-role capability");
        IFluidHandler east = handler(output, Direction.EAST);
        IFluidHandler west = handler(output, Direction.WEST);
        require(isWater(east.drain(751, FluidAction.SIMULATE), 750)
                        && isWater(west.drain(water(751), FluidAction.SIMULATE), 750)
                        && east.fill(water(1), FluidAction.EXECUTE) == 0
                        && handler(input, Direction.EAST).drain(1, FluidAction.EXECUTE).isEmpty(),
                "Actual sided Forge port direction or produced quota was bypassed");
        ownerStockIs(owner, 1750, 750);
        NativeFluidStore.Access eastAccess = access(output, Direction.EAST);
        NativeFluidStore.Access westAccess = access(output, Direction.WEST);
        require(eastAccess.store() == owner.nativeStore() && westAccess.store() == eastAccess.store(),
                "Actual port faces must share the actual controller store");
        require(NativeFluidStore.plan(List.of(request(751, eastAccess))).isEmpty(),
                "Native access exceeded the produced-only 750 mB quota");
        require(NativeFluidStore.plan(List.of(request(1500, eastAccess, westAccess))).isEmpty()
                        && NativeFluidStore.plan(List.of(request(750, eastAccess), request(750, westAccess))).isEmpty(),
                "Aliased faces or requests spent the same 750 mB quota twice");
        ownerStockIs(owner, 1750, 750);
        NativeFluidStore.Batch exact = NativeFluidStore.plan(List.of(request(750, eastAccess, westAccess)))
                .orElseThrow(() -> new IllegalStateException("Exact 750 mB native withdrawal was not planned"));
        require(exact.commit(), "Actual produced-only native withdrawal failed");
        ownerStockIs(owner, 1000, 0);
        require(output.contents().isEmpty() && east.drain(1, FluidAction.EXECUTE).isEmpty(),
                "Exhausted output port consumed reserved primary input");
        Grindless.LOG.info("WORKSHOP-INTERFACES-PORT-QUOTA-OK");

        ownerStock(owner);
        LazyOptional<IFluidHandler> retainedOptional = output.getCapability(
                ForgeCapabilities.FLUID_HANDLER, Direction.EAST);
        IFluidHandler retained = retainedOptional.orElseThrow(() -> new IllegalStateException("Missing retained port"));
        NativeFluidStore.Access retainedAccess = access(output, Direction.EAST);
        NativeFluidStore.Batch retainedBatch = NativeFluidStore.plan(List.of(request(750, retainedAccess)))
                .orElseThrow(() -> new IllegalStateException("Could not capture the original native batch"));
        CompoundTag saved = owner.saveWithoutMetadata();
        require(level.getBlockEntity(controllerPos) == owner, "Controller was replaced before the controlled removal");
        // Do not query the port during the gap: replacement identity, not an observed empty tick, must retire it.
        Relocation.run(() -> require(level.removeBlock(controllerPos, false), "Owned controller removal failed"));
        place(level, controllerPos, ModBlocks.SETTLING_TROUGH.get().defaultBlockState()
                .setValue(MachineProperties.FACING, Direction.NORTH));
        ProcessMachineBlockEntity replacement = entity(level, controllerPos, ProcessMachineBlockEntity.class);
        replacement.load(saved);
        require(owner.isRemoved() && replacement != owner && replacement.nativeStore() != retainedAccess.store()
                        && replacement.inspectionStructure().formed(), "Same-position controller was not a fresh owner");
        retired(retained);
        require(!retainedOptional.isPresent() && !retainedAccess.allowed().getAsBoolean()
                        && !retainedBatch.commit()
                        && NativeFluidStore.plan(List.of(request(750, retainedAccess))).isEmpty(),
                "Retained handler/access/batch rebound to a replacement controller");
        ownerStockIs(owner, 1750, 750);
        ownerStockIs(replacement, 1750, 750);
        LazyOptional<IFluidHandler> freshOptional = output.getCapability(
                ForgeCapabilities.FLUID_HANDLER, Direction.EAST);
        IFluidHandler fresh = freshOptional.orElseThrow(() -> new IllegalStateException("Missing replacement port"));
        require(fresh != retained && isWater(fresh.drain(751, FluidAction.EXECUTE), 750),
                "Fresh port capability did not exclusively drain the new owner's produced quota");
        ownerStockIs(replacement, 1000, 0);
        ownerStockIs(owner, 1750, 750);
        ownerStock(replacement);
        require(isWater(fresh.getFluidInTank(0), 750), "Port must have live quota before the setRemoved check");
        ProcessMachineBlockEntity current = callbackRebinding(level, controllerPos, input, output, replacement);
        ownerStock(current);
        freshOptional = output.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.EAST);
        fresh = freshOptional.orElseThrow(() -> new IllegalStateException("Missing post-callback port view"));
        require(isWater(fresh.getFluidInTank(0), 750), "Removal check needs the newest live produced quota");
        output.setRemoved();
        require(output.isRemoved() && !freshOptional.isPresent()
                        && !output.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.EAST).isPresent(),
                "Actual port setRemoved did not invalidate its capability");
        retired(fresh);
        require(fresh.getTanks() == 0, "Removed port still advertises accessible tanks");
        ownerStockIs(current, 1750, 750);
        Grindless.LOG.info("WORKSHOP-INTERFACES-RETAINED-OWNER-OK");
    }

    private static ProcessMachineBlockEntity callbackRebinding(ServerLevel level, BlockPos pos,
                                                               WorkshopPortBlockEntity input,
                                                               WorkshopPortBlockEntity output,
                                                               ProcessMachineBlockEntity original) {
        ownerStock(original, 0, 0);
        BlockPos sourcePos = input.getBlockPos().east();
        place(level, sourcePos, ModBlocks.CLAY_CONDUIT.get().defaultBlockState()
                .setValue(MachineProperties.FACING, Direction.WEST));
        ClayConduitBlockEntity source = entity(level, sourcePos, ClayConduitBlockEntity.class);
        FluidState pulse = FluidState.of(FluidLogic.WATER, 50);
        require(source.insert(Direction.EAST, pulse).isEmpty(), "Real source conduit did not take its controlled 50 mB");
        input.load(input.saveWithoutMetadata());
        ProcessMachineBlockEntity[] insertedOwner = {null};
        boolean[] insertArmed = {true};
        input.addCapabilityInvalidator(() -> {
            if (!insertArmed[0]) {
                return;
            }
            insertArmed[0] = false;
            require(input.insert(Direction.EAST, pulse).equals(pulse)
                            && !input.canInsert(Direction.EAST, pulse),
                    "Reentrant callback was allowed to operate before invalidation settled");
            insertedOwner[0] = replaceController(level, pos, original);
        });
        ClayConduitBlockEntity.tick(level, sourcePos, level.getBlockState(sourcePos), source);
        require(!insertArmed[0] && insertedOwner[0] != null && insertedOwner[0] != original
                        && source.contents().equals(pulse),
                "An owner-replacing callback consumed the real source conduit pulse");
        ownerStockIs(original, 0, 0);
        ownerStockIs(insertedOwner[0], 0, 0);
        ClayConduitBlockEntity.tick(level, sourcePos, level.getBlockState(sourcePos), source);
        require(source.contents().isEmpty(), "Fresh post-callback input did not accept the retained conduit pulse");
        ownerStockIs(insertedOwner[0], 50, 0);
        ownerStockIs(original, 0, 0);

        ProcessMachineBlockEntity oldOutputOwner = insertedOwner[0];
        ownerStock(oldOutputOwner);
        output.load(output.saveWithoutMetadata());
        ProcessMachineBlockEntity[] extractedOwner = {null};
        boolean[] extractArmed = {true};
        output.addCapabilityInvalidator(() -> {
            if (!extractArmed[0]) {
                return;
            }
            extractArmed[0] = false;
            require(output.extract(Direction.EAST, 50).isEmpty()
                            && !output.canExtract(Direction.EAST),
                    "Reentrant callback extracted before invalidation settled");
            extractedOwner[0] = replaceController(level, pos, oldOutputOwner);
        });
        require(output.extract(Direction.EAST, 50).isEmpty()
                        && !extractArmed[0] && extractedOwner[0] != null,
                "An operation retained its captured output owner across a synchronous replacement callback");
        ownerStockIs(oldOutputOwner, 1750, 750);
        ownerStockIs(extractedOwner[0], 1750, 750);
        require(output.extract(Direction.EAST, 50).equals(pulse),
                "Fresh output access did not bind exclusively to the post-callback owner");
        ownerStockIs(extractedOwner[0], 1700, 700);
        ownerStockIs(oldOutputOwner, 1750, 750);
        Grindless.LOG.info("WORKSHOP-INTERFACES-CALLBACK-REBIND-OK");
        return extractedOwner[0];
    }

    private static ProcessMachineBlockEntity replaceController(ServerLevel level, BlockPos pos,
                                                               ProcessMachineBlockEntity previous) {
        CompoundTag saved = previous.saveWithoutMetadata();
        require(level.hasChunkAt(pos) && level.getBlockEntity(pos) == previous && !previous.isRemoved(),
                "Callback replacement target is not this probe's current controller");
        Relocation.run(() -> require(level.removeBlock(pos, false), "Callback could not retire its owned controller"));
        place(level, pos, ModBlocks.SETTLING_TROUGH.get().defaultBlockState()
                .setValue(MachineProperties.FACING, Direction.NORTH));
        ProcessMachineBlockEntity next = entity(level, pos, ProcessMachineBlockEntity.class);
        next.load(saved);
        require(previous.isRemoved() && next != previous && next.nativeStore() != previous.nativeStore(),
                "Callback replacement did not acquire a distinct native owner");
        return next;
    }

    private static void require(boolean condition, String failure) {
        if (!condition) {
            throw new IllegalStateException(failure);
        }
    }
}
