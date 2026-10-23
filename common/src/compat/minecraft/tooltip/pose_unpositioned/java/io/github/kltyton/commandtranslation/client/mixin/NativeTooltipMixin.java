package io.github.kltyton.commandtranslation.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.commandtranslation.client.screen.tooltip.NativeTooltipAccess;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the pre-positioner native tooltip renderer. */
@Mixin(Screen.class)
public abstract class NativeTooltipMixin implements NativeTooltipAccess {
    @Invoker("renderTooltipInternal")
    public abstract void commandTranslation$renderComponents(PoseStack pose, List<ClientTooltipComponent> components, int x, int y);
}
