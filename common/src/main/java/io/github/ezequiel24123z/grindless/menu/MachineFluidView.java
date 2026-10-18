package io.github.ezequiel24123z.grindless.menu;

/** A read-only primary store; its output quota is included in amount, never added. */
public record MachineFluidView(String id, int amount, int capacity, int outputQuota) {

    public MachineFluidView {
        if (id == null || id.isBlank() || amount < 0 || capacity <= 0 || outputQuota < 0) {
            throw new IllegalArgumentException("a fluid view needs an ID, nonnegative volumes and positive capacity");
        }
    }

    public int fillHeight(int height) {
        if (height < 0) {
            throw new IllegalArgumentException("fluid view height cannot be negative");
        }
        if (height == 0 || amount == 0) {
            return 0;
        }
        return (int) Math.min(height, (height * (long) amount + capacity - 1) / capacity);
    }
}
