package io.github.ezequiel24123z.grindless.belt;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;

/**
 * Crude Manipulator numbers, independent of a world.
 *
 * <p>Unpowered, one item a second. The first inserter has to work the moment the first belt
 * does, or the factory still ends at a hopper line.
 */
public final class ManipulatorLogic {

    /** One item per second. */
    public static final int CYCLE_TICKS = 20;

    /** Crude moves a single item. Stack comes later. */
    public static final int STACK = 1;

    private ManipulatorLogic() {
    }

    /**
     * What the arm should show.
     *
     * <p>No work in reach is idle. Holding or transferring is running. The Crude arm has no
     * power draw, so it is never starved.
     */
    public static MachineStatus status(boolean hasWork, boolean transferring) {
        return transferring || hasWork ? MachineStatus.RUNNING : MachineStatus.IDLE;
    }

    /** Whether {@code progress} has reached a transfer. */
    public static boolean ready(int progress) {
        return progress >= CYCLE_TICKS;
    }
}
