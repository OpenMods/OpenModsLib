package openmods.calc;

import openmods.calc.executable.Operator;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.ast.IOperatorDictionary;

public interface ICompilerMapFactory<E, M> {
	public Compilers<E, M> create(E nullValue, IValueParser<E> valueParser, IOperatorDictionary<Operator<E>> operators, Environment<E> environment);
}
