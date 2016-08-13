package openmods.calc.parsing;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;

public class SimplePostfixCompilerState<E> implements IPostfixCompilerState<E> {

	final IExecutableListBuilder<E> builder;

	public SimplePostfixCompilerState(IExecutableListBuilder<E> builder) {
		this.builder = builder;
	}

	@Override
	public Result acceptToken(Token token) {
		if (token.type == TokenType.OPERATOR) builder.appendOperator(token.value);
		else if (token.type == TokenType.SYMBOL) builder.appendSymbolCall(token.value, SymbolCall.DEFAULT_ARG_COUNT, SymbolCall.DEFAULT_RET_COUNT);
		else if (token.type == TokenType.SYMBOL_WITH_ARGS) parseSymbolWithArgs(token.value, builder);
		else if (token.type.isValue()) builder.appendValue(token);
		else return Result.REJECTED;

		return Result.ACCEPTED;
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

		output.appendSymbolCall(id, argCount, retCount);
	}

	@Override
	public Result acceptExecutable(IExecutable<E> executable) {
		builder.appendExecutable(executable);
		return Result.ACCEPTED;
	}

	@Override
	public IExecutable<E> exit() {
		return builder.build();
	}

}
