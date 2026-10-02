package io.github.ezequiel24123z.grindless.assetgen;

import java.awt.image.BufferedImage;

/**
 * Draws machine casings and faces.
 *
 * <p>The visual direction is clean industrial futurism: dark metals, cyan energy accents, emissive
 * surfaces on anything powered, and a face that says what the machine does at a glance.
 *
 * <p>Machine blocks are composed rather than drawn whole — base plate, recessed panel, vents,
 * rivets, then an emissive overlay — so a new machine is a choice of panel motif rather than a new
 * texture, and a palette change is one edit rather than a hundred.
 */
public final class MachineTextures {

    public static final int SIZE = 16;

    /** The casing grey every machine shares. */
    public static final int CASING = 0x3A3F44;

    /** The cyan every powered surface uses. */
    public static final int ACCENT = 0x00E5FF;

    private MachineTextures() {
    }

    /**
     * A machine's front face.
     *
     * @param motif  what the recessed panel shows, which is how a player tells machines apart
     * @param active whether the indicator is lit
     */
    public static BufferedImage face(int casing, int accent, Motif motif, boolean active) {
        BufferedImage img = casingBase(casing);
        drawMotif(img, casing, accent, motif, active);
        if (active) {
            // An indicator strip along the bottom: visible from across a room, and the one
            // element every machine shares so "running" reads identically everywhere.
            for (int x = 5; x <= 10; x++) {
                set(img, x, 12, accent);
            }
            set(img, 4, 12, Palette.shade(accent, -0.4));
            set(img, 11, 12, Palette.shade(accent, -0.4));
        }
        return img;
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

    private static BufferedImage casingBase(int casing) {
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

    private static void drawMotif(BufferedImage img, int casing, int accent,
                                  Motif motif, boolean active) {
        int lit = active ? accent : Palette.shade(casing, -0.45);
        switch (motif) {
            case VENT -> {
                for (int y = 5; y <= 10; y++) {
                    for (int x = 4; x <= 11; x += 3) {
                        set(img, x, y, Palette.shade(casing, -0.5));
                    }
                }
            }
            case COIL -> {
                for (int x = 5; x <= 10; x++) {
                    set(img, x, 6, lit);
                    set(img, x, 9, lit);
                }
                set(img, 5, 7, Palette.shade(casing, -0.4));
                set(img, 10, 8, Palette.shade(casing, -0.4));
            }
            case APERTURE -> {
                for (int y = 6; y <= 9; y++) {
                    for (int x = 6; x <= 9; x++) {
                        boolean centre = x >= 7 && x <= 8 && y >= 7 && y <= 8;
                        set(img, x, y, centre ? lit : Palette.shade(casing, -0.45));
                    }
                }
            }
            case GAUGE -> {
                for (int x = 5; x <= 10; x++) {
                    set(img, x, 8, Palette.shade(casing, -0.5));
                }
                for (int x = 5; x <= 7; x++) {
                    set(img, x, 8, lit);
                }
            }
            case BLANK -> {
            }
        }
    }

    /** What a machine's panel shows. The one element that distinguishes machines from each
     * other, so it carries the recognisability the rest of the texture deliberately does not. */
    public enum Motif {
        BLANK,
        VENT,
        COIL,
        APERTURE,
        GAUGE
    }

    private static boolean isBorder(int x, int y) {
        return x == 0 || y == 0 || x == SIZE - 1 || y == SIZE - 1;
    }

    private static boolean inPanel(int x, int y) {
        return x >= 3 && x <= 12 && y >= 3 && y <= 12;
    }

    private static BufferedImage blank() {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static void set(BufferedImage img, int x, int y, int rgb) {
        img.setRGB(x, y, 0xFF000000 | rgb);
    }
}
