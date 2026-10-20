package io.github.ezequiel24123z.grindless.menu;

import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidState;

import java.util.Optional;

/**
 * A read-only primary store; its output quota is included in amount, never added.
 * An absent ID means unconfirmed identity, not an empty store.
 */
public record MachineFluidView(Optional<String> id, int amount, int capacity, int outputQuota) {

    public MachineFluidView {
        if (id == null || id.filter(String::isBlank).isPresent() || amount < 0 || capacity <= 0 || outputQuota < 0) {
            throw new IllegalArgumentException("a fluid view needs an optional ID, nonnegative volumes and positive capacity");
        }
    }

    public static int waterFlag(FluidState state) {
        return state.is(FluidLogic.WATER) ? 1 : 0;
    }

    public static MachineFluidView fromMenuData(int waterFlag, int amount, int capacity, int outputQuota) {
        Optional<String> id = waterFlag == 1 && amount > 0 ? Optional.of(FluidLogic.WATER) : Optional.empty();
        return new MachineFluidView(id, amount, capacity, outputQuota);
    }

    public int fillHeight(int height) {
        if (height < 0) {
            throw new IllegalArgumentException("fluid view height cannot be negative");
        }
        if (height == 0 || amount == 0) {
            return 0;
        }
        return (int) Math.min(height, (height * (long) amount + capacity - 1) / capacity);
    }
}
