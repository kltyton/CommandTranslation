package io.github.kltyton.commandtranslation.client.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the visible text origin for command-token hit testing. */
@Mixin(EditBox.class)
public interface EditBoxAccessor {
    @Accessor("displayPos") int commandTranslation$displayPos();
}
