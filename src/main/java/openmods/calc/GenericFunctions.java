package openmods.calc;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.utils.Stack;

public class GenericFunctions {

	public interface Accumulator<E> {
		public E accumulate(E prev, E value);
	}

	// WARNING: this assumes 'accumulate' operation is associative!
	public abstract static class AccumulatorFunction<E> extends FunctionSymbol<E> {
		private final E nullValue;

		public AccumulatorFunction(E nullValue) {
			this.nullValue = nullValue;
		}

		@Override
		public void call(ICalculatorFrame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			if (returnsCount.isPresent() && returnsCount.get() != 1) throw new StackValidationException("Invalid expected return values count");

			final Stack<E> stack = frame.stack();
			final int args = argumentsCount.or(2);

			if (args == 0) {
				stack.push(nullValue);
			} else {
				E result = stack.pop();

				for (int i = 1; i < args; i++) {
					final E value = stack.pop();
					result = accumulate(value, result);
				}

				stack.push(process(result, args));
			}
		}

		protected E process(E result, int argCount) {
			return result;
		}

		protected abstract E accumulate(E result, E value);
	}

	public static <E> void createStackManipulationFunctions(Environment<E> calculator) {
		calculator.setGlobalSymbol("swap", new FixedFunctionSymbol<E>(2, 2) {
			@Override
			public void call(ICalculatorFrame<E> frame) {
				final Stack<E> stack = frame.stack();

				final E first = stack.pop();
				final E second = stack.pop();

				stack.push(first);
				stack.push(second);
			}
		});

		calculator.setGlobalSymbol("pop", new FunctionSymbol<E>() {
			@Override
			public void call(ICalculatorFrame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				if (returnsCount.isPresent() && returnsCount.get() != 0) throw new StackValidationException("Invalid expected return values on 'pop'");

				final Stack<E> stack = frame.stack();

				final int count = argumentsCount.or(1);
				for (int i = 0; i < count; i++)
					stack.pop();
			}
		});

		calculator.setGlobalSymbol("dup", new FunctionSymbol<E>() {
			@Override
			public void call(ICalculatorFrame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				final Stack<E> stack = frame.stack();

				List<E> values = Lists.newArrayList();

				final int in = argumentsCount.or(1);
				for (int i = 0; i < in; i++) {
					final E value = stack.pop();
					values.add(value);
				}

				values = Lists.reverse(values);

				final int out = returnsCount.or(2 * in);
				for (int i = 0; i < out; i++) {
					final E value = values.get(i % in);
					stack.push(value);
				}
			}
		});
	}

}
