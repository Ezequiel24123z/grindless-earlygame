package io.github.ezequiel24123z.grindless.forge.quest;

import com.google.gson.JsonParser;
import dev.architectury.utils.Env;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ProgressionMode;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardAutoClaim;
import dev.ftb.mods.ftbquests.quest.reward.RewardClaimType;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.DimensionTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixNbt;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixSpec;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestLogic;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import io.github.ezequiel24123z.grindless.quest.QuestTeaching;
import io.github.ezequiel24123z.grindless.recipe.ThermalLogic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Production SNBT/data and physical Matrix parser contracts, plus explicitly SYNTHETIC
 * vanilla-stock native loader/TeamData mechanisms; not a Forge lifecycle.
 *
 * <p>No framework/API initialization substitute, registry mutation, reflection, fake item holder,
 * player, inventory or reward-delivery implementation belongs here. The full native loader needs
 * real item registration, including FTB's MissingItem if a referenced mod item is unavailable.
 * Production loader/reload, registered native task/reward items and canonical TeamData binding
 * therefore require FtbQuestChecks under real Forge. The synthetic fixture preserves production
 * IDs, quantities, dependencies and stock classes, but substitutes vanilla stone item subjects
 * only in deep-copied test chapters. It proves mechanism/codec contracts, never production item
 * matching, a canonical loaded graph or a Frame UI. No caught platform error becomes a pass.
 */
public final class VerifyFtbQuests {

    private static final String MATRIX_FILTER = "grindless:quest_matrix";
    // Synthetic test identities, not player or team data from a world.
    private static final UUID FIRST = new UUID(0, 1);
    private static final UUID SECOND = new UUID(0, 2);
    private static final UUID TEAM = new UUID(0, 16);
    private static final Date NATIVE_DATE = new Date(1_700_000_000_000L);
    private static final Set<String> PAID_START = Set.of(QuestCatalogue.STARTER_SUPPLIES, "multitool");
    private static final Set<String> ADDED_T1_LESSONS = Set.of(
            "first_grid", "forming_dies", "copper_coil", "flux_cell", "wet_beneficiation");
    private static final Set<String> T1_IDS = Set.of(
            "thermal", "pylon", "scanner", "terrestrial", "pulverizer", "furnace", "press",
            "assembler", "matrix_line", "glass", "logistics", "kiln", "factory_world", "industrial_matrix",
            "first_grid", "forming_dies", "copper_coil", "flux_cell", "wet_beneficiation");
    private static final Set<String> LESSON_IDS = Set.of(
            "quest_book_help", "superflat", "construction", "manual_work", "grog", "firing",
            "geological_collector", "aggregate", "water_recovery", "mineral_feed", "biocultivator",
            "biomass", "fibreboard", "biomass_dynamo", "fluid_logistics", "conveyors", "assembly_bench",
            "research_terminal", "data_core", "calibrated_core",
            "first_grid", "forming_dies", "copper_coil", "flux_cell", "wet_beneficiation");
    // Fixed reference literals catch ID-algorithm drift, not just consistency with the current helper.
    private static final Map<String, String> QUEST_CODES = Map.ofEntries(
            Map.entry("starter_supplies", "13ED15B1E32DD8BC"),
            Map.entry("quest_book_help", "6A99C04A93E11C86"),
            Map.entry("superflat", "092574D2D973A909"),
            Map.entry("construction", "2F4BD54706D6EA91"),
            Map.entry("multitool", "2EEA2518B9B31086"),
            Map.entry("manual_work", "76F90DA0450DE13A"),
            Map.entry("crank", "6DA124A8051C553E"),
            Map.entry("extractor", "7188DAFE3CB3F0D1"),
            Map.entry("quern", "105CF368A37EACEE"),
            Map.entry("water", "4B9C34ACB1673761"),
            Map.entry("clay_mixer", "40CE27CC42AE0850"),
            Map.entry("grog", "0D6B628D86E29594"),
            Map.entry("firing", "0F687C5A28939988"),
            Map.entry("ceramics", "1F0B401F4349E651"),
            Map.entry("geological_collector", "1253C4E7A830BC08"),
            Map.entry("aggregate", "45CB5291CDD91EF7"),
            Map.entry("settling", "5FD9E0CFA1F91C1B"),
            Map.entry("water_recovery", "04A07AF8E87BCA01"),
            Map.entry("mineral_feed", "241EBD805A946011"),
            Map.entry("biocultivator", "08C3FDF1624E210A"),
            Map.entry("biomass", "16E68E86C4022FE0"),
            Map.entry("fibreboard", "1E7F9245277762B7"),
            Map.entry("biomass_dynamo", "35D93D78AD1ACA61"),
            Map.entry("fluid_logistics", "684AE8B585A63B2B"),
            Map.entry("conveyors", "1A0E7661F201661D"),
            Map.entry("workshop_flow", "214C2112FAE5310E"),
            Map.entry("assembly_bench", "3EF3F2D60936BAFB"),
            Map.entry("research_terminal", "3193F819EF4B8ADC"),
            Map.entry("data_core", "16BE36555BB14AEC"),
            Map.entry("calibrate", "35D01EE9AE4C3179"),
            Map.entry("calibrated_core", "3E5CDA84CF438AB4"),
            Map.entry("relay", "2AAD4F1B45E769E1"),
            Map.entry("thermal", "05D44AC79B6C3217"),
            Map.entry("pylon", "081F7E1F95227356"),
            Map.entry("first_grid", "278E3B6B9CA4D99F"),
            Map.entry("scanner", "7226DBABB6A96288"),
            Map.entry("terrestrial", "281DFBB3E8209E9B"),
            Map.entry("pulverizer", "56E23E1C96E4CA5B"),
            Map.entry("furnace", "039C9181A138E2A3"),
            Map.entry("press", "43960D3AF1C336FB"),
            Map.entry("forming_dies", "5F4267A350690B3D"),
            Map.entry("copper_coil", "549F2D015A5FD614"),
            Map.entry("assembler", "457C480A4603993C"),
            Map.entry("matrix_line", "4D2A547321F38B65"),
            Map.entry("glass", "472DDBB0690DCAF0"),
            Map.entry("logistics", "10B0AA0FD2761439"),
            Map.entry("kiln", "7397B3E4A5579B1D"),
            Map.entry("factory_world", "57835299D482EB9F"),
            Map.entry("flux_cell", "1DF5F96E1A895AF9"),
            Map.entry("industrial_matrix", "7D275307FFE56605"),
            Map.entry("wet_beneficiation", "321B876BD5C53950"));
    private static final List<String> STARTER_STACKS = List.of(
            "minecraft:cobblestone/64", "minecraft:cobblestone/64", "minecraft:oak_log/64",
            "minecraft:bread/64", "minecraft:brick/64", "minecraft:clay_ball/64", "minecraft:sand/64",
            "minecraft:charcoal/32", "minecraft:iron_ingot/16", "minecraft:water_bucket/1",
            "minecraft:water_bucket/1");
    private static int assertions;

    private VerifyFtbQuests() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--managed-files-only")) {
            VerifyFtbQuestFiles.main(new String[0]);
            return;
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("Expected no arguments or --managed-files-only");
        }
        try {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            VerifyFtbQuestFiles.main(new String[0]);
            Map<String, CompoundTag> documents = FtbQuestDefinitions.chapters();
            definitions(documents);
            wetDefinition(documents);
            old50Invariants(documents);
            matrixParser();
            System.out.println("FTB PRODUCTION SNBT/PARSER DATA ASSERTIONS: " + assertions);
            Files.createDirectories(Path.of("build"));
            Path fixture = Files.createTempDirectory(Path.of("build"), "ftbquests-native-checks-");
            System.out.println("FTB standalone fixture (production documents + SYNTHETIC vanilla graph; retained): " + fixture.toAbsolutePath());
            documentCodec(fixture, documents);
            System.out.println("SYNTHETIC_NATIVE_MECHANISM_ONLY: vanilla stone subjects, canonical IDs/counts/stock classes; "
                    + "NOT a production item graph, registered Matrix match or Frame UI.");
            Path folder = writeSyntheticFixture(fixture, documents);
            NativeQuestFile file = load(folder);
            syntheticDefinitions(file);
            NativeQuestFile reloaded = load(folder);
            equal("synthetic native chapter reload preserves every object", snapshot(file), snapshot(reloaded));
            equal("synthetic reload creates distinct quest instances", false,
                    quest(file, "multitool") == quest(reloaded, "multitool"));
            migration(file, folder, fixture);
            receiptsBeforeCompletion(folder);
            blockedReceiptRetry(folder);
            retainedT1(folder);
            addedT1Lessons(folder);
            equal("synthetic item substitution never changes production documents", documents, FtbQuestDefinitions.chapters());
            System.out.println("FTB STANDALONE DATA/SYNTHETIC NATIVE MECHANISM CHECKS PASSED");
        } finally {
            System.out.println("FTB NATIVE DATA ASSERTIONS: " + assertions);
            System.out.println("REQUIRED_NOT_PROVED: production native loader/reload, registered task/reward subjects, "
                    + "production-bound TeamData migration/individual receipts/Relay gates and registered Matrix matching "
                    + "require grindless_native_quest_checks under real Forge; retained-world persistence requires "
                    + "seed then reopen in another JVM.");
            System.out.println("REQUIRED_NOT_PROVED: actual player inventory submission, physical reward delivery, "
                    + "login/team events and client/JEI UI are not simulated by data or console fixtures.");
        }
    }

    private static void definitions(Map<String, CompoundTag> documents) throws java.io.IOException {
        equal("exactly the two native chapter filenames", Set.of("grindless_t0.snbt", "grindless_t1.snbt"),
                documents.keySet());
        equal("chapter generation is deterministic", documents, FtbQuestDefinitions.chapters());
        equal("all 50 previous logical IDs and the one reward-free wet lesson", QUEST_CODES.keySet(),
                QuestCatalogue.tasks().stream().map(QuestCatalogue.Task::id).collect(Collectors.toSet()));
        equal("known lineage independently matches every fixed current logical ID", QUEST_CODES.keySet(),
                FtbQuestDefinitionPublisher.knownLogicalIds(51));
        equal("publisher current is the complete source-bound catalogue", 51,
                FtbQuestDefinitionPublisher.currentQuestCount());
        equal("51 catalogue entries, without duplicate logical IDs", 51, QuestCatalogue.tasks().size());
        equal("exact 19 in-development T1 objectives", T1_IDS, QuestCatalogue.tasks().stream()
                .filter(task -> QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1)
                .map(QuestCatalogue.Task::id).collect(Collectors.toSet()));
        equal("exact 25 explicitly reward-free lessons", LESSON_IDS, QuestCatalogue.tasks().stream()
                .filter(task -> task.type() == QuestCatalogue.Type.LESSON)
                .map(QuestCatalogue.Task::id).collect(Collectors.toSet()));
        equal("26 material-reward quests", 26L, QuestCatalogue.tasks().stream()
                .filter(task -> task.type() == QuestCatalogue.Type.REWARD).count());
        equal("pinned T0 chapter ID", "7A7EC224FEFEF423", FtbQuestDefinitions.code(FtbQuestDefinitions.id("chapter", "T0")));
        equal("pinned T1 chapter ID", "37497611D3E03C27", FtbQuestDefinitions.code(FtbQuestDefinitions.id("chapter", "T1")));
        equal("native T1 navigation retains the development label", "gui.grindless.quest.chapter.t1_development",
                JsonParser.parseString(documents.get("grindless_t1.snbt").getString("title"))
                        .getAsJsonObject().get("translate").getAsString());
        equal("pinned starter task ID", "79DA10B2C2D84144", FtbQuestDefinitions.code(FtbQuestDefinitions.taskId("starter_supplies")));
        equal("pinned F1 task ID", "3F50FF4E69F07A79", FtbQuestDefinitions.code(FtbQuestDefinitions.taskId("relay")));
        equal("pinned F2 task ID", "55B3145C00EA45F6", FtbQuestDefinitions.code(FtbQuestDefinitions.taskId("industrial_matrix")));
        equal("pinned wet acquisition task ID", "5A532E5A96A81625",
                FtbQuestDefinitions.code(FtbQuestDefinitions.taskId("wet_beneficiation")));
        equal("pinned first starter receipt ID", "246F5A62BDCF22A4",
                FtbQuestDefinitions.code(FtbQuestDefinitions.rewardIds(QuestCatalogue.byId("starter_supplies")).get(0)));
        equal("pinned last starter receipt ID", "408264CC5294961D",
                FtbQuestDefinitions.code(FtbQuestDefinitions.rewardIds(QuestCatalogue.byId("starter_supplies")).get(10)));
        equal("pinned multitool receipt ID", "0B43AB58FA1F3068",
                FtbQuestDefinitions.code(FtbQuestDefinitions.rewardIds(QuestCatalogue.byId("multitool")).get(0)));
        Set<Long> ids = new LinkedHashSet<>();
        Set<String> questCodes = new LinkedHashSet<>();
        for (QuestCatalogue.Chapter chapter : QuestCatalogue.Chapter.values()) {
            CompoundTag document = documents.get(chapter == QuestCatalogue.Chapter.T0
                    ? "grindless_t0.snbt" : "grindless_t1.snbt");
            unique(ids, parseId(document), "chapter " + chapter);
            ListTag quests = document.getList("quests", Tag.TAG_COMPOUND);
            equal("chapter size " + chapter, chapter == QuestCatalogue.Chapter.T0 ? 32 : 19, quests.size());
            for (int index = 0; index < quests.size(); index++) {
                equal("no duplicate generated quest", true, questCodes.add(quests.getCompound(index).getString("id")));
            }
        }
        equal("no missing or foreign generated quest ID", Set.copyOf(QUEST_CODES.values()), questCodes);
        for (QuestCatalogue.Task declared : QuestCatalogue.tasks()) {
            CompoundTag quest = questDocument(documents, declared);
            var body = JsonParser.parseString(quest.getList("description", Tag.TAG_STRING).getString(0))
                    .getAsJsonObject();
            equal("native description selects the shared body key " + declared.id(),
                    QuestTeaching.bodyKey(declared), body.get("translate").getAsString());
            if (declared.id().equals("thermal")) {
                equal("native Thermal text keeps its one live substitution", 1, body.getAsJsonArray("with").size());
                equal("native Thermal text binds actual FU/t", ThermalLogic.FU_PER_TICK,
                        body.getAsJsonArray("with").get(0).getAsLong());
            } else if (declared.id().equals("first_grid")) {
                equal("native first-grid text keeps both live substitutions", 2, body.getAsJsonArray("with").size());
                equal("native first-grid text binds the actual F0 rate", FluxTier.F0.nominal(),
                        body.getAsJsonArray("with").get(0).getAsLong());
                equal("native first-grid text binds the actual F1 rate", FluxTier.F1.nominal(),
                        body.getAsJsonArray("with").get(1).getAsLong());
            }
            equal("fixed quest ID " + declared.id(), QUEST_CODES.get(declared.id()),
                    FtbQuestDefinitions.code(FtbQuestDefinitions.questId(declared.id())));
            unique(ids, parseId(quest), "quest " + declared.id());
            equal("one native task " + declared.id(), 1, quest.getList("tasks", Tag.TAG_COMPOUND).size());
            CompoundTag task = quest.getList("tasks", Tag.TAG_COMPOUND).getCompound(0);
            unique(ids, parseId(task), "task " + declared.id());
            equal("task helper binds the generated ID " + declared.id(), FtbQuestDefinitions.taskId(declared.id()), parseId(task));
            equal("native evidence type " + declared.id(), taskType(declared), task.getString("type"));
            equal("no custom task binding " + declared.id(), false, task.contains("grindless_task"));
            if (declared.evidence() == QuestCatalogue.Evidence.MATRIX) {
                equal("Matrix uses an ordinary native item compound " + declared.id(), true,
                        task.get("item") instanceof CompoundTag);
                matrixFilterData("generated " + declared.id(), task.getCompound("item"), declared);
            }
            ListTag rewards = quest.getList("rewards", Tag.TAG_COMPOUND);
            equal("canonical reward count " + declared.id(), declared.rewards().size(), rewards.size());
            for (int index = 0; index < rewards.size(); index++) {
                CompoundTag reward = rewards.getCompound(index);
                unique(ids, parseId(reward), "reward " + declared.id() + "/" + index);
                equal("reward helper binds the generated receipt " + declared.id() + "/" + index,
                        FtbQuestDefinitions.rewardIds(declared).get(index), parseId(reward));
                equal("native item reward " + declared.id(), "ftbquests:item", reward.getString("type"));
                equal("individual reward " + declared.id(), false, reward.getBoolean("team_reward"));
                equal("explicitly no autoclaim " + declared.id(), "disabled", reward.getString("auto"));
                equal("canonical reward item " + declared.id(), declared.rewards().get(index).itemId(), reward.getString("item"));
                equal("canonical reward quantity " + declared.id(), declared.rewards().get(index).count(), reward.getInt("count"));
            }
        }
        equal("two chapters, 51 quests, 51 tasks and the unchanged 36 item receipts", 140, ids.size());
        equal("the exact ordered eleven starter stacks, including both duplicate pairs", STARTER_STACKS,
                QuestCatalogue.byId(QuestCatalogue.STARTER_SUPPLIES).rewards().stream()
                        .map(reward -> reward.itemId() + "/" + reward.count()).toList());
    }

    private static void wetDefinition(Map<String, CompoundTag> documents) {
        QuestCatalogue.Task declared = QuestCatalogue.byId("wet_beneficiation");
        CompoundTag quest = questDocument(documents, declared);
        CompoundTag task = quest.getList("tasks", Tag.TAG_COMPOUND).getCompound(0);
        equal("wet acquisition uses only the stock native item task", "ftbquests:item", task.getString("type"));
        equal("wet acquisition names the actual machine, not a custom guide or carrier",
                "grindless:chemical_washer", task.getString("item"));
        equal("wet acquisition observes one machine", 1L, task.getLong("count"));
        equal("wet acquisition never consumes its machine", false, task.getBoolean("consume_items"));
        equal("wet acquisition allows ordinary possession, not only a new craft", false,
                task.getBoolean("only_from_crafting"));
        equal("wet acquisition is an explicitly optional native lesson", true, QuestTeaching.optional(declared));
        equal("wet acquisition adds no material receipt", List.of(), FtbQuestDefinitions.rewardIds(declared));
        equal("wet acquisition has no native reward", 0, quest.getList("rewards", Tag.TAG_COMPOUND).size());
        equal("wet acquisition follows only the Assembler plus the existing Relay chapter gate",
                List.of("assembler", "relay"), QuestCatalogue.requirements(declared));
        ListTag dependencies = quest.getList("dependencies", Tag.TAG_STRING);
        equal("wet acquisition has exactly its two native dependencies", 2, dependencies.size());
        equal("the native Assembler dependency retains its fixed identity", QUEST_CODES.get("assembler"),
                dependencies.getString(0));
        equal("the native Relay chapter gate retains its fixed identity", QUEST_CODES.get("relay"),
                dependencies.getString(1));
        CompoundTag assembler = questDocument(documents, QuestCatalogue.byId("assembler"));
        equal("wet acquisition uses the existing next-depth native spacing",
                assembler.getDouble("x") + 2.5, quest.getDouble("x"));
        for (QuestCatalogue.Task previous : QuestCatalogue.tasks().subList(0, 50)) {
            if (QuestCatalogue.chapter(previous) == QuestCatalogue.Chapter.T1) {
                equal("the appended wet line cannot overlap an old T1 node " + previous.id(), true,
                        questDocument(documents, previous).getDouble("y") < quest.getDouble("y"));
            }
            equal("no previous objective gains the optional wet prerequisite " + previous.id(), false,
                    QuestCatalogue.requirements(previous).contains("wet_beneficiation"));
        }
    }

    private static void old50Invariants(Map<String, CompoundTag> documents) throws Exception {
        StringBuilder identities = new StringBuilder();
        StringBuilder positions = new StringBuilder();
        for (QuestCatalogue.Task previous : QuestCatalogue.tasks().subList(0, 50).stream()
                .sorted(Comparator.comparing(QuestCatalogue.Task::id)).toList()) {
            identities.append(previous.id()).append('\0')
                    .append(FtbQuestDefinitions.code(FtbQuestDefinitions.questId(previous.id()))).append('\0')
                    .append(FtbQuestDefinitions.code(FtbQuestDefinitions.taskId(previous.id()))).append('\0')
                    .append(FtbQuestDefinitions.rewardIds(previous).stream().map(FtbQuestDefinitions::code)
                            .collect(Collectors.joining(","))).append('\n');
            CompoundTag quest = questDocument(documents, previous);
            positions.append(previous.id()).append('\0')
                    .append(String.format(Locale.ROOT, "%.1f", quest.getDouble("x"))).append('\0')
                    .append(String.format(Locale.ROOT, "%.1f", quest.getDouble("y"))).append('\n');
        }
        // e4cc108: sorted old50 IDs; UTF-8 rows use NUL fields and final LF.
        equal("all old50 quest/task and ordered reward identities remain fixed",
                "7eba2fe0961882cecdff00710abaefe0786990b1af94723fa9262212b3253afd",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(identities.toString().getBytes(StandardCharsets.UTF_8))));
        equal("all old50 native positions remain fixed",
                "92b18d1aa0acb1697d52bfb8df0810b558dfcda3ea1bf10a98d616d3c5dbe0b3",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(positions.toString().getBytes(StandardCharsets.UTF_8))));
    }

    /** Real authored SNBT documents only: do not instantiate their platform-dependent item tasks. */
    private static void documentCodec(Path fixture, Map<String, CompoundTag> documents) throws Exception {
        Path config = fixture.resolve("config");
        FtbQuestFiles.ensure(config);
        for (Map.Entry<String, CompoundTag> entry : documents.entrySet()) {
            CompoundTag read = SNBT.read(config.resolve("ftbquests/quests/chapters").resolve(entry.getKey()));
            equal("FTB SNBT reader returns a native document tag " + entry.getKey(), true, read instanceof SNBTCompoundTag);
            equal("authored chapter SNBT document codec, not a loaded quest graph " + entry.getKey(), entry.getValue(), read);
        }
    }

    /** No fallback: a predetermined, private synthetic codec fixture, never production config. */
    private static Path writeSyntheticFixture(Path fixture, Map<String, CompoundTag> documents) throws Exception {
        Path folder = fixture.resolve("synthetic-vanilla-codec");
        Files.createDirectories(folder.resolve("chapters"));
        for (Map.Entry<String, CompoundTag> entry : documents.entrySet()) {
            CompoundTag copy = entry.getValue().copy();
            ListTag quests = copy.getList("quests", Tag.TAG_COMPOUND);
            for (int index = 0; index < quests.size(); index++) {
                CompoundTag quest = quests.getCompound(index);
                ListTag tasks = quest.getList("tasks", Tag.TAG_COMPOUND);
                for (int taskIndex = 0; taskIndex < tasks.size(); taskIndex++) {
                    CompoundTag task = tasks.getCompound(taskIndex);
                    if (task.getString("type").equals("ftbquests:item")) {
                        if (task.get("item") instanceof CompoundTag stack) {
                            stack.putString("id", "minecraft:stone");
                        } else {
                            task.putString("item", "minecraft:stone");
                        }
                    }
                }
                ListTag rewards = quest.getList("rewards", Tag.TAG_COMPOUND);
                for (int rewardIndex = 0; rewardIndex < rewards.size(); rewardIndex++) {
                    rewards.getCompound(rewardIndex).putString("item", "minecraft:stone");
                }
            }
            Files.writeString(folder.resolve("chapters").resolve(entry.getKey()), copy.toString());
        }
        return folder;
    }

    /** These loaded stock classes have synthetic item subjects, not production item registration. */
    private static void syntheticDefinitions(BaseQuestFile file) {
        equal("synthetic native chapter count", 2, file.getAllChapters().size());
        equal("synthetic native quest count", 51, file.getAllChapters().stream().mapToInt(chapter -> chapter.getQuests().size()).sum());
        equal("synthetic native task count", 51, file.getAllTasks().size());
        equal("synthetic native receipt count", 36L, file.getAllObjects().stream().filter(Reward.class::isInstance).count());
        TeamData empty = new TeamData(TEAM, file);
        for (QuestCatalogue.Task declared : QuestCatalogue.tasks()) {
            Quest quest = quest(file, declared.id());
            Task task = task(file, declared.id());
            equal("synthetic native chapter membership " + declared.id(), true,
                    quest.getChapter() == file.getChapter(FtbQuestDefinitions.id("chapter", QuestCatalogue.chapter(declared).name())));
            equal("synthetic native task membership " + declared.id(), true,
                    task.getQuest() == quest && quest.getTasks().size() == 1 && quest.getTasks().iterator().next() == task);
            equal("synthetic stock task class " + declared.id(), switch (declared.evidence()) {
                case ITEM, MATRIX -> ItemTask.class;
                case FREE -> CheckmarkTask.class;
                case DIMENSION -> DimensionTask.class;
            }, task.getClass());
            equal("synthetic native task type " + declared.id(), taskType(declared), task.getType().getTypeId().toString());
            equal("synthetic preserves production task count " + declared.id(), (long) declared.count(), task.getMaxProgress());
            equal("synthetic native quest is not repeatable " + declared.id(), false, quest.canBeRepeated());
            equal("synthetic native linear progression " + declared.id(), ProgressionMode.LINEAR, quest.getProgressionMode());
            equal("synthetic native dependency bindings " + declared.id(),
                    QuestCatalogue.requirements(declared).stream().map(FtbQuestDefinitions::questId).toList(),
                    quest.streamDependencies().map(dependency -> dependency.id).toList());
            for (String dependency : QuestCatalogue.requirements(declared)) {
                equal("synthetic native dependency object " + declared.id() + "/" + dependency, true,
                        quest.hasDependency(quest(file, dependency)));
            }
            boolean t1 = QuestCatalogue.chapter(declared) == QuestCatalogue.Chapter.T1;
            equal("synthetic native detail gate " + declared.id(), t1, quest.hideDetailsUntilStartable());
            equal("synthetic native initial visibility " + declared.id(), !t1, quest.isVisible(empty));
            if (t1) {
                equal("synthetic native T1 cannot start without Relay " + declared.id(), false, empty.canStartTasks(quest));
            }
            if (task instanceof ItemTask item) {
                equal("synthetic task uses only registered vanilla stone " + declared.id(), Items.STONE, item.getItemStack().getItem());
                equal("synthetic native task does not consume items " + declared.id(), false, item.consumesResources());
                equal("synthetic native inventory observation enabled " + declared.id(), true, item.submitItemsOnInventoryChange());
                equal("synthetic native task is not crafting-only " + declared.id(), false, item.isOnlyFromCrafting());
                equal("synthetic native task is not task-screen-only " + declared.id(), false, item.isTaskScreenOnly());
            }
            List<Reward> rewards = List.copyOf(quest.getRewards());
            equal("synthetic preserves production reward cardinality " + declared.id(), declared.rewards().size(), rewards.size());
            for (int index = 0; index < rewards.size(); index++) {
                Reward reward = rewards.get(index);
                equal("synthetic stock ItemReward " + declared.id(), ItemReward.class, reward.getClass());
                equal("synthetic preserves production reward ID " + declared.id(), FtbQuestDefinitions.rewardIds(declared).get(index), reward.id);
                equal("synthetic native reward lookup/parent " + declared.id(), true,
                        file.getReward(reward.id) == reward && reward.getQuest() == quest);
                equal("synthetic native reward is individual " + declared.id(), false, reward.isTeamReward());
                equal("synthetic native autoclaim disabled " + declared.id(), RewardAutoClaim.DISABLED, reward.getAutoClaimType());
                equal("synthetic reward uses only registered vanilla stone " + declared.id(), Items.STONE, ((ItemReward) reward).getItem().getItem());
                equal("synthetic preserves production reward quantity " + declared.id(), declared.rewards().get(index).count(), ((ItemReward) reward).getCount());
                CompoundTag rewardData = new CompoundTag();
                reward.writeData(rewardData);
                equal("synthetic native reward has no random bonus " + declared.id(), 0, rewardData.getInt("random_bonus"));
                equal("synthetic native reward is not only-one " + declared.id(), false, rewardData.getBoolean("only_one"));
            }
        }
        equal("synthetic native starter is still first in T0", FtbQuestDefinitions.questId(QuestCatalogue.STARTER_SUPPLIES),
                file.getChapter(FtbQuestDefinitions.id("chapter", "T0")).getQuests().iterator().next().id);
        // No DIMENSION objective exists in this catalogue; this is only the stock reader's codec.
        DimensionTask dimension = new DimensionTask(FtbQuestDefinitions.taskId("factory_world"), quest(file, "factory_world"));
        CompoundTag dimensionData = new CompoundTag();
        dimensionData.putString("dimension", "minecraft:overworld");
        dimension.readData(dimensionData);
        CompoundTag dimensionRoundTrip = new CompoundTag();
        dimension.writeData(dimensionRoundTrip);
        equal("synthetic stock dimension resource-key codec", "minecraft:overworld", dimensionRoundTrip.getString("dimension"));
        equal("stock dimension reader type", "ftbquests:dimension", dimension.getType().getTypeId().toString());
        equal("stock dimension task is boolean", 1L, dimension.getMaxProgress());
    }

    private static void addedT1Lessons(Path folder) {
        BaseQuestFile file = load(folder);
        Set<String> historical = QUEST_CODES.keySet().stream().filter(id -> !ADDED_T1_LESSONS.contains(id))
                .collect(Collectors.toSet());
        equal("the historical import remains exactly 46 logical IDs", 46, historical.size());
        QuestProgress legacy = paid(FIRST, historical);
        TeamData data = new TeamData(TEAM, file);
        FtbQuestNative.migrate(legacy, FIRST, data);
        equal("adding lessons preserves every old paid receipt", historical, legacy.legacyClaims(FIRST));
        equal("adding lessons preserves the native authority fence", true, legacy.usesNativeClaims(FIRST));
        equal("the five added lessons add no individual material receipts", 36,
                data.serializeNBT().getCompound("claimed_rewards").size());
        for (String id : ADDED_T1_LESSONS) {
            equal("the added T1 node is a reward-free lesson " + id, QuestCatalogue.Type.LESSON,
                    QuestCatalogue.byId(id).type());
            equal("a new lesson invents no reward " + id, List.of(), QuestCatalogue.byId(id).rewards());
            equal("old history cannot complete a new lesson " + id, false, data.isCompleted(quest(file, id)));
            equal("old history cannot complete its new task " + id, false, data.isCompleted(task(file, id)));
            equal("old history cannot seed new lesson progress " + id, 0L, data.getProgress(task(file, id)));
        }
        equal("native compatibility contains only the historical completion", historical, legacy.claimed(FIRST));
        SNBTCompoundTag imported = data.serializeNBT();
        FtbQuestNative.migrate(legacy, FIRST, data);
        equal("repeat import cannot advance the added lessons", imported, data.serializeNBT());
        Set<String> previous50 = QUEST_CODES.keySet().stream().filter(id -> !id.equals("wet_beneficiation"))
                .collect(Collectors.toSet());
        equal("the immediately preceding source catalogue has exactly 50 IDs", 50, previous50.size());
        QuestProgress old50 = paid(SECOND, previous50);
        TeamData upgrade50 = new TeamData(TEAM, file);
        FtbQuestNative.migrate(old50, SECOND, upgrade50);
        equal("all 50 previous completions remain exact", previous50, old50.claimed(SECOND));
        equal("the old50 import retains the native paid-history fence", true, old50.usesNativeClaims(SECOND));
        equal("the old50 import retains exactly the same material receipts", 36,
                upgrade50.serializeNBT().getCompound("claimed_rewards").size());
        equal("all old50 history cannot invent new Washer acquisition", false,
                upgrade50.isCompleted(quest(file, "wet_beneficiation")));
        equal("all old50 history cannot invent new acquisition progress", 0L,
                upgrade50.getProgress(task(file, "wet_beneficiation")));
        SNBTCompoundTag imported50 = upgrade50.serializeNBT();
        FtbQuestNative.migrate(old50, SECOND, upgrade50);
        equal("reimporting old50 cannot advance or repay the new lesson", imported50, upgrade50.serializeNBT());
    }

    /** Real native migration API on the explicitly synthetic vanilla-stock graph. */
    private static void migration(BaseQuestFile file, Path folder, Path fixture) {
        QuestProgress legacy = paid(FIRST, PAID_START);
        TeamData data = new TeamData(TEAM, file);
        seedCompletion(data, "multitool", NATIVE_DATE);
        seedCompletion(data, "crank", NATIVE_DATE);
        data.setProgress(task(file, "conveyors"), 3L);
        FtbQuestNative.migrate(legacy, FIRST, data);
        equal("migration permanently fences the old reward authority", true, legacy.usesNativeClaims(FIRST));
        equal("only historical paid claims remain import receipts", PAID_START, legacy.legacyClaims(FIRST));
        equal("old starter and multitool import exactly twelve native receipts", 12,
                data.serializeNBT().getCompound("claimed_rewards").size());
        assertReceipts(data, FIRST, QuestCatalogue.STARTER_SUPPLIES, true);
        assertReceipts(data, FIRST, "multitool", true);
        equal("paid starter is native complete", true, data.isCompleted(quest(file, QuestCatalogue.STARTER_SUPPLIES)));
        equal("paid starter task is explicitly complete", true, data.isCompleted(task(file, QuestCatalogue.STARTER_SUPPLIES)));
        equal("paid multitool task reaches its native maximum", task(file, "multitool").getMaxProgress(), data.getProgress(task(file, "multitool")));
        equal("import preserves an already completed native timestamp", NATIVE_DATE, data.getCompletedTime(quest(file, "multitool").id).orElseThrow());
        equal("native forward partial progress is not overwritten", 3L, data.getProgress(task(file, "conveyors")));
        equal("native forward completion is not reset", true, data.isCompleted(quest(file, "crank")));
        equal("native completion refreshes common compatibility", Set.of("starter_supplies", "multitool", "crank"), legacy.claimed(FIRST));
        equal("native completion is not an old paid crank receipt", false, legacy.legacyClaims(FIRST).contains("crank"));
        assertReceipts(data, FIRST, "crank", false);
        equal("a native-complete unpaid reward remains claimable only through FTB", RewardClaimType.CAN_CLAIM,
                data.getClaimType(FIRST, reward(file, "crank", 0)));
        equal("the old mint cannot add another receipt after handoff", false, legacy.claim(FIRST, "quern"));
        equal("fencing leaves all old paid receipts intact", PAID_START, legacy.legacyClaims(FIRST));
        SNBTCompoundTag imported = data.serializeNBT();
        FtbQuestNative.migrate(legacy, FIRST, data);
        equal("repeated import changes no native progress, dates or receipts", imported, data.serializeNBT());

        FtbQuestNative.migrate(legacy, SECOND, data);
        equal("the teammate uses the exact same real TeamData", true, data.getFile() == file);
        equal("the teammate's historical receipts remain empty", Set.of(), legacy.legacyClaims(SECOND));
        equal("team completion is visible without inventing an individual receipt", true,
                legacy.claimed(SECOND).contains(QuestCatalogue.STARTER_SUPPLIES));
        assertReceipts(data, SECOND, QuestCatalogue.STARTER_SUPPLIES, false);
        assertReceipts(data, SECOND, "multitool", false);
        equal("same-team completion does not consume another UUID's starter reward", RewardClaimType.CAN_CLAIM,
                data.getClaimType(SECOND, reward(file, QuestCatalogue.STARTER_SUPPLIES, 0)));
        equal("native bookkeeping accepts one teammate receipt independently", true,
                data.markRewardAsClaimed(SECOND, reward(file, QuestCatalogue.STARTER_SUPPLIES, 0), NATIVE_DATE.getTime()));
        for (int index = 0; index < 11; index++) {
            equal("teammate owns only the individually recorded starter receipt " + index, index == 0,
                    data.isRewardClaimed(SECOND, reward(file, QuestCatalogue.STARTER_SUPPLIES, index)));
        }
        FtbQuestNative.migrate(legacy, SECOND, data);
        equal("teammate re-import never turns common derived completion into paid receipts", Set.of(), legacy.legacyClaims(SECOND));
        equal("teammate import does not auto-claim the other ten starter stacks", 13,
                data.serializeNBT().getCompound("claimed_rewards").size());
        assertReceipts(data, FIRST, QuestCatalogue.STARTER_SUPPLIES, true);
        QuestProgress rejoinedLegacy = QuestProgress.load(legacy.save(new CompoundTag()));
        equal("the persisted migration fence survives legacy reload", true,
                rejoinedLegacy.usesNativeClaims(FIRST) && rejoinedLegacy.usesNativeClaims(SECOND));
        equal("native completed observation is not persisted as an old receipt", PAID_START, rejoinedLegacy.claimed(FIRST));
        equal("the teammate's derived completion is also non-persistent", Set.of(), rejoinedLegacy.claimed(SECOND));

        Path progressFile = fixture.resolve("synthetic-team-codec.snbt");
        SNBTCompoundTag beforeReload = data.serializeNBT();
        SNBT.write(progressFile, beforeReload);
        CompoundTag read = SNBT.read(progressFile);
        equal("FTB's actual SNBT reader returns its native tag type", true, read instanceof SNBTCompoundTag);
        BaseQuestFile reloadedFile = load(folder);
        TeamData rejoined = new TeamData(TEAM, reloadedFile);
        rejoined.deserializeNBT((SNBTCompoundTag) read);
        equal("synthetic native TeamData SNBT codec, not a world restart", beforeReload, rejoined.serializeNBT());
        equal("reload binds genuinely new quest objects", false,
                quest(file, "multitool") == quest(reloadedFile, "multitool"));
        FtbQuestNative.migrate(rejoinedLegacy, FIRST, rejoined);
        FtbQuestNative.migrate(rejoinedLegacy, SECOND, rejoined);
        equal("rejoin/import preserves all native serialized state", beforeReload, rejoined.serializeNBT());
        equal("rejoin retains native forward partial progress", 3L, rejoined.getProgress(task(reloadedFile, "conveyors")));
        equal("rejoin retains native forward completion date", NATIVE_DATE,
                rejoined.getCompletedTime(quest(reloadedFile, "crank").id).orElseThrow());
        equal("rejoin rebuilds common compatibility from native completion", Set.of("starter_supplies", "multitool", "crank"),
                rejoinedLegacy.claimed(FIRST));
        equal("rejoin still has only the historical old receipts", PAID_START, rejoinedLegacy.legacyClaims(FIRST));
        assertReceipts(rejoined, FIRST, QuestCatalogue.STARTER_SUPPLIES, true);
        assertReceipts(rejoined, FIRST, "multitool", true);
        equal("the teammate's one independent receipt survives native SNBT", true,
                rejoined.isRewardClaimed(SECOND, reward(reloadedFile, QuestCatalogue.STARTER_SUPPLIES, 0)));
        equal("the teammate's other starter receipt remains unpaid after rejoin", false,
                rejoined.isRewardClaimed(SECOND, reward(reloadedFile, QuestCatalogue.STARTER_SUPPLIES, 1)));
        assertReceipts(rejoined, SECOND, "multitool", false);
        rejoined.setCompleted(quest(reloadedFile, "crank").id, null);
        rejoined.setProgress(task(reloadedFile, "crank"), 0L);
        FtbQuestNative.refresh(rejoinedLegacy, FIRST, rejoined);
        equal("common compatibility is replaceable observation, not sticky paid progress", PAID_START, rejoinedLegacy.claimed(FIRST));
        FtbQuestNative.migrate(rejoinedLegacy, FIRST, rejoined);
        equal("a previously observed native completion cannot be re-imported as an old claim", false,
                rejoined.isCompleted(quest(reloadedFile, "crank")));
        assertReceipts(rejoined, FIRST, "crank", false);
    }

    private static void receiptsBeforeCompletion(Path folder) {
        BaseQuestFile file = load(folder);
        QuestProgress legacy = paid(FIRST, PAID_START);
        TeamData data = new TeamData(TEAM, file);
        Reward missing = reward(file, "multitool", 0);
        // Corrupt only a freshly loaded synthetic graph, never production files or the server graph.
        missing.getQuest().removeReward(missing);
        file.refreshIDMap();
        fails("a missing later receipt stops migration", IllegalStateException.class,
                () -> FtbQuestNative.migrate(legacy, FIRST, data));
        equal("even failed admission retires the old mint", true, legacy.usesNativeClaims(FIRST));
        equal("a failed import preserves the historical retry receipts", PAID_START, legacy.legacyClaims(FIRST));
        assertReceipts(data, FIRST, QuestCatalogue.STARTER_SUPPLIES, true);
        equal("all receipts are imported before ANY old completion", false,
                data.isCompleted(quest(file, QuestCatalogue.STARTER_SUPPLIES)));
        equal("failed receipt phase imports no earlier task progress", 0L,
                data.getProgress(task(file, QuestCatalogue.STARTER_SUPPLIES)));
        equal("failed receipt phase imports no later completion", false, data.isCompleted(quest(file, "multitool")));
        Date receiptBeforeRetry = data.getRewardClaimTime(FIRST, reward(file, "starter_supplies", 0)).orElseThrow();
        missing.getQuest().addReward(missing);
        file.refreshIDMap();
        FtbQuestNative.migrate(legacy, FIRST, data);
        assertReceipts(data, FIRST, "multitool", true);
        equal("repair/retry preserves receipts already recorded before interruption",
                receiptBeforeRetry, data.getRewardClaimTime(FIRST, reward(file, "starter_supplies", 0)).orElseThrow());
        equal("repair/retry completes the paid starter", true, data.isCompleted(quest(file, QuestCatalogue.STARTER_SUPPLIES)));
        equal("repair/retry completes the paid multitool", true, data.isCompleted(quest(file, "multitool")));
        equal("repair/retry records no duplicate receipt", 12, data.serializeNBT().getCompound("claimed_rewards").size());
    }

    private static void blockedReceiptRetry(Path folder) {
        BaseQuestFile file = load(folder);
        TeamData data = new TeamData(TEAM, file);
        QuestProgress legacy = paid(FIRST, PAID_START);
        data.setRewardsBlocked(true);
        fails("native refusal to record a receipt is not a successful import", IllegalStateException.class,
                () -> FtbQuestNative.migrate(legacy, FIRST, data));
        equal("blocked import still fences legacy delivery", true, legacy.usesNativeClaims(FIRST));
        equal("native blocked import records no receipt", 0, data.serializeNBT().getCompound("claimed_rewards").size());
        equal("native blocked import records no completion", false, data.isCompleted(quest(file, QuestCatalogue.STARTER_SUPPLIES)));
        data.setRewardsBlocked(false);
        FtbQuestNative.migrate(legacy, FIRST, data);
        assertReceipts(data, FIRST, QuestCatalogue.STARTER_SUPPLIES, true);
        assertReceipts(data, FIRST, "multitool", true);
        equal("retry after native unblock retains original paid set", PAID_START, legacy.legacyClaims(FIRST));
    }

    private static void retainedT1(Path folder) {
        BaseQuestFile file = load(folder);
        Set<String> oldT1 = Set.of("thermal", "industrial_matrix");
        QuestProgress legacy = paid(FIRST, oldT1);
        TeamData data = new TeamData(TEAM, file);
        FtbQuestNative.migrate(legacy, FIRST, data);
        for (String id : oldT1) {
            assertReceipts(data, FIRST, id, true);
            equal("old paid T1 receipt is retained without inventing gated completion " + id, false, data.isCompleted(quest(file, id)));
            equal("old paid T1 receipt does not invent task progress " + id, 0L, data.getProgress(task(file, id)));
        }
        equal("retained T1 paid receipts remain unchanged", oldT1, legacy.legacyClaims(FIRST));
        equal("retained receipts alone cannot unlock T1 compatibility", false,
                QuestCatalogue.unlocked(QuestCatalogue.Chapter.T1, legacy.claimed(FIRST)));
        equal("new T1 compatibility action remains Relay-gated despite old thermal payment", QuestLogic.Status.LOCKED,
                QuestLogic.consider(QuestCatalogue.byId("pylon"), legacy.claimed(FIRST), true));
        equal("new T1 native reward is not claimable", RewardClaimType.CANT_CLAIM, data.getClaimType(FIRST, reward(file, "pylon", 0)));

        TeamData gate = new TeamData(new UUID(0, 17), file);
        for (String id : T1_IDS) {
            for (String dependency : QuestCatalogue.requirements(QuestCatalogue.byId(id))) {
                if (!dependency.equals(QuestCatalogue.T0_COMPLETION)) {
                    gate.setCompleted(quest(file, dependency).id, NATIVE_DATE);
                }
            }
        }
        for (String id : T1_IDS) {
            equal("Relay is a real native start gate even with other dependencies complete " + id, false,
                    gate.canStartTasks(quest(file, id)));
            equal("Relay is a real native visibility gate " + id, false, quest(file, id).isVisible(gate));
        }
        gate.setCompleted(quest(file, QuestCatalogue.T0_COMPLETION).id, NATIVE_DATE);
        for (String id : T1_IDS) {
            equal("native Relay completion actually releases the start gate " + id, true, gate.canStartTasks(quest(file, id)));
            equal("native Relay completion actually releases the visibility gate " + id, true, quest(file, id).isVisible(gate));
        }

        seedCompletion(data, QuestCatalogue.T0_COMPLETION, NATIVE_DATE);
        FtbQuestNative.refresh(legacy, FIRST, data);
        FtbQuestNative.migrate(legacy, FIRST, data);
        equal("native Relay observation unlocks common compatibility", true,
                QuestCatalogue.unlocked(QuestCatalogue.Chapter.T1, legacy.claimed(FIRST)));
        equal("native Relay observation must not become a historical paid import receipt", oldT1, legacy.legacyClaims(FIRST));
        assertReceipts(data, FIRST, QuestCatalogue.T0_COMPLETION, false);
        for (String id : oldT1) {
            equal("re-import uses historical receipts, not the derived native Relay observation " + id, false,
                    data.isCompleted(quest(file, id)));
            assertReceipts(data, FIRST, id, true);
        }
        QuestProgress grandfathered = paid(SECOND, Set.of("relay", "thermal", "industrial_matrix"));
        TeamData historicalRelay = new TeamData(new UUID(0, 18), file);
        FtbQuestNative.migrate(grandfathered, SECOND, historicalRelay);
        for (String id : Set.of("relay", "thermal", "industrial_matrix")) {
            equal("a historical paid Relay permits historical unlocked completion " + id, true,
                    historicalRelay.isCompleted(quest(file, id)));
            assertReceipts(historicalRelay, SECOND, id, true);
        }
    }

    private static void matrixParser() {
        equal("legacy untagged Relay is physical F1", FluxTier.F1,
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, null).orElseThrow().rating());
        equal("empty legacy NBT also remains F1", FluxTier.F1,
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, new CompoundTag()).orElseThrow().rating());
        CompoundTag unrelated = new CompoundTag();
        unrelated.putString("Unrelated", "preserve");
        equal("unrelated legacy NBT does not invalidate F1", FluxTier.F1,
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, unrelated).orElseThrow().rating());
        for (FluxTier rating : List.of(FluxTier.F1, FluxTier.F2, FluxTier.F3)) {
            CompoundTag tag = ControlMatrixNbt.save(new ControlMatrixSpec(MatrixArchitecture.RELAY, rating));
            equal("the real physical parser retains integral rating " + rating, rating,
                    ControlMatrixNbt.load(MatrixArchitecture.RELAY, tag).orElseThrow().rating());
            equal("only physical F1 satisfies the T0 rating " + rating, rating == FluxTier.F1,
                    ControlMatrixNbt.load(MatrixArchitecture.RELAY, tag).orElseThrow().rating()
                            == QuestCatalogue.byId("relay").matrixRating());
            equal("only physical F2 satisfies the T1 rating " + rating, rating == FluxTier.F2,
                    ControlMatrixNbt.load(MatrixArchitecture.RELAY, tag).orElseThrow().rating()
                            == QuestCatalogue.byId("industrial_matrix").matrixRating());
        }
        List<Consumer<CompoundTag>> malformed = List.of(
                tag -> tag.putString(ControlMatrixNbt.RATING, "1"),
                tag -> tag.put(ControlMatrixNbt.RATING, new CompoundTag()),
                tag -> tag.put(ControlMatrixNbt.RATING, new ListTag()),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, 1.5D),
                tag -> tag.putFloat(ControlMatrixNbt.RATING, 1.25F),
                tag -> tag.putInt(ControlMatrixNbt.RATING, 0),
                tag -> tag.putInt(ControlMatrixNbt.RATING, -1),
                tag -> tag.putInt(ControlMatrixNbt.RATING, 4),
                tag -> tag.putInt(ControlMatrixNbt.RATING, 16),
                tag -> tag.putLong(ControlMatrixNbt.RATING, 4_294_967_297L),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, Double.NaN),
                tag -> tag.putDouble(ControlMatrixNbt.RATING, Double.POSITIVE_INFINITY));
        for (int index = 0; index < malformed.size(); index++) {
            CompoundTag tag = unrelated.copy();
            malformed.get(index).accept(tag);
            CompoundTag before = tag.copy();
            equal("malformed explicit Matrix never falls back to untagged F1, case " + index, true,
                    ControlMatrixNbt.load(MatrixArchitecture.RELAY, tag).isEmpty());
            equal("physical parser is observational, case " + index, before, tag);
        }
    }

    private static void matrixFilterData(String label, CompoundTag physical, QuestCatalogue.Task declared) {
        equal(label + " filter retains canonical physical item", declared.subject(), physical.getString("id"));
        equal(label + " filter is one representative stack, not the objective quantity", 1, physical.getInt("Count"));
        equal(label + " filter binds the exact logical quest", declared.id(), physical.getCompound("tag").getString(MATRIX_FILTER));
        equal(label + " filter retains the exact physical rating", declared.matrixRating().ordinal(),
                physical.getCompound("tag").getInt(ControlMatrixNbt.RATING));
    }

    private static QuestProgress paid(UUID player, Set<String> ids) {
        QuestProgress legacy = new QuestProgress();
        for (String id : ids) {
            equal("seed historical paid receipt " + id, true, legacy.claim(player, id));
        }
        return legacy;
    }

    /** Native bookkeeping in the synthetic graph, never player submission or item delivery. */
    private static void seedCompletion(TeamData data, String id, Date when) {
        Task task = task(data.getFile(), id);
        data.setProgress(task, task.getMaxProgress());
        data.setCompleted(task.id, when);
        data.setCompleted(quest(data.getFile(), id).id, when);
    }

    private static void assertReceipts(TeamData data, UUID player, String id, boolean claimed) {
        for (long rewardId : FtbQuestDefinitions.rewardIds(QuestCatalogue.byId(id))) {
            Reward reward = data.getFile().getReward(rewardId);
            equal("native receipt " + player + "/" + id + "/" + FtbQuestDefinitions.code(rewardId), claimed,
                    data.isRewardClaimed(player, reward));
            if (claimed) {
                equal("paid reward cannot mint a native payout " + id, RewardClaimType.CLAIMED, data.getClaimType(player, reward));
                equal("paid receipt is exactly-once in native bookkeeping " + id, false,
                        data.markRewardAsClaimed(player, reward, NATIVE_DATE.getTime()));
            }
        }
    }

    private static CompoundTag questDocument(Map<String, CompoundTag> documents, QuestCatalogue.Task declared) {
        CompoundTag chapter = documents.get(QuestCatalogue.chapter(declared) == QuestCatalogue.Chapter.T0
                ? "grindless_t0.snbt" : "grindless_t1.snbt");
        ListTag quests = chapter.getList("quests", Tag.TAG_COMPOUND);
        for (int index = 0; index < quests.size(); index++) {
            CompoundTag quest = quests.getCompound(index);
            if (quest.getString("id").equals(QUEST_CODES.get(declared.id()))) {
                return quest;
            }
        }
        throw new AssertionError("Missing generated quest " + declared.id());
    }

    private static String taskType(QuestCatalogue.Task task) {
        return switch (task.evidence()) {
            case ITEM, MATRIX -> "ftbquests:item";
            case FREE -> "ftbquests:checkmark";
            case DIMENSION -> "ftbquests:dimension";
        };
    }

    private static Quest quest(BaseQuestFile file, String id) {
        return Objects.requireNonNull(file.getQuest(FtbQuestDefinitions.questId(id)), "Missing synthetic native quest " + id);
    }

    private static Task task(BaseQuestFile file, String id) {
        return Objects.requireNonNull(file.getTask(FtbQuestDefinitions.taskId(id)), "Missing synthetic native task " + id);
    }

    private static Reward reward(BaseQuestFile file, String id, int index) {
        return Objects.requireNonNull(file.getReward(FtbQuestDefinitions.rewardIds(QuestCatalogue.byId(id)).get(index)),
                "Missing synthetic native reward " + id + "/" + index);
    }

    private static NativeQuestFile load(Path folder) {
        NativeQuestFile file = new NativeQuestFile(folder);
        file.readDataFull(folder);
        return file;
    }

    private static Map<Long, CompoundTag> snapshot(BaseQuestFile file) {
        Map<Long, CompoundTag> data = new LinkedHashMap<>();
        for (QuestObjectBase object : file.getAllObjects()) {
            CompoundTag tag = new CompoundTag();
            object.writeData(tag);
            data.put(object.id, tag);
        }
        return data;
    }

    private static long parseId(CompoundTag tag) {
        return Long.parseLong(tag.getString("id"), 16);
    }

    private static void unique(Set<Long> ids, long id, String label) {
        equal("positive, non-reserved ID " + label, true, id > 1);
        equal("globally unique native ID " + label, true, ids.add(id));
    }

    private static void fails(String label, Class<? extends RuntimeException> expected, Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (expected.isInstance(exception)) {
                return;
            }
            throw new AssertionError(label + ": expected " + expected.getSimpleName(), exception);
        }
        throw new AssertionError(label + ": expected " + expected.getSimpleName());
    }

    private static void equal(String label, Object expected, Object actual) {
        assertions++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    /** Only required abstract hooks; the native framework loads the synthetic stock data. */
    private static final class NativeQuestFile extends BaseQuestFile {
        private final Path folder;

        private NativeQuestFile(Path folder) {
            this.folder = folder;
        }

        @Override
        public Env getSide() {
            // Data only: no server packets, online membership or server lifecycle proof.
            return Env.CLIENT;
        }

        @Override
        public Path getFolder() {
            return folder;
        }

        @Override
        public void deleteObject(long id) {
            throw new UnsupportedOperationException("The synthetic loader fixture does not delete quest objects");
        }

        @Override
        public boolean isPlayerOnTeam(Player player, TeamData data) {
            throw new UnsupportedOperationException("The data-only loader fixture does not admit players");
        }
    }
}
