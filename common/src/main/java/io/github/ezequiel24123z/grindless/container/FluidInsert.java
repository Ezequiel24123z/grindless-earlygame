package io.github.ezequiel24123z.grindless.container;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.fluid.FluidEndpoint;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.fluid.NativeFluidStore;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Pushes a fluid through one face into a neighbour that implements {@link FluidEndpoint}.
 */
public final class FluidInsert {

    private FluidInsert() {
    }

    public static FluidState intoNeighbour(Level level, BlockPos from, Direction side, FluidState state) {
        if (state == null || state.isEmpty()) {
            return FluidState.EMPTY;
        }
        BlockPos position = from.relative(side);
        if (!level.hasChunkAt(position)) {
            return state;
        }
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity instanceof FluidEndpoint endpoint) {
            return endpoint.insert(side.getOpposite(), state);
        }
        return state;
    }

    /** One target face, resolved again at commit rather than trusting a cached block entity. */
    public record Source(Direction face, Supplier<FluidEndpoint> lookup) {
        public Source {
            Objects.requireNonNull(face);
            Objects.requireNonNull(lookup);
        }

        private NativeFluidStore.Access access() {
            FluidEndpoint endpoint = lookup.get();
            NativeFluidStore store = endpoint == null ? null : endpoint.nativeStore();
            if (store == null) {
                return null;
            }
            long revision = endpoint.nativeAccessRevision();
            return new NativeFluidStore.Access(store, () -> lookup.get() == endpoint
                    && endpoint.nativeStore() == store && endpoint.nativeAccessRevision() == revision
                    && endpoint.canExtract(face)
                    && visibleNativeState(endpoint.contents(), store.contents()),
                    () -> endpoint.contents().millibuckets());
        }
    }

    public static List<Source> neighbours(Level level, BlockPos from) {
        if (level == null) {
            return List.of();
        }
        List<Source> sources = new ArrayList<>();
        for (Direction side : Direction.values()) {
            BlockPos position = from.relative(side);
            sources.add(new Source(side.getOpposite(), () -> {
                if (!level.hasChunkAt(position)) {
                    return null;
                }
                BlockEntity entity = level.getBlockEntity(position);
                return entity instanceof FluidEndpoint endpoint ? endpoint : null;
            }));
        }
        return List.copyOf(sources);
    }

    /** The same native identity and face filter used by planning and commit, with aliases removed. */
    public static List<FluidState> available(List<Source> sources) {
        IdentityHashMap<NativeFluidStore, Integer> seen = new IdentityHashMap<>();
        List<FluidState> states = new ArrayList<>();
        for (Source source : sources) {
            NativeFluidStore.Access access = source.access();
            if (access != null && access.allowed().getAsBoolean()) {
                FluidState visible = access.store().contents().withAmount(Math.min(
                        access.store().contents().millibuckets(), Math.max(0, access.limit().getAsInt())));
                Integer index = seen.get(access.store());
                if (index == null) {
                    seen.put(access.store(), states.size());
                    states.add(visible);
                } else if (states.get(index).millibuckets() < visible.millibuckets()) {
                    states.set(index, visible);
                }
            }
        }
        return states;
    }

    private static boolean visibleNativeState(FluidState visible, FluidState full) {
        return !visible.isEmpty() && visible.millibuckets() <= full.millibuckets()
                && visible.withAmount(full.millibuckets()).equals(full);
    }

    /** Exact bounded transfer between the two already-owned native stores, never an external fallback. */
    public static int moveNative(Source source, Source destination, int maximum) {
        if (maximum <= 0) {
            return 0;
        }
        NativeFluidStore.Access access = source.access();
        FluidEndpoint target = destination.lookup().get();
        NativeFluidStore targetStore = target == null ? null : target.nativeStore();
        if (access == null || targetStore == null || access.store() == targetStore
                || !access.allowed().getAsBoolean()) {
            return 0;
        }
        FluidState offered = access.store().contents().withAmount(Math.min(maximum,
                Math.max(0, access.limit().getAsInt())));
        int accepted = target.acceptedAmount(destination.face(), offered);
        FluidState transfer = offered.withAmount(Math.min(offered.millibuckets(), accepted));
        if (transfer.isEmpty()) {
            return 0;
        }
        return NativeFluidStore.transfer(access, targetStore, transfer,
                () -> destination.lookup().get() == target && target.nativeStore() == targetStore
                        && target.acceptedAmount(destination.face(), transfer) >= transfer.millibuckets())
                ? transfer.millibuckets() : 0;
    }

    public static Optional<NativeFluidStore.Batch> plan(NativeFluidStore primary,
                                                       List<IngredientSpec> inputs, List<Source> sources) {
        List<NativeFluidStore.Request> requests = new ArrayList<>();
        List<NativeFluidStore.Access> neighbours = accesses(sources);
        for (int i = 0; i < inputs.size(); i++) {
            IngredientSpec input = inputs.get(i);
            List<NativeFluidStore.Access> candidates = i == 0
                    ? List.of(new NativeFluidStore.Access(primary, () -> true)) : neighbours;
            requests.add(new NativeFluidStore.Request(input.id(), input.count(), candidates));
        }
        return NativeFluidStore.plan(primary, requests);
    }

    private static List<NativeFluidStore.Access> accesses(List<Source> sources) {
        List<NativeFluidStore.Access> accesses = new ArrayList<>();
        for (Source source : sources) {
            NativeFluidStore.Access access = source.access();
            if (access != null) {
                accesses.add(access);
            }
        }
        return accesses;
    }

    /** Takes the exact amount from native neighbours, or nothing; never calls external handlers. */
    public static int takeFromNeighbours(Level level, BlockPos from, String id, int millibuckets) {
        if (level == null || id == null || id.isBlank() || millibuckets <= 0) {
            return 0;
        }
        Optional<NativeFluidStore.Batch> plan = NativeFluidStore.plan(List.of(
                new NativeFluidStore.Request(id, millibuckets, accesses(neighbours(level, from)))));
        if (plan.isEmpty()) {
            return 0;
        }
        if (!plan.get().commit()) {
            Grindless.LOG.warn("Native fluid sources changed during withdrawal of {} mB {} at {}; "
                    + "tentative withdrawals were restored", millibuckets, id, from);
            return 0;
        }
        return millibuckets;
    }
}
