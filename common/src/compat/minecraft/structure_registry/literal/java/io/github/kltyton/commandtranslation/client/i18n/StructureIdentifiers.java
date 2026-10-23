package io.github.kltyton.commandtranslation.client.i18n;

import net.minecraft.core.Registry;

/** Before 1.18, locate exposes registered structure types as literal command children. */
public final class StructureIdentifiers {
    public static final String REGISTRY = Registry.STRUCTURE_FEATURE_REGISTRY.location().toString();
    public static final boolean LITERAL_COMMANDS = true;
    public static final boolean STRUCTURE_ONLY_LOCATE = true;
    private StructureIdentifiers() {}
}
