package io.github.kltyton.commandtranslation.client.i18n;

import net.minecraft.client.Minecraft;

public final class ClientLocale {
    private ClientLocale() {}
    public static String selected() { return Minecraft.getInstance().getLanguageManager().getSelected(); }
}
