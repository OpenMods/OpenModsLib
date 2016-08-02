package openmods.calc;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Map;
import javax.annotation.Nullable;
import openmods.utils.Stack;

public abstract class Calculator<E, M> {

	public interface ICompiler<E> {
		public IExecutable<E> compile(String input);
	}

	public static final String VAR_ANS = "$ans";

	private final TopFrame<E> topFrame = new TopFrame<E>();

	private final E nullValue;

	private final Map<M, ICompiler<E>> compilers;

	public Calculator(E nullValue, Map<M, ICompiler<E>> compilers) {
		this.nullValue = nullValue;
		this.compilers = ImmutableMap.copyOf(compilers);

	}

	public E nullValue() {
		return nullValue;
	}

	public abstract String toString(E value);

	public IExecutable<E> compile(M type, String input) {
		final ICompiler<E> compiler = compilers.get(type);
		Preconditions.checkArgument(compiler != null, "Unknown compiler: " + type);
		return compiler.compile(input);
	}

	public void setGlobalSymbol(String id, ISymbol<E> value) {
		topFrame.setSymbol(id, value);
	}

	public int stackSize() {
		return topFrame.stack().size();
	}

	public Iterable<E> getStack() {
		return Iterables.unmodifiableIterable(topFrame.stack());
	}

	public Iterable<String> printStack() {
		return Iterables.transform(topFrame.stack(), new Function<E, String>() {
			@Override
			@Nullable
			public String apply(@Nullable E input) {
				return Calculator.this.toString(input);
			}
		});
	}

	public void execute(IExecutable<E> executable) {
		executable.execute(topFrame);
	}

	public E executeAndPop(IExecutable<E> executable) {
		executable.execute(topFrame);
		final Stack<E> stack = topFrame.stack();

		if (stack.isEmpty()) {
			topFrame.setSymbol(VAR_ANS, Constant.create(nullValue));
			return null;
		} else {
			final E result = stack.pop();
			topFrame.setSymbol(VAR_ANS, Constant.create(result));
			return result;
		}
	}

	public static String decorateBase(boolean allowCustom, int base, String value) {
		if (allowCustom) {
			switch (base) {
				case 2:
					return "0b" + value;
				case 8:
					return "0" + value;
				case 10:
					return value;
				case 16:
					return "0x" + value;
				default:
					return Integer.toString(base) + "#" + value;
			}
		} else {
			return Integer.toString(base) + "#" + value;
		}
	}
}
