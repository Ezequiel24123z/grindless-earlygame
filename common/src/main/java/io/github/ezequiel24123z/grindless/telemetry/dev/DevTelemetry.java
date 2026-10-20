package io.github.ezequiel24123z.grindless.telemetry.dev;

import com.google.gson.Gson;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;
import io.github.ezequiel24123z.grindless.shop.EconomyGraph;
import io.github.ezequiel24123z.grindless.shop.ShopCatalogue;
import io.github.ezequiel24123z.grindless.shop.ShopResult;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.github.ezequiel24123z.grindless.telemetry.dev.TelemetrySession.Event.*;

/** Explicit operator opt-in; local aggregate logs only, with no identity, location or wall-clock data. */
public final class DevTelemetry {
    private static final Gson JSON = new Gson();
    private static final int REPORT_INTERVAL_TICKS = 1200;
    private static WeakReference<MinecraftServer> owner = new WeakReference<>(null);
    private static TelemetrySession session;
    private static long lastReport;
    private static boolean registered;

    private DevTelemetry() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        TickEvent.SERVER_POST.register(server -> {
            if (active(server) && tick(server) - lastReport >= REPORT_INTERVAL_TICKS) {
                report(server);
            }
        });
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            if (active(server)) {
                stop(server);
            }
        });
    }

    public static boolean active(MinecraftServer server) {
        return session != null && owner.get() == server;
    }

    public static void start(MinecraftServer server) {
        requireThread(server);
        if (active(server)) {
            return;
        }
        owner = new WeakReference<>(server);
        lastReport = tick(server);
        session = new TelemetrySession(lastReport, definitions(),
                QuestCatalogue.tasks().stream().map(QuestCatalogue.Task::id).collect(Collectors.toSet()));
        Grindless.LOG.info("Grindless local development telemetry enabled; anonymous aggregate ticks/counters only");
    }

    public static void stop(MinecraftServer server) {
        requireThread(server);
        if (!active(server)) {
            return;
        }
        report(server);
        session = null;
        owner.clear();
        Grindless.LOG.info("Grindless local development telemetry stopped; no persistent account or automatic restart");
    }

    public static TelemetrySession.Snapshot snapshot(MinecraftServer server) {
        requireThread(server);
        return active(server) ? session.snapshot(tick(server)) : null;
    }

    public static void report(MinecraftServer server) {
        requireThread(server);
        if (active(server)) {
            Grindless.LOG.info("Grindless local development telemetry {}", JSON.toJson(session.snapshot(tick(server))));
            lastReport = tick(server);
        }
    }

    public static void processCompleted(Level level, String process) {
        TelemetrySession target = session(level);
        if (target != null) {
            target.record(PROCESS_COMPLETED, process, 1);
        }
    }

    public static void machinePlaced(Level level, String machine) {
        TelemetrySession target = session(level);
        if (target != null) {
            target.record(MACHINE_PLACEMENT_EVENT, machineName(machine), 1);
        }
    }

    public static void manualWork(Level level, String machine, long acceptedFu) {
        TelemetrySession target = session(level);
        if (target != null) {
            String id = machineName(machine);
            target.record(MANUAL_ACTION, id, 1);
            if (acceptedFu > 0) {
                target.record(MANUAL_FU, id, acceptedFu);
            }
        }
    }

    public static void blocked(Level level, String machine, MachineStatus status) {
        TelemetrySession target = session(level);
        if (target != null && (status == MachineStatus.BLOCKED || status == MachineStatus.STARVED
                || status == MachineStatus.OUT_OF_BAND)) {
            String id = machineName(machine);
            target.record(BLOCKED_TRANSITION, id == null ? null : id + "/" + status.getSerializedName(), 1);
        }
    }

    public static void milestone(Level level, String questId) {
        TelemetrySession target = session(level);
        if (target != null) {
            target.milestone(questId, level.getServer().overworld().getGameTime());
        }
    }

    public static void shop(Level level, ShopCatalogue.Offer offer, ShopResult result) {
        TelemetrySession target = session(level);
        if (target == null || offer == null) {
            return;
        }
        if (result == ShopResult.TRADED) {
            target.record(SHOP_TRADED, offer.id(), 1);
            if (offer.role() == ShopCatalogue.Role.RECOVERY || offer.role() == ShopCatalogue.Role.EMERGENCY) {
                target.record(RECOVERY, offer.id(), 1);
            }
        } else {
            target.record(SHOP_REJECTED, offer.id() + "/" + result.name().toLowerCase(Locale.ROOT), 1);
        }
    }

    private static TelemetrySession session(Level level) {
        if (session == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        MinecraftServer server = serverLevel.getServer();
        return active(server) && server.isSameThread() ? session : null;
    }

    private static Map<TelemetrySession.Event, Set<String>> definitions() {
        Set<String> processes = EconomyGraph.rows().stream()
                .filter(row -> Set.of("BootstrapProcesses", "RenewableProcesses", "BootstrapAssembly", "ResearchLogic")
                        .contains(row.source()))
                .map(EconomyGraph.Row::id).collect(Collectors.toSet());
        Set<String> machines = BlockCatalogue.blocks().stream()
                .map(BlockCatalogue.Entry::name).collect(Collectors.toSet());
        Set<String> offers = ShopCatalogue.offers().stream().map(ShopCatalogue.Offer::id).collect(Collectors.toSet());
        Set<String> rejections = new TreeSet<>();
        offers.forEach(offer -> Arrays.stream(ShopResult.values()).forEach(result ->
                rejections.add(offer + "/" + result.name().toLowerCase(Locale.ROOT))));
        Set<String> blocked = new TreeSet<>();
        machines.forEach(machine -> Set.of("blocked", "starved", "out_of_band")
                .forEach(status -> blocked.add(machine + "/" + status)));
        Map<TelemetrySession.Event, Set<String>> definitions = new EnumMap<>(TelemetrySession.Event.class);
        definitions.put(PROCESS_COMPLETED, processes);
        definitions.put(MANUAL_ACTION, machines);
        definitions.put(MANUAL_FU, machines);
        definitions.put(SHOP_TRADED, offers);
        definitions.put(SHOP_REJECTED, rejections);
        definitions.put(RECOVERY, offers);
        definitions.put(BLOCKED_TRANSITION, blocked);
        definitions.put(MACHINE_PLACEMENT_EVENT, machines);
        return definitions;
    }

    private static long tick(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static String machineName(String machine) {
        return machine != null && machine.startsWith("grindless:")
                ? machine.substring("grindless:".length()) : machine;
    }

    private static void requireThread(MinecraftServer server) {
        if (!server.isSameThread()) {
            throw new IllegalStateException("Development telemetry control must run on the server thread");
        }
    }
}
