package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.command.suggestion.RegistrySuggestion;
import net.mt1006.nbtac.autocomplete.suggestions.DataComponentSuggestion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Identifies NBT Autocomplete 2.x component keys before display formatting. */
@Mixin(value = DataComponentSuggestion.class, remap = false)
public abstract class NbtComponentRegistryMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void commandTranslation$registry(CallbackInfo callback) {
        ((RegistrySuggestion) this).commandTranslation$registry("minecraft:data_component_type");
    }
}
