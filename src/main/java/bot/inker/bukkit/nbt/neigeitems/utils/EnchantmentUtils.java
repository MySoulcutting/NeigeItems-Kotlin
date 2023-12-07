package bot.inker.bukkit.nbt.neigeitems.utils;

import pers.neige.neigeitems.ref.enchantment.RefBukkitEnchantment;
import pers.neige.neigeitems.ref.enchantment.RefEnchantment;
import pers.neige.neigeitems.ref.enchantment.RefEnchantments;
import pers.neige.neigeitems.ref.registry.RefBuiltInRegistries;
import pers.neige.neigeitems.ref.registry.RefMappedRegistry;
import pers.neige.neigeitems.ref.registry.RefReference;

public class EnchantmentUtils {
    public static void registerEnchantment(String name, RefEnchantment enchantment) {
        RefMappedRegistry<RefEnchantment> ENCHANTMENT = (RefMappedRegistry<RefEnchantment>) RefBuiltInRegistries.ENCHANTMENT;
        ENCHANTMENT.frozen = false;
        RefBukkitEnchantment.acceptingNew = true;
        try {
            RefEnchantments.register(name, enchantment);
            RefReference<RefEnchantment> reference = ENCHANTMENT.byValue.get(enchantment);
            if (reference != null) {
                reference.bindValue(enchantment);
            }
        } catch (Throwable error) {
            error.printStackTrace();
        }
        ENCHANTMENT.frozen = true;
        RefBukkitEnchantment.acceptingNew = false;
    }

    public static Class<?> getNmsEnchantmentClass() {
        return RefEnchantment.class;
    }
}
