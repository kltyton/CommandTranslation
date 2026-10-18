package io.github.kltyton.commandtranslation.client.integration.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.kltyton.commandtranslation.client.screen.configuration.CommandConfigScreen;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() { return CommandConfigScreen::create; }
}
