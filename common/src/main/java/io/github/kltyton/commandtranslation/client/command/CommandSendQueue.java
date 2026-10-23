package io.github.kltyton.commandtranslation.client.command;

import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import io.github.kltyton.commandtranslation.CommandTranslation;
import io.github.kltyton.commandtranslation.client.command.candidate.LocalizedInput;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import java.util.ArrayDeque;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/** Defers packet construction until asynchronous providers have resolved the command, preserving send order. */
public final class CommandSendQueue {
    private record Pending(String text, Consumer<String> send, Language language, ClientConfig config, boolean command) {}
    private final ArrayDeque<Pending> commands = new ArrayDeque<>();
    private boolean running;

    public void send(ClientPacketListener owner, String command, Consumer<String> original) {
        commands.addLast(new Pending(command, original, Language.getInstance(), ClientConfig.current(), true));
        if (!running) { advance(owner); }
    }

    public void sendChat(ClientPacketListener owner, String message, Consumer<String> original) {
        commands.addLast(new Pending(message, original, Language.getInstance(), ClientConfig.current(), false));
        if (!running) { advance(owner); }
    }

    private void advance(ClientPacketListener owner) {
        Pending pending = commands.peekFirst();
        if (pending == null) { running = false; return; }
        running = true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != owner || (pending.command() &&
                (pending.language() != Language.getInstance() || pending.config() != ClientConfig.current()))) {
            commands.removeFirst();
            io.github.kltyton.commandtranslation.client.screen.ClientScreenAccess.message(MinecraftText.translatable("command_translation.input.cancelled"));
            advance(owner);
            return;
        }
        if (!pending.command() || !pending.config().enabled || !pending.config().localizedInput) {
            try { pending.send().accept(pending.text()); }
            finally { commands.removeFirst(); advance(owner); }
            return;
        }
        var dispatcher = owner.getCommands();
        var source = owner.getSuggestionsProvider();
        java.util.concurrent.CompletableFuture.completedFuture(pending.text())
                .thenApply(command -> CommandClient.translate(dispatcher, command, 0, source))
                .thenCompose(fixed -> LocalizedInput.resolve(dispatcher, fixed, 0, source, dispatcher::getCompletionSuggestions, true,
                        () -> minecraft.getConnection() == owner && pending.language() == Language.getInstance() && pending.config() == ClientConfig.current()))
                .whenCompleteAsync((text, error) -> {
                    try {
                        if (error != null) {
                            CommandTranslation.LOGGER.error("Could not resolve localized command arguments", error);
                            io.github.kltyton.commandtranslation.client.screen.ClientScreenAccess.message(MinecraftText.translatable("command_translation.input.failed"));
                        } else if (minecraft.getConnection() != owner || pending.language() != Language.getInstance()
                                || pending.config() != ClientConfig.current()) {
                            io.github.kltyton.commandtranslation.client.screen.ClientScreenAccess.message(MinecraftText.translatable("command_translation.input.cancelled"));
                        } else { pending.send().accept(text.canonical()); }
                    } finally {
                        commands.removeFirst();
                        advance(owner);
                    }
                }, minecraft);
    }
}
