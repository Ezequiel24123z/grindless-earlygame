package io.github.ezequiel24123z.grindless.forge.overlay;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.client.inspection.MachineInspectionPanel;
import io.github.ezequiel24123z.grindless.client.inspection.MachineInspectionTooltip;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.ClientState;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.Reply;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.ReplyKind;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.Request;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionProtocol.Target;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Position;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

/** An explicitly native, opt-in equivalent. No Jade, WAILA or TOP API is loaded or required. */
@Mod.EventBusSubscriber(modid = Grindless.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MachineInspectionOverlayClient {

    private static final KeyMapping TOGGLE = new KeyMapping("key.grindless.machine_inspection",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.grindless");
    private static final ClientState STATE = new ClientState();
    private static WeakReference<ClientLevel> scope = new WeakReference<>(null);
    private static long clock;
    private static int page;
    private static boolean expanded;
    private static boolean reportedMalformed;

    private MachineInspectionOverlayClient() {
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientTickEvent.CLIENT_POST.register(MachineInspectionOverlayClient::tick);
            ClientGuiEvent.RENDER_HUD.register(MachineInspectionOverlayClient::render);
            ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> clear());
            ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register((oldPlayer, newPlayer) -> clear());
            ClientLifecycleEvent.CLIENT_STOPPING.register(client -> clear());
        });
    }

    private static void tick(Minecraft client) {
        if (scope.get() != client.level) {
            clear();
            scope = new WeakReference<>(client.level);
        }
        while (TOGGLE.consumeClick()) {
            if (client.player == null) {
                continue;
            }
            if (Screen.hasShiftDown() && MachineInspectionOverlay.ENABLED.get()) {
                page++;
            } else {
                boolean enabled = !MachineInspectionOverlay.ENABLED.get();
                MachineInspectionOverlay.ENABLED.set(enabled);
                MachineInspectionOverlay.CLIENT_SPEC.save();
                STATE.reset();
                page = 0;
                client.player.displayClientMessage(Component.translatable(enabled
                        ? suppressed() ? "inspection.grindless.suppressed" : "inspection.grindless.enabled"
                        : "inspection.grindless.disabled"), true);
            }
        }
        if (client.level == null || client.player == null || client.getConnection() == null
                || !MachineInspectionOverlay.ENABLED.get() || suppressed()
                || client.screen != null || client.getOverlay() != null || client.options.hideGui) {
            STATE.reset();
            return;
        }
        if (client.isPaused()) {
            return;
        }
        clock++;
        Target target = aimed(client);
        if (!Objects.equals(STATE.target(), target)) {
            page = 0;
        }
        var sequence = STATE.request(target, clock);
        if (target != null && sequence.isPresent()
                && MachineInspectionNetwork.available(client.getConnection().getConnection())) {
            MachineInspectionNetwork.request(new Request(sequence.getAsInt(), target.position()));
        }
    }

    public static void receive(Reply reply) {
        Minecraft client = Minecraft.getInstance();
        if (scope.get() != client.level || client.level == null) {
            clear();
            return;
        }
        if (STATE.receive(reply, clock)) {
            if (reply.kind() == ReplyKind.MALFORMED && !reportedMalformed) {
                Grindless.LOG.warn("Native machine inspection received invalid data; no machine state is assumed");
                reportedMalformed = true;
            } else if (reply.kind() == ReplyKind.AVAILABLE) {
                reportedMalformed = false;
            }
        }
    }

    private static Target aimed(Minecraft client) {
        if (client.level == null || client.player == null || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || hit.getLocation().distanceToSqr(client.player.getEyePosition())
                > MachineInspectionProtocol.MAX_DISTANCE * MachineInspectionProtocol.MAX_DISTANCE) {
            return null;
        }
        BlockPos position = hit.getBlockPos();
        if (!client.level.hasChunkAt(position)) {
            return null;
        }
        ResourceLocation block = BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(position).getBlock());
        return !block.getNamespace().equals(Grindless.MOD_ID) ? null
                : new Target(client.level.dimension().location().toString(),
                new Position(position.getX(), position.getY(), position.getZ()), block.toString());
    }

    private static void render(GuiGraphics graphics, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !MachineInspectionOverlay.ENABLED.get() || suppressed()
                || client.screen != null || client.getOverlay() != null || client.options.hideGui
                || STATE.target() == null || !Objects.equals(STATE.target(), aimed(client))) {
            return;
        }
        boolean details = Screen.hasShiftDown();
        if (details != expanded) {
            expanded = details;
            page = 0;
        }
        List<Component> lines = STATE.snapshot(clock)
                .map(snapshot -> MachineInspectionTooltip.lines(snapshot, details, MachineInspectionOverlay.DEVELOPER.get()))
                .orElseGet(() -> List.of(Component.translatable(STATE.kind() == ReplyKind.MALFORMED
                        ? "inspection.grindless.malformed" : "inspection.grindless.unavailable")));
        Component name = BuiltInRegistries.BLOCK.get(new ResourceLocation(STATE.target().blockId())).getName();
        MachineInspectionPanel.render(graphics,
                Component.translatable("inspection.grindless.native_title", name).withStyle(ChatFormatting.BOLD),
                lines, page, TOGGLE.getTranslatedKeyMessage());
    }

    private static boolean suppressed() {
        return !MachineInspectionOverlay.ALLOW_WITH_OTHER_OVERLAYS.get()
                && (ModList.get().isLoaded("jade") || ModList.get().isLoaded("waila")
                || ModList.get().isLoaded("wthit") || ModList.get().isLoaded("theoneprobe"));
    }

    private static void clear() {
        STATE.reset();
        scope.clear();
        clock = 0;
        page = 0;
        expanded = false;
        reportedMalformed = false;
    }
}
