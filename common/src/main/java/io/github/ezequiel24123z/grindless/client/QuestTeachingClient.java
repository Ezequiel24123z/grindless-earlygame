package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceContext;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestTeaching;
import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Kind;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Resource;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The same concise lesson and live recipe projection in the native book and Heracles. */
public final class QuestTeachingClient {

    private QuestTeachingClient() {
    }

    public static List<Component> content(QuestCatalogue.Task task) {
        List<Component> text = new ArrayList<>();
        text.add(Component.translatable("gui.grindless.quest.path."
                + QuestTeaching.path(task).name().toLowerCase(Locale.ROOT)));
        text.add(task.id().equals("biomass_dynamo")
                ? Component.translatable(QuestTeaching.bodyKey(task), FluxTier.F0.nominal(), SourceLogic.BIOMASS_BURN_TICKS)
                : task.id().equals("manual_work")
                ? Component.translatable(QuestTeaching.bodyKey(task), SourceLogic.MANUAL_CHARGE, SourceLogic.MANUAL_COOLDOWN)
                : Component.translatable(QuestTeaching.bodyKey(task)));
        text.add(Component.translatable("gui.grindless.quest.inventory_not_operation"));
        var structure = QuestTeaching.structure(task);
        if (!structure.cells().isEmpty()) {
            text.add(Component.translatable("gui.grindless.quest.structure_axes"));
            for (var cell : structure.cells()) {
                text.add(Component.translatable("gui.grindless.quest.structure_cell",
                        cell.offset().x(), cell.offset().y(), cell.offset().z(),
                        ResourceGuidanceClient.name(new Resource(Kind.ITEM, cell.block())),
                        cell.oriented() ? Component.translatable("gui.grindless.quest.same_facing") : Component.empty()));
            }
        }
        List<AtlasLogic.Entry> recipes = QuestTeaching.recipes(task, AtlasLogic.allEntries(ProcessLookup.recipes()));
        if (!recipes.isEmpty()) {
            text.add(Component.translatable("gui.grindless.quest.live_recipes"));
            Set<Resource> outputs = new LinkedHashSet<>();
            for (AtlasLogic.Entry entry : recipes) {
                text.add(Component.literal(AtlasLogic.line(entry)));
                ResourceGuidanceContext.operands(entry).stream()
                        .filter(operand -> operand.role() == ResourceGuidanceContext.Role.OUTPUT)
                        .map(ResourceGuidanceContext.Operand::resource).forEach(outputs::add);
            }
            for (Resource output : outputs) {
                var context = ResourceGuidanceClient.playerContext(output);
                if (!context.isEmpty()) {
                    text.add(Component.translatable("gui.grindless.quest.resource_context",
                            ResourceGuidanceClient.name(output)));
                    text.addAll(context);
                }
            }
        }
        return List.copyOf(text);
    }
}
