package openmods.calc.parsing;

import com.google.common.base.Preconditions;
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

	public static void checkIsValidBracketPair(String openingBracket, String closingBracket) {
		final String expectedClosingBracket = getClosingBracket(openingBracket);
		Preconditions.checkState(expectedClosingBracket != null, "Unknown bracket: %s", openingBracket);
		Preconditions.checkState(expectedClosingBracket.equals(closingBracket), "Unmatched brackets: %s and %s, expected %s", openingBracket, closingBracket, expectedClosingBracket);
	}

	// yeah, that's pretty non-standard for lisp-clones, but my tokenizer is too stupid to work otherwise
	public static final String MODIFIER_QUOTE = "#";
	public static final String SYMBOL_QUOTE = "quote";
	public static final String MODIFIER_CDR = "...";

	public static void setupTokenizerForQuoteNotation(Tokenizer tokenizerFactory) {
		tokenizerFactory.addModifier(MODIFIER_QUOTE);
		tokenizerFactory.addModifier(MODIFIER_CDR);
	}
}
