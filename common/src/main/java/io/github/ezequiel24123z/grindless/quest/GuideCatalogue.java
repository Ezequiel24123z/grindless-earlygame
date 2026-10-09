package io.github.ezequiel24123z.grindless.quest;

import java.util.List;

/**
 * The in-game guide (ADR-0100).
 *
 * <p>Pages of the T0-to-T1 survival route the quest book tracks.
 * Readable whether or not a task is claimed. Not advancements, not a config screen, and
 * not a translation: the English strings live in {@code en_us}.
 */
public final class GuideCatalogue {

    private static final List<String> PAGES = List.of(
            "bootstrap", "relay", "power", "extraction", "factory", "renewables", "logistics", "factory_world");

    private GuideCatalogue() {
    }

    public static List<String> pages() {
        return PAGES;
    }

    public static String titleKey(String page) {
        return "gui.grindless.guide." + page + ".title";
    }

    public static String bodyKey(String page) {
        return "gui.grindless.guide." + page + ".body";
    }
}
