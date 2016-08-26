package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.PeekingIterator;
import openmods.calc.BinaryOperator;
import openmods.calc.ICallable;
import openmods.calc.OperatorDictionary;
import openmods.calc.UnaryOperator;
import openmods.calc.parsing.CallableOperatorWrappers;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SingleStateTransition;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;
import openmods.calc.parsing.ValueNode;

public class CallableOperatorWrapperModifierTransition extends SingleStateTransition.ForModifier<TypedValue> {

	private final TypeDomain domain;
	private final OperatorDictionary<TypedValue> operators;

	public CallableOperatorWrapperModifierTransition(TypeDomain domain, OperatorDictionary<TypedValue> operators) {
		this.domain = domain;
		this.operators = operators;
	}

	@Override
	public IExprNode<TypedValue> parseSymbol(ICompilerState<TypedValue> state, PeekingIterator<Token> input) {
		final Token token = input.next();
		Preconditions.checkState(token.type == TokenType.OPERATOR, "Expected operator token, got %s", token);

		final BinaryOperator<TypedValue> binaryOp = operators.getBinaryOperator(token.value);
		if (binaryOp != null) return createGetter(new CallableOperatorWrappers.Binary(binaryOp));

		final UnaryOperator<TypedValue> unaryOp = operators.getUnaryOperator(token.value);
		if (unaryOp != null) return createGetter(new CallableOperatorWrappers.Unary(unaryOp));

		throw new IllegalArgumentException("Unknown operator: " + token.value);
	}

	private IExprNode<TypedValue> createGetter(ICallable<TypedValue> wrapper) {
		return new ValueNode<TypedValue>(domain.create(ICallable.class, wrapper));
	}

	@Override
	public IExprNode<TypedValue> createRootNode(IExprNode<TypedValue> child) {
		return child;
	}
}
