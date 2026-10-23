package io.github.kltyton.commandtranslation.client.i18n;

import java.util.stream.Stream;
import net.minecraft.core.Registry;

public final class RegistryTagIds {
    private RegistryTagIds() {}

    public static Stream<String> ids(Registry<?> registry) { return registry.getTagNames().map(key -> "#" + key.location()); }
}
