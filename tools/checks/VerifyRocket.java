package io.github.ezequiel24123z.grindless.flight;

import io.github.ezequiel24123z.grindless.planet.LunarLinkLogic;
import io.github.ezequiel24123z.grindless.planet.PlanetCatalogue;
import io.github.ezequiel24123z.grindless.recipe.FabricationLogic;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessGraph;
import io.github.ezequiel24123z.grindless.recipe.ProcessRecipe;
import io.github.ezequiel24123z.grindless.star.DriftCatalogue;
import io.github.ezequiel24123z.grindless.star.StarwardLinkLogic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Behaviour checks for local rocket flight (ADR-0097). Not part of the mod. */
public final class VerifyRocket {

    private static final Path DATA = Path.of("common/src/main/resources/data");
    private static final Path SOURCES = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        map();
        climb();
        toll();
        recipe();
        placeholders();

        System.out.println(failures == 0
                ? "ALL ROCKET CHECKS PASSED"
                : failures + " ROCKET CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void map() {
        eq("two bodies", 2, RocketFlight.sites().size());
        eq("home is first", RocketFlight.HOME, RocketFlight.sites().get(0));
        eq("luna is second", PlanetCatalogue.LUNA, RocketFlight.sites().get(1));
        yes("home is a site", RocketFlight.isSite(RocketFlight.HOME));
        yes("luna is a site", RocketFlight.isSite(PlanetCatalogue.LUNA));
        no("the drift is not a site", RocketFlight.isSite(DriftCatalogue.DRIFT));
        no("the nether is not a site", RocketFlight.isSite("minecraft:the_nether"));
        no("the end is not a site", RocketFlight.isSite("minecraft:the_end"));
        no("a black hole is not a site", RocketFlight.isSite("grindless:sagittarius"));
        no("the drift is not a launch", RocketFlight.canLaunch(DriftCatalogue.DRIFT));
        yes("home can launch", RocketFlight.canLaunch(RocketFlight.HOME));
        yes("luna can launch", RocketFlight.canLaunch(PlanetCatalogue.LUNA));
        eq("home is labelled home", "site.grindless.home", RocketFlight.siteKey(RocketFlight.HOME));
        eq("luna is labelled luna", "site.grindless.luna", RocketFlight.siteKey(PlanetCatalogue.LUNA));
    }

    private static void climb() {
        eq("the ceiling keeps three blocks of clearance", 317, RocketFlight.ceilingY(320));
        eq("one block per tick", 1.0, RocketFlight.ASCENT_PER_TICK);
        eq("the first step leaves the pad", 1.0, RocketFlight.climb(0.0, 317));
        eq("the last step stops on the ceiling", 317.0, RocketFlight.climb(316.4, 317));
        eq("the ceiling does not climb further", 317.0, RocketFlight.climb(317.0, 317));
        yes("the ceiling counts as arrived", RocketFlight.atCeiling(317.0, 317));
        no("one below is still climbing", RocketFlight.atCeiling(316.0, 317));
        double y = 64.0;
        int ceiling = RocketFlight.ceilingY(320);
        int steps = 0;
        while (!RocketFlight.atCeiling(y, ceiling) && steps < 1000) {
            y = RocketFlight.climb(y, ceiling);
            steps++;
        }
        eq("a flight from the ground reaches the ceiling", ceiling, (int) y);
        yes("and it takes more than one tick", steps > 1);
    }

    private static void toll() {
        eq("leaving home costs the lunar placeholder", LunarLinkLogic.COST, RocketFlight.toll(RocketFlight.HOME));
        eq("leaving luna is free", 0L, RocketFlight.toll(PlanetCatalogue.LUNA));
        eq("the drift has no rocket toll", -1L, RocketFlight.toll(DriftCatalogue.DRIFT));
        yes("a free toll is already ready", RocketFlight.ready(0L, 0L));
        no("one short of home is not ready", RocketFlight.ready(RocketFlight.COST - 1L, RocketFlight.COST));
        yes("the home toll is enough", RocketFlight.ready(RocketFlight.COST, RocketFlight.COST));
        eq("charge does not pass the toll", RocketFlight.COST,
                RocketFlight.accept(RocketFlight.COST - 10L, 50L, RocketFlight.COST));
        eq("nothing drawn stores nothing", 0L, RocketFlight.accept(0L, 0L, RocketFlight.COST));
        eq("the starward toll is untouched", RocketFlight.COST < StarwardLinkLogic.COST, true);
        eq("luna's pad sits on the regolith", PlanetCatalogue.SURFACE_Y + 1, RocketFlight.LUNA_PAD_Y);
        eq("the rider stands beside that pad", 1, RocketFlight.LUNA_STAND_X);
    }

    private static void recipe() throws IOException {
        List<ProcessRecipe> recipes = ProcessGraph.generate(List.of());
        ProcessRecipe pad = find(recipes, "assemble/launch_pad");
        ProcessRecipe rocket = find(recipes, "assemble/survey_rocket");
        if (pad == null || rocket == null) {
            return;
        }
        eq("the pad is the assembler", MachineFamily.ASSEMBLER, pad.family());
        eq("the pad takes one casing", RocketFlight.PAD_CASINGS, pad.itemInputs().get(0).count());
        eq("that casing is the machine casing", "item:" + FabricationLogic.MACHINE_CASING,
                pad.itemInputs().get(0).qualified());
        eq("the pad takes four plates", RocketFlight.PAD_PLATES, pad.itemInputs().get(1).count());
        eq("those plates are steel", "tag:forge:plates/steel", pad.itemInputs().get(1).qualified());
        eq("the pad makes one block", 1, pad.itemOutputs().get(0).count());
        eq("the output is the launch pad", "item:" + FabricationLogic.LAUNCH_PAD,
                pad.itemOutputs().get(0).qualified());
        eq("the rocket takes one motor", RocketFlight.ROCKET_MOTORS, rocket.itemInputs().get(1).count());
        eq("that motor is the motor", "item:" + FabricationLogic.MOTOR,
                rocket.itemInputs().get(1).qualified());
        eq("the rocket takes two plates", RocketFlight.ROCKET_PLATES, rocket.itemInputs().get(2).count());
        eq("the output is the survey rocket", "item:" + FabricationLogic.SURVEY_ROCKET,
                rocket.itemOutputs().get(0).qualified());
        eq("both are twenty seconds", FabricationLogic.ASSEMBLE_TICKS, pad.durationTicks());
        eq("both need Industrial", "industrial", rocket.blueprint());
        Path recipesDir = DATA.resolve("grindless/recipes");
        no("the pad has no crafting-table recipe",
                Files.isRegularFile(recipesDir.resolve("launch_pad.json")));
        no("the rocket has no crafting-table recipe",
                Files.isRegularFile(recipesDir.resolve("survey_rocket.json")));
    }

    /** The links stay registered. This slice marks them; it does not delete them. */
    private static void placeholders() throws IOException {
        String blocks = Files.readString(SOURCES.resolve("registry/ModBlocks.java"));
        yes("the lunar link is still registered", blocks.contains("register(\"lunar_link\""));
        yes("the starward link is still registered", blocks.contains("register(\"starward_link\""));
        yes("the lunar link is marked as a placeholder",
                blocks.contains("Placeholder: the rocket replaces this flight"));
        yes("the starward link is marked as a placeholder",
                blocks.contains("Placeholder: the station replaces this hop"));
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
