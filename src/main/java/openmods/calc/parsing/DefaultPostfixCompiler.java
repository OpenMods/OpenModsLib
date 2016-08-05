package openmods.calc.parsing;

import openmods.calc.OperatorDictionary;

public class DefaultPostfixCompiler<E> extends PostfixCompiler<E> {
	private final IValueParser<E> valueParser;
	private final OperatorDictionary<E> operators;

	public DefaultPostfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		this.valueParser = valueParser;
		this.operators = operators;
	}

	@Override
	protected IExecutableListBuilder<E> createExecutableBuilder() {
		return new DefaultExecutableListBuilder<E>(valueParser, operators);
	}
}