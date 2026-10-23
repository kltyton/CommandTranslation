package io.github.kltyton.commandtranslation.client.screen.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;

/** The legacy public overloads cannot accept measured component lists; this keeps native tooltip events. */
public interface NativeTooltipAccess {
    void commandTranslation$renderComponents(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner);
}
