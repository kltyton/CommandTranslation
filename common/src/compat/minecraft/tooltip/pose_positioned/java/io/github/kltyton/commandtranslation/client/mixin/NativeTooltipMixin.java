package io.github.kltyton.commandtranslation.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.commandtranslation.client.screen.tooltip.NativeTooltipAccess;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Preserves the native positioner and Loader tooltip event chain. */
@Mixin(Screen.class)
public abstract class NativeTooltipMixin implements NativeTooltipAccess {
    @Invoker("renderTooltipInternal")
    protected abstract void commandTranslation$renderNative(PoseStack pose, List<ClientTooltipComponent> components,
            int x, int y, ClientTooltipPositioner positioner);

    @Override
    public void commandTranslation$renderComponents(PoseStack pose, List<ClientTooltipComponent> components, int x, int y) {
        commandTranslation$renderNative(pose, components, x, y, (screen, left, top, width, height) -> new Vector2i(left, top));
    }
}
