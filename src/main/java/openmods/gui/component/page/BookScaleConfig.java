package openmods.gui.component.page;

import net.minecraft.util.StatCollector;
import openmods.Log;

public class BookScaleConfig {

	private static float getValue(String name, float defaultValue) {
		final String value = StatCollector.translateToLocal(name);
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			Log.warn(e, "Failed to parse float value '%s'='%s', returning default %s", name, value, defaultValue);
			return defaultValue;
		}
	}

	private static int getValue(String name, int defaultValue) {
		final String value = StatCollector.translateToLocal(name);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			Log.warn(e, "Failed to parse integer value '%s'='%s', returning default %s", name, value, defaultValue);
			return defaultValue;
		}
	}

	public static float getPageNumberScale() {
		return getValue("openmodslib.locale.book.scale.pageNumber", 0.5f);
	}

	public static float getSectionTitleScale() {
		return getValue("openmodslib.locale.book.scale.sectionTitle", 2.0f);
	}

	public static float getPageTitleScale() {
		return getValue("openmodslib.locale.book.scale.pageTitle", 1.0f);
	}

	public static float getPageContentScale() {
		return getValue("openmodslib.locale.book.scale.pageContent", 0.5f);
	}

	public static int getTitlePageSeparator() {
		return getValue("openmodslib.locale.book.lineSpace.titledPage", 2);
	}

	public static int getRecipePageSeparator() {
		return getValue("openmodslib.locale.book.lineSpace.recipePage", 4);
	}
}
