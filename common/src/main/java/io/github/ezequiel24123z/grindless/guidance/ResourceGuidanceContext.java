package io.github.ezequiel24123z.grindless.guidance;

import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.recipe.AtlasLogic;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.RenewableProcesses;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Kind;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Display selection only. Identities and amounts still come from the native Atlas/quest data. */
public final class ResourceGuidanceContext {
    private static final Pattern QUALIFIED = Pattern.compile(
            "(item|tag|fluid):([a-z0-9_.-]+:[a-z0-9_./-]+)(?: \\[F([1-9]|1[0-5])])?");
    private static final Set<String> BOOTSTRAP_CRAFTS = Set.of("craft/crank_handle", "craft/hand_pump",
            "craft/geological_collector", "craft/biocultivator", "craft/biomass_dynamo",
            "craft/assembly_bench", "craft/process_atlas", "craft/multitool");

    private ResourceGuidanceContext() {
    }

    public enum Role { INPUT, CATALYST, OUTPUT }

    public record Operand(Resource resource, int amount, Role role) {
    }

    /** Reads the Atlas's structured qualified operand, never a localized recipe description. */
    public static Optional<Resource> resource(String qualified) {
        if (qualified == null) {
            return Optional.empty();
        }
        var match = QUALIFIED.matcher(qualified);
        if (!match.matches()) {
            return Optional.empty();
        }
        Kind kind = Kind.valueOf(match.group(1).toUpperCase(Locale.ROOT));
        int rating = match.group(3) == null ? 0 : Integer.parseInt(match.group(3));
        return kind != Kind.ITEM && rating != 0 ? Optional.empty()
                : Optional.of(new Resource(kind, match.group(2), rating, "items"));
    }

    /** Roles come from the actual three operand lists, never output order or resource class. */
    public static List<Operand> operands(AtlasLogic.Entry entry) {
        List<Operand> operands = new ArrayList<>();
        append(operands, entry.inputs(), Role.INPUT);
        append(operands, entry.catalysts(), Role.CATALYST);
        append(operands, entry.outputs(), Role.OUTPUT);
        return List.copyOf(operands);
    }

    private static void append(List<Operand> target, List<String> values, Role role) {
        Map<Resource, Integer> counted = new LinkedHashMap<>();
        values.forEach(value -> resource(value).ifPresent(resource -> counted.merge(resource, 1, Integer::sum)));
        counted.forEach((resource, amount) -> target.add(new Operand(resource, amount, role)));
    }

    public static Optional<Resource> questSubject(QuestCatalogue.Task task) {
        if (task.evidence() == QuestCatalogue.Evidence.DIMENSION) {
            return Optional.empty();
        }
        int rating = task.evidence() == QuestCatalogue.Evidence.MATRIX ? task.matrixRating().ordinal() : 0;
        return Optional.of(new Resource(Kind.ITEM, task.subject(), rating, "items"));
    }

    /** Only current T0 topics gain embedded native data; parked T1/T2 text is not expanded. */
    public static List<AtlasLogic.Entry> guideRecipes(String page, List<AtlasLogic.Entry> entries) {
        Set<String> recurring = RenewableProcesses.recipes().stream().map(recipe -> recipe.id())
                .collect(Collectors.toUnmodifiableSet());
        return entries.stream().filter(entry -> switch (page) {
            case "bootstrap" -> BOOTSTRAP_CRAFTS.contains(entry.id());
            case "workshop" -> entry.family() != null && Set.of(MachineFamily.QUERN, MachineFamily.CLAY_MIXER,
                    MachineFamily.FIRING_CHAMBER).contains(entry.family());
            case "settling" -> entry.family() == MachineFamily.SETTLING_TROUGH;
            case "relay" -> Set.of("craft/data_core", "calibrate/data_core", "craft/relay_matrix").contains(entry.id());
            case "renewables" -> recurring.contains(entry.id());
            default -> false;
        }).toList();
    }
}
