package io.github.kltyton.commandtranslation.client.i18n;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;

/** Uses the registered object's native naming path, including mod-supplied translation keys. */
public final class RegistryValueNames {
    private RegistryValueNames() {}

    public static Component name(String kind, ResourceLocation id) {
        return switch (kind) {
            case "potion" -> BuiltInRegistries.POTION.getOptional(id)
                    .map(value -> PotionUtils.setPotion(new ItemStack(Items.POTION), value).getHoverName()).orElse(null);
            case "enchantment" -> BuiltInRegistries.ENCHANTMENT.getOptional(id)
                    .map(value -> Component.translatable(value.getDescriptionId())).orElse(null);
            default -> null;
        };
    }
}
