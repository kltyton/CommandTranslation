package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.command.suggestion.RegistrySuggestion;
import net.mt1006.nbtac.autocomplete.suggestions.DataComponentSuggestion;
import net.mt1006.nbtac.autocomplete.tag.NbtTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Identifies component keys before their visible representation is formatted. */
@Mixin(value = DataComponentSuggestion.class, remap = false)
public abstract class NbtComponentRegistryMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void commandTranslation$registry(NbtTag tag, boolean equalSign, CallbackInfo callback) {
        ((RegistrySuggestion) this).commandTranslation$registry("minecraft:data_component_type");
    }
}
