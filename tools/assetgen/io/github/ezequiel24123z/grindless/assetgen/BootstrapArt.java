package io.github.ezequiel24123z.grindless.assetgen;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue.Geometry;

import java.awt.image.BufferedImage;

import static io.github.ezequiel24123z.grindless.assetgen.MachineTextures.SIZE;
import static io.github.ezequiel24123z.grindless.assetgen.MachineTextures.set;

/** Original stone, timber, wet geology and living fibre surfaces for the primitive F0 machines. */
final class BootstrapArt {

    static final int STONE = 0x8F8774;
    static final int TIMBER = 0x96724C;
    static final int BIOMASS = 0x638747;
    static final int FIBRE = 0xB59663;
    private static final int WATER = 0x486878;
    private static final int CLAY = 0x985F48;

    private BootstrapArt() {
    }

    static BufferedImage front(Geometry geometry, String status) {
        return switch (geometry) {
            case COLLECTOR -> collectorFront(status);
            case CULTURE -> cultureFront();
            case BIOGEN -> biogenFront(status);
            case BENCH -> benchFront();
            default -> throw new IllegalArgumentException("not a primitive machine: " + geometry);
        };
    }

    static BufferedImage top(Geometry geometry) {
        return switch (geometry) {
            case COLLECTOR -> collectorTop();
            case CULTURE -> cultureTop();
            case BIOGEN -> biogenTop();
            case BENCH -> benchTop();
            default -> throw new IllegalArgumentException("not a primitive machine: " + geometry);
        };
    }

    static BufferedImage stone() {
        BufferedImage image = MachineTextures.blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int grain = (x * 13 + y * 7 + x * y) % 11;
                double shade = grain < 2 ? -0.24 : grain > 8 ? 0.16 : 0.0;
                if ((x + y * 2) % 13 == 0) {
                    shade = -0.38;
                }
                set(image, x, y, Palette.shade(STONE, shade));
            }
        }
        return image;
    }

    static BufferedImage timber() {
        BufferedImage image = MachineTextures.blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int grain = (x * 3 + y / 3) % 7;
                double shade = x % 8 == 0 ? -0.34 : grain < 2 ? -0.15 : grain == 6 ? 0.18 : 0.0;
                set(image, x, y, Palette.shade(TIMBER, shade));
            }
        }
        return image;
    }

    static BufferedImage clay() {
        BufferedImage image = MachineTextures.blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean joint = y % 4 == 0 || (x + y / 4 * 3) % 8 == 0;
                set(image, x, y, joint ? 0x594A40 : Palette.shade(CLAY, (x * 5 + y * 3) % 7 == 0 ? 0.14 : 0.0));
            }
        }
        return image;
    }

    /** Pressed strands and thin compression layers, with no tree rings or machine indicator. */
    static BufferedImage fibreboard(boolean top) {
        BufferedImage image = MachineTextures.blank();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int strand = (x * 5 + y * 3) % 13;
                double shade = strand < 3 ? -0.17 : strand > 10 ? 0.20 : 0.0;
                if (!top && y % 4 == 0) {
                    shade = (x + y) % 5 == 0 ? -0.12 : -0.30;
                } else if (top && (x + y * 2) % 9 < 2) {
                    shade = 0.28;
                }
                set(image, x, y, Palette.shade(FIBRE, shade));
            }
        }
        return image;
    }

    private static BufferedImage collectorFront(String status) {
        BufferedImage image = timber();
        for (int y = 3; y <= 10; y++) {
            for (int x = 2; x <= 13; x++) {
                boolean rim = y == 3 || y == 10 || x == 2 || x == 13;
                int color = rim ? STONE : y >= 8 ? 0xB8A183 : WATER;
                if (!rim && y >= 7 && (x + y * 2) % 4 == 0) {
                    color = 0x8F8774;
                }
                if (status.equals("running") && y == 5 && (x == 5 || x == 9)) {
                    color = 0xA7C5C5;
                }
                set(image, x, y, color);
            }
        }
        return image;
    }

    private static BufferedImage collectorTop() {
        BufferedImage image = stone();
        for (int y = 2; y <= 13; y++) {
            for (int x = 2; x <= 13; x++) {
                boolean channel = x == 4 || x == 11 || y == 7;
                int grain = (x * 7 + y * 3) % 9;
                set(image, x, y, channel ? WATER : grain < 3 ? 0xB8A183 : grain > 6 ? 0xB5AD96 : STONE);
            }
        }
        return image;
    }

    private static BufferedImage cultureFront() {
        BufferedImage image = timber();
        for (int y = 3; y <= 10; y++) {
            for (int x = 3; x <= 12; x++) {
                int color = y == 3 || y == 10 ? 0xC4AA79 : 0x594A36;
                if (x == 5 || x == 10) {
                    color = y >= 8 ? WATER : BIOMASS;
                }
                set(image, x, y, color);
            }
        }
        set(image, 6, 5, 0x9DB86A);
        set(image, 9, 6, 0x9DB86A);
        return image;
    }

    private static BufferedImage cultureTop() {
        BufferedImage image = timber();
        for (int y = 2; y <= 13; y++) {
            for (int x = 2; x <= 13; x++) {
                boolean water = x == 7 || x == 8;
                int leaf = (x + y * 2) % 5;
                set(image, x, y, water ? WATER : leaf == 0 ? 0x514935 : leaf < 3 ? BIOMASS : 0x9DB86A);
            }
        }
        return image;
    }

    private static BufferedImage biogenFront(String status) {
        BufferedImage image = clay();
        for (int y = 3; y <= 10; y++) {
            for (int x = 3; x <= 12; x++) {
                boolean rim = x == 3 || x == 12 || y == 3 || y == 10;
                int color = rim ? STONE : 0x352D25;
                if (!rim && y >= 6 && y <= 8) {
                    color = status.equals("running") ? ((x + y) % 3 == 0 ? 0xFFDB83 : 0xC8753D) : 0x52663C;
                }
                if (y == 9 && x % 2 == 0) {
                    color = STONE;
                }
                set(image, x, y, color);
            }
        }
        return image;
    }

    private static BufferedImage biogenTop() {
        BufferedImage image = clay();
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                boolean rim = x == 3 || x == 12 || y == 3 || y == 12;
                set(image, x, y, rim ? STONE : (x + y * 3) % 5 < 2 ? 0xA29A5D : 0x52663C);
            }
        }
        return image;
    }

    private static BufferedImage benchFront() {
        BufferedImage image = timber();
        for (int y = 4; y <= 10; y++) {
            for (int x = 3; x <= 12; x++) {
                set(image, x, y, y == 4 || y == 10 ? 0xC4AA79 : 0x654D35);
            }
        }
        for (int x = 5; x <= 10; x++) {
            set(image, x, 7, 0xC4AA79);
        }
        set(image, 5, 6, STONE);
        set(image, 10, 8, STONE);
        return image;
    }

    private static BufferedImage benchTop() {
        BufferedImage image = fibreboard(true);
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                if (x % 3 == 0 && y % 3 == 0) {
                    set(image, x, y, 0x594A36);
                } else if (x >= 5 && x <= 10 && y >= 6 && y <= 9) {
                    set(image, x, y, y == 6 || x == 5 ? 0xD1B99C : TIMBER);
                }
            }
        }
        return image;
    }
}
