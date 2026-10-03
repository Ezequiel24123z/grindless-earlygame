package io.github.ezequiel24123z.grindless.energy;

import net.minecraft.nbt.CompoundTag;

/**
 * The ordinary {@link FluxStorage} implementation: a fixed-capacity buffer with independent
 * insert and extract rate limits.
 *
 * <p>Separate rate limits are what let one class cover every role the design needs. A generator is
 * extract-only ({@code maxReceive} zero), a machine buffer is insert-only, and a battery is both
 * with different rates — so no subclass hierarchy is required.
 *
 * <p>Not thread-safe, and deliberately so: everything here runs on the server thread.
 */
public class SimpleFluxStorage implements FluxStorage {

    private static final String KEY_STORED = "FluxStored";

    private final long capacity;
    private final long maxReceive;
    private final long maxExtract;
    private final Runnable onChanged;

    private long stored;

    /** A buffer with the same insert and extract limit and no change callback. */
    public SimpleFluxStorage(long capacity, long maxTransfer) {
        this(capacity, maxTransfer, maxTransfer, null);
    }

    /**
     * @param onChanged run whenever the contents change, typically {@code BlockEntity::setChanged};
     *                  may be {@code null}
     */
    public SimpleFluxStorage(long capacity, long maxReceive, long maxExtract, Runnable onChanged) {
        this.capacity = Math.max(0L, capacity);
        this.maxReceive = Math.max(0L, maxReceive);
        this.maxExtract = Math.max(0L, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    public long receive(long maxReceive, boolean simulate) {
        if (!canReceive() || maxReceive <= 0L) {
            return 0L;
        }
        long accepted = Math.min(Math.min(maxReceive, this.maxReceive), getSpace());
        if (accepted > 0L && !simulate) {
            stored += accepted;
            markChanged();
        }
        return accepted;
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0L) {
            return 0L;
        }
        long removed = Math.min(Math.min(maxExtract, this.maxExtract), stored);
        if (removed > 0L && !simulate) {
            stored -= removed;
            markChanged();
        }
        return removed;
    }

    @Override
    public long getStored() {
        return stored;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0L;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0L;
    }

    /**
     * Sets the contents directly, clamped to capacity.
     *
     * <p>For world generation, creative tools and tests — not for transfer, which must go through
     * {@link #receive} and {@link #extract} so the rate limits apply.
     */
    public void setStored(long amount) {
        long clamped = Math.max(0L, Math.min(amount, capacity));
        if (clamped != stored) {
            stored = clamped;
            markChanged();
        }
    }

    /** Writes the contents into {@code tag}. Capacity and limits are code, not save data. */
    public CompoundTag save(CompoundTag tag) {
        tag.putLong(KEY_STORED, stored);
        return tag;
    }

    /**
     * Reads the contents from {@code tag}, clamping to capacity.
     *
     * <p>Clamping matters on load: a config change or a mod update can shrink a buffer that was
     * saved full, and silently keeping the larger value would duplicate energy.
     */
    public void load(CompoundTag tag) {
        stored = Math.max(0L, Math.min(tag.getLong(KEY_STORED), capacity));
    }

    private void markChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
