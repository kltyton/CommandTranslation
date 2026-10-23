package io.github.kltyton.commandtranslation.client.i18n;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class IdentifierAccess {
    private IdentifierAccess() {}

    public static Identifier tryParse(String raw) { return Identifier.tryParse(raw); }

    public static Identifier read(String raw) {
        try { return Identifier.read(new StringReader(raw)); }
        catch (CommandSyntaxException invalidIdentifier) { return null; }
    }

    public static String registryId(ResourceKey<?> key) { return key.identifier().toString(); }
}
