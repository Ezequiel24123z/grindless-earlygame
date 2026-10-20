package io.github.ezequiel24123z.grindless.shop;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import io.github.ezequiel24123z.grindless.telemetry.dev.DevTelemetry;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ShopCommands {
    private ShopCommands() {
    }

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("grindless")
                        .then(Commands.literal("shop").executes(context -> {
                            ShopAccess.open(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("economy").requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    var proof = EconomyCycleProof.check();
                                    context.getSource().sendSuccess(() -> Component.translatable("chat.grindless.shop.audit",
                                            ShopCatalogue.offers().size(), proof.conservative(), proof.unresolved().toString()), false);
                                    return proof.conservative() ? 1 : 0;
                                }))
                        .then(Commands.literal("telemetry").requires(source -> source.hasPermission(2))
                                .then(Commands.literal("start").executes(context -> {
                                    DevTelemetry.start(context.getSource().getServer());
                                    context.getSource().sendSuccess(() -> Component.translatable("chat.grindless.telemetry.started"), false);
                                    return 1;
                                }))
                                .then(Commands.literal("snapshot").executes(context -> {
                                    boolean active = DevTelemetry.active(context.getSource().getServer());
                                    DevTelemetry.report(context.getSource().getServer());
                                    context.getSource().sendSuccess(() -> Component.translatable(active
                                            ? "chat.grindless.telemetry.snapshot" : "chat.grindless.telemetry.inactive"), false);
                                    return active ? 1 : 0;
                                }))
                                .then(Commands.literal("stop").executes(context -> {
                                    DevTelemetry.stop(context.getSource().getServer());
                                    context.getSource().sendSuccess(() -> Component.translatable("chat.grindless.telemetry.stopped"), false);
                                    return 1;
                                })))));
    }
}
