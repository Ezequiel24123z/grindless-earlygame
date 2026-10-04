package io.github.ezequiel24123z.grindless.flight;

import io.github.ezequiel24123z.grindless.planet.PlanetCatalogue;
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
 * Finishes a rocket flight once the rider has chosen a body (ADR-0097).
 *
 * <p>The rocket has already climbed. This class only lands. The Drift is not a choice.
 * Neither is a black hole: that ride is the station (ADR-0098), and this rocket is not it.
 */
public final class RocketTravel {

    private RocketTravel() {
    }

    /**
     * Lands {@code player} on {@code site}, or back on the pad they climbed from when
     * {@code site} is the dimension they are already in.
     *
     * @return whether a destination dimension existed
     */
    public static boolean choose(ServerPlayer player, String site) {
        if (!(player.getVehicle() instanceof SurveyRocket rocket) || !rocket.atCeiling()) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel from)) {
            return false;
        }
        String here = from.dimension().location().toString();
        if (!RocketFlight.canLaunch(here) || !RocketFlight.isSite(site)) {
            return false;
        }
        if (site.equals(here)) {
            return landOnPad(player, rocket, from, rocket.padX(), rocket.padY(), rocket.padZ());
        }
        if (PlanetCatalogue.isLuna(site)) {
            if (RocketFlight.HOME.equals(here)) {
                RocketReturnData.get(from.getServer()).remember(
                        player.getUUID(), rocket.padDimension(),
                        rocket.padX(), rocket.padY(), rocket.padZ());
            }
            ServerLevel luna = from.getServer().getLevel(key(PlanetCatalogue.LUNA));
            if (luna == null) {
                player.displayClientMessage(Component.translatable("chat.grindless.rocket.missing"), true);
                return false;
            }
            return landBeside(player, rocket, luna);
        }
        ServerLevel home = from.getServer().getLevel(key(RocketFlight.HOME));
        if (home == null) {
            player.displayClientMessage(Component.translatable("chat.grindless.rocket.missing"), true);
            return false;
        }
        RocketReturnData data = RocketReturnData.get(from.getServer());
        RocketReturnData.Point point = data.point(player.getUUID());
        data.forget(player.getUUID());
        if (point != null && RocketFlight.HOME.equals(point.dimension())) {
            return landOnPad(player, rocket, home, point.x(), point.y(), point.z());
        }
        BlockPos spawn = home.getSharedSpawnPos();
        return landOnPad(player, rocket, home, spawn.getX(), spawn.getY(), spawn.getZ());
    }

    /** Feet on the pad. Used for the home world and for an abort back to the pad just left. */
    private static boolean landOnPad(ServerPlayer player, SurveyRocket rocket, ServerLevel dest,
                                     int x, int y, int z) {
        BlockPos pad = new BlockPos(x, y, z);
        BlockState state = dest.getBlockState(pad);
        if (!state.isAir() && !state.is(ModBlocks.LAUNCH_PAD.get())) {
            pad = pad.above();
        }
        placePad(dest, pad);
        arrive(player, rocket, dest, pad.getX() + 0.5, pad.getY() + 1.0, pad.getZ() + 0.5);
        return true;
    }

    /** Feet on the regolith beside the pad. The pad occupies the column the link used to. */
    private static boolean landBeside(ServerPlayer player, SurveyRocket rocket, ServerLevel luna) {
        BlockPos pad = new BlockPos(
                RocketFlight.LUNA_PAD_X, RocketFlight.LUNA_PAD_Y, RocketFlight.LUNA_PAD_Z);
        placePad(luna, pad);
        arrive(player, rocket, luna,
                RocketFlight.LUNA_STAND_X + 0.5,
                pad.getY(),
                RocketFlight.LUNA_STAND_Z + 0.5);
        return true;
    }

    private static void arrive(ServerPlayer player, SurveyRocket rocket, ServerLevel dest,
                               double x, double y, double z) {
        rocket.giveBack(player);
        player.stopRiding();
        player.teleportTo(dest, x, y, z, player.getYRot(), player.getXRot());
        rocket.discard();
        player.displayClientMessage(
                Component.translatable(RocketFlight.siteKey(dest.dimension().location().toString())), true);
    }

    private static void placePad(ServerLevel level, BlockPos pad) {
        if (level.getBlockState(pad).isAir()) {
            level.setBlockAndUpdate(pad, ModBlocks.LAUNCH_PAD.get().defaultBlockState());
        }
    }

    private static ResourceKey<Level> key(String dimension) {
        ResourceLocation id = ResourceLocation.tryParse(dimension);
        if (id == null) {
            id = new ResourceLocation(RocketFlight.HOME);
        }
        return ResourceKey.create(Registries.DIMENSION, id);
    }
}
