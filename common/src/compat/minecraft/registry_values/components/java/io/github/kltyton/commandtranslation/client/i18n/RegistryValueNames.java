package io.github.kltyton.commandtranslation.client.i18n;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public final class RegistryValueNames {
    private RegistryValueNames() {}

    public static Component name(String kind, ResourceLocation id) {
        if (kind.equals("potion")) {
            return BuiltInRegistries.POTION.getHolder(id)
                    .map(value -> PotionContents.createItemStack(Items.POTION, value).getHoverName()).orElse(null);
        }
        if (kind.equals("enchantment")) {
            return BuiltInRegistries.ENCHANTMENT.getOptional(id)
                    .map(value -> MinecraftText.translatable(value.getDescriptionId())).orElse(null);
        }
        return null;
    }
}
