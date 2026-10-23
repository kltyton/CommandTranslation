package io.github.kltyton.commandtranslation.client.screen;

import net.minecraft.client.gui.components.AbstractWidget;

public final class WidgetCoordinates {
    private WidgetCoordinates() {}
    public static int x(AbstractWidget widget) { return widget.getX(); }
    public static int y(AbstractWidget widget) { return widget.getY(); }
}
