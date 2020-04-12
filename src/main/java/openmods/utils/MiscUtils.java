package openmods.utils;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import net.minecraft.client.resources.I18n;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fluids.FluidStack;
import openmods.Log;
import openmods.api.IValueReceiver;

public class MiscUtils {
	private static final String[] EMPTY = new String[] {};
	public static final ITextComponent UNKNOWN_FLUID = new StringTextComponent("LOLNOPE").mergeStyle(TextFormatting.OBFUSCATED);

	public static int getHoliday() {
		Calendar today = Calendar.getInstance();
		int month = today.get(Calendar.MONTH);
		int day = today.get(Calendar.DAY_OF_MONTH);
		if ((month == 1) && (day == 14)) { return 1; }
		if ((month == 9) && (day == 31)) { return 2; }
		if ((month == 11) && (day >= 24) && (day <= 30)) { return 3; }
		return 0;
	}

	public static String[] loadTextFromURL(URL url) {
		return loadTextFromURL(url, EMPTY, 0);
	}

	public static String[] loadTextFromURL(URL url, int timeoutMS) {
		return loadTextFromURL(url, EMPTY, timeoutMS);
	}

	public static String[] loadTextFromURL(URL url, String defaultValue) {
		return loadTextFromURL(url, new String[] { defaultValue }, 0);
	}

	public static String[] loadTextFromURL(URL url, String defaultValue, int timeoutMS) {
		return loadTextFromURL(url, new String[] { defaultValue }, timeoutMS);
	}

	public static String[] loadTextFromURL(URL url, String[] defaultValue) {
		return loadTextFromURL(url, defaultValue, 0);
	}

	public static String[] loadTextFromURL(URL url, String[] defaultValue, int timeoutMS) {
		List<String> arraylist = new ArrayList<>();
		Scanner scanner;
		try {
			URLConnection uc = url.openConnection();
			uc.setReadTimeout(timeoutMS);
			uc.setConnectTimeout(timeoutMS);
			scanner = new Scanner(uc.getInputStream(), "UTF-8");
		} catch (Throwable e) {
			Log.warn(e, "Error retrieving remote string value! Defaulting to %s", Arrays.toString(defaultValue));
			return defaultValue;
		}

		while (scanner.hasNextLine()) {
			arraylist.add(scanner.nextLine());
		}
		scanner.close();
		return arraylist.toArray(new String[0]);
	}

	public static RuntimeException unhandledEnum(Enum<?> e) {
		throw new IllegalArgumentException(e.toString());
	}

	public static <T> IValueReceiver<T> createTextValueReceiver(final IValueReceiver<String> target) {
		return value -> target.setValue(value != null? value.toString() : null);
	}

	public static ITextComponent getTranslatedFluidName(FluidStack fluidStack) {
		final Fluid fluid = fluidStack.getFluid();
		String localizedName = fluid.getAttributes().getTranslationKey(fluidStack);
		if (I18n.hasKey(localizedName)) {
			return new TranslationTextComponent(localizedName).mergeStyle(fluid.getAttributes().getRarity(fluidStack).color);
		}
		return UNKNOWN_FLUID;
	}
}
