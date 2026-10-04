package io.github.ezequiel24123z.grindless.star;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Moves one player between the dimension they are standing in and the Drift (ADR-0096).
 *
 * <p>The link has one destination, short of the galactic centre. It does not dial an
 * address, and it does not open the black hole. The return pad is placed because no
 * rocket exists yet to deliver a far ring.
 */
public final class StarwardTravel {

    private StarwardTravel() {
    }

    /**
     * Sends {@code player} to the Drift and remembers {@code link} as the way home.
     *
     * @return whether the destination dimension existed
     */
    public static boolean depart(ServerPlayer player, BlockPos link) {
        if (!(player.level() instanceof ServerLevel from)) {
            return false;
        }
        if (DriftCatalogue.isDrift(from.dimension().location().toString())) {
            return false;
        }
        ServerLevel drift = from.getServer().getLevel(driftKey());
        if (drift == null) {
            return false;
        }
        player.stopRiding();
        StarwardReturnData.get(from.getServer()).remember(
                player.getUUID(),
                from.dimension().location().toString(),
                link.getX(), link.getY(), link.getZ());
        player.teleportTo(drift,
                StarwardLinkLogic.STAND_X + 0.5,
                StarwardLinkLogic.ARRIVAL_Y,
                StarwardLinkLogic.STAND_Z + 0.5,
                player.getYRot(), player.getXRot());
        placeReturnPad(drift);
        return true;
    }

    /**
     * Sends {@code player} back to the link they left from, or the overworld spawn when
     * that point was never written.
     */
    public static boolean home(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel from)) {
            return false;
        }
        player.stopRiding();
        StarwardReturnData.Point point = StarwardReturnData.get(from.getServer()).point(player.getUUID());
        ServerLevel dest = from.getServer().overworld();
        double x = dest.getSharedSpawnPos().getX() + 0.5;
        double y = dest.getSharedSpawnPos().getY();
        double z = dest.getSharedSpawnPos().getZ() + 0.5;
        if (point != null) {
            ResourceLocation id = ResourceLocation.tryParse(point.dimension());
            if (id != null) {
                ServerLevel saved = from.getServer().getLevel(
                        ResourceKey.create(Registries.DIMENSION, id));
                if (saved != null) {
                    dest = saved;
                    x = point.x() + 0.5;
                    y = point.y() + 1.0;
                    z = point.z() + 0.5;
                }
            }
        }
        player.teleportTo(dest, x, y, z, player.getYRot(), player.getXRot());
        return true;
    }

    private static void placeReturnPad(ServerLevel drift) {
        BlockPos pad = new BlockPos(
                StarwardLinkLogic.ARRIVAL_X, StarwardLinkLogic.ARRIVAL_Y, StarwardLinkLogic.ARRIVAL_Z);
        if (drift.getBlockState(pad).isAir()) {
            drift.setBlockAndUpdate(pad, ModBlocks.STARWARD_LINK.get().defaultBlockState());
        }
    }

    private static ResourceKey<Level> driftKey() {
        return ResourceKey.create(Registries.DIMENSION, Grindless.id("drift"));
    }
}
