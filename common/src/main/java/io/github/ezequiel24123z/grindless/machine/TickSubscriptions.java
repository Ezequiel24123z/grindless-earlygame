package io.github.ezequiel24123z.grindless.machine;

import java.util.ArrayList;
import java.util.List;

/**
 * The per-machine list of work to run each tick, and the reason an idle Grindless machine costs
 * almost nothing.
 *
 * <h2>Why this exists</h2>
 *
 * <p>The characteristic way a tech mod ruins a server is that every machine does a little work
 * every tick whether or not it has anything to do — poll the inventory, search the recipe list,
 * look up the neighbour. Ten thousand machines each wasting thirty microseconds is the whole tick
 * budget, and the base looks idle while it does it.
 *
 * <p>So a Grindless machine <b>does not tick by default</b>. It subscribes work when something
 * makes that work necessary and unsubscribes the moment it is not (ADR-0042). A machine with an
 * empty input buffer and no running recipe holds no subscriptions, and its tick is an
 * emptiness check.
 *
 * <p>This mirrors GregTech CEu Modern, whose documentation puts it plainly: <i>"for the sake of
 * performance, our machines are no longer always in a tickable state"</i>. The pattern is theirs;
 * the reasoning applies to any mod of this size.
 *
 * <h2>The rule that makes it work</h2>
 *
 * <p>Subscriptions must be re-evaluated whenever an <em>input to the decision</em> changes — a
 * buffer gaining its first item, a side being configured, a neighbour appearing. That is what
 * change listeners are for, and it is the part that is easy to get wrong: a machine that forgets
 * to resubscribe looks broken in a way that is very hard to diagnose, because it is doing nothing
 * both when it should and when it should not.
 *
 * <p>Not thread-safe. Everything here runs on the server thread.
 */
public final class TickSubscriptions {

    /** Most machines hold one or two: the recipe logic, and auto-output. */
    private final List<Entry> entries = new ArrayList<>(2);

    private boolean iterating;
    private boolean needsSweep;

    /**
     * Registers {@code work} to run every tick until cancelled.
     *
     * <p>Pass the existing handle back in to avoid subscribing the same work twice — the common
     * call shape is {@code subs = subscribe(subs, this::doThing)} from a method that may run
     * repeatedly as conditions change.
     *
     * @param existing a handle from a previous call, or {@code null}
     * @return the live subscription
     */
    public TickSubscription subscribe(TickSubscription existing, Runnable work) {
        if (existing != null && existing.isActive()) {
            return existing;
        }
        Entry entry = new Entry(work);
        entries.add(entry);
        return entry;
    }

    /** Registers {@code work}, with no existing handle. */
    public TickSubscription subscribe(Runnable work) {
        return subscribe(null, work);
    }

    /** Whether there is nothing to do. A machine in this state is costing nothing. */
    public boolean isIdle() {
        for (Entry entry : entries) {
            if (entry.active) {
                return false;
            }
        }
        return true;
    }

    /** How many subscriptions are live. */
    public int activeCount() {
        int count = 0;
        for (Entry entry : entries) {
            if (entry.active) {
                count++;
            }
        }
        return count;
    }

    /**
     * Runs every live subscription once.
     *
     * <p>Work routinely unsubscribes itself — that is the normal way a machine goes idle — and may
     * subscribe more. Both are safe here: the list is indexed rather than iterated, and removal of
     * cancelled entries is deferred until the pass finishes, so a subscription cancelling itself
     * mid-tick cannot shuffle the list underneath the loop.
     */
    public void tick() {
        iterating = true;
        try {
            // Indexed, and re-reading size each step, so work that subscribes more work during
            // the pass is picked up rather than skipped or concurrently modifying the list.
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                if (entry.active) {
                    entry.work.run();
                }
            }
        } finally {
            iterating = false;
        }
        if (needsSweep) {
            entries.removeIf(entry -> !entry.active);
            needsSweep = false;
        }
    }

    /** Cancels everything. For machine unload and removal. */
    public void clear() {
        for (Entry entry : entries) {
            entry.active = false;
        }
        if (iterating) {
            needsSweep = true;
        } else {
            entries.clear();
        }
    }

    private final class Entry implements TickSubscription {

        private final Runnable work;
        private boolean active = true;

        private Entry(Runnable work) {
            this.work = work;
        }

        @Override
        public void unsubscribe() {
            if (!active) {
                return;
            }
            active = false;
            if (iterating) {
                needsSweep = true;
            } else {
                entries.remove(this);
            }
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
