package io.github.kltyton.commandtranslation.client.i18n;

import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagCollection;

public final class RegistryTagIds {
    private RegistryTagIds() {}

    public static Stream<String> ids(Registry<?> registry) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) { return Stream.empty(); }
        var tags = connection.getTags();
        TagCollection<?> collection;
        if (registry == Registry.ITEM) { collection = tags.getItems(); }
        else if (registry == Registry.BLOCK) { collection = tags.getBlocks(); }
        else if (registry == Registry.FLUID) { collection = tags.getFluids(); }
        else if (registry == Registry.ENTITY_TYPE) { collection = tags.getEntityTypes(); }
        else { return Stream.empty(); }
        return collection.getAvailableTags().stream().map(id -> "#" + id);
    }
}
