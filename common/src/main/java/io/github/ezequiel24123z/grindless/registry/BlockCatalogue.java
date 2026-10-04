package io.github.ezequiel24123z.grindless.registry;

import java.util.List;
import java.util.Locale;

/**
 * Every Grindless block, what shape it is, and which states it can show.
 *
 * <p>Like {@code SupplyCatalogue}, this has no Minecraft imports so the asset generator can read
 * it: blockstates, models, textures, loot tables, tags and the server-side state scenario are all
 * written from this one list and so cannot drift from it. The block classes build their
 * {@code status} property from the same lists. {@code VerifyAssets} checks the list against
 * {@link ModBlocks}, which is where the blocks are actually registered.
 *
 * <p>Each machine has its own geometry, front panel, top and per-status textures, so two machines
 * are never a recolour of each other (ADR-0052).
 */
public final class BlockCatalogue {

    /** The silhouette a block is built as. */
    public enum Geometry {
        /** A flat base with a flywheel and a hand crank on the front. */
        DYNAMO,
        /** A heavy hopper-and-stack machine with a drill aperture on the front. */
        EXTRACTOR,
        /** A desk with a raked screen. */
        TERMINAL,
        /** A boiler box with a chimney. */
        GENERATOR,
        /** Two rollers over a hopper. */
        MILL,
        /** A chamber with a V of electrodes. */
        FURNACE,
        /** A ram over a forming bed. */
        PRESS,
        /** Two arms over a fabrication deck. */
        ASSEMBLER,
        /** A brick drum with a short stack. The cheapest heat. */
        KILN,
        /** Two drawing dies and a spool. Wire, not crush. */
        WIRE_MILL,
        /** A stirred vat with a sight glass. Contact process. */
        REACTOR,
        /** An open sluice with a spray bar. The wet line. */
        WASHER,
        /** A vessel with two electrodes. Water becomes hydrogen and oxygen. */
        CELL,
        /** A louvered cowl with a stack. It drinks air. */
        INTAKE,
        /** A wellhead over a bore. Chunk water, not a vanilla source. */
        WELL,
        /** A smooth coil box. Clean re-melt, no slag. */
        INDUCTION,
        /** A spout over a mould. Melt becomes a solid form. */
        CASTER,
        /** A froth cell. Sulfides become concentrate and tailings. */
        FLOTATION,
        /** A tall tower; taller and more ringed with each tier. */
        PYLON,
        /** A low slab with two lanes. */
        BELT,
        /** A low junction with three mouths. */
        SPLITTER,
        /** Three inlets into one outlet. */
        MERGER,
        /** A low entrance that swallows a lane. */
        TUNNEL,
        /** Front preferred, side when the front backs up. */
        OVERFLOW,
        /** Passthrough with two filter mouths. */
        SORTER,
        /** A horseshoe over a lane. Ferromagnetic items leave left. */
        MAGNET,
        /** A flat panel. Daylight, not fuel. */
        SOLAR,
        /** A kettle and a stack. Water becomes steam. */
        BOILER,
        /** A coil of cold pipe. Steam becomes water. */
        CONDENSER,
        /** A short arm on a post. */
        MANIPULATOR,
        /** A heavier bore than the Crude Extractor. */
        DRILL,
        /** An open trough that liquid runs along. */
        CONDUIT,
        /** A closed tube. Hot fluid and gas, not gravity. */
        PRESSURE,
        /** A piston over a well. */
        PUMP,
        /** A powered impeller. It moves fluid; it does not summon it. */
        EPUMP,
        /** A squat cylinder with a sight glass. */
        TANK,
        /** A tall rated vessel. One block, not a formed tank. */
        INDUSTRIAL,
        /** A nozzle on a post. Fluid, not items. */
        FLUID_ARM,
        /** A low slab with a flux rail. Twice the conveyor. */
        FLUX_BELT,
        /** A heavier arm. Twelve items in one cycle. */
        STACK_ARM,
        /** An arm with a gate. One whitelist id. */
        FILTER_ARM,
        /** A thin run of cable. One integer, one direction. */
        SIGNAL,
        /** A reader. It holds the machine in front. */
        LOGIC,
        /** A plate that speaks redstone. */
        INTERFACE,
        /** A rack of cells. Capacity, not coverage. */
        BANK,
        /** Two coils on a core. A tap, not a pylon. */
        TRANSFORMER
    }

    /** Belts and arms: they carry items or they do not. */
    public static final List<String> BELT = List.of("idle", "running");

    /** A pump: it has a source, or it does not. */
    public static final List<String> PUMP = List.of("idle", "running", "starved");

    /** A generator: it can have charge it cannot give away, but is never starved. */
    public static final List<String> GENERATOR = List.of("idle", "running", "blocked");

    /** The grid: it carries power, or is overdrawn. */
    public static final List<String> GRID = List.of("idle", "running", "starved");

    /** A consumer: it can be stuck on output, on input, or on conditions. */
    public static final List<String> CONSUMER = List.of("idle", "running", "blocked", "starved", "out_of_band");

    /**
     * @param name     the registry path
     * @param geometry the silhouette
     * @param tier     1 to 3 for pylons, whose height and detail grow with it; 1 otherwise
     * @param facing   whether the block has a front and so a {@code facing} property
     * @param statuses the values of its {@code status} property, in {@code MachineStatus} names
     */
    public record Entry(String name, Geometry geometry, int tier, boolean facing, List<String> statuses) {

        /** The blockstate variant key for one state, such as {@code facing=north,status=idle}. */
        public String variantKey(String facingValue, String status) {
            return facing ? "facing=" + facingValue + ",status=" + status : "status=" + status;
        }

        /** The model name for one status, shared by every facing. */
        public String modelName(String status) {
            return name + "_" + status;
        }

        public String lower() {
            return geometry.name().toLowerCase(Locale.ROOT);
        }
    }

    /** The four horizontal facings and the Y rotation that turns a north-facing model to each. */
    public static final List<String> FACINGS = List.of("north", "east", "south", "west");
    public static final List<Integer> FACING_ROTATIONS = List.of(0, 90, 180, 270);

    private static final List<Entry> BLOCKS = List.of(
            new Entry("hand_crank_dynamo", Geometry.DYNAMO, 1, true, GENERATOR),
            new Entry("crude_extractor", Geometry.EXTRACTOR, 1, true, CONSUMER),
            new Entry("research_terminal", Geometry.TERMINAL, 1, true, CONSUMER),
            new Entry("thermal_generator", Geometry.GENERATOR, 1, true, GENERATOR),
            new Entry("pulverizer", Geometry.MILL, 1, true, CONSUMER),
            new Entry("arc_furnace", Geometry.FURNACE, 1, true, CONSUMER),
            new Entry("press", Geometry.PRESS, 1, true, CONSUMER),
            new Entry("assembler", Geometry.ASSEMBLER, 1, true, CONSUMER),
            new Entry("kiln", Geometry.KILN, 1, true, CONSUMER),
            new Entry("wire_mill", Geometry.WIRE_MILL, 1, true, CONSUMER),
            new Entry("chemical_reactor", Geometry.REACTOR, 1, true, CONSUMER),
            new Entry("chemical_washer", Geometry.WASHER, 1, true, CONSUMER),
            new Entry("electrolysis_cell", Geometry.CELL, 1, true, CONSUMER),
            new Entry("atmospheric_intake", Geometry.INTAKE, 1, true, CONSUMER),
            new Entry("fluid_well", Geometry.WELL, 1, true, CONSUMER),
            new Entry("induction_furnace", Geometry.INDUCTION, 1, true, CONSUMER),
            new Entry("caster", Geometry.CASTER, 1, true, CONSUMER),
            new Entry("flotation_cell", Geometry.FLOTATION, 1, true, CONSUMER),
            new Entry("flux_pylon_mk1", Geometry.PYLON, 1, false, GRID),
            new Entry("flux_pylon_mk2", Geometry.PYLON, 2, false, GRID),
            new Entry("flux_pylon_mk3", Geometry.PYLON, 3, false, GRID),
            new Entry("conveyor_belt", Geometry.BELT, 1, true, BELT),
            new Entry("splitter", Geometry.SPLITTER, 1, true, BELT),
            new Entry("merger", Geometry.MERGER, 1, true, BELT),
            new Entry("tunnel_belt", Geometry.TUNNEL, 1, true, BELT),
            new Entry("overflow_gate", Geometry.OVERFLOW, 1, true, BELT),
            new Entry("sorter", Geometry.SORTER, 1, true, BELT),
            new Entry("magnetic_separator", Geometry.MAGNET, 1, true, BELT),
            new Entry("solar_array", Geometry.SOLAR, 1, true, GENERATOR),
            new Entry("boiler", Geometry.BOILER, 1, true, CONSUMER),
            new Entry("condenser", Geometry.CONDENSER, 1, true, CONSUMER),
            new Entry("crude_manipulator", Geometry.MANIPULATOR, 1, true, BELT),
            new Entry("terrestrial_extractor", Geometry.DRILL, 1, true, CONSUMER),
            new Entry("clay_conduit", Geometry.CONDUIT, 1, true, BELT),
            new Entry("pressure_pipe", Geometry.PRESSURE, 1, true, BELT),
            new Entry("hand_pump", Geometry.PUMP, 1, true, PUMP),
            new Entry("electric_pump", Geometry.EPUMP, 1, true, CONSUMER),
            new Entry("basic_tank", Geometry.TANK, 1, true, BELT),
            new Entry("industrial_tank", Geometry.INDUSTRIAL, 1, true, BELT),
            new Entry("fluid_manipulator", Geometry.FLUID_ARM, 1, true, CONSUMER),
            new Entry("flux_belt", Geometry.FLUX_BELT, 1, true, CONSUMER),
            new Entry("stack_manipulator", Geometry.STACK_ARM, 1, true, CONSUMER),
            new Entry("filter_manipulator", Geometry.FILTER_ARM, 1, true, CONSUMER),
            new Entry("signal_cable", Geometry.SIGNAL, 1, true, BELT),
            new Entry("logic_controller", Geometry.LOGIC, 1, true, BELT),
            new Entry("redstone_interface", Geometry.INTERFACE, 1, true, BELT),
            new Entry("capacitor_bank", Geometry.BANK, 1, true, GRID),
            new Entry("flux_transformer", Geometry.TRANSFORMER, 1, true, GRID));

    /** Items that are not blocks and not material forms, which need a hand-drawn sprite. */
    private static final List<String> PLACEHOLDER_SPRITES = List.of("multitool", "data_core", "advanced_data_core", "slag",
            "prospectors_scanner", "process_atlas", "flux_conduit", "plate_die", "rod_die", "gear_die", "coil_die",
            "copper_coil", "motor", "vanadia_pellet", "ingot_mould", "plate_mould");

    /** The item that renders as the bare casing cube. */
    public static final String CASING_ITEM = "machine_casing";

    private BlockCatalogue() {
    }

    public static List<Entry> blocks() {
        return BLOCKS;
    }

    public static Entry block(String name) {
        return BLOCKS.stream().filter(b -> b.name().equals(name)).findFirst().orElseThrow();
    }

    public static List<String> placeholderSprites() {
        return PLACEHOLDER_SPRITES;
    }

    /**
     * Blocks registered in {@code ModBlocks} that are not player-facing: no item, no catalogue
     * entry, generated separately. The pylon shaft is occupancy for the two blocks above a pylon.
     */
    public static List<String> technical() {
        return List.of("flux_pylon_shaft");
    }
}
