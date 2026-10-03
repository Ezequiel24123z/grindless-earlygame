package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.material.Material;
import io.github.ezequiel24123z.grindless.material.MaterialRegistry;
import io.github.ezequiel24123z.grindless.vein.ChunkVein;
import io.github.ezequiel24123z.grindless.vein.ClientSurvey;
import io.github.ezequiel24123z.grindless.vein.SurveyData;
import io.github.ezequiel24123z.grindless.vein.SurveyLogic;
import io.github.ezequiel24123z.grindless.vein.SurveySync;
import io.github.ezequiel24123z.grindless.vein.VeinGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Handheld survey. Right-click marks the standing chunk and its neighbours and reports
 * the vein underfoot.
 */
public final class ProspectorsScannerItem extends Item {

    public ProspectorsScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            ChunkPos center = new ChunkPos(player.blockPosition());
            SurveyData data = SurveyData.get(server);
            List<Material> mineable = MaterialRegistry.snapshot().mineable();
            List<ClientSurvey.Entry> entries = new ArrayList<>();
            ClientSurvey.Entry standing = null;
            for (SurveyLogic.ChunkRef ref : SurveyLogic.around(center.x, center.z)) {
                ChunkPos chunk = new ChunkPos(ref.x(), ref.z());
                data.mark(chunk);
                ChunkVein vein = VeinGenerator.generate(server.getSeed(), ref.x(), ref.z(), mineable);
                ClientSurvey.Entry entry = new ClientSurvey.Entry(
                        ref.x(), ref.z(),
                        vein == null ? "" : vein.material(),
                        vein == null ? 0.0 : vein.richness(),
                        vein == null ? 0L : vein.reserve());
                entries.add(entry);
                if (ref.x() == center.x && ref.z() == center.z) {
                    standing = entry;
                }
            }
            SurveySync.sendTo(serverPlayer, entries);
            if (standing != null && !standing.material().isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "chat.grindless.scanner.vein",
                        Component.translatable("material.grindless." + standing.material()),
                        String.format(java.util.Locale.ROOT, "%.2f", standing.richness()),
                        standing.reserve()), true);
            } else {
                player.displayClientMessage(Component.translatable("chat.grindless.scanner.empty"),
                        true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
