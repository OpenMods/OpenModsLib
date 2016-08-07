package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.FixedSymbol;
import openmods.calc.ICalculatorFrame;
import openmods.calc.IExecutable;
import openmods.calc.ISymbol;
import openmods.calc.SymbolReference;
import openmods.calc.Value;
import openmods.calc.parsing.ExprUtils;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.ICompilerState.ISymbolStateTransition;
import openmods.calc.parsing.IExprNode;

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
			output.add(Value.create(domain.create(Code.class, new Code(ExprUtils.flattenNode(ifTrue)))));
			output.add(Value.create(domain.create(Code.class, new Code(ExprUtils.flattenNode(ifFalse)))));
			output.add(new SymbolReference<TypedValue>(ifSymbolName));
		}

		@Override
		@SuppressWarnings("unchecked")
		public Iterable<IExprNode<TypedValue>> getChildren() {
			return Lists.newArrayList(condition, ifTrue, ifFalse);
		}

	}

	private class IfStateTransition implements ISymbolStateTransition<TypedValue> {

		private final ICompilerState<TypedValue> parentState;

		public IfStateTransition(ICompilerState<TypedValue> parentState) {
			this.parentState = parentState;
		}

		@Override
		public ICompilerState<TypedValue> getState() {
			return parentState;
		}

		@Override
		public IExprNode<TypedValue> createRootNode(final List<IExprNode<TypedValue>> children) {
			Preconditions.checkState(children.size() == 3, "Expected 3 parameter for 'if', got %s", children.size());
			return new IfNode(children.get(0), children.get(1), children.get(2));

		}
	}

	public ISymbolStateTransition<TypedValue> createStateTransition(ICompilerState<TypedValue> parentState) {
		return new IfStateTransition(parentState);
	}

	private class IfSymbol extends FixedSymbol<TypedValue> {

		public IfSymbol() {
			super(3, 1);
		}

		@Override
		public void execute(ICalculatorFrame<TypedValue> frame) {
			final TypedValue ifFalse = frame.stack().pop();
			Preconditions.checkState(ifFalse.is(Code.class), "Expected code on this 'if' parameter, got %s", ifFalse);

			final TypedValue ifTrue = frame.stack().pop();
			Preconditions.checkState(ifTrue.is(Code.class), "Expected code on second 'if' parameter, got %s", ifTrue);

			final TypedValue condition = frame.stack().pop();
			final Optional<Boolean> isTruthy = condition.isTruthy();
			Preconditions.checkState(isTruthy.isPresent(), "%s is neither true or false", condition);

			(isTruthy.get()? ifTrue : ifFalse).unwrap(Code.class).execute(frame);
		}
	}

	public ISymbol<TypedValue> createSymbol() {
		return new IfSymbol();
	}
}
