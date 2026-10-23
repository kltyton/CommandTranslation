package io.github.kltyton.commandtranslation.client.command.help;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;

final class ArgumentParserRegistry {
    private ArgumentParserRegistry() {}

    static String id(ArgumentTypeInfo<?, ?> info) { return BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(info).toString(); }
}
