package io.github.ezequiel24123z.grindless.quest;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;

/** An optional renderer opener; the original book/menu and claim ledger remain registered. */
public final class QuestBackendPlatform {

    private QuestBackendPlatform() {
    }

    /** False keeps the legacy menu usable when the optional backend is absent or unavailable. */
    @ExpectPlatform
    public static boolean open(ServerPlayer player) {
        throw new AssertionError("@ExpectPlatform stub was not transformed; check the platform impl");
    }
}
