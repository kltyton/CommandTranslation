package io.github.kltyton.commandtranslation.client.integration.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kltyton.commandtranslation.client.chat.ClientChatAccess;
import io.github.kltyton.commandtranslation.client.command.export.TranslationExportCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ComponentUtils;

/** Supplies the local export command before Forge added its client command event. */
public final class ClientCommandRegistration {
    private static final CommandDispatcher<SharedSuggestionProvider> COMMANDS = new CommandDispatcher<>();
    private ClientCommandRegistration() {}

    public static void register() { TranslationExportCommand.register(COMMANDS); }

    public static void attach(CommandDispatcher<SharedSuggestionProvider> serverCommands) {
        for (var root : COMMANDS.getRoot().getChildren()) { serverCommands.getRoot().addChild(root); }
    }

    public static boolean execute(ClientPacketListener connection, String message) {
        if (!message.startsWith("/")) { return false; }
        StringReader reader = new StringReader(message.substring(1));
        var root = COMMANDS.getRoot().getChild(reader.readUnquotedString());
        if (root == null || !root.canUse(connection.getSuggestionsProvider())) { return false; }
        try { COMMANDS.execute(message.substring(1), connection.getSuggestionsProvider()); }
        catch (CommandSyntaxException invalidCommand) {
            ClientChatAccess.show(ComponentUtils.fromMessage(invalidCommand.getRawMessage()).copy().withStyle(ChatFormatting.RED));
        }
        return true;
    }
}
