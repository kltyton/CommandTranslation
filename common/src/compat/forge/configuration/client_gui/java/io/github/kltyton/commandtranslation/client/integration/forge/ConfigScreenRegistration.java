package io.github.kltyton.commandtranslation.client.integration.forge;

import io.github.kltyton.commandtranslation.client.screen.configuration.CommandConfigScreen;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.fml.ModLoadingContext;

public final class ConfigScreenRegistration {
    private ConfigScreenRegistration() {}
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((minecraft, parent) -> CommandConfigScreen.create(parent)));
    }
}
