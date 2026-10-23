package io.github.ezequiel24123z.grindless.forge.quest;

import com.mojang.serialization.JsonOps;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.api.quests.QuestDisplayStatus;
import earth.terrarium.heracles.api.tasks.QuestTasks;
import earth.terrarium.heracles.common.handlers.progress.QuestProgress;
import earth.terrarium.heracles.common.handlers.progress.QuestProgressHandler;
import earth.terrarium.heracles.common.handlers.progress.QuestsProgress;
import earth.terrarium.heracles.common.handlers.quests.QuestHandler;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestLogic;
import io.github.ezequiel24123z.grindless.quest.QuestProjection;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Pure publisher-binary contracts, not Forge lifecycle, graphics or retained-world acceptance. */
public final class VerifyHeracles {

    private static final String STARTER_ID = "grindless_starter_supplies";
    private static final List<String> ORIGINAL_TASK_IDS = List.of(
            "multitool", "crank", "extractor", "quern", "water", "clay_mixer", "ceramics", "workshop_flow",
            "settling", "calibrate", "relay", "thermal", "pylon", "scanner", "terrestrial", "pulverizer",
            "furnace", "press", "assembler", "matrix_line", "glass", "logistics", "kiln", "factory_world",
            "industrial_matrix");
    private static final List<String> LESSON_IDS = List.of(
            "quest_book_help", "superflat", "construction", "manual_work", "grog", "firing",
            "geological_collector", "aggregate", "water_recovery", "mineral_feed", "biocultivator",
            "biomass", "fibreboard", "biomass_dynamo", "fluid_logistics", "conveyors", "assembly_bench",
            "research_terminal", "data_core", "calibrated_core");
    private static final List<String> T1_LESSON_IDS = List.of(
            "first_grid", "forming_dies", "copper_coil", "flux_cell", "wet_beneficiation", "gas_utilities");
    private static int assertions;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        QuestTasks.register(GrindlessClaimTask.TYPE);
        Map<String, Quest> definitions = HeraclesDefinitions.create(task ->
                task.evidence() == QuestCatalogue.Evidence.FREE ? HeraclesDefinitions.icon(task) : Items.BOOK);
        definitions(definitions);
        projection(definitions);
        chapterVisibility(definitions);
        files(definitions);
        packets();
        System.out.println("ALL HERACLES CONTRACT CHECKS PASSED: " + assertions);
    }

    private static void definitions(Map<String, Quest> definitions) {
        equal("one native quest per catalogue task", QuestCatalogue.tasks().size(), definitions.size());
        equal("the complete book has 32 unchanged T0 nodes and 20 in-development T1 nodes", 52, definitions.size());
        Set<String> expectedIds = new LinkedHashSet<>();
        ORIGINAL_TASK_IDS.forEach(id -> expectedIds.add("grindless_" + id));
        expectedIds.add(STARTER_ID);
        LESSON_IDS.forEach(id -> expectedIds.add("grindless_" + id));
        T1_LESSON_IDS.forEach(id -> expectedIds.add("grindless_" + id));
        equal("all 46 old generated ids remain with exactly six added T1 lessons", expectedIds, definitions.keySet());
        equal("the starter is the first generated entry", STARTER_ID, definitions.keySet().iterator().next());
        var starter = QuestCatalogue.tasks().get(0);
        equal("the first catalogue task is the starter", QuestCatalogue.STARTER_SUPPLIES, starter.id());
        equal("the starter stays in the existing bootstrap line", "bootstrap", starter.line());
        equal("the free objective has no item lookup", QuestCatalogue.Evidence.FREE, starter.evidence());
        equal("the free objective subject is empty", "", starter.subject());
        equal("the starter supplies all eleven legal display stacks", 11, starter.rewards().size());
        equal("the default FREE icon is an existing book", Items.BOOK, HeraclesDefinitions.icon(starter));
        equal("the first root needs no native unlock", Set.of(), definitions.get(STARTER_ID).dependencies());
        equal("the first root is visible at the earliest native status", QuestDisplayStatus.LOCKED,
                definitions.get(STARTER_ID).settings().hiddenUntil());
        Map<String, String> encoded = HeraclesDefinitions.encode(definitions);
        equal("native export is deterministic", encoded, HeraclesDefinitions.encode(definitions));
        for (QuestProjection.Entry entry : QuestProjection.entries()) {
            Quest quest = definitions.get(entry.id());
            equal("individual progress " + entry.id(), true, quest.settings().individualProgress());
            equal("no native rewards " + entry.id(), Map.of(), quest.rewards());
            equal("no native autoclaim " + entry.id(), false, quest.settings().autoClaimRewards());
            equal("not repeatable " + entry.id(), false, quest.settings().repeatable());
            equal("exact prerequisite ids " + entry.id(), Set.copyOf(entry.dependencies()), quest.dependencies());
            equal("one reusable claim task " + entry.id(), Map.of(QuestProjection.CLAIM_TASK,
                    new GrindlessClaimTask(QuestProjection.CLAIM_TASK, entry.task().id())), quest.tasks());
            Quest decoded = Quest.CODEC.parse(JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString(encoded.get(entry.id()))).getOrThrow(false,
                    message -> { throw new AssertionError(message); });
            equal("actual binary codec round-trip " + entry.id(), true, HeraclesDefinitions.matches(quest, decoded));
            equal("the codec preserves the chapter's real hidden policy " + entry.id(),
                    QuestCatalogue.chapter(entry.task()) == QuestCatalogue.Chapter.T0
                            ? QuestDisplayStatus.LOCKED : QuestDisplayStatus.IN_PROGRESS,
                    decoded.settings().hiddenUntil());
            Set<String> groups = entry.task().id().equals(QuestCatalogue.T0_COMPLETION)
                    ? Set.of(HeraclesDefinitions.GROUP, HeraclesDefinitions.T1_GROUP)
                    : Set.of(QuestCatalogue.chapter(entry.task()) == QuestCatalogue.Chapter.T0
                            ? HeraclesDefinitions.GROUP : HeraclesDefinitions.T1_GROUP);
            equal("the codec retains actual separate chapter membership " + entry.id(), groups,
                    decoded.display().groups().keySet());
            if (QuestCatalogue.chapter(entry.task()) == QuestCatalogue.Chapter.T1) {
                equal("every T1 node has the actual individual T0 gate " + entry.id(), true,
                        decoded.dependencies().contains("grindless_relay"));
                equal("T1 is explicitly in development in its real native subtitle " + entry.id(),
                        net.minecraft.network.chat.Component.translatable("gui.grindless.quest.t1_line",
                                net.minecraft.network.chat.Component.translatable(
                                        "gui.grindless.quest.line." + entry.task().line())),
                        decoded.display().subtitle());
            }
            equal("native descriptions remain empty; the shared widget owns rich teaching " + entry.id(), List.of(),
                    decoded.display().description());
        }
        equal("the actual F2 definition has no mandatory optional-portal edge",
                Set.of("grindless_matrix_line", "grindless_glass", "grindless_relay"),
                definitions.get("grindless_industrial_matrix").dependencies());
        var bad = com.google.gson.JsonParser.parseString("{\"type\":\"grindless:claim\",\"quest\":\"invented\"}");
        equal("task codec refuses unknown catalogue ids", true,
                GrindlessClaimTask.TYPE.codec("claim").parse(JsonOps.INSTANCE, bad).error().isPresent());
    }

    /** Exercise the selected binary's visibility function, including its absent-parent default. */
    private static void chapterVisibility(Map<String, Quest> definitions) throws Exception {
        earth.terrarium.heracles.client.handlers.ClientQuests.sync(definitions,
                List.of(HeraclesDefinitions.GROUP, HeraclesDefinitions.T1_GROUP));
        var widgets = Class.forName("earth.terrarium.heracles.client.screens.quests.QuestsWidget");
        var hide = widgets.getDeclaredMethod("shouldHide", String.class,
                it.unimi.dsi.fastutil.objects.Object2BooleanMap.class,
                earth.terrarium.heracles.client.handlers.ClientQuests.QuestEntry.class);
        hide.setAccessible(true);
        var statuses = new it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap<String>();
        statuses.defaultReturnValue(true);
        var projected = QuestProjection.claimed(Set.of("thermal", "industrial_matrix"));
        definitions.forEach((id, quest) -> {
            if (quest.display().groups().containsKey(HeraclesDefinitions.T1_GROUP)) {
                statuses.put(id, projected.get(id).booleanValue());
            }
        });
        equal("the final T0 ID is a real same-ledger gate anchor in the T1 group", true,
                statuses.containsKey("grindless_relay"));
        for (var entry : QuestProjection.entries()) {
            if (QuestCatalogue.chapter(entry.task()) != QuestCatalogue.Chapter.T1) {
                continue;
            }
            var nativeEntry = earth.terrarium.heracles.client.handlers.ClientQuests.get(entry.id()).orElseThrow();
            equal("the actual binary hides premature T1 content " + entry.id(), true,
                    hide.invoke(null, HeraclesDefinitions.T1_GROUP, statuses, nativeEntry));
        }
        var thermal = earth.terrarium.heracles.client.handlers.ClientQuests.get("grindless_thermal").orElseThrow();
        statuses.removeBoolean("grindless_relay");
        equal("counterexample: an absent cross-group parent defaults to visible in the actual binary", false,
                hide.invoke(null, HeraclesDefinitions.T1_GROUP, statuses, thermal));
        statuses.put("grindless_relay", false);
        equal("restoring the explicit incomplete gate hides T1", true,
                hide.invoke(null, HeraclesDefinitions.T1_GROUP, statuses, thermal));
        statuses.put("grindless_relay", true);
        equal("the actual binary exposes the first T1 objective after the final T0 claim", false,
                hide.invoke(null, HeraclesDefinitions.T1_GROUP, statuses, thermal));

        var old = new io.github.ezequiel24123z.grindless.quest.QuestProgress();
        var player = java.util.UUID.randomUUID();
        old.claim(player, "thermal");
        old.claim(player, "industrial_matrix");
        var before = old.save(new CompoundTag());
        var nativeProgress = new QuestsProgress(new HashMap<>());
        HeraclesClaimProjection.reconcile(nativeProgress, old.claimed(player), definitions);
        equal("a retained T1 claim is not a premature native completion", false, nativeProgress.isComplete("grindless_thermal"));
        equal("hiding a retained claim does not rewrite or reset its ledger", before, old.save(new CompoundTag()));
        old.claim(player, "relay");
        HeraclesClaimProjection.reconcile(nativeProgress, old.claimed(player), definitions);
        equal("the same retained claim reappears after the individual gate", true, nativeProgress.isComplete("grindless_thermal"));
        equal("restoration does not invent a native reward", true,
                nativeProgress.getProgress("grindless_thermal").claimedRewards().isEmpty());
    }

    private static void projection(Map<String, Quest> definitions) throws Exception {
        QuestHandler.quests().putAll(definitions);
        QuestProgress unrelated = new QuestProgress();
        unrelated.setComplete(true);
        QuestsProgress first = new QuestsProgress(new HashMap<>(Map.of("unrelated", unrelated)));
        QuestsProgress second = new QuestsProgress(new HashMap<>());
        var legacy = new io.github.ezequiel24123z.grindless.quest.QuestProgress();
        var player = java.util.UUID.randomUUID();
        legacy.claim(player, "multitool");
        legacy.claim(player, "quern");
        CompoundTag before = legacy.save(new CompoundTag());
        Set<String> claims = legacy.claimed(player);
        equal("first projection creates only owned records", QuestCatalogue.tasks().size(),
                HeraclesClaimProjection.reconcile(first, claims, definitions).size());
        equal("original claim is native complete", true, first.isComplete("grindless_multitool"));
        equal("possession is not native completion", false, first.isComplete("grindless_crank"));
        equal("a ready FREE objective is not an automatic claim", false, first.isComplete(STARTER_ID));
        equal("unrelated native progress is preserved by identity", true,
                first.progress().get("unrelated") == unrelated);
        equal("repeated open/projection is idempotent", List.of(),
                HeraclesClaimProjection.reconcile(first, claims, definitions));
        HeraclesClaimProjection.reconcile(second, Set.of(), definitions);
        equal("second player does not inherit claims", false, second.isComplete("grindless_multitool"));
        equal("the second player's starter remains unclaimed", false, second.isComplete(STARTER_ID));

        var copy = QuestProgressHandler.class.getDeclaredMethod("copyProgress", QuestsProgress.class, QuestsProgress.class);
        copy.setAccessible(true);
        copy.invoke(null, first, second);
        equal("actual binary party-copy path respects individualProgress", false, second.isComplete("grindless_multitool"));
        first.progress().get("grindless_crank").setComplete(true);
        first.progress().get(STARTER_ID).setComplete(true);
        HeraclesClaimProjection.reconcile(first, claims, definitions);
        equal("sticky native completion cannot supersede the legacy ledger", false, first.isComplete("grindless_crank"));
        equal("sticky native FREE completion cannot create a starter claim", false, first.isComplete(STARTER_ID));
        first.progress().get(STARTER_ID).reset();
        HeraclesClaimProjection.reconcile(first, claims, definitions);
        equal("native FREE reset and reopen leave the starter pending", false, first.isComplete(STARTER_ID));
        equal("native FREE reset and reopen record no rewards", true,
                first.getProgress(STARTER_ID).claimedRewards().isEmpty());
        first.progress().get("grindless_multitool").reset();
        HeraclesClaimProjection.reconcile(first, claims, definitions);
        equal("native reset reconstructs the original claim without paying again", true, first.isComplete("grindless_multitool"));
        equal("legacy save is never rewritten by projection", before, legacy.save(new CompoundTag()));

        // Model an already recorded authoritative claim, not a native completion or reward grant.
        equal("the authoritative fixture can record the starter once", true,
                legacy.claim(player, QuestCatalogue.STARTER_SUPPLIES));
        CompoundTag afterClaim = legacy.save(new CompoundTag());
        Set<String> starterClaims = legacy.claimed(player);
        HeraclesClaimProjection.reconcile(first, starterClaims, definitions);
        equal("only the authoritative starter claim becomes native complete", true, first.isComplete(STARTER_ID));
        first.progress().get(STARTER_ID).reset();
        HeraclesClaimProjection.reconcile(first, starterClaims, definitions);
        equal("native FREE reset restores an existing claim", true, first.isComplete(STARTER_ID));
        equal("reopening a restored FREE claim is idempotent", List.of(),
                HeraclesClaimProjection.reconcile(first, starterClaims, definitions));
        equal("restoring the FREE claim does not record native rewards", true,
                first.getProgress(STARTER_ID).claimedRewards().isEmpty());
        equal("restoring the FREE claim never rewrites the authoritative ledger", afterClaim,
                legacy.save(new CompoundTag()));
        copy.invoke(null, first, second);
        equal("actual party copying cannot share a claimed starter kit", false, second.isComplete(STARTER_ID));
        for (var entry : definitions.entrySet()) {
            QuestProgress restored = new QuestProgress(entry.getValue(), first.getProgress(entry.getKey()).save());
            equal("native progress reload " + entry.getKey(), first.isComplete(entry.getKey()), restored.isComplete());
            equal("native progress reload never reconstructs reward grants " + entry.getKey(), true,
                    restored.claimedRewards().isEmpty());
        }
    }

    private static void files(Map<String, Quest> definitions) throws Exception {
        Path root = Files.createTempDirectory(Path.of("build"), "heracles-contract-");
        try {
            Map<String, String> encoded = HeraclesDefinitions.encode(definitions);
            Path config = root.resolve("config");
            HeraclesDefinitions.synchronize(config, encoded);
            HeraclesDefinitions.verifyFiles(config, encoded);
            Path bundle = HeraclesDefinitions.bundle(config);
            byte[] original = Files.readAllBytes(bundle);
            try (var zip = new ZipFile(bundle.toFile())) {
                equal("one archive owns every definition and its manifest", encoded.size() + 1, zip.size());
                equal("the manifest is not a native quest JSON", true, zip.getEntry("grindless-manifest") != null);
            }
            HeraclesDefinitions.synchronize(config, encoded);
            equal("repeated generation preserves bytes", true, java.util.Arrays.equals(original, Files.readAllBytes(bundle)));
            loadNative(config);
            equal("real publisher loader accepts the ZIP without parse failures", false, QuestHandler.failedToLoad);
            for (var entry : definitions.entrySet()) {
                equal("actual binary loads the ZIP member filename id " + entry.getKey(), true,
                        HeraclesDefinitions.matches(entry.getValue(), QuestHandler.get(entry.getKey())));
            }
            equal("the real loader leaves the FREE root visible", QuestDisplayStatus.LOCKED,
                    QuestHandler.get(STARTER_ID).settings().hiddenUntil());
            equal("the real loader leaves the FREE root unlocked", Set.of(),
                    QuestHandler.get(STARTER_ID).dependencies());
            catalogueUpgrade(root.resolve("catalogue-upgrade"), definitions, encoded);
            previousCatalogueUpgrade(root.resolve("previous-46-upgrade"), definitions, encoded);
            try (var zip = FileSystems.newFileSystem(bundle, Map.of())) {
                Files.writeString(zip.getPath("grindless_multitool.json"), encoded.get("grindless_multitool") + " ");
            }
            byte[] modified = Files.readAllBytes(bundle);
            refuses("modified owned definitions are never overwritten", () -> HeraclesDefinitions.synchronize(config, encoded));
            equal("the user's changed archive bytes are preserved", true,
                    java.util.Arrays.equals(modified, Files.readAllBytes(bundle)));
            Files.write(bundle, original);
            try (var zip = FileSystems.newFileSystem(bundle, Map.of())) {
                Files.delete(zip.getPath("grindless_multitool.json"));
            }
            refuses("missing owned definitions require an explicit recovery",
                    () -> HeraclesDefinitions.synchronize(config, encoded));
            Files.write(bundle, original);
            interruptedPublication(config, encoded, definitions);

            Path coexist = root.resolve("coexist");
            Path retained = HeraclesDefinitions.bundle(coexist).resolveSibling("external_retained.json");
            Files.createDirectories(retained.getParent());
            Quest external = HeraclesDefinitions.create(task -> Items.PAPER).get("grindless_multitool");
            String externalJson = HeraclesDefinitions.encode(Map.of("external_retained", external)).get("external_retained");
            Files.writeString(retained, externalJson);
            HeraclesDefinitions.synchronize(coexist, encoded);
            HeraclesDefinitions.verifyFiles(coexist, encoded);
            equal("unrelated native quest bytes are not replaced", externalJson, Files.readString(retained));
            loadNative(coexist);
            equal("real loader retains an unrelated native quest alongside the bundle", true,
                    HeraclesDefinitions.matches(external, QuestHandler.get("external_retained")));
            equal("native coexistence does not drop owned definitions", definitions.size() + 1, QuestHandler.quests().size());

            Path malformed = root.resolve("malformed");
            Path malformedBundle = HeraclesDefinitions.bundle(malformed);
            Files.createDirectories(malformedBundle.getParent());
            Files.write(malformedBundle, original);
            try (var zip = FileSystems.newFileSystem(malformedBundle, Map.of())) {
                Files.writeString(zip.getPath("grindless-manifest"), "[]");
            }
            refuses("a malformed ownership manifest is refused explicitly",
                    () -> HeraclesDefinitions.synchronize(malformed, encoded));

            Path unowned = root.resolve("unowned");
            Path unownedBundle = HeraclesDefinitions.bundle(unowned);
            Files.createDirectories(unownedBundle.getParent());
            try (var zip = new ZipOutputStream(Files.newOutputStream(unownedBundle))) {
                zip.putNextEntry(new ZipEntry("notes.txt"));
                zip.write(new byte[]{1, 2, 3});
                zip.closeEntry();
            }
            byte[] unownedBytes = Files.readAllBytes(unownedBundle);
            refuses("an existing ZIP is never adopted by its filename",
                    () -> HeraclesDefinitions.synchronize(unowned, encoded));
            equal("unowned same-name ZIP bytes are preserved", true,
                    java.util.Arrays.equals(unownedBytes, Files.readAllBytes(unownedBundle)));

            Path colliding = root.resolve("collision");
            Path user = colliding.resolve("heracles").resolve("quests").resolve("custom")
                    .resolve("grindless_multitool.custom.json");
            Files.createDirectories(user.getParent());
            Files.writeString(user, "{}");
            refuses("native basename aliases collide before any generation",
                    () -> HeraclesDefinitions.synchronize(colliding, encoded));
            equal("unowned file is preserved", "{}", Files.readString(user));
            equal("collision does not leave a success-shaped owned book", false,
                    Files.exists(HeraclesDefinitions.bundle(colliding)));

            Path legacy = root.resolve("legacy");
            Path loose = legacy.resolve("heracles").resolve("quests").resolve("grindless")
                    .resolve("grindless_multitool.json");
            Files.createDirectories(loose.getParent());
            Files.writeString(loose, encoded.get("grindless_multitool"));
            refuses("an interrupted legacy loose export is not adopted without ownership",
                    () -> HeraclesDefinitions.synchronize(legacy, encoded));
            equal("legacy loose bytes remain intact", encoded.get("grindless_multitool"), Files.readString(loose));

            Path archived = root.resolve("archive");
            Path archive = archived.resolve("heracles").resolve("quests").resolve("user.zip");
            Files.createDirectories(archive.getParent());
            try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                zip.putNextEntry(new ZipEntry("custom/grindless_multitool.custom.json"));
                zip.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            byte[] originalArchive = Files.readAllBytes(archive);
            refuses("native archive aliases cannot shadow owned quest ids",
                    () -> HeraclesDefinitions.synchronize(archived, encoded));
            equal("archive bytes are preserved", true, java.util.Arrays.equals(originalArchive, Files.readAllBytes(archive)));
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private static void loadNative(Path config) {
        QuestHandler.quests().clear();
        QuestHandler.load(RegistryAccess.EMPTY, config);
        equal("native load must succeed without a previously seeded quest map", false, QuestHandler.failedToLoad);
    }

    private static void catalogueUpgrade(Path config, Map<String, Quest> definitions,
                                         Map<String, String> encoded) throws IOException {
        Map<String, String> oldCatalogue = new LinkedHashMap<>();
        for (String task : ORIGINAL_TASK_IDS) {
            String id = "grindless_" + task;
            equal("the original stable id still has a definition " + id, true, encoded.containsKey(id));
            oldCatalogue.put(id, encoded.get(id));
        }
        equal("the prior owned catalogue has exactly 25 stable ids", 25, oldCatalogue.size());
        HeraclesDefinitions.synchronize(config, oldCatalogue);
        HeraclesDefinitions.verifyFiles(config, oldCatalogue);
        loadNative(config);
        equal("the real loader accepts the prior 25-task owned bundle", 25, QuestHandler.quests().size());
        equal("the prior catalogue contains no starter definition", false, QuestHandler.quests().containsKey(STARTER_ID));

        HeraclesDefinitions.synchronize(config, encoded);
        HeraclesDefinitions.verifyFiles(config, encoded);
        loadNative(config);
        equal("the real loader accepts the expanded 52-task owned bundle", 52, QuestHandler.quests().size());
        for (String id : oldCatalogue.keySet()) {
            equal("bundle upgrade retains the original native id and claim contract " + id, true,
                    HeraclesDefinitions.matches(definitions.get(id), QuestHandler.get(id)));
        }
        equal("bundle upgrade adds the FREE claim adapter", true,
                HeraclesDefinitions.matches(definitions.get(STARTER_ID), QuestHandler.get(STARTER_ID)));
        equal("the upgraded starter is visible without unlocking another quest", QuestDisplayStatus.LOCKED,
                QuestHandler.get(STARTER_ID).settings().hiddenUntil());
        equal("the upgraded starter has no native dependencies", Set.of(), QuestHandler.get(STARTER_ID).dependencies());
        byte[] upgraded = Files.readAllBytes(HeraclesDefinitions.bundle(config));
        HeraclesDefinitions.synchronize(config, encoded);
        equal("reopening the expanded 52-task bundle is byte-idempotent", true,
                java.util.Arrays.equals(upgraded, Files.readAllBytes(HeraclesDefinitions.bundle(config))));
    }

    private static void previousCatalogueUpgrade(Path config, Map<String, Quest> definitions,
                                                 Map<String, String> encoded) throws Exception {
        Map<String, Quest> previous = new LinkedHashMap<>();
        for (QuestProjection.Entry entry : QuestProjection.entries()) {
            if (T1_LESSON_IDS.contains(entry.task().id())) {
                continue;
            }
            Quest old = Quest.CODEC.parse(JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseString(encoded.get(entry.id()))).getOrThrow(false,
                    message -> { throw new AssertionError(message); });
            old.display().setSubtitle(net.minecraft.network.chat.Component.translatable(
                    "gui.grindless.quest.line." + entry.task().line()));
            old.display().setDescription(List.of());
            Set<String> dependencies = new LinkedHashSet<>(old.dependencies());
            if (entry.task().id().equals("industrial_matrix")) {
                dependencies.add("grindless_factory_world");
            }
            previous.put(entry.id(), new Quest(old.display(), old.settings(), dependencies, old.tasks(), old.rewards()));
        }
        equal("the prior-source fixture contains all 46 old native IDs", 46, previous.size());
        HeraclesDefinitions.synchronize(config, HeraclesDefinitions.encode(previous));
        loadNative(config);
        equal("the actual loader accepts the prior-source 46-ID bundle", previous.keySet(),
                QuestHandler.quests().keySet());
        equal("the prior-source fixture retains the old optional-portal edge", true,
                QuestHandler.get("grindless_industrial_matrix").dependencies().contains("grindless_factory_world"));

        var legacy = new io.github.ezequiel24123z.grindless.quest.QuestProgress();
        var player = java.util.UUID.randomUUID();
        Map<String, QuestProgress> oldNative = new LinkedHashMap<>();
        for (QuestProjection.Entry entry : QuestProjection.entries()) {
            Quest old = previous.get(entry.id());
            if (old == null) {
                continue;
            }
            legacy.claim(player, entry.task().id());
            QuestProgress progress = new QuestProgress();
            var task = (GrindlessClaimTask) old.tasks().get(QuestProjection.CLAIM_TASK);
            var taskProgress = progress.getTask(task);
            taskProgress.setProgress(net.minecraft.nbt.ByteTag.valueOf(true));
            taskProgress.setComplete(true);
            progress.setComplete(true);
            oldNative.put(entry.id(), new QuestProgress(old, progress.save()));
        }
        Set<String> originalClaims = legacy.claimed(player);
        CompoundTag saved = legacy.save(new CompoundTag());
        legacy = io.github.ezequiel24123z.grindless.quest.QuestProgress.load(saved.copy());
        equal("the original save schema reloads all 46 individual claims", originalClaims, legacy.claimed(player));
        CompoundTag beforeProjection = legacy.save(new CompoundTag());
        QuestsProgress nativeProgress = new QuestsProgress(new HashMap<>(oldNative));
        HeraclesDefinitions.synchronize(config, encoded);
        HeraclesDefinitions.verifyFiles(config, encoded);
        loadNative(config);
        equal("the actual loader accepts the complete 46-to-52 definition upgrade", definitions.keySet(),
                QuestHandler.quests().keySet());
        equal("only new lessons need native progress construction", T1_LESSON_IDS.stream()
                        .map(QuestProjection::id).collect(java.util.stream.Collectors.toSet()),
                Set.copyOf(HeraclesClaimProjection.reconcile(nativeProgress, legacy.claimed(player), definitions)));
        for (var entry : oldNative.entrySet()) {
            equal("all 46 original native progress objects are retained " + entry.getKey(), true,
                    nativeProgress.progress().get(entry.getKey()) == entry.getValue());
            equal("all original authoritative claims remain complete " + entry.getKey(), true,
                    nativeProgress.isComplete(entry.getKey()));
            equal("restoring old progress cannot reconstruct native reward grants " + entry.getKey(), true,
                    nativeProgress.getProgress(entry.getKey()).claimedRewards().isEmpty());
        }
        for (String id : T1_LESSON_IDS) {
            equal("upgrade never auto-completes new teaching " + id, false,
                    nativeProgress.isComplete(QuestProjection.id(id)));
        }
        equal("changing optional edges and subtitles never rewrites the old individual ledger", beforeProjection,
                legacy.save(new CompoundTag()));
        equal("the upgraded native projection is idempotent", List.of(),
                HeraclesClaimProjection.reconcile(nativeProgress, legacy.claimed(player), definitions));
        byte[] upgraded = Files.readAllBytes(HeraclesDefinitions.bundle(config));
        HeraclesDefinitions.synchronize(config, encoded);
        equal("the 46-to-52 native bundle upgrade is byte-idempotent", true,
                java.util.Arrays.equals(upgraded, Files.readAllBytes(HeraclesDefinitions.bundle(config))));
    }

    private static void interruptedPublication(Path config, Map<String, String> encoded,
                                               Map<String, Quest> definitions) throws Exception {
        Path bundle = HeraclesDefinitions.bundle(config);
        byte[] previous = Files.readAllBytes(bundle);
        Map<String, String> updated = new LinkedHashMap<>(encoded);
        updated.put("grindless_multitool", encoded.get("grindless_multitool") + "\n");
        refuses("actual publication failure before replacement is explicit",
                () -> HeraclesDefinitions.synchronize(config, updated, VerifyHeracles::failingOutput));
        equal("actual publication failure preserves the previous archive", true,
                java.util.Arrays.equals(previous, Files.readAllBytes(bundle)));
        Path staging = bundle.resolveSibling(".grindless-interrupted.tmp");
        partialBundle(staging, updated);
        byte[] partial = Files.readAllBytes(staging);
        equal("failure before replacement preserves the complete previous bundle", true,
                java.util.Arrays.equals(previous, Files.readAllBytes(bundle)));
        HeraclesDefinitions.verifyFiles(config, encoded);
        loadNative(config);
        equal("the real loader still sees only the complete old catalogue", definitions.size(), QuestHandler.quests().size());
        HeraclesDefinitions.synchronize(config, updated);
        HeraclesDefinitions.verifyFiles(config, updated);
        equal("retry does not adopt or delete an interrupted temporary file", true,
                java.util.Arrays.equals(partial, Files.readAllBytes(staging)));
        try (var zip = new ZipFile(bundle.toFile())) {
            String contents = new String(zip.getInputStream(zip.getEntry("grindless_multitool.json")).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            equal("retry publishes the complete new definition bytes", updated.get("grindless_multitool"), contents);
        }

        loadNative(config);
        equal("the real ZIP loader accepts the complete new catalogue", false, QuestHandler.failedToLoad);
        for (var entry : definitions.entrySet()) {
            equal("retry retains native id/dependencies/settings " + entry.getKey(), true,
                    HeraclesDefinitions.matches(entry.getValue(), QuestHandler.get(entry.getKey())));
        }
        byte[] committed = Files.readAllBytes(bundle);
        HeraclesDefinitions.synchronize(config, updated);
        equal("retry after replacement is idempotent", true, java.util.Arrays.equals(committed, Files.readAllBytes(bundle)));

        Path initial = config.resolveSibling("first-publication");
        Path firstBundle = HeraclesDefinitions.bundle(initial);
        Files.createDirectories(firstBundle.getParent());
        refuses("actual first-publication failure is explicit",
                () -> HeraclesDefinitions.synchronize(initial, encoded, VerifyHeracles::failingOutput));
        partialBundle(firstBundle.resolveSibling(".grindless-interrupted.tmp"), encoded);
        equal("a failed first publication does not expose a partial archive", false, Files.exists(firstBundle));
        HeraclesDefinitions.synchronize(initial, encoded);
        HeraclesDefinitions.verifyFiles(initial, encoded);
        loadNative(initial);
        equal("first-publication retry loads every native definition", definitions.size(), QuestHandler.quests().size());
    }

    private static void partialBundle(Path staging, Map<String, String> definitions) throws IOException {
        refuses("a partially written staging archive fails explicitly",
                () -> HeraclesDefinitions.writeBundle(failingOutput(staging), definitions));
    }

    private static OutputStream failingOutput(Path path) throws IOException {
        return new FilterOutputStream(Files.newOutputStream(path)) {
            private int remaining = 128;

            @Override
            public void write(int value) throws IOException {
                if (remaining <= 0) {
                    throw new IOException("Simulated interruption before atomic publication");
                }
                remaining--;
                out.write(value);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) throws IOException {
                for (int index = 0; index < length; index++) {
                    write(bytes[offset + index]);
                }
            }
        };
    }

    private static void packets() {
        var states = QuestCatalogue.tasks().stream()
                .map(task -> new HeraclesQuestNetwork.State(task.id(), task.count(), QuestLogic.Status.CLAIMABLE)).toList();
        var snapshot = new HeraclesQuestNetwork.Snapshot(true, false, states);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            HeraclesQuestNetwork.encode(snapshot, buffer);
            equal("server evidence packet round-trip includes physical book state", snapshot, HeraclesQuestNetwork.decode(buffer));
            buffer.clear();
            buffer.writeBoolean(true);
            buffer.writeBoolean(true);
            buffer.writeVarInt(0);
            try {
                HeraclesQuestNetwork.decode(buffer);
                throw new AssertionError("Incomplete available snapshots must be rejected");
            } catch (IllegalArgumentException expected) {
                assertions++;
            }
            buffer.clear();
            var unavailable = new HeraclesQuestNetwork.Snapshot(false, false, List.of());
            HeraclesQuestNetwork.encode(unavailable, buffer);
            equal("an unavailable backend is not an available empty book", unavailable, HeraclesQuestNetwork.decode(buffer));
            for (var state : List.of(
                    new HeraclesQuestNetwork.State("invented", 0, QuestLogic.Status.CLAIMABLE),
                    new HeraclesQuestNetwork.State(states.get(0).taskId(), -1, QuestLogic.Status.CLAIMABLE))) {
                buffer.clear();
                buffer.writeBoolean(false);
                buffer.writeBoolean(false);
                buffer.writeVarInt(1);
                buffer.writeUtf(state.taskId());
                buffer.writeVarInt(state.held());
                buffer.writeEnum(state.status());
                try {
                    HeraclesQuestNetwork.decode(buffer);
                    throw new AssertionError("Unknown ids or negative counts must be rejected");
                } catch (IllegalArgumentException expected) {
                    assertions++;
                }
            }
            buffer.clear();
            HeraclesQuestNetwork.encode(new HeraclesQuestNetwork.Snapshot(false, false,
                    List.of(states.get(0), states.get(0))), buffer);
            try {
                HeraclesQuestNetwork.decode(buffer);
                throw new AssertionError("Duplicate task rows must be rejected");
            } catch (IllegalArgumentException expected) {
                assertions++;
            }
        } finally {
            buffer.release();
        }
    }

    private interface IoAction {
        void run() throws IOException;
    }

    private static void refuses(String description, IoAction action) throws IOException {
        try {
            action.run();
            throw new AssertionError(description);
        } catch (IOException expected) {
            assertions++;
        }
    }

    private static void equal(String description, Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
