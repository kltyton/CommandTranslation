package io.github.kltyton.commandtranslation.client.screen.configuration;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

final class CommandConfigScreenView extends CommandConfigScreen {
    CommandConfigScreenView(Screen parent, ClientConfig draft, Page page) { super(parent, draft, page); }

    @Override protected void addControl(AbstractWidget widget) { addRenderableWidget(widget); }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        paintSwatches((x0, y0, x1, y1, color) -> GuiComponent.fill(pose, x0, y0, x1, y1, color));
        paintLabels((text, x, y) -> font.draw(pose, text, x, y, 0xFFFFFFFF));
    }
}
