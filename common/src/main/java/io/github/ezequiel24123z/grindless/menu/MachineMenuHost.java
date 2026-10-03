package io.github.ezequiel24123z.grindless.menu;

import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;

/**
 * What the shared machine menu reads. Implemented by the Thermal Generator and both process
 * machines so one screen covers the dry line.
 */
public interface MachineMenuHost {

    MachineMenuKind menuKind();

    Container menuContainer();

    SimpleFluxStorage energy();

    ContainerData menuData();

    MachineStatus menuStatus();

    Component menuFault();

    boolean stillValid(Player player);
}
