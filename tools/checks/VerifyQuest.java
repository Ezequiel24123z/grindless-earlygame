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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Behaviour checks for the reachable quest route and field guide (ADR-0100). Not part of the mod. */
public final class VerifyQuest {

    private static final Path ROOT = Path.of(".");
    private static final Path LANG = Path.of("common/src/main/resources/assets/grindless/lang/en_us.json");
    private static final Path RECIPES = Path.of("common/src/main/resources/data/grindless/recipes");
    private static final Path SOURCES = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");

    private static final List<String> LINES = List.of(
            "bootstrap", "relay", "power", "extraction", "factory", "renewables", "logistics", "factory_world",
            "industrial");

    private static final List<String> FORBIDDEN = List.of(
            "betterquesting", "patchouli", "funwayguy", "draconic", "galacticraft",
            "advancedrocketry", "ad astra", "gregtech");

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        graph();
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
        eq("twenty tasks, from the multitool to the F2 Matrix", 20, tasks.size());
        eq("nine T0-to-T1 lines", LINES, QuestCatalogue.lines());
        yes("the first task has no dependency", tasks.get(0).requires().isEmpty());
        eq("the last task is the physical Industrial hand-off", "industrial_matrix", tasks.get(tasks.size() - 1).id());
        eq("one task per id", tasks.size(),
                (int) tasks.stream().map(QuestCatalogue.Task::id).distinct().count());
    }

    private static void route() {
        List<QuestCatalogue.Task> tasks = QuestCatalogue.tasks();
        eq("bootstrap starts at the multitool", "grindless:multitool", subject("multitool"));
        eq("then the crank", "grindless:hand_crank_dynamo", subject("crank"));
        eq("then the extractor", "grindless:crude_extractor", subject("extractor"));
        eq("calibration follows the extractor", "grindless:calibrated_data_core", subject("calibrate"));
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

        Set<String> claimed = new HashSet<>();
        for (QuestCatalogue.Task task : QuestCatalogue.tasks()) {
            boolean ready = task.requires().stream().allMatch(claimed::contains);
            yes(task.id() + " becomes ready in book order", ready);
            eq(task.id() + " stays unmet without evidence", QuestLogic.Status.UNMET,
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
            String reward = task.reward().itemId();
            no(task.id() + " reward is not some task's evidence", itemSubjects.contains(reward)
                    || QuestCatalogue.tasks().stream().anyMatch(other ->
                    other.evidence() == QuestCatalogue.Evidence.ITEM && other.subject().equals(reward)));
            yes(task.id() + " reward is a vanilla or Grindless item",
                    reward.startsWith("minecraft:") || known.contains(path(reward)));
        }
    }

    private static void guide() throws IOException {
        List<String> pages = GuideCatalogue.pages();
        eq("the guide follows the reachable quest lines", QuestCatalogue.lines(), pages);
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        StringBuilder all = new StringBuilder();
        for (String page : pages) {
            String title = GuideCatalogue.titleKey(page);
            String body = GuideCatalogue.bodyKey(page);
            yes(page + " has a title", lang.has(title) && !lang.get(title).getAsString().isBlank());
            yes(page + " has a body", lang.has(body) && !lang.get(body).getAsString().isBlank());
            all.append(lang.get(body).getAsString()).append('\n');
        }
        String text = all.toString();
        for (String mark : List.of(
                "Multitool", "Hand Crank", "Crude Extractor", "Relay Matrices", "Thermal Generator", "Flux Pylon",
                "Prospector", "Terrestrial Extractor", "Pulverizer", "Arc Furnace", "Press", "Assembler", "cobblestone",
                "Conveyor Belts", "Factory Portal")) {
            yes("the guide mentions " + mark, text.contains(mark));
        }
        for (String prototype : List.of(
                "ground array", "luna", "helium-3", "rocket", "drift", "station",
                "starward link", "sealed chamber", "arriving is the victory")) {
            no("the guide does not advertise " + prototype, text.toLowerCase().contains(prototype));
        }
        no("the guide does not send the reader to an orb", text.toLowerCase().contains("orb"));
        no("the guide does not name a forbidden mod", containsForbidden(text.toLowerCase()));
    }

    private static void items() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();
        for (String item : List.of("quest_book", "field_guide")) {
            yes(item + " is named", lang.has("item.grindless." + item));
            yes(item + " has a tooltip", lang.has("tooltip.grindless." + item));
            JsonObject recipe = JsonParser.parseString(Files.readString(RECIPES.resolve(item + ".json")))
                    .getAsJsonObject();
            eq(item + " is an ungated craft", "minecraft:crafting_shaped", recipe.get("type").getAsString());
            eq(item + " recipe result", "grindless:" + item,
                    recipe.getAsJsonObject("result").get("item").getAsString());
            no(item + " is not research gated", recipe.toString().contains("blueprint"));
        }
        String items = Files.readString(SOURCES.resolve("registry/ModItems.java"));
        String tabs = Files.readString(SOURCES.resolve("registry/ModCreativeTabs.java"));
        String menus = Files.readString(SOURCES.resolve("registry/ModMenus.java"));
        String client = Files.readString(SOURCES.resolve("client/GrindlessClient.java"));
        yes("both handhelds are registered",
                items.contains("register(\"quest_book\"") && items.contains("register(\"field_guide\""));
        yes("both sit on the creative tab",
                tabs.contains("QUEST_BOOK") && tabs.contains("FIELD_GUIDE"));
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
