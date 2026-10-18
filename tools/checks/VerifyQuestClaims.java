package io.github.ezequiel24123z.grindless.quest;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

/** The shared claim service and legacy-to-native authority boundary, without a second reward mint. */
public final class VerifyQuestClaims {

    private static int assertions;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        claims();
        invalidRewards();
        projectionAndReload();
        System.out.println("ALL QUEST CLAIM CHECKS PASSED: " + assertions);
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
                QuestClaimService.claim(ledger, first, "multitool", false, evidence, resolve, rewards::add));
        equal("child requires the parent's claim", QuestClaimService.Result.LOCKED,
                QuestClaimService.claim(ledger, first, "quern", true, evidence, resolve, rewards::add));
        currentEvidence.set(false);
        equal("past possession cannot pay a stale claim", QuestClaimService.Result.UNMET,
                QuestClaimService.claim(ledger, first, "multitool", true, evidence, resolve, rewards::add));
        equal("rejections never write progress", Set.of(), ledger.claimed(first));
        equal("rejections never pay rewards", 0, rewards.size());
        currentEvidence.set(true);
        equal("first valid claim succeeds", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, first, "multitool", true, evidence, resolve, rewards::add));
        equal("one exact reward stack", 4, rewards.get(0).getCount());
        equal("claim does not consume its evidence", true, currentEvidence.get());
        currentEvidence.set(false);
        equal("duplicate claim cannot pay again", QuestClaimService.Result.ALREADY_CLAIMED,
                QuestClaimService.claim(ledger, first, "multitool", true, evidence, resolve, rewards::add));
        equal("one reward after a duplicate request", 1, rewards.size());
        equal("second player does not inherit the claim", Set.of(), ledger.claimed(second));
        equal("second player still needs current evidence", QuestClaimService.Result.UNMET,
                QuestClaimService.claim(ledger, second, "multitool", true, evidence, resolve, rewards::add));
        currentEvidence.set(true);
        equal("second player has an independent claim", QuestClaimService.Result.CLAIMED,
                QuestClaimService.claim(ledger, second, "multitool", true, evidence, resolve, rewards::add));
        equal("two players receive only their own reward", 2, rewards.size());
        equal("unknown request is rejected", QuestClaimService.Result.UNKNOWN_TASK,
                QuestClaimService.claim(ledger, first, "invented", true, evidence, resolve, rewards::add));
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
                            stack -> paid.incrementAndGet()));
            equal("invalid reward leaves the ledger untouched", Set.of(), ledger.claimed(player));
            equal("invalid reward is never delivered", 0, paid.get());
        }
    }

    private static void projectionAndReload() {
        equal("all catalogue tasks have one native entry", QuestCatalogue.tasks().size(), QuestProjection.entries().size());
        equal("projection is deterministic", QuestProjection.entries(), QuestProjection.entries());
        for (QuestProjection.Entry entry : QuestProjection.entries()) {
            equal("stable owned id " + entry.task().id(), "grindless_" + entry.task().id(), entry.id());
            equal("native parents are legacy claimed prerequisites " + entry.id(),
                    entry.task().requires().stream().map(QuestProjection::id).toList(), entry.dependencies());
            equal("projection keeps the exact existing reward " + entry.id(),
                    QuestCatalogue.byId(entry.task().id()).reward(), entry.task().reward());
        }
        QuestProgress ledger = new QuestProgress();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ledger.claim(first, "multitool");
        ledger.claim(first, "quern");
        CompoundTag original = ledger.save(new CompoundTag());
        equal("original save key is unchanged", "grindless_quest_progress", QuestProgress.KEY);
        equal("original save has only the Players root", Set.of("Players"), original.getAllKeys());
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
    }

    private static void equal(String description, Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
