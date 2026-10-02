package io.github.ezequiel24123z.grindless.assetgen;

import java.awt.image.BufferedImage;

/**
 * A 16×16 height map, and the lighting that turns it into a sprite with real relief.
 *
 * <h2>Why heights instead of drawing pixels</h2>
 *
 * <p>The first generator picked a colour per pixel from a three-tone palette, which produced flat
 * shapes: recognisable, but with no sense of a lit object. Shading them convincingly by hand would
 * mean hand-placing highlights on every form, and then re-doing it whenever a form changed.
 *
 * <p>Describing a form as a <em>height field</em> instead and lighting it afterwards inverts that.
 * Each form says only how thick it is at each pixel; one shared lighting pass derives the surface
 * normal and shades it. Relief comes out automatically, it is identical across every form and
 * material, and a change to the lighting improves all of them at once.
 *
 * <p>This is also why the forms read as the same object in different metals: they are the same
 * geometry under the same light, which is exactly the property the catalogue matrix needs
 * (ADR-0032).
 */
public final class HeightField {

    public static final int SIZE = 16;

    /** Light comes from the upper left, the pixel-art convention. */
    private static final double LIGHT_X = -0.55;
    private static final double LIGHT_Y = -0.70;
    private static final double LIGHT_Z = 0.45;

    private final double[][] height = new double[SIZE][SIZE];

    /** Sets the height at a pixel. Zero is empty; anything above it is solid. */
    public void set(int x, int y, double value) {
        if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
            height[y][x] = value;
        }
    }

    public double get(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) {
            return 0;
        }
        return height[y][x];
    }

    public boolean solid(int x, int y) {
        return get(x, y) > 0;
    }

    /** Fills a rectangle to a constant height. */
    public void rect(int x0, int y0, int x1, int y1, double value) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                set(x, y, value);
            }
        }
    }

    /**
     * Fills a disc, doming it toward the centre.
     *
     * <p>The dome is what makes a round form read as round rather than as a circle: a flat disc
     * lit from one side looks like a coin seen face-on, while a domed one catches a highlight.
     */
    public void disc(double cx, double cy, double radius, double peak) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double d = Math.hypot(x - cx, y - cy);
                if (d <= radius) {
                    set(x, y, peak * Math.sqrt(Math.max(0, 1 - (d / radius) * (d / radius))));
                }
            }
        }
    }

    /** Clears a disc, for bores and holes. */
    public void clearDisc(double cx, double cy, double radius) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (Math.hypot(x - cx, y - cy) <= radius) {
                    set(x, y, 0);
                }
            }
        }
    }

    /**
     * Rounds the edges of whatever is already filled.
     *
     * <p>A solid pixel next to an empty one is pulled down toward the gap, which is what gives a
     * form a bevel rather than a cliff edge. Bevels are most of the reason a sprite looks like an
     * object rather than a sticker.
     */
    public void bevel(double strength) {
        double[][] copy = new double[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            System.arraycopy(height[y], 0, copy[y], 0, SIZE);
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (copy[y][x] <= 0) {
                    continue;
                }
                int empty = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if ((dx != 0 || dy != 0) && !solidIn(copy, x + dx, y + dy)) {
                            empty++;
                        }
                    }
                }
                if (empty > 0) {
                    height[y][x] = copy[y][x] * (1 - strength * Math.min(1.0, empty / 4.0));
                }
            }
        }
    }

    private static boolean solidIn(double[][] field, int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && field[y][x] > 0;
    }

    /**
     * Lights this height field and returns the finished sprite.
     *
     * <p>The surface normal is taken from the height gradient, dotted with the light direction,
     * and the result picks a tone. Two touches matter more than the lighting itself:
     *
     * <ul>
     *   <li>a <b>dark outline</b> on the outermost pixels, which is what separates a sprite from
     *       whatever is behind it in an inventory;</li>
     *   <li>a <b>specular highlight</b> where the surface faces the light most directly, which is
     *       what makes metal look like metal rather than like painted wood.</li>
     * </ul>
     */
    public BufferedImage light(Palette palette) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!solid(x, y)) {
                    continue;
                }
                // Outermost pixels become the outline. Without it a sprite dissolves into a busy
                // inventory background.
                if (isEdge(x, y)) {
                    img.setRGB(x, y, 0xFF000000 | Palette.shade(palette.shadow(), -0.45));
                    continue;
                }

                // Central difference on the height field gives the surface slope.
                double dx = get(x + 1, y) - get(x - 1, y);
                double dy = get(x, y + 1) - get(x, y - 1);
                double nx = -dx;
                double ny = -dy;
                double nz = 1.0;
                double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
                double lambert = (nx * LIGHT_X + ny * LIGHT_Y + nz * LIGHT_Z) / len;

                int colour;
                if (lambert > 0.72) {
                    colour = Palette.shade(palette.highlight(), 0.25);
                } else if (lambert > 0.56) {
                    colour = palette.highlight();
                } else if (lambert > 0.42) {
                    colour = palette.base();
                } else if (lambert > 0.30) {
                    colour = Palette.shade(palette.base(), -0.18);
                } else {
                    colour = palette.shadow();
                }
                img.setRGB(x, y, 0xFF000000 | colour);
            }
        }
        return img;
    }

    /** Whether this solid pixel borders empty space on a cardinal side. */
    private boolean isEdge(int x, int y) {
        return !solid(x - 1, y) || !solid(x + 1, y) || !solid(x, y - 1) || !solid(x, y + 1);
    }
}
