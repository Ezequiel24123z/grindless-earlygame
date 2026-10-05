package io.github.ezequiel24123z.grindless.planet;

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
 * Moves one player between the dimension they are standing in and Luna (ADR-0095).
 *
 * <p>The link has one destination. It does not dial an address, and it does not open a
 * second star. The return pad is placed because this link is still the placeholder
 * for the moon (ADR-0097). The survey rocket is the flight that replaces it. The
 * link is not a Horizon Gate, and this pass does not delete it.
 */
public final class LunarTravel {

    private LunarTravel() {
    }

    /**
     * Sends {@code player} to Luna and remembers {@code link} as the way home.
     *
     * @return whether the destination dimension existed
     */
    public static boolean depart(ServerPlayer player, BlockPos link) {
        if (!(player.level() instanceof ServerLevel from)) {
            return false;
        }
        if (PlanetCatalogue.isLuna(from.dimension().location().toString())) {
            return false;
        }
        ServerLevel luna = from.getServer().getLevel(lunaKey());
        if (luna == null) {
            return false;
        }
        player.stopRiding();
        LunarReturnData.get(from.getServer()).remember(
                player.getUUID(),
                from.dimension().location().toString(),
                link.getX(), link.getY(), link.getZ());
        player.teleportTo(luna,
                LunarLinkLogic.STAND_X + 0.5,
                LunarLinkLogic.ARRIVAL_Y,
                LunarLinkLogic.STAND_Z + 0.5,
                player.getYRot(), player.getXRot());
        placeReturnPad(luna);
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
        LunarReturnData.Point point = LunarReturnData.get(from.getServer()).point(player.getUUID());
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

    private static void placeReturnPad(ServerLevel luna) {
        BlockPos pad = new BlockPos(
                LunarLinkLogic.ARRIVAL_X, LunarLinkLogic.ARRIVAL_Y, LunarLinkLogic.ARRIVAL_Z);
        if (luna.getBlockState(pad).isAir()) {
            luna.setBlockAndUpdate(pad, ModBlocks.LUNAR_LINK.get().defaultBlockState());
        }
    }

    private static ResourceKey<Level> lunaKey() {
        return ResourceKey.create(Registries.DIMENSION, Grindless.id("luna"));
    }
}
