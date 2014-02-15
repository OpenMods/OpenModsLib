package openmods.utils;

import java.util.Random;

public class StringUtils {

	public static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static Random rnd = new Random();

	public static String randomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));
		return sb.toString();
	}

	public static String format(String str) {

		boolean bold = false;
		boolean italic = false;

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '*') {
				bold = !bold;
				builder.append(getFormatter(bold, italic));
			} else if (c == '`') {
				italic = !italic;
				builder.append(getFormatter(bold, italic));
			} else {
				builder.append(c);
			}
		}
		return builder.toString().replaceAll("\\\\n", "\n");
	}

	public static String getFormatter(boolean bold, boolean italic) {
		StringBuilder formatter = new StringBuilder();
		formatter.append("\u00a7r");
		if (bold) {
			formatter.append("\u00a7l");
		}
		if (italic) {
			formatter.append("\u00a7o");
		}
		return formatter.toString();
	}
}
