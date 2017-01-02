package openmods.calc;

public class PrinterUtils {

	public static String decorateBase(boolean allowCustom, int base, String value) {
		if (allowCustom) {
			switch (base) {
				case 2:
					return "0b" + value;
				case 8:
					return "0" + value;
				case 10:
					return value;
				case 16:
					return "0x" + value;
				default:
					return Integer.toString(base) + "#" + value;
			}
		} else {
			return Integer.toString(base) + "#" + value;
		}
	}

}
