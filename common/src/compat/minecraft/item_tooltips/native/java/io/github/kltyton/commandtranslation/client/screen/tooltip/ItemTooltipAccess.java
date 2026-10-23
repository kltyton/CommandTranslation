package io.github.kltyton.commandtranslation.client.screen.tooltip;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class ItemTooltipAccess {
    private ItemTooltipAccess() {}
    static List<Component> lines(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        return Screen.getTooltipFromItem(client, stack);
    }
}
