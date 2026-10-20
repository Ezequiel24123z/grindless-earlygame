package io.github.ezequiel24123z.grindless.shop;

import java.util.function.Supplier;

/** Transient menu sequencing, not another claim ledger or a persisted economy account. */
public final class ShopSession {
    public static final int MAX_SEQUENCE = 32766;
    public static final int ACTION_INTERVAL_TICKS = 5;

    private final int containerId;
    private final long token;
    private int sequence;
    private long nextActionTick = Long.MIN_VALUE;
    private boolean settling;
    private boolean closed;

    public ShopSession(int containerId, long token) {
        this.containerId = containerId;
        this.token = token;
    }

    public int sequence() {
        return sequence;
    }

    public ShopResult exchange(int container, long suppliedToken, int suppliedSequence, int offer,
                               long tick, Supplier<ShopResult> settlement) {
        if (closed) {
            return ShopResult.CLOSED;
        }
        if (settling) {
            return ShopResult.REENTRANT;
        }
        if (container != containerId || suppliedToken != token || suppliedSequence != sequence) {
            return ShopResult.STALE_REQUEST;
        }
        if (offer < 0 || offer >= ShopCatalogue.offers().size()) {
            return ShopResult.INVALID_OFFER;
        }
        if (sequence >= MAX_SEQUENCE) {
            closed = true;
            return ShopResult.CLOSED;
        }
        sequence++;
        if (tick < nextActionTick) {
            return ShopResult.TOO_FAST;
        }
        nextActionTick = tick > Long.MAX_VALUE - ACTION_INTERVAL_TICKS
                ? Long.MAX_VALUE : tick + ACTION_INTERVAL_TICKS;
        settling = true;
        try {
            return settlement.get();
        } finally {
            settling = false;
        }
    }

    public void close() {
        closed = true;
    }
}
