package io.github.ezequiel24123z.grindless.client;

import io.github.ezequiel24123z.grindless.guidance.ResourceGuidanceContext;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestTeaching;
import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ThermalLogic;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.network.PylonTier;
import io.github.ezequiel24123z.grindless.progression.T1Progression;
import io.github.ezequiel24123z.grindless.source.SourceLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortContract;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortRole;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Kind;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Resource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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
        boolean t1 = QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1;
        if (t1) {
            text.add(Component.translatable("gui.grindless.quest.t1_development"));
        }
        text.add(task.id().equals("biomass_dynamo")
                ? Component.translatable(QuestTeaching.bodyKey(task), FluxTier.F0.nominal(), SourceLogic.BIOMASS_BURN_TICKS)
                : task.id().equals("manual_work")
                ? Component.translatable(QuestTeaching.bodyKey(task), SourceLogic.MANUAL_CHARGE, SourceLogic.MANUAL_COOLDOWN)
                : task.id().equals("thermal")
                ? Component.translatable(QuestTeaching.bodyKey(task), ThermalLogic.FU_PER_TICK)
                : task.id().equals("first_grid")
                ? Component.translatable(QuestTeaching.bodyKey(task), FluxTier.F0.nominal(), FluxTier.F1.nominal())
                : Component.translatable(QuestTeaching.bodyKey(task)));
        text.add(Component.translatable("gui.grindless.quest.inventory_not_operation"));
        if (task.id().equals("pylon")) {
            text.add(Component.translatable("gui.grindless.quest.t1_pylon_limits",
                    PylonTier.MK1.supplyArea(), PylonTier.MK1.linkRange(), PylonTier.MK1.throughput()));
        }
        if (task.id().equals("fluid_logistics")) {
            text.add(Component.translatable("gui.grindless.quest.tank_containers", FluidLogic.TANK_CAPACITY));
        }
        var structure = QuestTeaching.structure(task);
        if (!structure.cells().isEmpty()) {
            text.add(Component.translatable("gui.grindless.quest.structure_axes"));
            for (var cell : structure.cells()) {
                text.add(Component.translatable("gui.grindless.quest.structure_cell",
                        cell.offset().x(), cell.offset().y(), cell.offset().z(),
                        ResourceGuidanceClient.name(new Resource(Kind.ITEM, cell.block())),
                        cell.oriented() ? Component.translatable("gui.grindless.quest.same_facing") : Component.empty()));
            }
            appendPorts(text, structure);
        }
        List<AtlasLogic.Entry> entries = AtlasLogic.allEntries(ProcessLookup.recipes());
        List<AtlasLogic.Entry> recipes = QuestTeaching.recipes(task, entries);
        if (t1) {
            for (String route : T1Progression.atlasForQuest(task.id(), entries).nonAtlasRouteIds().stream().sorted().toList()) {
                text.add(Component.translatable("gui.grindless.quest.t1_non_atlas_route", route));
            }
        }
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

    private static void appendPorts(List<Component> text, WorkshopStructure structure) {
        List<WorkshopPortContract.Slot> slots = WorkshopPortContract.slots(structure);
        if (slots.isEmpty()) {
            return;
        }
        text.add(Component.translatable("gui.grindless.quest.ports_limit", WorkshopPortContract.maximumPorts(structure)));
        int bricks = WorkshopPortContract.minimumBricks(structure);
        if (bricks > 0) {
            text.add(Component.translatable("gui.grindless.quest.ports_bricks", bricks));
        }
        for (WorkshopPortRole role : WorkshopPortRole.values()) {
            int maximum = WorkshopPortContract.maximum(structure, role);
            if (maximum > 0) {
                text.add(Component.translatable("gui.grindless.quest.ports_role_limit",
                        portRole(role), maximum));
            }
        }
        List<String> faceNames = List.of("north", "east", "south", "west");
        for (WorkshopPortContract.Slot slot : slots) {
            MutableComponent roles = Component.empty();
            for (WorkshopPortRole role : slot.roles().stream().sorted().toList()) {
                if (!roles.getString().isEmpty()) {
                    roles.append(" / ");
                }
                roles.append(portRole(role));
            }
            MutableComponent faces = Component.empty();
            for (int face : slot.faces().stream().sorted().toList()) {
                if (!faces.getString().isEmpty()) {
                    faces.append(" / ");
                }
                faces.append(Component.translatable("gui.grindless.quest.port_face." + faceNames.get(face)));
            }
            text.add(Component.translatable("gui.grindless.quest.port_slot", slot.offset().x(), slot.offset().y(),
                    slot.offset().z(), roles, faces));
        }
        text.add(Component.translatable("gui.grindless.quest.ports_notes"));
    }

    private static Component portRole(WorkshopPortRole role) {
        return Component.translatable("gui.grindless.quest.port_role." + role.name().toLowerCase(Locale.ROOT));
    }
}
