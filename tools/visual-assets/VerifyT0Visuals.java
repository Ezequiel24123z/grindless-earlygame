package io.github.ezequiel24123z.grindless.assetgen;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Native writer/pixel proof; no game, loader, Python, network or art approval. */
public final class VerifyT0Visuals {
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: VerifyT0Visuals <baseline-root> <selective-stage-root>");
        }
        Path baseline = Path.of(args[0], "common", "src", "main", "resources", "assets", "grindless", "textures", "block");
        Path stage = Path.of(args[1], "common", "src", "main", "resources", "assets", "grindless", "textures", "block");
        Set<Path> expected = new HashSet<>();
        for (BlockCatalogue.Entry block : BlockCatalogue.blocks()) {
            if (!MachineArt.hasT0StateMark(block.geometry())) {
                continue;
            }
            Set<String> masks = new HashSet<>();
            for (String status : block.statuses()) {
                String name = block.lower() + "_front_" + status + ".png";
                Path path = stage.resolve(name);
                expected.add(path);
                BufferedImage actual = ImageIO.read(path.toFile());
                BufferedImage old = ImageIO.read(baseline.resolve(name).toFile());
                check(actual != null && actual.getWidth() == 16 && actual.getHeight() == 16, name + " native size");
                StringBuilder mask = new StringBuilder();
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        check((actual.getRGB(x, y) >>> 24) == 255, name + " opaque face");
                        if (y < 12 || y > 14 || x < 3 || x > 12) {
                            check(actual.getRGB(x, y) == old.getRGB(x, y), name + " unchanged mechanism/body");
                        }
                        if (y >= 12 && y <= 14 && x >= 4 && x <= 11) {
                            int value = actual.getRGB(x, y) & 0xFFFFFF;
                            check(value == 0xF1DEB8 || value == 0x171B1F, name + " high-contrast neutral glyph");
                            mask.append(value == 0xF1DEB8 ? '#' : '.');
                        }
                    }
                }
                check(masks.add(mask.toString()), block.name() + " distinct status shape without color");
            }
        }
        Set<List<Integer>> rolePixels = new HashSet<>();
        for (String role : WorkshopPortTextures.ROLES) {
            Path path = stage.resolve("port_" + role + ".png");
            expected.add(path);
            BufferedImage image = ImageIO.read(path.toFile());
            check(image != null && image.getWidth() == 16 && image.getHeight() == 16, role + " native size");
            java.util.ArrayList<Integer> pixels = new java.util.ArrayList<>();
            int visible = 0;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int pixel = image.getRGB(x, y);
                    pixels.add(pixel);
                    check(pixel == 0 || pixel == 0xFF29251F || pixel == 0xFFF1DEB8, role + " hard alpha/palette");
                    if (x == 0 || y == 0 || x == 15 || y == 15) {
                        check(pixel == 0, role + " transparent margin");
                    }
                    if (pixel != 0) {
                        visible++;
                    }
                }
            }
            check(visible >= 30 && visible < 160, role + " bounded role/direction footprint");
            check(rolePixels.add(pixels), role + " distinct role and direction without hue");
        }
        try (var files = Files.walk(Path.of(args[1]))) {
            check(files.filter(Files::isRegularFile).collect(java.util.stream.Collectors.toSet()).equals(expected),
                    "Selective mode writes only the T0 state fronts and four shared overlays");
        }
        System.out.println("PASS: " + checks + " T0 visual writer/pixel checks; human in-client review remains false.");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
