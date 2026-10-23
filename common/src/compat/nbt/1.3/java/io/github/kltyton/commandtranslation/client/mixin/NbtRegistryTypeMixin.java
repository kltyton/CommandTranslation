package io.github.kltyton.commandtranslation.client.mixin;

import com.mt1006.nbt_ac.autocomplete.CustomTagParser;
import com.mt1006.nbt_ac.autocomplete.SuggestionList;
import com.mt1006.nbt_ac.autocomplete.suggestions.NbtSuggestion;
import com.mt1006.nbt_ac.autocomplete.suggestions.NbtSuggestionSubtype;
import io.github.kltyton.commandtranslation.client.integration.nbt.NbtRegistryTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves the registry declared by value subtypes and enchantment-keyed compounds. */
@Mixin(value = NbtSuggestionSubtype.class, remap = false)
public abstract class NbtRegistryTypeMixin {
    @Inject(method = "getSubtypeSuggestions", at = @At("RETURN"))
    private void commandTranslation$registry(NbtSuggestion suggestion, SuggestionList suggestions, String data,
                                             CustomTagParser.Type parser, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            NbtRegistryTypes.attach(suggestions, NbtRegistryTypes.registry(((NbtSuggestionSubtype) (Object) this).name(), data));
        }
    }

    @Inject(method = "getSubtypeTagSuggestions", at = @At("RETURN"))
    private void commandTranslation$registryKeys(SuggestionList suggestions, NbtSuggestion.ParentInfo parent,
                                                 String data, CustomTagParser.Type parser, CallbackInfo callback) {
        if (((NbtSuggestionSubtype) (Object) this).name().equals("ENCHANTMENTS")) {
            NbtRegistryTypes.attach(suggestions, "minecraft:enchantment");
        }
    }
}
