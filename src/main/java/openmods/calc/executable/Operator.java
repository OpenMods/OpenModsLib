package openmods.calc.executable;

import openmods.calc.parsing.ast.IOperator;

public abstract class Operator<E> implements IExecutable<E>, IOperator<Operator<E>> {

	public final String id;

	public Operator(String id) {
		this.id = id;
	}

	@Override
	public String id() {
		return id;
	}

}
