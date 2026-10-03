package io.github.ezequiel24123z.grindless.research;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Whether a blueprint is unlocked in this world — server save or client cache.
 */
public final class ResearchAccess {

    private ResearchAccess() {
    }

    public static boolean isUnlocked(Level level, Blueprint blueprint) {
        if (level == null || blueprint == null) {
            return false;
        }
        if (level instanceof ServerLevel server) {
            return ResearchData.get(server).isUnlocked(blueprint);
        }
        return ClientResearch.isUnlocked(blueprint);
    }
}
