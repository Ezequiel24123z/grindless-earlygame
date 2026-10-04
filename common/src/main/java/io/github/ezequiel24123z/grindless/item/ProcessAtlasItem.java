package io.github.ezequiel24123z.grindless.item;

import dev.architectury.registry.menu.MenuRegistry;
import io.github.ezequiel24123z.grindless.menu.ProcessAtlasMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Handheld process-recipe viewer (ADR-0066).
 *
 * <p>Right-click opens a list of the live {@code ProcessLookup} graph. It does not solve a
 * line. Vanilla already shows T0 JSON crafts.
 */
public final class ProcessAtlasItem extends Item {

    public ProcessAtlasItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            MenuRegistry.openMenu(server, new SimpleMenuProvider(
                    (id, inventory, opener) -> new ProcessAtlasMenu(id, inventory),
                    Component.translatable("gui.grindless.atlas.title")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.atlas"));
    }
}
