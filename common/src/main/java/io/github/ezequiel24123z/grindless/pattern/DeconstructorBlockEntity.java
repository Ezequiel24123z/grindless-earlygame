package io.github.ezequiel24123z.grindless.pattern;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.PoweredLogisticsBlockEntity;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import io.github.ezequiel24123z.grindless.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One item becomes one Matter. Matter smashed again is still one Matter (ADR-0088).
 */
public class DeconstructorBlockEntity extends PoweredLogisticsBlockEntity {

    public DeconstructorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECONSTRUCTOR.get(), pos, state);
    }

    /** Spends F1 and one item. A brownout leaves the stack alone. */
    public void smash(Player player, ItemStack held) {
        if (!draw()) {
            show(MachineStatus.STARVED);
            player.displayClientMessage(
                    Component.translatable("chat.grindless.pattern.starved"), true);
            return;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        ItemStack matter = new ItemStack(ModItems.MATTER.get(), PatternLogic.YIELD);
        if (!player.getInventory().add(matter)) {
            Block.popResource(level, worldPosition, matter);
        }
        show(MachineStatus.RUNNING);
        player.displayClientMessage(Component.translatable("chat.grindless.pattern.smashed"), true);
    }

    private void show(MachineStatus status) {
        if (level != null) {
            MachineProperties.publish(level, worldPosition, status);
        }
    }
}
