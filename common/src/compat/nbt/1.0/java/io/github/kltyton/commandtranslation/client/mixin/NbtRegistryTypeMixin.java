package io.github.kltyton.commandtranslation.client.mixin;

import com.mt1006.nbt_ac.autocomplete.suggestions.CustomSuggestion;
import com.mt1006.nbt_ac.autocomplete.suggestions.NbtSuggestion;
import io.github.kltyton.commandtranslation.client.integration.nbt.NbtRegistryTypes;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reads registry identity from the original 1.0 subtype fields. */
@Mixin(value = NbtSuggestion.class, remap = false)
public abstract class NbtRegistryTypeMixin {
    @Shadow public NbtSuggestion.Subtype subtype;
    @Shadow public String subtypeData;

    @Inject(method = "getSubtypeSuggestions", at = @At("RETURN"))
    private void commandTranslation$registry(List<CustomSuggestion> suggestions, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) { NbtRegistryTypes.attach(suggestions, NbtRegistryTypes.registry(subtype.name(), subtypeData)); }
    }
}
