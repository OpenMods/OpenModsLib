package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.Environment;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.SymbolMap;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.calc.parsing.SymbolCallNode;
import openmods.calc.parsing.ValueNode;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class PromiseExpressionFactory {

	private final TypeDomain domain;

	public PromiseExpressionFactory(TypeDomain domain) {
		this.domain = domain;
	}

	private class DelayStateTransition extends SameStateSymbolTransition<TypedValue> {

		public DelayStateTransition(ICompilerState<TypedValue> compilerState) {
			super(compilerState);
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			Preconditions.checkArgument(children.size() == 1, "'delay' expects single argument");
			final ValueNode<TypedValue> arg = new ValueNode<TypedValue>(Code.flattenAndWrap(domain, children.get(0)));
			return new SymbolCallNode<TypedValue>(TypedCalcConstants.SYMBOL_DELAY, ImmutableList.of(arg));
		}

	}

	public ISymbolCallStateTransition<TypedValue> createStateTransition(ICompilerState<TypedValue> compilerState) {
		return new DelayStateTransition(compilerState);
	}

	private static class DelayCallable extends FixedCallable<TypedValue> {

		private Optional<TypedValue> value = Optional.absent();

		private final SymbolMap<TypedValue> scope;

		private final Code code;

		public DelayCallable(SymbolMap<TypedValue> scope, Code code) {
			super(0, 1);
			this.scope = scope;
			this.code = code;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			if (!value.isPresent()) {
				final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrame(scope);

				code.execute(executionFrame);

				final Stack<TypedValue> resultStack = executionFrame.stack();
				Preconditions.checkState(resultStack.size() == 1, "Expected single result from promise execution, got %s", resultStack.size());
				value = Optional.of(resultStack.pop());
			}

			frame.stack().push(value.get());
		}

	}

	private static class DelaySymbol extends FixedCallable<TypedValue> {
		public DelaySymbol() {
			super(1, 1);
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack();
			final TypedValue arg = stack.pop();
			final Code code = arg.as(Code.class, "'code' argument");
			stack.push(FunctionValue.wrap(arg.domain, new DelayCallable(frame.symbols(), code)));
		}
	}

	private static class ForceSymbol extends FixedCallable<TypedValue> {
		public ForceSymbol() {
			super(1, 1);
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack();
			final TypedValue arg = stack.pop();

			MetaObjectUtils.call(frame, arg, OptionalInt.ZERO, OptionalInt.ONE);
		}

	}

	public void registerSymbols(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_DELAY, new DelaySymbol());
		env.setGlobalSymbol("force", new ForceSymbol());
	}

}
