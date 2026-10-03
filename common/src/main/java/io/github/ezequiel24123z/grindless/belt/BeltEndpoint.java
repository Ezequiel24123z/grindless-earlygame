package io.github.ezequiel24123z.grindless.belt;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * A block that can take an item from a belt or a manipulator and give one back.
 *
 * <p>Belts and the splitter implement this. Inventories do not — they stay on
 * {@code Container} / {@code ItemInsert}.
 */
public interface BeltEndpoint {

    boolean canInsert(Direction from, ItemStack stack);

    /** @return what did not fit */
    ItemStack insert(Direction from, ItemStack stack);

    boolean canExtract(Direction from);

    ItemStack extract(Direction from, int max);
}
