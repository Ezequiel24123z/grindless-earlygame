package io.github.ezequiel24123z.grindless.progression;

import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The first Voltaic factory, expressed as references to existing authorities.
 * Recipe batches, energy, durations, catalysts and physical ratings remain in their catalogues.
 * This is teaching and audit data, never a world permission or an additional recipe engine.
 */
public final class T1Progression {
    public static final String ENTRY_ROUTE = "craft/relay_matrix";
    public static final String REPRODUCTION_ROUTE = "assemble/relay_matrix";
    public static final String EXIT_ROUTE = "assemble/relay_matrix_f2";
    public static final String POWER_ROUTE = "fuel/thermal_generator/biomass";

    public enum Requirement { MANDATORY, OPTIONAL, EXIT }

    /** Dependencies name other lines, not quest claims or runtime unlock flags. */
    public record Line(String id, Requirement requirement, List<String> routeIds, Set<String> requires) {
        public Line {
            Objects.requireNonNull(id, "line id");
            Objects.requireNonNull(requirement, "line requirement");
            routeIds = List.copyOf(routeIds);
            requires = Set.copyOf(requires);
            if (id.isBlank() || routeIds.isEmpty() || routeIds.stream().anyMatch(String::isBlank)
                    || Set.copyOf(routeIds).size() != routeIds.size() || requires.contains(id)) {
                throw new IllegalArgumentException("Invalid T1 line: " + id);
            }
        }

        public boolean mandatory() {
            return requirement != Requirement.OPTIONAL;
        }
    }

    /** Fuel authorities are not handheld Atlas rows. */
    public record AtlasSelection(List<AtlasLogic.Entry> entries, Set<String> nonAtlasRouteIds) {
        public AtlasSelection {
            entries = List.copyOf(entries);
            nonAtlasRouteIds = Set.copyOf(nonAtlasRouteIds);
        }
    }

    private static final List<Line> LINES = List.of(
            new Line("power", Requirement.MANDATORY,
                    List.of("craft/thermal_generator", "craft/flux_pylon_mk1", POWER_ROUTE), Set.of()),
            new Line("milling", Requirement.MANDATORY,
                    List.of("craft/pulverizer", "b1/iron", "b1/copper"), Set.of("power")),
            new Line("reduction", Requirement.MANDATORY,
                    List.of("craft/arc_furnace", "b1_r1/iron", "b1_r1/copper"), Set.of("milling")),
            new Line("glass", Requirement.MANDATORY,
                    List.of("renew/sand", "renew/glass"), Set.of("reduction")),
            new Line("forming", Requirement.MANDATORY,
                    List.of("craft/press", "craft/plate_die", "craft/rod_die", "craft/gear_die",
                            "craft/coil_die", "press/plate/iron", "press/rod/iron", "press/gear/iron",
                            "press/coil/copper", "craft/machine_casing"), Set.of("reduction")),
            new Line("assembly", Requirement.MANDATORY,
                    List.of("craft/assembler", REPRODUCTION_ROUTE), Set.of("forming", "glass")),
            new Line("relay_f2", Requirement.EXIT,
                    List.of("craft/flux_cell", EXIT_ROUTE), Set.of("assembly")),
            new Line("extraction", Requirement.OPTIONAL,
                    List.of("craft/terrestrial_extractor", "craft/prospectors_scanner"), Set.of("power")),
            new Line("logistics", Requirement.OPTIONAL,
                    List.of("craft/conveyor_belt", "craft/splitter", "craft/merger", "craft/tunnel_belt",
                            "craft/overflow_gate", "craft/sorter"), Set.of("forming")),
            new Line("storage", Requirement.OPTIONAL,
                    List.of("craft/flux_conduit", "craft/capacitor_bank", "craft/flux_transformer"), Set.of("power")),
            new Line("kiln", Requirement.OPTIONAL,
                    List.of("craft/kiln", "roast/iron", "roast/copper", "r2/iron", "r2/copper"), Set.of("power")),
            new Line("factory_space", Requirement.OPTIONAL,
                    List.of("craft/factory_portal"), Set.of("assembly")));

    private T1Progression() {
    }

    /** Shared by runtime teaching and source-bound exports; the order is the teaching order. */
    public static List<Line> lines() {
        return LINES;
    }

    /** Rejects a stale teaching reference rather than silently substituting a different line. */
    public static Line byId(String id) {
        return LINES.stream().filter(line -> line.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown T1 line: " + id));
    }

    /** Task identities stay in the quest catalogue; this mapping selects their production line. */
    public static Line byQuest(String taskId) {
        return byId(switch (taskId) {
            case "thermal", "pylon", "first_grid" -> "power";
            case "pulverizer" -> "milling";
            case "furnace" -> "reduction";
            case "press", "forming_dies", "copper_coil" -> "forming";
            case "assembler", "matrix_line" -> "assembly";
            case "glass" -> "glass";
            case "industrial_matrix", "flux_cell" -> "relay_f2";
            case "scanner", "terrestrial" -> "extraction";
            case "logistics" -> "logistics";
            case "kiln" -> "kiln";
            case "factory_world" -> "factory_space";
            default -> throw new IllegalArgumentException("Unknown T1 teaching task: " + taskId);
        });
    }

    /** Selects actual authority identities, never formatted quantities or copied recipe batches. */
    public static List<String> questRouteIds(String taskId) {
        return switch (taskId) {
            case "thermal" -> List.of("craft/thermal_generator", POWER_ROUTE);
            case "pylon" -> List.of("craft/flux_pylon_mk1");
            case "scanner" -> List.of("craft/prospectors_scanner");
            case "terrestrial" -> List.of("craft/terrestrial_extractor");
            case "press" -> List.of("craft/press");
            case "forming_dies" -> List.of("craft/plate_die", "craft/rod_die", "craft/gear_die",
                    "press/plate/iron", "press/rod/iron", "press/gear/iron", "craft/machine_casing");
            case "copper_coil" -> List.of("craft/coil_die", "press/coil/copper");
            case "assembler" -> List.of("craft/assembler");
            case "matrix_line" -> List.of("bench/data_core", "calibrate/data_core", REPRODUCTION_ROUTE);
            case "flux_cell" -> List.of("craft/flux_cell");
            case "glass" -> List.of("source/geological_aggregate", "bootstrap/dry_aggregate",
                    "bootstrap/wet_aggregate", "source/biomass", "bootstrap/biocarbon",
                    "bootstrap/reduce_iron", "bootstrap/reduce_copper", "craft/hand_pump",
                    "renew/sand", "renew/glass");
            default -> byQuest(taskId).routeIds();
        };
    }

    /**
     * Missing expected Atlas rows fail visibly. The known non-Atlas fuel authority is returned
     * separately so a renderer cannot confuse an absent row with an explanation of that recipe.
     */
    public static AtlasSelection atlasForQuest(String taskId, List<AtlasLogic.Entry> entries) {
        Set<String> selected = new TreeSet<>(questRouteIds(taskId));
        Set<String> nonAtlas = new TreeSet<>();
        for (String id : selected) {
            if (id.equals(POWER_ROUTE)) {
                nonAtlas.add(id);
            }
        }
        List<AtlasLogic.Entry> found = entries.stream().filter(entry -> selected.contains(entry.id())).toList();
        Set<String> foundIds = new TreeSet<>();
        for (AtlasLogic.Entry entry : found) {
            if (!foundIds.add(entry.id())) {
                throw new IllegalArgumentException("Duplicate live T1 Atlas route for " + taskId + ": " + entry.id());
            }
        }
        Set<String> missing = new TreeSet<>(selected);
        nonAtlas.forEach(missing::remove);
        found.forEach(entry -> missing.remove(entry.id()));
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing live T1 Atlas routes for " + taskId + ": " + missing);
        }
        return new AtlasSelection(found, nonAtlas);
    }
}
