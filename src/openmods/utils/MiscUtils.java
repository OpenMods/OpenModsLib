package openmods.utils;

import java.util.Calendar;

public class MiscUtils {
	public static int getHoliday() {
		Calendar today = Calendar.getInstance();
		int month = today.get(2);
		int day = today.get(5);
		if ((month == 1) && (day == 14)) { return 1; }
		if ((month == 9) && (day == 31)) { return 2; }
		if ((month == 11) && (day >= 24) && (day <= 30)) { return 3; }
		return 0;
	}
}
