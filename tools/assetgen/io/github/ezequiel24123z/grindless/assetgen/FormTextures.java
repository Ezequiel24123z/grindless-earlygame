package io.github.ezequiel24123z.grindless.assetgen;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * The form axis of the item catalogue, drawn as geometry rather than as coloured pixels.
 *
 * <p>Each form describes only its <em>shape</em> — how thick the object is at each pixel — and
 * {@link HeightField} lights it. Relief, bevels, highlights and the outline all come from that one
 * shared pass, so every form and every material is lit identically, and improving the lighting
 * lifts the whole catalogue at once instead of one sprite at a time.
 *
 * <p>This is the half of the art problem a generator genuinely solves (ADR-0048). A pack with
 * forty materials needs forty plates differing only in palette: enormous by hand, trivial here,
 * and consistency <em>is</em> the quality. Hero items are the opposite and are not generated.
 *
 * <p>Every template is deterministic — the same palette always gives the same image — so
 * regenerating never produces a spurious diff.
 */
public final class FormTextures {

    public static final int SIZE = HeightField.SIZE;

    private FormTextures() {
    }

    /** A trapezoidal ingot: domed top face, chamfered sides, narrowing as it rises. */
    public static BufferedImage ingot(Palette palette) {
        HeightField field = new HeightField();
        for (int y = 5; y <= 11; y++) {
            // The silhouette narrows toward the top, so it reads as a cast trapezoid seen in
            // three-quarter view rather than as a brick.
            int inset = y <= 6 ? 4 : y <= 8 ? 3 : 2;
            for (int x = inset; x < SIZE - inset; x++) {
                // Doming across the width is what catches a highlight along the top face.
                double across = 1.0 - Math.abs((x - 7.5) / (SIZE / 2.0 - inset));
                double down = y <= 7 ? 1.0 : 1.0 - (y - 7) * 0.13;
                field.set(x, y, 3.0 + 2.2 * across * down);
            }
        }
        field.bevel(0.45);
        return field.light(palette);
    }

    /** A flat plate with a raised inner face and chamfered edges. */
    public static BufferedImage plate(Palette palette) {
        HeightField field = new HeightField();
        field.rect(2, 4, 13, 11, 3.2);
        // The raised panel reads as a rolled plate rather than a plain rectangle.
        field.rect(4, 6, 11, 9, 4.0);
        field.bevel(0.5);
        return field.light(palette);
    }

    /** A toothed ring with a bored centre and a raised hub. */
    public static BufferedImage gear(Palette palette) {
        HeightField field = new HeightField();
        double centre = (SIZE - 1) / 2.0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x - centre;
                double dy = y - centre;
                double radius = Math.hypot(dx, dy);
                double outer = 6.3 + Math.cos(Math.atan2(dy, dx) * 8);
                if (radius > outer) {
                    continue;
                }
                // The rim stands proud of the web, so teeth catch light and the body falls away.
                field.set(x, y, radius > outer - 1.8 ? 4.4 : 3.0);
            }
        }
        field.disc(centre, centre, 3.4, 4.6);
        field.clearDisc(centre, centre, 1.9);
        field.bevel(0.42);
        return field.light(palette);
    }

    /** A cylindrical rod, domed across its width so it reads as round. */
    public static BufferedImage rod(Palette palette) {
        HeightField field = new HeightField();
        for (int y = 2; y <= 13; y++) {
            for (int x = 6; x <= 9; x++) {
                double across = 1.0 - Math.abs((x - 7.5) / 2.0);
                field.set(x, y, 2.6 + 3.0 * Math.sqrt(Math.max(0, across)));
            }
        }
        field.bevel(0.3);
        return field.light(palette);
    }

    /** A hexagonal bolt head. */
    public static BufferedImage bolt(Palette palette) {
        HeightField field = new HeightField();
        double centre = (SIZE - 1) / 2.0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x - centre;
                double dy = y - centre;
                // A hexagon as the intersection of three slabs: cheaper than polygon clipping
                // and exact at this size.
                double a = Math.abs(dx);
                double b = Math.abs(dx * 0.5 + dy * 0.866);
                double c = Math.abs(dx * 0.5 - dy * 0.866);
                if (Math.max(a, Math.max(b, c)) <= 5.2) {
                    field.set(x, y, 3.4);
                }
            }
        }
        field.disc(centre, centre, 3.0, 4.6);
        field.bevel(0.45);
        return field.light(palette);
    }

    /** A ring: a torus with a clean bore. */
    public static BufferedImage ring(Palette palette) {
        HeightField field = new HeightField();
        double centre = (SIZE - 1) / 2.0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double radius = Math.hypot(x - centre, y - centre);
                if (radius <= 6.4 && radius >= 3.2) {
                    // Domed across the ring's thickness so the near edge catches the light.
                    double across = 1.0 - Math.abs((radius - 4.8) / 1.6);
                    field.set(x, y, 2.8 + 2.4 * Math.max(0, across));
                }
            }
        }
        field.bevel(0.35);
        return field.light(palette);
    }

    /** Fine powder: many small grains, nearly flat. */
    public static BufferedImage dust(Palette palette) {
        HeightField field = new HeightField();
        Random random = new Random(palette.base());
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x - 7.5;
                double dy = y - 8.5;
                if (dx * dx + dy * dy < 30 && random.nextDouble() < 0.60) {
                    field.set(x, y, 1.4 + random.nextDouble() * 0.8);
                }
            }
        }
        return field.light(palette);
    }

    /**
     * Ore rubble: fewer, chunkier, taller fragments than dust.
     *
     * <p>Crushed and dust are the two forms most easily confused in an inventory, so they are
     * deliberately pulled apart — rubble is lumpy and casts visible shadow, powder is fine and
     * nearly flat.
     */
    public static BufferedImage crushed(Palette palette) {
        HeightField field = new HeightField();
        Random random = new Random(palette.base() ^ 0x5CAB);
        for (int i = 0; i < 14; i++) {
            double cx = 3.5 + random.nextDouble() * 9;
            double cy = 4.5 + random.nextDouble() * 8;
            field.disc(cx, cy, 1.4 + random.nextDouble() * 1.3,
                    3.0 + random.nextDouble() * 2.4);
        }
        field.bevel(0.25);
        return field.light(palette);
    }

    /**
     * An unrefined lump straight from the extractor: a few large, rough chunks.
     *
     * <p>Sits between crushed (many small fragments) and an ingot (one clean shape), so the three
     * stages of the ore line read as three different objects at a glance.
     */
    public static BufferedImage raw(Palette palette) {
        HeightField field = new HeightField();
        Random random = new Random(palette.base() ^ 0x7A3F);
        double[][] lumps = {{6.0, 8.5, 3.6}, {10.2, 7.0, 3.0}, {8.0, 5.2, 2.4}};
        for (double[] lump : lumps) {
            double jitter = random.nextDouble() * 0.6;
            field.disc(lump[0] + jitter, lump[1], lump[2], 4.2 + random.nextDouble());
        }
        field.bevel(0.2);
        return field.light(palette);
    }

    /** A small cluster of nuggets. */
    public static BufferedImage nugget(Palette palette) {
        HeightField field = new HeightField();
        field.disc(7.0, 8.2, 2.6, 4.4);
        field.disc(9.6, 6.8, 1.9, 3.6);
        field.disc(5.4, 6.4, 1.5, 3.0);
        field.bevel(0.3);
        return field.light(palette);
    }

    /**
     * Placeholder sprite for the Data Core: a chip with a lit centre.
     *
     * <p>A stand-in, not art. Hero item sprites are a named gap in ADR-0048; this exists so the
     * item is recognisable and is not the purple-and-black missing-texture square.
     */
    public static BufferedImage dataCore(Palette palette) {
        HeightField field = new HeightField();
        field.rect(3, 3, 12, 12, 3.2);
        field.rect(5, 5, 10, 10, 4.2);
        field.disc(7.5, 7.5, 1.6, 5.2);
        for (int i = 4; i <= 11; i += 3) {
            field.rect(i, 1, i, 2, 2.6);
            field.rect(i, 13, i, 14, 2.6);
        }
        field.bevel(0.4);
        return field.light(palette);
    }

    /** Placeholder sprite for the Multitool: a handle with a head. See {@link #dataCore}. */
    public static BufferedImage multitool(Palette palette) {
        HeightField field = new HeightField();
        for (int y = 6; y <= 14; y++) {
            for (int x = 6; x <= 8; x++) {
                double across = 1.0 - Math.abs((x - 7.0) / 1.6);
                field.set(x, y, 2.8 + 2.0 * Math.max(0, across));
            }
        }
        field.rect(3, 2, 11, 5, 3.6);
        field.disc(7.0, 3.5, 2.2, 5.0);
        field.bevel(0.4);
        return field.light(palette);
    }
}
