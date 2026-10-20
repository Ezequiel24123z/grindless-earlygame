package io.github.ezequiel24123z.grindless.shop;

import java.util.Locale;

public enum ShopResult {
    READY, TRADED, LOCKED, MISSING_PAYMENT, INVENTORY_FULL, INVALID_OFFER,
    STALE_REQUEST, STALE_INVENTORY, REENTRANT, TOO_FAST, CLOSED;

    public String translationKey() {
        return "gui.grindless.shop.result." + name().toLowerCase(Locale.ROOT);
    }
}
