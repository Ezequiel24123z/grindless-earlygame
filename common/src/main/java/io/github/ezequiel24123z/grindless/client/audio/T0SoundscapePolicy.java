package io.github.ezequiel24123z.grindless.client.audio;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Bounded workshop mix and sparse observed-state edges; no native playback or world references. */
final class T0SoundscapePolicy<S> {
    static final int MAX_LOOPS = 8;
    static final int MAX_ONSETS = 4;
    private final Map<Long, WeakReference<S>> running = new HashMap<>();

    static <S> Map<Long, S> nearest(Map<Long, S> eligible, ToDoubleFunction<S> distanceSquared) {
        Map<Long, S> audible = new LinkedHashMap<>();
        eligible.entrySet().stream().sorted(Comparator
                .<Map.Entry<Long, S>>comparingDouble(entry -> distanceSquared.applyAsDouble(entry.getValue()))
                .thenComparingLong(Map.Entry::getKey)).limit(MAX_LOOPS)
                .forEach(entry -> audible.put(entry.getKey(), entry.getValue()));
        return audible;
    }

    Map<Long, S> onsets(Map<Long, S> current) {
        Map<Long, S> events = new LinkedHashMap<>();
        running.keySet().retainAll(current.keySet());
        current.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            WeakReference<S> previous = running.get(entry.getKey());
            if (previous == null || previous.get() != entry.getValue()) {
                if (events.size() < MAX_ONSETS) {
                    events.put(entry.getKey(), entry.getValue());
                }
                // Observe even a mix-suppressed edge; never queue a burst across later ticks.
                running.put(entry.getKey(), new WeakReference<>(entry.getValue()));
            }
        });
        return events;
    }

    void clear() {
        running.clear();
    }
}
