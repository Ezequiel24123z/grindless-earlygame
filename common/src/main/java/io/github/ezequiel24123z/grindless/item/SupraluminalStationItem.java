package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import io.github.ezequiel24123z.grindless.station.SupraluminalStation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Places a supraluminal station on a berth (ADR-0098).
 *
 * <p>The item is the station. It is manufactured, not crafted on a table. Right-click a
 * berth to set it down and sit. The climb starts once the berth has drawn its toll,
 * and the ceiling is the arrival.
 */
public final class SupraluminalStationItem extends Item {

    public SupraluminalStationItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(ModBlocks.STATION_BERTH.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }
        AABB near = new AABB(pos).inflate(1.0);
        if (!server.getEntitiesOfClass(SupraluminalStation.class, near).isEmpty()) {
            return InteractionResult.FAIL;
        }
        SupraluminalStation station = SupraluminalStation.create(server, pos);
        if (station == null || !server.addFreshEntity(station)) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        if (player != null) {
            player.startRiding(station);
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.grindless.supraluminal_station"));
    }
}
