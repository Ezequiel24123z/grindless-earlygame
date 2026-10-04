package io.github.ezequiel24123z.grindless.item;

/**
 * Multitool rotate arithmetic, without Minecraft (ADR-0069).
 *
 * <p>{@code VerifyMultitool} dumps this. Pickup and NBT live on the item; this is the facing
 * cycle that {@code Block.rotate(CLOCKWISE_90)} uses for horizontal facings.
 */
public final class MultitoolLogic {

    private MultitoolLogic() {
    }

    /**
     * Next horizontal facing, clockwise looking down. Unknown names are returned unchanged so a
     * block without a facing is a no-op rather than a crash.
     */
    public static String rotateClockwise(String facing) {
        if (facing == null) {
            return null;
        }
        return switch (facing) {
            case "north" -> "east";
            case "east" -> "south";
            case "south" -> "west";
            case "west" -> "north";
            default -> facing;
        };
    }

    public static boolean sneakMeansRelocate() {
        return true;
    }
}
