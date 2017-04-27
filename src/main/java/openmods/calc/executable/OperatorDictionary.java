package openmods.calc.executable;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Set;
import openmods.calc.parsing.ast.IOperator;
import openmods.calc.parsing.ast.IOperatorDictionary;
import openmods.calc.parsing.ast.OperatorArity;

public class OperatorDictionary<O extends IOperator<O>> implements IOperatorDictionary<O> {

	private final Table<OperatorArity, String, O> operators = HashBasedTable.create();

	private O defaultOperator;

	public class OperatorRegistration<T extends O> {
		private final T op;

		private OperatorRegistration(T op) {
			this.op = op;
		}

		public OperatorRegistration<T> setDefault() {
			setDefaultOperator(op);
			return this;
		}

		public T unwrap() {
			return op;
		}
	}

	public <T extends O> OperatorRegistration<T> registerOperator(T operator) {
		final O prev = operators.put(operator.arity(), operator.id(), operator);
		Preconditions.checkState(prev == null, "Duplicate operator '%s': %s -> %s", prev, operator);
		return new OperatorRegistration<T>(operator);
	}

	public <T extends O> T registerDefaultOperator(T operator) {
		setDefaultOperator(operator);
		return operator;
	}

	private void setDefaultOperator(O operator) {
		Preconditions.checkState(operator.arity() == OperatorArity.BINARY, "Only binary operators can be default, trying to set %s", operator);
		Preconditions.checkState(defaultOperator == null, "Trying to replace default operator: %s -> %s", defaultOperator, operator);
		defaultOperator = operator;
	}

	@Override
	public Set<String> allOperatorIds() {
		return operators.columnKeySet();
	}

	@Override
	public O getOperator(String op, OperatorArity arity) {
		return operators.get(arity, op);
	}

	@Override
	public O getDefaultOperator() {
		return defaultOperator;
	}
}
