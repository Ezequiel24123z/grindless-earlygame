package io.github.ezequiel24123z.grindless.fluid;

import net.minecraft.nbt.CompoundTag;

/** NBT for a {@link FluidState}. Kept off the record so the record stays Minecraft-free. */
public final class FluidNbt {

    private static final String KEY_ID = "Id";
    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_TEMP = "Temp";
    private static final String KEY_PRESSURE = "Pressure";

    private FluidNbt() {
    }

    public static CompoundTag save(FluidState state) {
        CompoundTag tag = new CompoundTag();
        if (state != null && !state.isEmpty()) {
            tag.putString(KEY_ID, state.id());
            tag.putInt(KEY_AMOUNT, state.millibuckets());
            tag.putDouble(KEY_TEMP, state.temperatureC());
            tag.putDouble(KEY_PRESSURE, state.pressureMPa());
        }
        return tag;
    }

    public static FluidState load(CompoundTag tag) {
        if (tag == null || !tag.contains(KEY_ID) || tag.getInt(KEY_AMOUNT) <= 0) {
            return FluidState.EMPTY;
        }
        return FluidState.of(tag.getString(KEY_ID), tag.getInt(KEY_AMOUNT),
                tag.contains(KEY_TEMP) ? tag.getDouble(KEY_TEMP) : FluidLogic.AMBIENT_C,
                tag.contains(KEY_PRESSURE) ? tag.getDouble(KEY_PRESSURE) : FluidLogic.AMBIENT_MPA);
    }
}
