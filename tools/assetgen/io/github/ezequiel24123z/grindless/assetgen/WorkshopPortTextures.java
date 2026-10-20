package io.github.ezequiel24123z.grindless.assetgen;

import java.awt.image.BufferedImage;
import java.util.List;

/** Original neutral role/direction markings over an independently selected structural family. */
public final class WorkshopPortTextures {

    public static final List<String> ROLES = List.of("item_input", "item_output", "fluid_input", "fluid_output");

    private WorkshopPortTextures() {
    }

    public static BufferedImage overlay(String role) {
        if (!ROLES.contains(role)) {
            throw new IllegalArgumentException("Unknown workshop port role: " + role);
        }
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        String[] symbol = role.startsWith("item")
                ? new String[]{"#####", "#...#", "#.#.#", "#...#", "#####"}
                : new String[]{"..#..", ".###.", "#####", "#####", ".###."};
        for (int y = 0; y < symbol.length; y++) {
            for (int x = 0; x < symbol[y].length(); x++) {
                if (symbol[y].charAt(x) == '#') {
                    outlinePixel(image, x + 3, y + 4);
                }
            }
        }
        boolean input = role.endsWith("input");
        String[] arrow = input
                ? new String[]{".#...", "#####", ".#..."}
                : new String[]{"...#.", "#####", "...#."};
        for (int y = 0; y < arrow.length; y++) {
            for (int x = 0; x < arrow[y].length(); x++) {
                if (arrow[y].charAt(x) == '#') {
                    outlinePixel(image, x + 8, y + 5);
                }
            }
        }
        return image;
    }

    private static void outlinePixel(BufferedImage image, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((image.getRGB(x + dx, y + dy) >>> 24) == 0) {
                    image.setRGB(x + dx, y + dy, 0xFF29251F);
                }
            }
        }
        image.setRGB(x, y, 0xFFF1DEB8);
    }
}
