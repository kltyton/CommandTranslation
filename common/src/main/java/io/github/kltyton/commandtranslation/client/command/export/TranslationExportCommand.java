package io.github.kltyton.commandtranslation.client.command.export;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.kltyton.commandtranslation.CommandTranslation;
import io.github.kltyton.commandtranslation.client.chat.ClientChatAccess;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.i18n.ExportPackMetadata;
import io.github.kltyton.commandtranslation.client.i18n.export.TranslationPackWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class TranslationExportCommand {
    private static boolean exporting;
    private TranslationExportCommand() {}

    public static <S> void register(CommandDispatcher<S> dispatcher) {
        var export = LiteralArgumentBuilder.<S>literal("export").executes(context -> export(""))
                .then(RequiredArgumentBuilder.<S, String>argument("namespace", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String namespace : Minecraft.getInstance().getResourceManager().getNamespaces()) {
                                if (!namespace.equals("minecraft") && namespace.startsWith(builder.getRemaining())) { builder.suggest(namespace); }
                            }
                            return builder.buildFuture();
                        }).executes(context -> export(StringArgumentType.getString(context, "namespace"))));
        dispatcher.register(LiteralArgumentBuilder.<S>literal("command_translation")
                .requires(source -> ClientConfig.current().enabled && ClientConfig.current().translationExport).then(export));
    }

    private static int export(String namespace) {
        Minecraft minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        if (connection == null) { message(Component.translatable("command_translation.export.no_world")); return 0; }
        if (exporting) { message(Component.translatable("command_translation.export.busy")); return 0; }
        var commands = CommandExportCatalog.capture(connection.getCommands(), connection.getSuggestionsProvider());
        var resources = minecraft.getResourceManager();
        String locale = minecraft.getLanguageManager().getSelected();
        var metadata = ExportPackMetadata.create(Component.translatable("command_translation.export.pack_description").getString());
        String guide = Component.translatable("command_translation.export.guide", locale).getString();
        var directory = minecraft.gameDirectory.toPath().resolve("resourcepacks");
        exporting = true;
        message(Component.translatable("command_translation.export.started"));
        CompletableFuture.supplyAsync(() -> {
            try { return TranslationPackWriter.write(resources, commands, locale, namespace, directory, metadata, guide); }
            catch (IOException failure) { throw new UncheckedIOException(failure); }
        }).whenCompleteAsync((result, failure) -> {
            exporting = false;
            if (failure != null) {
                CommandTranslation.LOGGER.error("Could not export command translations for {}", locale, failure);
                message(Component.translatable("command_translation.export.failed").withStyle(ChatFormatting.RED));
                return;
            }
            if (result.file().isEmpty()) { message(Component.translatable("command_translation.export.nothing")); return; }
            message(Component.translatable("command_translation.export.success", result.file().get().toString(),
                    result.commands(), result.keys(), result.missingDescriptions()));
            if (result.unknownNamespaces() > 0) {
                message(Component.translatable("command_translation.export.unknown_namespace", result.unknownNamespaces()));
            }
        }, minecraft);
        return 1;
    }

    private static void message(Component text) { ClientChatAccess.show(text); }
}
