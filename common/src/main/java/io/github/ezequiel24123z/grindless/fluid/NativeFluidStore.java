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

    /** A native store reached through one still-valid endpoint identity and face. */
    public record Access(NativeFluidStore store, BooleanSupplier allowed) {
        public Access {
            Objects.requireNonNull(store);
            Objects.requireNonNull(allowed);
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

    /** Plans without mutation, counting a backing store only once across faces and requests. */
    public static Optional<Batch> plan(List<Request> requests) {
        Map<NativeFluidStore, Receipt> byStore = new IdentityHashMap<>();
        List<Receipt> receipts = new ArrayList<>();
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
                int amount = Math.min(remaining, snapshot.state().millibuckets() - reserved);
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
            for (Access access : accesses) {
                if (!access.allowed().getAsBoolean()) {
                    return false;
                }
            }
            return before.equals(store.snapshot());
        }

        private boolean withdraw() {
            FluidState taken = store.buffer.extract(before.state().id(), amount);
            if (!taken.equals(before.state().withAmount(amount))) {
                store.restore(before);
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

        /**
         * Rechecks every source, then withdraws silently. A late failure restores earlier owners
         * exactly. Settlement must finish the consumer's own mutations before any notification.
         * This single-use operation runs on the server thread, never across ticks or a world crash.
         */
        public boolean commit(Runnable settle) {
            Objects.requireNonNull(settle);
            if (attempted) {
                throw new IllegalStateException("a native fluid batch is single-use");
            }
            attempted = true;
            for (Receipt receipt : receipts) {
                if (!receipt.valid()) {
                    return false;
                }
            }
            int withdrawn = 0;
            for (Receipt receipt : receipts) {
                if (!receipt.valid() || !receipt.withdraw()) {
                    for (int i = withdrawn - 1; i >= 0; i--) {
                        Receipt previous = receipts.get(i);
                        previous.store.restore(previous.before);
                    }
                    return false;
                }
                withdrawn++;
            }
            settle.run();
            for (Receipt receipt : receipts) {
                receipt.store.changed.run();
            }
            return true;
        }
    }
}
