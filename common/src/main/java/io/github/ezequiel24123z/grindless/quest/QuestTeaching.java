package io.github.ezequiel24123z.grindless.quest;

import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.progression.T1Progression;
import io.github.ezequiel24123z.grindless.structure.WorkshopStructure;
import io.github.ezequiel24123z.grindless.structure.WorkshopPortContract;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/** Lesson selection only; recipe quantities, conditions and outputs belong to the live Atlas. */
public final class QuestTeaching {

    public enum Path {
        STARTER, RECOVERY, CORE, RENEWABLE, OPTIONAL, PREVIEW, T1_CORE, T1_OPTIONAL, T1_EXIT
    }

    private QuestTeaching() {
    }

    public static String bodyKey(QuestCatalogue.Task task) {
        if (QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1) {
            return "gui.grindless.quest.lesson.t1." + T1Progression.byQuest(task.id()).id() + "." + task.id();
        }
        return "gui.grindless.quest.lesson." + (task.id().equals("settling") ? "settling_ports" : task.id());
    }

    public static WorkshopStructure structure(QuestCatalogue.Task task) {
        return switch (task.id()) {
            case "firing" -> WorkshopStructure.forFamily(MachineFamily.FIRING_CHAMBER);
            case "settling" -> WorkshopStructure.forFamily(MachineFamily.SETTLING_TROUGH);
            default -> WorkshopStructure.NONE;
        };
    }

    public static Path path(QuestCatalogue.Task task) {
        if (QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1) {
            return switch (T1Progression.byQuest(task.id()).requirement()) {
                case MANDATORY -> Path.T1_CORE;
                case OPTIONAL -> Path.T1_OPTIONAL;
                case EXIT -> Path.T1_EXIT;
            };
        }
        return switch (task.id()) {
            case "starter_supplies" -> Path.STARTER;
            case "quest_book_help", "calibrated_core" -> Path.RECOVERY;
            case "crank", "extractor", "fluid_logistics", "conveyors", "workshop_flow" -> Path.OPTIONAL;
            case "geological_collector", "aggregate", "biocultivator", "biomass", "fibreboard",
                    "biomass_dynamo", "mineral_feed" -> Path.RENEWABLE;
            default -> Path.CORE;
        };
    }

    public static boolean optional(QuestCatalogue.Task task) {
        Path path = path(task);
        return path == Path.OPTIONAL || path == Path.T1_OPTIONAL;
    }

    public static List<AtlasLogic.Entry> recipes(QuestCatalogue.Task task, List<AtlasLogic.Entry> entries) {
        if (QuestCatalogue.chapter(task) == QuestCatalogue.Chapter.T1) {
            return T1Progression.atlasForQuest(task.id(), entries).entries();
        }
        Set<String> ids = switch (task.id()) {
            case "quest_book_help" -> Set.of("craft/quest_book", "bootstrap/paper", "bootstrap/sticks");
            case "construction" -> Set.of("bench/crafting_table", "bench/furnace",
                    "bench/chest", "bench/brick_block", "bootstrap/dry_aggregate", "bootstrap/bricks");
            case "multitool" -> Set.of("craft/multitool");
            case "manual_work" -> Set.of("craft/crank_handle");
            case "crank" -> Set.of("craft/hand_crank_dynamo");
            case "extractor" -> Set.of("craft/crude_extractor");
            case "water" -> Set.of("craft/hand_pump", "craft/clay_conduit");
            case "quern" -> Set.of("craft/quern", "bootstrap/sand", "bootstrap/dry_aggregate");
            case "clay_mixer" -> Set.of("craft/clay_mixer", "bootstrap/sand_body", "bootstrap/grog_body");
            case "grog" -> Set.of("bootstrap/brick_grog", "bootstrap/insulator_grog",
                    "bootstrap/unfired_recovery", "bootstrap/grog_body", "bootstrap/insulators");
            case "firing" -> Set.of("craft/firing_chamber");
            case "ceramics" -> Set.of("bootstrap/sand_body", "bootstrap/grog_body", "bootstrap/insulators");
            case "geological_collector" -> Set.of("craft/geological_collector", "source/geological_aggregate");
            case "aggregate" -> Set.of("source/geological_aggregate", "bootstrap/dry_aggregate",
                    "bootstrap/wet_aggregate");
            case "settling" -> Set.of("craft/settling_trough", "craft/settling_channel", "craft/settling_screen");
            case "water_recovery" -> Set.of("bootstrap/settle_clay", "bootstrap/wet_aggregate",
                    "bootstrap/flint_separation");
            case "mineral_feed" -> Set.of("bootstrap/wet_aggregate", "bootstrap/reduce_iron",
                    "bootstrap/reduce_copper", "bootstrap/glass");
            case "biocultivator" -> Set.of("craft/biocultivator", "source/biomass");
            case "biomass" -> Set.of("source/biomass", "bootstrap/fibreboard", "bootstrap/paper",
                    "bootstrap/biocarbon");
            case "fibreboard" -> Set.of("bootstrap/fibreboard", "bootstrap/paper", "bootstrap/sticks",
                    "bootstrap/biocarbon");
            case "biomass_dynamo" -> Set.of("craft/biomass_dynamo", "source/biomass");
            case "fluid_logistics" -> Set.of("craft/basic_tank", "craft/clay_conduit", "craft/hand_pump");
            case "conveyors" -> Set.of("craft/conveyor_belt");
            case "workshop_flow" -> Set.of("craft/crude_manipulator", "craft/conveyor_belt");
            case "assembly_bench" -> Set.of("craft/assembly_bench", "bench/data_core", "bench/crank_handle",
                    "bench/hand_pump", "bench/biocultivator");
            case "research_terminal" -> Set.of("craft/research_terminal", "calibrate/data_core");
            case "data_core" -> Set.of("craft/data_core", "bench/data_core");
            case "calibrate", "calibrated_core" -> Set.of("craft/data_core", "calibrate/data_core");
            case "relay" -> Set.of("craft/relay_matrix", "calibrate/data_core");
            default -> Set.of();
        };
        Set<String> portCrafts = new HashSet<>();
        for (WorkshopPortContract.Kind kind : WorkshopPortContract.Kind.values()) {
            if (kind.structure() == structure(task)) {
                portCrafts.add("craft/" + kind.blockName());
            }
        }
        return entries.stream().filter(entry -> ids.contains(entry.id()) || portCrafts.contains(entry.id())
                || task.id().equals("firing") && entry.family() == MachineFamily.FIRING_CHAMBER).toList();
    }

    /** Shared, lossless paging for native quest help, including long translations. */
    public static <T> List<List<T>> paginate(List<T> lines, int linesPerPage) {
        return GuideCatalogue.paginate(lines, linesPerPage);
    }
}
