package openmods.calc.types.multi;

import com.google.common.collect.PeekingIterator;
import openmods.calc.executable.Operator;
import openmods.calc.parsing.ast.IOperatorDictionary;
import openmods.calc.parsing.ast.IParserState;
import openmods.calc.parsing.ast.OperatorArity;
import openmods.calc.parsing.ast.SingleStateTransition;
import openmods.calc.parsing.node.IExprNode;
import openmods.calc.parsing.node.SymbolGetNode;
import openmods.calc.parsing.node.ValueNode;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;
import openmods.calc.symbol.CallableOperatorWrapper;
import openmods.calc.symbol.ICallable;

public class CallableGetModifierTransition extends SingleStateTransition.ForModifier<IExprNode<TypedValue>> {

	private final TypeDomain domain;
	private final IOperatorDictionary<Operator<TypedValue>> operators;

	public CallableGetModifierTransition(TypeDomain domain, IOperatorDictionary<Operator<TypedValue>> operators) {
		this.domain = domain;
		this.operators = operators;
	}

	@Override
	public IExprNode<TypedValue> parseSymbol(IParserState<IExprNode<TypedValue>> state, PeekingIterator<Token> input) {
		final Token token = input.next();
		if (token.type == TokenType.OPERATOR) {
			Operator<TypedValue> op = operators.getOperator(token.value, OperatorArity.BINARY);
			if (op == null) op = operators.getOperator(token.value, OperatorArity.UNARY);
			if (op == null) throw new IllegalArgumentException("Unknown operator: " + token.value);
			return createGetter(new CallableOperatorWrapper(op));
		} else if (token.type == TokenType.SYMBOL) { return new SymbolGetNode<TypedValue>(token.value); }

		throw new IllegalStateException("Expected operator or symbol token, got " + token);
	}

	private IExprNode<TypedValue> createGetter(ICallable<TypedValue> wrapper) {
		return new ValueNode<TypedValue>(CallableValue.wrap(domain, wrapper));
	}

	@Override
	public IExprNode<TypedValue> createRootNode(IExprNode<TypedValue> child) {
		return child;
	}
}
