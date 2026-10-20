package io.github.ezequiel24123z.grindless.structure;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue.Geometry;

import java.util.ArrayList;
import java.util.List;

/** Original settling geometry shared by collision, generated models and assembled previews. */
public final class WorkshopGeometry {

    /** One box in Minecraft's 0..16 model units; null front means the ordinary side texture. */
    public record Box(double x0, double y0, double z0, double x1, double y1, double z1,
                      String top, String side, String front) {
    }

    private WorkshopGeometry() {
    }

    /** Returns the exact north-facing boxes; every segment has the same open bed and rail ends. */
    public static List<Box> boxes(Geometry geometry) {
        if (geometry != Geometry.SETTLER && geometry != Geometry.CHANNEL && geometry != Geometry.SCREEN) {
            throw new IllegalArgumentException("not a settling component: " + geometry);
        }
        List<Box> boxes = new ArrayList<>(List.of(
                new Box(1, 2, 0, 15, 4, 16, "#top", "#side", null),
                new Box(1, 4, 0, 3, 10, 16, "#cap", "#base", null),
                new Box(13, 4, 0, 15, 10, 16, "#cap", "#base", null)));
        if (geometry == Geometry.SETTLER) {
            boxes.add(new Box(3, 4, 0, 13, 12, 2, "#cap", "#base", "#front"));
            boxes.add(new Box(5, 0, 4, 11, 2, 12, "#cap", "#base", null));
        } else if (geometry == Geometry.SCREEN) {
            for (int x = 4; x <= 12; x += 2) {
                boxes.add(new Box(x, 4, 7, x + 1, 12, 9, "#top", "#side", null));
            }
        }
        return List.copyOf(boxes);
    }
}
