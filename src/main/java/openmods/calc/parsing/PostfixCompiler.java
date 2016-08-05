package openmods.calc.parsing;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.PeekingIterator;
import openmods.calc.IExecutable;

public abstract class PostfixCompiler<E> implements ITokenStreamCompiler<E> {

	protected abstract IExecutableListBuilder<E> createExecutableBuilder();

	@Override
	public IExecutable<E> compile(PeekingIterator<Token> input) {
		final IExecutableListBuilder<E> builder = createExecutableBuilder();

		while (input.hasNext()) {
			final Token token = input.next();
			if (token.type == TokenType.OPERATOR) builder.appendOperator(token.value);
			else if (token.type == TokenType.SYMBOL) builder.appendSymbol(token.value);
			else if (token.type == TokenType.SYMBOL_WITH_ARGS) parseSymbolWithArgs(token.value, builder);
			else if (token.type == TokenType.MODIFIER) parseModifier(token.value, input, builder); // TODO
			else if (token.type.isValue()) builder.appendValue(token);
		}

		return builder.build();
	}

	protected void parseModifier(String modifier, PeekingIterator<Token> input, IExecutableListBuilder<E> output) {
		throw new UnsupportedOperationException(modifier);
	}

	private static <E> void parseSymbolWithArgs(String value, IExecutableListBuilder<E> output) {
		final int argsStart = value.indexOf('@');
		Preconditions.checkArgument(argsStart >= 0, "No args in token '%s'", value);
		final String id = value.substring(0, argsStart);
		Optional<Integer> argCount = Optional.absent();
		Optional<Integer> retCount = Optional.absent();

		try {
			final String args = value.substring(argsStart + 1, value.length());
			final int argsSeparator = args.indexOf(',');

			if (argsSeparator >= 0) {
				{
					final String argCountStr = args.substring(0, argsSeparator);
					if (!argCountStr.isEmpty())
						argCount = Optional.of(Integer.parseInt(argCountStr));
				}

				{
					final String retCountStr = args.substring(argsSeparator + 1, args.length());
					if (!retCountStr.isEmpty())
						retCount = Optional.of(Integer.parseInt(retCountStr));
				}
			} else {
				argCount = Optional.of(Integer.parseInt(args));

			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Can't parse args on token '" + value + "'", e);
		}

		output.appendSymbol(id, argCount, retCount);
	}
}
