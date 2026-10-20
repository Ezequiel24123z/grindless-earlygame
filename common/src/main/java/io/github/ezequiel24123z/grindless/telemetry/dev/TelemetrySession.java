package io.github.ezequiel24123z.grindless.telemetry.dev;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Bounded anonymous counters. Definitions come from catalogues, never player-supplied labels. */
public final class TelemetrySession {
    public enum Event {
        PROCESS_COMPLETED, MANUAL_ACTION, MANUAL_FU, SHOP_TRADED, SHOP_REJECTED, RECOVERY, BLOCKED_TRANSITION,
        MACHINE_PLACEMENT_EVENT
    }

    public record Snapshot(int schemaVersion, long elapsedTicks, Map<String, Long> counters,
                           Map<String, Long> firstMilestoneTicks, long rejectedRecords, boolean counterOverflow) {
        public Snapshot {
            counters = Collections.unmodifiableMap(new TreeMap<>(counters));
            firstMilestoneTicks = Collections.unmodifiableMap(new TreeMap<>(firstMilestoneTicks));
        }
    }

    private final long startTick;
    private final Map<Event, Set<String>> definitions;
    private final Set<String> milestones;
    private final Map<String, Long> counters = new TreeMap<>();
    private final Map<String, Long> firstMilestoneTicks = new TreeMap<>();
    private long rejectedRecords;
    private boolean overflow;

    public TelemetrySession(long startTick, Map<Event, Set<String>> definitions, Set<String> milestones) {
        this.startTick = startTick;
        Map<Event, Set<String>> checked = new EnumMap<>(Event.class);
        definitions.forEach((event, ids) -> checked.put(event, Set.copyOf(ids)));
        this.definitions = Map.copyOf(checked);
        this.milestones = Set.copyOf(milestones);
    }

    public void record(Event event, String definition, long amount) {
        if (event == null || definition == null || amount <= 0
                || !definitions.getOrDefault(event, Set.of()).contains(definition)) {
            rejectedRecords = add(rejectedRecords, 1);
            return;
        }
        String key = event.name().toLowerCase(java.util.Locale.ROOT) + ":" + definition;
        counters.put(key, add(counters.getOrDefault(key, 0L), amount));
    }

    public void milestone(String id, long tick) {
        if (id == null || !milestones.contains(id)) {
            rejectedRecords = add(rejectedRecords, 1);
            return;
        }
        firstMilestoneTicks.putIfAbsent(id, elapsed(tick));
    }

    public Snapshot snapshot(long tick) {
        return new Snapshot(1, elapsed(tick), counters, firstMilestoneTicks, rejectedRecords, overflow);
    }

    private long elapsed(long tick) {
        if (tick < startTick) {
            return 0;
        }
        if (startTick < 0 && tick > Long.MAX_VALUE + startTick) {
            overflow = true;
            return Long.MAX_VALUE;
        }
        return tick - startTick;
    }

    private long add(long current, long amount) {
        if (current > Long.MAX_VALUE - amount) {
            overflow = true;
            return Long.MAX_VALUE;
        }
        return current + amount;
    }
}
