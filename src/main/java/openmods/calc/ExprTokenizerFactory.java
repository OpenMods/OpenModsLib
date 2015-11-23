package openmods.calc;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

public class ExprTokenizerFactory {

	private static final Token ZERO = new Token(TokenType.DEC_NUMBER, "0");

	private static final Pattern FLOAT_NUMBER_DOT = Pattern.compile("^(\\.[0-9]*(?:e[+-]?[0-9]+)?)");

	private static final Pattern FLOAT_NUMBER_FULL = Pattern.compile("^([0-9]+\\.[0-9]*(?:e[+-]?[0-9]+)?)");

	private static final Pattern FLOAT_NUMBER_EXPONENT = Pattern.compile("^([0-9]+e[+-]?[0-9]+)");

	private static final Pattern INT_NUMBER = Pattern.compile("^([1-9][0-9]*)");

	private static final Pattern HEX_NUMBER = Pattern.compile("^0x([0-9A-Fa-f]+)");

	private static final Pattern OCT_NUMBER = Pattern.compile("^0([0-7]+)");

	private static final Pattern BIN_NUMBER = Pattern.compile("^0b([01]+)");

	private static final Pattern QUOTED_NUMBER = Pattern.compile("^([0-9]+#[0-9A-Za-z'\"]+)");

	private static final Pattern SYMBOL = Pattern.compile("^([_A-Za-z$][_0-9A-Za-z$]*)");

	private static final Pattern CONSTANT = Pattern.compile("^([_A-Z][_0-9A-Z]*)");

	private final Set<String> operators = Sets.newTreeSet(new Comparator<String>() {

		@Override
		public int compare(String o1, String o2) {
			int sizes = Ints.compare(o2.length(), o1.length());
			if (sizes != 0) return sizes;

			return o1.compareTo(o2);
		}
	});

	private class TokenIterator extends AbstractIterator<Token> {

		private String input;

		public TokenIterator(String cls) {
			this.input = cls;
		}

		@Override
		protected Token computeNext() {
			try {
				if (input.isEmpty()) return endOfData();
				skipWhitespace();
				if (input.isEmpty()) return endOfData();

				if (input.startsWith("(")) return rawToken(1, TokenType.LEFT_BRACKET);
				if (input.startsWith(")")) return rawToken(1, TokenType.RIGHT_BRACKET);
				if (input.startsWith(",")) return rawToken(1, TokenType.SEPARATOR);

				final Matcher symbolMatcher = SYMBOL.matcher(input);

				if (symbolMatcher.find()) {
					final String operator = findOperator();

					final String symbol = symbolMatcher.group(1);
					if (operator != null && operator.length() >= symbol.length()) {
						consumeInput(operator.length());
						return new Token(TokenType.OPERATOR, operator);
					} else {

						consumeInput(symbolMatcher.end());
						return CONSTANT.matcher(symbol).matches()?
								new Token(TokenType.CONSTANT, symbol) :
								new Token(TokenType.SYMBOL, symbol);
					}
				}

				final String operator = findOperator();
				if (operator != null) {
					consumeInput(operator.length());
					return new Token(TokenType.OPERATOR, operator);
				}

				Token result;

				result = tryPattern(FLOAT_NUMBER_DOT, TokenType.FLOAT_NUMBER);
				if (result != null) return result;

				result = tryPattern(FLOAT_NUMBER_FULL, TokenType.FLOAT_NUMBER);
				if (result != null) return result;

				result = tryPattern(FLOAT_NUMBER_EXPONENT, TokenType.FLOAT_NUMBER);
				if (result != null) return result;

				result = tryPattern(QUOTED_NUMBER, TokenType.QUOTED_NUMBER);
				if (result != null) return result;

				result = tryPattern(INT_NUMBER, TokenType.DEC_NUMBER);
				if (result != null) return result;

				result = tryPattern(HEX_NUMBER, TokenType.HEX_NUMBER);
				if (result != null) return result;

				result = tryPattern(OCT_NUMBER, TokenType.OCT_NUMBER);
				if (result != null) return result;

				result = tryPattern(BIN_NUMBER, TokenType.BIN_NUMBER);
				if (result != null) return result;

				if (input.startsWith("0")) {
					input = input.substring(1);
					return ZERO;
				}

				throw new IllegalArgumentException("Unknown token type: '" + input + "'");

			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to parse: '" + input + "'", e);
			}
		}

		protected void consumeInput(final int length) {
			input = input.substring(length);
		}

		private Token rawToken(int charCount, TokenType type) {
			final String value = input.substring(0, charCount);
			consumeInput(charCount);
			return new Token(type, value);
		}

		private String tryPattern(Pattern pattern) {
			final Matcher matcher = pattern.matcher(input);
			if (matcher.find()) {
				final String matched = matcher.group(1);
				consumeInput(matcher.end());
				return matched;
			}

			return null;
		}

		private Token tryPattern(Pattern pattern, TokenType type) {
			final String matched = tryPattern(pattern);
			return matched != null? new Token(type, matched) : null;
		}

		private String findOperator() {
			for (String operator : operators)
				if (input.startsWith(operator)) return operator;

			return null;
		}

		private void skipWhitespace() {
			int i = 0;
			while (i < input.length() && Character.isWhitespace(input.charAt(i)))
				i++;

			if (i > 0) consumeInput(i);
		}

	}

	public class ExprTokenizer implements Iterable<Token> {
		private final String input;

		public ExprTokenizer(String input) {
			this.input = input;
		}

		@Override
		public Iterator<Token> iterator() {
			return new TokenIterator(input);

		}
	}

	public void addOperator(String operator) {
		operators.add(operator);
	}

	public Iterable<Token> tokenize(String input) {
		return new ExprTokenizer(input);
	}
}
