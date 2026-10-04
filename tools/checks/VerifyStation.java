package io.github.ezequiel24123z.grindless.station;

import io.github.ezequiel24123z.grindless.planet.PlanetCatalogue;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.star.DriftCatalogue;
import io.github.ezequiel24123z.grindless.star.StarwardLinkLogic;
import io.github.ezequiel24123z.grindless.structure.GroundArrayLogic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Behaviour checks for the supraluminal station (ADR-0098). Not part of the mod. */
public final class VerifyStation {

    private static final Path DATA = Path.of("common/src/main/resources/data");
    private static final Path SOURCES = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        destination();
        climb();
        toll();
        recipe();
        linkStays();

        System.out.println(failures == 0
                ? "ALL STATION CHECKS PASSED"
                : failures + " STATION CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void destination() {
        eq("home arrives on the drift", DriftCatalogue.DRIFT, StationRide.destination(StationRide.HOME));
        eq("luna arrives on the drift", DriftCatalogue.DRIFT, StationRide.destination(PlanetCatalogue.LUNA));
        eq("the nether arrives on the drift", DriftCatalogue.DRIFT,
                StationRide.destination("minecraft:the_nether"));
        eq("the end arrives on the drift", DriftCatalogue.DRIFT,
                StationRide.destination("minecraft:the_end"));
        eq("the drift rides to the centre", StationRide.HOLE, StationRide.destination(DriftCatalogue.DRIFT));
        eq("the centre rides home", StationRide.HOME, StationRide.destination(StationRide.HOLE));
        yes("the centre can depart", StationRide.canDepart(StationRide.HOLE));
        yes("the drift can depart", StationRide.canDepart(DriftCatalogue.DRIFT));
        yes("home can depart", StationRide.canDepart(StationRide.HOME));
        no("an empty id can depart", StationRide.canDepart(""));
        eq("the centre is the named hole", "grindless:sagittarius", StationRide.HOLE);
        eq("the berth uses the link's column", StarwardLinkLogic.ARRIVAL_Y, StationRide.DRIFT_BERTH_Y);
        eq("the rider stands beside that berth", StarwardLinkLogic.STAND_X, StationRide.DRIFT_STAND_X);
    }

    private static void climb() {
        eq("the ceiling keeps three blocks of clearance", 317, StationRide.ceilingY(320));
        eq("one block per tick", 1.0, StationRide.ASCENT_PER_TICK);
        eq("the first step leaves the berth", 1.0, StationRide.climb(0.0, 317));
        eq("the last step stops on the ceiling", 317.0, StationRide.climb(316.4, 317));
        eq("the ceiling does not climb further", 317.0, StationRide.climb(317.0, 317));
        yes("the ceiling counts as arrived", StationRide.atCeiling(317.0, 317));
        no("one below is still climbing", StationRide.atCeiling(316.0, 317));
        double y = 64.0;
        int ceiling = StationRide.ceilingY(320);
        int steps = 0;
        while (!StationRide.atCeiling(y, ceiling) && steps < 1000) {
            y = StationRide.climb(y, ceiling);
            steps++;
        }
        eq("a ride from the ground reaches the ceiling", ceiling, (int) y);
        yes("and it takes more than one tick", steps > 1);
    }

    private static void toll() {
        eq("leaving home costs the array buffer", GroundArrayLogic.CAPACITY, StationRide.toll(StationRide.HOME));
        eq("that is the link's toll", StarwardLinkLogic.COST, StationRide.toll(PlanetCatalogue.LUNA));
        eq("leaving the drift is free", 0L, StationRide.toll(DriftCatalogue.DRIFT));
        eq("leaving the centre is free", 0L, StationRide.toll(StationRide.HOLE));
        yes("a free toll is already ready", StationRide.ready(0L, 0L));
        no("one short of the array is not ready", StationRide.ready(StationRide.COST - 1L, StationRide.COST));
        yes("the array toll is enough", StationRide.ready(StationRide.COST, StationRide.COST));
        eq("charge does not pass the toll", StationRide.COST,
                StationRide.accept(StationRide.COST - 10L, 50L, StationRide.COST));
        eq("nothing drawn stores nothing", 0L, StationRide.accept(0L, 0L, StationRide.COST));
        no("a refused toll is never ready", StationRide.ready(StationRide.COST, -1L));
    }

    private static void recipe() throws IOException {
        List<ProcessRecipe> recipes = ProcessGraph.generate(List.of());
        ProcessRecipe berth = find(recipes, "assemble/station_berth");
        ProcessRecipe station = find(recipes, "assemble/station");
        if (berth == null || station == null) {
            return;
        }
        eq("the berth is the assembler", MachineFamily.ASSEMBLER, berth.family());
        eq("the berth takes one link", StationRide.BERTH_LINKS, berth.itemInputs().get(0).count());
        eq("that link is the starward link", "item:" + FabricationLogic.STARWARD_LINK,
                berth.itemInputs().get(0).qualified());
        eq("the berth takes four plates", StationRide.BERTH_PLATES, berth.itemInputs().get(1).count());
        eq("those plates are steel", "tag:forge:plates/steel", berth.itemInputs().get(1).qualified());
        eq("the output is the berth", "item:" + FabricationLogic.STATION_BERTH,
                berth.itemOutputs().get(0).qualified());
        eq("the station takes one casing", StationRide.STATION_CASINGS, station.itemInputs().get(0).count());
        eq("the station takes one motor", StationRide.STATION_MOTORS, station.itemInputs().get(1).count());
        eq("that motor is the motor", "item:" + FabricationLogic.MOTOR,
                station.itemInputs().get(1).qualified());
        eq("the station takes two array casings", StationRide.STATION_ARRAY_CASINGS,
                station.itemInputs().get(2).count());
        eq("those casings are array casings", "item:" + FabricationLogic.ARRAY_CASING,
                station.itemInputs().get(2).qualified());
        eq("the output is the station", "item:" + FabricationLogic.SUPRALUMINAL_STATION,
                station.itemOutputs().get(0).qualified());
        eq("both are twenty seconds", FabricationLogic.ASSEMBLE_TICKS, berth.durationTicks());
        eq("both need Industrial", "industrial", station.blueprint());
        Path recipesDir = DATA.resolve("grindless/recipes");
        no("the berth has no crafting-table recipe",
                Files.isRegularFile(recipesDir.resolve("station_berth.json")));
        no("the station has no crafting-table recipe",
                Files.isRegularFile(recipesDir.resolve("supraluminal_station.json")));
    }

    /** The link stays registered. This slice replaces the hop, not the block id. */
    private static void linkStays() throws IOException {
        String blocks = Files.readString(SOURCES.resolve("registry/ModBlocks.java"));
        String link = Files.readString(SOURCES.resolve("star/StarwardLinkBlockEntity.java"));
        yes("the starward link is still registered", blocks.contains("register(\"starward_link\""));
        yes("the berth is registered", blocks.contains("register(\"station_berth\""));
        yes("the link is still marked as the hop the station replaces",
                blocks.contains("Placeholder: the station replaces this hop"));
        no("the link entity no longer departs", link.contains("StarwardTravel.depart"));
        no("the link entity no longer returns", link.contains("StarwardTravel.home"));
    }

    private static ProcessRecipe find(List<ProcessRecipe> recipes, String id) {
        for (ProcessRecipe recipe : recipes) {
            if (id.equals(recipe.id())) {
                return recipe;
            }
        }
        fail("missing recipe " + id);
        return null;
    }

    private static void eq(String what, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected " + expected + " but was " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            System.out.println("  ok   " + what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void no(String what, boolean actual) {
        yes(what, !actual);
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
