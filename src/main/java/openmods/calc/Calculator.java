package openmods.calc;

import openmods.utils.Stack;

import com.google.common.collect.Iterables;

public abstract class Calculator<E> {

	public static final String VAR_ANS = "$ans";

	public enum ExprType {
		POSTFIX,
		INFIX
	}

	private final TopFrame<E> topFrame = new TopFrame<E>();

	private final OperatorDictionary<E> operators = new OperatorDictionary<E>();

	private final ExprTokenizerFactory tokenizerFactory = new ExprTokenizerFactory();

	private final ICompiler<E> rpnCompiler;

	private final ICompiler<E> infixCompiler;

	private final ISymbol<E> nullValue;

	public Calculator(IValueParser<E> parser, ISymbol<E> nullValue) {
		this.nullValue = nullValue;
		setupOperators(operators);

		for (String operator : operators.allOperators())
			tokenizerFactory.addOperator(operator);

		this.rpnCompiler = new PostfixCompiler<E>(parser, operators);
		this.infixCompiler = new InfixCompiler<E>(parser, operators);
		setupGenericFunctions(topFrame);
		setupGlobals(topFrame);
	}

	private static <E> void setupGenericFunctions(TopFrame<E> topFrame) {
		topFrame.setSymbol("swap", new Function<E>(2, 2) {
			@Override
			public void execute(ICalculatorFrame<E> frame) {
				final Stack<E> stack = frame.stack();

				final E first = stack.pop();
				final E second = stack.pop();

				stack.push(first);
				stack.push(second);
			}
		});

		topFrame.setSymbol("pop", new Function<E>(0, 1) {
			@Override
			public void execute(ICalculatorFrame<E> frame) {
				final Stack<E> stack = frame.stack();
				stack.pop();
			}
		});

		topFrame.setSymbol("dup", new Function<E>(1, 2) {
			@Override
			public void execute(ICalculatorFrame<E> frame) {
				final Stack<E> stack = frame.stack();
				final E value = stack.pop();

				stack.push(value);
				stack.push(value);
			}
		});
	}

	protected abstract void setupOperators(OperatorDictionary<E> operators);

	protected abstract void setupGlobals(TopFrame<E> globals);

	public IExecutable<E> compile(ExprType type, String input) {
		Iterable<Token> tokens = tokenizerFactory.tokenize(input);
		switch (type) {
			case INFIX:
				return infixCompiler.compile(tokens);
			case POSTFIX:
				return rpnCompiler.compile(tokens);
			default:
				throw new IllegalArgumentException(type.name());
		}
	}

	public void setGlobalSymbol(String id, ISymbol<E> value) {
		topFrame.setSymbol(id, value);
	}

	public Iterable<E> getStack() {
		return Iterables.unmodifiableIterable(topFrame.stack());
	}

	public void execute(IExecutable<E> executable) {
		executable.execute(topFrame);
	}

	public E executeAndPop(IExecutable<E> executable) {
		executable.execute(topFrame);
		final Stack<E> stack = topFrame.stack();

		if (stack.isEmpty()) {
			topFrame.setSymbol(VAR_ANS, nullValue);
			return null;
		} else {
			final E result = stack.pop();
			topFrame.setSymbol(VAR_ANS, Constant.create(result));
			return result;
		}
	}
}
