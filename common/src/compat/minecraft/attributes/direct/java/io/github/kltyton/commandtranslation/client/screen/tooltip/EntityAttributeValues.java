package io.github.kltyton.commandtranslation.client.screen.tooltip;

import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import java.util.List;
import io.github.kltyton.commandtranslation.client.i18n.CommandRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/** Reads only attributes actually present in the registered supplier. */
public final class EntityAttributeValues {
    private EntityAttributeValues() {}

    public static List<EntityAttributeTooltips.Value> read(EntityType<?> type) {
        if (!DefaultAttributes.hasSupplier(type)) { return List.of(); }
        var supplier = DefaultAttributes.getSupplier(living(type));
        return CommandRegistries.ATTRIBUTE.stream().filter(supplier::hasAttribute)
                .map(attribute -> new EntityAttributeTooltips.Value(CommandRegistries.ATTRIBUTE.getKey(attribute).toString(),
                        MinecraftText.translatable(attribute.getDescriptionId()), supplier.getValue(attribute))).toList();
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends LivingEntity> living(EntityType<?> type) {
        // hasSupplier restricts this cast to living-entity registrations.
        return (EntityType<? extends LivingEntity>) type;
    }
}
