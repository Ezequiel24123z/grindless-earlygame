package io.github.ezequiel24123z.grindless.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.progression.T1Progression;
import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Live teaching/data contracts only; not a machine run, client launch or saved-world pass. */
public final class VerifyT1Teaching {

    private static final Path LANG = Path.of("common", "src", "main", "resources", "assets",
            "grindless", "lang", "en_us.json");
    private static final Set<String> T0_IDS = Set.of(
            "starter_supplies", "quest_book_help", "superflat", "construction", "multitool", "manual_work",
            "crank", "extractor", "quern", "water", "clay_mixer", "grog", "firing", "ceramics",
            "geological_collector", "aggregate", "settling", "water_recovery", "mineral_feed",
            "biocultivator", "biomass", "fibreboard", "biomass_dynamo", "fluid_logistics", "conveyors",
            "workshop_flow", "assembly_bench", "research_terminal", "data_core", "calibrate",
            "calibrated_core", "relay");
    private static final Set<String> T1_IDS = Set.of(
            "thermal", "pylon", "first_grid", "scanner", "terrestrial", "pulverizer", "furnace",
            "press", "forming_dies", "copper_coil", "assembler", "matrix_line", "glass", "logistics",
            "kiln", "factory_world", "flux_cell", "industrial_matrix");
    private static final Set<String> NEW_LESSONS = Set.of(
            "first_grid", "forming_dies", "copper_coil", "flux_cell");
    // a64d44f: sorted key + NUL + decoded body + LF for the 33 retained T0 lesson keys.
    private static final String T0_BODY_DIGEST =
            "932c4c00e03bca58174247f90f9b41c08eda1a4caf5d4cb719de8c14c3e67941";
    private static int assertions;

    public static void main(String[] args) throws Exception {
        List<ProcessRecipe> processes = ProcessGraph.generate(List.of(
                new ProcessGraph.MaterialView("iron", true, true, true, true, true, true, true, true, true),
                new ProcessGraph.MaterialView("copper", true, true, true, true, true, true, true, true, true)));
        List<AtlasLogic.Entry> entries = AtlasLogic.allEntries(processes);
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        chapters();
        lessons(lang);
        selectors(entries);
        changedLiveData(entries);
        physicalRoutes(entries);
        unchangedT0(lang);
        System.out.println("ALL T1 TEACHING CONTRACT CHECKS PASSED: " + assertions
                + "; no native/client/save or successful-operation claim.");
    }

    private static void chapters() {
        equal("all T0 task identities remain exactly the deployed set", T0_IDS,
                QuestCatalogue.tasks().stream()
                        .filter(task -> QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T0)
                        .map(QuestCatalogue.Task::id).collect(Collectors.toSet()));
        equal("T1 adds only the four explicit teaching gaps", T1_IDS,
                QuestCatalogue.tasks().stream()
                        .filter(task -> QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1)
                        .map(QuestCatalogue.Task::id).collect(Collectors.toSet()));
        equal("no T2 chapter is activated", List.of(QuestCatalogue.Chapter.T0, QuestCatalogue.Chapter.T1),
                List.of(QuestCatalogue.Chapter.values()));
        for (String id : NEW_LESSONS) {
            var task = QuestCatalogue.byId(id);
            equal("new teaching is explicitly a lesson " + id, QuestCatalogue.Type.LESSON, task.type());
            equal("new teaching never mints a reward " + id, List.of(), task.rewards());
            check("new T1 nodes retain the real final-T0 projected anchor " + id,
                    QuestCatalogue.requirements(task).contains("relay"));
        }
        for (String id : List.of("first_grid", "forming_dies")) {
            equal("reading has no repeated component-possession tax " + id, QuestCatalogue.Evidence.FREE,
                    QuestCatalogue.byId(id).evidence());
        }
        for (String id : List.of("copper_coil", "flux_cell")) {
            equal("a physical checklist names the existing component " + id, "grindless:" + id,
                    QuestCatalogue.byId(id).subject());
            equal("a physical checklist is not a free acknowledgment " + id, QuestCatalogue.Evidence.ITEM,
                    QuestCatalogue.byId(id).evidence());
        }
    }

    private static void lessons(JsonObject lang) {
        Set<String> bodies = new TreeSet<>();
        for (String id : T1_IDS) {
            var task = QuestCatalogue.byId(id);
            var line = T1Progression.byQuest(id);
            String body = QuestTeaching.bodyKey(task);
            equal("the actual canonical line selects the lesson body " + id,
                    "gui.grindless.quest.lesson.t1." + line.id() + "." + id, body);
            check("T1 has a distinct concise lesson " + id, bodies.add(body) && lang.has(body)
                    && !lang.get(body).getAsString().isBlank() && lang.get(body).getAsString().length() <= 1400);
            var path = switch (line.requirement()) {
                case MANDATORY -> QuestTeaching.Path.T1_CORE;
                case OPTIONAL -> QuestTeaching.Path.T1_OPTIONAL;
                case EXIT -> QuestTeaching.Path.T1_EXIT;
            };
            equal("the canonical requirement selects the runtime path " + id, path, QuestTeaching.path(task));
            equal("the native optional marker follows the same requirement " + id,
                    line.requirement() == T1Progression.Requirement.OPTIONAL, QuestTeaching.optional(task));
            check("the actual route classification is localized " + id,
                    lang.has("gui.grindless.quest.path." + path.name().toLowerCase(Locale.ROOT)));
        }
        check("native navigation explicitly says in development",
                lang.get("gui.grindless.quest.chapter.t1_development").getAsString().contains("In development"));
        check("the shared disclaimer explicitly identifies the development state",
                lang.get("gui.grindless.quest.t1_development").getAsString().contains("T1 IN DEVELOPMENT"));
        check("Heracles uses an explicit development subtitle",
                lang.get("gui.grindless.quest.t1_line").getAsString().contains("T1 IN DEVELOPMENT"));
        equal("the Thermal Generator nominal rate is a live substitution", 1,
                substitutions(lang.get(QuestTeaching.bodyKey(QuestCatalogue.byId("thermal"))).getAsString()));
        equal("the grid compares actual Bootstrap and Voltaic rates", 2,
                substitutions(lang.get(QuestTeaching.bodyKey(QuestCatalogue.byId("first_grid"))).getAsString()));
        equal("pylon limits are live substitutions, not copied hardware numbers", 3,
                substitutions(lang.get("gui.grindless.quest.t1_pylon_limits").getAsString()));
        check("the Flux Cell lesson does not invent a charging gate",
                lang.get(QuestTeaching.bodyKey(QuestCatalogue.byId("flux_cell"))).getAsString()
                        .contains("does not require stored charge"));
    }

    private static void selectors(List<AtlasLogic.Entry> entries) {
        Map<String, AtlasLogic.Entry> live = entries.stream()
                .collect(Collectors.toMap(AtlasLogic.Entry::id, entry -> entry));
        for (String id : T1_IDS) {
            List<AtlasLogic.Entry> selected = QuestTeaching.recipes(QuestCatalogue.byId(id), entries);
            Set<String> expected = new TreeSet<>(T1Progression.questRouteIds(id));
            boolean nativeFuel = expected.remove(T1Progression.POWER_ROUTE);
            equal("the shared selector returns the exact canonical Atlas IDs " + id, expected,
                    selected.stream().map(AtlasLogic.Entry::id).collect(Collectors.toSet()));
            equal("no selected row is duplicated " + id, expected.size(), selected.size());
            equal("the known native fuel authority is explicit, never a fake recipe " + id,
                    nativeFuel ? Set.of(T1Progression.POWER_ROUTE) : Set.of(),
                    T1Progression.atlasForQuest(id, entries).nonAtlasRouteIds());
            for (AtlasLogic.Entry entry : selected) {
                check("teaching preserves the actual live row and every quantity " + id + "/" + entry.id(),
                        live.get(entry.id()) == entry);
                List<AtlasLogic.Entry> missing = entries.stream()
                        .filter(row -> !row.id().equals(entry.id())).toList();
                rejects("missing representable rows cannot silently disappear " + id + "/" + entry.id(),
                        () -> QuestTeaching.recipes(QuestCatalogue.byId(id), missing), entry.id());
                for (String deferred : List.of("mill/", "contact/", "pickle/", "wash/", "electrolysis/",
                        "assemble/wire_mill", "assemble/flux_exosuit", "assemble/flux_drill")) {
                    check("no deferred production class is activated by teaching " + id + "/" + deferred,
                            !entry.id().startsWith(deferred));
                }
            }
        }
    }

    private static void changedLiveData(List<AtlasLogic.Entry> entries) {
        AtlasLogic.Entry original = route(entries, "press/coil/copper");
        AtlasLogic.Entry changed = new AtlasLogic.Entry(original.id(), original.station(), original.family(),
                original.inputs(), List.of(original.outputs().get(0), original.outputs().get(0), original.outputs().get(0)),
                original.catalysts(), 321.0, "INERT", "STIRRED",
                original.durationTicks() + 1, original.fuPerTick() + 1);
        List<AtlasLogic.Entry> mutated = new ArrayList<>(entries);
        mutated.set(mutated.indexOf(original), changed);
        AtlasLogic.Entry projected = route(QuestTeaching.recipes(QuestCatalogue.byId("copper_coil"), mutated),
                original.id());
        check("a data mutation is observed without regenerating a quest bill", projected == changed);
        equal("the changed output batch stays live", changed.outputs(), projected.outputs());
        equal("the live unconsumed catalyst list is preserved", changed.catalysts(), projected.catalysts());
        String text = AtlasLogic.line(projected);
        for (String value : List.of("3x copper_coil", (original.durationTicks() + 1) + "t",
                (original.fuPerTick() + 1) + " FU/t", "321 C", "INERT", "STIRRED")) {
            check("the rendered row exposes the changed live data " + value, text.contains(value));
        }
        equal("the supplied original graph is not modified by projection", original,
                route(entries, original.id()));
    }

    private static void physicalRoutes(List<AtlasLogic.Entry> entries) {
        List<AtlasLogic.Entry> exit = QuestTeaching.recipes(QuestCatalogue.byId("industrial_matrix"), entries);
        AtlasLogic.Entry conversion = route(exit, T1Progression.EXIT_ROUTE);
        equal("the physical frontier uses the existing Assembler", MachineFamily.ASSEMBLER, conversion.family());
        check("the input carries the actual exact F1 rating",
                conversion.inputs().contains("item:grindless:relay_matrix [F1]"));
        equal("the output carries the actual F2 rating", List.of("item:grindless:relay_matrix [F2]"),
                conversion.outputs());
        equal("the cell lesson uses the actual shaped Flux Cell route", "CRAFTING_TABLE",
                route(QuestTeaching.recipes(QuestCatalogue.byId("flux_cell"), entries), "craft/flux_cell").station());
        check("the frontier contains the same real Flux Cell craft, not a Drill Cell",
                route(exit, "craft/flux_cell").outputs().contains("item:grindless:flux_cell"));
        for (String form : List.of("plate/iron", "rod/iron", "gear/iron", "coil/copper")) {
            AtlasLogic.Entry row = route(entries, "press/" + form);
            check("the forming die is actually an unconsumed catalyst " + form, !row.catalysts().isEmpty());
            check("the die is not also copied into consumed inputs " + form,
                    row.catalysts().stream().noneMatch(row.inputs()::contains));
        }
        Set<String> supply = QuestTeaching.recipes(QuestCatalogue.byId("glass"), entries).stream()
                .map(AtlasLogic.Entry::id).collect(Collectors.toSet());
        check("the lesson traces all established renewable upstream classes", supply.containsAll(Set.of(
                "source/geological_aggregate", "bootstrap/dry_aggregate", "bootstrap/wet_aggregate",
                "source/biomass", "bootstrap/biocarbon", "bootstrap/reduce_iron", "bootstrap/reduce_copper",
                "craft/hand_pump", "renew/sand", "renew/glass")));
    }

    private static void unchangedT0(JsonObject lang) throws Exception {
        StringBuilder snapshot = new StringBuilder();
        List<String> keys = lang.keySet().stream().filter(key -> key.startsWith("gui.grindless.quest.lesson.")
                        && !key.startsWith("gui.grindless.quest.lesson.t1.")
                        && !key.equals("gui.grindless.quest.lesson.t1_preview"))
                .sorted().toList();
        equal("all 33 deployed T0 lesson bodies, including the retained compatibility body, stay present",
                33, keys.size());
        for (String key : keys) {
            snapshot.append(key).append('\0').append(lang.get(key).getAsString()).append('\n');
        }
        equal("T0 lesson values remain byte-exact against a64d44f after JSON decoding", T0_BODY_DIGEST,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(snapshot.toString().getBytes(StandardCharsets.UTF_8))));
    }

    private static AtlasLogic.Entry route(List<AtlasLogic.Entry> entries, String id) {
        return entries.stream().filter(entry -> entry.id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing actual route: " + id));
    }

    private static int substitutions(String text) {
        return (text.length() - text.replace("%s", "").length()) / 2;
    }

    private static void rejects(String description, Runnable action, String missingId) {
        assertions++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            if (expected.getMessage().contains(missingId)) {
                return;
            }
            throw new AssertionError(description + ": wrong missing-route diagnostic", expected);
        }
        throw new AssertionError(description + ": a missing row was silently accepted");
    }

    private static void check(String description, boolean condition) {
        assertions++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void equal(String description, Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
