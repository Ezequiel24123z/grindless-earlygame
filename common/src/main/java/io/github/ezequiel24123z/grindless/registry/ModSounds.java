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
 * <p>Working beds are noticed physical imports; Quern A and Mixer A are unchanged.
 * Legacy identities remain registered with silent hums or native physical interaction aliases.
 * Event names must match {@code sounds.json}, which {@code VerifyAssets} checks.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Grindless.MOD_ID, Registries.SOUND_EVENT);

    /** Retired generic bed; kept silent for event identity compatibility. */
    public static final RegistrySupplier<SoundEvent> MACHINE_HUM = sound("machine_hum");
    /** Retired generic bed; kept silent for event identity compatibility. */
    public static final RegistrySupplier<SoundEvent> MACHINE_HUM_HEAVY = sound("machine_hum_heavy");
    /** The approved stone/granular working bed, currently used only by the Quern. */
    public static final RegistrySupplier<SoundEvent> T0_GRINDING = sound("t0_grinding");
    /** The wet working bed, currently used only by the Clay Mixer. */
    public static final RegistrySupplier<SoundEvent> T0_WET_WORK = sound("t0_wet_work");
    /** Native furnace crackle, shared only by actual T0 burning mechanisms. */
    public static final RegistrySupplier<SoundEvent> T0_FIRE_DRAFT = sound("t0_fire_draft");
    /** Small contained water movement, not viscous mixing or passive stored fluid. */
    public static final RegistrySupplier<SoundEvent> T0_WATER_FLOW = sound("t0_water_flow");
    /** Compact mineral-working head, distinct from stone-disc milling. */
    public static final RegistrySupplier<SoundEvent> T0_MINERAL_EXTRACTION = sound("t0_mineral_extraction");
    /** Explicit successful link action, using an installed native physical attachment sound. */
    public static final RegistrySupplier<SoundEvent> PYLON_LINK = sound("pylon_link");
    /** A native physical latch for explicit switching, never continuous generator ambience. */
    public static final RegistrySupplier<SoundEvent> RELAY_CLICK = sound("relay_click");
    /** Existing exceptional grid-shortage feedback, using a physical trip rather than a tone. */
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
