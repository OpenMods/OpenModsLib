package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.Operator;
import openmods.calc.OperatorDictionary;
import openmods.calc.UnaryOperator;
import openmods.utils.Stack;
import openmods.utils.Stack.StackUnderflowException;
import openmods.utils.Variant;

public class InfixCompiler<E> extends AstCompiler<E> {

	private final List<IExprNode<E>> NO_ARGS = Lists.newArrayList();

	private final IValueParser<E> valueParser;

	private final OperatorDictionary<E> operators;

	private final IExprNodeFactory<E> exprNodeFactory;

	public InfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		this.valueParser = valueParser;
		this.operators = operators;
		this.exprNodeFactory = exprNodeFactory;
	}

	private static class BracketInfo {
		private final String type;
		private int elementCount;

		public BracketInfo(String type) {
			this.type = type;
		}
	}

	private final Variant.Selector<BracketInfo> TYPE_BRACKET = Variant.createSelector();
	private final Variant.Selector<Operator<E>> TYPE_OPERATOR = Variant.createSelector();
	private final Variant.Selector<String> TYPE_SYMBOL = Variant.createSelector();

	@Override
	public IExprNode<E> compileAst(Iterable<Token> input) {
		final Stack<IExprNode<E>> nodeStack = Stack.create();
		final Stack<Variant> operatorStack = Stack.create();

		final BinaryOperator<E> defaultOperator = operators.getDefaultOperator();

		Token lastToken = null;

		for (Token token : input) {
			// nullary symbol fix
			if (lastToken != null && token.type != TokenType.LEFT_BRACKET && lastToken.type.isSymbol()) {
				final String symbol = operatorStack.pop().get(TYPE_SYMBOL);
				nodeStack.push(exprNodeFactory.createSymbolNode(symbol, NO_ARGS));
			}

			if (defaultOperator != null &&
					lastToken != null &&
					shouldInsertDefaultOperator(token, lastToken)) {
				pushOperator(nodeStack, operatorStack, defaultOperator);
				lastToken = new Token(TokenType.OPERATOR, defaultOperator.id);
			}

			if (token.type.isValue()) {
				final E value = valueParser.parseToken(token);
				nodeStack.push(exprNodeFactory.createValueNode(value));
			} else if (token.type.isSymbol()) {
				Preconditions.checkArgument(token.type != TokenType.SYMBOL_WITH_ARGS, "Symbol '%s' can't be used in infix mode", token.value);
				operatorStack.push(new Variant(TYPE_SYMBOL, token.value));
			} else {
				switch (token.type) {
					case LEFT_BRACKET:
						operatorStack.push(new Variant(TYPE_BRACKET, new BracketInfo(token.value)));
						break;
					case RIGHT_BRACKET: {
						if (lastToken == null) throw new UnmatchedBracketsException(token.value);
						final BracketInfo bracketInfo = popUntilBracket(nodeStack, operatorStack);
						final String endBracket = TokenUtils.getClosingBracket(bracketInfo.type);
						if (endBracket == null || !endBracket.equals(token.value)) throw new UnmatchedBracketsException(bracketInfo.type, token.value);
						if (lastToken.type != TokenType.LEFT_BRACKET) bracketInfo.elementCount++;
						createBracketNode(bracketInfo, operatorStack, nodeStack);
						break;
					}
					case SEPARATOR: {
						Preconditions.checkNotNull(lastToken, "Comma on invalid postion");
						final BracketInfo bracketInfo = popUntilBracket(nodeStack, operatorStack);
						bracketInfo.elementCount++;
						operatorStack.push(new Variant(TYPE_BRACKET, bracketInfo));
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
			else if (e.is(TYPE_BRACKET)) throw new UnmatchedBracketsException(e.get(TYPE_BRACKET).type);
			else if (e.is(TYPE_SYMBOL)) nodeStack.push(exprNodeFactory.createSymbolNode(e.get(TYPE_SYMBOL), NO_ARGS));
			else throw new AssertionError("What!?");
		}

		Preconditions.checkState(nodeStack.size() == 1, "Not valid infix expression");
		return nodeStack.pop();
	}

	private void createBracketNode(BracketInfo bracketInfo, Stack<Variant> operatorStack, Stack<IExprNode<E>> nodeStack) {
		final List<IExprNode<E>> reversedChildren = Lists.newArrayList();
		for (int i = 0; i < bracketInfo.elementCount; i++)
			reversedChildren.add(nodeStack.pop());

		final List<IExprNode<E>> children = Lists.reverse(reversedChildren);

		if (!operatorStack.isEmpty()) {
			final Variant top = operatorStack.peek(0);
			if (top.is(TYPE_SYMBOL)) {
				operatorStack.pop();
				final IExprNode<E> symbolNode = exprNodeFactory.createSymbolNode(top.get(TYPE_SYMBOL), children);
				nodeStack.push(symbolNode);
				return;
			}
		}

		nodeStack.push(exprNodeFactory.createBracketNode(bracketInfo.type, children));
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

	private BracketInfo popUntilBracket(Stack<IExprNode<E>> nodeStack, Stack<Variant> operatorStack) {
		try {
			while (true) {
				final Variant e = operatorStack.pop();
				if (e.is(TYPE_OPERATOR)) pushOperator(nodeStack, e.get(TYPE_OPERATOR));
				else if (e.is(TYPE_BRACKET)) return e.get(TYPE_BRACKET);
				else throw new AssertionError("What!?");
			}
		} catch (StackUnderflowException e) {
			throw new UnmatchedBracketsException();
		}
	}

	private void pushOperator(Stack<IExprNode<E>> nodeStack, Operator<E> op) {
		if (op instanceof BinaryOperator) {
			final IExprNode<E> right = nodeStack.pop();
			final IExprNode<E> left = nodeStack.pop();
			nodeStack.push(exprNodeFactory.createBinaryOpNode((BinaryOperator<E>)op, left, right));
		} else if (op instanceof UnaryOperator) {
			final IExprNode<E> arg = nodeStack.pop();
			nodeStack.push(exprNodeFactory.createUnaryOpNode((UnaryOperator<E>)op, arg));
		} else throw new IllegalStateException("Unknown type of operator: " + op.getClass());
	}
}
