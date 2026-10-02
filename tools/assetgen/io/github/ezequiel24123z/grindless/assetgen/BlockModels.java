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
            case PYLON -> pylon(block.tier());
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

    private static List<Box> terminal() {
        Box screen = tilted(front(2, 7, 2, 14, 15, 4, "top", "side", "front"), -22.5, new double[]{8, 7, 3});
        return List.of(
                box(1, 0, 2, 15, 7, 15, "top", "side"),
                box(0, 0, 0, 16, 3, 2, "cap", "base"),
                screen);
    }

    private static List<Box> pylon(int tier) {
        List<Box> boxes = new ArrayList<>();
        boxes.add(box(3, 0, 3, 13, 2, 13, "cap", "base"));
        int shaft = 10 + tier;
        boxes.add(box(6, 2, 6, 10, shaft, 10, "top", "side"));
        if (tier >= 2) {
            boxes.add(box(5, 5, 5, 11, 7, 11, "cap", "base"));
        }
        if (tier >= 3) {
            boxes.add(box(5, 9, 5, 11, 11, 11, "cap", "base"));
        }
        boxes.add(box(5, shaft, 5, 11, shaft + 3, 11, "top", "side"));
        return boxes;
    }
}
