package io.github.kltyton.commandtranslation.client.screen.tooltip;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.world.item.ItemStack;

/** Uses the registry-level lookup available before HolderLookup.Provider. */
public final class TooltipItemParser {
    private TooltipItemParser() {}

    public static ItemStack parse(String input) {
        try {
            var result = ItemParser.parseForItem(HolderLookup.forRegistry(Registry.ITEM), new StringReader(input));
            return new ItemInput(result.item(), result.nbt()).createItemStack(1, false);
        } catch (CommandSyntaxException incompleteOrInvalid) {
            return null;
        }
    }
}
