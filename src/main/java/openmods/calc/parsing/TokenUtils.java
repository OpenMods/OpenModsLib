package openmods.calc.parsing;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class TokenUtils {
	private static final BiMap<String, String> BRACKETS = ImmutableBiMap.of("(", ")", "{", "}", "[", "]");

	public static boolean isOpeningBracket(String bracket) {
		return BRACKETS.containsKey(bracket);
	}

	public static boolean isClosingBracket(String bracket) {
		return BRACKETS.containsValue(bracket);
	}

	public static String getClosingBracket(String bracket) {
		return BRACKETS.get(bracket);
	}
}
