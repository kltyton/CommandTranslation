package io.github.kltyton.commandtranslation.client.integration.forge;

import io.github.kltyton.commandtranslation.client.screen.configuration.CommandConfigScreen;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

public final class ConfigScreenRegistration {
    private ConfigScreenRegistration() {}

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (minecraft, parent) -> CommandConfigScreen.create(parent));
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> "client-only", (remote, network) -> network));
    }
}
