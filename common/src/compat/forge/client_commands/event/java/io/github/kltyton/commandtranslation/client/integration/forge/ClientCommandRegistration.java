package io.github.kltyton.commandtranslation.client.integration.forge;

import io.github.kltyton.commandtranslation.client.command.export.TranslationExportCommand;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;

public final class ClientCommandRegistration {
    private ClientCommandRegistration() {}
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> TranslationExportCommand.register(event.getDispatcher()));
    }
}
