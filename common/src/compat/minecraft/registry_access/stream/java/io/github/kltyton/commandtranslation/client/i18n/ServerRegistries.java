package io.github.kltyton.commandtranslation.client.i18n;

import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;

public final class ServerRegistries {
    private ServerRegistries() {}

    public static Stream<Registry<?>> stream() {
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? Stream.empty() : connection.registryAccess().registries().map(entry -> entry.value());
    }
}
