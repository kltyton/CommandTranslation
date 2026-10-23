package io.github.kltyton.commandtranslation.client.screen.configuration;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;

final class ConfigCheckboxes {
    private ConfigCheckboxes() {}
    static Checkbox create(int x, int y, int width, Component label, boolean selected, Font font) {
        return Checkbox.builder(label, font).pos(x, y).maxWidth(width).selected(selected).build();
    }
}
