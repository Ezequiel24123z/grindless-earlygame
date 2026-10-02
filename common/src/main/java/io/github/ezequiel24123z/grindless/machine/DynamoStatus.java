package io.github.ezequiel24123z.grindless.machine;

/** The Hand Crank Dynamo's status as a pure function of its state, so it can be checked alone. */
final class DynamoStatus {

    /** Pushes that moved nothing before a charged dynamo is shown as blocked: one is a blip, two is a fact. */
    static final int BLOCKED_AFTER = 2;

    private DynamoStatus() {
    }

    static MachineStatus of(long stored, int fruitlessPushes) {
        if (stored <= 0L) {
            return MachineStatus.IDLE;
        }
        return fruitlessPushes >= BLOCKED_AFTER ? MachineStatus.BLOCKED : MachineStatus.RUNNING;
    }
}
