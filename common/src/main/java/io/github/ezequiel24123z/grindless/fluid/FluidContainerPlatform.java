package io.github.ezequiel24123z.grindless.fluid;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/** Loader-owned fluid-container capabilities; the Basic Tank keeps its native storage. */
public final class FluidContainerPlatform {

    private FluidContainerPlatform() {
    }

    @ExpectPlatform
    public static InteractionResult interact(Player player, InteractionHand hand,
                                             BasicTankBlockEntity tank, Direction face) {
        throw new AssertionError("@ExpectPlatform stub was not transformed; check the platform impl");
    }
}
