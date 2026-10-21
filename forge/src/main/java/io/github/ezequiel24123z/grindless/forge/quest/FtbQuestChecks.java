package io.github.ezequiel24123z.grindless.forge.quest;

import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardAutoClaim;
import dev.ftb.mods.ftbquests.quest.reward.RewardClaimType;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.DimensionTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixNbt;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixSpec;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Opt-in, console-only native-quests smoke for a disposable Forge diagnostic world.
 * The default command uses unattached in-memory progress. Explicit seed/reopen commands
 * additionally exercise native disk persistence for one reserved diagnostic UUID and one
 * separate SavedData key. No real player, inventory, block or FTB Teams registration is used.
 */
public final class FtbQuestChecks {

    private static final Date HISTORY = new Date(1_700_000_000_000L);
    private static final UUID DISK_TEAM = UUID.fromString("00000000-0000-0000-0000-465442510001");
    private static final UUID DISK_FIRST = UUID.fromString("00000000-0000-0000-0000-465442510002");
    private static final UUID DISK_SECOND = UUID.fromString("00000000-0000-0000-0000-465442510003");
    private static final String DISK_NAME = "Grindless native quest disk probe";
    private static final String DISK_LEGACY_KEY = "grindless_native_quest_probe";
    private static final Set<String> DISK_PAID = Set.of("starter_supplies", "multitool");
    private static boolean diskSeededHere;
    private final ServerQuestFile file;
    private final List<UUID> transientTeams = new ArrayList<>();
    private int assertions;

    private FtbQuestChecks(ServerQuestFile file) {
        this.file = file;
    }

    /** Wired only by the real FTB integration, never an unconditional optional-mod subscriber. */
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("grindless_native_quest_checks")
                .requires(source -> source.getEntity() == null && source.hasPermission(4))
                .executes(context -> run(context.getSource()))
                .then(Commands.literal("seed").executes(context -> disk(context.getSource(), false)))
                .then(Commands.literal("reopen").executes(context -> disk(context.getSource(), true))));
    }

    private static ServerQuestFile liveFile(CommandSourceStack source) {
        require(source.getEntity() == null && source.hasPermission(4), "Console permission 4 required");
        require(source.getServer().isSameThread(), "Native checks must execute on the server thread");
        require(source.getServer().getPlayerList().getPlayers().isEmpty(), "Use a disposable diagnostic server with no connected players");
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        require(file != null && file.server == source.getServer(), "Actual native ServerQuestFile is not ready");
        return file;
    }

    private static int run(CommandSourceStack source) {
        ServerQuestFile file = liveFile(source);
        FtbQuestChecks checks = new FtbQuestChecks(file);
        try {
            int savedTeams = file.getAllTeamData().size();
            checks.graph();
            checks.matrices();
            checks.migration();
            checks.relayGate();
            checks.equal("saved team count is unchanged", savedTeams, file.getAllTeamData().size());
            for (UUID id : checks.transientTeams) {
                checks.check("diagnostic TeamData never enters the saved-team map", file.getNullableTeamData(id) == null);
            }
            Grindless.LOG.info("GRINDLESS_FTB_NATIVE_PASS quests=46 rewards=36 legacy=46 assertions={}", checks.assertions);
            return 1;
        } catch (RuntimeException failure) {
            Grindless.LOG.error("GRINDLESS_FTB_NATIVE_FAIL assertions={}", checks.assertions, failure);
            throw failure;
        }
    }

    private static int disk(CommandSourceStack source, boolean reopen) {
        FtbQuestChecks checks = new FtbQuestChecks(liveFile(source));
        try {
            checks.assertions++;
            FtbQuestNative.verify(checks.file);
            if (reopen) {
                checks.reopenDisk();
                Grindless.LOG.info("GRINDLESS_FTB_NATIVE_REOPEN_PASS legacy=2 receipts=14 advance=3 assertions={}", checks.assertions);
            } else {
                checks.seedDisk();
                Grindless.LOG.info("GRINDLESS_FTB_NATIVE_SEED_PASS legacy=2 receipts=14 advance=3 awaiting_stop=1 assertions={}", checks.assertions);
            }
            return 1;
        } catch (RuntimeException failure) {
            Grindless.LOG.error("GRINDLESS_FTB_NATIVE_DISK_FAIL phase={} assertions={}",
                    reopen ? "reopen" : "seed", checks.assertions, failure);
            throw failure;
        }
    }

    private void seedDisk() {
        check("seed runs only once per process", !diskSeededHere);
        check("seed refuses any already-loaded diagnostic progress", file.getNullableTeamData(DISK_TEAM) == null);
        check("seed refuses an existing native disk file", !Files.exists(nativeDiskFile(), LinkOption.NOFOLLOW_LINKS));
        Path legacyFile = file.server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(DISK_LEGACY_KEY + ".dat");
        check("seed refuses an existing diagnostic legacy file", !Files.exists(legacyFile, LinkOption.NOFOLLOW_LINKS));
        check("diagnostic UUID is not a real FTB team", FTBTeamsAPI.api().getManager().getTeamByID(DISK_TEAM).isEmpty());
        var storage = file.server.overworld().getDataStorage();
        check("seed refuses cached diagnostic legacy data", storage.get(QuestProgress::load, DISK_LEGACY_KEY) == null);
        diskSeededHere = true; // Never reset: reopening the same JVM is not second-process proof.

        QuestProgress legacy = new QuestProgress();
        for (String id : DISK_PAID) {
            check("seed original paid history " + id, legacy.claim(DISK_FIRST, id));
        }
        TeamData data = new TeamData(DISK_TEAM, file);
        data.setName(DISK_NAME);
        check("diagnostic progress has no online members", data.getOnlineMembers().isEmpty());
        // Public native API, also used by ServerQuestFile.load(); no synthetic FTB Teams object.
        file.addData(data, false);
        check("native save lifecycle owns only the diagnostic fixture", file.getNullableTeamData(DISK_TEAM) == data);
        FtbQuestNative.migrate(legacy, DISK_FIRST, data);
        FtbQuestNative.migrate(legacy, DISK_SECOND, data);

        // Actual server-side native completion: one unpaid reward and one individually paid reward.
        data.setProgress(task("crank"), task("crank").getMaxProgress());
        data.setProgress(task("quern"), task("quern").getMaxProgress());
        check("record an individual native reward receipt", data.markRewardAsClaimed(DISK_FIRST, reward("quern", 0), HISTORY.getTime()));
        check("record only one second-player starter receipt", data.markRewardAsClaimed(DISK_SECOND, reward("starter_supplies", 0), HISTORY.getTime()));
        data.saveIfChanged();
        check("native saveIfChanged wrote the real diagnostic file", Files.isRegularFile(nativeDiskFile(), LinkOption.NOFOLLOW_LINKS));

        // Deliberately advance AFTER the explicit native save. Native saveNow() on natural
        // server stop must save this dirty, attached TeamData, not merely the initial receipt file.
        data.setProgress(task("conveyors"), 3L);
        FtbQuestNative.migrate(legacy, DISK_FIRST, data);
        FtbQuestNative.migrate(legacy, DISK_SECOND, data);
        storage.set(DISK_LEGACY_KEY, legacy); // Original QuestProgress/SavedData serializer, normal world save.
        diskState(data);
        equal("seed retains only the original first-player paid history", DISK_PAID, legacy.legacyClaims(DISK_FIRST));
        equal("seed invents no second-player paid history", Set.of(), legacy.legacyClaims(DISK_SECOND));
        // Do not save again here, stop the server, delete files or clean up failed evidence.
    }

    private void reopenDisk() {
        check("reopen requires another JVM process", !diskSeededHere);
        check("reopen requires the original native disk file", Files.isRegularFile(nativeDiskFile(), LinkOption.NOFOLLOW_LINKS));
        // ServerQuestFile.load() scans world/ftbquests/*.snbt, creates TeamData, calls
        // addData(data, true) and deserializeNBT(). Never create/default/reload it in this command.
        TeamData data = file.getNullableTeamData(DISK_TEAM);
        check("actual server startup loaded the diagnostic TeamData", data != null);
        check("disk progress belongs to the actual native server file", data.getFile() == file);
        equal("native disk fixture identity", DISK_NAME, data.getName());
        check("disk fixture is not a real FTB team", FTBTeamsAPI.api().getManager().getTeamByID(DISK_TEAM).isEmpty());
        QuestProgress legacy = file.server.overworld().getDataStorage().get(QuestProgress::load, DISK_LEGACY_KEY);
        check("natural world save retained the original legacy ledger", legacy != null);
        equal("original paid receipts survive the process boundary", DISK_PAID, legacy.legacyClaims(DISK_FIRST));
        equal("second player still has no historical paid receipts", Set.of(), legacy.legacyClaims(DISK_SECOND));
        check("both persisted legacy authority fences survive", legacy.usesNativeClaims(DISK_FIRST) && legacy.usesNativeClaims(DISK_SECOND));
        equal("native completion was not serialized as first-player paid history", DISK_PAID, legacy.claimed(DISK_FIRST));
        equal("native completion was not serialized as second-player paid history", Set.of(), legacy.claimed(DISK_SECOND));
        diskState(data); // Assert disk-loaded state BEFORE any import/refresh can repair it.
        SNBTCompoundTag beforeImport = data.serializeNBT();
        FtbQuestNative.migrate(legacy, DISK_FIRST, data);
        FtbQuestNative.migrate(legacy, DISK_SECOND, data);
        equal("reopen import cannot reset native progress, dates, pending rewards or individual receipts",
                beforeImport, data.serializeNBT());
        equal("reopen still has exactly the original paid history", DISK_PAID, legacy.legacyClaims(DISK_FIRST));
        equal("reopen does not turn shared completion into second-player paid history", Set.of(), legacy.legacyClaims(DISK_SECOND));
        equal("reopen rebuilds common completion from native progress",
                Set.of("starter_supplies", "multitool", "crank", "quern"), legacy.claimed(DISK_FIRST));
        check("persisted fence still refuses the retired reward mint", !legacy.claim(DISK_FIRST, "thermal"));
    }

    private void diskState(TeamData data) {
        for (String id : Set.of("starter_supplies", "multitool", "crank", "quern")) {
            check("disk fixture native quest remains complete " + id, data.isCompleted(quest(id)));
            check("disk fixture native task remains complete " + id, data.isCompleted(task(id)));
        }
        for (int index = 0; index < 11; index++) {
            Reward reward = reward("starter_supplies", index);
            check("old first-player starter receipt survives " + index, data.isRewardClaimed(DISK_FIRST, reward));
            equal("second-player individual starter receipt " + index, index == 0, data.isRewardClaimed(DISK_SECOND, reward));
        }
        equal("old multitool payment stays claimed", RewardClaimType.CLAIMED, data.getClaimType(DISK_FIRST, reward("multitool", 0)));
        equal("same-team player does not inherit old multitool payment", RewardClaimType.CAN_CLAIM, data.getClaimType(DISK_SECOND, reward("multitool", 0)));
        equal("native completion with no receipt stays pending", RewardClaimType.CAN_CLAIM, data.getClaimType(DISK_FIRST, reward("crank", 0)));
        equal("native paid reward stays claimed", RewardClaimType.CLAIMED, data.getClaimType(DISK_FIRST, reward("quern", 0)));
        equal("another player does not inherit the native paid reward", RewardClaimType.CAN_CLAIM, data.getClaimType(DISK_SECOND, reward("quern", 0)));
        equal("native individual reward date survives", HISTORY, data.getRewardClaimTime(DISK_FIRST, reward("quern", 0)).orElseThrow());
        equal("second-player individual reward date survives", HISTORY, data.getRewardClaimTime(DISK_SECOND, reward("starter_supplies", 0)).orElseThrow());
        equal("post-save native forward progress survives", 3L, data.getProgress(task("conveyors")));
        check("partial native forward progress is not completed", !data.isCompleted(task("conveyors")) && !data.isCompleted(quest("conveyors")));
        equal("exact persisted individual receipt count", 14, data.serializeNBT().getCompound("claimed_rewards").size());
    }

    private Path nativeDiskFile() {
        return file.server.getWorldPath(ServerQuestFile.FTBQUESTS_DATA).resolve(DISK_TEAM + ".snbt");
    }

    private void graph() {
        assertions++;
        FtbQuestNative.verify(file);
        equal("canonical 46 quest objectives", 46, QuestCatalogue.tasks().size());
        equal("actual T0 chapter size", 32, file.getChapter(FtbQuestDefinitions.id("chapter", "T0")).getQuests().size());
        equal("actual gated T1 chapter size", 14, file.getChapter(FtbQuestDefinitions.id("chapter", "T1")).getQuests().size());
        Set<Long> ids = new HashSet<>();
        ids.add(FtbQuestDefinitions.id("chapter", "T0"));
        ids.add(FtbQuestDefinitions.id("chapter", "T1"));
        int lessons = 0;
        int material = 0;
        int rewards = 0;
        for (QuestCatalogue.Task declared : QuestCatalogue.tasks()) {
            Quest quest = quest(declared.id());
            Task task = task(declared.id());
            check("unique native quest ID " + declared.id(), ids.add(quest.id));
            check("unique native task ID " + declared.id(), ids.add(task.id));
            equal("actual stock task class " + declared.id(), switch (declared.evidence()) {
                case ITEM, MATRIX -> ItemTask.class;
                case FREE -> CheckmarkTask.class;
                case DIMENSION -> DimensionTask.class;
            }, task.getClass());
            equal("canonical native target count " + declared.id(), (long) declared.count(), task.getMaxProgress());
            equal("native bound dependency set " + declared.id(),
                    Set.copyOf(QuestCatalogue.requirements(declared).stream().map(FtbQuestDefinitions::questId).toList()),
                    quest.streamDependencies().map(dependency -> dependency.id).collect(Collectors.toSet()));
            check("native rewards never repeat " + declared.id(), !quest.canBeRepeated());
            equal("canonical native reward cardinality " + declared.id(), declared.rewards().size(), quest.getRewards().size());
            if (declared.type() == QuestCatalogue.Type.LESSON) {
                lessons++;
            } else {
                material++;
            }
            List<Reward> nativeRewards = List.copyOf(quest.getRewards());
            for (int index = 0; index < nativeRewards.size(); index++) {
                Reward reward = nativeRewards.get(index);
                equal("actual native item reward " + declared.id(), ItemReward.class, reward.getClass());
                check("unique native reward ID " + declared.id(), ids.add(reward.id));
                check("actual reward lookup " + declared.id(), file.getReward(FtbQuestDefinitions.rewardIds(declared).get(index)) == reward);
                check("native reward is individual " + declared.id(), !reward.isTeamReward());
                equal("native autoclaim disabled " + declared.id(), RewardAutoClaim.DISABLED, reward.getAutoClaimType());
                ItemReward item = (ItemReward) reward;
                equal("real registered reward item " + declared.id(), declared.rewards().get(index).itemId(),
                        BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString());
                equal("actual canonical reward quantity " + declared.id(), declared.rewards().get(index).count(), item.getCount());
                rewards++;
            }
        }
        equal("twenty reward-free lessons", 20, lessons);
        equal("twenty-six material-reward quests", 26, material);
        equal("thirty-six native reward receipts", 36, rewards);
        equal("130 unique native chapter/quest/task/reward IDs", 130, ids.size());
        equal("eleven distinct starter reward stacks", 11, quest(QuestCatalogue.STARTER_SUPPLIES).getRewards().size());
    }

    private void matrices() {
        Item registered = ModItems.RELAY_MATRIX.get();
        check("actual registered Matrix item type", registered instanceof ControlMatrixItem);
        equal("actual registered Matrix item ID", QuestCatalogue.byId("relay").subject(),
                BuiltInRegistries.ITEM.getKey(registered).toString());
        ItemTask relay = (ItemTask) task("relay");
        ItemTask industrial = (ItemTask) task("industrial_matrix");
        equal("stock F1 task has the exact five-item target", 5L, relay.getMaxProgress());
        equal("stock F2 task has the exact one-item target", 1L, industrial.getMaxProgress());
        check("stock Matrix tasks are nonconsuming", !relay.consumesResources() && !industrial.consumesResources());
        check("stock Matrix tasks observe inventory", relay.submitItemsOnInventoryChange() && industrial.submitItemsOnInventoryChange());
        FtbMatrixFilter adapter = new FtbMatrixFilter();
        check("real native F1 filter stack has its adapter binding", adapter.isFilterStack(relay.getItemStack()));
        check("real native F2 filter stack has its adapter binding", adapter.isFilterStack(industrial.getItemStack()));
        ItemStack untagged = new ItemStack(registered, 5);
        ItemStack f1 = matrix(registered, FluxTier.F1, 5);
        ItemStack f2 = matrix(registered, FluxTier.F2, 1);
        ItemStack f3 = matrix(registered, FluxTier.F3, 5);
        matrixCase("valid legacy untagged F1", relay, industrial, adapter, untagged, true, false);
        matrixCase("valid tagged F1", relay, industrial, adapter, f1, true, false);
        matrixCase("valid tagged F2 is not F1", relay, industrial, adapter, f2, false, true);
        matrixCase("wrong valid physical rating F3", relay, industrial, adapter, f3, false, false);
        matrixCase("empty stack", relay, industrial, adapter, ItemStack.EMPTY, false, false);
        ItemStack wrongItem = new ItemStack(Items.REDSTONE, 5);
        wrongItem.setTag(f1.getTag().copy());
        matrixCase("wrong item cannot borrow Matrix NBT", relay, industrial, adapter, wrongItem, false, false);
        for (int index = 0; index < 4; index++) {
            ItemStack malformed = new ItemStack(registered, 5);
            CompoundTag tag = new CompoundTag();
            switch (index) {
                case 0 -> tag.putString(ControlMatrixNbt.RATING, "1");
                case 1 -> tag.putDouble(ControlMatrixNbt.RATING, 1.5D);
                case 2 -> tag.putLong(ControlMatrixNbt.RATING, 4_294_967_297L);
                default -> tag.putInt(ControlMatrixNbt.RATING, 4);
            }
            malformed.setTag(tag);
            matrixCase("malformed/unsupported explicit rating " + index, relay, industrial, adapter, malformed, false, false);
        }
        ItemStack badBinding = relay.getItemStack().copy();
        badBinding.getOrCreateTag().putString(FtbMatrixFilter.BINDING, "invented");
        check("marked malformed filter remains owned by the adapter", adapter.isFilterStack(badBinding));
        check("unknown filter binding cannot match", !adapter.doesItemMatch(badBinding, untagged));
        check("compiled unknown filter binding cannot match", !adapter.getMatcher(badBinding).test(untagged));
        badBinding.getOrCreateTag().putString(FtbMatrixFilter.BINDING, "industrial_matrix");
        check("F1 filter cannot borrow the F2 logical binding", !adapter.doesItemMatch(badBinding, f2));
        badBinding.getOrCreateTag().putInt(FtbMatrixFilter.BINDING, 1);
        check("non-string filter binding remains adapter-owned", adapter.isFilterStack(badBinding));
        check("non-string filter binding fails closed", !adapter.getMatcher(badBinding).test(f1));
        ItemStack wrongFilter = wrongItem.copy();
        wrongFilter.getOrCreateTag().putString(FtbMatrixFilter.BINDING, "relay");
        check("a marked wrong filter item cannot borrow the Relay binding", !adapter.doesItemMatch(wrongFilter, f1));

        // Native target-count/completion mechanics, not a substitute for real player inventory detection.
        TeamData count = transientData();
        count.setCompleted(quest("calibrate").id, HISTORY);
        count.setProgress(relay, 4L);
        equal("native progress retains four matching items", 4L, count.getProgress(relay));
        check("four of five does not complete the real stock task", !count.isCompleted(relay));
        count.setProgress(relay, 5L);
        check("five of five completes the real stock task", count.isCompleted(relay));
        check("five of five completes its real quest", count.isCompleted(quest("relay")));
        count.setProgress(relay, 6L);
        equal("native progress clamps to exactly five", 5L, count.getProgress(relay));
        equal("completion alone never auto-claims a material reward", 0, count.serializeNBT().getCompound("claimed_rewards").size());
    }

    private void matrixCase(String label, ItemTask relay, ItemTask industrial, FtbMatrixFilter adapter,
                            ItemStack candidate, boolean f1, boolean f2) {
        // These stock calls, with opposite expected results for the same item at F1/F2,
        // prove real mod setup registered the adapter. No API._init or local registration.
        equal("actual stock ItemTask.test F1: " + label, f1, relay.test(candidate));
        equal("actual stock ItemTask.test F2: " + label, f2, industrial.test(candidate));
        equal("direct adapter F1: " + label, f1, adapter.doesItemMatch(relay.getItemStack(), candidate));
        equal("compiled adapter F2: " + label, f2, adapter.getMatcher(industrial.getItemStack()).test(candidate));
    }

    private void migration() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        QuestProgress legacy = new QuestProgress();
        Set<String> all = QuestCatalogue.tasks().stream().map(QuestCatalogue.Task::id).collect(Collectors.toSet());
        for (String id : all) {
            check("seed historical receipt " + id, legacy.claim(first, id));
        }
        TeamData data = transientData();
        FtbQuestNative.migrate(legacy, first, data);
        equal("all 46 historical receipts are retained", all, legacy.legacyClaims(first));
        check("historical authority is fenced", legacy.usesNativeClaims(first));
        check("old mint cannot add another receipt", !legacy.claim(first, "starter_supplies"));
        for (QuestCatalogue.Task declared : QuestCatalogue.tasks()) {
            check("actual migrated quest is complete " + declared.id(), data.isCompleted(quest(declared.id())));
            check("actual migrated task is complete " + declared.id(), data.isCompleted(task(declared.id())));
            equal("actual migrated task reaches its native target " + declared.id(), task(declared.id()).getMaxProgress(),
                    data.getProgress(task(declared.id())));
            for (Reward reward : quest(declared.id()).getRewards()) {
                check("old paid native receipt " + declared.id(), data.isRewardClaimed(first, reward));
                equal("old payment cannot be paid natively again " + declared.id(), RewardClaimType.CLAIMED,
                        data.getClaimType(first, reward));
                check("a second UUID has no individual receipt " + declared.id(), !data.isRewardClaimed(second, reward));
            }
        }
        equal("exactly 36 individual receipts for the 46 paid objectives", 36, data.serializeNBT().getCompound("claimed_rewards").size());
        FtbQuestNative.migrate(legacy, second, data);
        equal("native team completion does not invent second-player historical receipts", Set.of(), legacy.legacyClaims(second));
        equal("common completion is rebuilt for the second UUID", all, legacy.claimed(second));
        for (QuestCatalogue.Task declared : QuestCatalogue.tasks()) {
            for (Reward reward : quest(declared.id()).getRewards()) {
                check("second UUID still has no paid native receipt " + declared.id(), !data.isRewardClaimed(second, reward));
                equal("individual reward remains available only to its unpaid UUID " + declared.id(), RewardClaimType.CAN_CLAIM,
                        data.getClaimType(second, reward));
            }
        }
        Reward firstStarter = quest("starter_supplies").getRewards().iterator().next();
        check("native bookkeeping records a second UUID independently", data.markRewardAsClaimed(second, firstStarter, HISTORY.getTime()));
        equal("second-player bookkeeping adds exactly one receipt", 37, data.serializeNBT().getCompound("claimed_rewards").size());
        SNBTCompoundTag saved = data.serializeNBT();
        FtbQuestNative.migrate(legacy, first, data);
        FtbQuestNative.migrate(legacy, second, data);
        equal("repeat import preserves native progress/receipt timestamps", saved, data.serializeNBT());
        // Use the native SNBT types/signatures directly; no world save, byte codec or surrogate ledger.
        TeamData reloaded = new TeamData(data.getTeamId(), file);
        reloaded.deserializeNBT(saved);
        equal("real native SNBT in-memory reload", saved, reloaded.serializeNBT());
        QuestProgress legacyReloaded = QuestProgress.load(legacy.save(new CompoundTag()));
        equal("derived second-player completion was not serialized as paid history", Set.of(), legacyReloaded.claimed(second));
        FtbQuestNative.migrate(legacyReloaded, first, reloaded);
        FtbQuestNative.migrate(legacyReloaded, second, reloaded);
        equal("native rejoin/import preserves the entire native state", saved, reloaded.serializeNBT());
        check("first UUID receipt survives reload", reloaded.isRewardClaimed(first, firstStarter));
        check("second UUID receipt survives reload independently", reloaded.isRewardClaimed(second, firstStarter));
        Reward nextStarter = List.copyOf(quest("starter_supplies").getRewards()).get(1);
        check("second UUID does not inherit the other starter receipts after reload", !reloaded.isRewardClaimed(second, nextStarter));
    }

    private void relayGate() {
        UUID player = UUID.randomUUID();
        QuestProgress legacy = new QuestProgress();
        Set<String> paid = Set.of("thermal", "industrial_matrix");
        for (String id : paid) {
            check("seed retained T1 payment " + id, legacy.claim(player, id));
        }
        TeamData data = transientData();
        FtbQuestNative.migrate(legacy, player, data);
        for (String id : paid) {
            check("paid T1 receipt survives before Relay " + id, data.isRewardClaimed(player, quest(id).getRewards().iterator().next()));
            check("paid T1 is not imported as premature completion " + id, !data.isCompleted(quest(id)));
            equal("paid T1 does not import task progress before Relay " + id, 0L, data.getProgress(task(id)));
        }
        check("old T1 receipts alone do not unlock compatibility", !QuestCatalogue.unlocked(QuestCatalogue.Chapter.T1, legacy.claimed(player)));
        List<QuestCatalogue.Task> t1 = QuestCatalogue.tasks().stream()
                .filter(task -> QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1).toList();
        equal("fourteen actual gated objectives", 14, t1.size());
        for (QuestCatalogue.Task declared : t1) {
            for (String dependency : QuestCatalogue.requirements(declared)) {
                if (!dependency.equals(QuestCatalogue.T0_COMPLETION)) {
                    data.setCompleted(quest(dependency).id, HISTORY);
                }
            }
        }
        data.setProgress(task("conveyors"), 3L);
        for (QuestCatalogue.Task declared : t1) {
            check("stock native start remains Relay-gated " + declared.id(), !data.canStartTasks(quest(declared.id())));
            check("stock native visibility remains Relay-gated " + declared.id(), !quest(declared.id()).isVisible(data));
        }
        data.setCompleted(quest("relay").id, HISTORY);
        FtbQuestNative.refresh(legacy, player, data);
        FtbQuestNative.migrate(legacy, player, data);
        for (QuestCatalogue.Task declared : t1) {
            check("actual Relay completion releases native start " + declared.id(), data.canStartTasks(quest(declared.id())));
        }
        equal("native forward partial progress survives re-import", 3L, data.getProgress(task("conveyors")));
        equal("native completions never become historical paid import receipts", paid, legacy.legacyClaims(player));
        check("derived native Relay is not an individual paid receipt",
                !data.isRewardClaimed(player, quest("relay").getRewards().iterator().next()));
        check("derived native Relay is not re-imported as old T1 completion", !data.isCompleted(quest("industrial_matrix")));
        check("retained paid T1 reward is still fenced after native Relay", data.isRewardClaimed(player, quest("industrial_matrix").getRewards().iterator().next()));
        SNBTCompoundTag saved = data.serializeNBT();
        TeamData rejoined = new TeamData(data.getTeamId(), file);
        rejoined.deserializeNBT(saved);
        QuestProgress rejoinedLegacy = QuestProgress.load(legacy.save(new CompoundTag()));
        FtbQuestNative.migrate(rejoinedLegacy, player, rejoined);
        equal("native reload/rejoin preserves forward partial progress and all receipt dates", saved, rejoined.serializeNBT());
        equal("partial progress survives native reload/rejoin", 3L, rejoined.getProgress(task("conveyors")));
        equal("reload/rejoin still cannot promote observed completion into old paid history", paid, rejoinedLegacy.legacyClaims(player));
    }

    private TeamData transientData() {
        UUID id = UUID.randomUUID();
        check("diagnostic UUID does not address an existing saved team", file.getNullableTeamData(id) == null);
        check("diagnostic UUID does not address an existing FTB team", FTBTeamsAPI.api().getManager().getTeamByID(id).isEmpty());
        TeamData data = new TeamData(id, file);
        check("transient native team has no online members", data.getOnlineMembers().isEmpty());
        check("transient native team is not registered for saving", file.getNullableTeamData(id) == null);
        transientTeams.add(id);
        return data;
    }

    private static ItemStack matrix(Item item, FluxTier rating, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.setTag(ControlMatrixNbt.save(new ControlMatrixSpec(MatrixArchitecture.RELAY, rating)));
        return stack;
    }

    private Quest quest(String id) {
        return Objects.requireNonNull(file.getQuest(FtbQuestDefinitions.questId(id)), "Missing native quest " + id);
    }

    private Task task(String id) {
        return Objects.requireNonNull(file.getTask(FtbQuestDefinitions.taskId(id)), "Missing native task " + id);
    }

    private Reward reward(String id, int index) {
        return Objects.requireNonNull(file.getReward(FtbQuestDefinitions.rewardIds(QuestCatalogue.byId(id)).get(index)),
                "Missing native reward " + id + "/" + index);
    }

    private void equal(String label, Object expected, Object actual) {
        check(label + ": expected " + expected + ", got " + actual, Objects.equals(expected, actual));
    }

    private void check(String label, boolean condition) {
        assertions++;
        require(condition, label);
    }

    private static void require(boolean condition, String label) {
        if (!condition) {
            throw new IllegalStateException("Native FTB check failed: " + label);
        }
    }
}
