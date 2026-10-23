package io.github.kltyton.commandtranslation.client.screen.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/** Invokes the native renderer, including the Loader's tooltip events. */
public interface NativeTooltipAccess {
    void commandTranslation$renderComponents(PoseStack pose, List<ClientTooltipComponent> components, int x, int y);
}
