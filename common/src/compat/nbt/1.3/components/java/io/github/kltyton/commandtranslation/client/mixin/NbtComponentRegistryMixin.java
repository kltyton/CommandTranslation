package io.github.kltyton.commandtranslation.client.mixin;

import com.mt1006.nbt_ac.autocomplete.suggestions.ComponentSuggestion;
import io.github.kltyton.commandtranslation.client.command.suggestion.RegistrySuggestion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Binds component keys after either supported constructor has finished. */
@Mixin(value = ComponentSuggestion.class, remap = false)
public abstract class NbtComponentRegistryMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void commandTranslation$registry(CallbackInfo callback) {
        ((RegistrySuggestion) this).commandTranslation$registry("minecraft:data_component_type");
    }
}
