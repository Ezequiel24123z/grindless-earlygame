package io.github.ezequiel24123z.grindless.forge.quest;

import com.mojang.serialization.JsonOps;
import earth.terrarium.heracles.api.quests.Quest;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Real publisher-binary contracts, not a graphics/client or authentic retained-world acceptance. */
public final class VerifyHeracles {

    private static int assertions;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        QuestTasks.register(GrindlessClaimTask.TYPE);
        Map<String, Quest> definitions = HeraclesDefinitions.create(task -> Items.BOOK);
        definitions(definitions);
        projection(definitions);
        files(definitions);
        packets();
        System.out.println("ALL HERACLES CONTRACT CHECKS PASSED: " + assertions);
    }

    private static void definitions(Map<String, Quest> definitions) {
        equal("one native quest per original catalogue task", QuestCatalogue.tasks().size(), definitions.size());
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
        }
        var bad = com.google.gson.JsonParser.parseString("{\"type\":\"grindless:claim\",\"quest\":\"invented\"}");
        equal("task codec refuses unknown catalogue ids", true,
                GrindlessClaimTask.TYPE.codec("claim").parse(JsonOps.INSTANCE, bad).error().isPresent());
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
        equal("unrelated native progress is preserved by identity", true,
                first.progress().get("unrelated") == unrelated);
        equal("repeated open/projection is idempotent", List.of(),
                HeraclesClaimProjection.reconcile(first, claims, definitions));
        HeraclesClaimProjection.reconcile(second, Set.of(), definitions);
        equal("second player does not inherit claims", false, second.isComplete("grindless_multitool"));

        var copy = QuestProgressHandler.class.getDeclaredMethod("copyProgress", QuestsProgress.class, QuestsProgress.class);
        copy.setAccessible(true);
        copy.invoke(null, first, second);
        equal("actual binary party-copy path respects individualProgress", false, second.isComplete("grindless_multitool"));
        first.progress().get("grindless_crank").setComplete(true);
        HeraclesClaimProjection.reconcile(first, claims, definitions);
        equal("sticky native completion cannot supersede the legacy ledger", false, first.isComplete("grindless_crank"));
        first.progress().get("grindless_multitool").reset();
        HeraclesClaimProjection.reconcile(first, claims, definitions);
        equal("native reset reconstructs the original claim without paying again", true, first.isComplete("grindless_multitool"));
        equal("legacy save is never rewritten by projection", before, legacy.save(new CompoundTag()));
        for (var entry : definitions.entrySet()) {
            QuestProgress restored = new QuestProgress(entry.getValue(), first.getProgress(entry.getKey()).save());
            equal("native progress reload " + entry.getKey(), first.isComplete(entry.getKey()), restored.isComplete());
        }
    }

    private static void files(Map<String, Quest> definitions) throws Exception {
        Path root = Files.createTempDirectory(Path.of("build"), "heracles-contract-");
        try {
            Map<String, String> encoded = HeraclesDefinitions.encode(definitions);
            Path config = root.resolve("config");
            HeraclesDefinitions.synchronize(config, encoded);
            HeraclesDefinitions.verifyFiles(config, encoded);
            Path owned = config.resolve("heracles").resolve("quests").resolve("grindless");
            Path first = owned.resolve("grindless_multitool.json");
            String original = Files.readString(first);
            HeraclesDefinitions.synchronize(config, encoded);
            equal("repeated generation preserves bytes", original, Files.readString(first));
            QuestHandler.load(RegistryAccess.EMPTY, config);
            for (var entry : definitions.entrySet()) {
                equal("actual binary loads the owned filename id " + entry.getKey(), true,
                        HeraclesDefinitions.matches(entry.getValue(), QuestHandler.get(entry.getKey())));
            }
            Files.writeString(first, original + " ");
            refuses("modified owned definitions are never overwritten", () -> HeraclesDefinitions.synchronize(config, encoded));
            equal("the user's changed bytes are preserved", original + " ", Files.readString(first));
            Files.writeString(first, original);
            Files.delete(first);
            refuses("missing owned definitions require an explicit recovery",
                    () -> HeraclesDefinitions.synchronize(config, encoded));

            Path malformed = root.resolve("malformed");
            Path manifest = malformed.resolve("grindless").resolve("heracles-manifest.json");
            Files.createDirectories(manifest.getParent());
            Files.writeString(manifest, "[]");
            refuses("a malformed ownership manifest is refused explicitly",
                    () -> HeraclesDefinitions.synchronize(malformed, encoded));

            Path colliding = root.resolve("collision");
            Path user = colliding.resolve("heracles").resolve("quests").resolve("custom")
                    .resolve("grindless_multitool.custom.json");
            Files.createDirectories(user.getParent());
            Files.writeString(user, "{}");
            refuses("native basename aliases collide before any generation",
                    () -> HeraclesDefinitions.synchronize(colliding, encoded));
            equal("unowned file is preserved", "{}", Files.readString(user));
            equal("collision does not leave a success-shaped owned book", false,
                    Files.exists(colliding.resolve("heracles").resolve("quests").resolve("grindless")));

            Path archived = root.resolve("archive");
            Path archive = archived.resolve("heracles").resolve("quests").resolve("user.zip");
            Files.createDirectories(archive.getParent());
            try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                zip.putNextEntry(new ZipEntry("custom/grindless_multitool.json"));
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
