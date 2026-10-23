package io.github.kltyton.commandtranslation.client.screen.tooltip;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.world.item.ItemStack;

/** Reads preview item NBT with the native parser. */
public final class TooltipItemParser {
    private TooltipItemParser() {}

    public static ItemStack parse(String input) {
        try {
            var result = new ItemParser(new StringReader(input), false).parse();
            return new ItemInput(result.getItem(), result.getNbt()).createItemStack(1, false);
        } catch (CommandSyntaxException incompleteOrInvalid) {
            return null;
        }
    }
}
