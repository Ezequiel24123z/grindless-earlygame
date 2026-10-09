package io.github.ezequiel24123z.grindless.factory;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Teleports through a Factory Portal while preserving a player-specific return endpoint. */
public final class FactoryTravel {

    public static final int ARRIVAL_X = 0;
    public static final int ARRIVAL_Y = FactoryCatalogue.SURFACE_Y + 1;
    public static final int ARRIVAL_Z = 0;
    public static final int STAND_X = 1;
    public static final int STAND_Z = 0;

    private FactoryTravel() {
    }

    /** Uses a portal in either direction. @return true when a destination level existed. */
    public static boolean travel(ServerPlayer player, BlockPos portal) {
        return player.level() instanceof ServerLevel level && FactoryCatalogue.isFactory(
                level.dimension().location().toString()) ? home(player) : depart(player, portal);
    }

    private static boolean depart(ServerPlayer player, BlockPos portal) {
        if (!(player.level() instanceof ServerLevel from)) {
            return false;
        }
        ServerLevel factory = from.getServer().getLevel(factoryKey());
        if (factory == null) {
            return false;
        }
        player.stopRiding();
        FactoryReturnData.get(from.getServer()).remember(player.getUUID(),
                from.dimension().location().toString(), portal.getX(), portal.getY(), portal.getZ());
        placeReturnPortal(factory);
        player.teleportTo(factory, STAND_X + 0.5, ARRIVAL_Y, STAND_Z + 0.5,
                player.getYRot(), player.getXRot());
        return true;
    }

    private static boolean home(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel from)) {
            return false;
        }
        FactoryReturnData.Point point = FactoryReturnData.get(from.getServer()).point(player.getUUID());
        ServerLevel destination = from.getServer().overworld();
        double x = destination.getSharedSpawnPos().getX() + 0.5;
        double y = destination.getSharedSpawnPos().getY();
        double z = destination.getSharedSpawnPos().getZ() + 0.5;
        if (point != null) {
            ResourceLocation id = ResourceLocation.tryParse(point.dimension());
            if (id != null) {
                ServerLevel saved = from.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
                if (saved != null) {
                    destination = saved;
                    x = point.x() + 0.5;
                    y = point.y() + 1.0;
                    z = point.z() + 0.5;
                }
            }
        }
        player.stopRiding();
        player.teleportTo(destination, x, y, z, player.getYRot(), player.getXRot());
        return true;
    }

    private static void placeReturnPortal(ServerLevel factory) {
        BlockPos portal = new BlockPos(ARRIVAL_X, ARRIVAL_Y, ARRIVAL_Z);
        if (factory.getBlockState(portal).isAir()) {
            factory.setBlockAndUpdate(portal, ModBlocks.FACTORY_PORTAL.get().defaultBlockState());
        }
    }

    private static ResourceKey<Level> factoryKey() {
        return ResourceKey.create(Registries.DIMENSION, Grindless.id("factory"));
    }
}
