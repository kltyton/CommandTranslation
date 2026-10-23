package io.github.kltyton.commandtranslation.client.screen;

import net.minecraft.client.gui.components.AbstractWidget;

public final class WidgetCoordinates {
    private WidgetCoordinates() {}
    public static int x(AbstractWidget widget) { return widget.x; }
    public static int y(AbstractWidget widget) { return widget.y; }
}
