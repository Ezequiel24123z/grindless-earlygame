package io.github.ezequiel24123z.grindless.container;

import io.github.ezequiel24123z.grindless.machine.TickSubscription;

import java.util.ArrayList;
import java.util.List;

/**
 * Notifies interested parties that a container's contents changed.
 *
 * <p>This is the other half of the tick-subscription model (ADR-0042). Machines only avoid ticking
 * because something tells them when the world changed underneath them — a buffer gaining its first
 * item is what makes recipe logic worth subscribing. Without notification a machine would have to
 * poll to discover whether polling was needed, which is not an improvement.
 *
 * <p>Listeners are fired on <em>content</em> changes, not on reads. A listener must be cheap and
 * must not itself modify the container, since a change that fires a listener that causes a change
 * would recurse. The intended body of a listener is a call to the machine's
 * {@code updateSubscriptions} method, which is a handful of comparisons.
 *
 * <p>Registration returns a handle that must be cancelled when the listener's owner is unloaded.
 * A listener held by a removed block entity keeps that block entity reachable, which is a memory
 * leak that looks like a chunk failing to unload.
 *
 * <p>Not thread-safe; this is server-thread state.
 */
public final class ChangeListeners {

    private final List<Runnable> listeners = new ArrayList<>(2);

    private boolean firing;
    private boolean needsSweep;

    /**
     * Registers {@code listener}, returning a handle that removes it.
     *
     * <p>The handle is a {@link TickSubscription} because it is the same idea — something you
     * asked for and must cancel — and reusing the type keeps machine cleanup to one loop over one
     * kind of handle rather than two parallel lists that can fall out of step.
     */
    public TickSubscription add(Runnable listener) {
        Entry entry = new Entry(listener);
        listeners.add(entry);
        return entry;
    }

    /** Whether anything is listening. */
    public boolean isEmpty() {
        for (Object listener : listeners) {
            if (((Entry) listener).active) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells every listener the contents changed.
     *
     * <p>Re-entrant in the same way the tick loop is: a listener may cancel itself or register
     * another, and removal is deferred until the pass ends so the list cannot shuffle underneath
     * the loop.
     */
    public void notifyChanged() {
        firing = true;
        try {
            for (int i = 0; i < listeners.size(); i++) {
                Entry entry = (Entry) listeners.get(i);
                if (entry.active) {
                    entry.listener.run();
                }
            }
        } finally {
            firing = false;
        }
        if (needsSweep) {
            listeners.removeIf(listener -> !((Entry) listener).active);
            needsSweep = false;
        }
    }

    /** Removes every listener. For container removal. */
    public void clear() {
        for (Object listener : listeners) {
            ((Entry) listener).active = false;
        }
        if (firing) {
            needsSweep = true;
        } else {
            listeners.clear();
        }
    }

    private final class Entry implements Runnable, TickSubscription {

        private final Runnable listener;
        private boolean active = true;

        private Entry(Runnable listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            listener.run();
        }

        @Override
        public void unsubscribe() {
            if (!active) {
                return;
            }
            active = false;
            if (firing) {
                needsSweep = true;
            } else {
                listeners.remove(this);
            }
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
