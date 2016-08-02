package openmods.calc;

import openmods.calc.parsing.IValueParser;

public interface ICalculatorFactory<E, M, C extends Calculator<E, M>> {
	public C create(E nullValue, IValueParser<E> valueParser, OperatorDictionary<E> operators);
}
