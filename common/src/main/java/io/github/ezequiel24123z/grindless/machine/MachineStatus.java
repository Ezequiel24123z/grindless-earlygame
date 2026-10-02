package io.github.ezequiel24123z.grindless.machine;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Locale;
import java.util.Optional;

/**
 * What a machine is doing, as the player sees it.
 *
 * <p>These are the five statuses the machine anatomy promises (MACHINES.md): "my factory stopped
 * and I do not know why" is the commonest failure in a complex pack, so a machine says <em>which
 * kind</em> of stopped it is. {@link #BLOCKED} (cannot output), {@link #STARVED} (cannot get
 * input or power) and {@link #OUT_OF_BAND} (conditions outside the envelope) are different faults
 * with different fixes, and each has its own look, sound and particle.
 *
 * <p>The status is a block state property rather than block entity data, because the model, the
 * light level and the client-side effects all key off it and a block entity is not available to
 * the model baker.
 */
public enum MachineStatus implements StringRepresentable {

    /** Nothing to do. The default, and the cheapest to render. */
    IDLE(0),
    /** Working normally. */
    RUNNING(8),
    /** Has output it cannot place anywhere. */
    BLOCKED(4),
    /** Wants input or power it is not getting. */
    STARVED(3),
    /** Running conditions are outside the envelope. */
    OUT_OF_BAND(5);

    /** The block-state property name every machine uses. */
    public static final String PROPERTY = "status";

    private final int light;

    MachineStatus(int light) {
        this.light = light;
    }

    /** Block light this status emits. A running machine lights its surroundings; an idle one does not. */
    public int lightLevel() {
        return light;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MachineStatus parse(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }

    /** The status a block state carries, if it has one. */
    public static Optional<MachineStatus> of(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(PROPERTY) && property instanceof EnumProperty<?> enumProperty
                    && enumProperty.getValueClass() == MachineStatus.class) {
                return Optional.of((MachineStatus) state.getValue(enumProperty));
            }
        }
        return Optional.empty();
    }

    /**
     * The state with its status replaced, or the same state if it has no status property or does
     * not allow that value. A pylon has no {@code BLOCKED}; asking for it is not an error, it is
     * simply not representable.
     */
    @SuppressWarnings("unchecked")
    public static BlockState with(BlockState state, MachineStatus status) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(PROPERTY) && property instanceof EnumProperty<?> enumProperty
                    && enumProperty.getValueClass() == MachineStatus.class
                    && enumProperty.getPossibleValues().contains(status)) {
                return state.setValue((EnumProperty<MachineStatus>) enumProperty, status);
            }
        }
        return state;
    }

    /** Light level for a block state, for {@code BlockBehaviour.Properties#lightLevel}. */
    public static int lightOf(BlockState state) {
        return of(state).map(MachineStatus::lightLevel).orElse(0);
    }
}
