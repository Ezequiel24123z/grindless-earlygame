package io.github.ezequiel24123z.grindless.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.material.SupplyCatalogue;

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

    private static final List<String> LINES = List.of(
            "bootstrap", "workshop", "renewable_supply", "settling", "bioculture", "workshop_logistics",
            "relay", "power", "extraction", "factory", "renewables", "logistics", "factory_world",
            "industrial");
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
        eq("thirty-two T0 tasks and fourteen preserved T1 objectives", 46, tasks.size());
        eq("all source-defined lines remain explicit", LINES, QuestCatalogue.lines());
        eq("the T0 chapter has its own seven lines", LINES.subList(0, 7),
                QuestCatalogue.lines(QuestCatalogue.Chapter.T0));
        eq("the T1 chapter has its own seven lines", LINES.subList(7, 14),
                QuestCatalogue.lines(QuestCatalogue.Chapter.T1));
        yes("the first task has no dependency", tasks.get(0).requires().isEmpty());
        eq("the last task is the physical Industrial hand-off", "industrial_matrix", tasks.get(tasks.size() - 1).id());
        eq("one task per id", tasks.size(),
                (int) tasks.stream().map(QuestCatalogue.Task::id).distinct().count());
        Set<String> old = new HashSet<>(LEGACY_IDS);
        old.add(QuestCatalogue.STARTER_SUPPLIES);
        eq("all 26 original rewarded IDs remain; new teaching never mints a material reward", old,
                tasks.stream().filter(task -> task.type() == QuestCatalogue.Type.REWARD)
                        .map(QuestCatalogue.Task::id).collect(java.util.stream.Collectors.toSet()));
        eq("every added task has an explicit rewardless lesson type", 20L, tasks.stream()
                .filter(task -> task.type() == QuestCatalogue.Type.LESSON && task.rewards().isEmpty()).count());
        eq("old evidence ordinals do not move", List.of(0, 1, 2), List.of(
                QuestCatalogue.Evidence.ITEM.ordinal(), QuestCatalogue.Evidence.DIMENSION.ordinal(),
                QuestCatalogue.Evidence.MATRIX.ordinal()));
        eq("status wire meanings do not move", List.of(0, 1, 2, 3),
                java.util.Arrays.stream(QuestLogic.Status.values()).map(Enum::ordinal).toList());
        eq("all old prerequisites and rewards retain their exact values", List.of(
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
                "industrial_matrix|matrix_line,glass,factory_world|minecraft:iron_ingot/8"),
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
        eq("the Factory Portal finishes the current route", "grindless:factory_portal", subject("factory_world"));
        eq("the F2 Matrix is the T1 exit", "grindless:relay_matrix", subject("industrial_matrix"));
        eq("the F2 Matrix requires F2", io.github.ezequiel24123z.grindless.energy.FluxTier.F2,
                task("industrial_matrix").matrixRating());
        yes("a walk from the multitool reaches the F2 Matrix", reaches("multitool", "industrial_matrix"));
        no("the survival route has no dimension objectives",
                tasks.stream().anyMatch(task -> task.evidence() == QuestCatalogue.Evidence.DIMENSION));
        eq("relay is the final T0 task", "relay", tasks.stream()
                .filter(task -> QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T0)
                .reduce((first, second) -> second).orElseThrow().id());
        eq("the T0 gate checks the physical Matrix, not ordinary possession",
                QuestCatalogue.Evidence.MATRIX, task("relay").evidence());
        eq("the T0 gate requires the exact F1 rating", io.github.ezequiel24123z.grindless.energy.FluxTier.F1,
                task("relay").matrixRating());
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
        StringBuilder all = new StringBuilder();
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            String body = QuestTeaching.bodyKey(task);
            yes(task.id() + " has concise in-quest teaching", lang.has(body)
                    && !lang.get(body).getAsString().isBlank() && lang.get(body).getAsString().length() <= 1400);
            yes(task.id() + " has a localized route classification",
                    lang.has("gui.grindless.quest.path." + QuestTeaching.path(task).name().toLowerCase(java.util.Locale.ROOT)));
            if (QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T0) {
                all.append(lang.get(body).getAsString()).append('\n');
            }
        }
        String text = all.toString();
        for (String mark : List.of(
                "starter", "Quest Book", "Superflat", "construction", "Multitool", "Crank Handle",
                "Hand Pump", "Quern", "Clay Mixer", "grog", "Firing Chamber", "insulators", "Crude Extractor",
                "Geological Collector", "aggregate", "Settling", "Settling Channel", "Settling Screen",
                "water", "iron", "copper", "redstone", "Slag", "Biocultivator", "daylight", "biomass",
                "fibreboard", "paper", "sticks", "charcoal", "Biomass Dynamo", "Basic Tank", "Clay Conduit",
                "Conveyor Belts", "Crude Manipulator", "Assembly Bench", "Research Terminal", "Data Core",
                "Calibrated Data Core", "F1", "T1")) {
            yes("the T0 quests teach " + mark, text.contains(mark));
        }
        no("T0 does not present the Thermal Generator as its next required machine", text.contains("Thermal Generator"));
        yes("optional extraction is classified explicitly", QuestTeaching.path(task("extractor")) == QuestTeaching.Path.OPTIONAL);
        yes("the first core is replaceable through production, not reset claims",
                lang.get(QuestTeaching.bodyKey(task("calibrated_core"))).getAsString().contains("not a reward reset"));
        eq("Firing help uses runtime formation cells", io.github.ezequiel24123z.grindless.structure.WorkshopStructure.FIRING_CHAMBER,
                QuestTeaching.structure(task("firing")));
        eq("Settling help uses runtime formation cells", io.github.ezequiel24123z.grindless.structure.WorkshopStructure.SETTLING_TROUGH,
                QuestTeaching.structure(task("settling")));
        for (String prototype : List.of(
                "ground array", "luna", "helium-3", "rocket", "drift", "station",
                "starward link", "sealed chamber", "arriving is the victory")) {
            no("T0 teaching does not advertise " + prototype, text.toLowerCase().contains(prototype));
        }
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
        String client = Files.readString(SOURCES.resolve("client/GrindlessClient.java"));
        yes("both handhelds are registered",
                items.contains("register(\"quest_book\"") && items.contains("register(\"field_guide\""));
        yes("the Quest Book remains on the normal creative tab", tabs.contains("QUEST_BOOK"));
        no("the retired Guide is no longer offered on the creative tab", tabs.contains("FIELD_GUIDE"));
        yes("both menus are registered",
                menus.contains("register(\"quest_book\"") && menus.contains("register(\"field_guide\""));
        yes("both screens are bound",
                client.contains("QuestBookScreen") && client.contains("FieldGuideScreen"));
        String questMenu = Files.readString(SOURCES.resolve("menu/QuestBookMenu.java"));
        String questScreen = Files.readString(SOURCES.resolve("client/QuestBookScreen.java"));
        yes("the book synchronizes held-item progress", questMenu.contains("public int held(int index)")
                && questMenu.contains("tasks.size() + i"));
        yes("the screen shows the synchronized objective", questScreen.contains("menu.held(selected)")
                && lang.has("gui.grindless.quest.objective.item"));
        yes("native help pages all lesson lines without truncating them", questScreen.contains("QuestTeaching.paginate(")
                && questScreen.contains("QuestTeachingClient.content("));
        yes("native T1 navigation is server-gated", questScreen.contains("menu.chapterUnlocked(")
                && questMenu.contains("QuestCatalogue.unlocked(QuestCatalogue.Chapter.T1, claimed)"));
        String legacy = Files.readString(SOURCES.resolve("item/FieldGuideItem.java"));
        yes("old Guide use routes to quests without rewriting item NBT", legacy.contains("new QuestBookMenu(")
                && legacy.contains("QuestBackendPlatform.open(server)") && !legacy.contains("setItemInHand"));
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
