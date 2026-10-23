package io.github.ezequiel24123z.grindless.quest;

import java.util.ArrayList;
import java.util.List;

/**
 * Retained legacy Guide keys and the shared lossless pager.
 *
 * <p>Gameplay teaching now belongs to {@link QuestTeaching}. Legacy key identities remain
 * available for compatibility; no current Guide screen reads these route pages.
 */
public final class GuideCatalogue {

    private static final List<String> PAGES = List.of(
            "bootstrap", "workshop", "settling", "relay", "power", "extraction", "factory", "renewables", "logistics", "factory_world",
            "industrial");

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

    /** Keeps every wrapped line, including longer translations, instead of clipping a topic. */
    public static <T> List<List<T>> paginate(List<T> lines, int linesPerPage) {
        if (linesPerPage < 1) {
            throw new IllegalArgumentException("a guide page must fit at least one line");
        }
        if (lines.isEmpty()) {
            return List.of(List.of());
        }
        List<List<T>> result = new ArrayList<>();
        for (int start = 0; start < lines.size(); start += linesPerPage) {
            result.add(List.copyOf(lines.subList(start, Math.min(start + linesPerPage, lines.size()))));
        }
        return List.copyOf(result);
    }
}
