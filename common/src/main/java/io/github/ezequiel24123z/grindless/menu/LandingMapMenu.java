package io.github.ezequiel24123z.grindless.menu;

import io.github.ezequiel24123z.grindless.flight.RocketFlight;
import io.github.ezequiel24123z.grindless.flight.RocketTravel;
import io.github.ezequiel24123z.grindless.flight.SurveyRocket;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The landing map. No slots: the choice is a body, not an item (ADR-0097).
 *
 * <p>Button ids are indexes into {@link RocketFlight#sites()}. The screen and the
 * server share that list, so a click cannot name a world the map does not show.
 */
public final class LandingMapMenu extends AbstractContainerMenu {

    public LandingMapMenu(int id, Inventory inventory) {
        super(ModMenus.LANDING_MAP.get(), id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getVehicle() instanceof SurveyRocket rocket && rocket.atCeiling();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        List<String> sites = RocketFlight.sites();
        if (id < 0 || id >= sites.size()) {
            return false;
        }
        return RocketTravel.choose(server, sites.get(id));
    }
}
