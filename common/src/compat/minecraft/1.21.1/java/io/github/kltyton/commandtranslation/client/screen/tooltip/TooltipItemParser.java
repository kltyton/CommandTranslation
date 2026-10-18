package io.github.kltyton.commandtranslation.client.screen.tooltip;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.world.item.ItemStack;

/** Parses only a preview stack; incomplete command input has no valid stack yet. */
public final class TooltipItemParser {
    private TooltipItemParser() {}

    public static ItemStack parse(String input) {
        StringReader reader = new StringReader(input);
        try {
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null) { return null; }
            var result = new ItemParser(connection.registryAccess()).parse(reader);
            return new ItemInput(result.item(), result.components()).createItemStack(1, false);
        } catch (CommandSyntaxException incompleteOrInvalid) {
            return null;
        }
    }
}

