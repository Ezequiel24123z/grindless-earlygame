package io.github.ezequiel24123z.grindless.assetgen;

import java.awt.image.BufferedImage;

import static io.github.ezequiel24123z.grindless.assetgen.MachineTextures.CASING;
import static io.github.ezequiel24123z.grindless.assetgen.MachineTextures.SIZE;
import static io.github.ezequiel24123z.grindless.assetgen.MachineTextures.casingBase;
import static io.github.ezequiel24123z.grindless.assetgen.MachineTextures.set;

/**
 * Each machine's own faces, in each status (ADR-0052).
 *
 * <p>Two things vary independently and are kept apart on purpose. <b>Which machine</b> is carried
 * by shape and by the machine's own accent colour: a flywheel, a drill aperture, a screen, a
 * coil. <b>Which status</b> is carried by a colour that means the same on every machine: cyan is
 * working, amber is blocked, red is starved. A player who has learned the colours can read a
 * machine they have never seen; one who has learned the shapes can tell machines apart in the dark.
 */
public final class MachineArt {

    public static final int RUNNING = 0x00E5FF;
    public static final int BLOCKED = 0xFFB300;
    public static final int STARVED = 0xFF3D00;

    /** Per-machine accents, deliberately none of the three status colours. */
    public static final int DYNAMO = 0x76FF03;
    public static final int EXTRACTOR = 0xFF8A00;
    public static final int TERMINAL = 0xB388FF;
    public static final int[] PYLON = {0x18FFFF, 0x69F0AE, 0xE040FB};

    private static final double CX = 7.5;
    private static final double CY = 7.5;

    private MachineArt() {
    }

    /** The colour a status paints with, or {@code -1} for idle. */
    static int statusColour(String status, int accent) {
        return switch (status) {
            case "running" -> accent;
            case "blocked" -> BLOCKED;
            case "starved" -> STARVED;
            default -> -1;
        };
    }

    private static int dim(int rgb) {
        return Palette.shade(rgb, -0.7);
    }

    /** The status strip every front shares, so "running" reads identically on every machine. */
    private static void strip(BufferedImage img, String status) {
        int dark = Palette.shade(CASING, -0.5);
        for (int x = 4; x <= 11; x++) {
            int colour = switch (status) {
                case "running" -> RUNNING;
                case "blocked" -> (x % 2 == 0) ? BLOCKED : dark;
                case "starved" -> (x <= 5 || x == 9) ? STARVED : dark;
                default -> dark;
            };
            set(img, x, 13, colour);
        }
    }

    // ---- Hand Crank Dynamo ---------------------------------------------------------------

    /** A flywheel with spokes: the machine is a thing you turn. */
    public static BufferedImage dynamoFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, DYNAMO);
        int rim = Palette.shade(CASING, 0.3);
        for (int y = 1; y < 13; y++) {
            for (int x = 1; x < SIZE - 1; x++) {
                double d = Math.hypot(x - CX, y - 6.5);
                if (d >= 4.0 && d <= 5.2) {
                    set(img, x, y, rim);
                } else if (d < 4.0 && (x == 7 || x == 8 || y == 6 || y == 7)) {
                    set(img, x, y, Palette.shade(CASING, 0.12));
                } else if (d < 4.0) {
                    set(img, x, y, Palette.shade(CASING, -0.55));
                }
                if (d < 1.5) {
                    set(img, x, y, glow < 0 ? Palette.shade(CASING, -0.2) : glow);
                }
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage dynamoTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 4; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
                double d = Math.hypot(x - CX, y - CY);
                if (d <= 3.6) {
                    set(img, x, y, d >= 2.6 ? Palette.shade(CASING, 0.3) : Palette.shade(CASING, -0.6));
                }
            }
        }
        set(img, 7, 7, DYNAMO);
        set(img, 8, 8, DYNAMO);
        return img;
    }

    // ---- Crude Extractor -----------------------------------------------------------------

    /** A barred aperture: the machine is a thing that takes ground in. */
    public static BufferedImage extractorFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, EXTRACTOR);
        for (int y = 2; y <= 10; y++) {
            for (int x = 3; x <= 12; x++) {
                boolean bar = (y - 2) % 3 == 2;
                int inside = glow < 0 ? Palette.shade(CASING, -0.65) : dim(glow);
                if (glow >= 0 && !bar && (x + y) % 2 == 0) {
                    inside = glow;
                }
                set(img, x, y, bar ? Palette.shade(CASING, 0.2) : inside);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage extractorTop() {
        BufferedImage img = MachineTextures.top(Palette.shade(CASING, -0.1));
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                int ring = Math.min(Math.min(x - 3, 12 - x), Math.min(y - 3, 12 - y));
                set(img, x, y, Palette.shade(CASING, -0.15 - ring * 0.12));
            }
        }
        return img;
    }

    // ---- Research Terminal ---------------------------------------------------------------

    /** A screen over a keyboard: the machine is a thing you read and type at. */
    public static BufferedImage terminalFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, TERMINAL);
        for (int y = 2; y <= 8; y++) {
            for (int x = 2; x <= 13; x++) {
                int pixel = Palette.shade(CASING, -0.7);
                switch (status) {
                    case "running" -> {
                        if ((y == 3 || y == 5 || y == 7) && x < 13 - (y * 3 % 5)) {
                            pixel = glow;
                        }
                    }
                    case "blocked" -> {
                        if (x >= 7 && x <= 8 && (y <= 6 || y == 8)) {
                            pixel = BLOCKED;
                        }
                    }
                    case "starved" -> {
                        if ((x + y) % 3 == 0) {
                            pixel = Palette.shade(STARVED, -0.35);
                        }
                    }
                    default -> {
                        if (y == 5) {
                            pixel = Palette.shade(CASING, -0.5);
                        }
                    }
                }
                set(img, x, y, pixel);
            }
        }
        for (int y = 10; y <= 12; y++) {
            for (int x = 2; x <= 13; x++) {
                boolean gap = x % 2 == 0 || y == 11;
                set(img, x, y, gap ? Palette.shade(CASING, -0.3) : Palette.shade(CASING, 0.25));
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage terminalTop() {
        BufferedImage img = MachineTextures.top(Palette.shade(CASING, -0.05));
        for (int y = 5; y <= 10; y += 2) {
            for (int x = 4; x <= 11; x++) {
                set(img, x, y, Palette.shade(CASING, -0.55));
            }
        }
        return img;
    }

    // ---- Flux Pylons ---------------------------------------------------------------------

    /**
     * A coiled conduit. More rings with each tier, and each tier its own accent, so a Mk3 is
     * recognisable from a distance as a bigger relative of a Mk1 rather than a different machine.
     */
    public static BufferedImage pylonSide(int tier, String status) {
        BufferedImage img = MachineTextures.blank();
        int accent = PYLON[tier - 1];
        int glow = statusColour(status, accent);
        int rings = 2 + tier;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int pixel = Palette.shade(CASING, -0.25);
                if (x == 0 || x == SIZE - 1) {
                    pixel = Palette.shade(CASING, -0.45);
                }
                int band = y * rings / SIZE;
                boolean ringRow = (y * rings) % SIZE < rings && y != 0;
                if (ringRow) {
                    pixel = glow < 0 ? Palette.shade(CASING, 0.1) : glow;
                    if ("starved".equals(status) && (x + band) % 3 != 0) {
                        pixel = Palette.shade(STARVED, -0.55);
                    }
                }
                if ((x == 7 || x == 8) && !ringRow) {
                    pixel = glow < 0 ? Palette.shade(CASING, -0.55) : dim(glow);
                }
                set(img, x, y, pixel);
            }
        }
        return img;
    }

    public static BufferedImage pylonTop(int tier, String status) {
        BufferedImage img = MachineTextures.blank();
        int glow = statusColour(status, PYLON[tier - 1]);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int ring = Math.min(Math.min(x, SIZE - 1 - x), Math.min(y, SIZE - 1 - y));
                int pixel = Palette.shade(CASING, -0.1 - (ring % 3) * 0.12);
                double d = Math.hypot(x - CX, y - CY);
                if (d < 2.6) {
                    pixel = glow < 0 ? Palette.shade(CASING, -0.5) : glow;
                } else if (d < 3.6 && glow >= 0) {
                    pixel = dim(glow);
                }
                set(img, x, y, pixel);
            }
        }
        return img;
    }
}
