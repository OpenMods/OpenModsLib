package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.Value;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.SameStateSymbolTransition;

public class IfExpressionFactory {

	private final TypeDomain domain;
	private final String ifSymbolName;

	public IfExpressionFactory(TypeDomain domain, String ifSymbolName) {
		this.domain = domain;
		this.ifSymbolName = ifSymbolName;
	}

	private class IfNode implements IExprNode<TypedValue> {
		private final IExprNode<TypedValue> condition;
		private final IExprNode<TypedValue> ifTrue;
		private final IExprNode<TypedValue> ifFalse;

		public IfNode(IExprNode<TypedValue> condition, IExprNode<TypedValue> ifTrue, IExprNode<TypedValue> ifFalse) {
			this.condition = condition;
			this.ifTrue = ifTrue;
			this.ifFalse = ifFalse;
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			condition.flatten(output);
			output.add(Value.create(Code.flattenAndWrap(domain, ifTrue)));
			output.add(Value.create(Code.flattenAndWrap(domain, ifFalse)));
			output.add(new SymbolCall<TypedValue>(ifSymbolName, 3, 1));
		}

		@Override
		@SuppressWarnings("unchecked")
		public Iterable<IExprNode<TypedValue>> getChildren() {
			return Lists.newArrayList(condition, ifTrue, ifFalse);
		}

	}

	private class IfStateTransition extends SameStateSymbolTransition<TypedValue> {
		public IfStateTransition(ICompilerState<TypedValue> parentState) {
			super(parentState);
		}

		@Override
		public IExprNode<TypedValue> createRootNode(final List<IExprNode<TypedValue>> children) {
			Preconditions.checkState(children.size() == 3, "Expected 3 parameter for 'if', got %s", children.size());
			return new IfNode(children.get(0), children.get(1), children.get(2));

		}
	}

	public ISymbolCallStateTransition<TypedValue> createStateTransition(ICompilerState<TypedValue> parentState) {
		return new IfStateTransition(parentState);
	}

	private class IfSymbol extends FixedCallable<TypedValue> {

		public IfSymbol() {
			super(3, 1);
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final TypedValue ifFalse = frame.stack().pop();
			ifFalse.checkType(Code.class, "third (false branch) 'if' parameter");

			final TypedValue ifTrue = frame.stack().pop();
			ifTrue.checkType(Code.class, "second (true branch) 'if' parameter");

			final TypedValue condition = frame.stack().pop();
			final Optional<Boolean> isTruthy = condition.isTruthy();
			Preconditions.checkState(isTruthy.isPresent(), "%s is neither true or false", condition);

			(isTruthy.get()? ifTrue : ifFalse).as(Code.class).execute(frame);
		}
	}

	public ICallable<TypedValue> createSymbol() {
		return new IfSymbol();
	}
}
