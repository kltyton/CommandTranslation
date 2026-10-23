package io.github.kltyton.commandtranslation.client.i18n;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class IdentifierAccess {
    private IdentifierAccess() {}

    public static ResourceLocation tryParse(String raw) { return ResourceLocation.tryParse(raw); }

    public static ResourceLocation read(String raw) {
        try { return ResourceLocation.read(new StringReader(raw)); }
        catch (CommandSyntaxException invalidIdentifier) { return null; }
    }

    public static String registryId(ResourceKey<?> key) { return key.location().toString(); }
}
