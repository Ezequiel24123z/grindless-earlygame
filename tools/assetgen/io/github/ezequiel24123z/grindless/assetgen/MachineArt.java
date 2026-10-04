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
    public static final int OUT_OF_BAND = 0xD500F9;

    /** Per-machine accents, deliberately none of the three status colours. */
    public static final int DYNAMO = 0x76FF03;
    public static final int EXTRACTOR = 0xFF8A00;
    public static final int TERMINAL = 0xB388FF;
    public static final int GENERATOR = 0xFF6E40;
    public static final int MILL = 0x40C4FF;
    public static final int FURNACE = 0xFF8A80;
    public static final int PRESS = 0xFFCC80;
    public static final int ASSEMBLER = 0x82B1FF;
    public static final int KILN = 0xE64A19;
    public static final int WIRE_MILL = 0xFF8A65;
    public static final int REACTOR = 0xCDDC39;
    public static final int WASHER = 0x0277BD;
    public static final int CELL = 0x00897B;
    public static final int INTAKE = 0x5C6BC0;
    public static final int WELL = 0x1E88E5;
    public static final int INDUCTION = 0x26A69A;
    public static final int CASTER = 0xF4511E;
    public static final int[] PYLON = {0x18FFFF, 0x69F0AE, 0xE040FB};
    public static final int BELT = 0xFFD54F;
    public static final int SPLITTER = 0xFFAB40;
    public static final int MERGER = 0xFFCA28;
    public static final int TUNNEL = 0x90A4AE;
    public static final int OVERFLOW = 0xFF7043;
    public static final int SORTER = 0x26C6DA;
    public static final int MANIPULATOR = 0x69F0AE;
    public static final int DRILL = 0xFF6D00;
    public static final int CONDUIT = 0x8D6E63;
    public static final int PUMP = 0x4FC3F7;
    public static final int TANK = 0x80CBC4;
    public static final int BANK = 0xFFEE58;
    public static final int TRANSFORMER = 0x7C4DFF;

    private static final double CX = 7.5;
    private static final double CY = 7.5;

    private MachineArt() {
    }

    public static BufferedImage front(io.github.ezequiel24123z.grindless.registry.BlockCatalogue.Geometry geometry,
                                     String status) {
        return switch (geometry) {
            case DYNAMO -> dynamoFront(status);
            case EXTRACTOR -> extractorFront(status);
            case TERMINAL -> terminalFront(status);
            case GENERATOR -> generatorFront(status);
            case MILL -> millFront(status);
            case FURNACE -> furnaceFront(status);
            case PRESS -> pressFront(status);
            case ASSEMBLER -> assemblerFront(status);
            case KILN -> kilnFront(status);
            case WIRE_MILL -> wireMillFront(status);
            case REACTOR -> reactorFront(status);
            case WASHER -> washerFront(status);
            case CELL -> cellFront(status);
            case INTAKE -> intakeFront(status);
            case WELL -> wellFront(status);
            case INDUCTION -> inductionFront(status);
            case CASTER -> casterFront(status);
            case BELT -> beltFront(status);
            case SPLITTER -> splitterFront(status);
            case MERGER -> mergerFront(status);
            case TUNNEL -> tunnelFront(status);
            case OVERFLOW -> overflowFront(status);
            case SORTER -> sorterFront(status);
            case MANIPULATOR -> manipulatorFront(status);
            case DRILL -> drillFront(status);
            case CONDUIT -> conduitFront(status);
            case PUMP -> pumpFront(status);
            case TANK -> tankFront(status);
            case BANK -> bankFront(status);
            case TRANSFORMER -> transformerFront(status);
            case PYLON -> pylonSide(1, status);
        };
    }

    public static BufferedImage top(io.github.ezequiel24123z.grindless.registry.BlockCatalogue.Geometry geometry) {
        return switch (geometry) {
            case DYNAMO -> dynamoTop();
            case EXTRACTOR -> extractorTop();
            case TERMINAL -> terminalTop();
            case GENERATOR -> generatorTop();
            case MILL -> millTop();
            case FURNACE -> furnaceTop();
            case PRESS -> pressTop();
            case ASSEMBLER -> assemblerTop();
            case KILN -> kilnTop();
            case WIRE_MILL -> wireMillTop();
            case REACTOR -> reactorTop();
            case WASHER -> washerTop();
            case CELL -> cellTop();
            case INTAKE -> intakeTop();
            case WELL -> wellTop();
            case INDUCTION -> inductionTop();
            case CASTER -> casterTop();
            case BELT -> beltTop();
            case SPLITTER -> splitterTop();
            case MERGER -> mergerTop();
            case TUNNEL -> tunnelTop();
            case OVERFLOW -> overflowTop();
            case SORTER -> sorterTop();
            case MANIPULATOR -> manipulatorTop();
            case DRILL -> drillTop();
            case CONDUIT -> conduitTop();
            case PUMP -> pumpTop();
            case TANK -> tankTop();
            case BANK -> bankTop();
            case TRANSFORMER -> transformerTop();
            case PYLON -> pylonTop(1, "idle");
        };
    }

    /** The colour a status paints with, or {@code -1} for idle. */
    static int statusColour(String status, int accent) {
        return switch (status) {
            case "running" -> accent;
            case "blocked" -> BLOCKED;
            case "starved" -> STARVED;
            case "out_of_band" -> OUT_OF_BAND;
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
                case "out_of_band" -> (x % 2 == 0) ? OUT_OF_BAND : dark;
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
                    case "out_of_band" -> {
                        if ((x == y || x + y == 15) && y >= 2 && y <= 8) {
                            pixel = OUT_OF_BAND;
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

    // ---- Thermal Generator ---------------------------------------------------------------

    /** Firebox bars: the machine is a thing that burns. */
    public static BufferedImage generatorFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, GENERATOR);
        for (int y = 3; y <= 10; y++) {
            for (int x = 3; x <= 12; x++) {
                boolean bar = x % 2 == 0;
                int inside = glow < 0 ? Palette.shade(CASING, -0.7) : dim(glow);
                if (glow >= 0 && !bar && y >= 6 && (x + y) % 2 == 0) {
                    inside = glow;
                }
                set(img, x, y, bar ? Palette.shade(CASING, 0.15) : inside);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage generatorTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 3; y <= 8; y++) {
            for (int x = 3; x <= 8; x++) {
                set(img, x, y, Palette.shade(CASING, -0.45));
            }
        }
        set(img, 5, 5, GENERATOR);
        set(img, 6, 5, GENERATOR);
        return img;
    }

    // ---- Pulverizer ----------------------------------------------------------------------

    /** Twin rollers: the machine is a thing that crushes. */
    public static BufferedImage millFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, MILL);
        for (int y = 3; y <= 10; y++) {
            for (int x = 2; x <= 6; x++) {
                double d = Math.hypot(x - 4.0, y - 6.5);
                if (d <= 2.6) {
                    set(img, x, y, glow < 0 ? Palette.shade(CASING, -0.35) : dim(glow));
                }
            }
            for (int x = 9; x <= 13; x++) {
                double d = Math.hypot(x - 11.0, y - 6.5);
                if (d <= 2.6) {
                    set(img, x, y, glow < 0 ? Palette.shade(CASING, -0.35) : dim(glow));
                }
            }
        }
        if (glow >= 0) {
            set(img, 4, 6, glow);
            set(img, 11, 6, glow);
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage millTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 5; y <= 10; y++) {
            for (int x = 3; x <= 6; x++) {
                set(img, x, y, Palette.shade(CASING, -0.4));
            }
            for (int x = 9; x <= 12; x++) {
                set(img, x, y, Palette.shade(CASING, -0.4));
            }
        }
        return img;
    }

    // ---- Arc Furnace ---------------------------------------------------------------------

    /** A dark window with an arc: the machine is a thing that melts. */
    public static BufferedImage furnaceFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, FURNACE);
        for (int y = 3; y <= 10; y++) {
            for (int x = 3; x <= 12; x++) {
                int pixel = Palette.shade(CASING, -0.75);
                if (glow >= 0 && Math.abs(x - 7.5) + Math.abs(y - 6.5) < 4) {
                    pixel = (x + y) % 2 == 0 ? glow : dim(glow);
                } else if (glow < 0 && y == 7) {
                    pixel = Palette.shade(CASING, -0.5);
                }
                set(img, x, y, pixel);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage furnaceTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 5; y <= 10; y++) {
            set(img, 4, y, Palette.shade(CASING, 0.25));
            set(img, 11, y, Palette.shade(CASING, 0.25));
        }
        set(img, 7, 7, FURNACE);
        set(img, 8, 8, FURNACE);
        return img;
    }

    // ---- Press ---------------------------------------------------------------------------

    /** A ram over a dark bed: the machine is a thing that stamps. */
    public static BufferedImage pressFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, PRESS);
        for (int y = 8; y <= 11; y++) {
            for (int x = 3; x <= 12; x++) {
                set(img, x, y, Palette.shade(CASING, -0.55));
            }
        }
        for (int y = 2; y <= 7; y++) {
            for (int x = 6; x <= 9; x++) {
                set(img, x, y, glow < 0 ? Palette.shade(CASING, 0.15) : dim(glow));
            }
        }
        if (glow >= 0) {
            set(img, 7, 7, glow);
            set(img, 8, 7, glow);
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage pressTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 4; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
                set(img, x, y, Palette.shade(CASING, -0.45));
            }
        }
        for (int x = 6; x <= 9; x++) {
            set(img, x, 3, PRESS);
        }
        return img;
    }

    // ---- Assembler -----------------------------------------------------------------------

    /** Two claws over a deck: the machine is a thing that builds. */
    public static BufferedImage assemblerFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, ASSEMBLER);
        for (int y = 7; y <= 11; y++) {
            for (int x = 3; x <= 12; x++) {
                set(img, x, y, Palette.shade(CASING, -0.5));
            }
        }
        for (int y = 2; y <= 8; y++) {
            set(img, 4, y, glow < 0 ? Palette.shade(CASING, 0.2) : dim(glow));
            set(img, 11, y, glow < 0 ? Palette.shade(CASING, 0.2) : dim(glow));
        }
        set(img, 3, 3, glow < 0 ? Palette.shade(CASING, -0.1) : glow);
        set(img, 5, 3, glow < 0 ? Palette.shade(CASING, -0.1) : glow);
        set(img, 10, 3, glow < 0 ? Palette.shade(CASING, -0.1) : glow);
        set(img, 12, 3, glow < 0 ? Palette.shade(CASING, -0.1) : glow);
        strip(img, status);
        return img;
    }

    public static BufferedImage assemblerTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 3; y <= 12; y++) {
            set(img, 4, y, Palette.shade(CASING, -0.4));
            set(img, 11, y, Palette.shade(CASING, -0.4));
        }
        set(img, 4, 3, ASSEMBLER);
        set(img, 11, 3, ASSEMBLER);
        return img;
    }

    // ---- Kiln ----------------------------------------------------------------------------

    /** Brick courses around a dark arch: the machine is a thing that roasts. */
    public static BufferedImage kilnFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, KILN);
        int brick = Palette.shade(KILN, -0.55);
        int mortar = Palette.shade(CASING, -0.25);
        for (int y = 2; y <= 12; y++) {
            for (int x = 2; x <= 13; x++) {
                boolean course = (y % 2) == 0;
                boolean joint = course ? (x % 3 == 0) : (x % 3 == 1);
                set(img, x, y, joint ? mortar : brick);
            }
        }
        for (int y = 5; y <= 11; y++) {
            for (int x = 5; x <= 10; x++) {
                double dx = Math.abs(x - 7.5);
                double dy = 11 - y;
                if (dx * dx / 9.0 + dy * dy / 36.0 <= 1.0) {
                    int pixel = Palette.shade(CASING, -0.8);
                    if (glow >= 0 && y >= 8) {
                        pixel = (x + y) % 2 == 0 ? glow : dim(glow);
                    }
                    set(img, x, y, pixel);
                }
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage kilnTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 5; y <= 10; y++) {
            for (int x = 5; x <= 10; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d <= 3.6 && d >= 1.8) {
                    set(img, x, y, Palette.shade(KILN, -0.35));
                } else if (d < 1.8) {
                    set(img, x, y, Palette.shade(CASING, -0.7));
                }
            }
        }
        set(img, 7, 7, KILN);
        set(img, 8, 7, KILN);
        return img;
    }

    // ---- Wire Mill -----------------------------------------------------------------------

    /** Two drawing dies with a strand: the machine is a thing that draws wire. */
    public static BufferedImage wireMillFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, WIRE_MILL);
        int die = Palette.shade(CASING, 0.2);
        int dark = Palette.shade(CASING, -0.65);
        for (int y = 3; y <= 11; y++) {
            for (int x = 2; x <= 5; x++) {
                set(img, x, y, die);
            }
            for (int x = 10; x <= 13; x++) {
                set(img, x, y, die);
            }
        }
        for (int y = 5; y <= 9; y++) {
            set(img, 3, y, dark);
            set(img, 4, y, dark);
            set(img, 11, y, dark);
            set(img, 12, y, dark);
        }
        int strand = glow < 0 ? Palette.shade(WIRE_MILL, -0.35) : glow;
        for (int x = 5; x <= 10; x++) {
            set(img, x, 7, strand);
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage wireMillTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 4; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
                double d = Math.hypot(x - CX, y - CY);
                if (d <= 4.2 && d >= 2.4) {
                    set(img, x, y, Palette.shade(WIRE_MILL, -0.25));
                } else if (d < 2.4) {
                    set(img, x, y, Palette.shade(CASING, -0.55));
                }
            }
        }
        set(img, 7, 7, WIRE_MILL);
        set(img, 8, 7, WIRE_MILL);
        return img;
    }

    // ---- Chemical Reactor ----------------------------------------------------------------

    /** A sight-glass vat with a stirrer: the machine is a thing that stirs. */
    public static BufferedImage reactorFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, REACTOR);
        int rim = Palette.shade(CASING, 0.15);
        int dark = Palette.shade(CASING, -0.7);
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                double d = Math.hypot(x - CX, y - 8.0);
                if (d <= 5.2 && d >= 4.2) {
                    set(img, x, y, rim);
                } else if (d < 4.2) {
                    int fill = glow < 0 ? Palette.shade(REACTOR, -0.45) : glow;
                    set(img, x, y, y >= 8 ? fill : dark);
                }
            }
        }
        int shaft = glow < 0 ? Palette.shade(REACTOR, -0.2) : glow;
        for (int y = 3; y <= 10; y++) {
            set(img, 7, y, shaft);
            set(img, 8, y, shaft);
        }
        set(img, 5, 8, shaft);
        set(img, 6, 8, shaft);
        set(img, 9, 8, shaft);
        set(img, 10, 8, shaft);
        strip(img, status);
        return img;
    }

    public static BufferedImage reactorTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                double d = Math.hypot(x - CX, y - CY);
                if (d <= 5.4 && d >= 4.0) {
                    set(img, x, y, Palette.shade(REACTOR, -0.2));
                } else if (d < 4.0) {
                    set(img, x, y, Palette.shade(CASING, -0.65));
                }
            }
        }
        for (int i = 4; i <= 11; i++) {
            set(img, i, 7, REACTOR);
            set(img, i, 8, REACTOR);
            set(img, 7, i, REACTOR);
            set(img, 8, i, REACTOR);
        }
        return img;
    }

    // ---- Chemical Washer -----------------------------------------------------------------

    /** A sluice and a spray bar: the machine is a thing that washes. */
    public static BufferedImage washerFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, WASHER);
        int water = glow < 0 ? Palette.shade(WASHER, -0.35) : glow;
        int rim = Palette.shade(CASING, 0.2);
        for (int y = 8; y <= 13; y++) {
            for (int x = 2; x <= 13; x++) {
                boolean wall = y == 8 || y == 13 || x == 2 || x == 13;
                set(img, x, y, wall ? rim : water);
            }
        }
        int spray = glow < 0 ? Palette.shade(WASHER, 0.35) : glow;
        for (int x = 4; x <= 11; x++) {
            set(img, x, 4, Palette.shade(CASING, -0.4));
            if (x % 2 == 0) {
                set(img, x, 5, spray);
                set(img, x, 6, spray);
            }
        }
        set(img, 7, 14, Palette.shade(CASING, -0.55));
        set(img, 8, 14, Palette.shade(CASING, -0.55));
        strip(img, status);
        return img;
    }

    public static BufferedImage washerTop() {
        BufferedImage img = MachineTextures.top(CASING);
        int water = Palette.shade(WASHER, -0.15);
        for (int y = 3; y <= 12; y++) {
            for (int x = 2; x <= 13; x++) {
                boolean wall = y == 3 || y == 12 || x == 2 || x == 13;
                set(img, x, y, wall ? Palette.shade(CASING, 0.15) : water);
            }
        }
        for (int x = 4; x <= 11; x++) {
            set(img, x, 6, WASHER);
        }
        return img;
    }

    // ---- Electrolysis Cell ---------------------------------------------------------------

    /** Two electrodes in a bath: the machine is a thing that splits. */
    public static BufferedImage cellFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, CELL);
        int bath = glow < 0 ? Palette.shade(CELL, -0.25) : glow;
        for (int y = 6; y <= 13; y++) {
            for (int x = 3; x <= 12; x++) {
                boolean wall = y == 6 || y == 13 || x == 3 || x == 12;
                set(img, x, y, wall ? Palette.shade(CASING, 0.15) : bath);
            }
        }
        for (int y = 7; y <= 12; y++) {
            set(img, 5, y, Palette.shade(CASING, -0.45));
            set(img, 10, y, Palette.shade(CASING, -0.45));
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage cellTop() {
        BufferedImage img = MachineTextures.top(CASING);
        int bath = Palette.shade(CELL, -0.1);
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                boolean wall = y == 3 || y == 12 || x == 3 || x == 12;
                set(img, x, y, wall ? Palette.shade(CASING, 0.15) : bath);
            }
        }
        for (int y = 5; y <= 10; y++) {
            set(img, 5, y, CELL);
            set(img, 10, y, CELL);
        }
        return img;
    }

    // ---- Atmospheric Intake --------------------------------------------------------------

    /** Louvers and a stack: the machine is a thing that drinks air. */
    public static BufferedImage intakeFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, INTAKE);
        int slat = glow < 0 ? Palette.shade(INTAKE, -0.2) : glow;
        for (int y = 5; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                set(img, x, y, (y % 2 == 0) ? slat : Palette.shade(CASING, -0.35));
            }
        }
        for (int y = 2; y <= 4; y++) {
            set(img, 7, y, Palette.shade(CASING, -0.2));
            set(img, 8, y, Palette.shade(CASING, -0.2));
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage intakeTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int i = 4; i <= 11; i++) {
            set(img, i, 4, INTAKE);
            set(img, i, 11, INTAKE);
            set(img, 4, i, INTAKE);
            set(img, 11, i, INTAKE);
        }
        for (int y = 6; y <= 9; y++) {
            for (int x = 6; x <= 9; x++) {
                set(img, x, y, Palette.shade(INTAKE, -0.25));
            }
        }
        return img;
    }

    // ---- Fluid Well ----------------------------------------------------------------------

    /** A bore and a riser: the machine is a thing that draws water from the chunk. */
    public static BufferedImage wellFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, WELL);
        int water = glow < 0 ? Palette.shade(WELL, -0.3) : glow;
        for (int y = 8; y <= 13; y++) {
            for (int x = 4; x <= 11; x++) {
                boolean wall = y == 8 || y == 13 || x == 4 || x == 11;
                set(img, x, y, wall ? Palette.shade(CASING, 0.1) : water);
            }
        }
        for (int y = 3; y <= 7; y++) {
            set(img, 7, y, Palette.shade(CASING, -0.25));
            set(img, 8, y, Palette.shade(CASING, -0.25));
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage wellTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int i = 5; i <= 10; i++) {
            set(img, i, 5, WELL);
            set(img, i, 10, WELL);
            set(img, 5, i, WELL);
            set(img, 10, i, WELL);
        }
        for (int y = 7; y <= 8; y++) {
            for (int x = 7; x <= 8; x++) {
                set(img, x, y, Palette.shade(WELL, -0.2));
            }
        }
        return img;
    }

    // ---- Induction Furnace ---------------------------------------------------------------

    /** Coil turns around a crucible: the machine melts without a flame. */
    public static BufferedImage inductionFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, INDUCTION);
        int coil = glow < 0 ? Palette.shade(INDUCTION, -0.15) : glow;
        for (int y = 4; y <= 11; y++) {
            for (int x = 3; x <= 12; x++) {
                boolean turn = y == 4 || y == 6 || y == 8 || y == 10;
                boolean edge = x == 3 || x == 12;
                if (turn || edge) {
                    set(img, x, y, coil);
                }
            }
        }
        for (int y = 12; y <= 13; y++) {
            for (int x = 5; x <= 10; x++) {
                set(img, x, y, glow < 0 ? 0xFF6D00 : glow);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage inductionTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int i = 3; i <= 12; i++) {
            set(img, i, 3, INDUCTION);
            set(img, i, 12, INDUCTION);
            set(img, 3, i, INDUCTION);
            set(img, 12, i, INDUCTION);
            set(img, i, 5, Palette.shade(INDUCTION, -0.2));
            set(img, i, 10, Palette.shade(INDUCTION, -0.2));
        }
        for (int y = 7; y <= 8; y++) {
            for (int x = 7; x <= 8; x++) {
                set(img, x, y, 0xFF6D00);
            }
        }
        return img;
    }

    // ---- Caster --------------------------------------------------------------------------

    /** An open mould and a pour lip: melt becomes a shape. */
    public static BufferedImage casterFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, CASTER);
        int metal = glow < 0 ? Palette.shade(CASTER, -0.25) : glow;
        for (int y = 7; y <= 13; y++) {
            for (int x = 3; x <= 12; x++) {
                boolean wall = y == 7 || y == 13 || x == 3 || x == 12;
                set(img, x, y, wall ? Palette.shade(CASING, 0.2) : metal);
            }
        }
        for (int x = 6; x <= 9; x++) {
            set(img, x, 4, CASTER);
            set(img, x, 5, CASTER);
            set(img, x, 6, Palette.shade(CASTER, 0.15));
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage casterTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 4; y <= 11; y++) {
            for (int x = 3; x <= 12; x++) {
                boolean wall = y == 4 || y == 11 || x == 3 || x == 12;
                set(img, x, y, wall ? Palette.shade(CASING, 0.2) : Palette.shade(CASTER, -0.15));
            }
        }
        for (int x = 6; x <= 9; x++) {
            set(img, x, 2, CASTER);
        }
        return img;
    }

    // ---- Conveyor Belt -------------------------------------------------------------------

    /** Two lanes receding: the block is a thing that carries. */
    public static BufferedImage beltFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, BELT);
        for (int y = 8; y <= 13; y++) {
            for (int x = 1; x <= 14; x++) {
                boolean rail = x <= 2 || x >= 13 || x == 7 || x == 8;
                int pixel = rail ? Palette.shade(CASING, 0.25) : Palette.shade(CASING, -0.55);
                if (!rail && glow >= 0 && (x + y) % 3 == 0) {
                    pixel = glow;
                }
                set(img, x, y, pixel);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage beltTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 1; y <= 14; y++) {
            for (int x = 1; x <= 6; x++) {
                set(img, x, y, Palette.shade(CASING, (x + y) % 2 == 0 ? -0.35 : -0.5));
            }
            for (int x = 9; x <= 14; x++) {
                set(img, x, y, Palette.shade(CASING, (x + y) % 2 == 0 ? -0.35 : -0.5));
            }
            set(img, 7, y, Palette.shade(CASING, 0.2));
            set(img, 8, y, Palette.shade(CASING, 0.2));
        }
        return img;
    }

    // ---- Splitter ------------------------------------------------------------------------

    /** Three mouths: the block is a thing that chooses. */
    public static BufferedImage splitterFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, SPLITTER);
        for (int y = 6; y <= 12; y++) {
            for (int mouth = 0; mouth < 3; mouth++) {
                int x0 = 2 + mouth * 4;
                for (int x = x0; x <= x0 + 2; x++) {
                    int pixel = glow < 0 ? Palette.shade(CASING, -0.6) : dim(glow);
                    if (glow >= 0 && y == 9) {
                        pixel = glow;
                    }
                    set(img, x, y, pixel);
                }
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage splitterTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 2; y <= 6; y++) {
            for (int x = 6; x <= 9; x++) {
                set(img, x, y, Palette.shade(CASING, -0.5));
            }
        }
        for (int y = 8; y <= 13; y++) {
            for (int x = 2; x <= 5; x++) {
                set(img, x, y, Palette.shade(CASING, -0.45));
            }
            for (int x = 10; x <= 13; x++) {
                set(img, x, y, Palette.shade(CASING, -0.45));
            }
        }
        set(img, 7, 4, SPLITTER);
        set(img, 8, 4, SPLITTER);
        return img;
    }

    // ---- Merger --------------------------------------------------------------------------

    /** One mouth: the block is a thing that joins. */
    public static BufferedImage mergerFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, MERGER);
        for (int y = 6; y <= 12; y++) {
            for (int x = 5; x <= 10; x++) {
                int pixel = glow < 0 ? Palette.shade(CASING, -0.6) : dim(glow);
                if (glow >= 0 && y == 9) {
                    pixel = glow;
                }
                set(img, x, y, pixel);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage mergerTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 8; y <= 13; y++) {
            for (int x = 6; x <= 9; x++) {
                set(img, x, y, Palette.shade(CASING, -0.5));
            }
        }
        for (int y = 2; y <= 7; y++) {
            for (int x = 2; x <= 5; x++) {
                set(img, x, y, Palette.shade(CASING, -0.45));
            }
            for (int x = 10; x <= 13; x++) {
                set(img, x, y, Palette.shade(CASING, -0.45));
            }
        }
        set(img, 7, 11, MERGER);
        set(img, 8, 11, MERGER);
        return img;
    }

    // ---- Tunnel Belt ---------------------------------------------------------------------

    /** A dark mouth: the block swallows a lane. */
    public static BufferedImage tunnelFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, TUNNEL);
        for (int y = 8; y <= 13; y++) {
            for (int x = 3; x <= 12; x++) {
                boolean lip = x <= 4 || x >= 11 || y == 8 || y == 13;
                int pixel = lip ? Palette.shade(CASING, 0.15) : Palette.shade(CASING, -0.7);
                if (!lip && glow >= 0 && y == 10) {
                    pixel = glow;
                }
                set(img, x, y, pixel);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage tunnelTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 4; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
                set(img, x, y, Palette.shade(CASING, -0.65));
            }
        }
        set(img, 7, 7, TUNNEL);
        set(img, 8, 7, TUNNEL);
        return img;
    }

    // ---- Overflow Gate -------------------------------------------------------------------

    /** Front plus a side notch: the block is a thing that yields. */
    public static BufferedImage overflowFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, OVERFLOW);
        for (int y = 7; y <= 12; y++) {
            for (int x = 4; x <= 11; x++) {
                int pixel = glow < 0 ? Palette.shade(CASING, -0.55) : dim(glow);
                if (glow >= 0 && x >= 10) {
                    pixel = glow;
                }
                set(img, x, y, pixel);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage overflowTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 2; y <= 13; y++) {
            for (int x = 6; x <= 9; x++) {
                set(img, x, y, Palette.shade(CASING, -0.5));
            }
        }
        for (int y = 6; y <= 9; y++) {
            for (int x = 10; x <= 14; x++) {
                set(img, x, y, Palette.shade(CASING, -0.45));
            }
        }
        set(img, 12, 7, OVERFLOW);
        set(img, 12, 8, OVERFLOW);
        return img;
    }

    // ---- Sorter --------------------------------------------------------------------------

    /** Two side mouths and a passthrough: the block is a thing that peels. */
    public static BufferedImage sorterFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, SORTER);
        for (int y = 6; y <= 12; y++) {
            for (int x = 5; x <= 10; x++) {
                int pixel = glow < 0 ? Palette.shade(CASING, -0.6) : dim(glow);
                if (glow >= 0 && y == 9) {
                    pixel = glow;
                }
                set(img, x, y, pixel);
            }
        }
        for (int y = 7; y <= 11; y++) {
            int pixel = glow < 0 ? Palette.shade(CASING, -0.4) : dim(glow);
            set(img, 1, y, pixel);
            set(img, 2, y, pixel);
            set(img, 13, y, pixel);
            set(img, 14, y, pixel);
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage sorterTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 2; y <= 13; y++) {
            for (int x = 6; x <= 9; x++) {
                set(img, x, y, Palette.shade(CASING, -0.5));
            }
        }
        for (int y = 6; y <= 9; y++) {
            for (int x = 1; x <= 4; x++) {
                set(img, x, y, Palette.shade(CASING, -0.45));
            }
            for (int x = 11; x <= 14; x++) {
                set(img, x, y, Palette.shade(CASING, -0.45));
            }
        }
        set(img, 2, 7, SORTER);
        set(img, 2, 8, SORTER);
        set(img, 13, 7, SORTER);
        set(img, 13, 8, SORTER);
        return img;
    }

    // ---- Crude Manipulator ---------------------------------------------------------------

    /** A claw: the block is a thing that reaches. */
    public static BufferedImage manipulatorFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, MANIPULATOR);
        for (int y = 3; y <= 10; y++) {
            set(img, 7, y, Palette.shade(CASING, 0.2));
            set(img, 8, y, Palette.shade(CASING, 0.2));
        }
        for (int x = 4; x <= 11; x++) {
            set(img, x, 4, glow < 0 ? Palette.shade(CASING, -0.2) : glow);
        }
        set(img, 4, 5, glow < 0 ? Palette.shade(CASING, -0.1) : glow);
        set(img, 11, 5, glow < 0 ? Palette.shade(CASING, -0.1) : glow);
        strip(img, status);
        return img;
    }

    public static BufferedImage manipulatorTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 3; y <= 12; y++) {
            set(img, 7, y, Palette.shade(CASING, -0.4));
            set(img, 8, y, Palette.shade(CASING, -0.4));
        }
        set(img, 7, 3, MANIPULATOR);
        set(img, 8, 3, MANIPULATOR);
        return img;
    }

    // ---- Terrestrial Extractor -----------------------------------------------------------

    /** A wider bore with a collar: the machine is a heavier relative of the Crude Extractor. */
    public static BufferedImage drillFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, DRILL);
        for (int y = 1; y <= 11; y++) {
            for (int x = 2; x <= 13; x++) {
                double d = Math.hypot(x - CX, y - 6.0);
                if (d <= 4.6 && d >= 3.4) {
                    set(img, x, y, Palette.shade(CASING, 0.25));
                } else if (d < 3.4) {
                    boolean bar = (x + y) % 3 == 0;
                    int inside = glow < 0 ? Palette.shade(CASING, -0.7) : dim(glow);
                    set(img, x, y, bar ? Palette.shade(CASING, 0.1) : inside);
                }
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage drillTop() {
        BufferedImage img = MachineTextures.top(Palette.shade(CASING, -0.15));
        for (int y = 2; y <= 13; y++) {
            for (int x = 2; x <= 13; x++) {
                double d = Math.hypot(x - CX, y - CY);
                if (d <= 5.2) {
                    set(img, x, y, d >= 4.0
                            ? Palette.shade(CASING, 0.2)
                            : Palette.shade(CASING, -0.55 - (5.0 - d) * 0.05));
                }
            }
        }
        set(img, 7, 7, DRILL);
        set(img, 8, 8, DRILL);
        return img;
    }

    // ---- Clay Conduit --------------------------------------------------------------------

    /** A U-shaped trough: the block is a thing liquid runs along. */
    public static BufferedImage conduitFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, CONDUIT);
        for (int y = 6; y <= 13; y++) {
            for (int x = 1; x <= 14; x++) {
                boolean wall = x <= 2 || x >= 13 || y >= 12;
                int pixel = wall ? Palette.shade(CASING, 0.15) : Palette.shade(CASING, -0.6);
                if (!wall && glow >= 0 && y >= 9) {
                    pixel = (x + y) % 2 == 0 ? glow : dim(glow);
                }
                set(img, x, y, pixel);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage conduitTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 1; y <= 14; y++) {
            set(img, 1, y, Palette.shade(CASING, 0.15));
            set(img, 2, y, Palette.shade(CASING, 0.1));
            set(img, 13, y, Palette.shade(CASING, 0.1));
            set(img, 14, y, Palette.shade(CASING, 0.15));
            for (int x = 3; x <= 12; x++) {
                set(img, x, y, Palette.shade(CONDUIT, (x + y) % 3 == 0 ? -0.35 : -0.55));
            }
        }
        return img;
    }

    // ---- Hand Pump -----------------------------------------------------------------------

    /** A piston over a well: the block is a thing that lifts water. */
    public static BufferedImage pumpFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, PUMP);
        for (int y = 8; y <= 13; y++) {
            for (int x = 4; x <= 11; x++) {
                double d = Math.hypot(x - CX, y - 11.0);
                if (d <= 3.4) {
                    int inside = glow < 0 ? Palette.shade(CASING, -0.65) : dim(glow);
                    set(img, x, y, d >= 2.6 ? Palette.shade(CASING, 0.2) : inside);
                }
            }
        }
        for (int y = 2; y <= 8; y++) {
            set(img, 7, y, Palette.shade(CASING, 0.25));
            set(img, 8, y, Palette.shade(CASING, 0.25));
        }
        for (int x = 5; x <= 10; x++) {
            set(img, x, 3, glow < 0 ? Palette.shade(CASING, -0.15) : glow);
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage pumpTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                double d = Math.hypot(x - CX, y - CY);
                if (d <= 5.0) {
                    set(img, x, y, d >= 4.0
                            ? Palette.shade(CASING, 0.2)
                            : Palette.shade(PUMP, -0.45 - (4.5 - d) * 0.05));
                }
            }
        }
        set(img, 7, 7, PUMP);
        set(img, 8, 8, PUMP);
        return img;
    }

    // ---- Basic Tank ----------------------------------------------------------------------

    /** A sight glass: the block is a thing that holds a volume you can read. */
    public static BufferedImage tankFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, TANK);
        for (int y = 3; y <= 12; y++) {
            for (int x = 5; x <= 10; x++) {
                boolean frame = x == 5 || x == 10 || y == 3 || y == 12;
                int pixel = frame ? Palette.shade(CASING, 0.25) : Palette.shade(CASING, -0.7);
                if (!frame && glow >= 0 && y >= 8) {
                    pixel = (x + y) % 2 == 0 ? glow : dim(glow);
                }
                set(img, x, y, pixel);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage tankTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 2; y <= 13; y++) {
            for (int x = 2; x <= 13; x++) {
                double d = Math.hypot(x - CX, y - CY);
                if (d <= 6.0) {
                    set(img, x, y, d >= 5.0
                            ? Palette.shade(CASING, 0.15)
                            : Palette.shade(TANK, -0.25));
                }
            }
        }
        set(img, 7, 7, TANK);
        set(img, 8, 7, TANK);
        return img;
    }

    // ---- Capacitor Bank ------------------------------------------------------------------

    public static BufferedImage bankFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, BANK);
        for (int col = 0; col < 3; col++) {
            int x0 = 3 + col * 4;
            for (int y = 3; y <= 11; y++) {
                for (int x = x0; x <= x0 + 2; x++) {
                    boolean frame = x == x0 || x == x0 + 2 || y == 3 || y == 11;
                    int pixel = frame ? Palette.shade(CASING, 0.2) : Palette.shade(CASING, -0.65);
                    if (!frame && glow >= 0 && y >= 7) {
                        pixel = (x + y) % 2 == 0 ? glow : dim(glow);
                    }
                    set(img, x, y, pixel);
                }
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage bankTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int col = 0; col < 3; col++) {
            int x0 = 3 + col * 4;
            for (int y = 4; y <= 11; y++) {
                for (int x = x0; x <= x0 + 2; x++) {
                    set(img, x, y, Palette.shade(BANK, (x + y) % 2 == 0 ? -0.1 : -0.4));
                }
            }
        }
        return img;
    }

    // ---- Flux Transformer ----------------------------------------------------------------

    public static BufferedImage transformerFront(String status) {
        BufferedImage img = casingBase(CASING);
        int glow = statusColour(status, TRANSFORMER);
        for (int y = 4; y <= 11; y++) {
            for (int x = 6; x <= 9; x++) {
                set(img, x, y, Palette.shade(CASING, -0.5));
            }
        }
        for (int coil = 0; coil < 2; coil++) {
            int y = 5 + coil * 4;
            for (int x = 3; x <= 12; x++) {
                int pixel = glow < 0 ? Palette.shade(TRANSFORMER, -0.45) : (x % 2 == 0 ? glow : dim(glow));
                set(img, x, y, pixel);
                set(img, x, y + 1, pixel);
            }
        }
        strip(img, status);
        return img;
    }

    public static BufferedImage transformerTop() {
        BufferedImage img = MachineTextures.top(CASING);
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 6; x++) {
                set(img, x, y, Palette.shade(TRANSFORMER, -0.3));
            }
            for (int x = 9; x <= 12; x++) {
                set(img, x, y, Palette.shade(TRANSFORMER, -0.3));
            }
        }
        for (int y = 6; y <= 9; y++) {
            for (int x = 6; x <= 9; x++) {
                set(img, x, y, Palette.shade(CASING, -0.2));
            }
        }
        set(img, 7, 7, TRANSFORMER);
        set(img, 8, 8, TRANSFORMER);
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
