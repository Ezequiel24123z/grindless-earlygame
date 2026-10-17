package io.github.ezequiel24123z.grindless.source;

/** World conditions a real renewable producer requires; also exercised without booting Minecraft. */
public enum SourceSite {
    NONE, GEOLOGICAL, SUNLIT_CULTURE;

    /** Stable menu values; append new failures rather than reordering them. */
    public enum Fault {
        NONE, WRONG_DIMENSION, NEEDS_ROCK_BED, NEEDS_OPEN_SKY, NEEDS_DAYLIGHT;

        public String translationKey() {
            return "gui.grindless.source." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public Fault evaluate(boolean terrestrial, boolean rockBed, boolean openSky, boolean daylight) {
        if (this == NONE) {
            return Fault.NONE;
        }
        if (!terrestrial) {
            return Fault.WRONG_DIMENSION;
        }
        if (this == GEOLOGICAL) {
            return rockBed ? Fault.NONE : Fault.NEEDS_ROCK_BED;
        }
        if (!openSky) {
            return Fault.NEEDS_OPEN_SKY;
        }
        return daylight ? Fault.NONE : Fault.NEEDS_DAYLIGHT;
    }
}
