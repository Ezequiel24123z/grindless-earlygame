package io.github.ezequiel24123z.grindless.quest;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

/** The shared claim service and legacy-to-native authority boundary, without a second reward mint. */
public final class VerifyQuestClaims {

    private static int assertions;

    public static void main(String[] args) throws IOException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        claims();
        starterClaims();
        fullInventory();
        standardInventoryClaim();
        stacking();
        bookAccess();
        lessons();
        t1Claims();
        invalidRewards();
        projectionAndReload();
        nativeClaimAuthority();
        guardedLedgerCreation();
        System.out.println("ALL QUEST CLAIM CHECKS PASSED: " + assertions
                + "; positive registered F1 evidence/server claims remain required in workshop-ports native probe"
                + "; actual SavedData load-failure/recreation remains required in a native probe.");
    }

    private static void nativeClaimAuthority() {
        QuestProgress ledger = new QuestProgress();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ledger.claim(first, QuestCatalogue.STARTER_SUPPLIES);
        ledger.claim(first, "multitool");
        Set<String> receipts = ledger.claimed(first);
        ledger.useNativeClaims(first);
        ledger.useNativeClaims(first);
        ledger.updateNativeCompleted(first, Set.of("quern"));
        equal("compatibility gates can observe native completion", true, ledger.claimed(first).contains("quern"));
        equal("native completion is not an old paid reward receipt", receipts, ledger.legacyClaims(first));
        ledger.updateNativeCompleted(first, Set.of());
        equal("native observations are replaceable, not sticky invented receipts", receipts, ledger.claimed(first));
        equal("native authority preserves all legacy import receipts", receipts, ledger.claimed(first));
        equal("native authority is player scoped", false, ledger.usesNativeClaims(second));
        equal("native authority also fences direct legacy ledger mutation", false, ledger.claim(first, "quern"));
        AtomicInteger effects = new AtomicInteger();
        for (int restart = 0; restart < 3; restart++) {
            ledger = QuestProgress.load(ledger.save(new CompoundTag()));
            equal("native authority survives restart", true, ledger.usesNativeClaims(first));
            equal("legacy import receipts survive restart", receipts, ledger.claimed(first));
            for (String id : List.of(QuestCatalogue.STARTER_SUPPLIES, "multitool", "quern", "wet_beneficiation")) {
                equal("a retired legacy engine cannot grant " + id, QuestClaimService.Result.NATIVE_BACKEND,
                        QuestClaimService.claim(ledger, first, id, true,
                                task -> { effects.incrementAndGet(); return true; },
                                reward -> { effects.incrementAndGet(); return resolve(reward); },
                                rewards -> { effects.incrementAndGet(); return Optional.of(effects::incrementAndGet); }));
            }
        }
        equal("retired claims never inspect, stage or deliver rewards", 0, effects.get());
        equal("an unmigrated second player keeps the legacy fallback", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, second, QuestCatalogue.STARTER_SUPPLIES, true,
                        task -> false, VerifyQuestClaims::resolve, capture(new ArrayList<>())));
        equal("pre-native saved data does not invent an authority", false,
                QuestProgress.load(new CompoundTag()).usesNativeClaims(first));
        QuestProgress empty = new QuestProgress();
        empty.useNativeClaims(first);
        QuestProgress restored = QuestProgress.load(empty.save(new CompoundTag()));
        equal("a native-first player retains authority without old receipts", true, restored.usesNativeClaims(first));
        equal("native-first authority does not fabricate old claims", Set.of(), restored.claimed(first));
        ListTag numericPlayers = new ListTag();
        numericPlayers.add(IntTag.valueOf(1));
        ListTag invalidPlayers = new ListTag();
        invalidPlayers.add(StringTag.valueOf("not-a-player"));
        for (net.minecraft.nbt.Tag malformed : List.of(StringTag.valueOf(first.toString()),
                numericPlayers, invalidPlayers)) {
            CompoundTag corrupted = new CompoundTag();
            corrupted.put("NativeClaims", malformed);
            boolean rejected = false;
            try {
                QuestProgress.load(corrupted);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            equal("corrupt native authority must reject its parsed saved data", true, rejected);
        }
    }

    private static void guardedLedgerCreation() throws IOException {
        Path root = Files.createTempDirectory("grindless-quest-ledger-");
        try {
            Path data = Files.createDirectory(root.resolve("data"));
            Path file = data.resolve(QuestProgress.KEY + ".dat");
            QuestProgress fresh = QuestProgress.createIfMissing(file);
            equal("only an absent ledger permits fresh in-memory progress", Set.of(),
                    fresh.claimed(UUID.randomUUID()));
            equal("guarded creation does not write a replacement file", false, Files.exists(file));
            byte[] original = {0x1f, (byte) 0x8b, 0};
            Files.write(file, original);
            equal("an existing corrupt ledger cannot be recreated after a swallowed load failure", true,
                    refusesLedgerCreation(file));
            equal("refusal preserves the exact existing ledger bytes", true,
                    java.util.Arrays.equals(original, Files.readAllBytes(file)));
            Files.write(file, new byte[0]);
            equal("an empty corrupt ledger is existing data, not a new world", true, refusesLedgerCreation(file));
            equal("an empty corrupt ledger is not overwritten", 0L, Files.size(file));
            Files.delete(file);
            Files.createDirectory(file);
            equal("a directory at the ledger path cannot be replaced by empty progress", true,
                    refusesLedgerCreation(file));
            equal("the existing directory is preserved", true, Files.isDirectory(file));
            Files.delete(file);
            try {
                Files.createSymbolicLink(file, Path.of("missing-ledger-target"));
            } catch (IOException | UnsupportedOperationException | SecurityException unavailable) {
                System.out.println("SKIPPED dangling-ledger symlink fixture: symbolic-link creation unavailable ("
                        + unavailable.getClass().getSimpleName() + ").");
            }
            if (Files.isSymbolicLink(file)) {
                equal("a dangling ledger symlink cannot masquerade as an absent ledger", true,
                        refusesLedgerCreation(file));
                equal("refusal preserves the ledger symlink", true, Files.isSymbolicLink(file));
                Files.delete(file);
            }
            Files.write(file, original);
            if (Files.getFileAttributeView(data, PosixFileAttributeView.class) != null) {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(data);
                try {
                    Files.setPosixFilePermissions(data, Set.of());
                    boolean inspectable = true;
                    try {
                        Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    } catch (IOException | SecurityException denied) {
                        inspectable = false;
                    }
                    if (inspectable) {
                        System.out.println("SKIPPED inaccessible-ledger fixture: this process can still inspect attributes.");
                    } else {
                        equal("an uninspectable ledger cannot fall back to empty progress", true,
                                refusesLedgerCreation(file));
                    }
                } finally {
                    Files.setPosixFilePermissions(data, permissions);
                }
                equal("permission-fixture refusal preserves the existing ledger bytes", true,
                        java.util.Arrays.equals(original, Files.readAllBytes(file)));
            } else {
                System.out.println("SKIPPED inaccessible-ledger fixture: POSIX permission control is unavailable.");
            }
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static boolean refusesLedgerCreation(Path file) {
        try {
            QuestProgress.createIfMissing(file);
            return false;
        } catch (IllegalStateException expected) {
            return true;
        }
    }

    private static void claims() {
        QuestProgress ledger = new QuestProgress();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        List<ItemStack> rewards = new ArrayList<>();
        AtomicBoolean currentEvidence = new AtomicBoolean(true);
        Predicate<QuestCatalogue.Task> evidence = task -> currentEvidence.get();
        Function<QuestCatalogue.Reward, ItemStack> resolve = reward -> new ItemStack(Items.CLAY_BALL, reward.count());

        equal("no crafted book means no claim", QuestClaimService.Result.BOOK_REQUIRED,
                QuestClaimService.claim(ledger, first, "multitool", false, evidence, resolve, capture(rewards)));
        equal("child requires the parent's claim", QuestClaimService.Result.LOCKED,
                QuestClaimService.claim(ledger, first, "quern", true, evidence, resolve, capture(rewards)));
        currentEvidence.set(false);
        equal("past possession cannot pay a stale claim", QuestClaimService.Result.UNMET,
                QuestClaimService.claim(ledger, first, "multitool", true, evidence, resolve, capture(rewards)));
        equal("rejections never write progress", Set.of(), ledger.claimed(first));
        equal("rejections never pay rewards", 0, rewards.size());
        currentEvidence.set(true);
        equal("first valid claim succeeds", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, first, "multitool", true, evidence, resolve, capture(rewards)));
        equal("one exact reward stack", 4, rewards.get(0).getCount());
        equal("claim does not consume its evidence", true, currentEvidence.get());
        currentEvidence.set(false);
        equal("duplicate claim cannot pay again", QuestClaimService.Result.ALREADY_CLAIMED,
                QuestClaimService.claim(ledger, first, "multitool", true, evidence, resolve, capture(rewards)));
        equal("one reward after a duplicate request", 1, rewards.size());
        equal("second player does not inherit the claim", Set.of(), ledger.claimed(second));
        equal("second player still needs current evidence", QuestClaimService.Result.UNMET,
                QuestClaimService.claim(ledger, second, "multitool", true, evidence, resolve, capture(rewards)));
        currentEvidence.set(true);
        equal("second player has an independent claim", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, second, "multitool", true, evidence, resolve, capture(rewards)));
        equal("two players receive only their own reward", 2, rewards.size());
        equal("unknown request is rejected", QuestClaimService.Result.UNKNOWN_TASK,
                QuestClaimService.claim(ledger, first, "invented", true, evidence, resolve, capture(rewards)));
    }

    private static void starterClaims() {
        QuestProgress ledger = new QuestProgress();
        UUID player = UUID.randomUUID();
        List<ItemStack> rewards = new ArrayList<>();
        equal("even a free claim requires the actual book", QuestClaimService.Result.BOOK_REQUIRED,
                QuestClaimService.claim(ledger, player, QuestCatalogue.STARTER_SUPPLIES, false,
                        task -> false, VerifyQuestClaims::resolve, capture(rewards)));
        equal("a no-book request cannot record starter progress", Set.of(), ledger.claimed(player));
        equal("a no-book request cannot create rewards", 0, rewards.size());
        equal("free means no item or hidden evidence requirement", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, player, QuestCatalogue.STARTER_SUPPLIES, true,
                        task -> false, VerifyQuestClaims::resolve, capture(rewards)));
        equal("one free claim delivers all eleven stacks", 11, rewards.size());
        equal("the ledger records only the explicit starter claim", Set.of(QuestCatalogue.STARTER_SUPPLIES),
                ledger.claimed(player));
        equal("the starter does not claim or unlock the multitool for the player", false,
                QuestProjection.claimed(ledger.claimed(player)).get("grindless_multitool"));
        equal("one full source-bound allocation is delivered", Map.of(
                "minecraft:cobblestone", 128, "minecraft:oak_log", 64, "minecraft:bread", 64,
                "minecraft:brick", 64, "minecraft:clay_ball", 64, "minecraft:sand", 64,
                "minecraft:charcoal", 32, "minecraft:iron_ingot", 16, "minecraft:water_bucket", 2),
                quantities(rewards));
        for (ItemStack stack : rewards) {
            equal("every delivered stack fits its real vanilla maximum", true,
                    stack.getCount() > 0 && stack.getCount() <= stack.getMaxStackSize());
        }
        for (int reopen = 0; reopen < 3; reopen++) {
            ledger = QuestProgress.load(ledger.save(new CompoundTag()));
            equal("reopen/reconnect/reconstruction cannot reclaim starter supplies",
                    QuestClaimService.Result.ALREADY_CLAIMED,
                    QuestClaimService.claim(ledger, player, QuestCatalogue.STARTER_SUPPLIES, true,
                            task -> true, VerifyQuestClaims::resolve, capture(rewards)));
            equal("reconstruction never duplicates the kit", 11, rewards.size());
        }
        List<ItemStack> separate = new ArrayList<>();
        equal("another player in the same world has a separate kit", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, UUID.randomUUID(), QuestCatalogue.STARTER_SUPPLIES, true,
                        task -> false, VerifyQuestClaims::resolve, capture(separate)));
        equal("the same player in a different world has a separate kit", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(new QuestProgress(), player, QuestCatalogue.STARTER_SUPPLIES, true,
                        task -> false, VerifyQuestClaims::resolve, capture(new ArrayList<>())));
        equal("all old result codes remain in place", 6, QuestClaimService.Result.INVALID_REWARD.ordinal());
    }

    private static void fullInventory() {
        QuestProgress ledger = new QuestProgress();
        UUID player = UUID.randomUUID();
        Inventory inventory = new Inventory(null);
        inventory.setItem(0, new ItemStack(Items.BOOK));
        for (int slot = 1; slot < inventory.items.size(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.BEDROCK, 64));
        }
        ListTag full = inventory.save(new ListTag());
        equal("a full inventory defers the complete starter", QuestClaimService.Result.INVENTORY_FULL,
                starterClaim(ledger, player, inventory));
        equal("a full inventory receives no partial rewards", full, inventory.save(new ListTag()));
        equal("a failed delivery leaves the claim available", Set.of(), ledger.claimed(player));
        for (int slot = 26; slot < inventory.items.size(); slot++) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }
        ListTag tenSlots = inventory.save(new ListTag());
        equal("ten empty slots cannot hold eleven legal starter stacks", QuestClaimService.Result.INVENTORY_FULL,
                starterClaim(ledger, player, inventory));
        equal("capacity simulation never mutates even a nearly sufficient inventory", tenSlots,
                inventory.save(new ListTag()));
        inventory.setItem(25, ItemStack.EMPTY);
        equal("eleven empty slots accept the whole kit", QuestClaimService.Result.CLAIMED,
                starterClaim(ledger, player, inventory));
        equal("the nonconsumed utility book remains", true, inventory.getItem(0).is(Items.BOOK));
        equal("the exact cobblestone amount is retained in inventory", 128, count(inventory, Items.COBBLESTONE));
        equal("both unstackable buckets are retained in inventory", 2, count(inventory, Items.WATER_BUCKET));
        equal("ordinary carried items are preserved", 24 * 64, count(inventory, Items.BEDROCK));
        equal("no reward stack exceeds its vanilla limit", true, inventory.items.stream()
                .allMatch(stack -> stack.isEmpty() || stack.getCount() <= stack.getMaxStackSize()));
        ListTag complete = inventory.save(new ListTag());
        equal("a repeated full-inventory claim is still already claimed", QuestClaimService.Result.ALREADY_CLAIMED,
                starterClaim(ledger, player, inventory));
        equal("a repeated claim cannot alter delivered inventory", complete, inventory.save(new ListTag()));
    }

    private static void stacking() {
        Inventory inventory = new Inventory(null);
        inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        inventory.getItem(0).getOrCreateTag().putString("Existing", "preserve");
        List<ItemStack> rewards = List.of(new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.WATER_BUCKET),
                new ItemStack(Items.WATER_BUCKET));
        ListTag before = inventory.save(new ListTag());
        Runnable commit = QuestRewardDelivery.prepare(inventory, rewards).orElseThrow();
        equal("preparation makes no inventory writes", before, inventory.save(new ListTag()));
        equal("preparation leaves every caller-owned stack unchanged", List.of(64, 64, 1, 1),
                rewards.stream().map(ItemStack::getCount).toList());
        commit.run();
        equal("existing tagged stock is not merged with unrelated reward tags", true,
                inventory.getItem(0).getCount() == 32 && inventory.getItem(0).hasTag());
        equal("native stacking conserves old plus new cobblestone", 160, count(inventory, Items.COBBLESTONE));
        equal("native stacking preserves separate water buckets", 2, count(inventory, Items.WATER_BUCKET));
        equal("delivery does not consume the caller's reward declarations", List.of(64, 64, 1, 1),
                rewards.stream().map(ItemStack::getCount).toList());

        Inventory partial = new Inventory(null);
        partial.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        for (int slot = 2; slot < partial.items.size(); slot++) {
            partial.setItem(slot, new ItemStack(Items.BEDROCK, 64));
        }
        equal("one partial and one empty slot cannot hold two full cobblestone stacks", true,
                QuestRewardDelivery.prepare(partial, rewards.subList(0, 2)).isEmpty());
        equal("failed partial-stack planning retains the original quantity", 32, count(partial, Items.COBBLESTONE));
        QuestRewardDelivery.prepare(partial, List.of(new ItemStack(Items.COBBLESTONE, 64))).orElseThrow().run();
        equal("ordinary partial-stack merging uses the real vanilla capacity", 96, count(partial, Items.COBBLESTONE));

        Inventory retained = new Inventory(null);
        ItemStack existing = new ItemStack(Items.BEDROCK, 100);
        retained.setItem(0, existing);
        QuestRewardDelivery.prepare(retained, List.of(new ItemStack(Items.COBBLESTONE, 1))).orElseThrow().run();
        equal("staging never clamps an unrelated pre-existing saved stack", 100, count(retained, Items.BEDROCK));
        equal("unchanged carried stacks retain their native object identity", true, retained.getItem(0) == existing);
    }

    private static void standardInventoryClaim() {
        QuestProgress ledger = new QuestProgress();
        UUID player = UUID.randomUUID();
        Inventory inventory = new Inventory(null);
        inventory.setItem(0, new ItemStack(Items.BOOK));
        inventory.setItem(1, new ItemStack(Items.CLAY_BALL, 63));
        for (int slot = 2; slot < inventory.items.size(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.BEDROCK, 64));
        }
        ListTag before = inventory.save(new ListTag());
        equal("an ordinary reward also refuses lossy overflow", QuestClaimService.Result.INVENTORY_FULL,
                QuestClaimService.claim(ledger, player, "multitool", true, task -> true,
                        VerifyQuestClaims::resolve, rewards -> QuestRewardDelivery.prepare(inventory, rewards)));
        equal("an ordinary overflow cannot consume current evidence or a claim", Set.of(), ledger.claimed(player));
        equal("an ordinary overflow cannot partially merge a reward", before, inventory.save(new ListTag()));
        inventory.setItem(2, ItemStack.EMPTY);
        equal("an ordinary quest keeps the same ready/claim flow", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, player, "multitool", true, task -> true,
                        VerifyQuestClaims::resolve, rewards -> QuestRewardDelivery.prepare(inventory, rewards)));
        equal("the old four-clay reward remains exact", 67, count(inventory, Items.CLAY_BALL));
        equal("the old task id alone is recorded", Set.of("multitool"), ledger.claimed(player));
    }

    private static void bookAccess() {
        QuestProgress ledger = new QuestProgress();
        UUID player = UUID.randomUUID();
        ledger.claim(player, "multitool");
        Inventory inventory = new Inventory(null);
        equal("first login can obtain the existing book without paper or resources", QuestBookAccess.Result.DELIVERED,
                QuestBookAccess.ensure(ledger, player, inventory, Items.BOOK));
        equal("utility delivery creates exactly one book", 1, count(inventory, Items.BOOK));
        equal("utility delivery never creates starter or other new claims", Set.of("multitool"),
                ledger.claimed(player));
        for (int rejoin = 0; rejoin < 3; rejoin++) {
            equal("rejoin never duplicates a book already in real inventory", QuestBookAccess.Result.PRESENT,
                    QuestBookAccess.ensure(ledger, player, inventory, Items.BOOK));
        }
        equal("rejoin has still delivered only one book", 1, count(inventory, Items.BOOK));
        Inventory offhand = new Inventory(null);
        offhand.setItem(40, new ItemStack(Items.BOOK));
        equal("an existing offhand book prevents a duplicate", QuestBookAccess.Result.PRESENT,
                QuestBookAccess.ensure(ledger, player, offhand, Items.BOOK));
        equal("offhand evidence is not copied into a new main slot", true, offhand.getItem(0).isEmpty());
        Inventory respawn = new Inventory(null);
        equal("a pre-claim death can recover the utility book", QuestBookAccess.Result.DELIVERED,
                QuestBookAccess.ensure(ledger, player, respawn, Items.BOOK));
        equal("respawn book recovery does not award the kit", 0, count(respawn, Items.COBBLESTONE));
        equal("old progress is not reset by book recovery", Set.of("multitool"), ledger.claimed(player));

        Inventory full = new Inventory(null);
        for (int slot = 0; slot < full.items.size(); slot++) {
            full.setItem(slot, new ItemStack(Items.BEDROCK, 64));
        }
        ListTag original = full.save(new ListTag());
        equal("full inventory defers the utility book instead of dropping it", QuestBookAccess.Result.INVENTORY_FULL,
                QuestBookAccess.ensure(ledger, player, full, Items.BOOK));
        equal("book deferral cannot destroy or replace carried items", original, full.save(new ListTag()));
        full.setItem(0, ItemStack.EMPTY);
        equal("a deferred book is recoverable when one slot is freed", QuestBookAccess.Result.DELIVERED,
                QuestBookAccess.ensure(ledger, player, full, Items.BOOK));
        equal("a deferred retry creates only one book", QuestBookAccess.Result.PRESENT,
                QuestBookAccess.ensure(ledger, player, full, Items.BOOK));
        ledger.claim(player, QuestCatalogue.STARTER_SUPPLIES);
        Inventory afterClaim = new Inventory(null);
        equal("a post-claim respawn recovers only the utility book",
                QuestBookAccess.Result.DELIVERED,
                QuestBookAccess.ensure(ledger, player, afterClaim, Items.BOOK));
        equal("post-claim recovery creates exactly one utility book", 1, count(afterClaim, Items.BOOK));
        equal("post-claim recovery cannot repeat the kit", 0, count(afterClaim, Items.COBBLESTONE));
        equal("post-claim recovery cannot repeat old rewards", 0, count(afterClaim, Items.CLAY_BALL));
        equal("post-claim recovery retains every saved claim", Set.of("multitool", QuestCatalogue.STARTER_SUPPLIES),
                ledger.claimed(player));
        equal("repeated post-claim recovery is idempotent", QuestBookAccess.Result.PRESENT,
                QuestBookAccess.ensure(ledger, player, afterClaim, Items.BOOK));
    }

    private static void lessons() {
        QuestProgress ledger = new QuestProgress();
        UUID player = UUID.randomUUID();
        Function<QuestCatalogue.Reward, ItemStack> noReward = reward -> {
            throw new AssertionError("A lesson cannot resolve a material reward");
        };
        Function<List<ItemStack>, Optional<Runnable>> noDelivery = rewards -> {
            throw new AssertionError("A lesson cannot enter inventory delivery");
        };
        equal("a read-only lesson has an explicit type", QuestCatalogue.Type.LESSON,
                QuestCatalogue.byId("quest_book_help").type());
        equal("reading still requires the actual book", QuestClaimService.Result.BOOK_REQUIRED,
                QuestClaimService.claim(ledger, player, "quest_book_help", false, task -> false, noReward, noDelivery));
        equal("an explicit reading claim needs no stock or inventory capacity", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, player, "quest_book_help", true, task -> false, noReward, noDelivery));
        equal("reopening reading cannot create a reward", QuestClaimService.Result.ALREADY_CLAIMED,
                QuestClaimService.claim(ledger, player, "quest_book_help", true, task -> false, noReward, noDelivery));
        equal("a lesson does not silently complete the starter or T0", Set.of("quest_book_help"), ledger.claimed(player));
        ledger.claim(player, "multitool");
        equal("an inventory lesson still needs current physical evidence", QuestClaimService.Result.UNMET,
                QuestClaimService.claim(ledger, player, "manual_work", true, task -> false, noReward, noDelivery));
        equal("an inventory checklist can be recorded without material delivery", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, player, "manual_work", true, task -> true, noReward, noDelivery));
        equal("saved reading and checklist claims survive reload", ledger.claimed(player),
                QuestProgress.load(ledger.save(new CompoundTag())).claimed(player));
        equal("another player does not inherit a recorded lesson", Set.of(), ledger.claimed(UUID.randomUUID()));
    }

    private static void t1Claims() {
        QuestProgress ledger = new QuestProgress();
        UUID player = UUID.randomUUID();
        Function<QuestCatalogue.Reward, ItemStack> noReward = reward -> {
            throw new AssertionError("A T1 lesson cannot resolve a material reward");
        };
        Function<List<ItemStack>, Optional<Runnable>> noDelivery = rewards -> {
            throw new AssertionError("A T1 lesson cannot enter inventory delivery");
        };
        for (String id : List.of("first_grid", "forming_dies", "copper_coil", "flux_cell", "wet_beneficiation")) {
            QuestCatalogue.Task task = QuestCatalogue.byId(id);
            equal("new teaching remains in the actual T1 chapter " + id, QuestCatalogue.Chapter.T1,
                    QuestCatalogue.chapter(task));
            equal("new teaching remains rewardless " + id, QuestCatalogue.Type.LESSON, task.type());
            equal("T1 teaching cannot record before the individual final T0 claim " + id,
                    QuestClaimService.Result.LOCKED,
                    QuestClaimService.claim(ledger, player, id, true, value -> true, noReward, noDelivery));
        }
        ledger.claim(player, "relay");
        ledger.claim(player, "pylon");
        equal("the grid reading needs no invented operation or inventory evidence", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, player, "first_grid", true, task -> false, noReward, noDelivery));
        ledger.claim(player, "press");
        equal("forming reading has no separate die-possession tax", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, player, "forming_dies", true, task -> false, noReward, noDelivery));
        for (String id : List.of("copper_coil", "flux_cell")) {
            QuestCatalogue.byId(id).requires().forEach(parent -> ledger.claim(player, parent));
            equal("physical teaching still requires the real book " + id, QuestClaimService.Result.BOOK_REQUIRED,
                    QuestClaimService.claim(ledger, player, id, false, task -> true, noReward, noDelivery));
            equal("physical teaching rejects absent current evidence " + id, QuestClaimService.Result.UNMET,
                    QuestClaimService.claim(ledger, player, id, true, task -> false, noReward, noDelivery));
            equal("physical teaching records only the existing individual checklist " + id,
                    QuestClaimService.Result.CLAIMED,
                    QuestClaimService.claim(ledger, player, id, true, task -> true, noReward, noDelivery));
        }
        ledger.claim(player, "glass");
        List<ItemStack> rewards = new ArrayList<>();
        equal("the terminal service still rejects absent rated evidence without a portal requirement",
                QuestClaimService.Result.UNMET,
                QuestClaimService.claim(ledger, player, "industrial_matrix", true, task -> false,
                        VerifyQuestClaims::resolve, capture(rewards)));
        equal("a rejected frontier cannot mint its reward", 0, rewards.size());
        equal("the old F2 reward is claimable without optional portal progress when its evidence provider passes",
                QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, player, "industrial_matrix", true, task -> true,
                        VerifyQuestClaims::resolve, capture(rewards)));
        equal("the F2 reward remains eight iron ingots", Map.of("minecraft:iron_ingot", 8), quantities(rewards));
        equal("the F2 claim cannot silently complete the optional branch", false,
                ledger.claimed(player).contains("factory_world"));
        equal("the first F2 claim never needs the optional wet acquisition", false,
                ledger.claimed(player).contains("wet_beneficiation"));
        equal("the wet acquisition still needs its existing Assembler parent", QuestClaimService.Result.LOCKED,
                QuestClaimService.claim(ledger, player, "wet_beneficiation", true, task -> true, noReward, noDelivery));
        ledger.claim(player, "assembler");
        equal("the wet acquisition rejects absent current possession", QuestClaimService.Result.UNMET,
                QuestClaimService.claim(ledger, player, "wet_beneficiation", true, task -> false, noReward, noDelivery));
        equal("the optional acquisition never resolves or delivers a reward", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, player, "wet_beneficiation", true, task -> true, noReward, noDelivery));
        equal("the optional acquisition cannot be claimed twice", QuestClaimService.Result.ALREADY_CLAIMED,
                QuestClaimService.claim(ledger, player, "wet_beneficiation", true, task -> true, noReward, noDelivery));
        CompoundTag saved = ledger.save(new CompoundTag());
        QuestProgress restored = QuestProgress.load(saved.copy());
        equal("new lessons and the terminal claim survive the original save schema", ledger.claimed(player),
                restored.claimed(player));
        equal("reopening the old F2 id cannot repay it", QuestClaimService.Result.ALREADY_CLAIMED,
                QuestClaimService.claim(restored, player, "industrial_matrix", true, task -> true,
                        VerifyQuestClaims::resolve, capture(rewards)));
        equal("reconstruction preserves only the one original reward", 1, rewards.size());
        equal("a second player cannot inherit the T1 chapter", QuestClaimService.Result.LOCKED,
                QuestClaimService.claim(restored, UUID.randomUUID(), "first_grid", true, task -> true,
                        noReward, noDelivery));
    }

    private static void invalidRewards() {
        for (Function<QuestCatalogue.Reward, ItemStack> invalid : List
                .<Function<QuestCatalogue.Reward, ItemStack>>of(reward -> ItemStack.EMPTY,
                        reward -> null, reward -> new ItemStack(Items.CLAY_BALL, reward.count() - 1),
                        reward -> new ItemStack(Items.WOODEN_PICKAXE, reward.count()))) {
            QuestProgress ledger = new QuestProgress();
            UUID player = UUID.randomUUID();
            AtomicInteger paid = new AtomicInteger();
            equal("invalid reward rejected before recording a claim", QuestClaimService.Result.INVALID_REWARD,
                    QuestClaimService.claim(ledger, player, "multitool", true, task -> true, invalid,
                            batch -> Optional.of(paid::incrementAndGet)));
            equal("invalid reward leaves the ledger untouched", Set.of(), ledger.claimed(player));
            equal("invalid reward is never delivered", 0, paid.get());
        }

        QuestProgress ledger = new QuestProgress();
        UUID player = UUID.randomUUID();
        Inventory inventory = new Inventory(null);
        AtomicInteger resolved = new AtomicInteger();
        equal("an invalid final stack rejects the entire starter before capacity or progress",
                QuestClaimService.Result.INVALID_REWARD,
                QuestClaimService.claim(ledger, player, QuestCatalogue.STARTER_SUPPLIES, true, task -> false,
                        reward -> resolved.incrementAndGet() == 11 ? ItemStack.EMPTY : resolve(reward),
                        rewards -> QuestRewardDelivery.prepare(inventory, rewards)));
        equal("earlier valid stacks cannot leak from a failed reward batch", true, inventory.isEmpty());
        equal("a late invalid reward cannot consume the claim", Set.of(), ledger.claimed(player));
    }

    private static void projectionAndReload() {
        equal("all catalogue tasks have one native entry", QuestCatalogue.tasks().size(), QuestProjection.entries().size());
        equal("projection is deterministic", QuestProjection.entries(), QuestProjection.entries());
        for (QuestProjection.Entry entry : QuestProjection.entries()) {
            equal("stable owned id " + entry.task().id(), "grindless_" + entry.task().id(), entry.id());
            equal("native parents are legacy claimed prerequisites " + entry.id(),
                    QuestCatalogue.requirements(entry.task()).stream().map(QuestProjection::id).toList(), entry.dependencies());
            equal("projection keeps the exact existing reward " + entry.id(),
                    QuestCatalogue.byId(entry.task().id()).rewards(), entry.task().rewards());
        }
        QuestProgress ledger = new QuestProgress();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Set<String> paid = Set.of("multitool", "quern");
        ledger.claim(first, "multitool");
        ledger.claim(first, "quern");
        CompoundTag original = ledger.save(new CompoundTag());
        equal("original save key is unchanged", "grindless_quest_progress", QuestProgress.KEY);
        equal("saved progress has only paid receipts and native authority roots",
                Set.of("Players", "NativeClaims"), original.getAllKeys());
        equal("unmigrated players have an explicit empty native authority list", new ListTag(),
                original.get("NativeClaims"));
        CompoundTag legacyOnly = original.copy();
        legacyOnly.remove("NativeClaims");
        equal("legacy load fixture retains the historical Players-only shape", Set.of("Players"),
                legacyOnly.getAllKeys());
        QuestProgress legacy = QuestProgress.load(legacyOnly.copy());
        equal("Players-only saves retain every historical paid receipt", paid, legacy.legacyClaims(first));
        equal("Players-only saves do not invent native reward authority", false, legacy.usesNativeClaims(first));
        equal("Players-only reload cannot reopen an already-paid claim", false, legacy.claim(first, "multitool"));
        equal("Players-only saves upgrade only by adding an empty native authority list",
                original, legacy.save(new CompoundTag()));
        QuestProgress restored = QuestProgress.load(original.copy());
        for (int reopen = 0; reopen < 3; reopen++) {
            var projected = QuestProjection.claimed(restored.claimed(first));
            equal("claimed parent reconstructs native complete", true, projected.get("grindless_multitool"));
            equal("claimed child reconstructs native complete", true, projected.get("grindless_quern"));
            equal("possession alone cannot reconstruct completion", false, projected.get("grindless_crank"));
            equal("only owned ids are reconstructed", QuestCatalogue.tasks().size(), projected.size());
            equal("another player stays unclaimed", false,
                    QuestProjection.claimed(restored.claimed(second)).get("grindless_multitool"));
            equal("projection never mutates legacy save", original, restored.save(new CompoundTag()));
            restored = QuestProgress.load(restored.save(new CompoundTag()));
        }
        restored.useNativeClaims(first);
        CompoundTag nativeOriginal = restored.save(new CompoundTag());
        ListTag nativePlayers = new ListTag();
        nativePlayers.add(StringTag.valueOf(first.toString()));
        equal("migrated saves retain only paid receipts and native authority roots",
                Set.of("Players", "NativeClaims"), nativeOriginal.getAllKeys());
        equal("native authority persists exactly the migrated player's UUID", nativePlayers,
                nativeOriginal.get("NativeClaims"));
        equal("migration leaves the complete historical receipt NBT unchanged",
                original.get("Players"), nativeOriginal.get("Players"));
        restored.updateNativeCompleted(first, Set.of("crank"));
        equal("compatibility gates can observe native-only completion", Set.of("multitool", "quern", "crank"),
                restored.claimed(first));
        equal("native-only completion never enters the paid-receipt projection", false,
                QuestProjection.claimed(restored.legacyClaims(first)).get("grindless_crank"));
        equal("native observation and receipt projection never mutate the complete save",
                nativeOriginal, restored.save(new CompoundTag()));
        equal("projection and saving retain the live native observation", Set.of("multitool", "quern", "crank"),
                restored.claimed(first));
        restored = QuestProgress.load(restored.save(new CompoundTag()));
        equal("native authority survives after transient observations are discarded", true,
                restored.usesNativeClaims(first));
        equal("reloaded native authority remains player scoped", false, restored.usesNativeClaims(second));
        equal("native reload retains only historical paid receipts", paid, restored.legacyClaims(first));
        equal("native completion observations are not restored from saved data", paid, restored.claimed(first));
        equal("native reload preserves the complete receipt and authority save",
                nativeOriginal, restored.save(new CompoundTag()));
    }

    private static Function<List<ItemStack>, Optional<Runnable>> capture(List<ItemStack> target) {
        return rewards -> Optional.of(() -> target.addAll(rewards));
    }

    private static ItemStack resolve(QuestCatalogue.Reward reward) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(reward.itemId())), reward.count());
    }

    private static QuestClaimService.Result starterClaim(QuestProgress ledger, UUID player, Inventory inventory) {
        return QuestClaimService.claim(ledger, player, QuestCatalogue.STARTER_SUPPLIES, true, task -> false,
                VerifyQuestClaims::resolve, rewards -> QuestRewardDelivery.prepare(inventory, rewards));
    }

    private static int count(Inventory inventory, net.minecraft.world.item.Item item) {
        int result = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                result += stack.getCount();
            }
        }
        return result;
    }

    private static Map<String, Integer> quantities(List<ItemStack> stacks) {
        Map<String, Integer> result = new TreeMap<>();
        stacks.forEach(stack -> result.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(), Integer::sum));
        return result;
    }

    private static void equal(String description, Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
