package io.github.kltyton.commandtranslation;

import net.fabricmc.api.ClientModInitializer;

public final class CommandTranslationFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CommandTranslation.initialize();
    }
}
