package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.ExecutableList;
import openmods.calc.IExecutable;
import openmods.calc.Operator;
import openmods.calc.OperatorDictionary;
import openmods.calc.UnaryOperator;
import openmods.utils.Stack;
import openmods.utils.Stack.StackUnderflowException;
import openmods.utils.Variant;

public class InfixCompiler<E> implements ICompiler<E> {

	private final IValueParser<E> valueParser;

	private final OperatorDictionary<E> operators;

	private final IExprNodeFactory<E> exprNodeFactory;

	public InfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		this.valueParser = valueParser;
		this.operators = operators;
		this.exprNodeFactory = exprNodeFactory;
	}

	private final Variant.Selector<String> TYPE_BRACKET = Variant.createSelector();
	private final Variant.Selector<Operator<E>> TYPE_OPERATOR = Variant.createSelector();

	@Override
	public IExecutable<E> compile(Iterable<Token> input) {
		final Stack<IExprNode<E>> nodeStack = Stack.create();
		final Stack<Variant> operatorStack = Stack.create();

		final BinaryOperator<E> defaultOperator = operators.getDefaultOperator();

		Token lastToken = null;

		for (Token token : input) {
			if (defaultOperator != null &&
					lastToken != null &&
					shouldInsertDefaultOperator(token, lastToken)) {
				pushOperator(nodeStack, operatorStack, defaultOperator);
				lastToken = new Token(TokenType.OPERATOR, defaultOperator.id);
			}

			if (token.type.isValue()) {
				final E value = valueParser.parseToken(token);
				nodeStack.push(new ValueNode<E>(value));
			} else if (token.type.isSymbol()) {
				Preconditions.checkArgument(token.type != TokenType.SYMBOL_WITH_ARGS, "Symbol '%s' can't be used in infix mode", token.value);
				nodeStack.push(exprNodeFactory.createSymbolNode(token.value));
			} else {
				switch (token.type) {
					case LEFT_BRACKET:
						operatorStack.push(new Variant(TYPE_BRACKET, token.value));
						if (lastToken == null || !lastToken.type.isSymbol()) nodeStack.push(exprNodeFactory.createBracketNode(token.value));
						break;
					case RIGHT_BRACKET: {
						if (lastToken == null) throw new UnmatchedBracketsException(token.value);
						final String startBracket = popUntilBracket(nodeStack, operatorStack);
						final String endBracket = ExprTokenizerFactory.BRACKETS.get(startBracket);
						if (endBracket == null || !endBracket.equals(token.value)) throw new UnmatchedBracketsException(startBracket, token.value);
						if (lastToken.type != TokenType.LEFT_BRACKET) appendNodeChild(nodeStack);
						break;
					}
					case SEPARATOR: {
						Preconditions.checkNotNull(lastToken, "Comma on invalid postion");
						final String startBracket = popUntilBracket(nodeStack, operatorStack);
						appendNodeChild(nodeStack);
						operatorStack.push(new Variant(TYPE_BRACKET, startBracket));
						break;
					}
					case OPERATOR: {
						final Operator<E> op;
						if (lastToken == null || lastToken.type.isNextOpUnary()) {
							op = operators.getUnaryOperator(token.value);
							Preconditions.checkArgument(op != null, "No unary version of operator: %s", token.value);
						} else {
							op = operators.getBinaryOperator(token.value);
							Preconditions.checkArgument(op != null, "Invalid operator: %s", token.value);
						}

						pushOperator(nodeStack, operatorStack, op);
						break;
					}
					default:
						throw new InvalidTokenException(token);
				}
			}

			lastToken = token;
		}

		while (!operatorStack.isEmpty()) {
			final Variant e = operatorStack.pop();
			if (e.is(TYPE_OPERATOR)) pushOperator(nodeStack, e.get(TYPE_OPERATOR));
			else if (e.is(TYPE_BRACKET)) throw new UnmatchedBracketsException(e.get(TYPE_BRACKET));
			else throw new AssertionError("What!?");
		}

		Preconditions.checkState(nodeStack.size() == 1, "Not valid infix expression");
		final List<IExecutable<E>> output = Lists.newArrayList();
		nodeStack.pop().flatten(output);
		return new ExecutableList<E>(output);
	}

	protected boolean shouldInsertDefaultOperator(Token token, Token lastToken) {
		// special rule: always assume call
		if (token.type.isCallStart() && lastToken.type.isSymbol()) return false;
		return token.type.canInsertDefaultOpOnLeft() && lastToken.type.canInsertDefaultOpOnRight();
	}

	private void pushOperator(Stack<IExprNode<E>> output, Stack<Variant> operatorStack, Operator<E> newOp) {
		while (!operatorStack.isEmpty()) {
			final Variant top = operatorStack.peek(0);
			if (!top.is(TYPE_OPERATOR)) break;

			final Operator<E> topOp = top.get(TYPE_OPERATOR);
			if (!newOp.isLessThan(topOp)) break;

			operatorStack.pop();
			pushOperator(output, topOp);
		}

		operatorStack.push(new Variant(TYPE_OPERATOR, newOp));
	}

	private void appendNodeChild(Stack<IExprNode<E>> output) {
		final IExprNode<E> child = output.pop();
		final IExprNode<E> parent = output.peek(0);
		Preconditions.checkState(parent instanceof IInnerNode, "Expected inner node, got %s", parent.getClass());
		((IInnerNode<E>)parent).addChild(child);
	}

	private String popUntilBracket(Stack<IExprNode<E>> output, Stack<Variant> operatorStack) {
		try {
			while (true) {
				final Variant e = operatorStack.pop();
				if (e.is(TYPE_OPERATOR)) pushOperator(output, e.get(TYPE_OPERATOR));
				else if (e.is(TYPE_BRACKET)) return e.get(TYPE_BRACKET);
				else throw new AssertionError("What!?");
			}
		} catch (StackUnderflowException e) {
			throw new UnmatchedBracketsException();
		}
	}

	private void pushOperator(Stack<IExprNode<E>> output, Operator<E> op) {
		if (op instanceof BinaryOperator) {
			final IExprNode<E> right = output.pop();
			final IExprNode<E> left = output.pop();
			output.push(exprNodeFactory.createBinaryOpNode((BinaryOperator<E>)op, left, right));
		} else if (op instanceof UnaryOperator) {
			final IExprNode<E> arg = output.pop();
			output.push(exprNodeFactory.createUnaryOpNode((UnaryOperator<E>)op, arg));
		} else throw new IllegalStateException("Unknown type of operator: " + op.getClass());
	}
}
