package io.github.ezequiel24123z.grindless.assetgen;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Block model JSON, built from boxes.
 *
 * <p>Each machine is a handful of boxes, which is what gives it a silhouette of its own instead of
 * a textured cube. All fronts face north (negative Z); the blockstate rotates the model for the
 * other facings, so the geometry is written once.
 *
 * <p>Texture slots: {@code #front} (the status-dependent face), {@code #top}, {@code #side},
 * {@code #base} and {@code #cap} (the shared casing, which does not change with status).
 */
final class BlockModels {

    private BlockModels() {
    }

    /** One box. A face is drawn only if a texture slot is given for it. */
    private record Box(double[] from, double[] to, String up, String down, String north, String south,
                       String east, String west, boolean fullFrontUv, double[] rotation) {
    }

    private static Box box(double x0, double y0, double z0, double x1, double y1, double z1,
                           String top, String sides) {
        return new Box(new double[]{x0, y0, z0}, new double[]{x1, y1, z1},
                top, top, sides, sides, sides, sides, false, null);
    }

    /** A box whose north face shows a texture in full, not the slice its position implies. */
    private static Box front(double x0, double y0, double z0, double x1, double y1, double z1,
                             String top, String sides, String north) {
        return new Box(new double[]{x0, y0, z0}, new double[]{x1, y1, z1},
                top, top, north, sides, sides, sides, true, null);
    }

    private static Box tilted(Box box, double angle, double[] origin) {
        double[] rotation = {angle, origin[0], origin[1], origin[2]};
        return new Box(box.from, box.to, box.up, box.down, box.north, box.south, box.east, box.west,
                box.fullFrontUv, rotation);
    }

    static String model(BlockCatalogue.Entry block, String status) {
        List<Box> boxes = switch (block.geometry()) {
            case DYNAMO -> dynamo();
            case EXTRACTOR -> extractor();
            case TERMINAL -> terminal();
            case GENERATOR -> generator();
            case MILL -> mill();
            case FURNACE -> furnace();
            case PRESS -> press();
            case ASSEMBLER -> assembler();
            case KILN -> kiln();
            case PYLON -> pylonPiece(block.tier(), 0);
            case BELT -> belt();
            case SPLITTER -> splitter();
            case MERGER -> merger();
            case TUNNEL -> tunnel();
            case OVERFLOW -> overflow();
            case SORTER -> sorter();
            case MANIPULATOR -> manipulator();
            case DRILL -> drill();
            case CONDUIT -> conduit();
            case PUMP -> pump();
            case TANK -> tank();
            case BANK -> bank();
            case TRANSFORMER -> transformer();
        };
        StringBuilder out = new StringBuilder("{\n");
        out.append("  \"textures\": {\n");
        out.append(textures(block, status));
        out.append("  },\n  \"elements\": [\n");
        for (int i = 0; i < boxes.size(); i++) {
            out.append(element(boxes.get(i)));
            out.append(i + 1 < boxes.size() ? ",\n" : "\n");
        }
        out.append("  ]\n}\n");
        return out.toString();
    }

    /** The middle or top third of a pylon, as its own model sitting in 0–16. */
    static String pylonPart(int tier, int piece, String status) {
        return assembled(pylonPiece(tier, piece), pylonTextures(tier, status));
    }

    /** A full tower squashed into one block, for the item. JSON cannot extend past y=32. */
    static String pylonItem(int tier) {
        return assembled(pylonScaled(tier), pylonTextures(tier, "idle"));
    }

    private static String assembled(List<Box> boxes, String textures) {
        StringBuilder out = new StringBuilder("{\n");
        out.append("  \"textures\": {\n");
        out.append(textures);
        out.append("  },\n  \"elements\": [\n");
        for (int i = 0; i < boxes.size(); i++) {
            out.append(element(boxes.get(i)));
            out.append(i + 1 < boxes.size() ? ",\n" : "\n");
        }
        out.append("  ]\n}\n");
        return out.toString();
    }

    private static String pylonTextures(int tier, String status) {
        List<String> lines = new ArrayList<>();
        lines.add(slot("base", "casing_side"));
        lines.add(slot("cap", "casing_top"));
        lines.add(slot("side", "pylon" + tier + "_side_" + status));
        lines.add(slot("top", "pylon" + tier + "_top_" + status));
        lines.add(slot("particle", "pylon" + tier + "_side_" + status));
        return String.join(",\n", lines) + "\n";
    }

    private static String textures(BlockCatalogue.Entry block, String status) {
        List<String> lines = new ArrayList<>();
        String lower = block.lower();
        lines.add(slot("base", "casing_side"));
        lines.add(slot("cap", "casing_top"));
        if (block.geometry() == BlockCatalogue.Geometry.PYLON) {
            lines.add(slot("side", "pylon" + block.tier() + "_side_" + status));
            lines.add(slot("top", "pylon" + block.tier() + "_top_" + status));
            lines.add(slot("particle", "pylon" + block.tier() + "_side_" + status));
        } else {
            lines.add(slot("front", lower + "_front_" + status));
            lines.add(slot("top", lower + "_top"));
            lines.add(slot("side", "casing_side"));
            lines.add(slot("particle", "casing_side"));
        }
        return String.join(",\n", lines) + "\n";
    }

    private static String slot(String name, String texture) {
        return "    \"" + name + "\": \"grindless:block/" + texture + "\"";
    }

    private static String element(Box b) {
        StringBuilder out = new StringBuilder();
        out.append("    {\n      \"from\": ").append(vec(b.from)).append(",\n      \"to\": ").append(vec(b.to));
        if (b.rotation != null) {
            out.append(",\n      \"rotation\": { \"angle\": ").append(num(b.rotation[0]))
                    .append(", \"axis\": \"x\", \"origin\": ")
                    .append(vec(new double[]{b.rotation[1], b.rotation[2], b.rotation[3]})).append(" }");
        }
        out.append(",\n      \"faces\": {\n");
        List<String> faces = new ArrayList<>();
        faces.add(face("up", b.up, false));
        faces.add(face("down", b.down, false));
        faces.add(face("north", b.north, b.fullFrontUv));
        faces.add(face("south", b.south, false));
        faces.add(face("east", b.east, false));
        faces.add(face("west", b.west, false));
        out.append(String.join(",\n", faces)).append("\n      }\n    }");
        return out.toString();
    }

    private static String face(String direction, String slot, boolean fullUv) {
        String uv = fullUv ? ", \"uv\": [0, 0, 16, 16]" : "";
        return "        \"" + direction + "\": { \"texture\": \"#" + slot + "\"" + uv + " }";
    }

    private static String vec(double[] v) {
        return "[" + num(v[0]) + ", " + num(v[1]) + ", " + num(v[2]) + "]";
    }

    private static String num(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.format(Locale.ROOT, "%s", value);
    }

    // ---- Geometries --------------------------------------------------------------------

    private static List<Box> dynamo() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "cap", "base"),
                front(1, 2, 2, 15, 11, 15, "top", "side", "front"),
                box(4, 11, 5, 12, 14, 13, "top", "side"),
                box(7, 5, -2, 9, 7, 2, "cap", "cap"),
                box(7, 7, -2, 9, 12, -1, "cap", "cap"),
                box(6, 11, -3, 10, 13, 0, "cap", "cap"));
    }

    private static List<Box> extractor() {
        return List.of(
                box(0, 0, 0, 16, 3, 16, "cap", "base"),
                front(1, 3, 1, 15, 11, 15, "top", "side", "front"),
                box(2, 11, 2, 14, 14, 14, "top", "side"),
                box(10, 14, 10, 13, 16, 13, "cap", "base"),
                box(5, 0, -1, 11, 3, 1, "cap", "cap"));
    }

    /** A squat boiler with a chimney on the back-left. */
    private static List<Box> generator() {
        return List.of(
                box(0, 0, 0, 16, 3, 16, "cap", "base"),
                front(1, 3, 2, 15, 12, 15, "top", "side", "front"),
                box(2, 12, 3, 10, 14, 13, "top", "side"),
                box(3, 14, 10, 7, 16, 14, "cap", "base"));
    }

    /** Two rollers over a hopper mouth. */
    private static List<Box> mill() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "cap", "base"),
                front(1, 2, 1, 15, 10, 15, "top", "side", "front"),
                box(2, 10, 3, 7, 14, 13, "top", "side"),
                box(9, 10, 3, 14, 14, 13, "top", "side"),
                box(5, 0, -1, 11, 3, 1, "cap", "cap"));
    }

    /** A chamber with a V of electrodes on top. */
    private static List<Box> furnace() {
        return List.of(
                box(0, 0, 0, 16, 3, 16, "cap", "base"),
                front(1, 3, 2, 15, 12, 15, "top", "side", "front"),
                box(3, 12, 6, 6, 16, 10, "cap", "cap"),
                box(10, 12, 6, 13, 16, 10, "cap", "cap"),
                box(5, 14, 7, 11, 16, 9, "top", "side"));
    }

    /** A ram over a forming bed. */
    private static List<Box> press() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "cap", "base"),
                front(1, 2, 2, 15, 8, 15, "top", "side", "front"),
                box(3, 8, 4, 13, 14, 14, "top", "side"),
                box(5, 14, 6, 11, 16, 12, "cap", "cap"));
    }

    /** Two arms over a fabrication deck. */
    private static List<Box> assembler() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "cap", "base"),
                front(1, 2, 2, 15, 10, 15, "top", "side", "front"),
                box(2, 10, 3, 6, 15, 7, "top", "side"),
                box(10, 10, 3, 14, 15, 7, "top", "side"),
                box(6, 11, 8, 10, 13, 14, "cap", "cap"));
    }

    /** A brick drum with a short stack. */
    private static List<Box> kiln() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "cap", "base"),
                front(2, 2, 2, 14, 12, 14, "top", "side", "front"),
                box(5, 12, 5, 11, 16, 11, "cap", "cap"));
    }

    /** A low slab with two raised rails. */
    private static List<Box> belt() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "top", "base"),
                box(0, 2, 0, 2, 4, 16, "top", "side"),
                box(14, 2, 0, 16, 4, 16, "top", "side"),
                front(2, 2, 0, 14, 4, 16, "top", "side", "front"));
    }

    /** A slightly taller junction with three mouths. */
    private static List<Box> splitter() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "top", "base"),
                front(1, 2, 1, 15, 6, 15, "top", "side", "front"),
                box(6, 6, 0, 10, 7, 4, "top", "side"),
                box(0, 6, 6, 4, 7, 10, "top", "side"),
                box(12, 6, 6, 16, 7, 10, "top", "side"));
    }

    /** Three inlets, one outlet. */
    private static List<Box> merger() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "top", "base"),
                front(2, 2, 2, 14, 6, 14, "top", "side", "front"),
                box(6, 6, 0, 10, 7, 4, "top", "side"),
                box(0, 4, 6, 3, 6, 10, "top", "side"),
                box(13, 4, 6, 16, 6, 10, "top", "side"));
    }

    /** A low mouth that sits flush so a wall can stand on it. */
    private static List<Box> tunnel() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "top", "base"),
                box(0, 2, 0, 3, 4, 16, "top", "side"),
                box(13, 2, 0, 16, 4, 16, "top", "side"),
                front(3, 2, 0, 13, 4, 8, "top", "side", "front"));
    }

    /** Front lane plus a clockwise dump. */
    private static List<Box> overflow() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "top", "base"),
                front(2, 2, 1, 14, 5, 14, "top", "side", "front"),
                box(12, 5, 6, 16, 6, 10, "top", "side"));
    }

    /** Passthrough with two side mouths. */
    private static List<Box> sorter() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "top", "base"),
                front(2, 2, 2, 14, 6, 14, "top", "side", "front"),
                box(0, 4, 6, 3, 7, 10, "top", "side"),
                box(13, 4, 6, 16, 7, 10, "top", "side"),
                box(6, 6, 6, 10, 7, 10, "cap", "cap"));
    }

    /** A post with a reaching arm. */
    private static List<Box> manipulator() {
        return List.of(
                box(5, 0, 5, 11, 2, 11, "cap", "base"),
                box(7, 2, 7, 9, 10, 9, "top", "side"),
                front(4, 8, 2, 12, 11, 8, "top", "side", "front"),
                box(6, 9, 0, 10, 11, 2, "cap", "cap"));
    }

    /** An open trough: liquid sits in the channel, walls keep it in. */
    private static List<Box> conduit() {
        return List.of(
                box(0, 0, 0, 16, 2, 16, "top", "base"),
                box(0, 2, 0, 2, 6, 16, "top", "side"),
                box(14, 2, 0, 16, 6, 16, "top", "side"),
                front(2, 2, 0, 14, 5, 16, "top", "side", "front"));
    }

    /** A well with a piston and a handle on the front. */
    private static List<Box> pump() {
        return List.of(
                box(2, 0, 2, 14, 2, 14, "cap", "base"),
                front(3, 2, 3, 13, 10, 13, "top", "side", "front"),
                box(6, 10, 6, 10, 14, 10, "top", "side"),
                box(5, 12, 1, 11, 15, 5, "cap", "cap"));
    }

    /** A squat cylinder with a cap; the front is the sight glass. */
    private static List<Box> tank() {
        return List.of(
                box(2, 0, 2, 14, 2, 14, "cap", "base"),
                front(3, 2, 3, 13, 13, 13, "top", "side", "front"),
                box(4, 13, 4, 12, 16, 12, "cap", "cap"));
    }

    /** A cabinet of cells: a battery rack, not a tank and not a pylon. */
    private static List<Box> bank() {
        return List.of(
                box(1, 0, 1, 15, 2, 15, "cap", "base"),
                front(2, 2, 2, 14, 14, 14, "top", "side", "front"),
                box(3, 14, 3, 7, 16, 13, "cap", "cap"),
                box(9, 14, 3, 13, 16, 13, "cap", "cap"));
    }

    /** Two coils on a core, with a front face so the tap has a direction. */
    private static List<Box> transformer() {
        return List.of(
                box(1, 0, 2, 15, 3, 14, "cap", "base"),
                front(5, 3, 5, 11, 14, 11, "top", "side", "front"),
                box(1, 4, 4, 5, 13, 12, "top", "side"),
                box(11, 4, 4, 15, 13, 12, "top", "side"));
    }

    /** A heavier bore: wider collar and a deeper stack than the Crude Extractor. */
    private static List<Box> drill() {
        return List.of(
                box(0, 0, 0, 16, 3, 16, "cap", "base"),
                front(1, 3, 1, 15, 12, 15, "top", "side", "front"),
                box(3, 12, 3, 13, 15, 13, "top", "side"),
                box(6, 15, 6, 10, 16, 10, "cap", "base"),
                box(4, 0, -1, 12, 3, 1, "cap", "cap"));
    }

    private static List<Box> terminal() {
        Box screen = tilted(front(2, 7, 2, 14, 15, 4, "top", "side", "front"), -22.5, new double[]{8, 7, 3});
        return List.of(
                box(1, 0, 2, 15, 7, 15, "top", "side"),
                box(0, 0, 0, 16, 3, 2, "cap", "base"),
                screen);
    }

    /**
     * A three-block-tall tower in 0–48 space, sliced per piece into 0–16 (ADR-0054). JSON models
     * cannot extend past 32 on an axis, so the middle and top live on their own blocks.
     */
    private static List<Box> pylonFull(int tier) {
        int inset = 7 - tier;
        List<Box> boxes = new ArrayList<>();
        boxes.add(box(2, 0, 2, 14, 3, 14, "cap", "base"));
        boxes.add(box(inset, 3, inset, 16 - inset, 42, 16 - inset, "top", "side"));
        for (int r = 0; r < tier; r++) {
            int y = 12 + r * 10;
            boxes.add(box(inset - 1, y, inset - 1, 16 - inset + 1, y + 2, 16 - inset + 1, "cap", "base"));
        }
        boxes.add(box(inset - 1, 42, inset - 1, 16 - inset + 1, 48, 16 - inset + 1, "top", "side"));
        return boxes;
    }

    private static List<Box> pylonPiece(int tier, int piece) {
        double y0 = piece * 16.0;
        double y1 = y0 + 16.0;
        List<Box> sliced = new ArrayList<>();
        for (Box box : pylonFull(tier)) {
            if (box.to[1] <= y0 || box.from[1] >= y1) {
                continue;
            }
            double fromY = Math.max(box.from[1], y0) - y0;
            double toY = Math.min(box.to[1], y1) - y0;
            if (toY - fromY < 0.05) {
                continue;
            }
            sliced.add(new Box(
                    new double[]{box.from[0], fromY, box.from[2]},
                    new double[]{box.to[0], toY, box.to[2]},
                    box.up, box.down, box.north, box.south, box.east, box.west,
                    box.fullFrontUv, box.rotation));
        }
        return sliced;
    }

    private static List<Box> pylonScaled(int tier) {
        List<Box> scaled = new ArrayList<>();
        for (Box box : pylonFull(tier)) {
            scaled.add(new Box(
                    new double[]{box.from[0], box.from[1] / 3.0, box.from[2]},
                    new double[]{box.to[0], box.to[1] / 3.0, box.to[2]},
                    box.up, box.down, box.north, box.south, box.east, box.west,
                    box.fullFrontUv, box.rotation));
        }
        return scaled;
    }
}
