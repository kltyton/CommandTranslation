package io.github.kltyton.commandtranslation.client.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientChatAccess {
    private ClientChatAccess() {}
    public static void show(Component message) { Minecraft.getInstance().gui.getChat().addClientSystemMessage(message); }
}
