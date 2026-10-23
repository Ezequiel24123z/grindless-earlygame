package io.github.ezequiel24123z.grindless.quest;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The original quest book (ADR-0100).
 *
 * <p>T0 teaching and in-development T1 objectives share one per-player claim ledger. A chapter
 * unlock is navigation, never a machine permission or evidence of successful operation.
 */
public final class QuestCatalogue {

    public enum Chapter {
        T0, T1
    }

    public enum Type {
        REWARD, LESSON
    }

    public enum Evidence {
        /** The named item is in the player's inventory. */
        ITEM,
        /** The player is standing in the named dimension. */
        DIMENSION,
        /** A named Control Matrix item carries the required physical rating. */
        MATRIX,
        /** An explicit free claim, with no item, location or payment objective. */
        FREE
    }

    /** An existing item, given once. Not a new material. */
    public record Reward(String itemId, int count) {
    }

    /**
     * One task.
     *
     * @param id       stable id, saved on the player
     * @param line     the quest line it belongs to
     * @param evidence what the game already knows how to see
     * @param subject  an item id or a dimension id; empty for a free claim
     * @param count    how many of an item; ignored for dimension and free evidence
     * @param matrixRating required physical rating for Matrix evidence; null otherwise
     * @param requires tasks that must already be claimed
     * @param rewards  legal item stacks given together; empty for a lesson
     * @param type     material reward claim or explicitly rewardless teaching checklist
     */
    public record Task(
            String id,
            String line,
            Evidence evidence,
            String subject,
            int count,
            FluxTier matrixRating,
            List<String> requires,
            List<Reward> rewards,
            Type type) {
        public Task {
            requires = List.copyOf(requires);
            rewards = List.copyOf(rewards);
        }

        public Task(String id, String line, Evidence evidence, String subject, int count,
                    FluxTier matrixRating, List<String> requires, List<Reward> rewards) {
            this(id, line, evidence, subject, count, matrixRating, requires, rewards, Type.REWARD);
        }

        public Task(String id, String line, Evidence evidence, String subject, int count,
                    FluxTier matrixRating, List<String> requires, Reward reward) {
            this(id, line, evidence, subject, count, matrixRating, requires, List.of(reward));
        }

        /** Compatibility for existing single-stack task consumers. Use rewards() for delivery. */
        public Reward reward() {
            if (rewards.isEmpty()) {
                throw new IllegalStateException("This lesson has no material reward: " + id);
            }
            return rewards.get(0);
        }
    }

    public static final String STARTER_SUPPLIES = "starter_supplies";
    public static final String T0_COMPLETION = "relay";

    private static final List<Task> TASKS = List.of(
            new Task(STARTER_SUPPLIES, "bootstrap", Evidence.FREE, "", 1, null, List.of(), List.of(
                    reward("minecraft:cobblestone", 64),
                    reward("minecraft:cobblestone", 64),
                    reward("minecraft:oak_log", 64),
                    reward("minecraft:bread", 64),
                    reward("minecraft:brick", 64),
                    reward("minecraft:clay_ball", 64),
                    reward("minecraft:sand", 64),
                    reward("minecraft:charcoal", 32),
                    reward("minecraft:iron_ingot", 16),
                    reward("minecraft:water_bucket", 1),
                    reward("minecraft:water_bucket", 1))),
            lesson("quest_book_help", "bootstrap", Evidence.FREE, "", 1, List.of()),
            lesson("superflat", "bootstrap", Evidence.FREE, "", 1, List.of("quest_book_help")),
            lesson("construction", "bootstrap", Evidence.ITEM, "minecraft:furnace", 1, List.of()),
            task("multitool", "bootstrap", Evidence.ITEM, "grindless:multitool", 1,
                    List.of(), reward("minecraft:clay_ball", 4)),
            lesson("manual_work", "bootstrap", Evidence.ITEM, "grindless:crank_handle", 1,
                    List.of("multitool")),
            task("crank", "bootstrap", Evidence.ITEM, "grindless:hand_crank_dynamo", 1,
                    List.of("multitool"), reward("minecraft:coal", 4)),
            task("extractor", "bootstrap", Evidence.ITEM, "grindless:crude_extractor", 1,
                    List.of("crank"), reward("minecraft:gravel", 8)),
            task("quern", "workshop", Evidence.ITEM, "grindless:quern", 1,
                    List.of("multitool"), reward("minecraft:brick", 4)),
            task("water", "workshop", Evidence.ITEM, "grindless:hand_pump", 1,
                    List.of("quern"), reward("minecraft:stick", 4)),
            task("clay_mixer", "workshop", Evidence.ITEM, "grindless:clay_mixer", 1,
                    List.of("quern", "water"), reward("minecraft:clay_ball", 8)),
            lesson("grog", "workshop", Evidence.ITEM, "grindless:ceramic_grog", 1,
                    List.of("clay_mixer")),
            lesson("firing", "workshop", Evidence.ITEM, "grindless:firing_chamber", 1,
                    List.of("clay_mixer")),
            task("ceramics", "workshop", Evidence.ITEM, "grindless:ceramic_insulator", 2,
                    List.of("clay_mixer"), reward("minecraft:charcoal", 4)),
            lesson("geological_collector", "renewable_supply", Evidence.ITEM, "grindless:geological_collector", 1,
                    List.of("manual_work", "water")),
            lesson("aggregate", "renewable_supply", Evidence.ITEM, "grindless:mineral_aggregate", 1,
                    List.of("geological_collector")),
            task("settling", "settling", Evidence.ITEM, "grindless:settling_trough", 1,
                    List.of("ceramics", "water"), reward("minecraft:dirt", 16)),
            lesson("water_recovery", "settling", Evidence.FREE, "", 1, List.of("settling")),
            lesson("mineral_feed", "settling", Evidence.FREE, "", 1,
                    List.of("aggregate", "settling")),
            lesson("biocultivator", "bioculture", Evidence.ITEM, "grindless:biocultivator", 1,
                    List.of("aggregate")),
            lesson("biomass", "bioculture", Evidence.ITEM, "grindless:biomass", 1,
                    List.of("biocultivator")),
            lesson("fibreboard", "bioculture", Evidence.ITEM, "grindless:fibreboard", 1,
                    List.of("biomass", "clay_mixer")),
            lesson("biomass_dynamo", "bioculture", Evidence.ITEM, "grindless:biomass_dynamo", 1,
                    List.of("biomass", "mineral_feed")),
            lesson("fluid_logistics", "workshop_logistics", Evidence.ITEM, "grindless:basic_tank", 1,
                    List.of("ceramics", "mineral_feed")),
            lesson("conveyors", "workshop_logistics", Evidence.ITEM, "grindless:conveyor_belt", 8,
                    List.of("ceramics")),
            task("workshop_flow", "workshop_logistics", Evidence.ITEM, "grindless:crude_manipulator", 1,
                    List.of("ceramics"), reward("minecraft:stick", 8)),
            lesson("assembly_bench", "relay", Evidence.ITEM, "grindless:assembly_bench", 1,
                    List.of("fibreboard")),
            lesson("research_terminal", "relay", Evidence.ITEM, "grindless:research_terminal", 1,
                    List.of("biomass_dynamo")),
            lesson("data_core", "relay", Evidence.ITEM, "grindless:data_core", 1,
                    List.of("ceramics", "mineral_feed")),
            task("calibrate", "relay", Evidence.ITEM, "grindless:calibrated_data_core", 1,
                    List.of("ceramics"), reward("grindless:process_atlas", 1)),
            lesson("calibrated_core", "relay", Evidence.FREE, "", 1, List.of("calibrate")),
            matrixTask("relay", "relay", "grindless:relay_matrix", FluxTier.F1, 5,
                    List.of("calibrate"), reward("minecraft:flint", 2)),
            task("thermal", "power", Evidence.ITEM, "grindless:thermal_generator", 1,
                    List.of("relay"), reward("minecraft:coal", 16)),
            task("pylon", "power", Evidence.ITEM, "grindless:flux_pylon_mk1", 1,
                    List.of("thermal"), reward("minecraft:redstone", 4)),
            lesson("first_grid", "power", Evidence.FREE, "", 1, List.of("pylon")),
            task("scanner", "extraction", Evidence.ITEM, "grindless:prospectors_scanner", 1,
                    List.of("pylon"), reward("minecraft:charcoal", 4)),
            task("terrestrial", "extraction", Evidence.ITEM, "grindless:terrestrial_extractor", 1,
                    List.of("scanner", "thermal"), reward("grindless:data_core", 1)),
            task("pulverizer", "factory", Evidence.ITEM, "grindless:pulverizer", 1,
                    List.of("thermal"), reward("minecraft:cobblestone", 32)),
            task("furnace", "factory", Evidence.ITEM, "grindless:arc_furnace", 1,
                    List.of("thermal", "pulverizer"), reward("minecraft:coal", 8)),
            task("press", "factory", Evidence.ITEM, "grindless:press", 1,
                    List.of("furnace"), reward("minecraft:iron_ingot", 4)),
            lesson("forming_dies", "factory", Evidence.FREE, "", 1, List.of("press")),
            lesson("copper_coil", "factory", Evidence.ITEM, "grindless:copper_coil", 1,
                    List.of("forming_dies")),
            task("assembler", "factory", Evidence.ITEM, "grindless:assembler", 1,
                    List.of("press", "furnace"), reward("grindless:data_core", 1)),
            task("matrix_line", "factory", Evidence.ITEM, "grindless:relay_matrix", 5,
                    List.of("assembler"), reward("minecraft:redstone", 8)),
            task("glass", "renewables", Evidence.ITEM, "minecraft:glass", 8,
                    List.of("pulverizer", "furnace"), reward("minecraft:clay_ball", 16)),
            task("logistics", "logistics", Evidence.ITEM, "grindless:conveyor_belt", 8,
                    List.of("assembler"), reward("minecraft:hopper", 1)),
            task("kiln", "logistics", Evidence.ITEM, "grindless:kiln", 1,
                    List.of("matrix_line"), reward("minecraft:brick", 8)),
            task("factory_world", "factory_world", Evidence.ITEM, "grindless:factory_portal", 1,
                    List.of("matrix_line", "glass"), reward("minecraft:grass_block", 16)),
            lesson("flux_cell", "industrial", Evidence.ITEM, "grindless:flux_cell", 1,
                    List.of("matrix_line")),
            matrixTask("industrial_matrix", "industrial", "grindless:relay_matrix", FluxTier.F2, 1,
                    List.of("matrix_line", "glass"), reward("minecraft:iron_ingot", 8)),
            lesson("wet_beneficiation", "wet_beneficiation", Evidence.ITEM, "grindless:chemical_washer", 1,
                    List.of("assembler")),
            lesson("gas_utilities", "gas_utilities", Evidence.ITEM, "grindless:electrolysis_cell", 1,
                    List.of("assembler")));

    private QuestCatalogue() {
    }

    public static List<Task> tasks() {
        return TASKS;
    }

    public static Task byId(String id) {
        for (Task task : TASKS) {
            if (task.id().equals(id)) {
                return task;
            }
        }
        return null;
    }

    public static Chapter chapter(Task task) {
        return switch (task.id()) {
            case "thermal", "pylon", "scanner", "terrestrial", "pulverizer", "furnace", "press",
                    "assembler", "matrix_line", "glass", "logistics", "kiln", "factory_world",
                    "industrial_matrix", "first_grid", "forming_dies", "copper_coil", "flux_cell",
                    "wet_beneficiation", "gas_utilities" -> Chapter.T1;
            default -> Chapter.T0;
        };
    }

    public static boolean unlocked(Chapter chapter, Set<String> claimed) {
        return chapter == Chapter.T0 || claimed.contains(T0_COMPLETION);
    }

    /** Explicit chapter gate also protects retained T1 claims with incomplete old prerequisite sets. */
    public static List<String> requirements(Task task) {
        if (chapter(task) != Chapter.T1 || task.requires().contains(T0_COMPLETION)) {
            return task.requires();
        }
        List<String> required = new ArrayList<>(task.requires());
        required.add(T0_COMPLETION);
        return List.copyOf(required);
    }

    public static List<String> lines(Chapter chapter) {
        return TASKS.stream().filter(task -> chapter(task) == chapter).map(Task::line).distinct().toList();
    }

    /** Quest lines in the order a reader meets them. */
    public static List<String> lines() {
        List<String> lines = new ArrayList<>();
        for (Task task : TASKS) {
            if (!lines.contains(task.line())) {
                lines.add(task.line());
            }
        }
        return List.copyOf(lines);
    }

    private static Task task(String id, String line, Evidence evidence, String subject, int count,
                             List<String> requires, Reward reward) {
        return new Task(id, line, evidence, subject, count, null, List.copyOf(requires), reward);
    }

    private static Task lesson(String id, String line, Evidence evidence, String subject, int count,
                               List<String> requires) {
        return new Task(id, line, evidence, subject, count, null, List.copyOf(requires), List.of(), Type.LESSON);
    }

    private static Task matrixTask(String id, String line, String subject, FluxTier rating, int count,
                                   List<String> requires, Reward reward) {
        return new Task(id, line, Evidence.MATRIX, subject, count, rating, List.copyOf(requires), reward);
    }

    private static Reward reward(String itemId, int count) {
        return new Reward(itemId, count);
    }
}
