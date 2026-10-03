package io.github.ezequiel24123z.grindless.assetgen;

import java.awt.image.BufferedImage;

/**
 * Draws machine casings and faces.
 *
 * <p>The visual direction is clean industrial futurism: dark metals, cyan energy accents, emissive
 * surfaces on anything powered, and a face that says what the machine does at a glance.
 *
 * <p>Holds the shared casing pieces. What distinguishes one machine from another, and one status
 * from another, lives in {@link MachineArt}.
 */
public final class MachineTextures {

    public static final int SIZE = 16;

    /** The casing grey every machine shares. */
    public static final int CASING = 0x3A3F44;

    /** The cyan every powered surface uses. */
    public static final int ACCENT = 0x00E5FF;

    private MachineTextures() {
    }

    /** A plain casing side: horizontal ribbing, bevelled border. */
    public static BufferedImage side(int casing) {
        BufferedImage img = blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int colour = y % 4 == 0 ? Palette.shade(casing, -0.25) : casing;
                if (isBorder(x, y)) {
                    colour = Palette.shade(casing, -0.35);
                }
                set(img, x, y, colour);
            }
        }
        return img;
    }

    /** A casing top: a panel with corner bolts. */
    public static BufferedImage top(int casing) {
        BufferedImage img = blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int colour = isBorder(x, y) ? Palette.shade(casing, -0.35) : casing;
                if ((x == 3 || x == 12) && (y == 3 || y == 12)) {
                    colour = Palette.shade(casing, 0.25);
                }
                set(img, x, y, colour);
            }
        }
        return img;
    }

    static BufferedImage casingBase(int casing) {
        BufferedImage img = blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int colour = casing;
                if (isBorder(x, y)) {
                    colour = Palette.shade(casing, -0.35);
                } else if (inPanel(x, y)) {
                    colour = Palette.shade(casing, -0.2);
                }
                // Rivets do more for "industrial" than any amount of gradient.
                if ((x == 2 || x == 13) && (y == 2 || y == 13)) {
                    colour = Palette.shade(casing, 0.3);
                }
                set(img, x, y, colour);
            }
        }
        return img;
    }

    static boolean isBorder(int x, int y) {
        return x == 0 || y == 0 || x == SIZE - 1 || y == SIZE - 1;
    }

    static boolean inPanel(int x, int y) {
        return x >= 3 && x <= 12 && y >= 3 && y <= 12;
    }

    static BufferedImage blank() {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    static void set(BufferedImage img, int x, int y, int rgb) {
        img.setRGB(x, y, 0xFF000000 | rgb);
    }
}
