package io.github.ezequiel24123z.grindless.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.flight.SurveyRocket;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entities. The survey rocket is the only one: a seat that climbs, not a mob (ADR-0097).
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Grindless.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<SurveyRocket>> SURVEY_ROCKET = ENTITIES.register(
            "survey_rocket",
            () -> EntityType.Builder.<SurveyRocket>of(SurveyRocket::new, MobCategory.MISC)
                    .sized(0.9F, 0.6F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .build("survey_rocket"));

    private ModEntities() {
    }

    public static void register() {
        ENTITIES.register();
    }
}
