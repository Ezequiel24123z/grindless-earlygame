package io.github.ezequiel24123z.grindless.telemetry.dev;

import com.google.gson.Gson;

import java.util.Map;
import java.util.Set;

import static io.github.ezequiel24123z.grindless.telemetry.dev.TelemetrySession.Event.*;

/** Relative milestone/counter behavior and the absence of arbitrary personal-data fields. */
public final class VerifyDevTelemetry {
    private static int assertions;

    private VerifyDevTelemetry() {
    }

    public static void main(String[] args) {
        equal("telemetry is disabled until an explicit server/operator start", false, DevTelemetry.active(null));
        DevTelemetry.processCompleted(null, "not-a-definition");
        DevTelemetry.manualWork(null, null, -1);
        DevTelemetry.milestone(null, "not-a-definition");
        equal("disabled hooks neither start nor retain a session", false, DevTelemetry.active(null));
        TelemetrySession session = new TelemetrySession(100, Map.of(
                PROCESS_COMPLETED, Set.of("bootstrap/sand"),
                MANUAL_ACTION, Set.of("quern"),
                MANUAL_FU, Set.of("quern"),
                SHOP_TRADED, Set.of("stone"),
                RECOVERY, Set.of("stone"),
                BLOCKED_TRANSITION, Set.of("quern/blocked"),
                MACHINE_PLACEMENT_EVENT, Set.of("quern")), Set.of("relay"));
        session.record(PROCESS_COMPLETED, "bootstrap/sand", 2);
        session.record(MANUAL_ACTION, "quern", 1);
        session.record(MANUAL_FU, "quern", 160);
        session.record(SHOP_TRADED, "stone", 1);
        session.record(RECOVERY, "stone", 1);
        session.record(BLOCKED_TRANSITION, "quern/blocked", 1);
        session.record(MACHINE_PLACEMENT_EVENT, "quern", 1);
        session.milestone("relay", 350);
        session.milestone("relay", 450);
        TelemetrySession.Snapshot snapshot = session.snapshot(500);
        equal("time is relative to explicit opt-in", 400L, snapshot.elapsedTicks());
        equal("first milestone is stable across players/repeated observations", Map.of("relay", 250L), snapshot.firstMilestoneTicks());
        equal("completed processes are exact aggregate counts", 2L, snapshot.counters().get("process_completed:bootstrap/sand"));
        equal("accepted manual FU is distinct from action count", 160L, snapshot.counters().get("manual_fu:quern"));
        equal("placement observations are explicit, not unique-world machine claims", 1L,
                snapshot.counters().get("machine_placement_event:quern"));
        equal("reporting never resets ongoing counts", snapshot, session.snapshot(500));
        session.record(PROCESS_COMPLETED, "personal-name-or-coordinate", 1);
        session.milestone("arbitrary-private-text", 600);
        session.record(MANUAL_FU, "quern", -1);
        equal("unknown/invalid observations are explicit bounded diagnostics", 3L, session.snapshot(600).rejectedRecords());
        String json = new Gson().toJson(session.snapshot(600));
        equal("arbitrary input never becomes a log field", false, json.contains("personal-name-or-coordinate"));
        equal("arbitrary milestones never become a log field", false, json.contains("arbitrary-private-text"));
        for (String field : Set.of("uuid", "player", "username", "position", "coordinates", "address",
                "worldName", "worldSeed", "host", "timestamp", "sessionId")) {
            equal("no identifying field " + field, false, json.contains("\"" + field + "\""));
        }
        equal("tick rewind cannot generate negative elapsed time", 0L, session.snapshot(50).elapsedTicks());
        TelemetrySession newSession = new TelemetrySession(900, Map.of(), Set.of());
        equal("new explicit session starts empty", Map.of(), newSession.snapshot(900).counters());
        equal("new explicit session has no inherited milestones", Map.of(), newSession.snapshot(900).firstMilestoneTicks());
        session.record(PROCESS_COMPLETED, "bootstrap/sand", Long.MAX_VALUE);
        equal("counter overflow is bounded and visible", true, session.snapshot(600).counterOverflow());
        equal("counter cannot wrap negative", Long.MAX_VALUE,
                session.snapshot(600).counters().get("process_completed:bootstrap/sand"));
        equal("previous snapshots are immutable", 2L, snapshot.counters().get("process_completed:bootstrap/sand"));
        for (int i = 0; i < 1000; i++) {
            session.record(PROCESS_COMPLETED, "invalid-" + i, 1);
        }
        equal("unknown labels cannot grow the counter map", 7, session.snapshot(600).counters().size());
        System.out.println("ALL DEV TELEMETRY CHECKS PASSED: " + assertions);
    }

    private static void equal(String name, Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }
}
