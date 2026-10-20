package io.github.kltyton.commandtranslation.client.integration.neoforge;

import io.github.kltyton.commandtranslation.client.command.export.TranslationExportCommand;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientCommandRegistration {
    private ClientCommandRegistration() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> TranslationExportCommand.register(event.getDispatcher()));
    }
}
