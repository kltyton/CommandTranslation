package io.github.kltyton.commandtranslation.client.integration.neoforge;

import io.github.kltyton.commandtranslation.client.screen.configuration.CommandConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ConfigScreenRegistration {
    private ConfigScreenRegistration() {}
    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (owner, parent) -> CommandConfigScreen.create(parent));
    }
}
