package io.github.ezequiel24123z.grindless.pattern;

import io.github.ezequiel24123z.grindless.machine.MachineProperties;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.PoweredLogisticsBlockEntity;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.github.ezequiel24123z.grindless.recipe.ReplicationCost;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One right-click stores an item id, once, and says what the graph says it costs (ADR-0088).
 */
public class PatternScannerBlockEntity extends PoweredLogisticsBlockEntity {

    public static final TagKey<Item> BLACKLIST = TagKey.create(Registries.ITEM,
            new ResourceLocation("grindless", "replication_blacklist"));

    public PatternScannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PATTERN_SCANNER.get(), pos, state);
    }

    /** Scans {@code held}. A refusal or a brownout leaves the stack alone. */
    public void scan(Player player, ItemStack held) {
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        if (PatternLogic.refused(id) || held.is(BLACKLIST)) {
            show(MachineStatus.BLOCKED);
            player.displayClientMessage(Component.translatable("chat.grindless.pattern.refused"), true);
            return;
        }
        int cost = ReplicationCost.ofRecipes(id, ProcessLookup.recipes());
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        PatternData data = PatternData.get(server);
        if (data.has(id)) {
            show(MachineStatus.RUNNING);
            player.displayClientMessage(
                    Component.translatable("chat.grindless.pattern.known", id, cost), true);
            return;
        }
        if (!draw()) {
            show(MachineStatus.STARVED);
            player.displayClientMessage(Component.translatable("chat.grindless.pattern.starved"), true);
            return;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        data.store(id);
        show(MachineStatus.RUNNING);
        player.displayClientMessage(
                Component.translatable("chat.grindless.pattern.stored", id, cost), true);
    }

    private void show(MachineStatus status) {
        if (level != null) {
            MachineProperties.publish(level, worldPosition, status);
        }
    }
}
