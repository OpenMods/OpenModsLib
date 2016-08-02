package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.List;
import openmods.calc.ExecutableList;
import openmods.calc.IExecutable;
import openmods.calc.OperatorDictionary;
import openmods.calc.SymbolReference;
import openmods.calc.Value;

public class PostfixCompiler<E> implements ITokenStreamCompiler<E> {

	private final IValueParser<E> valueParser;

	private final OperatorDictionary<E> operators;

	public PostfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		this.valueParser = valueParser;
		this.operators = operators;
	}

	@Override
	public IExecutable<E> compile(PeekingIterator<Token> input) {
		final List<IExecutable<E>> result = Lists.newArrayList();
		while (input.hasNext())
			result.add(compileToken(input.next()));

		return new ExecutableList<E>(result);
	}

	private IExecutable<E> compileToken(Token token) {
		final String value = token.value;
		if (token.type == TokenType.OPERATOR) {
			final IExecutable<E> operator = operators.getAnyOperator(value);
			Preconditions.checkArgument(operator != null, "Invalid operator: " + token);
			return operator;
		}

		if (token.type == TokenType.SYMBOL) return new SymbolReference<E>(value);

		if (token.type == TokenType.SYMBOL_WITH_ARGS) return parseSymbolWithArgs(value);

		if (token.type.isValue()) {
			try {
				final E parsedValue = valueParser.parseToken(token);
				return Value.create(parsedValue);
			} catch (Throwable t) {
				throw new InvalidTokenException(token, t);
			}
		}

		throw new InvalidTokenException(token);
	}

	private static <E> IExecutable<E> parseSymbolWithArgs(final String value) {
		final int argsStart = value.indexOf('@');
		Preconditions.checkArgument(argsStart >= 0, "No args in token '%s'", value);
		final String id = value.substring(0, argsStart);
		final SymbolReference<E> ref = new SymbolReference<E>(id);

		try {
			final String args = value.substring(argsStart + 1, value.length());
			final int argsSeparator = args.indexOf(',');

			if (argsSeparator >= 0) {
				{
					final String argCountStr = args.substring(0, argsSeparator);
					if (!argCountStr.isEmpty()) {
						final int argCount = Integer.parseInt(argCountStr);
						ref.setArgumentsCount(argCount);
					}
				}

				{
					final String retCountStr = args.substring(argsSeparator + 1, args.length());
					if (!retCountStr.isEmpty()) {
						final int retCount = Integer.parseInt(retCountStr);
						ref.setReturnsCount(retCount);
					}
				}
			} else {
				final int argCount = Integer.parseInt(args);
				ref.setArgumentsCount(argCount);

			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Can't parse args on token '" + value + "'", e);
		}

		return ref;
	}
}
