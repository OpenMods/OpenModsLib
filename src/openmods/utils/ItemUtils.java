package openmods.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import openmods.Log;
import openmods.interfaces.RegisterItem;

import com.google.common.base.Throwables;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;

public class ItemUtils {
	public static ItemStack consumeItem(ItemStack stack) {
		if (stack.stackSize == 1) {
			if (stack.getItem().hasContainerItem()) { return stack.getItem().getContainerItemStack(stack); }
			return null;
		}
		stack.splitStack(1);

		return stack;
	}

	public static NBTTagCompound getItemTag(ItemStack stack) {
		if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound("tag");

		return stack.stackTagCompound;
	}

	public static Integer getInt(ItemStack stack, String tagName) {
		NBTTagCompound tag = getItemTag(stack);
		NBTBase data = tag.getTag(tagName);
		return (data != null)? ((NBTTagInt)data).data : null;
	}
	

	public static void registerItems(Class<?> klazz, String mod) {
		for (Field f : klazz.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) && Item.class.isAssignableFrom(f.getType())) {
				RegisterItem annotation = f.getAnnotation(RegisterItem.class);
				if (annotation != null) {
					try {
						Item item = (Item)f.get(null);
						if (item != null) {
							String name = String.format("%s.%s", mod, annotation.name());
							GameRegistry.registerItem(item, name);
						}
					} catch (Exception e) {
						throw Throwables.propagate(e);
					}
				} else {
					Log.warn("Field %s has valid type for registration, but no annotation", f);
				}
			}
		}
	}
}
