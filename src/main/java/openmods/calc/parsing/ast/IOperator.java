package openmods.calc.parsing.ast;

public interface IOperator<O extends IOperator<O>> {

	public String id();

	public OperatorArity arity();

	public boolean isLowerPriority(O other);
}
