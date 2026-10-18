package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Loads client configuration once during game initialization. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void commandTranslation$configure(CallbackInfo callback) {
        try {
            ClientConfig.load(Minecraft.getInstance().gameDirectory.toPath().resolve("config"));
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot load command_translation.json", failure);
        }
    }
}
