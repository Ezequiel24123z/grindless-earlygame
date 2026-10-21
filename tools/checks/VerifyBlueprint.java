package io.github.ezequiel24123z.grindless.item;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Blueprint contracts with vanilla NBT/stacks and a read-only site fixture, not a native-world proof. */
public final class VerifyBlueprint {

    private static int failures = 0;
    private static final String STONE = "minecraft:stone";
    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

    public static void main(String[] args) throws Exception {
        bounds();
        pieces();
        plans();
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        payload();
        registryAndFacing();
        destinations();
        capture();
        payment();
        sourceBoundary();
        System.out.println(failures == 0
                ? "ALL BLUEPRINT CHECKS PASSED"
                : failures + " BLUEPRINT CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void bounds() {
        eq("an edge may be thirty-two", true, BlueprintLogic.edgeOk(0, 31));
        eq("thirty-three is too long", false, BlueprintLogic.edgeOk(0, 32));
        eq("a reversed edge is not a box", false, BlueprintLogic.edgeOk(4, 1));
        eq("one block is a box", true, BlueprintLogic.edgeOk(7, 7));
        eq("an overflowing edge is refused", false,
                BlueprintLogic.edgeOk(Integer.MIN_VALUE, Integer.MAX_VALUE));
        eq("a positive overflowing span is refused", false,
                BlueprintLogic.edgeOk(0, Integer.MAX_VALUE));
        eq("the largest single coordinate is a box", true,
                BlueprintLogic.edgeOk(Integer.MAX_VALUE, Integer.MAX_VALUE));
        eq("empty is not a blueprint", false, BlueprintLogic.countOk(0));
        eq("a negative count is not a blueprint", false, BlueprintLogic.countOk(-1));
        eq("five hundred twelve is the cap", true, BlueprintLogic.countOk(512));
        eq("five hundred thirteen is too many", false, BlueprintLogic.countOk(513));
    }

    private static void pieces() {
        eq("facing is kept", "north", BlueprintLogic.keptProperty("facing", "north"));
        eq("status is dropped", "", BlueprintLogic.keptProperty("status", "running"));
        eq("a blank facing is dropped", "", BlueprintLogic.keptProperty("facing", " "));
        List<BlueprintLogic.Piece> layout = List.of(
                new BlueprintLogic.Piece(0, 0, 0, "grindless:assembler", "grindless:assembler", "north"),
                new BlueprintLogic.Piece(1, 0, 0, "grindless:assembler", "grindless:assembler", "south"),
                new BlueprintLogic.Piece(0, 1, 0, "grindless:conveyor_belt", "grindless:conveyor_belt", ""));
        Map<String, Integer> cost = BlueprintLogic.cost(layout);
        eq("two assemblers", 2, cost.get("grindless:assembler").intValue());
        eq("one belt", 1, cost.get("grindless:conveyor_belt").intValue());
        Map<String, Integer> have = Map.of("grindless:assembler", 2, "grindless:conveyor_belt", 1);
        yes("a paid stamp has no shortfall", BlueprintLogic.shortfall(cost, have).isEmpty());
        Map<String, Integer> shortOne = BlueprintLogic.shortfall(cost, Map.of("grindless:assembler", 1));
        eq("one assembler short", 1, shortOne.get("grindless:assembler").intValue());
        eq("the belt is also short", 1, shortOne.get("grindless:conveyor_belt").intValue());
        eq("negative available stock is not credit", Map.of(STONE, 1),
                BlueprintLogic.shortfall(Map.of(STONE, 1), Map.of(STONE, Integer.MIN_VALUE)));
    }

    private static void plans() {
        List<BlueprintLogic.Piece> layout = new ArrayList<>(stoneLine(2));
        Map<String, Integer> have = new HashMap<>(Map.of(STONE, 2));
        BlueprintLogic.Plan plan = BlueprintLogic.preflight(layout, have, false, piece -> BlueprintLogic.Failure.NONE);
        yes("a complete plan is ready", plan.ready());
        eq("the complete multiset is retained", Map.of(STONE, 2), plan.required());
        layout.clear();
        have.clear();
        eq("the plan snapshots the layout", 2, plan.pieces().size());
        yes("later stock changes cannot rewrite a diagnosis", plan.missing().isEmpty());
        throwsType("plan pieces are immutable", UnsupportedOperationException.class,
                () -> plan.pieces().clear());
        throwsType("plan costs are immutable", UnsupportedOperationException.class,
                () -> plan.required().clear());
        throwsType("plan missing materials are immutable", UnsupportedOperationException.class,
                () -> plan.missing().put(STONE, 7));
        throwsType("plan problems are immutable", UnsupportedOperationException.class,
                () -> plan.problems().clear());

        eq("null layouts are malformed", BlueprintLogic.Failure.MALFORMED,
                failure(BlueprintLogic.preflight(null, Map.of(), false, piece -> BlueprintLogic.Failure.NONE)));
        eq("null pieces are malformed", BlueprintLogic.Failure.MALFORMED,
                failure(BlueprintLogic.preflight(Arrays.asList((BlueprintLogic.Piece) null), Map.of(),
                        false, piece -> BlueprintLogic.Failure.NONE)));
        eq("empty layouts remain empty", BlueprintLogic.Failure.EMPTY,
                failure(BlueprintLogic.preflight(List.of(), Map.of(), false, piece -> BlueprintLogic.Failure.NONE)));
        yes("all 512 unique pieces fit", BlueprintLogic.preflight(cube(512), Map.of(STONE, 512),
                false, piece -> BlueprintLogic.Failure.NONE).ready());
        yes("the far corner of the exact 32-cube fits", BlueprintLogic.preflight(List.of(
                new BlueprintLogic.Piece(31, 31, 31, STONE, STONE, "")), Map.of(STONE, 1),
                false, piece -> BlueprintLogic.Failure.NONE).ready());
        eq("513 unique pieces are refused", BlueprintLogic.Failure.TOO_LARGE,
                failure(BlueprintLogic.preflight(cube(513), Map.of(), true, piece -> BlueprintLogic.Failure.NONE)));

        List<BlueprintLogic.Piece> invalid = new ArrayList<>();
        invalid.add(new BlueprintLogic.Piece(0, 0, 0, "", STONE, ""));
        invalid.add(new BlueprintLogic.Piece(0, 0, 0, STONE, "", ""));
        invalid.add(new BlueprintLogic.Piece(0, 0, 0, null, STONE, ""));
        invalid.add(new BlueprintLogic.Piece(0, 0, 0, STONE, null, ""));
        for (int coordinate : new int[]{-1, 32, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            invalid.add(new BlueprintLogic.Piece(coordinate, 0, 0, STONE, STONE, ""));
            invalid.add(new BlueprintLogic.Piece(0, coordinate, 0, STONE, STONE, ""));
            invalid.add(new BlueprintLogic.Piece(0, 0, coordinate, STONE, STONE, ""));
        }
        for (BlueprintLogic.Piece piece : invalid) {
            int[] inspections = {0};
            BlueprintLogic.Plan rejected = BlueprintLogic.preflight(List.of(piece), Map.of(), true, ignored -> {
                inspections[0]++;
                return BlueprintLogic.Failure.NONE;
            });
            eq("invalid pieces fail structurally", BlueprintLogic.Failure.MALFORMED, failure(rejected));
            eq("invalid pieces never inspect the world", 0, inspections[0]);
        }
        BlueprintLogic.Piece sameCell = new BlueprintLogic.Piece(0, 0, 0, "minecraft:dirt", "minecraft:dirt", "");
        eq("different blocks cannot share a cell", BlueprintLogic.Failure.DUPLICATE,
                failure(BlueprintLogic.preflight(List.of(stoneLine(1).get(0), sameCell), Map.of(), true,
                        piece -> { throw new AssertionError("duplicate reached the inspector"); })));
        BlueprintLogic.Plan shortage = BlueprintLogic.preflight(List.of(stoneLine(1).get(0),
                new BlueprintLogic.Piece(1, 0, 0, "minecraft:dirt", "minecraft:dirt", "")), Map.of(),
                false, piece -> BlueprintLogic.Failure.BLOCKED);
        eq("all site problems are retained", 2, shortage.problems().size());
        eq("all missing item types are retained", Map.of(STONE, 1, "minecraft:dirt", 1), shortage.missing());
        eq("blocked cells do not hide the bill", Map.of(STONE, 1, "minecraft:dirt", 1), shortage.required());
    }

    private static void payload() {
        List<BlueprintLogic.Piece> legacy = List.of(stoneLine(1).get(0),
                new BlueprintLogic.Piece(1, 0, 0, "minecraft:furnace", "minecraft:furnace", "east"));
        ItemStack carrier = carrier(legacy);
        CompoundTag before = carrier.getTag().copy();
        eq("the old payload round-trips", legacy, BlueprintItem.read(carrier));
        eq("reading does not change NBT", before, carrier.getTag());
        ListTag blocks = carrier.getTag().getList("Blocks", Tag.TAG_COMPOUND);
        eq("the old un-oriented shape is unchanged", Set.of("X", "Y", "Z", "Id", "Item"),
                blocks.getCompound(0).getAllKeys());
        eq("facing remains the only optional state field", Set.of("X", "Y", "Z", "Id", "Item", "Facing"),
                blocks.getCompound(1).getAllKeys());
        ItemStack reloaded = ItemStack.of(carrier.save(new CompoundTag()));
        eq("vanilla item-NBT reload preserves the old pieces", legacy, BlueprintItem.read(reloaded));
        blocks.getCompound(1).putBoolean("lit", true);
        blocks.getCompound(1).putInt("Energy", 99999);
        CompoundTag machine = new CompoundTag();
        machine.putString("id", "minecraft:furnace");
        machine.putInt("BurnTime", 999);
        blocks.getCompound(1).put("BlockEntityTag", machine);
        eq("arbitrary machine NBT is not a piece", legacy, BlueprintItem.read(carrier));
        BlueprintItem.StampPlan projected = BlueprintItem.preflight(BlueprintItem.decode(carrier), ORIGIN,
                new Site(), Map.of(), true);
        yes("extra machine NBT cannot invalidate the legacy whitelist", projected.diagnosis().ready());
        eq("only the saved facing is applied", Direction.EAST,
                projected.placements().get(1).state().getValue(BlockStateProperties.HORIZONTAL_FACING));
        eq("a stamped machine keeps its idle default", false,
                projected.placements().get(1).state().getValue(BlockStateProperties.LIT));
        ItemStack clean = carrier(BlueprintItem.read(carrier));
        eq("writing never propagates arbitrary machine fields", Set.of("X", "Y", "Z", "Id", "Item", "Facing"),
                clean.getTag().getList("Blocks", Tag.TAG_COMPOUND).getCompound(1).getAllKeys());

        ItemStack empty = new ItemStack(Items.PAPER);
        yes("a tagless carrier reads as an empty legacy blueprint", BlueprintItem.read(empty).isEmpty());
        eq("reading cannot create a root tag", null, empty.getTag());
        ItemStack wrongList = new ItemStack(Items.PAPER);
        wrongList.getOrCreateTag().putString("Blocks", "not a list");
        malformed("non-list Blocks is malformed", wrongList);
        ListTag integers = new ListTag();
        integers.add(IntTag.valueOf(1));
        wrongList.getOrCreateTag().put("Blocks", integers);
        malformed("non-compound entries are malformed", wrongList);
        for (String key : List.of("X", "Y", "Z", "Id", "Item")) {
            ItemStack missing = carrier(stoneLine(1));
            missing.getTag().getList("Blocks", Tag.TAG_COMPOUND).getCompound(0).remove(key);
            malformed("missing " + key + " is not defaulted", missing);
        }
        for (String key : List.of("X", "Y", "Z")) {
            ItemStack mistyped = carrier(stoneLine(1));
            mistyped.getTag().getList("Blocks", Tag.TAG_COMPOUND).getCompound(0).putByte(key, (byte) 0);
            malformed("non-int " + key + " is not coerced", mistyped);
        }
        for (String key : List.of("Id", "Item", "Facing")) {
            ItemStack mistyped = carrier(stoneLine(1));
            mistyped.getTag().getList("Blocks", Tag.TAG_COMPOUND).getCompound(0).putInt(key, 0);
            malformed("non-string " + key + " is not defaulted", mistyped);
        }
        eq("oversized old payloads are explicitly refused", BlueprintLogic.Failure.TOO_LARGE,
                BlueprintItem.decode(carrier(cube(513))).failure());
    }

    private static void registryAndFacing() {
        for (String block : List.of("missing:unregistered_block", "Not a valid id")) {
            pieceFailure("unknown blocks cannot silently become air", BlueprintLogic.Failure.UNKNOWN_BLOCK,
                    new BlueprintLogic.Piece(0, 0, 0, block, STONE, ""));
        }
        for (String item : List.of("missing:unregistered_item", "Not a valid id",
                "minecraft:air", "minecraft:dirt", "minecraft:stick", "stone")) {
            pieceFailure("a block must be paid with its real registered item", BlueprintLogic.Failure.INVALID_ITEM,
                    new BlueprintLogic.Piece(0, 0, 0, STONE, item, ""));
        }
        for (String facing : List.of("diagonal", "up")) {
            pieceFailure("invalid horizontal orientations are refused", BlueprintLogic.Failure.INVALID_FACING,
                    new BlueprintLogic.Piece(0, 0, 0, "minecraft:furnace", "minecraft:furnace", facing));
        }
        pieceFailure("non-facing blocks cannot accept a saved direction", BlueprintLogic.Failure.INVALID_FACING,
                new BlueprintLogic.Piece(0, 0, 0, STONE, STONE, "north"));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlueprintLogic.Piece furnace = new BlueprintLogic.Piece(0, 0, 0,
                    "minecraft:furnace", "minecraft:furnace", facing.getName());
            BlueprintItem.StampPlan plan = plan(List.of(furnace), new Site(), Map.of(), true);
            yes("the four old horizontal orientations remain valid", plan.diagnosis().ready());
            eq("the actual default state receives the saved orientation", facing,
                    plan.placements().get(0).state().getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        for (Direction facing : List.of(Direction.UP, Direction.DOWN)) {
            BlueprintLogic.Piece piston = new BlueprintLogic.Piece(0, 0, 0,
                    "minecraft:piston", "minecraft:piston", facing.getName());
            BlueprintItem.StampPlan plan = plan(List.of(piston), new Site(), Map.of(), true);
            yes("legal vertical orientations remain valid", plan.diagnosis().ready());
            eq("vertical facing is not flattened", facing,
                    plan.placements().get(0).state().getValue(BlockStateProperties.FACING));
        }
    }

    private static void destinations() {
        List<BlueprintLogic.Piece> layout = stoneLine(2);
        Site site = new Site();
        Inventory inventory = stock(2);
        ItemStack carrier = carrier(layout);
        CompoundTag nbt = carrier.getTag().copy();
        Map<BlockPos, BlockState> world = new HashMap<>(site.blocks);
        BlueprintItem.StampPlan plan = BlueprintItem.preflight(BlueprintItem.decode(carrier), ORIGIN, site,
                BlueprintItem.contents(inventory), false);
        yes("a clear paid destination is ready", plan.diagnosis().ready());
        eq("preflight leaves every world cell alone", world, site.blocks);
        eq("preflight leaves the real inventory alone", Map.of(STONE, 2), BlueprintItem.contents(inventory));
        eq("preflight leaves the payload alone", nbt, carrier.getTag());
        eq("the plan contains the full resolved destination", ORIGIN.offset(1, 0, 0),
                plan.placements().get(1).position());
        BlockPos.MutableBlockPos mutable = ORIGIN.mutable();
        BlueprintItem.StampPlan frozen = BlueprintItem.preflight(BlueprintItem.decode(carrier), mutable,
                site, Map.of(), true);
        mutable.move(100, 100, 100);
        eq("resolved positions are immutable snapshots", ORIGIN, frozen.placements().get(0).position());

        for (BlueprintLogic.Failure failure : List.of(BlueprintLogic.Failure.OUTSIDE_WORLD,
                BlueprintLogic.Failure.UNLOADED, BlueprintLogic.Failure.FORBIDDEN, BlueprintLogic.Failure.BLOCKED)) {
            Site invalid = new Site();
            switch (failure) {
                case OUTSIDE_WORLD -> invalid.outside.add(ORIGIN);
                case UNLOADED -> invalid.unloaded.add(ORIGIN);
                case FORBIDDEN -> invalid.forbidden.add(ORIGIN);
                case BLOCKED -> invalid.blocks.put(ORIGIN, Blocks.STONE.defaultBlockState());
                default -> throw new AssertionError(failure);
            }
            BlueprintItem.StampPlan rejected = plan(stoneLine(1), invalid, Map.of(), true);
            eq("the exact site failure is reported", failure, failure(rejected.diagnosis()));
            if (failure != BlueprintLogic.Failure.BLOCKED) {
                eq("an inadmissible cell is never read", 0, invalid.reads);
            }
            eq("invalid sites never invent a placement", 0, rejected.placements().size());
            Inventory untouched = stock(2);
            BlueprintItem.StampResult refused = BlueprintItem.stamp(rejected, invalid, untouched, false,
                    (position, state) -> { throw new AssertionError("inadmissible site placed"); },
                    stack -> { throw new AssertionError("inadmissible site reserved payment"); });
            eq("site refusal leaves all material in place", Map.of(STONE, 2), BlueprintItem.contents(untouched));
            eq("site refusal cannot place a partial stamp", 0, refused.placed());
        }
        Site callbackUnload = new Site() {
            @Override
            public boolean mayBuild(BlockPos position) {
                unloaded.add(position);
                return true;
            }
        };
        eq("permission callbacks cannot cause an unloaded read", BlueprintLogic.Failure.UNLOADED,
                failure(plan(stoneLine(1), callbackUnload, Map.of(), true).diagnosis()));
        eq("the post-permission load guard prevents reads", 0, callbackUnload.reads);
        Site noRead = new Site();
        BlueprintLogic.Piece offset = new BlueprintLogic.Piece(1, 0, 0, STONE, STONE, "");
        BlueprintItem.StampPlan overflow = BlueprintItem.preflight(BlueprintItem.decode(carrier(List.of(offset))),
                new BlockPos(Integer.MAX_VALUE, 0, 0), noRead, Map.of(), true);
        eq("overflowing destinations are rejected before wrapping", BlueprintLogic.Failure.OUTSIDE_WORLD,
                failure(overflow.diagnosis()));
        eq("overflowing destinations do not inspect chunks", 0, noRead.reads);
        BlueprintItem.StampPlan duplicate = plan(List.of(layout.get(0), layout.get(0)),
                noRead, Map.of(), true);
        eq("NBT duplicates use the shared diagnosis", BlueprintLogic.Failure.DUPLICATE,
                failure(duplicate.diagnosis()));
        eq("duplicate NBT never inspects chunks", 0, noRead.reads);
    }

    private static void capture() {
        Site site = new Site();
        site.blocks.put(ORIGIN, Blocks.FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.LIT, true));
        site.blocks.put(ORIGIN.offset(1, 0, 0), Blocks.WATER.defaultBlockState());
        BlueprintLogic.Plan captured = BlueprintToolItem.capture(site, 0, 64, 0, 2, 64, 0);
        yes("capture uses the shared legal-piece preflight", captured.ready());
        eq("capture still skips air and blocks without items", 1, captured.pieces().size());
        eq("capture retains facing only", new BlueprintLogic.Piece(0, 0, 0,
                "minecraft:furnace", "minecraft:furnace", "west"), captured.pieces().get(0));
        eq("capture reports the complete material bill", Map.of("minecraft:furnace", 1), captured.required());
        eq("capture never changes the original machine state", true,
                site.blocks.get(ORIGIN).getValue(BlockStateProperties.LIT));
        Site unloaded = new Site();
        unloaded.unloaded.add(ORIGIN);
        eq("capture refuses unloaded source cells", BlueprintLogic.Failure.UNLOADED,
                failure(BlueprintToolItem.capture(unloaded, 0, 64, 0, 0, 64, 0)));
        eq("capture does not force-read the unloaded source", 0, unloaded.reads);
        Site outside = new Site();
        outside.outside.add(ORIGIN);
        eq("capture refuses out-of-bounds source cells", BlueprintLogic.Failure.OUTSIDE_WORLD,
                failure(BlueprintToolItem.capture(outside, 0, 64, 0, 0, 64, 0)));
        eq("capture does not read out-of-bounds sources", 0, outside.reads);
        Site filled = new Site();
        filled.defaultState = Blocks.STONE.defaultBlockState();
        yes("capture keeps the exact 512-piece bound",
                BlueprintToolItem.capture(filled, 0, 64, 0, 31, 64, 15).ready());
        eq("capture refuses the 513th part", BlueprintLogic.Failure.TOO_LARGE,
                failure(BlueprintToolItem.capture(filled, 0, 64, 0, 31, 64, 16)));
        int before = filled.reads;
        eq("capture keeps the exact 32-block edge", BlueprintLogic.Failure.TOO_LARGE,
                failure(BlueprintToolItem.capture(filled, 0, 64, 0, 32, 64, 0)));
        eq("oversized boxes are rejected before reading", before, filled.reads);
        eq("empty captures remain explicit", BlueprintLogic.Failure.EMPTY,
                failure(BlueprintToolItem.capture(new Site(), 0, 64, 0, 0, 64, 0)));
        eq("extreme single-cell capture terminates", BlueprintLogic.Failure.EMPTY,
                failure(BlueprintToolItem.capture(new Site(), Integer.MAX_VALUE, Integer.MAX_VALUE,
                        Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)));
    }

    private static void payment() {
        Inventory split = stock(1);
        split.items.set(12, new ItemStack(Items.STONE, 2));
        split.offhand.set(0, new ItemStack(Items.STONE, 3));
        split.armor.set(0, new ItemStack(Items.STONE, 64));
        eq("main slots and offhand are summed, never armour", Map.of(STONE, 6), BlueprintItem.contents(split));
        split.items.get(12).setCount(Integer.MAX_VALUE);
        eq("corrupt huge counts cannot overflow the bounded bill", Map.of(STONE, 512), BlueprintItem.contents(split));

        Site success = new Site();
        Inventory materials = stock(1);
        materials.offhand.set(0, new ItemStack(Items.STONE));
        List<ItemStack> refunds = new ArrayList<>();
        BlueprintItem.StampPlan plan = plan(stoneLine(2), success, BlueprintItem.contents(materials), false);
        BlueprintItem.StampResult built = BlueprintItem.stamp(plan, success, materials, false, (position, state) -> {
            yes("all payment is reserved before placement callbacks", BlueprintItem.contents(materials).isEmpty());
            success.blocks.put(position, state);
            return true;
        }, refunds::add);
        yes("a successful stamp is ready", built.diagnosis().ready());
        eq("a successful stamp reports actual placed count", 2, built.placed());
        eq("a successful stamp pays exactly its multiset", Map.of(), BlueprintItem.contents(materials));
        yes("a successful stamp refunds nothing", refunds.isEmpty());

        List<BlueprintLogic.Piece> mixed = List.of(stoneLine(1).get(0),
                new BlueprintLogic.Piece(1, 0, 0, "minecraft:dirt", "minecraft:dirt", ""),
                new BlueprintLogic.Piece(2, 0, 0, STONE, STONE, ""));
        Inventory mixedStock = stock(3);
        mixedStock.offhand.set(0, new ItemStack(Items.DIRT));
        mixedStock.items.set(9, new ItemStack(Items.DIAMOND, 5));
        Site mixedSite = new Site();
        BlueprintItem.StampResult mixedResult = BlueprintItem.stamp(
                plan(mixed, mixedSite, BlueprintItem.contents(mixedStock), false), mixedSite, mixedStock, false,
                (position, state) -> {
                    mixedSite.blocks.put(position, state);
                    return true;
                }, stack -> { throw new AssertionError("complete mixed bill refunded"); });
        eq("every material type pays for its own placements", 3, mixedResult.placed());
        eq("surplus and unrelated material remain untouched",
                Map.of(STONE, 1, "minecraft:diamond", 5), BlueprintItem.contents(mixedStock));

        Site shortage = new Site();
        Inventory one = stock(1);
        BlueprintItem.StampPlan unpaid = plan(stoneLine(2), shortage, BlueprintItem.contents(one), false);
        BlueprintItem.StampResult refused = BlueprintItem.stamp(unpaid, shortage, one, false,
                (position, state) -> { throw new AssertionError("unpaid placement"); },
                stack -> { throw new AssertionError("unpaid refund"); });
        eq("all missing units remain named", Map.of(STONE, 1), refused.diagnosis().missing());
        eq("failed preflight spends nothing", Map.of(STONE, 1), BlueprintItem.contents(one));
        eq("failed preflight places nothing", 0, refused.placed());
        Inventory changed = stock(2);
        BlueprintItem.StampPlan stale = plan(stoneLine(2), new Site(), BlueprintItem.contents(changed), false);
        changed.items.get(0).shrink(2);
        BlueprintItem.StampResult staleResult = BlueprintItem.stamp(stale, new Site(), changed, false,
                (position, state) -> { throw new AssertionError("stale payment placed"); }, refunds::add);
        eq("changed payment has an explicit failure", BlueprintLogic.Failure.MATERIALS_CHANGED,
                failure(staleResult.diagnosis()));
        eq("changed payment reports the entire current shortfall", Map.of(STONE, 2), staleResult.diagnosis().missing());
        yes("a refused reservation cannot look successful", !staleResult.diagnosis().ready());

        for (int failedAt : new int[]{0, 1}) {
            Site partial = new Site();
            Inventory paid = stock(2);
            paid.items.get(0).getOrCreateTag().putString("Marker", "refund keeps original NBT");
            List<ItemStack> unused = new ArrayList<>();
            int[] attempts = {0};
            BlueprintItem.StampResult result = BlueprintItem.stamp(
                    plan(stoneLine(2), partial, BlueprintItem.contents(paid), false), partial, paid, false,
                    (position, state) -> {
                        if (attempts[0]++ == failedAt) {
                            return false;
                        }
                        partial.blocks.put(position, state);
                        return true;
                    }, unused::add);
            eq("false placement is reported, not ignored", BlueprintLogic.Failure.PLACEMENT_FAILED,
                    failure(result.diagnosis()));
            eq("only confirmed placements are counted", failedAt, result.placed());
            eq("only confirmed cells remain in the world", failedAt, partial.blocks.size());
            eq("failed and unattempted blocks are refunded exactly", 2 - failedAt, total(unused));
            yes("refunds retain original item NBT", unused.stream()
                    .allMatch(stack -> "refund keeps original NBT".equals(stack.getTag().getString("Marker"))));
            eq("nothing is deducted twice after refund", Map.of(), BlueprintItem.contents(paid));
        }
        Site changedSite = new Site();
        Inventory paid = stock(2);
        List<ItemStack> unused = new ArrayList<>();
        BlueprintItem.StampResult interrupted = BlueprintItem.stamp(
                plan(stoneLine(2), changedSite, BlueprintItem.contents(paid), false), changedSite, paid, false,
                (position, state) -> {
                    changedSite.blocks.put(position, state);
                    changedSite.blocks.put(ORIGIN.offset(1, 0, 0), Blocks.DIRT.defaultBlockState());
                    return true;
                }, unused::add);
        eq("callbacks cannot make a later occupied cell overwriteable", BlueprintLogic.Failure.BLOCKED,
                failure(interrupted.diagnosis()));
        eq("the prior successful cell is not falsely rolled back", 1, interrupted.placed());
        eq("the later blocked cell is not paid", 1, total(unused));
        eq("the unrelated callback cell is preserved", Blocks.DIRT.defaultBlockState(),
                changedSite.blocks.get(ORIGIN.offset(1, 0, 0)));
        for (boolean unload : new boolean[]{false, true}) {
            Site changedBeforeApply = new Site();
            Inventory untouched = stock(2);
            BlueprintItem.StampPlan ready = plan(stoneLine(2), changedBeforeApply,
                    BlueprintItem.contents(untouched), false);
            if (unload) {
                changedBeforeApply.unloaded.add(ORIGIN);
            } else {
                changedBeforeApply.blocks.put(ORIGIN, Blocks.DIRT.defaultBlockState());
            }
            List<ItemStack> returned = new ArrayList<>();
            BlueprintItem.StampResult refusedBeforeFirst = BlueprintItem.stamp(ready, changedBeforeApply,
                    untouched, false, (position, state) -> { throw new AssertionError("stale site placed"); },
                    returned::add);
            eq("a stale site is rechecked before the first placement",
                    unload ? BlueprintLogic.Failure.UNLOADED : BlueprintLogic.Failure.BLOCKED,
                    failure(refusedBeforeFirst.diagnosis()));
            eq("a stale site cannot charge an unplaced block", 2, total(returned));
            eq("a stale site has no confirmed placements", 0, refusedBeforeFirst.placed());
        }

        Site throwing = new Site();
        Inventory throwingStock = stock(2);
        List<ItemStack> afterThrow = new ArrayList<>();
        IllegalStateException expected = new IllegalStateException("callback failed after changing the cell");
        try {
            BlueprintItem.stamp(plan(stoneLine(2), throwing, BlueprintItem.contents(throwingStock), false),
                    throwing, throwingStock, false, (position, state) -> {
                        throwing.blocks.put(position, state);
                        throw expected;
                    }, afterThrow::add);
            fail("throwing placement must propagate its original failure");
        } catch (IllegalStateException actual) {
            yes("the original callback exception is not swallowed", actual == expected);
        }
        eq("an ambiguous attempted cell stays paid", 1, throwing.blocks.size());
        eq("unattempted resources survive an exception", 1, total(afterThrow));

        Site creative = new Site();
        Inventory empty = stock(0);
        BlueprintItem.StampResult free = BlueprintItem.stamp(plan(stoneLine(2), creative, Map.of(), true),
                creative, empty, true, (position, state) -> {
                    creative.blocks.put(position, state);
                    return true;
                }, stack -> { throw new AssertionError("creative cannot mint refunds"); });
        eq("creative still builds the whole valid layout", 2, free.placed());
        eq("creative spends no inventory", Map.of(), BlueprintItem.contents(empty));
    }

    private static void sourceBoundary() throws Exception {
        Path directory = Path.of("common", "src", "main", "java", "io", "github",
                "ezequiel24123z", "grindless", "item");
        String item = Files.readString(directory.resolve("BlueprintItem.java"));
        String tool = Files.readString(directory.resolve("BlueprintToolItem.java"));
        yes("the runtime adapter checks loaded chunks without forcing them",
                item.contains("return level.hasChunkAt(position);"));
        yes("the adapter uses the actual build height and world border",
                item.contains("level.isInWorldBounds(position)")
                        && item.contains("level.getWorldBorder().isWithinBounds(position)"));
        yes("the adapter checks native player interaction and item permissions",
                item.contains("player.mayInteract(level, position)")
                        && item.contains("player.mayUseItemAt(position, context.getClickedFace(), context.getItemInHand())"));
        yes("capture has no raw level reads", !tool.contains("level.getBlockState("));
        yes("capture and stamp never inspect block entity payloads",
                !item.contains("getBlockEntity(") && !tool.contains("getBlockEntity("));
        yes("no loader-specific API enters these common paths",
                !item.contains("net.minecraftforge") && !tool.contains("net.minecraftforge"));
    }

    private static void malformed(String label, ItemStack carrier) {
        CompoundTag before = carrier.getTag().copy();
        eq(label, BlueprintLogic.Failure.MALFORMED, BlueprintItem.decode(carrier).failure());
        eq("invalid decoding is read-only", before, carrier.getTag());
        throwsType("the compatibility reader does not silently hide malformed data",
                IllegalArgumentException.class, () -> BlueprintItem.read(carrier));
        Site untouched = new Site();
        eq("malformed data cannot produce a stamp", BlueprintLogic.Failure.MALFORMED,
                failure(BlueprintItem.preflight(BlueprintItem.decode(carrier), ORIGIN, untouched,
                        Map.of(), true).diagnosis()));
        eq("malformed data never reads the destination", 0, untouched.reads);
    }

    private static void pieceFailure(String label, BlueprintLogic.Failure failure, BlueprintLogic.Piece piece) {
        Site site = new Site();
        eq(label, failure, failure(plan(List.of(piece), site, Map.of(), true).diagnosis()));
        eq("invalid registry/state facts never inspect a chunk", 0, site.reads);
    }

    private static BlueprintLogic.Failure failure(BlueprintLogic.Plan plan) {
        return plan.problems().isEmpty() ? BlueprintLogic.Failure.NONE : plan.problems().get(0).failure();
    }

    private static ItemStack carrier(List<BlueprintLogic.Piece> pieces) {
        ItemStack carrier = new ItemStack(Items.PAPER);
        BlueprintItem.write(carrier, pieces);
        return carrier;
    }

    private static BlueprintItem.StampPlan plan(List<BlueprintLogic.Piece> pieces, Site site,
                                                Map<String, Integer> have, boolean creative) {
        return BlueprintItem.preflight(BlueprintItem.decode(carrier(pieces)), ORIGIN, site, have, creative);
    }

    private static List<BlueprintLogic.Piece> stoneLine(int count) {
        List<BlueprintLogic.Piece> pieces = new ArrayList<>();
        for (int x = 0; x < count; x++) {
            pieces.add(new BlueprintLogic.Piece(x, 0, 0, STONE, STONE, ""));
        }
        return pieces;
    }

    private static List<BlueprintLogic.Piece> cube(int count) {
        List<BlueprintLogic.Piece> pieces = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            pieces.add(new BlueprintLogic.Piece(index % 32, index / 1024, (index / 32) % 32, STONE, STONE, ""));
        }
        return pieces;
    }

    private static Inventory stock(int count) {
        Inventory inventory = new Inventory(null);
        if (count > 0) {
            inventory.items.set(0, new ItemStack(Items.STONE, count));
        }
        return inventory;
    }

    private static int total(List<ItemStack> stacks) {
        return stacks.stream().mapToInt(ItemStack::getCount).sum();
    }

    private static class Site implements BlueprintItem.WorldView {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        final Set<BlockPos> outside = new HashSet<>();
        final Set<BlockPos> unloaded = new HashSet<>();
        final Set<BlockPos> forbidden = new HashSet<>();
        BlockState defaultState = Blocks.AIR.defaultBlockState();
        int reads;

        @Override
        public boolean inBounds(BlockPos position) {
            return !outside.contains(position);
        }

        @Override
        public boolean loaded(BlockPos position) {
            return !unloaded.contains(position);
        }

        @Override
        public boolean mayBuild(BlockPos position) {
            return !forbidden.contains(position);
        }

        @Override
        public BlockState state(BlockPos position) {
            if (outside.contains(position) || unloaded.contains(position)) {
                throw new AssertionError("preflight read an unavailable cell " + position);
            }
            reads++;
            return blocks.getOrDefault(position, defaultState);
        }
    }

    private static void throwsType(String what, Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
            fail(what + ": expected " + expected.getSimpleName());
        } catch (RuntimeException actual) {
            yes(what, expected.isInstance(actual));
        }
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void eq(String what, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
