package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.command.suggestion.RegistrySuggestion;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.mt1006.nbtac.autocomplete.SuggestionList;
import net.mt1006.nbtac.autocomplete.suggestions.IdSuggestion;
import net.mt1006.nbtac.autocomplete.type.Type;
import net.mt1006.nbtac.autocomplete.type.complex.RegistryKeyType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Associates NBT registry values with their declared registry rather than guessing from their IDs. */
@Mixin(value = RegistryKeyType.class, remap = false)
public abstract class NbtRegistryTypeMixin {
    @Shadow @Final private ResourceKey<Registry<Object>> registryKey;

    @WrapMethod(method = "getBasicSuggestions")
    private void commandTranslation$registry(Type.SuggestionListContext context, SuggestionList suggestions, Operation<Void> original) {
        int before = 0;
        for (var ignored : suggestions) { before++; }
        original.call(context, suggestions);
        if (registryKey == null) { return; }
        String registry = ClientPlatform.registryId(registryKey);
        int index = 0;
        for (var suggestion : suggestions) {
            if (index++ >= before && suggestion instanceof IdSuggestion) {
                ((RegistrySuggestion) suggestion).commandTranslation$registry(registry);
            }
        }
    }
}
