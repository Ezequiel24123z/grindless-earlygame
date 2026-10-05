package io.github.ezequiel24123z.grindless.flight;

import dev.architectury.registry.menu.MenuRegistry;
import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.menu.LandingMapMenu;
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
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * A seat that climbs to the build ceiling and then waits on the landing map (ADR-0097).
 *
 * <p>It is not a link. It does not change dimension by itself. The rider chooses a body,
 * and {@link RocketTravel} finishes the flight. Shift does not drop the rider once the
 * climb has started: the map is the way off.
 */
public class SurveyRocket extends Entity {

    private static final EntityDataAccessor<Boolean> LAUNCHED =
            SynchedEntityData.defineId(SurveyRocket.class, EntityDataSerializers.BOOLEAN);

    private static final String KEY_CHARGE = "Charge";
    private static final String KEY_LAUNCHED = "Launched";
    private static final String KEY_PAD_DIM = "PadDim";
    private static final String KEY_PAD_X = "PadX";
    private static final String KEY_PAD_Y = "PadY";
    private static final String KEY_PAD_Z = "PadZ";
    private static final String KEY_RIDER = "Rider";

    private long stored;
    private String padDimension = "";
    private int padX;
    private int padY;
    private int padZ;
    private UUID rider;
    private boolean settled;
    private boolean toldUnpowered;
    private boolean toldNotLocal;
    private boolean toldClimbing;

    public SurveyRocket(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static SurveyRocket create(ServerLevel level, BlockPos pad) {
        SurveyRocket rocket = ModEntities.SURVEY_ROCKET.get().create(level);
        if (rocket == null) {
            return null;
        }
        rocket.moveTo(pad.getX() + 0.5, pad.getY() + 1.0, pad.getZ() + 0.5, 0.0F, 0.0F);
        rocket.padDimension = level.dimension().location().toString();
        rocket.padX = pad.getX();
        rocket.padY = pad.getY();
        rocket.padZ = pad.getZ();
        return rocket;
    }

    public String padDimension() {
        return padDimension;
    }

    public int padX() {
        return padX;
    }

    public int padY() {
        return padY;
    }

    public int padZ() {
        return padZ;
    }

    public BlockPos padPos() {
        return new BlockPos(padX, padY, padZ);
    }

    public boolean isLaunched() {
        return entityData.get(LAUNCHED);
    }

    public boolean atCeiling() {
        return isLaunched() && RocketFlight.atCeiling(getY(), ceiling());
    }

    /**
     * Puts the rocket item back once. A later discard must not drop a second one.
     */
    public void giveBack(ServerPlayer player) {
        if (settled) {
            return;
        }
        settled = true;
        ItemStack stack = new ItemStack(ModItems.SURVEY_ROCKET.get());
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
        padDimension = tag.getString(KEY_PAD_DIM);
        padX = tag.getInt(KEY_PAD_X);
        padY = tag.getInt(KEY_PAD_Y);
        padZ = tag.getInt(KEY_PAD_Z);
        rider = tag.hasUUID(KEY_RIDER) ? tag.getUUID(KEY_RIDER) : null;
        noPhysics = isLaunched();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong(KEY_CHARGE, stored);
        tag.putBoolean(KEY_LAUNCHED, isLaunched());
        tag.putString(KEY_PAD_DIM, padDimension);
        tag.putInt(KEY_PAD_X, padX);
        tag.putInt(KEY_PAD_Y, padY);
        tag.putInt(KEY_PAD_Z, padZ);
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
        return 0.6;
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
            spawnAtLocation(ModItems.SURVEY_ROCKET.get());
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
        if (!RocketFlight.canLaunch(here)) {
            if (!toldNotLocal) {
                toldNotLocal = true;
                player.displayClientMessage(Component.translatable("chat.grindless.rocket.not_local"), true);
            }
            return;
        }
        if (!isLaunched()) {
            charge(server, player, here);
            return;
        }
        double next = RocketFlight.climb(getY(), ceiling());
        if (next != getY()) {
            setPos(getX(), next, getZ());
        }
        if (atCeiling() && !(player.containerMenu instanceof LandingMapMenu)
                && server.getGameTime() % 20L == 0L) {
            openMap(player);
        }
    }

    private void charge(ServerLevel server, ServerPlayer player, String here) {
        long toll = RocketFlight.toll(here);
        if (!onPad(server)) {
            if (!toldUnpowered) {
                toldUnpowered = true;
                player.displayClientMessage(Component.translatable("chat.grindless.rocket.pad"), true);
            }
            return;
        }
        if (!RocketFlight.ready(stored, toll)) {
            FluxNetwork network = FluxNetworkData.get(server).networkCovering(padPos());
            if (network == null) {
                MachineProperties.publish(server, padPos(), MachineStatus.STARVED);
                if (!toldUnpowered) {
                    toldUnpowered = true;
                    player.displayClientMessage(Component.translatable("chat.grindless.rocket.unpowered"), true);
                }
                return;
            }
            long ask = RocketFlight.remaining(stored, toll);
            network.registerDemand(ask);
            long share = Math.round(ask * network.satisfaction());
            long drawn = network.extract(share, false);
            if (drawn > 0L) {
                stored = RocketFlight.accept(stored, drawn, toll);
            }
            MachineProperties.publish(server, padPos(),
                    drawn > 0L ? MachineStatus.RUNNING : MachineStatus.STARVED);
            if (!RocketFlight.ready(stored, toll)) {
                return;
            }
        }
        MachineProperties.publish(server, padPos(), MachineStatus.IDLE);
        entityData.set(LAUNCHED, true);
        noPhysics = true;
        if (!toldClimbing) {
            toldClimbing = true;
            player.displayClientMessage(Component.translatable("chat.grindless.rocket.climbing"), true);
        }
    }

    private boolean onPad(ServerLevel server) {
        return server.getBlockState(padPos()).is(ModBlocks.LAUNCH_PAD.get());
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

    private void openMap(ServerPlayer player) {
        MenuRegistry.openMenu(player, new SimpleMenuProvider(
                (id, inventory, opener) -> new LandingMapMenu(id, inventory),
                Component.translatable("gui.grindless.landing.title")));
    }

    private int ceiling() {
        return RocketFlight.ceilingY(level().getMaxBuildHeight());
    }
}
