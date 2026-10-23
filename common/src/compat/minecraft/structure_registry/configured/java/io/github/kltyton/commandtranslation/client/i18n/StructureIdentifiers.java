package io.github.kltyton.commandtranslation.client.i18n;

import net.minecraft.core.Registry;

/** Minecraft 1.18 locates configured structure features through the server registry. */
public final class StructureIdentifiers {
    public static final String REGISTRY = Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY.location().toString();
    public static final boolean LITERAL_COMMANDS = false;
    public static final boolean STRUCTURE_ONLY_LOCATE = true;
    private StructureIdentifiers() {}
}
