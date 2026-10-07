package io.github.ezequiel24123z.grindless.quest;

import io.github.ezequiel24123z.grindless.research.Blueprint;

import java.util.ArrayList;
import java.util.List;

/**
 * The original quest book (ADR-0100).
 *
 * <p>Lines, tasks, dependencies and rewards for the currently reachable survival route:
 * T0 through electronic silicon at T2. The book does not gate a machine or expose the
 * registered spatial prototypes before their campaign tiers.
 */
public final class QuestCatalogue {

    public enum Evidence {
        /** The named item is in the player's inventory. */
        ITEM,
        /** The named blueprint is unlocked on the world. */
        RESEARCH,
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
     * @param subject  an item id, a blueprint id, or a dimension id
     * @param count    how many of an item; ignored for the other evidence
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
            task("voltaic", "voltaic", Evidence.RESEARCH, Blueprint.VOLTAIC.id(), 1,
                    List.of("extractor"), reward("grindless:process_atlas", 1)),
            task("furnace", "voltaic", Evidence.ITEM, "grindless:arc_furnace", 1,
                    List.of("voltaic"), reward("minecraft:coal", 8)),
            task("industrial", "contact", Evidence.RESEARCH, Blueprint.INDUSTRIAL.id(), 1,
                    List.of("voltaic"), reward("grindless:data_core", 1)),
            task("reactor", "contact", Evidence.ITEM, "grindless:chemical_reactor", 1,
                    List.of("industrial"), reward("grindless:vanadia_pellet", 1)),
            task("steel", "metals", Evidence.ITEM, "grindless:steel_ingot", 1,
                    List.of("furnace"), reward("minecraft:coal", 8)),
            task("brick", "metals", Evidence.ITEM, "grindless:refractory_brick", 1,
                    List.of("steel"), reward("grindless:data_core", 1)),
            task("silicon", "metals", Evidence.ITEM, "grindless:metallurgical_silicon", 1,
                    List.of("steel"), reward("minecraft:coal", 4)),
            task("electronic", "metals", Evidence.ITEM, "grindless:electronic_silicon", 1,
                    List.of("silicon"), reward("grindless:data_core", 1)));

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
