package io.github.kltyton.commandtranslation;

import io.github.kltyton.commandtranslation.client.integration.neoforge.ConfigScreenRegistration;
import io.github.kltyton.commandtranslation.client.integration.neoforge.ClientCommandRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = BuildInfo.MOD_ID, dist = Dist.CLIENT)
public final class CommandTranslationNeoForge {
    public CommandTranslationNeoForge(ModContainer container) {
        CommandTranslation.initialize();
        ConfigScreenRegistration.register(container);
        ClientCommandRegistration.register();
    }
}
