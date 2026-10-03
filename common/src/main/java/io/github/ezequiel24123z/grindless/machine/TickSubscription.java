package io.github.ezequiel24123z.grindless.machine;

/**
 * A handle on work a machine has asked to have run each tick.
 *
 * <p>Held by whatever decided the work was needed, and cancelled by the same code when it stops
 * being needed. See {@link TickSubscriptions} for why machines work this way.
 */
public interface TickSubscription {

    /** Stops the work running. Calling this more than once is harmless. */
    void unsubscribe();

    /** Whether this subscription is still running its work. */
    boolean isActive();
}
