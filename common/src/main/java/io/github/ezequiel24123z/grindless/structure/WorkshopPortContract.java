package io.github.ezequiel24123z.grindless.structure;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue.Geometry;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** One bounded authority for T0 replacement geometry, roles, positions, counts and exposed faces. */
public final class WorkshopPortContract {

    public enum Family {
        BRICK, CHANNEL, SCREEN
    }

    public enum Kind {
        WORKSHOP_ITEM_INPUT("workshop_item_input", Family.BRICK, WorkshopPortRole.ITEM_INPUT),
        WORKSHOP_ITEM_OUTPUT("workshop_item_output", Family.BRICK, WorkshopPortRole.ITEM_OUTPUT),
        SETTLING_ITEM_INPUT("settling_item_input", Family.CHANNEL, WorkshopPortRole.ITEM_INPUT),
        SETTLING_FLUID_INPUT("settling_fluid_input", Family.CHANNEL, WorkshopPortRole.FLUID_INPUT),
        SETTLING_ITEM_OUTPUT("settling_item_output", Family.SCREEN, WorkshopPortRole.ITEM_OUTPUT),
        SETTLING_FLUID_OUTPUT("settling_fluid_output", Family.SCREEN, WorkshopPortRole.FLUID_OUTPUT);

        private final String name;
        private final Family family;
        private final WorkshopPortRole role;

        Kind(String name, Family family, WorkshopPortRole role) {
            this.name = name;
            this.family = family;
            this.role = role;
        }

        public String id() {
            return "grindless:" + name;
        }

        public String blockName() {
            return name;
        }

        public Family family() {
            return family;
        }

        public WorkshopPortRole role() {
            return role;
        }

        public WorkshopStructure structure() {
            return family == Family.BRICK ? WorkshopStructure.FIRING_CHAMBER : WorkshopStructure.SETTLING_TROUGH;
        }

        public Geometry geometry() {
            return switch (family) {
                case BRICK -> Geometry.FIREBOX;
                case CHANNEL -> Geometry.CHANNEL;
                case SCREEN -> Geometry.SCREEN;
            };
        }
    }

    /** Faces are clockwise horizontal turns from north, in the controller's local coordinates. */
    public record Slot(WorkshopStructure.Offset offset, Family family, Set<WorkshopPortRole> roles,
                       Set<Integer> faces) {
        public Slot {
            roles = Set.copyOf(roles);
            faces = Set.copyOf(faces);
        }

        public boolean accepts(Kind kind) {
            return family == kind.family() && roles.contains(kind.role());
        }

        public boolean permitsFace(int controllerTurns, int worldFaceTurns) {
            return worldFaceTurns >= 0 && faces.contains(Math.floorMod(worldFaceTurns - controllerTurns, 4));
        }
    }

    private static final Set<WorkshopPortRole> ITEM_ROLES = Set.of(
            WorkshopPortRole.ITEM_INPUT, WorkshopPortRole.ITEM_OUTPUT);
    private static final List<Slot> FIRING = List.of(
            new Slot(new WorkshopStructure.Offset(1, 0, 0), Family.BRICK, ITEM_ROLES, Set.of(0, 1)),
            new Slot(new WorkshopStructure.Offset(1, 1, 0), Family.BRICK, ITEM_ROLES, Set.of(0, 1)));
    private static final List<Slot> SETTLING = List.of(
            new Slot(new WorkshopStructure.Offset(0, 0, 1), Family.CHANNEL,
                    Set.of(WorkshopPortRole.ITEM_INPUT, WorkshopPortRole.FLUID_INPUT), Set.of(1, 3)),
            new Slot(new WorkshopStructure.Offset(0, 0, 2), Family.SCREEN,
                    Set.of(WorkshopPortRole.ITEM_OUTPUT, WorkshopPortRole.FLUID_OUTPUT), Set.of(1, 2, 3)));

    private WorkshopPortContract() {
    }

    public static List<Slot> slots(WorkshopStructure structure) {
        return switch (structure) {
            case FIRING_CHAMBER -> FIRING;
            case SETTLING_TROUGH -> SETTLING;
            case NONE -> List.of();
        };
    }

    public static Optional<Slot> slot(WorkshopStructure structure, WorkshopStructure.Offset localOffset) {
        return slots(structure).stream().filter(slot -> slot.offset().equals(localOffset)).findFirst();
    }

    public static Optional<Kind> kind(String block) {
        for (Kind kind : Kind.values()) {
            if (kind.id().equals(block)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    public static int maximum(WorkshopStructure structure, WorkshopPortRole role) {
        return slots(structure).stream().anyMatch(slot -> slot.roles().contains(role)) ? 1 : 0;
    }

    public static int maximumPorts(WorkshopStructure structure) {
        return slots(structure).size();
    }

    public static int minimumBricks(WorkshopStructure structure) {
        return structure == WorkshopStructure.FIRING_CHAMBER ? 5 : 0;
    }
}
