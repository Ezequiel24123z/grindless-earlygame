package io.github.ezequiel24123z.grindless.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.material.SupplyCatalogue;
import io.github.ezequiel24123z.grindless.progression.T1Progression;
import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.BootstrapProcesses;
import io.github.ezequiel24123z.grindless.recipe.RenewableProcesses;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortContract;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortRole;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Quest teaching, chapter boundaries and legacy identity checks. Not part of the mod. */
public final class VerifyQuest {

    private static final Path ROOT = Path.of(".");
    private static final Path LANG = Path.of("common/src/main/resources/assets/grindless/lang/en_us.json");
    private static final Path RECIPES = Path.of("common/src/main/resources/data/grindless/recipes");
    private static final Path SOURCES = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");
    private static final Path FORGE_SOURCES = Path.of("forge/src/main/java/io/github/ezequiel24123z/grindless");

    private static final List<String> LINES = List.of(
            "bootstrap", "workshop", "renewable_supply", "settling", "bioculture", "workshop_logistics",
            "relay", "power", "extraction", "factory", "renewables", "logistics", "factory_world",
            "industrial", "wet_beneficiation", "gas_utilities");
    private static final List<String> LEGACY_IDS = List.of(
            "multitool", "crank", "extractor", "quern", "water", "clay_mixer", "ceramics",
            "workshop_flow", "settling", "calibrate", "relay", "thermal", "pylon", "scanner",
            "terrestrial", "pulverizer", "furnace", "press", "assembler", "matrix_line", "glass",
            "logistics", "kiln", "factory_world", "industrial_matrix");

    private static final List<String> FORBIDDEN = List.of(
            "betterquesting", "patchouli", "funwayguy", "draconic", "galacticraft",
            "advancedrocketry", "ad astra", "gregtech");

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        graph();
        starter();
        route();
        claims();
        evidence();
        rewards();
        guide();
        items();
        noCopies();

        System.out.println(failures == 0
                ? "ALL QUEST CHECKS PASSED"
                : failures + " QUEST CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void graph() {
        List<QuestCatalogue.Task> tasks = QuestCatalogue.tasks();
        List<String> problems = QuestLogic.problems(tasks);
        yes("the catalogue is a book" + (problems.isEmpty() ? "" : " " + problems), problems.isEmpty());
        eq("thirty-two unchanged T0 tasks and twenty in-development T1 tasks", 52, tasks.size());
        eq("the complete T0 chapter still has thirty-two tasks", 32L, tasks.stream()
                .filter(task -> QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T0).count());
        eq("all source-defined lines remain explicit", LINES, QuestCatalogue.lines());
        eq("the T0 chapter has its own seven lines", LINES.subList(0, 7),
                QuestCatalogue.lines(QuestCatalogue.Chapter.T0));
        eq("the T1 chapter appends optional lines after its unchanged seven lines", LINES.subList(7, 16),
                QuestCatalogue.lines(QuestCatalogue.Chapter.T1));
        yes("the first task has no dependency", tasks.get(0).requires().isEmpty());
        eq("the old final task retains its ordinal and physical Industrial hand-off",
                "industrial_matrix", tasks.get(49).id());
        eq("the new optional wet lesson appends without moving existing tasks",
                "wet_beneficiation", tasks.get(50).id());
        eq("the optional gas lesson appends after all 51 old tasks", "gas_utilities", tasks.get(51).id());
        eq("one task per id", tasks.size(),
                (int) tasks.stream().map(QuestCatalogue.Task::id).distinct().count());
        Set<String> old = new HashSet<>(LEGACY_IDS);
        old.add(QuestCatalogue.STARTER_SUPPLIES);
        eq("all 26 original rewarded IDs remain; new teaching never mints a material reward", old,
                tasks.stream().filter(task -> task.type() == QuestCatalogue.Type.REWARD)
                        .map(QuestCatalogue.Task::id).collect(java.util.stream.Collectors.toSet()));
        eq("all twenty old T0 lessons and six T1 lessons are rewardless", 26L, tasks.stream()
                .filter(task -> task.type() == QuestCatalogue.Type.LESSON && task.rewards().isEmpty()).count());
        try {
            task("quest_book_help").reward();
            fail("the compatibility accessor cannot invent a lesson reward");
        } catch (IllegalStateException expected) {
            yes("requesting a material reward from a lesson fails explicitly", expected.getMessage().contains("no material reward"));
        }
        eq("old evidence ordinals do not move", List.of(0, 1, 2), List.of(
                QuestCatalogue.Evidence.ITEM.ordinal(), QuestCatalogue.Evidence.DIMENSION.ordinal(),
                QuestCatalogue.Evidence.MATRIX.ordinal()));
        eq("status wire meanings do not move", List.of(0, 1, 2, 3),
                java.util.Arrays.stream(QuestLogic.Status.values()).map(Enum::ordinal).toList());
        eq("old rewards and prerequisites remain exact except the explicitly optional Factory Portal edge", List.of(
                "multitool||minecraft:clay_ball/4",
                "crank|multitool|minecraft:coal/4",
                "extractor|crank|minecraft:gravel/8",
                "quern|multitool|minecraft:brick/4",
                "water|quern|minecraft:stick/4",
                "clay_mixer|quern,water|minecraft:clay_ball/8",
                "ceramics|clay_mixer|minecraft:charcoal/4",
                "workshop_flow|ceramics|minecraft:stick/8",
                "settling|ceramics,water|minecraft:dirt/16",
                "calibrate|ceramics|grindless:process_atlas/1",
                "relay|calibrate|minecraft:flint/2",
                "thermal|relay|minecraft:coal/16",
                "pylon|thermal|minecraft:redstone/4",
                "scanner|pylon|minecraft:charcoal/4",
                "terrestrial|scanner,thermal|grindless:data_core/1",
                "pulverizer|thermal|minecraft:cobblestone/32",
                "furnace|thermal,pulverizer|minecraft:coal/8",
                "press|furnace|minecraft:iron_ingot/4",
                "assembler|press,furnace|grindless:data_core/1",
                "matrix_line|assembler|minecraft:redstone/8",
                "glass|pulverizer,furnace|minecraft:clay_ball/16",
                "logistics|assembler|minecraft:hopper/1",
                "kiln|matrix_line|minecraft:brick/8",
                "factory_world|matrix_line,glass|minecraft:grass_block/16",
                "industrial_matrix|matrix_line,glass|minecraft:iron_ingot/8"),
                LEGACY_IDS.stream().map(QuestCatalogue::byId).map(task -> task.id() + "|"
                        + String.join(",", task.requires()) + "|" + task.rewards().stream()
                        .map(reward -> reward.itemId() + "/" + reward.count())
                        .collect(java.util.stream.Collectors.joining(","))).toList());
    }

    private static void starter() {
        QuestCatalogue.Task starter = QuestCatalogue.tasks().get(0);
        eq("the welcome task is the first visible task", QuestCatalogue.STARTER_SUPPLIES, starter.id());
        eq("the starter belongs to the existing first line", "bootstrap", starter.line());
        eq("the starter has an explicit free goal", QuestCatalogue.Evidence.FREE, starter.evidence());
        yes("the starter has no prerequisite", starter.requires().isEmpty());
        yes("the starter has no hidden resource subject", starter.subject().isEmpty());
        eq("free means ready without evidence", QuestLogic.Status.CLAIMABLE,
                QuestLogic.consider(starter, Set.of(), false));
        yes("free evidence needs no items or dimension", QuestEvidence.met(starter, 0, ""));
        eq("a claimed starter remains claimed", QuestLogic.Status.CLAIMED,
                QuestLogic.consider(starter, Set.of(QuestCatalogue.STARTER_SUPPLIES), false));
        eq("the generous allocation consists of eleven legal stacks", 11, starter.rewards().size());
        Map<String, Integer> actual = new TreeMap<>();
        starter.rewards().forEach(reward -> actual.merge(reward.itemId(), reward.count(), Integer::sum));
        eq("the owner-decided finite kit is exact", Map.of(
                "minecraft:cobblestone", 128, "minecraft:oak_log", 64, "minecraft:bread", 64,
                "minecraft:brick", 64, "minecraft:clay_ball", 64, "minecraft:sand", 64,
                "minecraft:charcoal", 32, "minecraft:iron_ingot", 16, "minecraft:water_bucket", 2), actual);
        yes("no machine, core, insulator or Matrix is granted",
                starter.rewards().stream().allMatch(reward -> reward.itemId().startsWith("minecraft:")));
        eq("water buckets remain separate legal stacks", List.of(1, 1), starter.rewards().stream()
                .filter(reward -> reward.itemId().equals("minecraft:water_bucket"))
                .map(QuestCatalogue.Reward::count).toList());
        QuestCatalogue.Reward reward = new QuestCatalogue.Reward("minecraft:bread", 1);
        no("an item objective cannot fake free by asking for zero", QuestLogic.problems(List.of(
                new QuestCatalogue.Task("fake", "bootstrap", QuestCatalogue.Evidence.ITEM,
                        "minecraft:bread", 0, null, List.of(), reward))).isEmpty());
        no("a free goal cannot hide a physical subject", QuestLogic.problems(List.of(
                new QuestCatalogue.Task("fake", "bootstrap", QuestCatalogue.Evidence.FREE,
                        "minecraft:bread", 1, null, List.of(), reward))).isEmpty());
    }

    private static void route() {
        List<QuestCatalogue.Task> tasks = QuestCatalogue.tasks();
        eq("bootstrap starts at the multitool", "grindless:multitool", subject("multitool"));
        eq("then the crank", "grindless:hand_crank_dynamo", subject("crank"));
        eq("then the extractor", "grindless:crude_extractor", subject("extractor"));
        eq("the workshop starts after the multitool", List.of("multitool"), task("quern").requires());
        eq("the optional crank follows the multitool", List.of("multitool"), task("crank").requires());
        eq("the optional extractor keeps its crank prerequisite", List.of("crank"), task("extractor").requires());
        no("the optional crank is not a prerequisite of the T0 exit", reaches("crank", "relay"));
        no("the optional extractor is not a prerequisite of the T0 exit", reaches("extractor", "relay"));
        eq("calibration needs a physical core", "grindless:calibrated_data_core", subject("calibrate"));
        eq("calibration now follows physical ceramics", List.of("ceramics"), task("calibrate").requires());
        eq("water supply precedes wet mixing", List.of("quern", "water"), task("clay_mixer").requires());
        for (QuestCatalogue.Task task : tasks) {
            if (Set.of("bootstrap", "workshop", "settling").contains(task.line())) {
                no(task.id() + " cannot reward a bypass around ceramic manufacture",
                        Set.of("grindless:data_core", "grindless:calibrated_data_core",
                                "grindless:relay_matrix", "grindless:ceramic_insulator")
                                .stream().anyMatch(item -> task.rewards().stream()
                                        .anyMatch(reward -> reward.itemId().equals(item))));
            }
        }
        eq("the physical Bootstrap exit is five Relay Matrices", "grindless:relay_matrix", subject("relay"));
        eq("the first power source follows the Matrix", List.of("relay"), task("thermal").requires());
        eq("the pylon follows unattended power", "grindless:flux_pylon_mk1", subject("pylon"));
        eq("surveying precedes terrestrial extraction", List.of("pylon"), task("scanner").requires());
        eq("the extractor follows the scanner", "grindless:terrestrial_extractor", subject("terrestrial"));
        eq("the furnace is the factory spine", "grindless:arc_furnace", subject("furnace"));
        eq("the Press precedes the Assembler", List.of("press", "furnace"), task("assembler").requires());
        eq("the Assembler unlocks the repeated Matrix line", List.of("assembler"), task("matrix_line").requires());
        eq("renewable glass has a physical stock objective", "minecraft:glass", subject("glass"));
        eq("the optional Factory Portal keeps its original physical objective",
                "grindless:factory_portal", subject("factory_world"));
        eq("the F2 Matrix is the T1 exit", "grindless:relay_matrix", subject("industrial_matrix"));
        eq("the F2 Matrix requires F2", io.github.ezequiel24123z.grindless.energy.FluxTier.F2,
                task("industrial_matrix").matrixRating());
        yes("a walk from the multitool reaches the F2 Matrix", reaches("multitool", "industrial_matrix"));
        eq("only the repeated Matrix line and glass remain terminal claim prerequisites",
                List.of("matrix_line", "glass"), task("industrial_matrix").requires());
        for (String optional : List.of("scanner", "terrestrial", "logistics", "kiln", "factory_world",
                "wet_beneficiation", "gas_utilities")) {
            no(optional + " is not a mandatory edge to F2", reaches(optional, "industrial_matrix"));
            yes(optional + " is visibly optional", QuestTeaching.optional(task(optional)));
        }
        for (String reading : List.of("first_grid", "forming_dies", "copper_coil", "flux_cell")) {
            no(reading + " does not introduce a new claim tax before the F2 exit", reaches(reading, "industrial_matrix"));
        }
        eq("the optional wet lesson requires only the existing Assembler lesson",
                List.of("assembler"), task("wet_beneficiation").requires());
        eq("the wet lesson observes one actual Chemical Washer", "grindless:chemical_washer",
                subject("wet_beneficiation"));
        eq("the optional gas lesson requires only the existing Assembler lesson",
                List.of("assembler"), task("gas_utilities").requires());
        eq("the gas lesson observes one actual Electrolysis Cell", "grindless:electrolysis_cell",
                subject("gas_utilities"));
        eq("gas possession is not an observed process objective", QuestCatalogue.Evidence.ITEM,
                task("gas_utilities").evidence());
        eq("gas acquisition remains reward-free", List.of(), task("gas_utilities").rewards());
        no("the survival route has no dimension objectives",
                tasks.stream().anyMatch(task -> task.evidence() == QuestCatalogue.Evidence.DIMENSION));
        eq("relay is the final T0 task", "relay", tasks.stream()
                .filter(task -> QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T0)
                .reduce((first, second) -> second).orElseThrow().id());
        eq("the T0 gate checks the physical Matrix, not ordinary possession",
                QuestCatalogue.Evidence.MATRIX, task("relay").evidence());
        eq("the T0 gate requires the exact F1 rating", io.github.ezequiel24123z.grindless.energy.FluxTier.F1,
                task("relay").matrixRating());
        eq("tag-resolved mineral products do not force a vanilla-only inventory checklist",
                QuestCatalogue.Evidence.FREE, task("mineral_feed").evidence());
        no("opening the book or reaching calibration cannot unlock T1",
                QuestCatalogue.unlocked(QuestCatalogue.Chapter.T1, Set.of("calibrate")));
        no("an old T1 claim does not unlock T1", QuestCatalogue.unlocked(QuestCatalogue.Chapter.T1,
                Set.of("thermal", "industrial_matrix")));
        yes("only the final T0 claim unlocks T1", QuestCatalogue.unlocked(QuestCatalogue.Chapter.T1, Set.of("relay")));
        for (QuestCatalogue.Task task : tasks) {
            if (QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1) {
                yes(task.id() + " has an explicit projected Relay dependency",
                        QuestCatalogue.requirements(task).contains("relay"));
                eq(task.id() + " cannot expose a retained T1 claim prematurely", QuestLogic.Status.LOCKED,
                        QuestLogic.consider(task, Set.of(task.id()), true));
            }
        }
        for (QuestCatalogue.Task task : tasks) {
            String blob = (task.id() + " " + task.subject() + " " + task.line()).toLowerCase();
            no(task.id() + " is not a spatial prototype or an unbuilt world",
                    blob.contains("orb") || blob.contains("tharsis") || blob.contains("kardashev")
                            || blob.contains("teleport") || blob.contains("array")
                            || blob.contains("rocket") || blob.contains("luna")
                            || blob.contains("station") || blob.contains("drift")
                            || blob.contains("sagittarius") || blob.contains("voyage"));
        }
    }

    private static void claims() {
        Set<String> empty = Set.of();
        eq("the Factory Portal is locked at the start", QuestLogic.Status.LOCKED,
                QuestLogic.consider(task("factory_world"), empty, true));
        eq("the multitool is unmet until it is held", QuestLogic.Status.UNMET,
                QuestLogic.consider(task("multitool"), empty, false));
        eq("the multitool is claimable when it is held", QuestLogic.Status.CLAIMABLE,
                QuestLogic.consider(task("multitool"), empty, true));
        eq("the quern reward is locked before the multitool claim", QuestLogic.Status.LOCKED,
                QuestLogic.consider(task("quern"), empty, true));
        eq("the workshop needs quern evidence, not an automatic starting reward", QuestLogic.Status.UNMET,
                QuestLogic.consider(task("quern"), Set.of("multitool"), false));
        eq("the quern can be claimed without either optional machine", QuestLogic.Status.CLAIMABLE,
                QuestLogic.consider(task("quern"), Set.of("multitool"), true));
        eq("the quern claim cannot be repeated", QuestLogic.Status.CLAIMED,
                QuestLogic.consider(task("quern"), Set.of("multitool", "quern"), true));
        eq("the optional extractor still needs the crank claim", QuestLogic.Status.LOCKED,
                QuestLogic.consider(task("extractor"), Set.of("multitool", "quern"), true));
        eq("the optional extractor remains claimable on its own branch", QuestLogic.Status.CLAIMABLE,
                QuestLogic.consider(task("extractor"), Set.of("multitool", "crank"), true));
        eq("the quern keeps its existing conditional four-brick reward",
                new QuestCatalogue.Reward("minecraft:brick", 4), task("quern").reward());
        eq("the optional crank keeps its existing four-coal reward",
                new QuestCatalogue.Reward("minecraft:coal", 4), task("crank").reward());
        eq("the optional extractor keeps its existing eight-gravel reward",
                new QuestCatalogue.Reward("minecraft:gravel", 8), task("extractor").reward());

        Set<String> claimed = new HashSet<>();
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            boolean ready = task.requires().stream().allMatch(claimed::contains);
            yes(task.id() + " becomes ready in book order", ready);
            eq(task.id() + " respects its explicit evidence kind", task.evidence() == QuestCatalogue.Evidence.FREE
                            ? QuestLogic.Status.CLAIMABLE : QuestLogic.Status.UNMET,
                    QuestLogic.consider(task, claimed, false));
            eq(task.id() + " is claimable with evidence", QuestLogic.Status.CLAIMABLE,
                    QuestLogic.consider(task, claimed, true));
            claimed.add(task.id());
            eq(task.id() + " stays claimed after the evidence is gone", QuestLogic.Status.CLAIMED,
                    QuestLogic.consider(task, claimed, false));
        }
        eq("a second pass finds every task claimed", QuestCatalogue.tasks().size(), claimed.size());
    }

    private static void evidence() {
        QuestCatalogue.Task item = task("multitool");
        yes("one multitool is enough", QuestEvidence.met(item, 1, "minecraft:overworld"));
        no("zero is not enough", QuestEvidence.met(item, 0, "minecraft:overworld"));
        QuestCatalogue.Task calibrated = task("calibrate");
        yes("a calibrated core counts", QuestEvidence.met(calibrated, 1, "minecraft:overworld"));
        no("no calibrated core does not count", QuestEvidence.met(calibrated, 0, "minecraft:overworld"));
        QuestCatalogue.Task relay = task("relay");
        no("four Relay Matrices do not complete the five-machine seed", QuestEvidence.met(relay, 4, "minecraft:overworld"));
        yes("five Relay Matrices complete the seed", QuestEvidence.met(relay, 5, "minecraft:overworld"));
        QuestCatalogue.Task industrial = task("industrial_matrix");
        no("no F2 Matrix does not complete the Industrial hand-off", QuestEvidence.met(industrial, 0, "minecraft:overworld"));
        yes("one F2 Matrix completes the Industrial hand-off", QuestEvidence.met(industrial, 1, "minecraft:overworld"));
        QuestCatalogue.Task wet = task("wet_beneficiation");
        no("reading alone does not prove Washer acquisition", QuestEvidence.met(wet, 0, "minecraft:overworld"));
        yes("one acquired Washer meets the optional possession objective",
                QuestEvidence.met(wet, 1, "minecraft:overworld"));
        QuestCatalogue.Task gas = task("gas_utilities");
        no("reading alone does not prove Cell acquisition", QuestEvidence.met(gas, 0, "minecraft:overworld"));
        yes("one acquired Cell meets only the optional possession objective",
                QuestEvidence.met(gas, 1, "minecraft:overworld"));
    }

    private static void rewards() throws IOException {
        Set<String> itemSubjects = new HashSet<>();
        Set<String> known = knownItems();
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            if (task.evidence() == QuestCatalogue.Evidence.ITEM) {
                String path = path(task.subject());
                itemSubjects.add(task.subject());
                yes(task.id() + " names a registered or vanilla item", known.contains(path)
                        || task.subject().startsWith("minecraft:"));
            }
            for (QuestCatalogue.Reward entry : task.rewards()) {
                String reward = entry.itemId();
                no(task.id() + " T0 reward cannot bypass a physical T0 checklist",
                        QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T0 && (itemSubjects.contains(reward)
                        || QuestCatalogue.tasks().stream().anyMatch(other ->
                        other.evidence() == QuestCatalogue.Evidence.ITEM && other.subject().equals(reward))));
                yes(task.id() + " reward is a vanilla or Grindless item",
                        reward.startsWith("minecraft:") || known.contains(path(reward)));
            }
        }
    }

    private static void guide() throws IOException {
        List<Integer> longText = java.util.stream.IntStream.range(0, 31).boxed().toList();
        List<List<Integer>> parts = QuestTeaching.paginate(longText, 15);
        eq("long quest help uses three physical pages", 3, parts.size());
        eq("pagination never drops or duplicates a wrapped line", longText,
                parts.stream().flatMap(List::stream).toList());
        yes("each guide page fits the body area", parts.stream().allMatch(part -> part.size() <= 15));
        eq("an empty topic still has a titled page", 1, GuideCatalogue.paginate(List.of(), 15).size());
        try {
            GuideCatalogue.paginate(longText, 0);
            fail("a zero-line page must be rejected");
        } catch (IllegalArgumentException expected) {
            yes("a zero-line page is rejected explicitly", true);
        }
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        List<AtlasLogic.Entry> recipes = AtlasLogic.allEntries(java.util.stream.Stream.concat(
                BootstrapProcesses.recipes().stream(), RenewableProcesses.recipes().stream()).toList());
        StringBuilder all = new StringBuilder();
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            String body = QuestTeaching.bodyKey(task);
            String expectedBody = "gui.grindless.quest.lesson."
                    + (QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1
                    ? "t1." + T1Progression.byQuest(task.id()).id() + "." + task.id()
                    : task.id().equals("settling") ? "settling_ports" : task.id());
            eq(task.id() + " selects the native description key", expectedBody, body);
            String title = "gui.grindless.quest.task." + task.id();
            yes(task.id() + " has a nonblank native quest title",
                    lang.has(title) && !lang.get(title).getAsString().isBlank());
            yes(task.id() + " has concise in-quest teaching", lang.has(body)
                    && !lang.get(body).getAsString().isBlank() && lang.get(body).getAsString().length() <= 1400);
            if (QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1) {
                yes(task.id() + " native T1 teaching has at most 42 words",
                        lang.get(body).getAsString().strip().split("\\s+").length <= 42);
            }
            yes(task.id() + " has a localized route classification",
                    lang.has("gui.grindless.quest.path." + QuestTeaching.path(task).name().toLowerCase(java.util.Locale.ROOT)));
            if (QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T0) {
                all.append(lang.get(title).getAsString()).append('\n')
                        .append(lang.get(body).getAsString()).append('\n');
            }
        }
        String text = all.toString();
        // Ordinary item names and bills belong to native objectives and live recipes, not prose quotas.
        for (String mark : List.of(
                "Superflat", "construction", "Multitool", "Crank Handle",
                "Hand Pump", "Quern", "grog", "Firing Chamber", "insulators", "Crude Extractor",
                "Geological Collector", "aggregate",
                "water", "iron", "copper", "redstone", "Slag", "Biocultivator", "daylight", "biomass",
                "sticks", "Biomass Dynamo", "Basic Tank", "Clay Conduit",
                "Crude Manipulator", "Assembly Bench", "Research Terminal", "Data Core",
                "Calibrated Data Core", "F1", "T1")) {
            yes("the T0 quests teach " + mark, text.contains(mark));
        }
        eq("native starter teaching retains the finite-kit classification", QuestTeaching.Path.STARTER,
                QuestTeaching.path(task(QuestCatalogue.STARTER_SUPPLIES)));
        String starterBody = lang.get(QuestTeaching.bodyKey(task(QuestCatalogue.STARTER_SUPPLIES))).getAsString();
        yes("native starter teaching warns about separate rewards and overflow drops",
                starterBody.contains("separately") && starterBody.contains("drop"));
        eq("native quest help remains a rewardless lesson", QuestCatalogue.Type.LESSON, task("quest_book_help").type());
        eq("native quest help does not require a held book objective", QuestCatalogue.Evidence.FREE,
                task("quest_book_help").evidence());
        String help = lang.get(QuestTeaching.bodyKey(task("quest_book_help"))).getAsString();
        yes("native help identifies FTB Quests and individual rewards despite shared team progress",
                help.contains("FTB Quests") && help.contains("team") && help.contains("individually"));
        for (Map.Entry<String, String> topic : Map.of(
                "clay_mixer", "grindless:clay_mixer", "settling", "grindless:settling_trough",
                "fibreboard", "grindless:fibreboard", "conveyors", "grindless:conveyor_belt").entrySet()) {
            QuestCatalogue.Task objective = task(topic.getKey());
            eq(topic.getKey() + " remains a physical native item objective", QuestCatalogue.Evidence.ITEM,
                    objective.evidence());
            eq(topic.getKey() + " names the actual workshop item", topic.getValue(), objective.subject());
            yes(topic.getKey() + " selects a live recipe producing its objective",
                    QuestTeaching.recipes(objective, recipes).stream()
                            .anyMatch(entry -> entry.outputs().contains("item:" + topic.getValue())));
        }
        eq("fibreboard follows actual biomass production and mixing", List.of("biomass", "clay_mixer"),
                task("fibreboard").requires());
        eq("conveyors follow ceramics without becoming a mandatory exit", List.of("ceramics"),
                task("conveyors").requires());
        eq("conveyors remain optional", QuestTeaching.Path.OPTIONAL, QuestTeaching.path(task("conveyors")));
        eq("native conveyor evidence retains the eight-belt batch", 8, task("conveyors").count());
        for (Map.Entry<String, String> route : Map.of(
                "bootstrap/fibreboard", "grindless:fibreboard", "bootstrap/paper", "minecraft:paper",
                "bootstrap/biocarbon", "minecraft:charcoal").entrySet()) {
            AtlasLogic.Entry actual = recipes.stream().filter(entry -> entry.id().equals(route.getKey()))
                    .findFirst().orElseThrow();
            yes(route.getKey() + " remains an actual biomass-to-product route",
                    actual.inputs().contains("item:grindless:biomass")
                            && actual.outputs().contains("item:" + route.getValue()));
            yes(route.getKey() + " remains selected by the renewable workshop lesson",
                    QuestTeaching.recipes(task("fibreboard"), recipes).contains(actual));
        }
        no("T0 does not present the Thermal Generator as its next required machine", text.contains("Thermal Generator"));
        yes("optional extraction is classified explicitly", QuestTeaching.path(task("extractor")) == QuestTeaching.Path.OPTIONAL);
        AtlasLogic.Entry rawCore = recipes.stream().filter(entry -> entry.id().equals("craft/data_core"))
                .findFirst().orElseThrow();
        AtlasLogic.Entry calibration = recipes.stream().filter(entry -> entry.id().equals("calibrate/data_core"))
                .findFirst().orElseThrow();
        eq("replacement raw cores use ordinary ceramic and redstone production",
                List.of("item:grindless:ceramic_insulator", "item:minecraft:redstone"), rawCore.inputs());
        eq("the replacement craft really produces a raw core", List.of("item:grindless:data_core"), rawCore.outputs());
        eq("replacement calibration consumes a raw core", rawCore.outputs(), calibration.inputs());
        eq("replacement calibration really produces a calibrated core",
                List.of("item:grindless:calibrated_data_core"), calibration.outputs());
        yes("replacement calibration requires powered operation", calibration.fuPerTick() > 0 && calibration.durationTicks() > 0);
        yes("core recovery selects actual manufacture and calibration",
                QuestTeaching.recipes(task("calibrated_core"), recipes).containsAll(List.of(rawCore, calibration)));
        eq("the recovery note follows calibration", List.of("calibrate"), task("calibrated_core").requires());
        eq("the recovery note never pays a replacement core", List.of(), task("calibrated_core").rewards());
        yes("core recovery teaches manufacture and calibration rather than repeating a claim",
                Pattern.compile("\\b(?:make|craft)\\b.*\\bcalibrat\\w*\\b.*\\b(?:claim|reward)\\w*\\b.*\\b(?:not|never)\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                        .matcher(lang.get(QuestTeaching.bodyKey(task("calibrated_core"))).getAsString()).find());
        eq("Firing help uses runtime formation cells", WorkshopStructure.FIRING_CHAMBER,
                QuestTeaching.structure(task("firing")));
        eq("Settling help uses runtime formation cells", WorkshopStructure.SETTLING_TROUGH,
                QuestTeaching.structure(task("settling")));
        eq("the Settling lesson retains the ordered, oriented Channel and Screen",
                List.of("grindless:settling_channel", "grindless:settling_screen"),
                QuestTeaching.structure(task("settling")).cells().stream()
                        .filter(WorkshopStructure.Cell::oriented).map(WorkshopStructure.Cell::block).toList());
        for (String part : List.of("settling_channel", "settling_screen")) {
            yes(part + " remains an actual selected craft, not a prose-only part",
                    QuestTeaching.recipes(task("settling"), recipes).stream().anyMatch(entry ->
                            entry.id().equals("craft/" + part) && entry.outputs().contains("item:grindless:" + part)));
        }
        String settling = lang.get(QuestTeaching.bodyKey(task("settling"))).getAsString();
        yes("selected Settling teaching preserves facing and overhead clearance",
                (settling.contains("same way") || settling.contains("same facing")) && settling.contains("air above"));
        yes("selected Settling teaching preserves controller-owned storage and paid repair",
                settling.contains("controller's storage") && settling.contains("not their own") && settling.contains("paid"));
        for (WorkshopPortContract.Kind kind : WorkshopPortContract.Kind.values()) {
            String topic = kind.structure() == io.github.ezequiel24123z.grindless.structure.WorkshopStructure.FIRING_CHAMBER
                    ? "firing" : "settling";
            AtlasLogic.Entry actualCraft = recipes.stream().filter(entry ->
                    entry.id().equals("craft/" + kind.blockName())).findFirst().orElseThrow();
            yes("optional port recipe is projected from its actual craft " + kind.id(),
                    QuestTeaching.recipes(task(topic), recipes).contains(actualCraft));
        }
        for (WorkshopPortRole role : WorkshopPortRole.values()) {
            yes("actual port role is localized " + role,
                    lang.has("gui.grindless.quest.port_role." + role.name().toLowerCase(java.util.Locale.ROOT)));
        }
        for (String prototype : List.of(
                "ground array", "luna", "helium-3", "rocket", "drift", "station",
                "starward link", "sealed chamber", "arriving is the victory")) {
            Pattern namedPrototype = Pattern.compile("\\b" + Pattern.quote(prototype) + "\\b", Pattern.CASE_INSENSITIVE);
            no("T0 teaching does not advertise " + prototype, namedPrototype.matcher(text).find());
            yes("the guard still detects the actual prototype " + prototype,
                    namedPrototype.matcher("Visit the " + prototype + " now.").find());
        }
        no("stationary item transport is not an orbital station advertisement",
                Pattern.compile("\\bstation\\b").matcher("The stock is stationary.").find());
        no("the guide does not send the reader to an orb", text.toLowerCase().contains("orb"));
        no("the guide does not name a forbidden mod", containsForbidden(text.toLowerCase()));
    }

    private static void items() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        for (String item : List.of("quest_book")) {
            yes(item + " is named", lang.has("item.grindless." + item));
            yes(item + " has a tooltip", lang.has("tooltip.grindless." + item));
            JsonObject recipe = JsonParser.parseString(Files.readString(RECIPES.resolve(item + ".json")))
                    .getAsJsonObject();
            eq(item + " is an ungated craft", "minecraft:crafting_shaped", recipe.get("type").getAsString());
            eq(item + " recipe result", "grindless:" + item,
                    recipe.getAsJsonObject("result").get("item").getAsString());
            no(item + " is not research gated", recipe.toString().contains("blueprint"));
        }
        no("the deprecated Guide cannot be crafted normally", Files.exists(RECIPES.resolve("field_guide.json")));
        yes("the old Guide has a distinct deprecation tooltip", lang.has("tooltip.grindless.field_guide.legacy"));
        String items = Files.readString(SOURCES.resolve("registry/ModItems.java"));
        String tabs = Files.readString(SOURCES.resolve("registry/ModCreativeTabs.java"));
        String menus = Files.readString(SOURCES.resolve("registry/ModMenus.java"));
        yes("both handhelds are registered",
                items.contains("register(\"quest_book\"") && items.contains("register(\"field_guide\""));
        yes("the Quest Book remains on the normal creative tab", tabs.contains("QUEST_BOOK"));
        no("the retired Guide is no longer offered on the creative tab", tabs.contains("FIELD_GUIDE"));
        yes("retained historical menu identities remain registered",
                menus.contains("register(\"quest_book\"") && menus.contains("register(\"field_guide\""));
        // Historical custom screen bindings are not the active quest presentation contract.
        String definitions = Files.readString(FORGE_SOURCES.resolve("forge/quest/FtbQuestDefinitions.java"));
        String integration = Files.readString(FORGE_SOURCES.resolve("forge/quest/FtbQuestIntegration.java"));
        String platform = Files.readString(FORGE_SOURCES.resolve("quest/forge/QuestBackendPlatformImpl.java"));
        yes("native quest titles use the localized task identity",
                definitions.contains("Component.translatable(\"gui.grindless.quest.task.\" + task.id())"));
        yes("native descriptions use the selected lesson rather than a custom help widget",
                definitions.contains("json(body(task))") && definitions.contains("QuestTeaching.bodyKey(task)"));
        yes("native item objectives own counts and do not consume evidence",
                definitions.contains("case ITEM, MATRIX -> \"ftbquests:item\"")
                        && definitions.contains("evidence.putLong(\"count\", task.count())")
                        && definitions.contains("evidence.putBoolean(\"consume_items\", false)"));
        yes("the native T1 graph retains catalogue prerequisite gates",
                definitions.contains("\"dependencies\", strings(QuestCatalogue.requirements(task).stream()")
                        && definitions.contains("\"hide_until_deps_complete\", QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1"));
        yes("opening quests sends the framework's native book message",
                integration.contains("new OpenQuestBookMessage(") && integration.contains(".sendTo(player)"));
        yes("missing FTB is handled visibly without enabling a historical renderer",
                platform.contains("FtbQuestIntegration.open(player)") && platform.contains("return true;")
                        && platform.contains("displayClientMessage(") && platform.contains("backend_unavailable")
                        && !platform.contains("return false;") && !platform.contains("Heracles")
                        && !platform.contains("MenuRegistry") && !platform.contains("QuestBookMenu"));
        String book = Files.readString(SOURCES.resolve("item/QuestBookItem.java"));
        yes("the retained Quest Book routes through native platform admission",
                book.contains("QuestBackendPlatform.open(server)"));
        String legacy = Files.readString(SOURCES.resolve("item/FieldGuideItem.java"));
        yes("old Guide use routes to native admission without rewriting item NBT",
                legacy.contains("QuestBackendPlatform.open(server)") && !legacy.contains("setItemInHand"));
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            yes(task.id() + " has a row label", lang.has("gui.grindless.quest.task." + task.id()));
        }
        for (String line : QuestCatalogue.lines()) {
            yes(line + " has a line label", lang.has("gui.grindless.quest.line." + line));
        }
    }

    private static void noCopies() throws IOException {
        StringBuilder sources = new StringBuilder();
        sources.append(Files.readString(SOURCES.resolve("quest/QuestCatalogue.java")));
        sources.append(Files.readString(SOURCES.resolve("quest/QuestLogic.java")));
        sources.append(Files.readString(SOURCES.resolve("quest/GuideCatalogue.java")));
        sources.append(Files.readString(SOURCES.resolve("item/QuestBookItem.java")));
        sources.append(Files.readString(SOURCES.resolve("item/FieldGuideItem.java")));
        sources.append(Files.readString(LANG));
        no("this slice copies no named mod", containsForbidden(sources.toString().toLowerCase()));
    }

    private static boolean reaches(String from, String to) {
        Set<String> seen = new HashSet<>();
        List<String> open = new ArrayList<>();
        open.add(from);
        while (!open.isEmpty()) {
            String id = open.remove(open.size() - 1);
            if (!seen.add(id)) {
                continue;
            }
            if (id.equals(to)) {
                return true;
            }
            for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
                if (task.requires().contains(id)) {
                    open.add(task.id());
                }
            }
        }
        return false;
    }

    private static Set<String> knownItems() throws IOException {
        Set<String> known = new HashSet<>();
        Matcher matcher = Pattern.compile("register\\(\"([a-z0-9_]+)\"").matcher(
                Files.readString(SOURCES.resolve("registry/ModItems.java"))
                        + Files.readString(SOURCES.resolve("registry/ModBlocks.java")));
        while (matcher.find()) {
            known.add(matcher.group(1));
        }
        for (SupplyCatalogue.Entry entry : SupplyCatalogue.entries()) {
            known.add(entry.itemName());
        }
        return known;
    }

    private static boolean containsForbidden(String text) {
        for (String name : FORBIDDEN) {
            if (text.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static QuestCatalogue.Task task(String id) {
        QuestCatalogue.Task task = QuestCatalogue.byId(id);
        if (task == null) {
            throw new IllegalStateException(id);
        }
        return task;
    }

    private static String subject(String id) {
        return task(id).subject();
    }

    private static String path(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    private static void eq(String what, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but was " + actual);
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
        yes(what, !actual);
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
