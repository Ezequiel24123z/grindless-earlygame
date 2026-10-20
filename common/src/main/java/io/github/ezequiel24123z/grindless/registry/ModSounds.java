package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events. Each is mono, so Minecraft can place it in 3D: a stereo file is played flat, with
 * no direction and no distance falloff, which defeats the point of hearing a machine across a
 * room (ADR-0048).
 *
 * <p>Legacy files are generated and converted by {@code tools/convert-audio.sh}; the approved
 * working beds are noticed imports; Quern A is unchanged (ADR-0148). Event names must match {@code sounds.json},
 * which {@code VerifyAssets} checks.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Grindless.MOD_ID, Registries.SOUND_EVENT);

    /** A light machine at work. */
    public static final RegistrySupplier<SoundEvent> MACHINE_HUM = sound("machine_hum");
    /** A heavy machine at work. */
    public static final RegistrySupplier<SoundEvent> MACHINE_HUM_HEAVY = sound("machine_hum_heavy");
    /** The approved stone/granular working bed, currently used only by the Quern. */
    public static final RegistrySupplier<SoundEvent> T0_GRINDING = sound("t0_grinding");
    /** The wet working bed, currently used only by the Clay Mixer. */
    public static final RegistrySupplier<SoundEvent> T0_WET_WORK = sound("t0_wet_work");
    /** A pylon carrying load. */
    public static final RegistrySupplier<SoundEvent> PYLON_LINK = sound("pylon_link");
    /** A relay, for anything that switches. */
    public static final RegistrySupplier<SoundEvent> RELAY_CLICK = sound("relay_click");
    /** A grid that cannot meet demand. */
    public static final RegistrySupplier<SoundEvent> BROWNOUT_ALARM = sound("brownout_alarm");

    private ModSounds() {
    }

    private static RegistrySupplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Grindless.id(name)));
    }

    public static void register() {
        SOUNDS.register();
    }
}
