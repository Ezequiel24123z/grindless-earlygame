package io.github.ezequiel24123z.grindless.assetgen;

/**
 * A material's three-tone palette: base, highlight and shadow.
 *
 * <p>Three tones rather than a gradient is a deliberate pixel-art constraint. Minecraft textures
 * are 16×16, and smooth shading at that size reads as mud; flat tones with hard edges read as a
 * shape. Every generated texture uses the same three roles in the same places, which is what makes
 * a copper plate and an iron plate look like the same object in two metals rather than two
 * different objects.
 */
public record Palette(String name, int base, int highlight, int shadow) {

    /**
     * Derives a palette from one base colour.
     *
     * <p>Most materials need no more than this — the whole point of the matrix is that adding a
     * material is a single colour, not an art task. A material whose real-world appearance needs
     * hand-picked tones can still declare all three.
     */
    public static Palette of(String name, int base) {
        return new Palette(name, base, shade(base, 0.35), shade(base, -0.35));
    }

    /** Brightens ({@code amount > 0}) or darkens a colour, per channel, clamped. */
    public static int shade(int rgb, double amount) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (clamp(adjust(r, amount)) << 16)
                | (clamp(adjust(g, amount)) << 8)
                | clamp(adjust(b, amount));
    }

    private static double adjust(int channel, double amount) {
        return amount < 0 ? channel * (1 + amount) : channel + (255 - channel) * amount;
    }

    private static int clamp(double value) {
        return (int) Math.max(0, Math.min(255, value));
    }

    /** The three tones, darkest first, for generators that index rather than name them. */
    public int[] tones() {
        return new int[]{base, highlight, shadow};
    }
}
