package io.github.kltyton.commandtranslation.client.screen.configuration;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ConfigButtons {
    private ConfigButtons() {}
    static Button create(Screen owner, int x, int y, int width, int height, Component text, Component tooltip, Runnable action) {
        var builder = Button.builder(text, ignored -> action.run()).bounds(x, y, width, height);
        if (tooltip != null) { builder.tooltip(Tooltip.create(tooltip)); }
        return builder.build();
    }
}
