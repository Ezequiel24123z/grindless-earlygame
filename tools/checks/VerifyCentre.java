package io.github.ezequiel24123z.grindless.centre;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.flight.RocketFlight;
import io.github.ezequiel24123z.grindless.material.Material;
import io.github.ezequiel24123z.grindless.material.MaterialForm;
import io.github.ezequiel24123z.grindless.planet.PlanetCatalogue;
import io.github.ezequiel24123z.grindless.station.StationRide;
import io.github.ezequiel24123z.grindless.vein.VeinGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Behaviour checks for arrival at the galactic centre (ADR-0099). Not part of the mod. */
public final class VerifyCentre {

    private static final Path DATA = Path.of("common/src/main/resources/data");
    private static final Path ASSETS = Path.of("common/src/main/resources/assets/grindless");
    private static final Path SOURCES = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        room();
        veins();
        ride();
        dimension();
        blocks();
        noLink();

        System.out.println(failures == 0
                ? "ALL CENTRE CHECKS PASSED"
                : failures + " CENTRE CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void room() {
        List<CentreChamber.Cell> cells = CentreChamber.cells();
        int room = (CentreCatalogue.RADIUS * 2 + 1) * (CentreCatalogue.RADIUS * 2 + 1)
                * (CentreCatalogue.AIR_TOP - CentreCatalogue.AIR_BOTTOM);
        int shaft = (CentreCatalogue.SHAFT * 2 + 1) * (CentreCatalogue.SHAFT * 2 + 1)
                * (CentreCatalogue.HEIGHT - CentreCatalogue.AIR_TOP);
        eq("the carve is the room plus the shaft", room + shaft, cells.size());
        yes("and that is a room, not a world", cells.size() < 400);
        Set<String> seen = new HashSet<>();
        int air = 0;
        int berths = 0;
        int marks = 0;
        boolean bounded = true;
        for (CentreChamber.Cell cell : cells) {
            bounded &= Math.abs(cell.x()) <= CentreCatalogue.RADIUS;
            bounded &= Math.abs(cell.z()) <= CentreCatalogue.RADIUS;
            bounded &= cell.y() >= CentreCatalogue.AIR_BOTTOM && cell.y() < CentreCatalogue.HEIGHT;
            bounded &= cell.kind() == CentreChamber.kindAt(cell.x(), cell.y(), cell.z());
            seen.add(cell.x() + "," + cell.y() + "," + cell.z());
            switch (cell.kind()) {
                case AIR -> air++;
                case BERTH -> berths++;
                case MARK -> marks++;
            }
        }
        yes("every carved cell stays inside the mass", bounded);
        eq("no cell is listed twice", cells.size(), seen.size());
        eq("one berth", 1, berths);
        eq("one mark", 1, marks);
        eq("the rest is air", cells.size() - 2, air);
        eq("the rider stands in air", CentreChamber.Kind.AIR,
                CentreChamber.kindAt(CentreCatalogue.STAND_X, CentreCatalogue.STAND_Y, CentreCatalogue.STAND_Z));
        eq("the berth is the berth", CentreChamber.Kind.BERTH,
                CentreChamber.kindAt(CentreCatalogue.BERTH_X, CentreCatalogue.BERTH_Y, CentreCatalogue.BERTH_Z));
        eq("the mark is the mark", CentreChamber.Kind.MARK,
                CentreChamber.kindAt(CentreCatalogue.MARK_X, CentreCatalogue.MARK_Y, CentreCatalogue.MARK_Z));
        yes("the floor under the rider stays shell",
                CentreChamber.kindAt(CentreCatalogue.STAND_X, CentreCatalogue.STAND_Y - 1, CentreCatalogue.STAND_Z) == null);
        yes("the wall outside the room stays shell",
                CentreChamber.kindAt(CentreCatalogue.RADIUS + 1, CentreCatalogue.AIR_BOTTOM, 0) == null);
        yes("the mass beside the shaft stays shell",
                CentreChamber.kindAt(CentreCatalogue.SHAFT + 1, CentreCatalogue.AIR_TOP, 0) == null);
        boolean climb = true;
        for (int y = CentreCatalogue.BERTH_Y + 1; y < CentreCatalogue.HEIGHT; y++) {
            climb &= CentreChamber.kindAt(CentreCatalogue.BERTH_X, y, CentreCatalogue.BERTH_Z) == CentreChamber.Kind.AIR;
        }
        yes("the ride home has air above the berth", climb);
        eq("the shaft reaches the top of the mass", CentreChamber.Kind.AIR,
                CentreChamber.kindAt(0, CentreCatalogue.HEIGHT - 1, 0));
        eq("the mark lights the room", 15, CentreCatalogue.MARK_LIGHT);
    }

    private static void veins() {
        List<Material> pack = List.of(
                new Material("iron", EnumSet.of(MaterialForm.ORE, MaterialForm.INGOT), 200));
        yes("the centre has no vein",
                PlanetCatalogue.veins(CentreCatalogue.SAGITTARIUS, pack).isEmpty());
        yes("an empty pool derives nothing",
                VeinGenerator.generate(11L, 0, 0, PlanetCatalogue.veins(CentreCatalogue.SAGITTARIUS, pack)) == null);
        yes("the centre emits no reagent",
                PlanetCatalogue.extractable(CentreCatalogue.SAGITTARIUS, "iron") == null);
        yes("the overworld keeps the pack list",
                PlanetCatalogue.veins("minecraft:overworld", pack) == pack);
        no("the centre is not the drift", CentreCatalogue.isCentre("grindless:drift"));
        eq("the mass is sixteen blocks tall", 16, CentreCatalogue.HEIGHT);
    }

    private static void ride() {
        eq("the station's hole is this dimension", CentreCatalogue.SAGITTARIUS, StationRide.HOLE);
        no("a rocket does not list the centre", RocketFlight.isSite(CentreCatalogue.SAGITTARIUS));
        no("a rocket does not launch there", RocketFlight.canLaunch(CentreCatalogue.SAGITTARIUS));
        eq("a rocket draws nothing because it does not fly", -1L, RocketFlight.toll(CentreCatalogue.SAGITTARIUS));
    }

    private static void dimension() throws IOException {
        JsonObject dimension = read(DATA.resolve("grindless/dimension/sagittarius.json"));
        JsonObject settings = dimension.getAsJsonObject("generator").getAsJsonObject("settings");
        eq("the dimension uses the centre biome", "grindless:sagittarius", settings.get("biome").getAsString());
        no("the dimension has no lakes", settings.get("lakes").getAsBoolean());
        no("the dimension has no features", settings.get("features").getAsBoolean());
        int height = 0;
        JsonArray layers = settings.getAsJsonArray("layers");
        eq("one layer entry", 1, layers.size());
        JsonObject layer = layers.get(0).getAsJsonObject();
        eq("that layer is the shell", CentreCatalogue.SHELL, layer.get("block").getAsString());
        height = layer.get("height").getAsInt();
        eq("the layer fills the mass", CentreCatalogue.HEIGHT, height);
        String dimensionText = Files.readString(DATA.resolve("grindless/dimension/sagittarius.json"));
        no("the dimension does not name an ore", dimensionText.contains("ore"));
        no("the dimension does not name stone", dimensionText.contains("stone"));
        no("the dimension does not name regolith", dimensionText.contains("regolith"));
        no("the dimension does not name helium", dimensionText.contains("helium"));
        no("the dimension does not name a deck", dimensionText.contains("deck"));
        JsonObject type = read(DATA.resolve("grindless/dimension_type/sagittarius.json"));
        eq("the type is one section", CentreCatalogue.HEIGHT, type.get("height").getAsInt());
        eq("the type starts at zero", 0, type.get("min_y").getAsInt());
        no("the centre is not ultrawarm", type.get("ultrawarm").getAsBoolean());
        no("the centre has no skylight", type.get("has_skylight").getAsBoolean());
        yes("the centre has a ceiling", type.get("has_ceiling").getAsBoolean());
        no("a bed does not set spawn in the mass", type.get("bed_works").getAsBoolean());
        JsonObject biome = read(DATA.resolve("grindless/worldgen/biome/sagittarius.json"));
        no("the centre has no rain", biome.get("has_precipitation").getAsBoolean());
        yes("the centre spawns nothing", biome.getAsJsonObject("spawners").getAsJsonArray("monster").isEmpty());
    }

    /** The mass and the mark render, drop nothing, and are not items. */
    private static void blocks() throws IOException {
        for (String name : List.of("horizon_shell", "arrival_mark")) {
            yes(name + " has a blockstate",
                    Files.isRegularFile(ASSETS.resolve("blockstates/" + name + ".json")));
            yes(name + " has an idle model",
                    Files.isRegularFile(ASSETS.resolve("models/block/" + name + "_idle.json")));
            no(name + " has no item model",
                    Files.isRegularFile(ASSETS.resolve("models/item/" + name + ".json")));
            String loot = Files.readString(DATA.resolve("grindless/loot_tables/blocks/" + name + ".json"));
            yes(name + " drops nothing", loot.contains("\"pools\": []"));
            no(name + " loot does not name itself", loot.contains("\"grindless:" + name + "\""));
        }
        String pickaxe = Files.readString(DATA.resolve("minecraft/tags/blocks/mineable/pickaxe.json"));
        yes("the shell is mineable with a pickaxe", pickaxe.contains("\"grindless:horizon_shell\""));
        yes("the mark is mineable with a pickaxe", pickaxe.contains("\"grindless:arrival_mark\""));
        String lang = Files.readString(ASSETS.resolve("lang/en_us.json"));
        yes("the shell is named", lang.contains("\"block.grindless.horizon_shell\""));
        yes("the mark is named", lang.contains("\"block.grindless.arrival_mark\""));
        yes("arrival says it is the victory", lang.contains("\"chat.grindless.station.centre\""));
        String blocks = Files.readString(SOURCES.resolve("registry/ModBlocks.java"));
        yes("the shell is registered without an item helper",
                blocks.contains("BLOCKS.register(\"horizon_shell\""));
        yes("the mark is registered without an item helper",
                blocks.contains("BLOCKS.register(\"arrival_mark\""));
        no("there is no crafting recipe for the shell",
                Files.isRegularFile(DATA.resolve("grindless/recipes/horizon_shell.json")));
        no("there is no crafting recipe for the mark",
                Files.isRegularFile(DATA.resolve("grindless/recipes/arrival_mark.json")));
    }

    /** The way in is the station. The link still does not move a player. */
    private static void noLink() throws IOException {
        String link = Files.readString(SOURCES.resolve("star/StarwardLinkBlockEntity.java"));
        String travel = Files.readString(SOURCES.resolve("station/StationTravel.java"));
        no("the link does not name the centre", link.contains("sagittarius"));
        no("the link entity does not depart", link.contains("StarwardTravel.depart"));
        yes("the station carves the chamber", travel.contains("CentreChamber.carve"));
        no("the station does not open a landing map", travel.contains("LandingMap"));
    }

    private static JsonObject read(Path file) throws IOException {
        JsonElement json = JsonParser.parseString(Files.readString(file));
        return json.getAsJsonObject();
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
