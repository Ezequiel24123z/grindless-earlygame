package io.github.ezequiel24123z.grindless.menu;

import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;

import java.util.Map;
import java.util.Optional;

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

    default Optional<RecipeSelection> recipeSelection() {
        return Optional.empty();
    }

    default SelectionResult selectRecipe(Player player, String id) {
        return SelectionResult.NOT_ASSEMBLER;
    }

    record RecipeSelection(String selected, String activeRecipe, double activeWork,
                           Map<String, Double> suspended, SelectionIssue issue) {
        public RecipeSelection {
            suspended = Map.copyOf(suspended);
        }
    }

    enum SelectionIssue {
        AUTO, SELECTED, UNAVAILABLE, MISSING_ITEMS, MISSING_FLUID, CONDITIONS, OUTPUT_BLOCKED, INVALID_WORK;

        public String translationKey() {
            return "gui.grindless.assembler.state." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    enum SelectionResult {
        SELECTED, UNCHANGED, CLOSED, DENIED, STALE, INVALID_RECIPE, NOT_ASSEMBLER, BUSY, INVALID_WORK, MALFORMED;

        public String translationKey() {
            return "gui.grindless.assembler.result." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
