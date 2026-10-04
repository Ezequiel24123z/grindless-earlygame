package io.github.ezequiel24123z.grindless.item;

/**
 * One-shot flag for a Multitool relocate (ADR-0069).
 *
 * <p>{@code onRemove} drops inventories when a machine is broken. Relocate must not: the
 * contents are already in {@code BlockEntityTag}. Callers wrap {@code removeBlock} in
 * {@link #run(Runnable)}.
 */
public final class Relocation {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private Relocation() {
    }

    public static boolean active() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }

    public static void run(Runnable action) {
        ACTIVE.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            ACTIVE.remove();
        }
    }
}
