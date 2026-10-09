package io.github.ezequiel24123z.grindless.quest;

import java.util.ArrayList;
import java.util.List;

/**
 * The original quest book (ADR-0100).
 *
 * <p>Lines, tasks, dependencies and rewards for the T0-to-T1 survival route. The book does
 * not gate a machine or expose registered future-tier prototypes before their campaign tiers.
 */
public final class QuestCatalogue {

    public enum Evidence {
        /** The named item is in the player's inventory. */
        ITEM,
        /** The player is standing in the named dimension. */
        DIMENSION
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
     * @param subject  an item id or a dimension id
     * @param count    how many of an item; ignored for dimension evidence
     * @param requires tasks that must already be claimed
     * @param reward   given on the first successful claim, and not consumed from the evidence
     */
    public record Task(
            String id,
            String line,
            Evidence evidence,
            String subject,
            int count,
            List<String> requires,
            Reward reward) {
    }

    private static final List<Task> TASKS = List.of(
            task("multitool", "bootstrap", Evidence.ITEM, "grindless:multitool", 1,
                    List.of(), reward("grindless:data_core", 1)),
            task("crank", "bootstrap", Evidence.ITEM, "grindless:hand_crank_dynamo", 1,
                    List.of("multitool"), reward("minecraft:coal", 4)),
            task("extractor", "bootstrap", Evidence.ITEM, "grindless:crude_extractor", 1,
                    List.of("crank"), reward("grindless:data_core", 1)),
            task("calibrate", "relay", Evidence.ITEM, "grindless:calibrated_data_core", 1,
                    List.of("extractor"), reward("grindless:process_atlas", 1)),
            task("relay", "relay", Evidence.ITEM, "grindless:relay_matrix", 5,
                    List.of("calibrate"), reward("minecraft:flint", 2)),
            task("thermal", "power", Evidence.ITEM, "grindless:thermal_generator", 1,
                    List.of("relay"), reward("minecraft:coal", 16)),
            task("pylon", "power", Evidence.ITEM, "grindless:flux_pylon_mk1", 1,
                    List.of("thermal"), reward("minecraft:redstone", 4)),
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
            task("assembler", "factory", Evidence.ITEM, "grindless:assembler", 1,
                    List.of("press", "furnace"), reward("grindless:data_core", 1)),
            task("matrix_line", "factory", Evidence.ITEM, "grindless:relay_matrix", 5,
                    List.of("assembler"), reward("minecraft:redstone", 8)),
            task("glass", "renewables", Evidence.ITEM, "minecraft:glass", 8,
                    List.of("pulverizer", "furnace"), reward("minecraft:clay_ball", 16)),
            task("water", "renewables", Evidence.ITEM, "grindless:hand_pump", 1,
                    List.of("thermal"), reward("minecraft:bucket", 1)),
            task("logistics", "logistics", Evidence.ITEM, "grindless:conveyor_belt", 8,
                    List.of("assembler"), reward("minecraft:hopper", 1)),
            task("kiln", "logistics", Evidence.ITEM, "grindless:kiln", 1,
                    List.of("matrix_line"), reward("minecraft:brick", 8)),
            task("factory_world", "factory_world", Evidence.ITEM, "grindless:factory_portal", 1,
                    List.of("matrix_line", "glass"), reward("minecraft:grass_block", 16)));

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
        return new Task(id, line, evidence, subject, count, List.copyOf(requires), reward);
    }

    private static Reward reward(String itemId, int count) {
        return new Reward(itemId, count);
    }
}
