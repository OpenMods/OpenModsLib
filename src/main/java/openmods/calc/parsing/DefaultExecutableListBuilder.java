package openmods.calc.parsing;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.ExecutableList;
import openmods.calc.ICalculatorFrame;
import openmods.calc.IExecutable;
import openmods.calc.Operator;
import openmods.calc.OperatorDictionary;
import openmods.calc.SymbolReference;
import openmods.calc.Value;

public class DefaultExecutableListBuilder<E> implements IExecutableListBuilder<E> {

	private static class Nop<E> implements IExecutable<E> {

		@Override
		public void execute(ICalculatorFrame<E> frame) {}

		@Override
		public String serialize() {
			return "<nop>";
		}

	}

	private final IValueParser<E> valueParser;
	private final OperatorDictionary<E> operators;

	private final List<IExecutable<E>> buffer = Lists.newArrayList();

	public DefaultExecutableListBuilder(IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		this.valueParser = valueParser;
		this.operators = operators;
	}

	protected void addToBuffer(IExecutable<E> executable) {
		buffer.add(executable);
	}

	@Override
	public void appendValue(E value) {
		addToBuffer(Value.create(value));
	}

	@Override
	public void appendValue(Token token) {
		try {
			final E parsedValue = valueParser.parseToken(token);
			appendValue(parsedValue);
		} catch (Throwable t) {
			throw new InvalidTokenException(token, t);
		}
	}

	private Operator<E> getAnyOperator(String id) {
		Operator<E> op = operators.getBinaryOperator(id);
		if (op != null) return op;
		op = operators.getUnaryOperator(id);
		if (op != null) return op;
		throw new IllegalArgumentException("Invalid operator: " + id);
	}

	@Override
	public void appendOperator(String id) {
		addToBuffer(getAnyOperator(id));
	}

	@Override
	public void appendSymbol(String id) {
		addToBuffer(new SymbolReference<E>(id));
	}

	@Override
	public void appendSymbol(String id, Optional<Integer> argCount, Optional<Integer> returnCount) {
		addToBuffer(new SymbolReference<E>(id).setArgumentsCount(argCount).setReturnsCount(returnCount));
	}

	@Override
	public void appendExecutable(IExecutable<E> executable) {
		if (executable instanceof Nop) {
			// well, no-op
		} else if (executable instanceof ExecutableList) {
			List<IExecutable<E>> flattenedList = Lists.newArrayList();
			((ExecutableList<E>)executable).deepFlatten(flattenedList);

			for (IExecutable<E> e : flattenedList)
				addToBuffer(e);
		} else {
			addToBuffer(executable);
		}
	}

	@Override
	public IExecutable<E> build() {
		if (buffer.size() == 0) return new Nop<E>();
		if (buffer.size() == 1) return buffer.get(0);
		return new ExecutableList<E>(buffer);
	}

}
