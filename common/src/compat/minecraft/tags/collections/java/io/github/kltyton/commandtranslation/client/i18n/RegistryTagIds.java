package io.github.kltyton.commandtranslation.client.i18n;

import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;

public final class RegistryTagIds {
    private RegistryTagIds() {}

    public static Stream<String> ids(Registry<?> registry) {
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? Stream.empty() : connection.getTags().getOrEmpty(registry.key())
                .getAvailableTags().stream().map(id -> "#" + id);
    }
}
