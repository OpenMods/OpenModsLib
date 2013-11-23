package openmods.utils;

import java.lang.reflect.Field;

import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import openmods.interfaces.BlockId;
import openmods.interfaces.ItemId;

import com.google.common.base.Throwables;

public class ConfigUtils {

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
}
