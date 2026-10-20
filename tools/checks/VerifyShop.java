package io.github.ezequiel24123z.grindless.shop;

import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.recipe.BootstrapProcesses;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/** Native inventory arithmetic and transient server-menu admission; not a live packet/client proof. */
public final class VerifyShop {
    private static int assertions;
    private static final Function<String, Item> ITEMS = id -> switch (id) {
        case SourceLogic.AGGREGATE -> Items.AMETHYST_SHARD;
        case SourceLogic.BIOMASS -> Items.DRIED_KELP;
        case SourceLogic.FIBREBOARD -> Items.BAMBOO;
        case BootstrapProcesses.GROG -> Items.PRISMARINE_SHARD;
        default -> ShopInventory.resolve(id);
    };

    private VerifyShop() {
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        catalogue();
        atomicPayment();
        capacity();
        staleInventory();
        session();
        cycles();
        System.out.println("ALL SHOP CHECKS PASSED: " + assertions);
    }

    private static void catalogue() {
        Set<String> ids = new HashSet<>();
        Set<String> allowedPayments = Set.of(SourceLogic.AGGREGATE, SourceLogic.BIOMASS,
                SourceLogic.FIBREBOARD, BootstrapProcesses.GROG, "minecraft:bucket");
        for (ShopCatalogue.Offer offer : ShopCatalogue.offers()) {
            check("unique offer", ids.add(offer.id()));
            equal("stable lookup", offer, ShopCatalogue.byId(offer.id()));
            check("physical native goods only, never machines/cores/matrices", offer.goods().item().startsWith("minecraft:"));
            check("no abstract balance, currency or imported token", allowedPayments.containsAll(offer.payment().keySet()));
            check("surplus payment is actually required", offer.payment().keySet().stream().anyMatch(id -> id.startsWith("grindless:")));
            check("prerequisite ids exist in the single ledger", offer.requires().stream().allMatch(id -> QuestCatalogue.byId(id) != null));
            Inventory inventory = new Inventory(null);
            int slot = 0;
            for (var payment : offer.payment().entrySet()) {
                inventory.setItem(slot++, new ItemStack(ITEMS.apply(payment.getKey()), payment.getValue()));
            }
            ListTag before = inventory.save(new ListTag());
            ShopInventory.Prepared plan = prepare(inventory, offer);
            equal("every offer admits an exact funded exchange", ShopResult.READY, plan.result());
            equal("preparation never mutates inventory", before, inventory.save(new ListTag()));
            equal("every offer settles", ShopResult.TRADED, plan.transaction().commit());
            equal("exact native output", offer.goods().count(), count(inventory, ITEMS.apply(offer.goods().item())));
            equal("the prepared exchange cannot mint twice", ShopResult.STALE_REQUEST, plan.transaction().commit());
        }
        equal("unknown ids have no offer", null, ShopCatalogue.byId("invented"));
        equal("food is bounded to a small batch", 2, ShopCatalogue.byId("food").goods().count());
        equal("emergency fuel is one, not a permanent bulk shortcut", 1, ShopCatalogue.byId("fuel").goods().count());
        equal("glass cannot skip first calibration", Set.of("calibrate"), ShopCatalogue.byId("glass").requires());
        equal("water consumes a real container", 1, ShopCatalogue.byId("water").payment().get("minecraft:bucket"));
        equal("no currency or reward changed", 11, QuestCatalogue.byId(QuestCatalogue.STARTER_SUPPLIES).rewards().size());
    }

    private static void atomicPayment() {
        ShopCatalogue.Offer offer = ShopCatalogue.byId("bricks");
        Inventory inventory = new Inventory(null);
        inventory.setItem(0, new ItemStack(ITEMS.apply(SourceLogic.AGGREGATE), 4));
        inventory.setItem(1, new ItemStack(ITEMS.apply(BootstrapProcesses.GROG), 7));
        ListTag before = inventory.save(new ListTag());
        equal("the last missing cost rejects the entire exchange", ShopResult.MISSING_PAYMENT, prepare(inventory, offer).result());
        equal("no first-input debit on a missing second input", before, inventory.save(new ListTag()));
        inventory.getItem(1).setCount(8);
        inventory.getItem(1).getOrCreateTag().putString("display", "preserve");
        equal("named/tagged products are not silently spent", ShopResult.MISSING_PAYMENT, prepare(inventory, offer).result());
        inventory.getItem(1).setTag(null);
        equal("claims are prerequisites, not a second payment/claim ledger", ShopResult.LOCKED,
                ShopInventory.prepare(inventory, offer, Set.of(), ITEMS).result());
        equal("a fully funded plain payment settles once", ShopResult.TRADED, prepare(inventory, offer).transaction().commit());
        equal("all aggregate paid", 0, count(inventory, ITEMS.apply(SourceLogic.AGGREGATE)));
        equal("all grog paid", 0, count(inventory, ITEMS.apply(BootstrapProcesses.GROG)));
        equal("only declared goods received", 4, count(inventory, Items.BRICK));
        ShopCatalogue.Offer invalid = new ShopCatalogue.Offer("invalid", Map.of(SourceLogic.AGGREGATE, 1),
                new ShopCatalogue.Goods("minecraft:air", 1), Set.of(), ShopCatalogue.Role.RECOVERY, "source", "route", "risk");
        equal("invalid registry outputs are explicit errors", ShopResult.INVALID_OFFER,
                ShopInventory.prepare(new Inventory(null), invalid, Set.of(), id -> Items.AIR).result());
    }

    private static void capacity() {
        ShopCatalogue.Offer stone = ShopCatalogue.byId("stone");
        Inventory inventory = filled();
        inventory.setItem(0, new ItemStack(ITEMS.apply(SourceLogic.AGGREGATE), 64));
        ListTag before = inventory.save(new ListTag());
        equal("no output room means no exchange", ShopResult.INVENTORY_FULL, prepare(inventory, stone).result());
        equal("overflow never partially pays or drops", before, inventory.save(new ListTag()));
        inventory.getItem(0).setCount(4);
        equal("fully consumed payment makes real room", ShopResult.TRADED, prepare(inventory, stone).transaction().commit());
        equal("capacity is tested after the debit", 4, count(inventory, Items.COBBLESTONE));
        equal("unrelated inventory remains exact", 35 * 64, count(inventory, Items.BEDROCK));

        Inventory partial = filled();
        partial.setItem(0, new ItemStack(ITEMS.apply(SourceLogic.AGGREGATE), 5));
        partial.setItem(1, new ItemStack(Items.COBBLESTONE, 61));
        before = partial.save(new ListTag());
        equal("insufficient partial output capacity rejects", ShopResult.INVENTORY_FULL, prepare(partial, stone).result());
        equal("failed staging does not fill the partial stack", before, partial.save(new ListTag()));
        partial.getItem(1).setCount(60);
        equal("exact partial capacity succeeds", ShopResult.TRADED, prepare(partial, stone).transaction().commit());
        equal("one paid aggregate remains", 1, count(partial, ITEMS.apply(SourceLogic.AGGREGATE)));
        equal("native stack maximum respected", 64, count(partial, Items.COBBLESTONE));

        Inventory water = filled();
        water.setItem(0, new ItemStack(ITEMS.apply(SourceLogic.AGGREGATE), 8));
        water.setItem(1, new ItemStack(Items.BUCKET, 2));
        equal("water refill stages the unstackable output", ShopResult.TRADED,
                prepare(water, ShopCatalogue.byId("water")).transaction().commit());
        equal("refill returns one filled bucket", 1, count(water, Items.WATER_BUCKET));
        equal("refill preserves the unused empty bucket", 1, count(water, Items.BUCKET));

        Inventory legacy = new Inventory(null);
        legacy.setItem(0, new ItemStack(Items.BEDROCK, 100));
        legacy.setItem(1, new ItemStack(ITEMS.apply(SourceLogic.AGGREGATE), 4));
        ItemStack retained = legacy.getItem(0);
        prepare(legacy, stone).transaction().commit();
        equal("old unusual stacks are not clamped", 100, legacy.getItem(0).getCount());
        check("unchanged native stack identity survives", retained == legacy.getItem(0));
    }

    private static void staleInventory() {
        Inventory inventory = new Inventory(null);
        inventory.setItem(0, new ItemStack(ITEMS.apply(SourceLogic.AGGREGATE), 4));
        ShopInventory.Transaction prepared = prepare(inventory, ShopCatalogue.byId("stone")).transaction();
        inventory.setItem(0, new ItemStack(Items.DIAMOND));
        ListTag before = inventory.save(new ListTag());
        equal("a stale native snapshot cannot overwrite new custody", ShopResult.STALE_INVENTORY, prepared.commit());
        equal("a stale rejection changes nothing", before, inventory.save(new ListTag()));
        equal("a stale plan is still single-use", ShopResult.STALE_REQUEST, prepared.commit());
    }

    private static void session() {
        ShopSession session = new ShopSession(4, 123L);
        AtomicInteger paid = new AtomicInteger();
        java.util.function.Supplier<ShopResult> settle = () -> { paid.incrementAndGet(); return ShopResult.TRADED; };
        equal("wrong menu cannot settle", ShopResult.STALE_REQUEST, session.exchange(5, 123, 0, 0, 0, settle));
        equal("wrong session cannot settle", ShopResult.STALE_REQUEST, session.exchange(4, 124, 0, 0, 0, settle));
        equal("negative offer rejected", ShopResult.INVALID_OFFER, session.exchange(4, 123, 0, -1, 0, settle));
        equal("unbounded offer rejected", ShopResult.INVALID_OFFER,
                session.exchange(4, 123, 0, Integer.MAX_VALUE, 0, settle));
        equal("a well-framed request settles once", ShopResult.TRADED, session.exchange(4, 123, 0, 0, 0, settle));
        equal("packet replay never grants again", ShopResult.STALE_REQUEST, session.exchange(4, 123, 0, 0, 0, settle));
        equal("a rapid new request is acknowledged but not paid", ShopResult.TOO_FAST,
                session.exchange(4, 123, 1, 0, 0, settle));
        equal("only the admitted call settled", 1, paid.get());
        equal("settlement cannot be reentered", ShopResult.TRADED, session.exchange(4, 123, 2, 0, 5, () -> {
            equal("nested call is rejected before its supplier", ShopResult.REENTRANT,
                    session.exchange(4, 123, 3, 0, 5, settle));
            paid.incrementAndGet();
            return ShopResult.TRADED;
        }));
        equal("two real payments, not three", 2, paid.get());
        session.close();
        equal("closed menu requests cannot trade", ShopResult.CLOSED, session.exchange(4, 123, 3, 0, 10, settle));
        ShopSession reopened = new ShopSession(4, 456L);
        equal("reconnect/reused container id does not revive the token", ShopResult.STALE_REQUEST,
                reopened.exchange(4, 123, 0, 0, 10, settle));
        ShopSession exhausted = new ShopSession(1, 1);
        for (int i = 0; i < ShopSession.MAX_SEQUENCE; i++) {
            equal("sequence remains transport-safe", ShopResult.TRADED,
                    exhausted.exchange(1, 1, i, 0, i * 5L, () -> ShopResult.TRADED));
        }
        equal("sequence cannot wrap into a reusable request", ShopResult.CLOSED,
                exhausted.exchange(1, 1, ShopSession.MAX_SEQUENCE, 0, Long.MAX_VALUE, settle));
    }

    private static void cycles() {
        EconomyCycleProof.Certificate proof = EconomyCycleProof.check();
        equal("all live T0 conversions, offers and finite rewards are covered", true, proof.conservative());
        equal("one checked row for every projected authority row", EconomyGraph.rows().size(), proof.checks().size());
        equal("every quest is finite, never an unbounded grant edge", (long) QuestCatalogue.tasks().size(),
                proof.checks().stream().filter(check -> check.disposition().equals("FINITE_ONE_CLAIM_NOT_REPEATABLE")).count());
        List<EconomyGraph.Row> triangle = List.of(row("a_b", "a", 1, "b", 1),
                row("b_c", "b", 1, "c", 1), row("c_a", "c", 1, "a", 2));
        equal("a three-leg exploit is not mistaken for safe buy/sell pairs", false,
                EconomyCycleProof.check(triangle, Map.of("a", 1L, "b", 1L, "c", 1L)).conservative());
        equal("unpriced new conversion fails closed", false,
                EconomyCycleProof.check(List.of(row("unknown", "a", 1, "new", 1)), Map.of("a", 1L)).conservative());
        List<EconomyGraph.Row> combined = List.of(row("a_b", "a", 2, "b", 3), row("b_c", "b", 2, "c", 1),
                row("c_a", "c", 1, "a", 1));
        equal("an arbitrary multi-batch cycle composes under a certificate", true,
                EconomyCycleProof.check(combined, Map.of("a", 3L, "b", 2L, "c", 4L)).conservative());
        equal("large quantity arithmetic is exact rather than wrapping", false,
                EconomyCycleProof.check(List.of(row("overflow", "a", 1, "b", Long.MAX_VALUE)),
                        Map.of("a", 1L, "b", Long.MAX_VALUE)).conservative());
        List<EconomyGraph.Row> mutated = new java.util.ArrayList<>(EconomyGraph.rows());
        mutated.add(row("paid_material_from_nothing", "item:minecraft:bucket", 1,
                "item:" + SourceLogic.AGGREGATE, 64));
        equal("a conversion added outside a buy/sell pair invalidates the proof", false,
                EconomyCycleProof.check(mutated, proof.potential()).conservative());
    }

    private static EconomyGraph.Row row(String id, String input, long consumed, String output, long produced) {
        return new EconomyGraph.Row(id, Map.of(input, consumed), Map.of(output, produced),
                EconomyGraph.Boundary.CONVERSION, Set.of(), "counterexample", 0, 0, "TEST");
    }

    private static ShopInventory.Prepared prepare(Inventory inventory, ShopCatalogue.Offer offer) {
        return ShopInventory.prepare(inventory, offer, offer.requires(), ITEMS);
    }

    private static Inventory filled() {
        Inventory inventory = new Inventory(null);
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.BEDROCK, 64));
        }
        return inventory;
    }

    private static int count(Inventory inventory, Item item) {
        return inventory.items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static void check(String name, boolean value) {
        equal(name, true, value);
    }

    private static void equal(String name, Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }
}
