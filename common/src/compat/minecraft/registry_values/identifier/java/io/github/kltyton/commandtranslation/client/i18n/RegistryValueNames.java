package io.github.kltyton.commandtranslation.client.i18n;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;

public final class RegistryValueNames {
    private RegistryValueNames() {}

    public static Component name(String kind, Identifier id) {
        if (kind.equals("potion")) {
            return BuiltInRegistries.POTION.get(id)
                    .map(value -> PotionContents.createItemStack(Items.POTION, value).getHoverName()).orElse(null);
        }
        if (kind.equals("enchantment")) {
            var registry = ClientPlatform.registry("minecraft:enchantment");
            return registry == null ? null : registry.getOptional(id)
                    .map(value -> value instanceof Enchantment enchantment ? enchantment.description() : null).orElse(null);
        }
        return null;
    }
}
