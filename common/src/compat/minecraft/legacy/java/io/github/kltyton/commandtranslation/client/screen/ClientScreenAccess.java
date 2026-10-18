package io.github.kltyton.commandtranslation.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ClientScreenAccess {
    private ClientScreenAccess() {}
    public static Screen current() { return Minecraft.getInstance().screen; }
    public static void open(Screen screen) { Minecraft.getInstance().setScreen(screen); }
    public static void message(net.minecraft.network.chat.Component message) { Minecraft.getInstance().getChatListener().handleSystemMessage(message, false); }
}
