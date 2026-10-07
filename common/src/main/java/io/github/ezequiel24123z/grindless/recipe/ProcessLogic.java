package io.github.ezequiel24123z.grindless.recipe;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;

/**
 * Cycle math and status for process machines, independent of a world.
 *
 * <p>B1 is six seconds; R1 is twelve; roast is eight; oxide reduce is ten. All draw F1.
 * Condition efficiency scales progress, never yield (ADR-0040).
 */
public final class ProcessLogic {

    public static final int PULVERIZE_TICKS = 20 * 6;
    public static final int REDUCE_TICKS = 20 * 12;
    public static final int ROAST_TICKS = 20 * 8;
    public static final int OXIDE_REDUCE_TICKS = 20 * 10;
    public static final long FU_PER_TICK = FluxTier.F1.nominal();

    /** R1 carbothermic reduction. The T1 Arc Furnace holds this without a dial. */
    public static final double REDUCE_TEMPERATURE = 1500.0;
    public static final String REDUCE_ATMOSPHERE = "REDUCING";

    /** R2 roast. The T1 Kiln holds this without a dial (ADR-0065). */
    public static final double ROAST_TEMPERATURE = 700.0;
    public static final String ROAST_ATMOSPHERE = "OXIDISING";

    public static final String CARBON = "grindless:carbon";
    public static final String SLAG = "grindless:slag";
    public static final String CARBON_MONOXIDE = "grindless:carbon_monoxide";
    public static final String SULFUR_DIOXIDE = "grindless:sulfur_dioxide";
    public static final String SULFUR_TRIOXIDE = "grindless:sulfur_trioxide";
    public static final String SULFURIC_ACID = "grindless:sulfuric_acid";
    public static final String WATER = "minecraft:water";
    public static final int CO_MB = 1000;
    public static final int SO2_MB = 1000;
    public static final int SO3_MB = 1000;
    public static final int ACID_MB = 1000;
    public static final int WATER_MB = 500;
    public static final int ABSORB_WATER_MB = 200;
    public static final int PICKLE_ACID_MB = 100;
    public static final int CONTACT_OXIDE_TICKS = 20 * 6;
    public static final int CONTACT_ACID_TICKS = 20 * 4;
    public static final int PICKLE_TICKS = 20 * 4;
    public static final double CONTACT_TEMPERATURE = 450.0;
    public static final String CONTACT_ATMOSPHERE = "OXIDISING";
    public static final String WASH_AGITATION = "STIRRED";

    /**
     * B2 integer batch (ADR-0076). Four raw are eight crushed: 2 B water, 20 s, one byproduct.
     * That is four times the per-raw line in {@code PROCESSES.md}.
     */
    public static final int WASH_TICKS = 20 * 20;
    public static final int WASH_CRUSHED = 8;
    public static final int WASH_WATER_MB = 2000;

    /**
     * Water electrolysis (ADR-0077). Two buckets fill the buffer; oxygen is the vented
     * half-volume. Ten seconds matches the documented chlor-alkali cycle.
     */
    public static final String HYDROGEN = "grindless:hydrogen";
    public static final String OXYGEN = "grindless:oxygen";
    public static final String NITROGEN = "grindless:nitrogen";
    public static final int ELECTROLYSIS_WATER_MB = 2000;
    public static final int ELECTROLYSIS_HYDROGEN_MB = 2000;
    public static final int ELECTROLYSIS_OXYGEN_MB = 1000;
    public static final int ELECTROLYSIS_TICKS = 20 * 10;

    /** Oxyhydrogen recombination. The named oxygen sink, and a second water source. */
    public static final int RECOMBINE_HYDROGEN_MB = 2000;
    public static final int RECOMBINE_OXYGEN_MB = 1000;
    public static final int RECOMBINE_WATER_MB = 2000;
    public static final int RECOMBINE_TICKS = 20 * 8;

    /**
     * Ambient air skim (ADR-0077). Two buckets is the oxygen cut of the documented
     * ten-second separation, rounded to the machine buffer. Nitrogen and argon wait.
     */
    public static final int AIR_OXYGEN_MB = 2000;
    public static final int AIR_TICKS = 20 * 10;

    /** 1 u of melt. The modded convention, so the caster needs no conversion table (ADR-0079). */
    public static final int MOLTEN_MB = 144;
    public static final double MOLTEN_C = 1000.0;
    /** Induction and caster buffers. The Basic Tank stays at {@link io.github.ezequiel24123z.grindless.fluid.FluidLogic#AMBIENT_MAX_C}. */
    public static final double MOLTEN_MAX_C = 1200.0;
    public static final int MELT_TICKS = 20 * 8;
    public static final int CAST_TICKS = 20 * 4;
    public static final double MELT_TEMPERATURE = MOLTEN_C;
    public static final String MELT_ATMOSPHERE = "INERT";

    public static String moltenId(String material) {
        return "grindless:molten/" + material;
    }

    /**
     * B3 integer batch (ADR-0080). Ten raw are twenty crushed: 2.4 concentrate and
     * 0.3 tailings become 24 and 3, and 0.05 B of surfactant becomes 500 mB.
     * Eighty seconds is the documented eight, times ten.
     */
    public static final String SURFACTANT = "grindless:surfactant";
    public static final int FLOTATION_CRUSHED = 20;
    public static final int FLOTATION_SURFACTANT_MB = 500;
    public static final int FLOTATION_CONCENTRATE = 24;
    public static final int FLOTATION_TAILINGS = 3;
    public static final int FLOTATION_TICKS = 20 * 80;
    public static final int TAILINGS_PER_INGOT = 10;
    public static final int SURFACTANT_WATER_MB = 1000;
    public static final int SURFACTANT_MB = 1000;
    public static final int SURFACTANT_TICKS = 20 * 8;

    /** Steam at the documented boiler point. The tank refuses both the heat and the pressure. */
    public static final String STEAM = "grindless:steam";
    public static final double STEAM_C = 150.0;
    public static final double STEAM_MPA = 0.5;
    public static final double STEAM_MAX_C = 200.0;
    public static final double STEAM_MAX_MPA = 1.0;
    public static final int BOILER_MB = 1000;
    public static final int BOILER_TICKS = 20 * 10;
    public static final int CONDENSE_TICKS = 20 * 4;

    /**
     * Electric-arc steel (ADR-0090). The graph's 0.1 u of carbon is one coal per ten
     * ingots, and 14 s per ingot is 140 s for that batch.
     */
    public static final int STEEL_IRON = 10;
    public static final int STEEL_CARBON = 1;
    public static final int STEEL_OUT = 10;
    public static final int STEEL_TICKS = 20 * 14 * STEEL_OUT;
    public static final double STEEL_TEMPERATURE = 1600.0;

    /**
     * Refractory brick (ADR-0091). One slag is one reduction's output. Twenty seconds
     * and 1400 °C are the only numbers the ceramics row already writes.
     */
    public static final String REFRACTORY_BRICK = "grindless:refractory_brick";
    public static final int REFRACTORY_SLAG = 1;
    public static final int REFRACTORY_OUT = 1;
    public static final int REFRACTORY_TICKS = 20 * 20;
    public static final double REFRACTORY_TEMPERATURE = 1400.0;

    /**
     * Metallurgical silicon (ADR-0092). The graph's 1 u silica and 2 u carbon are
     * already an integer batch, so the cycle stays 14 s. Silica is a tag over sand
     * and quartz. The furnace hold stays 1500 °C, inside tolerance and outside the
     * optimal zone of 1900 °C.
     */
    public static final String SILICA = "grindless:silica";
    public static final String METALLURGICAL_SILICON = "grindless:metallurgical_silicon";
    public static final int SILICON_SILICA = 1;
    public static final int SILICON_CARBON = 2;
    public static final int SILICON_OUT = 1;
    public static final int SILICON_CO_MB = 2 * CO_MB;
    public static final int SILICON_TICKS = 20 * 14;
    public static final double SILICON_TEMPERATURE = 1900.0;

    /**
     * Zone refining (ADR-0093). The graph's 0.70 u per metallurgical silicon is seven
     * electronic silicon per ten, in ten times 60 s. The ±5 °C inert band waits with
     * the Induction Furnace; 1420 °C here is the ordinary relative band.
     */
    public static final String ELECTRONIC_SILICON = "grindless:electronic_silicon";
    public static final int ZONE_IN = 10;
    public static final int ZONE_OUT = 7;
    public static final int ZONE_TICKS = 20 * 60 * ZONE_IN;
    public static final double ZONE_TEMPERATURE = 1420.0;

    private ProcessLogic() {
    }

    /**
     * How many ticks of cycle this game tick is worth.
     *
     * <p>{@code timeMultiplier} is below 1.0 when upgrades speed the machine up, matching
     * {@code MachineBlockEntity#timeMultiplier}.
     */
    public static double work(long drawn, long requested, double conditionEfficiency,
                              double timeMultiplier) {
        if (requested <= 0L || drawn <= 0L || conditionEfficiency <= 0.0) {
            return 0.0;
        }
        double speed = timeMultiplier <= 0.0 ? 1.0 : 1.0 / timeMultiplier;
        return ((double) drawn / (double) requested) * conditionEfficiency * speed;
    }

    /**
     * What a process machine should show.
     *
     * <p>Order is the player's question: nothing loaded, conditions wrong, product stuck,
     * missing feed or power, working.
     */
    public static MachineStatus status(boolean hasRecipe, boolean missingInput, boolean outputStuck,
                                       boolean inBand, boolean powered, boolean working) {
        if (!hasRecipe && !missingInput) {
            return MachineStatus.IDLE;
        }
        if (hasRecipe && !inBand) {
            return MachineStatus.OUT_OF_BAND;
        }
        if (outputStuck) {
            return MachineStatus.BLOCKED;
        }
        if (missingInput || !powered) {
            return MachineStatus.STARVED;
        }
        return working ? MachineStatus.RUNNING : MachineStatus.IDLE;
    }
}
