package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.network.FluxNetworkData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * While an exosuit is worn: a tap pulls from pylon coverage, and legs spend a cell for speed.
 */
public final class ExosuitTick {

    private static final UUID LEGS_ID = UUID.fromString("c0ffee00-0090-4000-8000-000000000090");

    private static final AttributeModifier LEGS = new AttributeModifier(
            LEGS_ID, "grindless_exoskeleton", ExosuitLogic.SPEED, AttributeModifier.Operation.ADDITION);

    private ExosuitTick() {
    }

    public static void tickLevel(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            tick(player);
        }
    }

    static void tick(Player player) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        boolean tap = false;
        boolean legs = false;
        int room = 0;
        for (ItemStack stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ExosuitItem)) {
                continue;
            }
            ExosuitLogic.Piece piece = ExosuitItem.piece(stack);
            tap = tap || piece.has(ExosuitLogic.NETWORK_TAP);
            legs = legs || piece.has(ExosuitLogic.EXOSKELETON);
            room += ExosuitLogic.room(piece);
        }
        if (tap && room > 0) {
            int drawn = draw(server, player, Math.min(ExosuitLogic.TAP_FU, room));
            if (drawn > 0) {
                distribute(player, drawn);
            }
        }
        boolean moving = legs && spend(player, ExosuitLogic.LEGS_FU);
        applySpeed(player, moving);
    }

    private static int draw(ServerLevel level, Player player, int want) {
        FluxNetwork network = FluxNetworkData.get(level).networkCovering(player.blockPosition());
        if (network == null || want <= 0) {
            return 0;
        }
        long drawn = network.extract(want, false);
        return drawn > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) drawn;
    }

    private static void distribute(Player player, int fu) {
        int left = fu;
        for (ItemStack stack : player.getArmorSlots()) {
            if (left <= 0 || !(stack.getItem() instanceof ExosuitItem)) {
                continue;
            }
            ExosuitLogic.Piece piece = ExosuitItem.piece(stack);
            int before = ExosuitLogic.room(piece);
            ExosuitLogic.Piece next = ExosuitLogic.addCharge(piece, left);
            int taken = before - ExosuitLogic.room(next);
            if (taken > 0) {
                ExosuitItem.write(stack, next);
                left -= taken;
            }
        }
    }

    /** Spends {@code fu} from the first cell that has it. Returns whether the whole cost was paid. */
    private static boolean spend(Player player, int fu) {
        for (ItemStack stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ExosuitItem)) {
                continue;
            }
            ExosuitLogic.Spent spent = ExosuitLogic.spend(ExosuitItem.piece(stack), fu);
            if (spent.spent() <= 0) {
                continue;
            }
            ExosuitItem.write(stack, spent.piece());
            return spent.spent() == fu;
        }
        return false;
    }

    private static void applySpeed(Player player, boolean active) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (active) {
            if (!speed.hasModifier(LEGS)) {
                speed.addTransientModifier(LEGS);
            }
        } else if (speed.hasModifier(LEGS)) {
            speed.removeModifier(LEGS);
        }
    }
}
