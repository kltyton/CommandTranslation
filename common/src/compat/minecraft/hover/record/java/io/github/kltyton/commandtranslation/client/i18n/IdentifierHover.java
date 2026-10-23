package io.github.kltyton.commandtranslation.client.i18n;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public final class IdentifierHover {
    private IdentifierHover() {}

    public static HoverEvent text(Component text) { return new HoverEvent.ShowText(text); }
}
