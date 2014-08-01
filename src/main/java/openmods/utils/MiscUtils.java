package openmods.utils;

import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import openmods.Log;
import openmods.api.IValueReceiver;

public class MiscUtils {
	private static final String[] EMPTY = new String[] {};

	public static int getHoliday() {
		Calendar today = Calendar.getInstance();
		int month = today.get(2);
		int day = today.get(5);
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
		List<String> arraylist = new ArrayList<String>();
		Scanner scanner = null;
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
		return arraylist.toArray(new String[arraylist.size()]);
	}

	public static RuntimeException unhandledEnum(Enum<?> e) {
		throw new IllegalArgumentException(e.toString());
	}

	public static <T> IValueReceiver<T> createTextValueReceiver(final IValueReceiver<String> target) {
		return new IValueReceiver<T>() {
			@Override
			public void setValue(T value) {
				target.setValue(value != null? value.toString() : null);
			}
		};
	}
}
