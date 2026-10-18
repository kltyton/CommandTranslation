package io.github.kltyton.commandtranslation;

import io.github.kltyton.commandtranslation.client.integration.forge.ConfigScreenRegistration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(BuildInfo.MOD_ID)
public final class CommandTranslationForge {
    public CommandTranslationForge() {
        CommandTranslation.initialize();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ConfigScreenRegistration::register);
    }
}
