package io.github.ezequiel24123z.grindless.structure;

import java.util.Locale;

/** Justified T0 interfaces; a role is an access policy, never another stored inventory or tank. */
public enum WorkshopPortRole {

    ITEM_INPUT(true, true),
    ITEM_OUTPUT(true, false),
    FLUID_INPUT(false, true),
    FLUID_OUTPUT(false, false);

    private final boolean item;
    private final boolean input;

    WorkshopPortRole(boolean item, boolean input) {
        this.item = item;
        this.input = input;
    }

    public boolean item() {
        return item;
    }

    public boolean input() {
        return input;
    }

    public String translationKey() {
        return "gui.grindless.port." + name().toLowerCase(Locale.ROOT);
    }

    public String texture() {
        return "port_" + name().toLowerCase(Locale.ROOT);
    }
}
