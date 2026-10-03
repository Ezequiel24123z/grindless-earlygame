package io.github.ezequiel24123z.grindless.machine;

/**
 * How often a charged generator with nowhere to put its power should try again.
 *
 * <p>Unsubscribing is not an option here, and that is the reason this class exists. A generator
 * stops being useful for reasons that raise no event when they end: a neighbouring machine that
 * was full drains, or a pylon placed nearby brings the generator inside a network's supply area.
 * Nothing notifies the generator of either, so a generator that went to sleep on "nobody took my
 * power" would sleep through the moment somebody could.
 *
 * <p>So it keeps polling, and slows down while polling finds nothing. The period doubles with each
 * consecutive fruitless push up to a ceiling short enough that nobody notices the delay when a
 * sink does appear, and any push that moves power puts it back to full speed.
 */
final class PushBackoff {

    /** The slowest an idle generator polls, in ticks: one second. */
    static final int MAX_PERIOD = 20;

    private PushBackoff() {
    }

    /**
     * The push period after {@code fruitlessPushes} consecutive pushes that moved nothing.
     *
     * @param base the period while power is flowing, in ticks
     */
    static int period(int base, int fruitlessPushes) {
        int period = base;
        for (int i = 0; i < fruitlessPushes && period < MAX_PERIOD; i++) {
            period *= 2;
        }
        return Math.min(period, Math.max(base, MAX_PERIOD));
    }
}
