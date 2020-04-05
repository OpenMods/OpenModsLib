package openmods.utils;

import net.minecraft.client.resources.I18n;

@SuppressWarnings("deprecation") // TODO find better solution
public class TranslationUtils {

	public static String translateToLocal(String key) {
		return I18n.format(key);
	}

	public static String translateToLocalFormatted(String key, Object... args) {
		return I18n.format(key, args);
	}

}
