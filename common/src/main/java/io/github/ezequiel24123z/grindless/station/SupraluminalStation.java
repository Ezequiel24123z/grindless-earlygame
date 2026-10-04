package io.github.ezequiel24123z.grindless.station;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.network.FluxNetworkData;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import io.github.ezequiel24123z.grindless.registry.ModEntities;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * A seat that climbs to the build ceiling and then arrives (ADR-0098, ADR-0099).
 *
 * <p>It is not a link. The ceiling is the arrival: the Drift on the way out, the
 * galactic centre from the Drift, and the berth they left on the way out of the
 * centre. Shift does not end the trip once the climb has started.
 */
public class SupraluminalStation extends Entity {

    private static final EntityDataAccessor<Boolean> LAUNCHED =
            SynchedEntityData.defineId(SupraluminalStation.class, EntityDataSerializers.BOOLEAN);

    private static final String KEY_CHARGE = "Charge";
    private static final String KEY_LAUNCHED = "Launched";
    private static final String KEY_BERTH_DIM = "BerthDim";
    private static final String KEY_BERTH_X = "BerthX";
    private static final String KEY_BERTH_Y = "BerthY";
    private static final String KEY_BERTH_Z = "BerthZ";
    private static final String KEY_RIDER = "Rider";

    private long stored;
    private String berthDimension = "";
    private int berthX;
    private int berthY;
    private int berthZ;
    private UUID rider;
    private boolean settled;
    private boolean toldUnpowered;
    private boolean toldClimbing;

    public SupraluminalStation(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static SupraluminalStation create(ServerLevel level, BlockPos berth) {
        SupraluminalStation station = ModEntities.SUPRALUMINAL_STATION.get().create(level);
        if (station == null) {
            return null;
        }
        station.moveTo(berth.getX() + 0.5, berth.getY() + 1.0, berth.getZ() + 0.5, 0.0F, 0.0F);
        station.berthDimension = level.dimension().location().toString();
        station.berthX = berth.getX();
        station.berthY = berth.getY();
        station.berthZ = berth.getZ();
        return station;
    }

    public String berthDimension() {
        return berthDimension;
    }

    public int berthX() {
        return berthX;
    }

    public int berthY() {
        return berthY;
    }

    public int berthZ() {
        return berthZ;
    }

    public BlockPos berthPos() {
        return new BlockPos(berthX, berthY, berthZ);
    }

    public boolean isLaunched() {
        return entityData.get(LAUNCHED);
    }

    public boolean atCeiling() {
        return isLaunched() && StationRide.atCeiling(getY(), ceiling());
    }

    /**
     * Puts the station item back once. A later discard must not drop a second one.
     */
    public void giveBack(ServerPlayer player) {
        if (settled) {
            return;
        }
        settled = true;
        ItemStack stack = new ItemStack(ModItems.SUPRALUMINAL_STATION.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(LAUNCHED, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        stored = tag.getLong(KEY_CHARGE);
        entityData.set(LAUNCHED, tag.getBoolean(KEY_LAUNCHED));
        berthDimension = tag.getString(KEY_BERTH_DIM);
        berthX = tag.getInt(KEY_BERTH_X);
        berthY = tag.getInt(KEY_BERTH_Y);
        berthZ = tag.getInt(KEY_BERTH_Z);
        rider = tag.hasUUID(KEY_RIDER) ? tag.getUUID(KEY_RIDER) : null;
        noPhysics = isLaunched();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong(KEY_CHARGE, stored);
        tag.putBoolean(KEY_LAUNCHED, isLaunched());
        tag.putString(KEY_BERTH_DIM, berthDimension);
        tag.putInt(KEY_BERTH_X, berthX);
        tag.putInt(KEY_BERTH_Y, berthY);
        tag.putInt(KEY_BERTH_Z, berthZ);
        if (rider != null) {
            tag.putUUID(KEY_RIDER, rider);
        }
    }

    @Override
    public boolean isPickable() {
        return !settled;
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.85;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            player.startRiding(this);
            rider = player.getUUID();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && !settled && reason.shouldDestroy()) {
            settled = true;
            spawnAtLocation(ModItems.SUPRALUMINAL_STATION.get());
        }
        super.remove(reason);
    }

    @Override
    public void tick() {
        super.tick();
        noPhysics = isLaunched();
        if (level().isClientSide || settled) {
            return;
        }
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        ServerPlayer player = rider(server);
        if (player == null) {
            return;
        }
        String here = server.dimension().location().toString();
        if (!StationRide.canDepart(here)) {
            return;
        }
        if (!isLaunched()) {
            charge(server, player, here);
            return;
        }
        double next = StationRide.climb(getY(), ceiling());
        if (next != getY()) {
            setPos(getX(), next, getZ());
        }
        if (atCeiling() && server.getGameTime() % 20L == 0L) {
            StationTravel.arrive(player, this);
        }
    }

    private void charge(ServerLevel server, ServerPlayer player, String here) {
        long toll = StationRide.toll(here);
        if (!onBerth(server)) {
            if (!toldUnpowered) {
                toldUnpowered = true;
                player.displayClientMessage(Component.translatable("chat.grindless.station.berth"), true);
            }
            return;
        }
        if (!StationRide.ready(stored, toll)) {
            FluxNetwork network = FluxNetworkData.get(server).networkCovering(berthPos());
            if (network == null) {
                MachineProperties.publish(server, berthPos(), MachineStatus.STARVED);
                if (!toldUnpowered) {
                    toldUnpowered = true;
                    player.displayClientMessage(Component.translatable("chat.grindless.station.unpowered"), true);
                }
                return;
            }
            long ask = StationRide.remaining(stored, toll);
            network.registerDemand(ask);
            long share = Math.round(ask * network.satisfaction());
            long drawn = network.extract(share, false);
            if (drawn > 0L) {
                stored = StationRide.accept(stored, drawn, toll);
            }
            MachineProperties.publish(server, berthPos(),
                    drawn > 0L ? MachineStatus.RUNNING : MachineStatus.STARVED);
            if (!StationRide.ready(stored, toll)) {
                return;
            }
        }
        MachineProperties.publish(server, berthPos(), MachineStatus.IDLE);
        entityData.set(LAUNCHED, true);
        noPhysics = true;
        if (!toldClimbing) {
            toldClimbing = true;
            player.displayClientMessage(Component.translatable("chat.grindless.station.climbing"), true);
        }
    }

    private boolean onBerth(ServerLevel server) {
        return server.getBlockState(berthPos()).is(ModBlocks.STATION_BERTH.get());
    }

    private ServerPlayer rider(ServerLevel server) {
        if (getFirstPassenger() instanceof ServerPlayer riding) {
            rider = riding.getUUID();
            return riding;
        }
        if (rider == null || !isLaunched()) {
            return null;
        }
        ServerPlayer player = server.getServer().getPlayerList().getPlayer(rider);
        if (player != null && player.level() == server) {
            player.startRiding(this);
            return player;
        }
        return null;
    }

    private int ceiling() {
        return StationRide.ceilingY(level().getMaxBuildHeight());
    }
}
