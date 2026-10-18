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
 * and consistency <em>is</em> the quality. The three Bootstrap progression objects instead use
 * authored local material layers and different reliefs, not palette swaps of one matrix template.
 *
 * <p>Every template is deterministic — the same palette always gives the same image — so
 * regenerating never produces a spurious diff.
 */
public final class FormTextures {

    public static final int SIZE = HeightField.SIZE;

    /** Local materials for the three Bootstrap progression objects, not the material matrix. */
    private static final Palette CORE_CERAMIC =
            new Palette("core_ceramic", 0xB9A78A, 0xE2D5B8, 0x79674F);
    private static final Palette CORE_CONTACT =
            new Palette("core_contact", 0x967044, 0xD5B36D, 0x4E3C30);

    private FormTextures() {
    }

    /** Angular mixed pebbles with pale clay inclusions, not a pure-metal ore or a dust pile. */
    public static BufferedImage mineralAggregate(Palette palette) {
        HeightField field = new HeightField();
        double[][] pebbles = {{5, 7, 3.6, 4.1}, {10, 9, 3.3, 3.6}, {6, 11, 3.0, 2.8}};
        for (double[] pebble : pebbles) {
            for (int y = 2; y <= 13; y++) {
                for (int x = 2; x <= 13; x++) {
                    double distance = Math.abs(x - pebble[0]) + Math.abs(y - pebble[1]);
                    if (distance <= pebble[2]) {
                        double facet = (x < pebble[0] ? 0.35 : -0.20)
                                + (y < pebble[1] ? 0.25 : -0.15);
                        field.set(x, y, Math.max(field.get(x, y), pebble[3] - distance * 0.3 + facet));
                    }
                }
            }
        }
        field.bevel(0.4);
        Palette stone = new Palette(palette.name(), palette.base(),
                Palette.shade(palette.base(), 0.18), palette.shadow());
        BufferedImage image = field.light(stone);
        // Two clay seams follow the pebbles, instead of scattering unrelated bright speckles.
        surfacePixel(image, 5, 6, 0xC9B498);
        surfacePixel(image, 6, 6, 0xB8A183);
        surfacePixel(image, 6, 7, 0x97836B);
        surfacePixel(image, 9, 10, 0xB8A183);
        surfacePixel(image, 10, 10, 0x97836B);
        return image;
    }

    /** A loose three-leaf culture harvest with exposed pale stems. */
    public static BufferedImage biomass(Palette palette) {
        HeightField field = new HeightField();
        field.rect(6, 7, 9, 13, 2.7);
        double[][] leaves = {{4.5, 7, 2.6, 4}, {8, 5, 2.8, 4}, {11, 8, 2.5, 3.7}};
        for (double[] leaf : leaves) {
            for (int y = 2; y <= 12; y++) {
                for (int x = 2; x <= 13; x++) {
                    double distance = Math.abs(x - leaf[0]) / leaf[2] + Math.abs(y - leaf[1]) / leaf[3];
                    if (distance < 1.0) {
                        double fold = Math.max(0, 1.0 - Math.abs(x - leaf[0]) * 0.8) * 0.55;
                        field.set(x, y, Math.max(field.get(x, y),
                                3.0 + (1.0 - distance) * 1.6 + fold));
                    }
                }
            }
        }
        field.bevel(0.35);
        Palette leaf = new Palette(palette.name(), palette.base(),
                Palette.shade(palette.base(), 0.22), Palette.shade(palette.base(), -0.30));
        BufferedImage image = field.light(leaf);
        // Short midribs describe the existing leaves without changing their silhouette.
        surfacePixel(image, 7, 4, leaf.highlight());
        surfacePixel(image, 7, 5, leaf.highlight());
        surfacePixel(image, 8, 6, leaf.base());
        surfacePixel(image, 4, 7, leaf.highlight());
        surfacePixel(image, 5, 8, leaf.base());
        surfacePixel(image, 10, 7, leaf.highlight());
        surfacePixel(image, 11, 8, leaf.base());
        for (int y = 9; y <= 12; y++) {
            image.setRGB(7, y, 0xFFBEAC6E);
            if (y > 10) {
                image.setRGB(8, y, 0xFF8E804F);
            }
        }
        return image;
    }

    /** An offset wooden grip and a bored stone socket; a hand tool, never a machine sprite. */
    public static BufferedImage crankHandle(Palette palette) {
        HeightField handle = new HeightField();
        handle.rect(6, 5, 11, 7, 3.2);
        handle.rect(10, 7, 12, 13, 4.0);
        handle.bevel(0.35);
        BufferedImage image = handle.light(palette);
        HeightField socket = new HeightField();
        socket.rect(2, 2, 7, 7, 3.6);
        socket.clearDisc(4.5, 4.5, 1.0);
        socket.bevel(0.3);
        BufferedImage stone = socket.light(Palette.of("crank_socket", BootstrapArt.STONE));
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if ((stone.getRGB(x, y) >>> 24) != 0) {
                    image.setRGB(x, y, stone.getRGB(x, y));
                }
            }
        }
        return image;
    }

    /** Three angular, separately outlined fired fragments, ready to return to a clay body. */
    public static BufferedImage ceramicGrog(Palette palette) {
        HeightField field = new HeightField();
        field.rect(2, 7, 6, 12, 3.4);
        field.rect(5, 3, 9, 7, 4.1);
        field.rect(10, 8, 13, 12, 3.0);
        field.set(2, 7, 0.0);
        field.set(9, 3, 0.0);
        field.rect(3, 8, 5, 9, 3.9);
        field.rect(6, 4, 8, 5, 4.5);
        field.rect(11, 9, 12, 10, 3.5);
        field.set(5, 11, 2.7);
        field.set(7, 6, 3.1);
        field.bevel(0.55);
        Palette fired = new Palette(palette.name(), palette.base(),
                Palette.shade(palette.base(), 0.22), palette.shadow());
        BufferedImage image = field.light(fired);
        surfacePixel(image, 4, 10, Palette.shade(palette.base(), -0.25));
        surfacePixel(image, 7, 6, Palette.shade(palette.base(), -0.25));
        surfacePixel(image, 11, 11, Palette.shade(palette.base(), -0.25));
        return image;
    }

    /** Stacked damp blanks, distinct from the finished ribbed insulator. */
    public static BufferedImage unfiredCeramic(Palette palette) {
        HeightField field = new HeightField();
        field.rect(2, 8, 13, 12, 2.6);
        field.rect(3, 5, 12, 8, 3.8);
        field.rect(4, 3, 11, 5, 4.8);
        // Undercut front edges separate the three damp blanks; none of their mask is removed.
        field.rect(3, 8, 12, 8, 1.9);
        field.rect(4, 5, 11, 5, 2.7);
        field.set(12, 10, 2.2);
        field.bevel(0.45);
        Palette clay = new Palette(palette.name(), palette.base(),
                Palette.shade(palette.base(), 0.16), Palette.shade(palette.base(), -0.28));
        BufferedImage image = field.light(clay);
        surfacePixel(image, 6, 4, clay.highlight());
        surfacePixel(image, 7, 4, clay.highlight());
        surfacePixel(image, 9, 6, Palette.shade(palette.base(), -0.18));
        surfacePixel(image, 10, 6, Palette.shade(palette.base(), -0.18));
        return image;
    }

    /** A ribbed ceramic spool with a visible bore, not a recolored data core. */
    public static BufferedImage ceramicInsulator(Palette palette) {
        HeightField field = new HeightField();
        field.rect(5, 3, 10, 12, 3.2);
        field.rect(3, 3, 12, 5, 4.3);
        field.rect(3, 7, 12, 9, 4.3);
        field.rect(3, 11, 12, 13, 4.3);
        field.clearDisc(7.5, 4.0, 1.3);
        for (int y : new int[]{4, 8, 12}) {
            for (int x = 3; x <= 12; x++) {
                if (field.solid(x, y)) {
                    double across = 1.0 - Math.abs(x - 7.5) / 4.5;
                    field.set(x, y, 3.4 + across * 1.4);
                }
            }
        }
        field.bevel(0.35);
        Palette glazed = new Palette(palette.name(), palette.base(),
                Palette.shade(palette.base(), 0.20), Palette.shade(palette.base(), -0.32));
        BufferedImage image = field.light(glazed);
        surfacePixel(image, 4, 7, glazed.highlight());
        surfacePixel(image, 5, 7, glazed.highlight());
        surfacePixel(image, 6, 3, glazed.shadow());
        return image;
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

    /** A drawn strand: thinner than a rod, with a second stroke so it reads as cable. */
    public static BufferedImage wire(Palette palette) {
        HeightField field = new HeightField();
        for (int y = 2; y <= 13; y++) {
            double wave = Math.sin((y - 2) * 0.7) * 1.4;
            int cx = (int) Math.round(7.5 + wave);
            for (int x = cx - 1; x <= cx; x++) {
                double across = 1.0 - Math.abs((x - (7.5 + wave)) / 1.4);
                field.set(x, y, 2.4 + 2.6 * Math.max(0, across));
            }
            int cx2 = (int) Math.round(7.5 - wave);
            for (int x = cx2; x <= cx2 + 1; x++) {
                double across = 1.0 - Math.abs((x - (7.5 - wave)) / 1.4);
                field.set(x, y, Math.max(field.get(x, y), 2.0 + 2.2 * Math.max(0, across)));
            }
        }
        field.bevel(0.28);
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

    /** A tight pile of bright grains. Denser than crushed, not a plate. */
    public static BufferedImage concentrate(Palette palette) {
        HeightField field = new HeightField();
        Random random = new Random(palette.base() ^ 0xC0FF);
        for (int i = 0; i < 22; i++) {
            double cx = 2.5 + random.nextDouble() * 11;
            double cy = 3.0 + random.nextDouble() * 10;
            field.disc(cx, cy, 0.7 + random.nextDouble() * 0.6, 2.2 + random.nextDouble());
        }
        field.bevel(0.15);
        return field.light(palette);
    }

    /** A flat, dull smear. The residue, not the metal. */
    public static BufferedImage tailings(Palette palette) {
        HeightField field = new HeightField();
        Random random = new Random(palette.base() ^ 0x0A11);
        for (int y = 5; y <= 12; y++) {
            for (int x = 2; x <= 13; x++) {
                if (random.nextDouble() < 0.55) {
                    field.set(x, y, 0.4 + random.nextDouble() * 0.5);
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
     * Washed rubble: fewer, flatter stones than crushed, with a water band along the bottom.
     *
     * <p>The wet line has to read as the same metal after a wash, not as dust and not as oxide.
     */
    public static BufferedImage washed(Palette palette) {
        HeightField field = new HeightField();
        Random random = new Random(palette.base() ^ 0x1A7E);
        for (int i = 0; i < 8; i++) {
            double cx = 3.0 + random.nextDouble() * 10;
            double cy = 3.0 + random.nextDouble() * 7;
            field.disc(cx, cy, 1.6 + random.nextDouble() * 0.8, 1.8 + random.nextDouble());
        }
        field.bevel(0.45);
        BufferedImage img = field.light(palette);
        int water = 0xFF4FC3F7;
        for (int y = 11; y < SIZE; y++) {
            for (int x = 2; x < SIZE - 2; x++) {
                if ((img.getRGB(x, y) >>> 24) != 0) {
                    img.setRGB(x, y, water);
                }
            }
        }
        return img;
    }

    /**
     * A sintered cake: one rounded mound, smoother than crushed rubble.
     *
     * <p>Roast is the reason this form exists, so it has to read as fired material rather than
     * as another pile of fragments or as an ingot that is not quite done.
     */
    public static BufferedImage oxide(Palette palette) {
        HeightField field = new HeightField();
        field.disc(7.5, 8.0, 5.0, 4.6);
        field.disc(7.5, 7.4, 3.2, 5.4);
        field.disc(6.2, 9.0, 2.0, 3.8);
        field.bevel(0.35);
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

    /** An unsealed ceramic carrier, a raised data inlay and three exposed contact fingers. */
    public static BufferedImage bootstrapDataCore(Palette palette) {
        HeightField contacts = new HeightField();
        for (int x : new int[]{4, 7, 10}) {
            contacts.rect(x, 12, x + 1, 14, 2.8);
        }
        BufferedImage image = contacts.light(CORE_CONTACT);
        HeightField body = new HeightField();
        body.rect(3, 4, 11, 11, 3.0);
        body.rect(4, 3, 10, 12, 3.0);
        body.rect(4, 4, 10, 9, 3.7);
        body.rect(4, 10, 10, 11, 2.5);
        body.set(10, 4, 2.6);
        body.bevel(0.28);
        overlaySolid(image, body.light(CORE_CERAMIC));
        HeightField inlay = new HeightField();
        inlay.rect(5, 5, 8, 9, 4.2);
        inlay.rect(6, 4, 7, 10, 4.7);
        inlay.bevel(0.22);
        overlaySolid(image, inlay.light(palette));
        surfacePixel(image, 4, 4, CORE_CERAMIC.highlight());
        surfacePixel(image, 5, 4, CORE_CERAMIC.highlight());
        surfacePixel(image, 9, 6, CORE_CERAMIC.shadow());
        surfacePixel(image, 9, 7, CORE_CERAMIC.shadow());
        surfacePixel(image, 6, 5, palette.highlight());
        for (int x : new int[]{4, 7, 10}) {
            surfacePixel(image, x, 13, CORE_CONTACT.highlight());
        }
        return image;
    }

    /** The same substrate sealed in a tall green capsule with a metal mount and side clamps. */
    public static BufferedImage calibratedDataCore(Palette palette) {
        HeightField mount = new HeightField();
        mount.rect(4, 2, 11, 13, 3.1);
        mount.rect(2, 5, 13, 10, 3.1);
        mount.rect(5, 13, 6, 14, 2.5);
        mount.rect(9, 13, 10, 14, 2.5);
        mount.bevel(0.25);
        BufferedImage image = mount.light(CORE_CONTACT);
        HeightField body = new HeightField();
        body.rect(5, 3, 10, 12, 3.7);
        body.rect(4, 5, 11, 10, 3.7);
        body.bevel(0.24);
        overlaySolid(image, body.light(CORE_CERAMIC));
        HeightField capsule = new HeightField();
        capsule.rect(6, 4, 9, 10, 4.6);
        capsule.disc(7.5, 6.5, 2.1, 5.3);
        capsule.rect(7, 10, 8, 11, 3.6);
        capsule.bevel(0.25);
        overlaySolid(image, capsule.light(palette));
        surfacePixel(image, 6, 2, CORE_CONTACT.highlight());
        surfacePixel(image, 7, 2, CORE_CONTACT.highlight());
        surfacePixel(image, 3, 5, CORE_CONTACT.highlight());
        surfacePixel(image, 12, 5, CORE_CONTACT.base());
        surfacePixel(image, 3, 9, CORE_CONTACT.shadow());
        surfacePixel(image, 6, 5, palette.highlight());
        surfacePixel(image, 8, 10, palette.shadow());
        return image;
    }

    /** Three discrete relay bodies sit above a ceramic tray and a visible branching contact bus. */
    public static BufferedImage relayMatrix(Palette palette) {
        HeightField frame = new HeightField();
        frame.rect(1, 5, 14, 12, 3.0);
        frame.rect(2, 4, 13, 13, 3.0);
        frame.bevel(0.22);
        BufferedImage image = frame.light(CORE_CONTACT);
        HeightField tray = new HeightField();
        tray.rect(3, 5, 12, 11, 3.2);
        tray.rect(4, 6, 11, 10, 3.7);
        tray.bevel(0.25);
        overlaySolid(image, tray.light(CORE_CERAMIC));
        HeightField bus = new HeightField();
        bus.rect(4, 7, 11, 8, 3.4);
        bus.rect(7, 7, 8, 11, 3.7);
        overlaySolid(image, bus.light(CORE_CONTACT));
        for (int x = 5; x <= 11; x++) {
            surfacePixel(image, x, 7, CORE_CONTACT.highlight());
        }
        HeightField relays = new HeightField();
        relays.rect(2, 2, 6, 6, 4.4);
        relays.rect(9, 2, 13, 6, 4.4);
        relays.rect(6, 8, 10, 12, 4.1);
        relays.bevel(0.22);
        overlaySolid(image, relays.light(Palette.of("relay_shell", 0x7E8C87)));
        HeightField caps = new HeightField();
        caps.rect(3, 3, 5, 5, 5.4);
        caps.rect(10, 3, 12, 5, 5.4);
        caps.rect(7, 9, 9, 11, 5.2);
        caps.bevel(0.25);
        overlaySolid(image, caps.light(palette));
        surfacePixel(image, 3, 2, 0xC4CDC4);
        surfacePixel(image, 4, 2, 0xAAB8B0);
        surfacePixel(image, 10, 2, 0xAAB8B0);
        surfacePixel(image, 3, 3, palette.highlight());
        surfacePixel(image, 10, 3, palette.highlight());
        surfacePixel(image, 7, 9, palette.highlight());
        surfacePixel(image, 4, 12, CORE_CONTACT.highlight());
        surfacePixel(image, 5, 12, CORE_CONTACT.base());
        return image;
    }

    /** Composites local solid surfaces; all layers use the unchanged upper-left HeightField light. */
    private static void overlaySolid(BufferedImage image, BufferedImage layer) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if ((layer.getRGB(x, y) >>> 24) != 0) {
                    image.setRGB(x, y, layer.getRGB(x, y));
                }
            }
        }
    }

    /** A deliberate material mark may shade a surface, never grow the existing silhouette. */
    private static void surfacePixel(BufferedImage image, int x, int y, int rgb) {
        if ((image.getRGB(x, y) >>> 24) != 0) {
            image.setRGB(x, y, 0xFF000000 | rgb);
        }
    }

    /** Legacy chip relief retained unchanged for advanced_data_core, outside the T0 art batch. */
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

    /** A cooled, fractured lump with two recessed pores and one small glassy inclusion. */
    public static BufferedImage slag(Palette palette) {
        HeightField field = new HeightField();
        field.disc(7.5, 8.0, 4.2, 4.0);
        field.disc(5.2, 6.4, 2.2, 3.2);
        field.disc(10.0, 7.2, 2.0, 3.0);
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                if (field.solid(x, y)) {
                    double facet = x + y < 14 ? 0.6 : x > 8 ? -0.7 : 0.0;
                    field.set(x, y, Math.max(0.4, field.get(x, y) + facet));
                }
            }
        }
        field.set(6, 8, 1.0);
        field.set(9, 7, 1.4);
        field.bevel(0.25);
        Palette cooled = new Palette(palette.name(), palette.base(), 0x948C7E, 0x393936);
        BufferedImage image = field.light(cooled);
        surfacePixel(image, 6, 8, cooled.shadow());
        surfacePixel(image, 9, 7, cooled.shadow());
        surfacePixel(image, 6, 5, 0x9CA59C);
        surfacePixel(image, 7, 5, 0x7F8A83);
        return image;
    }

    /** Placeholder sprite for the Prospector's Scanner: a dish on a grip. See {@link #dataCore}. */
    public static BufferedImage scanner(Palette palette) {
        HeightField field = new HeightField();
        field.disc(8.0, 6.0, 4.4, 4.6);
        field.disc(8.0, 6.0, 2.2, 2.4);
        for (int y = 9; y <= 14; y++) {
            for (int x = 7; x <= 9; x++) {
                field.set(x, y, 2.8);
            }
        }
        field.rect(6, 13, 10, 14, 3.4);
        field.bevel(0.35);
        return field.light(palette);
    }

    /** Placeholder sprite for the quest book: a bound volume with a marked spine. See {@link #dataCore}. */
    public static BufferedImage questBook(Palette palette) {
        HeightField field = new HeightField();
        field.rect(3, 2, 12, 13, 3.2);
        field.rect(3, 2, 5, 13, 4.6);
        field.rect(7, 4, 11, 5, 4.0);
        field.rect(7, 7, 11, 8, 4.0);
        field.rect(7, 10, 10, 11, 4.0);
        field.bevel(0.35);
        return field.light(palette);
    }

    /** Placeholder sprite for the field guide: an open page with rules. See {@link #dataCore}. */
    public static BufferedImage fieldGuide(Palette palette) {
        HeightField field = new HeightField();
        field.rect(2, 2, 13, 13, 3.0);
        field.rect(4, 4, 11, 5, 4.2);
        field.rect(4, 7, 11, 8, 3.8);
        field.rect(4, 9, 11, 10, 3.8);
        field.rect(4, 11, 9, 12, 3.8);
        field.bevel(0.35);
        return field.light(palette);
    }

    /** Placeholder sprite for the Process Atlas: a tablet with three graph nodes. See {@link #dataCore}. */
    public static BufferedImage atlas(Palette palette) {
        HeightField field = new HeightField();
        field.rect(3, 2, 12, 13, 3.4);
        field.rect(5, 4, 10, 10, 4.2);
        field.disc(6.0, 6.0, 1.2, 5.2);
        field.disc(10.0, 6.0, 1.2, 5.2);
        field.disc(8.0, 9.0, 1.2, 5.2);
        field.bevel(0.35);
        return field.light(palette);
    }

    /** Placeholder sprite for the Flux Conduit: a wound cable on a grip. See {@link #dataCore}. */
    public static BufferedImage conduit(Palette palette) {
        HeightField field = new HeightField();
        double centreX = 8.0;
        double centreY = 6.0;
        for (int y = 1; y <= 10; y++) {
            for (int x = 2; x <= 13; x++) {
                double radius = Math.hypot(x - centreX, y - centreY);
                if (radius <= 5.2 && radius >= 2.2) {
                    double across = 1.0 - Math.abs((radius - 3.7) / 1.5);
                    double wind = 0.5 * Math.max(0, Math.cos(Math.atan2(y - centreY, x - centreX) * 5));
                    field.set(x, y, 2.4 + 2.0 * Math.max(0, across) + wind);
                }
            }
        }
        field.rect(7, 10, 9, 14, 3.0);
        field.rect(6, 13, 10, 14, 3.4);
        field.bevel(0.35);
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

    /** Placeholder sprite for a forming die: a stamp with a recessed face. See {@link #dataCore}. */
    public static BufferedImage die(Palette palette) {
        HeightField field = new HeightField();
        field.rect(3, 3, 12, 12, 3.6);
        field.rect(5, 5, 10, 10, 2.0);
        field.rect(6, 1, 9, 2, 2.8);
        field.bevel(0.4);
        return field.light(palette);
    }

    /** Placeholder sprite for a copper coil: a wound torus. See {@link #dataCore}. */
    public static BufferedImage coil(Palette palette) {
        HeightField field = new HeightField();
        double centre = (SIZE - 1) / 2.0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double radius = Math.hypot(x - centre, y - centre);
                if (radius <= 6.2 && radius >= 2.6) {
                    double across = 1.0 - Math.abs((radius - 4.4) / 1.8);
                    double wind = 0.6 * Math.max(0, Math.cos(Math.atan2(y - centre, x - centre) * 6));
                    field.set(x, y, 2.6 + 2.2 * Math.max(0, across) + wind);
                }
            }
        }
        field.bevel(0.35);
        return field.light(palette);
    }

    /** Placeholder sprite for a motor: a canned housing with a shaft. See {@link #dataCore}. */
    public static BufferedImage motor(Palette palette) {
        HeightField field = new HeightField();
        field.rect(3, 4, 12, 12, 3.4);
        double centre = 8.0;
        for (int y = 5; y <= 11; y++) {
            for (int x = 4; x <= 11; x++) {
                double radius = Math.hypot(x - centre, y - 8.0);
                if (radius <= 3.6 && radius >= 1.6) {
                    double across = 1.0 - Math.abs((radius - 2.6) / 1.0);
                    field.set(x, y, 4.2 + 1.4 * Math.max(0, across));
                }
            }
        }
        field.rect(7, 1, 8, 4, 3.8);
        field.bevel(0.4);
        return field.light(palette);
    }

    /** Placeholder sprite for a vanadia pellet: a ceramic bead with oxide speckles. */
    public static BufferedImage vanadia(Palette palette) {
        HeightField field = new HeightField();
        double centre = 8.0;
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 12; x++) {
                double radius = Math.hypot(x - centre, y - centre);
                if (radius <= 5.4) {
                    double across = 1.0 - radius / 5.4;
                    field.set(x, y, 2.4 + 2.8 * Math.max(0, across));
                }
            }
        }
        field.rect(6, 6, 7, 7, 4.6);
        field.rect(9, 8, 10, 9, 4.4);
        field.bevel(0.4);
        return field.light(palette);
    }

    /** A bit, a shaft and a grip. Not the Multitool's disc head. */
    public static BufferedImage drill(Palette palette) {
        HeightField field = new HeightField();
        field.rect(6, 1, 9, 4, 3.4);
        field.rect(7, 4, 8, 12, 2.4);
        field.rect(4, 11, 11, 15, 4.0);
        field.bevel(0.4);
        return field.light(palette);
    }

    /** A canister with a cap. Not a die and not a coil. */
    public static BufferedImage cell(Palette palette) {
        HeightField field = new HeightField();
        field.rect(5, 3, 10, 14, 3.2);
        field.rect(6, 1, 9, 3, 2.6);
        field.rect(6, 7, 9, 8, 4.0);
        field.bevel(0.4);
        return field.light(palette);
    }

    /** A folded sheet with a marked corner. Not the atlas. */
    public static BufferedImage blueprintTool(Palette palette) {
        HeightField field = new HeightField();
        field.rect(3, 2, 12, 14, 2.6);
        field.rect(9, 2, 13, 6, 3.4);
        field.rect(4, 8, 8, 12, 3.2);
        field.bevel(0.35);
        return field.light(palette);
    }

    /** The sheet the tool writes. A frame and one filled cell. */
    public static BufferedImage blueprint(Palette palette) {
        HeightField field = new HeightField();
        field.rect(2, 2, 13, 14, 2.2);
        field.rect(4, 4, 7, 7, 3.6);
        field.rect(8, 8, 11, 11, 3.2);
        field.bevel(0.3);
        return field.light(palette);
    }

    /** Placeholder sprite for electronic silicon: a pale crystal, distinct from the grey chunk. */
    public static BufferedImage electronicSilicon(Palette palette) {
        HeightField field = new HeightField();
        field.rect(7, 2, 8, 13, 3.6);
        field.rect(5, 4, 10, 11, 4.4);
        field.rect(6, 6, 9, 9, 5.6);
        field.disc(8.0, 7.5, 1.2, 6.2);
        field.bevel(0.25);
        return field.light(palette);
    }

    /** Placeholder sprite for metallurgical silicon: a faceted grey chunk. */
    public static BufferedImage metallurgicalSilicon(Palette palette) {
        HeightField field = new HeightField();
        field.rect(4, 5, 11, 11, 3.4);
        field.rect(5, 3, 10, 6, 4.4);
        field.rect(6, 9, 10, 13, 4.0);
        field.disc(8.0, 7.5, 1.8, 5.2);
        field.bevel(0.3);
        return field.light(palette);
    }

    /**
     * Placeholder sprite for the supraluminal station: a wide hull and a ring, not a nose
     * and not fins. See {@link #rocket}.
     */
    public static BufferedImage station(Palette palette) {
        HeightField field = new HeightField();
        field.rect(3, 5, 12, 11, 3.8);
        field.rect(1, 6, 3, 10, 2.4);
        field.rect(12, 6, 14, 10, 2.4);
        field.disc(8.0, 8.0, 2.2, 5.4);
        field.rect(7, 2, 8, 5, 4.6);
        field.bevel(0.3);
        return field.light(palette);
    }

    /** A dome. The harness helmet, not a drill. */
    public static BufferedImage harnessHelmet(Palette palette) {
        HeightField field = new HeightField();
        field.disc(8, 7, 5.2, 4.0);
        field.rect(3, 10, 13, 13, 2.4);
        field.bevel(0.3);
        return field.light(palette);
    }

    /** Placeholder sprite for the survey rocket: a body, a nose and two fins. See {@link #dataCore}. */
    public static BufferedImage rocket(Palette palette) {
        HeightField field = new HeightField();
        field.rect(6, 3, 9, 13, 4.2);
        field.rect(7, 1, 8, 4, 5.2);
        field.rect(3, 10, 6, 13, 2.6);
        field.rect(9, 10, 12, 13, 2.6);
        field.rect(7, 12, 8, 15, 3.0);
        field.bevel(0.3);
        return field.light(palette);
    }

    /** A plate with straps. The harness chest, not a casing cube. */
    public static BufferedImage harnessChest(Palette palette) {
        HeightField field = new HeightField();
        field.rect(3, 2, 13, 14, 3.2);
        field.rect(1, 3, 3, 8, 2.6);
        field.rect(13, 3, 15, 8, 2.6);
        field.bevel(0.25);
        return field.light(palette);
    }

    /** Two legs. Not a pair of boots. */
    public static BufferedImage harnessLegs(Palette palette) {
        HeightField field = new HeightField();
        field.rect(3, 2, 7, 14, 3.0);
        field.rect(9, 2, 13, 14, 3.0);
        field.rect(3, 2, 13, 5, 3.4);
        field.bevel(0.25);
        return field.light(palette);
    }

    /** A pair of boots. Low, not a leg. */
    public static BufferedImage harnessBoots(Palette palette) {
        HeightField field = new HeightField();
        field.rect(2, 8, 7, 14, 3.2);
        field.rect(9, 8, 14, 14, 3.2);
        field.bevel(0.3);
        return field.light(palette);
    }

    /** A framed plate. The exosuit, not the harness's solid chest. */
    public static BufferedImage exosuit(Palette palette) {
        HeightField field = new HeightField();
        field.rect(3, 2, 13, 14, 2.4);
        field.rect(5, 4, 11, 12, 4.0);
        field.rect(1, 3, 3, 7, 3.0);
        field.rect(13, 3, 15, 7, 3.0);
        field.bevel(0.2);
        return field.light(palette);
    }

    /** A short mast. The tap, not a cable. */
    public static BufferedImage networkTap(Palette palette) {
        HeightField field = new HeightField();
        field.rect(6, 8, 10, 14, 2.6);
        field.rect(7, 2, 9, 8, 4.2);
        field.disc(8, 2, 2.2, 5.0);
        field.bevel(0.25);
        return field.light(palette);
    }

    /** A spring. Legs, not boots. */
    public static BufferedImage exoskeleton(Palette palette) {
        HeightField field = new HeightField();
        field.rect(4, 2, 7, 14, 3.2);
        field.rect(9, 2, 12, 14, 3.2);
        field.rect(4, 6, 12, 8, 4.0);
        field.bevel(0.3);
        return field.light(palette);
    }

    /** A brick battery. Not the drill cell's cylinder. */
    public static BufferedImage fluxCell(Palette palette) {
        HeightField field = new HeightField();
        field.rect(4, 2, 12, 14, 3.6);
        field.rect(6, 1, 10, 3, 4.4);
        field.bevel(0.2);
        return field.light(palette);
    }

    /** A dense pellet. Matter, not a dust. */
    public static BufferedImage matter(Palette palette) {
        HeightField field = new HeightField();
        field.disc(8, 8, 5.5, 4.2);
        field.disc(8, 8, 2.2, 5.4);
        field.bevel(0.25);
        return field.light(palette);
    }

    /** Two corner brackets. A mark, not a wrench. */
    public static BufferedImage planner(Palette palette) {
        HeightField field = new HeightField();
        field.rect(2, 2, 6, 3, 3.4);
        field.rect(2, 2, 3, 6, 3.4);
        field.rect(10, 12, 14, 13, 3.4);
        field.rect(13, 9, 14, 13, 3.4);
        field.bevel(0.35);
        return field.light(palette);
    }

    /** Placeholder sprite for helium-3: a pale sealed cell, not a rock. */
    public static BufferedImage helium3(Palette palette) {
        HeightField field = new HeightField();
        field.disc(8.0, 8.0, 4.6, 4.2);
        field.disc(8.0, 8.0, 2.4, 5.4);
        field.rect(7, 2, 8, 4, 3.2);
        field.rect(6, 1, 9, 2, 2.6);
        field.bevel(0.3);
        return field.light(palette);
    }

    /** Placeholder sprite for a refractory brick: a fired block with a mortar cross. */
    public static BufferedImage brick(Palette palette) {
        HeightField field = new HeightField();
        field.rect(2, 4, 13, 11, 3.6);
        field.rect(7, 4, 8, 11, 2.2);
        field.rect(2, 7, 13, 8, 2.2);
        field.bevel(0.35);
        return field.light(palette);
    }
}
