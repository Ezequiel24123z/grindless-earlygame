package io.github.ezequiel24123z.grindless.station;

import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Finishes a station ride once the ceiling is reached (ADR-0098).
 *
 * <p>The station has already climbed. This class only arrives. The Drift is the hop.
 * The black hole is not. There is no link and no landing map.
 */
public final class StationTravel {

    private StationTravel() {
    }

    /**
     * Lands {@code player} on the Drift, or back on the berth they climbed from when
     * they are already on the Drift.
     *
     * @return whether a destination dimension existed
     */
    public static boolean arrive(ServerPlayer player, SupraluminalStation station) {
        if (!(player.getVehicle() instanceof SupraluminalStation riding) || riding != station
                || !station.atCeiling()) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel from)) {
            return false;
        }
        String here = from.dimension().location().toString();
        if (!StationRide.canDepart(here)) {
            return false;
        }
        if (StationRide.isDrift(here)) {
            return home(player, station, from);
        }
        return toDrift(player, station, from);
    }

    private static boolean toDrift(ServerPlayer player, SupraluminalStation station, ServerLevel from) {
        ServerLevel drift = from.getServer().getLevel(key(StationRide.destination(from.dimension().location().toString())));
        if (drift == null) {
            player.displayClientMessage(Component.translatable("chat.grindless.station.missing"), true);
            return false;
        }
        StationReturnData.get(from.getServer()).remember(
                player.getUUID(), station.berthDimension(),
                station.berthX(), station.berthY(), station.berthZ());
        BlockPos berth = new BlockPos(
                StationRide.DRIFT_BERTH_X, StationRide.DRIFT_BERTH_Y, StationRide.DRIFT_BERTH_Z);
        placeBerth(drift, berth);
        arriveAt(player, station, drift,
                StationRide.DRIFT_STAND_X + 0.5,
                berth.getY(),
                StationRide.DRIFT_STAND_Z + 0.5,
                "chat.grindless.station.drift");
        return true;
    }

    private static boolean home(ServerPlayer player, SupraluminalStation station, ServerLevel from) {
        StationReturnData data = StationReturnData.get(from.getServer());
        StationReturnData.Point point = data.point(player.getUUID());
        data.forget(player.getUUID());
        ServerLevel dest = from.getServer().getLevel(key(StationRide.HOME));
        int x;
        int y;
        int z;
        if (point != null) {
            ServerLevel saved = from.getServer().getLevel(key(point.dimension()));
            if (saved != null) {
                dest = saved;
                x = point.x();
                y = point.y();
                z = point.z();
            } else {
                dest = from.getServer().overworld();
                BlockPos spawn = dest.getSharedSpawnPos();
                x = spawn.getX();
                y = spawn.getY();
                z = spawn.getZ();
            }
        } else if (dest != null) {
            BlockPos spawn = dest.getSharedSpawnPos();
            x = spawn.getX();
            y = spawn.getY();
            z = spawn.getZ();
        } else {
            player.displayClientMessage(Component.translatable("chat.grindless.station.missing"), true);
            return false;
        }
        if (dest == null) {
            player.displayClientMessage(Component.translatable("chat.grindless.station.missing"), true);
            return false;
        }
        return landOnBerth(player, station, dest, x, y, z);
    }

    private static boolean landOnBerth(ServerPlayer player, SupraluminalStation station, ServerLevel dest,
                                       int x, int y, int z) {
        BlockPos berth = new BlockPos(x, y, z);
        BlockState state = dest.getBlockState(berth);
        if (!state.isAir() && !state.is(ModBlocks.STATION_BERTH.get())
                && !state.is(ModBlocks.STARWARD_LINK.get())) {
            berth = berth.above();
        }
        placeBerth(dest, berth);
        arriveAt(player, station, dest,
                berth.getX() + 0.5, berth.getY() + 1.0, berth.getZ() + 0.5,
                "chat.grindless.station.returned");
        return true;
    }

    private static void arriveAt(ServerPlayer player, SupraluminalStation station, ServerLevel dest,
                                 double x, double y, double z, String message) {
        station.giveBack(player);
        player.stopRiding();
        player.teleportTo(dest, x, y, z, player.getYRot(), player.getXRot());
        station.discard();
        player.displayClientMessage(Component.translatable(message), true);
    }

    /** Air becomes a berth. A starward link on that column becomes a berth. Anything else stays. */
    private static void placeBerth(ServerLevel level, BlockPos berth) {
        BlockState state = level.getBlockState(berth);
        if (state.isAir() || state.is(ModBlocks.STARWARD_LINK.get())) {
            level.setBlockAndUpdate(berth, ModBlocks.STATION_BERTH.get().defaultBlockState());
        }
    }

    private static ResourceKey<Level> key(String dimension) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) {
            id = new ResourceLocation(StationRide.HOME);
        }
        return ResourceKey.create(Registries.DIMENSION, id);
    }
}
