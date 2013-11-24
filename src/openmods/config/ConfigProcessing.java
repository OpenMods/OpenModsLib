package openmods.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import openmods.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import cpw.mods.fml.common.registry.GameRegistry;

public class ConfigProcessing {

	private static void getBlock(Configuration configFile, Field field, String description) {
		try {
			int defaultValue = field.getInt(null);
			Property prop = configFile.getBlock("block", field.getName(), defaultValue, description);
			field.set(null, prop.getInt());
		} catch (Throwable e) {
			throw Throwables.propagate(e);
		}
	}

	private static void getItem(Configuration configFile, Field field, String description) {
		try {
			int defaultValue = field.getInt(null);
			Property prop = configFile.getItem("item", field.getName(), defaultValue, description);
			field.set(null, prop.getInt());
		} catch (Throwable e) {
			throw Throwables.propagate(e);
		}
	}

	public static boolean canRegisterBlock(int blockId) {
		if (blockId > 0) {
			Preconditions.checkState(Block.blocksList[blockId] == null,
					"OpenBlocks tried to register a block for ID: %s but it was in use", blockId);
			return true;
		}
		return false; // Block disabled, fail silently
	}

	public static void processAnnotations(Configuration configFile, Class<?> klazz) {
		for (Field f : klazz.getFields()) {
			{
				ItemId a = f.getAnnotation(ItemId.class);
				if (a != null) {
					getItem(configFile, f, a.description());
					continue;
				}
			}

			{
				BlockId a = f.getAnnotation(BlockId.class);
				if (a != null) {
					getBlock(configFile, f, a.description());
				}
			}
		}
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
