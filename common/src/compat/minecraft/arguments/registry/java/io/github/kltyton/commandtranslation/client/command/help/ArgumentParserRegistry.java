package io.github.kltyton.commandtranslation.client.command.help;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;

final class ArgumentParserRegistry {
    private ArgumentParserRegistry() {}

    static String id(ArgumentTypeInfo<?, ?> info) { return Registry.COMMAND_ARGUMENT_TYPE.getKey(info).toString(); }
}
