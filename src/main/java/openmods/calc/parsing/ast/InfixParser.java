package openmods.calc.parsing.ast;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import openmods.calc.parsing.InvalidTokenException;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;
import openmods.calc.parsing.token.TokenUtils;
import openmods.utils.Stack;

public class InfixParser<N, O extends IOperator<O>> implements IAstParser<N> {

	private static final String CALL_OPENING_BRACKET = "(";

	private final IOperatorDictionary<O> operators;

	private INodeFactory<N, O> nodeFactory;

	public InfixParser(IOperatorDictionary<O> operators, INodeFactory<N, O> exprNodeFactory) {
		this.nodeFactory = exprNodeFactory;
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
	public N parse(IParserState<N> state, PeekingIterator<Token> input) {
		final Stack<N> nodeStack = Stack.create();
		final Stack<O> operatorStack = Stack.create();

		final O defaultOperator = operators.getDefaultOperator();

		boolean pushedNonOperatorLastLoop = false;

		while (input.hasNext()) {
			final Token token = input.peek();

			boolean pushedNonOperatorThisLoop = true;

			if (token.type.isExpressionTerminator())
				break;

			next(input);

			if (token.type.isValue()) {
				nodeStack.push(nodeFactory.createValueNode(token));
			} else if (token.type.isSymbol()) {
				Preconditions.checkArgument(token.type != TokenType.SYMBOL_WITH_ARGS, "Symbol '%s' can't be used in infix mode", token.value);

				if (input.hasNext()) {
					final Token nextToken = input.peek();
					if (nextToken.type == TokenType.LEFT_BRACKET && nextToken.value.equals(CALL_OPENING_BRACKET)) {
						input.next();
						final String openingBracket = nextToken.value;
						final String closingBracket = TokenUtils.getClosingBracket(openingBracket);
						final ISymbolCallStateTransition<N> stateTransition = state.getStateForSymbolCall(token.value);
						final List<N> childrenNodes = collectChildren(input, openingBracket, closingBracket, stateTransition.getState());
						nodeStack.push(stateTransition.createRootNode(childrenNodes));
					} else {
						nodeStack.push(nodeFactory.createSymbolGetNode(token.value));
					}
				} else {
					nodeStack.push(nodeFactory.createSymbolGetNode(token.value));
				}
			} else if (token.type == TokenType.MODIFIER) {
				final IModifierStateTransition<N> stateTransition = state.getStateForModifier(token.value);
				final IParserState<N> newState = stateTransition.getState();
				final IAstParser<N> newParser = newState.getParser();
				final N parsedNode = newParser.parse(newState, input);
				nodeStack.push(stateTransition.createRootNode(parsedNode));
			} else if (token.type == TokenType.LEFT_BRACKET) {
				final String openingBracket = token.value;
				final String closingBracket = TokenUtils.getClosingBracket(openingBracket);
				final List<N> childrenNodes = collectChildren(input, openingBracket, closingBracket, state);
				nodeStack.push(nodeFactory.createBracketNode(openingBracket, closingBracket, childrenNodes));
			} else if (token.type == TokenType.OPERATOR) {
				final O op;
				if (!pushedNonOperatorLastLoop) { // i.e. pushed operator or parsing started
					op = operators.getOperator(token.value, OperatorArity.UNARY);
					Preconditions.checkArgument(op != null, "No unary version of operator: %s", token.value);
				} else {
					op = operators.getOperator(token.value, OperatorArity.BINARY);
					Preconditions.checkArgument(op != null, "Invalid operator: %s", token.value);
				}

				pushOperator(nodeStack, operatorStack, op);
				pushedNonOperatorThisLoop = false;
			} else {
				throw new InvalidTokenException(token);
			}

			if (pushedNonOperatorLastLoop && pushedNonOperatorThisLoop) {
				// simulate push of operator in last loop
				final N thisLoopPush = nodeStack.pop();
				pushOperator(nodeStack, operatorStack, defaultOperator);
				nodeStack.push(thisLoopPush);
			}

			pushedNonOperatorLastLoop = pushedNonOperatorThisLoop;
		}

		while (!operatorStack.isEmpty()) {
			final O op = operatorStack.pop();
			pushOperator(nodeStack, op);
		}

		if (nodeStack.size() != 1) throw new NonExpressionException("Stack: " + nodeStack.printContents());
		return nodeStack.pop();
	}

	private List<N> collectChildren(PeekingIterator<Token> input, String openingBracket, String closingBracket, IParserState<N> compilerState) {
		final List<N> args = Lists.newArrayList();

		{
			if (!input.hasNext()) throw new UnmatchedBracketsException(openingBracket);
			if (input.peek().type == TokenType.RIGHT_BRACKET) {
				Token token = next(input);
				Preconditions.checkState(token.value.equals(closingBracket), "Unmatched brackets: '%s' and '%s'", openingBracket, token.value);
				return args;
			}
		}

		while (true) {
			final IAstParser<N> newParser = compilerState.getParser();
			final N parsedNode = newParser.parse(compilerState, input);
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

	private void pushOperator(Stack<N> output, Stack<O> operatorStack, O newOp) {
		while (!operatorStack.isEmpty()) {
			final O top = operatorStack.peek(0);
			if (!newOp.isLowerPriority(top)) break;
			operatorStack.pop();
			pushOperator(output, top);
		}

		operatorStack.push(newOp);
	}

	private void pushOperator(Stack<N> nodeStack, O op) {
		List<N> children = Lists.newArrayList();
		for (int i = 0; i < op.arity().args; i++)
			children.add(nodeStack.pop());

		final N newOpNode = nodeFactory.createOpNode(op, Lists.reverse(children));
		nodeStack.push(newOpNode);
	}
}
