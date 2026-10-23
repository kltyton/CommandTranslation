package io.github.kltyton.commandtranslation;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import io.github.kltyton.commandtranslation.client.command.export.TranslationExportCommand;

public final class CommandTranslationFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CommandTranslation.initialize();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> TranslationExportCommand.register(dispatcher));
    }
}
