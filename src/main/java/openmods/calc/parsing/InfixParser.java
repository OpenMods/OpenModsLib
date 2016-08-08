package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import openmods.calc.BinaryOperator;
import openmods.calc.Operator;
import openmods.calc.OperatorDictionary;
import openmods.calc.UnaryOperator;
import openmods.calc.parsing.ICompilerState.IModifierStateTransition;
import openmods.calc.parsing.ICompilerState.ISymbolStateTransition;
import openmods.utils.Stack;

public class InfixParser<E> implements IAstParser<E> {

	private static final String CALL_OPENING_BRACKET = "(";

	private final List<IExprNode<E>> NO_ARGS = Lists.newArrayList();

	private final OperatorDictionary<E> operators;

	private IExprNodeFactory<E> exprNodeFactory;

	public InfixParser(OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		this.exprNodeFactory = exprNodeFactory;
		this.operators = operators;
	}

	private static Token next(Iterator<Token> input) {
		try {
			return input.next();
		} catch (NoSuchElementException e) {
			throw new UnfinishedExpressionException();
		}
	}

	@Override
	public IExprNode<E> parse(ICompilerState<E> state, PeekingIterator<Token> input) {
		final Stack<IExprNode<E>> nodeStack = Stack.create();
		final Stack<Operator<E>> operatorStack = Stack.create();

		final BinaryOperator<E> defaultOperator = operators.getDefaultOperator();

		boolean pushedNonOperatorLastLoop = false;

		while (input.hasNext()) {
			final Token token = input.peek();

			boolean pushedNonOperatorThisLoop = true;

			if (token.type.isExpressionTerminator())
				break;

			next(input);

			if (token.type.isValue()) {
				nodeStack.push(exprNodeFactory.createValueNode(token));
			} else if (token.type.isSymbol()) {
				Preconditions.checkArgument(token.type != TokenType.SYMBOL_WITH_ARGS, "Symbol '%s' can't be used in infix mode", token.value);
				final ISymbolStateTransition<E> stateTransition = state.getStateForSymbol(token.value);

				if (input.hasNext()) {
					final Token nextToken = input.peek();
					if (nextToken.type == TokenType.LEFT_BRACKET && nextToken.value.equals(CALL_OPENING_BRACKET)) {
						input.next();
						final String openingBracket = nextToken.value;
						final String closingBracket = TokenUtils.getClosingBracket(openingBracket);
						final List<IExprNode<E>> childrenNodes = collectChildren(input, openingBracket, closingBracket, stateTransition.getState());
						nodeStack.push(stateTransition.createRootNode(childrenNodes));
					} else {
						nodeStack.push(stateTransition.createRootNode(NO_ARGS));
					}
				} else {
					nodeStack.push(stateTransition.createRootNode(NO_ARGS));
				}
			} else if (token.type == TokenType.MODIFIER) {
				final IModifierStateTransition<E> stateTransition = state.getStateForModifier(token.value);
				final ICompilerState<E> newState = stateTransition.getState();
				final IAstParser<E> newParser = newState.getParser();
				final IExprNode<E> parsedNode = newParser.parse(newState, input);
				nodeStack.push(stateTransition.createRootNode(parsedNode));
			} else if (token.type == TokenType.LEFT_BRACKET) {
				final String openingBracket = token.value;
				final String closingBracket = TokenUtils.getClosingBracket(openingBracket);
				final List<IExprNode<E>> childrenNodes = collectChildren(input, openingBracket, closingBracket, state);
				nodeStack.push(exprNodeFactory.createBracketNode(openingBracket, closingBracket, childrenNodes));
			} else if (token.type == TokenType.OPERATOR) {
				final Operator<E> op;
				if (!pushedNonOperatorLastLoop) { // i.e. pushed operator or parsing started
					op = operators.getUnaryOperator(token.value);
					Preconditions.checkArgument(op != null, "No unary version of operator: %s", token.value);
				} else {
					op = operators.getBinaryOperator(token.value);
					Preconditions.checkArgument(op != null, "Invalid operator: %s", token.value);
				}

				pushOperator(nodeStack, operatorStack, op);
				pushedNonOperatorThisLoop = false;
			} else {
				throw new InvalidTokenException(token);
			}

			if (pushedNonOperatorLastLoop && pushedNonOperatorThisLoop) {
				// simulate push of operator in last loop
				final IExprNode<E> thisLoopPush = nodeStack.pop();
				pushOperator(nodeStack, operatorStack, defaultOperator);
				nodeStack.push(thisLoopPush);
			}

			pushedNonOperatorLastLoop = pushedNonOperatorThisLoop;
		}

		while (!operatorStack.isEmpty()) {
			final Operator<E> op = operatorStack.pop();
			pushOperator(nodeStack, op);
		}

		if (nodeStack.size() != 1) throw new NonExpressionException();
		return nodeStack.pop();
	}

	private List<IExprNode<E>> collectChildren(PeekingIterator<Token> input, String openingBracket, String closingBracket, ICompilerState<E> compilerState) {
		final List<IExprNode<E>> args = Lists.newArrayList();

		{
			if (!input.hasNext()) throw new UnmatchedBracketsException(openingBracket);
			if (input.peek().type == TokenType.RIGHT_BRACKET) {
				Token token = next(input);
				Preconditions.checkState(token.value.equals(closingBracket), "Unmatched brackets: '%s' and '%s'", openingBracket, token.value);
				return args;
			}
		}

		while (true) {
			final IAstParser<E> newParser = compilerState.getParser();
			final IExprNode<E> parsedNode = newParser.parse(compilerState, input);
			args.add(parsedNode);

			final Token token = next(input);
			if (token.type == TokenType.RIGHT_BRACKET) {
				if (!token.value.equals(closingBracket)) throw new UnmatchedBracketsException(openingBracket, token.value);
				return args;
			} else {
				Preconditions.checkState(token.type == TokenType.SEPARATOR, "Expected arg separator, got %s", token);
			}
		}
	}

	private void pushOperator(Stack<IExprNode<E>> output, Stack<Operator<E>> operatorStack, Operator<E> newOp) {
		while (!operatorStack.isEmpty()) {
			final Operator<E> top = operatorStack.peek(0);
			if (!newOp.isLessThan(top)) break;
			operatorStack.pop();
			pushOperator(output, top);
		}

		operatorStack.push(newOp);
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
