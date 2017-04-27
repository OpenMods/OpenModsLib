package openmods.calc;

import openmods.calc.executable.Operator;
import openmods.calc.executable.OperatorDictionary;
import openmods.calc.parsing.IValueParser;
import openmods.calc.symbol.GenericFunctions;

public abstract class SimpleCalculatorFactory<E, M> {

	protected abstract E getNullValue();

	protected Environment<E> createEnvironment() {
		return new Environment<E>(getNullValue());
	}

	protected abstract IValueParser<E> getValueParser();

	protected abstract IValuePrinter<E> createValuePrinter();

	protected abstract void configureEnvironment(Environment<E> env);

	protected abstract void configureOperators(OperatorDictionary<Operator<E>> operators);

	public Calculator<E, M> create(ICompilerMapFactory<E, M> compilersFactory) {
		final OperatorDictionary<Operator<E>> operators = new OperatorDictionary<Operator<E>>();
		configureOperators(operators);
		final Environment<E> env = createEnvironment();
		GenericFunctions.createStackManipulationFunctions(env);
		configureEnvironment(env);
		final Compilers<E, M> compilers = compilersFactory.create(getNullValue(), getValueParser(), operators, env);
		final IValuePrinter<E> printer = createValuePrinter();
		return new Calculator<E, M>(env, compilers, printer);
	}
}
