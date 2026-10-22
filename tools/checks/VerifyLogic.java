package io.github.ezequiel24123z.grindless.logic;

import io.github.ezequiel24123z.grindless.machine.AssemblerWork;
import io.github.ezequiel24123z.grindless.recipe.ProcessLogic;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Behaviour checks for logic numbers and recipe-owned base-work. Not part of the mod. */
public final class VerifyLogic {

    private static int failures = 0;

    public static void main(String[] args) {
        numbers();
        assemblerWork();
        System.out.println(failures == 0
                ? "ALL LOGIC CHECKS PASSED"
                : failures + " LOGIC CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void numbers() {
        eq("the example threshold is five hundred", 500, SignalLogic.THRESHOLD);
        eq("run writes one", 1, SignalLogic.RUN);
        eq("hold writes zero", 0, SignalLogic.HOLD);
        yes("four hundred ninety-nine still runs", SignalLogic.shouldRun(499, SignalLogic.THRESHOLD));
        no("five hundred holds", SignalLogic.shouldRun(500, SignalLogic.THRESHOLD));
        no("above the threshold holds", SignalLogic.shouldRun(501, SignalLogic.THRESHOLD));
        eq("a quiet cable emits nothing", 0, SignalLogic.toRedstone(0));
        eq("a negative cable emits nothing", 0, SignalLogic.toRedstone(-3));
        eq("one on the cable is one on the wire", 1, SignalLogic.toRedstone(1));
        eq("a large cable is still a full redstone line", 15, SignalLogic.toRedstone(20));
        eq("no redstone writes zero", 0, SignalLogic.fromRedstone(0));
        eq("seven stays seven", 7, SignalLogic.fromRedstone(7));
        eq("redstone above fifteen is clipped", 15, SignalLogic.fromRedstone(20));
    }

    private static void assemblerWork() {
        // Pure ledger identities, not registered recipes or accepted Survival manufacture.
        String partial = "unit/work/partial";
        String blocked = "unit/work/blocked";
        String fractional = "unit/work/fractional";
        AssemblerWork work = new AssemblerWork();
        yes("a fresh machine is unselected AUTO", work.selected().isEmpty() && work.suspended().isEmpty());
        work.select(partial);
        double paid = 17.0 + ProcessLogic.work(13L, 32L, 0.75, 1.3);
        double fraction = ProcessLogic.work(1L, 32L, 0.5, 0.8);
        Map<String, Double> expected = new LinkedHashMap<>();
        expected.put(partial, paid);
        expected.put(blocked, 160.0);
        expected.put(fractional, fraction);
        String active = partial;
        for (String id : List.of(blocked, fractional)) {
            double resumed = work.activate(active, paid, id);
            exact("a fresh target never inherits another target's paid work: " + id, 0.0, resumed);
            active = id;
            paid = expected.get(id);
        }
        for (int pass = 0; pass < 120; pass++) {
            String next = List.of(partial, blocked, fractional).get(pass % 3);
            paid = work.activate(active, paid, next);
            active = next;
            work.select(next);
            exact("switch " + pass + " restores the exact recipe-owned double", expected.get(active), paid);
            yes("switch " + pass + " has exactly one active owner", !work.suspended().containsKey(active));
            Map<String, Double> all = new LinkedHashMap<>(work.suspended());
            all.put(active, paid);
            yes("switch " + pass + " preserves all three balances without duplication", all.equals(expected));
            CompoundTag checkpoint = new CompoundTag();
            checkpoint.putDouble("Progress", paid);
            checkpoint.putString("WorkRecipe", active);
            work.save(checkpoint);
            work = AssemblerWork.load(checkpoint, active, paid);
            yes("switch " + pass + " reloads a valid exact target", work.valid() && work.selected().equals(active));
        }
        work.select("");
        exact("selecting AUTO does not debit active work", expected.get(active), paid);
        String previous = active;
        String next = previous.equals(partial) ? blocked : partial;
        paid = work.activate(previous, paid, next);
        exact("AUTO's chosen ID also resumes only its own paid balance", expected.get(next), paid);

        AssemblerWork legacy = new AssemblerWork();
        exact("unattributed legacy work cannot pay a new recipe", 0.0, legacy.activate("", 0.125, partial));
        exact("unattributed legacy work is retained under its empty owner", 0.125, legacy.suspended().get(""));
        exact("a zero idle owner does not consume retained unattributed work", 0.0,
                legacy.activate(partial, 0.0, blocked));
        AssemblerWork unknown = new AssemblerWork();
        exact("unknown paid work cannot pay a known ID", 0.0, unknown.activate("removed/recipe", 25.375, partial));
        exact("the exact unknown ID can recover its own work when it returns", 25.375,
                unknown.activate(partial, 0.0, "removed/recipe"));

        AssemblerWork completed = new AssemblerWork();
        double carry = 160.0 + fraction - 160.0;
        exact("a successful fractional carry can be suspended without an idle-only dead end", 0.0,
                completed.activate(partial, carry, blocked));
        exact("fractional post-completion work returns bit-exactly", carry,
                completed.activate(blocked, 0.0, partial));
        exact("selecting the current recipe cannot duplicate fractional carry", carry,
                completed.activate(partial, carry, partial));

        AssemblerWork history = new AssemblerWork();
        String historical = "removed/history/0";
        for (int index = 1; index <= 4100; index++) {
            String nextHistory = "removed/history/" + index;
            history.activate(historical, 0.125, nextHistory);
            historical = nextHistory;
        }
        exact("unavailable history cannot impose a paid-job cap on a new selected target", 0.0,
                history.activate(historical, 0.125, partial));
        exact("fractional work remains switchable even beside thousands of unavailable old IDs", 0.0,
                history.activate(partial, 0.25, blocked));
        exact("that fractional work remains exactly recoverable without deleting unavailable history", 0.25,
                history.activate(blocked, 0.0, partial));
        yes("all unavailable history remains retained", history.suspended().size() == 4101);
    }

    private static void exact(String what, double expected, double actual) {
        if (Double.doubleToRawLongBits(expected) != Double.doubleToRawLongBits(actual)) {
            fail(what + ": expected exact " + expected + " got " + actual);
        }
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void no(String what, boolean actual) {
        if (!actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected false");
        }
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
