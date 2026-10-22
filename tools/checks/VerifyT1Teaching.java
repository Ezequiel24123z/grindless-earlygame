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
            "kiln", "factory_world", "flux_cell", "industrial_matrix", "wet_beneficiation");
    private static final Set<String> NEW_LESSONS = Set.of(
            "first_grid", "forming_dies", "copper_coil", "flux_cell", "wet_beneficiation");
    private static final Set<String> WIRE_FORMING_ROUTES = Set.of(
            "craft/wire_mill", "mill/wire/iron", "mill/wire/copper", "mill/coil/copper");
    private static final Set<String> WET_ROUTES = Set.of(
            "assemble/motor", "craft/chemical_washer", "b2/iron", "b2/copper",
            "b2_r1/iron", "b2_r1/copper");
    // dd0b4bc: sorted key + NUL + decoded body + LF for the 33 native FTB T0 lesson keys.
    private static final String T0_BODY_DIGEST =
            "bf93a3d61465ed953cb33eea9de7b061e4f8f35dc36a68eb5393df3214424391";
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
        equal("the native identity set retains all 50 tasks and adds one rewardless wet lesson",
                51, QuestCatalogue.tasks().size());
        equal("all T0 task identities remain exactly the deployed set", T0_IDS,
                QuestCatalogue.tasks().stream()
                        .filter(task -> QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T0)
                        .map(QuestCatalogue.Task::id).collect(Collectors.toSet()));
        equal("T1 adds only the accepted optional wet lesson to its prior 18 objectives", T1_IDS,
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
        var wet = QuestCatalogue.byId("wet_beneficiation");
        equal("the wet lesson appends without moving any old catalogue ordinal", wet,
                QuestCatalogue.tasks().get(50));
        equal("the wet lesson follows only the Assembler", List.of("assembler"), wet.requires());
        equal("the wet lesson observes actual Washer possession", QuestCatalogue.Evidence.ITEM, wet.evidence());
        equal("the wet lesson names the existing machine item", "grindless:chemical_washer", wet.subject());
        equal("the wet lesson asks for one machine, not a copied construction bill", 1, wet.count());
        equal("the wet lesson stays on its optional path", QuestTeaching.Path.T1_OPTIONAL, QuestTeaching.path(wet));
    }

    private static void lessons(JsonObject lang) {
        Set<String> bodies = new TreeSet<>();
        for (String id : T1_IDS) {
            var task = QuestCatalogue.byId(id);
            var line = T1Progression.byQuest(id);
            String body = QuestTeaching.bodyKey(task);
            equal("the actual canonical line selects the lesson body " + id,
                    "gui.grindless.quest.lesson.t1." + line.id() + "." + id, body);
            check("T1 has a distinct lesson " + id, bodies.add(body) && lang.has(body));
            String text = lang.get(body).getAsString();
            check("native T1 copy stays within 42 words " + id,
                    !text.isBlank() && text.strip().split("\\s+").length <= 42);
            String lower = text.toLowerCase(Locale.ROOT);
            check("native T1 copy never promises embedded recipe rows " + id,
                    !lower.contains("atlas") && !lower.contains("live rows") && !lower.contains("rows below"));
            if (!id.equals("first_grid")) {
                check("ordinary T1 recipes belong to JEI " + id, text.contains("JEI"));
            }
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
        check("the retained development subtitle remains explicit",
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
        String cell = lang.get(QuestTeaching.bodyKey(QuestCatalogue.byId("flux_cell"))).getAsString();
        check("the Flux Cell lesson presents actual dual methods as choices, not a forced duplicate",
                cell.contains("by hand or in the Assembler") && cell.contains("Choose either route"));
        String assembler = lang.get(QuestTeaching.bodyKey(QuestCatalogue.byId("assembler"))).getAsString();
        check("the Assembler lesson distinguishes manual machines from optional automatic components",
                assembler.contains("Manually assemble") && assembler.contains("when useful")
                        && assembler.contains("Normal machines stay manually assembled")
                        && assembler.contains("not required for the first F2 Matrix"));
        String matrices = lang.get(QuestTeaching.bodyKey(QuestCatalogue.byId("matrix_line"))).getAsString();
        check("the Matrix lesson preserves both actual F1 methods and the machine-only frontier",
                matrices.contains("by hand or in the Assembler") && matrices.contains("machine-only F2"));
        equal("the Assembler objective remains mandatory despite its optional expansion",
                QuestTeaching.Path.T1_CORE, QuestTeaching.path(QuestCatalogue.byId("assembler")));
        String wet = lang.get(QuestTeaching.bodyKey(QuestCatalogue.byId("wet_beneficiation"))).getAsString();
        check("wet native teaching has at most 40 words", wet.strip().split("\\s+").length <= 40);
        equal("the wet title matches acquisition evidence", "Acquire a Chemical Washer",
                lang.get("gui.grindless.quest.task.wet_beneficiation").getAsString());
        check("the wet lesson starts with possession, not a claimed successful operation",
                wet.startsWith("Acquire a Chemical Washer by manual assembly."));
        for (String concept : List.of("water", "crushed feed", "washed feed", "secondary ingot",
                "Reduce", "Arc Furnace", "JEI", "optional")) {
            check("short wet teaching explains " + concept, wet.contains(concept));
        }
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
                List<AtlasLogic.Entry> duplicate = new ArrayList<>(entries);
                duplicate.add(entry);
                rejects("duplicate live rows cannot duplicate teaching " + id + "/" + entry.id(),
                        () -> QuestTeaching.recipes(QuestCatalogue.byId(id), duplicate), entry.id());
                for (String deferred : List.of("mill/", "contact/", "pickle/", "wash/", "electrolysis/",
                        "craft/wire_mill", "assemble/flux_exosuit", "assemble/flux_drill")) {
                    check("only the bounded optional base wire routes bypass deferred exclusions "
                                    + id + "/" + deferred,
                            !entry.id().startsWith(deferred)
                                    || (id.equals("assembler") && WIRE_FORMING_ROUTES.contains(entry.id())));
                }
            }
        }
    }

    private static void changedLiveData(List<AtlasLogic.Entry> entries) {
        changedLiveData(entries, "press/coil/copper", "copper_coil");
        for (String id : WIRE_FORMING_ROUTES) {
            changedLiveData(entries, id, "assembler");
        }
        for (String id : WET_ROUTES) {
            changedLiveData(entries, id, "wet_beneficiation");
        }
    }

    private static void changedLiveData(List<AtlasLogic.Entry> entries, String routeId, String taskId) {
        AtlasLogic.Entry original = route(entries, routeId);
        AtlasLogic.Entry changed = new AtlasLogic.Entry(original.id(), original.station(), original.family(),
                original.inputs(), List.of(original.outputs().get(0), original.outputs().get(0), original.outputs().get(0)),
                original.catalysts(), 321.0, "INERT", "STIRRED",
                original.durationTicks() + 1, original.fuPerTick() + 1);
        List<AtlasLogic.Entry> mutated = new ArrayList<>(entries);
        mutated.set(mutated.indexOf(original), changed);
        AtlasLogic.Entry projected = route(QuestTeaching.recipes(QuestCatalogue.byId(taskId), mutated),
                original.id());
        check("a data mutation is observed without regenerating a quest bill", projected == changed);
        equal("the changed output batch stays live", changed.outputs(), projected.outputs());
        equal("the live unconsumed catalyst list is preserved", changed.catalysts(), projected.catalysts());
        equal("the exact mutated tick duration stays live", changed.durationTicks(), projected.durationTicks());
        String durationText = changed.durationTicks() % 20 == 0
                ? (changed.durationTicks() / 20) + "s"
                : String.format(Locale.ROOT, "%.1fs", changed.durationTicks() / 20.0);
        String text = AtlasLogic.line(projected);
        for (String value : List.of(AtlasLogic.describe(changed.outputs()), durationText,
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
        check("the first F2 lesson never requires the optional wire-forming branch",
                exit.stream().noneMatch(row -> WIRE_FORMING_ROUTES.contains(row.id())));
        check("the first F2 lesson never requires the optional wet branch",
                exit.stream().noneMatch(row -> WET_ROUTES.contains(row.id())));
        List<AtlasLogic.Entry> wet = QuestTeaching.recipes(QuestCatalogue.byId("wet_beneficiation"), entries);
        equal("wet teaching selects exactly the six real Motor/Washer/B2/B2-R1 authorities", WET_ROUTES,
                wet.stream().map(AtlasLogic.Entry::id).collect(Collectors.toSet()));
        equal("the wet acquisition route produces the actual Washer item",
                List.of("item:grindless:chemical_washer"), route(wet, "craft/chemical_washer").outputs());
        equal("the Washer is manually assembled rather than a live automatic final product",
                "CRAFTING_TABLE", route(wet, "craft/chemical_washer").station());
        for (String material : List.of("iron", "copper")) {
            AtlasLogic.Entry wash = route(wet, "b2/" + material);
            equal("wet teaching uses the actual Washer family " + material,
                    MachineFamily.CHEMICAL_WASHER, wash.family());
            check("wet teaching retains the actual fluid input " + material,
                    wash.inputs().contains("fluid:minecraft:water"));
            check("wet teaching retains the washed primary output " + material,
                    wash.outputs().contains("tag:grindless:washed_crushed_materials/" + material));
            equal("wet teaching retains both distinct output streams " + material, 2L,
                    wash.outputs().stream().distinct().count());
            check("wet reduction consumes washed feed, not the old crushed input " + material,
                    route(wet, "b2_r1/" + material).inputs()
                            .contains("tag:grindless:washed_crushed_materials/" + material));
        }
        List<AtlasLogic.Entry> assembler = QuestTeaching.recipes(QuestCatalogue.byId("assembler"), entries);
        AtlasLogic.Entry mill = route(assembler, "craft/wire_mill");
        equal("the optional base Wire Mill is manually assembled", "CRAFTING_TABLE", mill.station());
        check("the optional base Wire Mill teaches minimum control, not an exact predecessor",
                mill.inputs().contains("item:grindless:relay_matrix [F1+]"));
        equal("the optional base Wire Mill remains an ordinary output, not MK2",
                List.of("item:grindless:wire_mill"), mill.outputs());
        for (String id : List.of("mill/wire/iron", "mill/wire/copper", "mill/coil/copper")) {
            equal("the optional forming row retains its actual Wire Mill identity " + id,
                    MachineFamily.WIRE_MILL, route(assembler, id).family());
        }
        equal("the first Assembler coil path still uses only the Press and its die",
                Set.of("craft/coil_die", "press/coil/copper"),
                QuestTeaching.recipes(QuestCatalogue.byId("copper_coil"), entries).stream()
                        .map(AtlasLogic.Entry::id).collect(Collectors.toSet()));
        check("F1 teaching includes both existing actual manufacturing methods",
                QuestTeaching.recipes(QuestCatalogue.byId("matrix_line"), entries).stream().map(AtlasLogic.Entry::id)
                        .collect(Collectors.toSet()).containsAll(Set.of(T1Progression.ENTRY_ROUTE, T1Progression.REPRODUCTION_ROUTE)));
        Set<String> bench = QuestTeaching.recipes(QuestCatalogue.byId("assembly_bench"), entries).stream()
                .map(AtlasLogic.Entry::id).collect(Collectors.toSet());
        check("Bench teaching retains its component routes", bench.containsAll(Set.of("bench/data_core", "bench/crank_handle")));
        check("Bench teaching selects real manual normal-machine counterparts",
                bench.containsAll(Set.of("craft/hand_pump", "craft/biocultivator")));
        check("retired automatic normal-machine targets are not taught", !bench.contains("bench/hand_pump")
                && !bench.contains("bench/biocultivator"));
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
        equal("T0 lesson values remain byte-exact against dd0b4bc after JSON decoding", T0_BODY_DIGEST,
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
