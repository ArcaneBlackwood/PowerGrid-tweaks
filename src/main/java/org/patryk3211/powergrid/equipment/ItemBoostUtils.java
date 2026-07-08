package org.patryk3211.powergrid.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.utility.Lang;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class ItemBoostUtils {
    public static boolean isBoosted(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag()
            .getInt("Boosted") > 0;
    }


    public static void setBoosted(ItemStack stack, boolean boosted) {
        stack.update(
            DataComponents.CUSTOM_DATA,
            CustomData.EMPTY,
            data -> {
                CompoundTag tag = data.copyTag();
                if (boosted)
                    tag.putInt("Boosted", (int)(stack.getMaxDamage() * 0.3f));
                else
                    tag.remove("Boosted");
                return CustomData.of(tag);
            }
        );
    }

    public static void addTooltip(ItemStack stack, List<Component> tooltip) {
        if(isBoosted(stack)) {
            Lang.translate("tooltip.boosted")
                    .style(ChatFormatting.BLUE).style(ChatFormatting.ITALIC)
                    .addTo(tooltip);
        }
    }

    public static void damageBoost(ItemStack stack, Runnable breakCallback) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();

        if (!tag.contains("Boosted"))
            return;

        int dmg = tag.getInt("Boosted") - 1;

        if (dmg <= 0) {
            tag.remove("Boosted");
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            if (dmg == 0)
                breakCallback.run();

            return;
        }

        tag.putInt("Boosted", dmg);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean useBoost(ItemStack stack, LivingEntity entity) {
        if(!isBoosted(stack))
            return false;
        ItemBoostUtils.damageBoost(stack, () -> stack.hurtAndBreak(1000000, entity, EquipmentSlot.MAINHAND));
        return true;
    }
}
