package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryFunction;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.SingleReturnCallable;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class LibFunctional {

	private static class PartialCallable extends CallableValue {

		private final TypedValue target;
		private final List<TypedValue> fixedArgs;

		public PartialCallable(TypedValue target, List<TypedValue> args) {
			this.target = target;
			this.fixedArgs = ImmutableList.copyOf(args);
		}

		@Override
		public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
			final int givenArgsCount = argumentsCount.get();

			if (givenArgsCount > 0) {
				final Stack<TypedValue> stack = frame.stack().substack(givenArgsCount);
				final List<TypedValue> givenArgs = ImmutableList.copyOf(stack);
				stack.clear();
				stack.pushAll(fixedArgs);
				stack.pushAll(givenArgs);
			} else {
				frame.stack().pushAll(fixedArgs);
			}

			MetaObjectUtils.call(frame, target, OptionalInt.of(fixedArgs.size() + givenArgsCount), returnsCount);
		}

		public PartialCallable expand(List<TypedValue> extraArgs) {
			final List<TypedValue> newArgs = Lists.newArrayList(this.fixedArgs);
			newArgs.addAll(extraArgs);
			return new PartialCallable(target, newArgs);
		}
	}

	private static class ChainedCallable extends CallableValue {

		private final List<TypedValue> chain;

		public ChainedCallable(List<TypedValue> chain) {
			this.chain = ImmutableList.copyOf(chain);
		}

		@Override
		public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack().substack(argumentsCount.get());

			for (int i = chain.size() - 1; i >= 0; i--) {
				final TypedValue target = chain.get(i);
				if (i == 0) {
					MetaObjectUtils.call(frame, target, argumentsCount, returnsCount);
				} else {
					MetaObjectUtils.call(frame, target, argumentsCount, OptionalInt.ABSENT);
					argumentsCount = OptionalInt.of(stack.size());
				}
			}
		}

		public ChainedCallable expand(TypedValue newPart) {
			final List<TypedValue> chain = Lists.newArrayList(this.chain);
			chain.add(newPart);
			return new ChainedCallable(chain);
		}

	}

	public static void register(Environment<TypedValue> env) {
		final TypedValue nullValue = env.nullValue();
		final TypeDomain typeDomain = nullValue.domain;

		env.setGlobalSymbol("curry", CallableValue.wrap(typeDomain, new SingleReturnCallable<TypedValue>() {
			@Override
			public TypedValue call(Frame<TypedValue> frame, OptionalInt argumentsCount) {
				final int argCount = argumentsCount.get();
				Preconditions.checkState(argCount > 0, "Expected more than one arg for 'curry' function");
				final Stack<TypedValue> stack = frame.stack();
				final Stack<TypedValue> argsStack = stack.substack(argCount - 1);
				final List<TypedValue> args = Lists.newArrayList(argsStack);
				argsStack.clear();

				final TypedValue target = stack.pop();
				if (target.value instanceof PartialCallable) {
					return ((PartialCallable)target.value).expand(args).selfValue(typeDomain);
				} else {
					Preconditions.checkState(MetaObjectUtils.isCallable(target), "Value %s is not callable", target);
					return new PartialCallable(target, args).selfValue(typeDomain);
				}
			}
		}));

		env.setGlobalSymbol("chain", CallableValue.wrap(typeDomain, new BinaryFunction.Direct<TypedValue>() {

			@Override
			protected TypedValue call(TypedValue left, TypedValue right) {
				Preconditions.checkState(MetaObjectUtils.isCallable(right), "Value %s is not callable", right);

				if (left.value instanceof ChainedCallable) {
					return ((ChainedCallable)left.value).expand(right).selfValue(typeDomain);
				} else {
					Preconditions.checkState(MetaObjectUtils.isCallable(left), "Value %s is not callable", left);
					return new ChainedCallable(ImmutableList.of(left, right)).selfValue(typeDomain);
				}
			}
		}));

		env.setGlobalSymbol("id", new CallableValue() {
			@Override
			public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
				if (argumentsCount.isPresent() && returnsCount.isPresent())
					Preconditions.checkArgument(argumentsCount.get() == returnsCount.get(), "Expected %s returns, got %s", argumentsCount.get(), returnsCount.get());

				int requiredArgs = 1;
				if (argumentsCount.isPresent()) requiredArgs = argumentsCount.get();
				if (returnsCount.isPresent()) requiredArgs = returnsCount.get();

				frame.stack().checkSizeIsAtLeast(requiredArgs);
			}
		}.selfValue(typeDomain));
	}

}
