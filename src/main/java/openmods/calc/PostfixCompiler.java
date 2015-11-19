package openmods.calc;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public abstract class PostfixCompiler<E> implements ICompiler<E> {

	private final IValueParser<E> valueParser;

	private final OperatorDictionary<E> operators;

	public PostfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		this.valueParser = valueParser;
		this.operators = operators;
	}

	@Override
	public IExecutable<E> compile(Iterable<Token> input) {
		List<IExecutable<E>> result = Lists.newArrayList();
		for (Token token : input)
			result.add(compileToken(token));

		return new ExecutableList<E>(result);
	}

	private IExecutable<E> compileToken(Token token) {
		if (token.type == TokenType.OPERATOR) {
			final IExecutable<E> operator = operators.get(token.value);
			Preconditions.checkState(operator != null, "Invalid operator: " + token);
			return operator;
		}

		if (token.type.isSymbol) return DelayedSymbol.create(token.value);

		if (token.type.isValue) {
			try {
				final E value = valueParser.parseToken(token);
				return Constant.create(value);
			} catch (Throwable t) {
				throw new InvalidTokenException(token, t);
			}
		}

		throw new InvalidTokenException(token);
	}

}
