package io.github.ezequiel24123z.grindless.menu;

import dev.architectury.networking.NetworkManager;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.AssemblerWork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** One ID-based intent and one authoritative menu snapshot; no new recipe or transport framework. */
public final class AssemblerSelectionNetwork {
    private static final ResourceLocation SELECT = Grindless.id("assembler_select");
    private static final ResourceLocation STATE = Grindless.id("assembler_selection");
    private static final int MAX_ROWS = 4096;

    public record Request(int container, long token, int revision, String recipe) {
    }

    public record Update(int container, long token, int revision, MachineMenuHost.RecipeSelection state,
                         Optional<List<ProcessMachineMenu.RecipeChoice>> catalogue, boolean acknowledgement) {
    }

    private AssemblerSelectionNetwork() {
    }

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SELECT, (data, context) -> {
            Request request;
            try {
                request = readRequest(data);
            } catch (io.netty.handler.codec.DecoderException | IndexOutOfBoundsException invalid) {
                Grindless.LOG.warn("Rejected malformed Assembler selection packet", invalid);
                context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        refuse(player, MachineMenuHost.SelectionResult.MALFORMED);
                        if (player.containerMenu instanceof ProcessMachineMenu menu) {
                            menu.sendSelection(true);
                        }
                    }
                });
                return;
            }
            context.queue(() -> {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    if (player.containerMenu instanceof ProcessMachineMenu menu) {
                        MachineMenuHost.SelectionResult result = menu.selectRecipe(player, request.container(),
                                request.token(), request.revision(), request.recipe());
                        if (result != MachineMenuHost.SelectionResult.SELECTED
                                && result != MachineMenuHost.SelectionResult.UNCHANGED) {
                            refuse(player, result);
                        }
                        menu.sendSelection(true);
                    } else {
                        refuse(player, MachineMenuHost.SelectionResult.CLOSED);
                    }
                }
            });
        });
    }

    /** Architectury S2C registration is client-only, just like the existing SurveySync receiver. */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, STATE, (data, context) -> {
            Update update = readUpdate(data);
            context.queue(() -> {
                if (context.getPlayer() != null
                        && context.getPlayer().containerMenu instanceof ProcessMachineMenu menu
                        && menu.containerId == update.container()) {
                    menu.receiveSelection(update.token(), update.revision(), update.state(),
                            update.catalogue(), update.acknowledgement());
                }
            });
        });
    }

    private static void refuse(ServerPlayer player, MachineMenuHost.SelectionResult result) {
        player.displayClientMessage(Component.translatable(result.translationKey()), false);
    }

    static void request(int container, long token, int revision, String recipe) {
        FriendlyByteBuf data = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        writeRequest(data, new Request(container, token, revision, recipe));
        NetworkManager.sendToServer(SELECT, data);
    }

    static void send(ServerPlayer player, int container, long token, int revision,
                     MachineMenuHost.RecipeSelection state,
                     Optional<List<ProcessMachineMenu.RecipeChoice>> catalogue, boolean acknowledgement) {
        FriendlyByteBuf data = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        writeUpdate(data, new Update(container, token, revision, state, catalogue, acknowledgement));
        NetworkManager.sendToPlayer(player, STATE, data);
    }

    public static void writeRequest(FriendlyByteBuf data, Request request) {
        data.writeInt(request.container());
        data.writeLong(request.token());
        data.writeInt(request.revision());
        data.writeUtf(request.recipe(), AssemblerWork.MAX_ID_LENGTH);
    }

    public static Request readRequest(FriendlyByteBuf data) {
        Request request = new Request(data.readInt(), data.readLong(), data.readInt(),
                data.readUtf(AssemblerWork.MAX_ID_LENGTH));
        if (data.isReadable()) {
            throw new io.netty.handler.codec.DecoderException("Trailing Assembler selection data");
        }
        return request;
    }

    public static void writeUpdate(FriendlyByteBuf data, Update update) {
        data.writeInt(update.container());
        data.writeLong(update.token());
        data.writeInt(update.revision());
        data.writeBoolean(update.acknowledgement());
        MachineMenuHost.RecipeSelection state = update.state();
        data.writeUtf(state.selected(), AssemblerWork.MAX_ID_LENGTH);
        data.writeUtf(state.activeRecipe(), AssemblerWork.MAX_ID_LENGTH);
        data.writeDouble(state.activeWork());
        data.writeEnum(state.issue());
        data.writeVarInt(state.suspended().size());
        state.suspended().forEach((id, work) -> {
            data.writeUtf(id, AssemblerWork.MAX_ID_LENGTH);
            data.writeDouble(work);
        });
        data.writeBoolean(update.catalogue().isPresent());
        update.catalogue().ifPresent(choices -> {
            data.writeVarInt(choices.size());
            for (ProcessMachineMenu.RecipeChoice choice : choices) {
                data.writeUtf(choice.id(), AssemblerWork.MAX_ID_LENGTH);
                data.writeItem(choice.output());
            }
        });
    }

    public static Update readUpdate(FriendlyByteBuf data) {
        int container = data.readInt();
        long token = data.readLong();
        int revision = data.readInt();
        boolean acknowledgement = data.readBoolean();
        String selected = data.readUtf(AssemblerWork.MAX_ID_LENGTH);
        String active = data.readUtf(AssemblerWork.MAX_ID_LENGTH);
        double progress = data.readDouble();
        if (!Double.isFinite(progress) || progress < 0.0) {
            throw new io.netty.handler.codec.DecoderException("Invalid synchronized Assembler progress");
        }
        MachineMenuHost.SelectionIssue issue = data.readEnum(MachineMenuHost.SelectionIssue.class);
        Map<String, Double> suspended = new LinkedHashMap<>();
        int count = readCount(data);
        for (int index = 0; index < count; index++) {
            String id = data.readUtf(AssemblerWork.MAX_ID_LENGTH);
            double work = data.readDouble();
            if (!Double.isFinite(work) || work <= 0.0 || suspended.putIfAbsent(id, work) != null) {
                throw new io.netty.handler.codec.DecoderException("Invalid synchronized suspended Assembler work");
            }
        }
        Optional<List<ProcessMachineMenu.RecipeChoice>> catalogue = Optional.empty();
        if (data.readBoolean()) {
            int size = readCount(data);
            List<ProcessMachineMenu.RecipeChoice> choices = new ArrayList<>(size);
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (int index = 0; index < size; index++) {
                String id = data.readUtf(AssemblerWork.MAX_ID_LENGTH);
                if (id.isBlank() || !ids.add(id)) {
                    throw new io.netty.handler.codec.DecoderException("Invalid synchronized Assembler catalogue");
                }
                choices.add(new ProcessMachineMenu.RecipeChoice(id, data.readItem()));
            }
            catalogue = Optional.of(List.copyOf(choices));
        }
        if (data.isReadable()) {
            throw new io.netty.handler.codec.DecoderException("Trailing Assembler state data");
        }
        return new Update(container, token, revision,
                new MachineMenuHost.RecipeSelection(selected, active, progress, suspended, issue),
                catalogue, acknowledgement);
    }

    private static int readCount(FriendlyByteBuf data) {
        int count = data.readVarInt();
        if (count < 0 || count > MAX_ROWS) {
            throw new io.netty.handler.codec.DecoderException("Invalid Assembler row count: " + count);
        }
        return count;
    }
}
