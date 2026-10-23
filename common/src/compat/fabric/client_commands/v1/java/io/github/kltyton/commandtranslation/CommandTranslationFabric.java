package io.github.kltyton.commandtranslation;

import io.github.kltyton.commandtranslation.client.command.export.TranslationExportCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;

public final class CommandTranslationFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CommandTranslation.initialize();
        TranslationExportCommand.register(ClientCommandManager.DISPATCHER);
    }
}
