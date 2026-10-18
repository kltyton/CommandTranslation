package io.github.kltyton.commandtranslation.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ClientScreenAccess {
    private ClientScreenAccess() {}
    public static Screen current() { return Minecraft.getInstance().gui.screen(); }
    public static void open(Screen screen) { Minecraft.getInstance().gui.setScreen(screen); }
    public static void message(net.minecraft.network.chat.Component message) { Minecraft.getInstance().gui.chatListener().handleSystemMessage(message, false); }
}
