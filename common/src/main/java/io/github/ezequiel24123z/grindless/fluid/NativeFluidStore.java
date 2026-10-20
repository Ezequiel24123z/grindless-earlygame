package io.github.ezequiel24123z.grindless.fluid;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Owner-controlled, synchronous fluid withdrawals (ADR-0138). This is not a handler adapter.
 * Snapshots and restoration stay private; external insertion permissions are never involved.
 */
public final class NativeFluidStore {

    private final FluidBuffer buffer;
    private final IntSupplier outputQuota;
    private final IntConsumer restoreQuota;
    private final Runnable changed;

    public NativeFluidStore(FluidBuffer buffer, Runnable changed) {
        this(buffer, () -> 0, ignored -> { }, changed);
    }

    /** Quota accessors only read or assign owner accounting; they must never notify listeners. */
    public NativeFluidStore(FluidBuffer buffer, IntSupplier outputQuota, IntConsumer restoreQuota,
                            Runnable changed) {
        this.buffer = Objects.requireNonNull(buffer);
        this.outputQuota = Objects.requireNonNull(outputQuota);
        this.restoreQuota = Objects.requireNonNull(restoreQuota);
        this.changed = Objects.requireNonNull(changed);
        buffer.bindOwner(this);
    }

    public FluidState contents() {
        return buffer.state();
    }

    /** A native store reached through one still-valid endpoint identity and face; the check is read-only. */
    public record Access(NativeFluidStore store, BooleanSupplier allowed, IntSupplier limit) {
        public Access {
            Objects.requireNonNull(store);
            Objects.requireNonNull(allowed);
            Objects.requireNonNull(limit);
        }

        /** Existing full-store endpoints keep their original unbounded-by-role access. */
        public Access(NativeFluidStore store, BooleanSupplier allowed) {
            this(store, allowed, () -> store.contents().millibuckets());
        }
    }

    public record Request(String id, int millibuckets, List<Access> sources) {
        public Request {
            if (id == null || id.isBlank() || millibuckets <= 0) {
                throw new IllegalArgumentException("a withdrawal needs a fluid and positive volume");
            }
            sources = List.copyOf(sources);
        }
    }

    /** Moves an exact native amount with the same receipt/compensation and deferred notifications. */
    public static boolean transfer(Access source, NativeFluidStore destination, FluidState state,
                                   BooleanSupplier accepts) {
        if (source.store() == destination || state == null || state.isEmpty() || !accepts.getAsBoolean()
                || !source.store().contents().equals(state.withAmount(source.store().contents().millibuckets()))) {
            return false;
        }
        Access guarded = new Access(source.store(),
                () -> source.allowed().getAsBoolean() && accepts.getAsBoolean(), source.limit());
        Optional<Batch> plan = plan(destination, List.of(
                new Request(state.id(), state.millibuckets(), List.of(guarded))));
        return plan.isPresent() && plan.get().commit(new Settlement() {
            @Override
            public void apply() {
                if (!accepts.getAsBoolean() || !destination.buffer.offer(state).isEmpty()) {
                    throw new IllegalStateException("Reserved native fluid transfer no longer fits its destination");
                }
            }

            @Override
            public void restore() {
                // Both stores and their quotas are already owned by the native receipts.
            }
        });
    }

    /** Consumer-owned mutations and their silent, complete compensation. */
    public interface Settlement {
        void apply();

        void restore();
    }

    private static final Settlement NO_SETTLEMENT = new Settlement() {
        @Override
        public void apply() {
        }

        @Override
        public void restore() {
        }
    };

    /** Plans without mutation, counting a backing store only once across faces and requests. */
    public static Optional<Batch> plan(List<Request> requests) {
        return plan(null, requests);
    }

    /** Also retains the consumer's native state, even when it has no fluid input to withdraw. */
    public static Optional<Batch> plan(NativeFluidStore consumer, List<Request> requests) {
        Map<NativeFluidStore, Receipt> byStore = new IdentityHashMap<>();
        List<Receipt> receipts = new ArrayList<>();
        if (consumer != null) {
            Receipt receipt = new Receipt(consumer, consumer.snapshot());
            byStore.put(consumer, receipt);
            receipts.add(receipt);
        }
        for (Request request : requests) {
            int remaining = request.millibuckets();
            for (Access access : request.sources()) {
                if (remaining == 0) {
                    break;
                }
                if (!access.allowed().getAsBoolean()) {
                    continue;
                }
                NativeFluidStore store = access.store();
                Receipt receipt = byStore.get(store);
                Snapshot snapshot = receipt == null ? store.snapshot() : receipt.before;
                if (!snapshot.state().is(request.id())) {
                    continue;
                }
                if (!snapshot.equals(store.snapshot())) {
                    return Optional.empty();
                }
                int reserved = receipt == null ? 0 : receipt.amount;
                int accessible = Math.min(snapshot.state().millibuckets(), Math.max(0, access.limit().getAsInt()));
                int amount = Math.min(remaining, accessible - reserved);
                if (amount <= 0) {
                    continue;
                }
                if (receipt == null) {
                    receipt = new Receipt(store, snapshot);
                    byStore.put(store, receipt);
                    receipts.add(receipt);
                }
                receipt.amount += amount;
                receipt.accesses.add(access);
                remaining -= amount;
            }
            if (remaining != 0) {
                return Optional.empty();
            }
        }
        return Optional.of(new Batch(receipts));
    }

    private Snapshot snapshot() {
        return new Snapshot(buffer.state(), outputQuota.getAsInt());
    }

    private void restore(Snapshot snapshot) {
        buffer.set(snapshot.state());
        restoreQuota.accept(snapshot.quota());
    }

    private record Snapshot(FluidState state, int quota) {
    }

    private static final class Receipt {
        private final NativeFluidStore store;
        private final Snapshot before;
        private final List<Access> accesses = new ArrayList<>();
        private int amount;

        private Receipt(NativeFluidStore store, Snapshot before) {
            this.store = store;
            this.before = before;
        }

        private boolean valid() {
            int accessible = 0;
            for (Access access : accesses) {
                if (!access.allowed().getAsBoolean()) {
                    return false;
                }
                accessible = Math.max(accessible, Math.max(0, access.limit().getAsInt()));
            }
            return amount <= accessible && before.equals(store.snapshot());
        }

        private boolean withdraw() {
            if (amount == 0) {
                return true;
            }
            FluidState taken = store.buffer.extract(before.state().id(), amount);
            if (!taken.equals(before.state().withAmount(amount))) {
                return false;
            }
            store.restoreQuota.accept(Math.max(0, before.quota() - amount));
            return true;
        }
    }

    public static final class Batch {
        private final List<Receipt> receipts;
        private boolean attempted;

        private Batch(List<Receipt> receipts) {
            this.receipts = List.copyOf(receipts);
        }

        /** A withdrawal-only batch has no non-native consumer state to compensate. */
        public boolean commit() {
            return commit(NO_SETTLEMENT);
        }

        /**
         * Rechecks every source, then withdraws and settles silently. A rejection or synchronous
         * runtime exception compensates every attempted withdrawal and any consumer mutation.
         * Restoration must not notify; unexpected failures retain their original exception and
         * any compensation failures as suppressed diagnostics. Fatal VM errors are not caught.
         * Notifications run only after settlement succeeds and are outside the tentative phase.
         * This single-use operation runs on the server thread, never across ticks or a world crash.
         */
        public boolean commit(Settlement settlement) {
            Objects.requireNonNull(settlement);
            if (attempted) {
                throw new IllegalStateException("a native fluid batch is single-use");
            }
            attempted = true;
            int touched = 0;
            boolean settling = false;
            boolean accepted = true;
            try {
                for (Receipt receipt : receipts) {
                    if (!receipt.valid()) {
                        return false;
                    }
                }
                for (Receipt receipt : receipts) {
                    if (!receipt.valid()) {
                        accepted = false;
                        break;
                    }
                    touched++;
                    if (!receipt.withdraw()) {
                        accepted = false;
                        break;
                    }
                }
                if (accepted) {
                    settling = true;
                    settlement.apply();
                }
            } catch (RuntimeException failure) {
                compensate(touched, settling ? settlement : null, failure);
                throw failure;
            }
            if (!accepted) {
                RuntimeException failure = compensate(touched, null, null);
                if (failure != null) {
                    throw failure;
                }
                return false;
            }
            for (Receipt receipt : receipts) {
                receipt.store.changed.run();
            }
            return true;
        }

        private RuntimeException compensate(int touched, Settlement settlement, RuntimeException failure) {
            if (settlement != null) {
                try {
                    settlement.restore();
                } catch (RuntimeException restorationFailure) {
                    failure = retain(failure, restorationFailure);
                }
            }
            for (int i = touched - 1; i >= 0; i--) {
                Receipt receipt = receipts.get(i);
                try {
                    receipt.store.restore(receipt.before);
                } catch (RuntimeException restorationFailure) {
                    failure = retain(failure, restorationFailure);
                }
            }
            return failure;
        }

        private static RuntimeException retain(RuntimeException original, RuntimeException restorationFailure) {
            if (original == null) {
                return restorationFailure;
            }
            if (original != restorationFailure) {
                original.addSuppressed(restorationFailure);
            }
            return original;
        }
    }
}
