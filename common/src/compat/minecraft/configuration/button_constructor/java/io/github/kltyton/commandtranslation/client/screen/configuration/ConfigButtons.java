package io.github.kltyton.commandtranslation.client.screen.configuration;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ConfigButtons {
    private ConfigButtons() {}
    static Button create(Screen owner, int x, int y, int width, int height, Component text, Component tooltip, Runnable action) {
        if (tooltip == null) { return new Button(x, y, width, height, text, ignored -> action.run()); }
        return new Button(x, y, width, height, text, ignored -> action.run(),
                (button, pose, mouseX, mouseY) -> owner.renderTooltip(pose, tooltip, mouseX, mouseY));
    }
}
