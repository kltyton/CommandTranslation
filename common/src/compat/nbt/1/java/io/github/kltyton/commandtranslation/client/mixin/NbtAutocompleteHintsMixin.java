package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import net.minecraft.network.chat.Component;
import com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Localizes NBT Autocomplete 1.x hints while preserving NBT syntax tokens. */
@Mixin(value = NbtSuggestionManager.class, remap = false)
public abstract class NbtAutocompleteHintsMixin {
    @ModifyReturnValue(method = "getSubtext", at = @At("RETURN"))
    private static String commandTranslation$localizeHint(String original) {
        if (original == null || !ClientConfig.current().enabled) { return original; }
        String key = switch (original) {
            case "(remove component)" -> "remove_component";
            case "[suggestions not loaded]" -> "loading";
            default -> null;
        };
        return key == null ? original : MinecraftText.translatable("command_translation.autocomplete.hint." + key).getString();
    }
}
