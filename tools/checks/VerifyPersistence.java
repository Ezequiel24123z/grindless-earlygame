package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.container.SlottedContainer;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixNbt;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixSpec;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestInventoryEvidence;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Executable vanilla NBT and inventory contracts; real registered Matrices also need a Forge smoke. */
public final class VerifyPersistence {

    private static int failures;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        outputRoundTrip();
        sparseInputs();
        legacyInventory();
        matrixCodec();
        itemPacket();
        questInventory();
        questProgress();
        System.out.println(failures == 0 ? "ALL PERSISTENCE CHECKS PASSED"
                : failures + " PERSISTENCE CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void outputRoundTrip() throws Exception {
        SimpleContainer original = new SlottedContainer(5);
        ItemStack carrier = new ItemStack(Items.PAPER, 2);
        carrier.getOrCreateTag().putInt("Rating", 2);
        original.setItem(4, carrier);
        CompoundTag saved = new CompoundTag();
        saved.put("Items", original.createTag());
        ListTag items = saved.getList("Items", 10);
        check("output serialization records slot four",
                items.getCompound(0).contains("Slot") && items.getCompound(0).getInt("Slot") == 4);
        SimpleContainer restored = new SlottedContainer(5);
        restored.fromTag(roundTrip(saved).getList("Items", 10));
        check("reconstruction keeps the output out of input zero", restored.getItem(0).isEmpty());
        check("reconstruction preserves the output slot, count and rating",
                ItemStack.isSameItemSameTags(carrier, restored.getItem(4))
                        && restored.getItem(4).getCount() == 2);
        for (int restart = 0; restart < 3; restart++) {
            CompoundTag checkpoint = new CompoundTag();
            checkpoint.put("Items", restored.createTag());
            SimpleContainer next = new SlottedContainer(5);
            next.fromTag(roundTrip(checkpoint).getList("Items", 10));
            restored = next;
        }
        check("repeated reconstruction neither moves nor duplicates the rated output",
                restored.getItem(0).isEmpty() && restored.getItem(4).getCount() == 2
                        && ItemStack.isSameItemSameTags(carrier, restored.getItem(4)));
    }

    private static void sparseInputs() {
        SimpleContainer original = new SlottedContainer(5);
        original.setItem(1, new ItemStack(Items.GLASS, 4));
        original.setItem(3, new ItemStack(Items.REDSTONE, 1));
        SimpleContainer restored = new SlottedContainer(5);
        restored.fromTag(original.createTag());
        check("empty inputs stay empty after loading",
                restored.getItem(0).isEmpty() && restored.getItem(2).isEmpty());
        check("non-primary inputs keep their positions",
                restored.getItem(1).is(Items.GLASS) && restored.getItem(1).getCount() == 4
                        && restored.getItem(3).is(Items.REDSTONE));
    }

    private static void legacyInventory() {
        SimpleContainer compact = new SimpleContainer(5);
        compact.setItem(0, new ItemStack(Items.GLASS, 4));
        compact.setItem(1, new ItemStack(Items.REDSTONE, 2));
        ListTag saved = compact.createTag();
        ListTag before = saved.copy();
        SlottedContainer restored = new SlottedContainer(5);
        restored.fromTag(saved);
        check("legacy compact input order and quantities are retained",
                restored.getItem(0).is(Items.GLASS) && restored.getItem(0).getCount() == 4
                        && restored.getItem(1).is(Items.REDSTONE) && restored.getItem(1).getCount() == 2);
        check("migration does not mutate the caller's saved tag", saved.equals(before));
        restored.fromTag(new ListTag());
        check("missing inventory data restores an empty container", restored.isEmpty());
    }

    private static void matrixCodec() throws Exception {
        ControlMatrixSpec f2 = new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F2);
        CompoundTag saved = ControlMatrixNbt.save(f2);
        check("Matrix serialization keeps the original integer Rating key",
                saved.contains("Rating", 3) && saved.getInt("Rating") == 2 && saved.getAllKeys().size() == 1);
        ControlMatrixSpec decoded = ControlMatrixNbt.load(MatrixArchitecture.RELAY, roundTrip(saved)).orElseThrow();
        check("binary NBT save/load reconstructs the exact F2 specification", decoded.equals(f2));
        check("untagged Relay remains F1",
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, null).orElseThrow().rating() == FluxTier.F1);
        // NBT/data evidence, not a registered-Matrix positive matching or Forge qualification.
        IngredientSpec millMatrix = ProcessGraph.generate(List.of()).stream()
                .filter(recipe -> recipe.id().equals("assemble/wire_mill")).findFirst().orElseThrow()
                .itemInputs().get(3);
        check("the live base mill's fourth group declares minimum-F1 control",
                millMatrix.equals(IngredientSpec.minimumControl(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1)));
        check("valid decoded F2 supplies the minimum control contract (NBT/data, not registered item proof)",
                millMatrix.acceptsMatrix(FabricationLogic.RELAY_MATRIX, decoded));
        check("untagged legacy F1 agrees with the base mill's declared rating (NBT/data contract)",
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, null).orElseThrow().rating().ordinal()
                        == millMatrix.matrixRating());
        CompoundTag explicitF1 = ControlMatrixNbt.save(new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F1));
        check("explicit F1 agrees with the base mill's declared rating (NBT/data contract)",
                ControlMatrixNbt.load(MatrixArchitecture.RELAY, roundTrip(explicitF1))
                        .orElseThrow().rating().ordinal() == millMatrix.matrixRating());
        CompoundTag explicitF3 = ControlMatrixNbt.save(new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F3));
        ControlMatrixSpec decodedF3 = ControlMatrixNbt.load(MatrixArchitecture.RELAY, roundTrip(explicitF3))
                .orElseThrow();
        check("valid decoded F3 supplies the minimum control contract (NBT/data, not registered item proof)",
                decodedF3.rating() == FluxTier.F3
                        && millMatrix.acceptsMatrix(FabricationLogic.RELAY_MATRIX, decodedF3));
        IngredientSpec predecessor = IngredientSpec.matrix(FabricationLogic.RELAY_MATRIX, FluxTier.F1, 1);
        check("the same valid F2 cannot serve as its own exact predecessor",
                !predecessor.acceptsMatrix(FabricationLogic.RELAY_MATRIX, decoded));
        check("a missing Matrix stack fails the real ingredient matcher",
                !ProcessLookup.matches(millMatrix, ItemStack.EMPTY));
        ItemStack wrongKind = new ItemStack(Items.PAPER);
        wrongKind.setTag(explicitF1.copy());
        check("an F1 tag on a non-Matrix item fails the real ingredient matcher",
                !ProcessLookup.matches(millMatrix, wrongKind));
        Path common = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");
        String matrixItem = Files.readString(common.resolve("item/ControlMatrixItem.java"));
        String lookup = Files.readString(common.resolve("recipe/ProcessLookup.java"));
        check("registered Matrix reads reuse this NBT decoder (source contract)",
                matrixItem.contains("return ControlMatrixNbt.load(architecture, stack.getTag());"));
        check("native input matching validates the actual Matrix before dispatching its typed role (source contract)",
                lookup.contains("stack.getItem() instanceof ControlMatrixItem matrix")
                        && lookup.contains("matrix.spec(stack)")
                        && lookup.contains("spec.acceptsMatrix("));
        check("missing Rating on a nonempty tag remains the architecture's first frontier",
                ControlMatrixNbt.load(MatrixArchitecture.INTEGRATED, new CompoundTag())
                        .orElseThrow().rating() == FluxTier.F4);
        List<Consumer<CompoundTag>> numeric = List.of(
                tag -> tag.putByte("Rating", (byte) 2), tag -> tag.putShort("Rating", (short) 2),
                tag -> tag.putInt("Rating", 2), tag -> tag.putLong("Rating", 2L),
                tag -> tag.putFloat("Rating", 2.0f), tag -> tag.putDouble("Rating", 2.0));
        for (int index = 0; index < numeric.size(); index++) {
            CompoundTag tag = new CompoundTag();
            numeric.get(index).accept(tag);
            CompoundTag before = tag.copy();
            check("lossless legacy numeric rating type " + index + " remains valid",
                    ControlMatrixNbt.load(MatrixArchitecture.RELAY, tag).orElseThrow().equals(f2));
            check("reading numeric rating type " + index + " does not rewrite it", tag.equals(before));
        }
        rejectRating("F0 is not a Matrix", tag -> tag.putInt("Rating", 0));
        rejectRating("negative ratings are invalid", tag -> tag.putInt("Rating", -1));
        rejectRating("Relay does not carry F4", tag -> tag.putInt("Rating", 4));
        rejectRating("out-of-ladder ratings are invalid", tag -> tag.putInt("Rating", 16));
        rejectRating("a numeric-looking string is invalid", tag -> tag.putString("Rating", "2"));
        rejectRating("a compound is not a rating", tag -> tag.put("Rating", new CompoundTag()));
        rejectRating("fractional doubles cannot become F2", tag -> tag.putDouble("Rating", 2.75));
        rejectRating("fractional floats cannot become F1", tag -> tag.putFloat("Rating", 1.5f));
        rejectRating("overflowing longs cannot wrap into F2", tag -> tag.putLong("Rating", 4_294_967_298L));
        rejectRating("NaN is invalid", tag -> tag.putDouble("Rating", Double.NaN));
        rejectRating("infinite ratings are invalid", tag -> tag.putDouble("Rating", Double.POSITIVE_INFINITY));
    }

    private static void rejectRating(String label, Consumer<CompoundTag> write) {
        CompoundTag tag = new CompoundTag();
        write.accept(tag);
        check(label, ControlMatrixNbt.load(MatrixArchitecture.RELAY, tag).isEmpty());
    }

    private static void itemPacket() {
        ItemStack carrier = new ItemStack(Items.PAPER, 3);
        carrier.getOrCreateTag().putInt("Rating", 2);
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.writeItem(carrier);
            ItemStack received = packet.readItem();
            check("vanilla item synchronization preserves rating payload and count",
                    ItemStack.isSameItemSameTags(carrier, received) && received.getCount() == 3);
        } finally {
            packet.release();
        }
    }

    private static CompoundTag roundTrip(CompoundTag tag) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            NbtIo.write(tag, output);
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return NbtIo.read(input);
        }
    }

    private static void questInventory() {
        Inventory inventory = new Inventory(null);
        inventory.setItem(40, new ItemStack(Items.PAPER, 3));
        QuestCatalogue.Task paper = new QuestCatalogue.Task("paper", "check", QuestCatalogue.Evidence.ITEM,
                "minecraft:paper", 4, null, List.of(), new QuestCatalogue.Reward("minecraft:paper", 1));
        check("the actual player inventory includes its offhand slot", inventory.getContainerSize() == 41);
        check("an offhand stack is counted exactly once", QuestInventoryEvidence.count(inventory, paper) == 3);
        inventory.setItem(0, new ItemStack(Items.PAPER, 2));
        check("main and offhand stacks add without duplication", QuestInventoryEvidence.count(inventory, paper) == 5);
        check("evidence counting never consumes the inventory",
                inventory.getItem(0).getCount() == 2 && inventory.getItem(40).getCount() == 3);
        inventory.getItem(0).getOrCreateTag().putInt("Rating", 2);
        QuestCatalogue.Task fake = new QuestCatalogue.Task("fake", "check", QuestCatalogue.Evidence.MATRIX,
                "minecraft:paper", 1, FluxTier.F2, List.of(), new QuestCatalogue.Reward("minecraft:paper", 1));
        check("a rating tag on a non-Matrix item cannot satisfy Matrix evidence",
                QuestInventoryEvidence.count(inventory, fake) == 0);
    }

    private static void questProgress() throws Exception {
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        CompoundTag oldPlayer = new CompoundTag();
        oldPlayer.putString("Player", player.toString());
        ListTag oldTasks = new ListTag();
        for (String id : List.of("multitool", "quern", "ceramics", "calibrate", "industrial_matrix")) {
            oldTasks.add(StringTag.valueOf(id));
        }
        oldPlayer.put("Tasks", oldTasks);
        ListTag players = new ListTag();
        players.add(oldPlayer);
        CompoundTag legacy = new CompoundTag();
        legacy.put("Players", players);
        QuestProgress restored = QuestProgress.load(roundTrip(legacy));
        Set<String> previous = Set.of("multitool", "quern", "ceramics", "calibrate", "industrial_matrix");
        check("binary loading of a pre-starter save preserves every old stable quest id",
                restored.claimed(player).equals(previous));
        check("Players-only binary saves remain compatible without inventing native authority",
                legacy.getAllKeys().equals(Set.of("Players"))
                        && !restored.usesNativeClaims(player) && !restored.usesNativeClaims(otherPlayer));
        check("upgrading an old save does not automatically claim the new starter",
                !restored.claimed(player).contains(QuestCatalogue.STARTER_SUPPLIES));
        check("the new explicit starter claim fits the existing ledger",
                restored.claim(player, QuestCatalogue.STARTER_SUPPLIES));
        Set<String> paid = new HashSet<>(previous);
        paid.add(QuestCatalogue.STARTER_SUPPLIES);
        for (int reload = 0; reload < 3; reload++) {
            CompoundTag saved = restored.save(new CompoundTag());
            check("quest progress adds only NativeClaims and keeps the original player/task schema",
                    saved.getAllKeys().equals(Set.of("Players", "NativeClaims"))
                            && saved.get("Players") instanceof ListTag savedPlayers
                            && savedPlayers.getElementType() == Tag.TAG_COMPOUND && savedPlayers.size() == 1
                            && savedPlayers.getCompound(0).getAllKeys().equals(Set.of("Player", "Tasks"))
                            && savedPlayers.getCompound(0).getString("Player").equals(player.toString())
                            && savedPlayers.getCompound(0).get("Tasks") instanceof ListTag savedTasks
                            && savedTasks.getElementType() == Tag.TAG_STRING && savedTasks.size() == paid.size());
            check("legacy saves persist an exactly empty native authority list",
                    new ListTag().equals(saved.get("NativeClaims")));
            restored = QuestProgress.load(roundTrip(saved));
            check("binary save/reload retains old claims alongside starter supplies",
                    restored.claimed(player).containsAll(previous)
                            && restored.claimed(player).contains(QuestCatalogue.STARTER_SUPPLIES));
            check("binary save/reload never resets the one-time starter claim",
                    !restored.claim(player, QuestCatalogue.STARTER_SUPPLIES));
            check("binary reconstruction never gives another player the starter claim",
                    restored.claimed(UUID.randomUUID()).isEmpty());
            check("binary legacy reload retains exactly the paid receipts without native authority",
                    restored.legacyClaims(player).equals(paid)
                            && !restored.usesNativeClaims(player) && !restored.usesNativeClaims(otherPlayer));
        }
        CompoundTag paidSave = restored.save(new CompoundTag());
        restored.useNativeClaims(player);
        restored.useNativeClaims(player);
        ListTag nativePlayers = new ListTag();
        nativePlayers.add(StringTag.valueOf(player.toString()));
        CompoundTag nativeSave = restored.save(new CompoundTag());
        for (int reload = 0; reload < 3; reload++) {
            check("native authority refuses a conflicting unpaid known legacy claim",
                    QuestCatalogue.byId("crank") != null && !restored.legacyClaims(player).contains("crank")
                            && !restored.claim(player, "crank"));
            restored.updateNativeCompleted(player, Set.of("crank"));
            check("native completion opens compatibility gates without becoming a paid receipt",
                    restored.claimed(player).contains("crank") && restored.legacyClaims(player).equals(paid));
            CompoundTag saved = restored.save(new CompoundTag());
            CompoundTag persisted = roundTrip(saved);
            check("binary native saves keep exactly the two ledger roots and the migrated UUID marker",
                    persisted.getAllKeys().equals(Set.of("Players", "NativeClaims"))
                            && nativePlayers.equals(persisted.get("NativeClaims")));
            check("migration retains the original paid player/task NBT unchanged",
                    paidSave.get("Players").equals(persisted.get("Players")));
            check("native observations never change the full persisted reward ledger",
                    saved.equals(nativeSave) && persisted.equals(saved));
            restored = QuestProgress.load(persisted);
            check("binary reload preserves native authority for only the migrated UUID",
                    restored.usesNativeClaims(player) && !restored.usesNativeClaims(otherPlayer));
            check("binary reload retains old receipts and starter but discards native observations",
                    restored.legacyClaims(player).equals(paid) && restored.claimed(player).equals(paid)
                            && !restored.claim(player, QuestCatalogue.STARTER_SUPPLIES));
            check("binary native reconstruction never gives another UUID receipts or completion",
                    restored.legacyClaims(otherPlayer).isEmpty() && restored.claimed(otherPlayer).isEmpty());
        }
        ListTag wrongElement = new ListTag();
        wrongElement.add(IntTag.valueOf(1));
        ListTag wrongPlayer = nativePlayers.copy();
        wrongPlayer.add(StringTag.valueOf("not-a-player"));
        List<Tag> malformedMarkers = List.of(StringTag.valueOf(player.toString()), new CompoundTag(),
                wrongElement, wrongPlayer);
        for (int corruption = 0; corruption < malformedMarkers.size(); corruption++) {
            CompoundTag corrupt = nativeSave.copy();
            corrupt.put("NativeClaims", malformedMarkers.get(corruption).copy());
            CompoundTag persisted = roundTrip(corrupt);
            boolean rejected = false;
            try {
                QuestProgress.load(persisted);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            check("malformed NativeClaims case " + corruption + " fails closed on binary reload",
                    rejected && persisted.equals(corrupt));
        }
        check("the quest SavedData key has not migrated", QuestProgress.KEY.equals("grindless_quest_progress"));
    }

    private static void check(String label, boolean passed) {
        System.out.println((passed ? "PASS " : "FAIL ") + label);
        if (!passed) {
            failures++;
        }
    }
}
