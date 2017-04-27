package openmods.calc.parsing.ast;

import java.util.Set;

public interface IOperatorDictionary<O extends IOperator<O>> {

	public O getOperator(String op, OperatorArity arity);

	public O getDefaultOperator();

	public Set<String> allOperatorIds();
}
