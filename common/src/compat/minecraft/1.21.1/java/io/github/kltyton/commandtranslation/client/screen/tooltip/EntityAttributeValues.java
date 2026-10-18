package io.github.kltyton.commandtranslation.client.screen.tooltip;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
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
        return BuiltInRegistries.ATTRIBUTE.holders().filter(supplier::hasAttribute)
                .map(attribute -> new EntityAttributeTooltips.Value(BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()).toString(),
                        Component.translatable(attribute.value().getDescriptionId()), supplier.getValue(attribute))).toList();
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends LivingEntity> living(EntityType<?> type) {
        // hasSupplier restricts this cast to living-entity registrations.
        return (EntityType<? extends LivingEntity>) type;
    }
}

