package io.github.ezequiel24123z.grindless.forge;

import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.client.ResourceGuidanceClient;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Kind;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel.Resource;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;

/** Includes annotated vanilla ingredients without replacing item classes or stack/NBT behavior. */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID, value = Dist.CLIENT)
public final class ResourceGuidanceTooltips {
    private ResourceGuidanceTooltips() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ResourceGuidanceClient.item(event.getItemStack()).ifPresent(resource ->
                ResourceGuidanceClient.append(event.getToolTip(), resource, false, Screen.hasShiftDown()));
        FluidUtil.getFluidContained(event.getItemStack()).ifPresent(actual -> {
            var id = ForgeRegistries.FLUIDS.getKey(actual.getFluid());
            if (id == null) {
                return;
            }
            var guidance = ResourceGuidanceClient.lines(new Resource(Kind.FLUID, id.toString()), false, Screen.hasShiftDown());
            if (!guidance.isEmpty()) {
                event.getToolTip().add(Component.translatable("tooltip.grindless.resource.fluid_contents",
                        actual.getDisplayName(), actual.getAmount()));
                event.getToolTip().addAll(guidance);
            }
        });
    }
}
