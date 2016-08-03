package openmods.calc;

import openmods.calc.parsing.IValueParser;

public interface ICompilerMapFactory<E, M> {
	public Compilers<E, M> create(E nullValue, IValueParser<E> valueParser, OperatorDictionary<E> operators);
}
