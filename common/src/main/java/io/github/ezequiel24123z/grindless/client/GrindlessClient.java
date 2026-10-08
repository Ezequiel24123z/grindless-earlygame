package io.github.ezequiel24123z.grindless.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import io.github.ezequiel24123z.grindless.registry.ModEntities;
import io.github.ezequiel24123z.grindless.registry.ModMenus;
import io.github.ezequiel24123z.grindless.vein.SurveySync;

/**
 * Client-only setup. Screens, belt rendering and S2C receivers bind here so a dedicated
 * server never loads them.
 */
public final class GrindlessClient {

    private GrindlessClient() {
    }

    public static void init() {
        SurveySync.register();
        MenuRegistry.registerScreenFactory(ModMenus.PROCESS_MACHINE.get(), ProcessMachineScreen::new);
        MenuRegistry.registerScreenFactory(ModMenus.PROCESS_ATLAS.get(), ProcessAtlasScreen::new);
        MenuRegistry.registerScreenFactory(ModMenus.LANDING_MAP.get(), LandingMapScreen::new);
        MenuRegistry.registerScreenFactory(ModMenus.QUEST_BOOK.get(), QuestBookScreen::new);
        MenuRegistry.registerScreenFactory(ModMenus.FIELD_GUIDE.get(), FieldGuideScreen::new);
        EntityRendererRegistry.register(ModEntities.SURVEY_ROCKET, SurveyRocketRenderer::new);
        EntityRendererRegistry.register(ModEntities.SUPRALUMINAL_STATION, SupraluminalStationRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.CONVEYOR_BELT.get(), BeltRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.FLUX_BELT.get(), BeltRenderer::new);
        ClientGuiEvent.RENDER_HUD.register(SurveyOverlay::render);
    }
}
