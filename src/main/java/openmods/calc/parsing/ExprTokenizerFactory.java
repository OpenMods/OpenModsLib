package openmods.calc.parsing;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;

public class ExprTokenizerFactory {

	private static final Pattern DEC_NUMBER = Pattern.compile("^([0-9](?:_*[0-9]+)*(?:\\.[0-9](?:_*[0-9]+)*)?)");

	private static final Pattern HEX_NUMBER = Pattern.compile("^0x([0-9A-Fa-f](?:_*[0-9A-Fa-f]+)*(?:\\.[0-9A-Fa-f](?:_*[0-9A-Fa-f]+)*)?)");

	private static final Pattern OCT_NUMBER = Pattern.compile("^0((?:_*[0-7]+)+(?:\\.[0-7](?:_*[0-7]+)*)?)");

	private static final Pattern BIN_NUMBER = Pattern.compile("^0b([01](?:_*[01]+)*(?:\\.[01](?:_*[01]+)*)?)");

	private static final Pattern QUOTED_NUMBER = Pattern.compile("^([0-9]+#[0-9A-Za-z'\"](?:_*[0-9A-Za-z'\"]+)*(?:\\.[0-9A-Za-z'\"](?:_*[0-9A-Za-z'\"]+)*)?)");

	private static final Pattern SYMBOL = Pattern.compile("^([_A-Za-z$][_0-9A-Za-z$]*)");

	private static final Pattern SYMBOL_ARGS = Pattern.compile("^(@[0-9]*,?[0-9]*)");

	private static final Set<String> STRING_STARTERS = ImmutableSet.of("\"", "'");

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

				{
					final String nextCh = input.substring(0, 1);
					if (STRING_STARTERS.contains(nextCh)) return consumeStringLiteral();
					if (TokenUtils.isOpeningBracket(nextCh)) return rawToken(1, TokenType.LEFT_BRACKET);
					if (TokenUtils.isClosingBracket(nextCh)) return rawToken(1, TokenType.RIGHT_BRACKET);
					if (nextCh.equals(",")) return rawToken(1, TokenType.SEPARATOR);
				}

				final Matcher symbolMatcher = SYMBOL.matcher(input);

				if (symbolMatcher.find()) {
					final String operator = findOperator();

					final String symbol = symbolMatcher.group(1);
					if (operator != null && operator.length() >= symbol.length()) {
						discardInput(operator.length());
						return new Token(TokenType.OPERATOR, operator);
					} else {
						discardInput(symbolMatcher.end());

						final Matcher argMatcher = SYMBOL_ARGS.matcher(input);

						if (argMatcher.find()) {
							discardInput(argMatcher.end());
							final String symbolArgs = argMatcher.group(1);
							return new Token(TokenType.SYMBOL_WITH_ARGS, symbol + symbolArgs);
						} else {
							return new Token(TokenType.SYMBOL, symbol);
						}
					}
				}

				final String operator = findOperator();
				if (operator != null) {
					discardInput(operator.length());
					return new Token(TokenType.OPERATOR, operator);
				}

				Token result;

				result = tryPattern(QUOTED_NUMBER, TokenType.QUOTED_NUMBER);
				if (result != null) return result;

				result = tryPattern(HEX_NUMBER, TokenType.HEX_NUMBER);
				if (result != null) return result;

				result = tryPattern(OCT_NUMBER, TokenType.OCT_NUMBER);
				if (result != null) return result;

				result = tryPattern(BIN_NUMBER, TokenType.BIN_NUMBER);
				if (result != null) return result;

				result = tryPattern(DEC_NUMBER, TokenType.DEC_NUMBER);
				if (result != null) return result;

				throw new IllegalArgumentException("Unknown token type: '" + input + "'");

			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to parse: '" + input + "'", e);
			}
		}

		private Token consumeStringLiteral() {
			final Pair<String, Integer> result = StringEscaper.unescapeDelimitedString(input, 0);
			discardInput(result.getRight());
			return new Token(TokenType.STRING, result.getLeft());
		}

		private void discardInput(int length) {
			input = input.substring(length);
		}

		private Token rawToken(int charCount, TokenType type) {
			final String value = input.substring(0, charCount);
			discardInput(charCount);
			return new Token(type, value);
		}

		private String tryPattern(Pattern pattern) {
			final Matcher matcher = pattern.matcher(input);
			if (matcher.find()) {
				final String matched = matcher.group(1);
				discardInput(matcher.end());
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

			if (i > 0) discardInput(i);
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
