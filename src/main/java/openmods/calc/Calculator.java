package openmods.calc;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.ExprTokenizerFactory;
import openmods.calc.parsing.ICompiler;
import openmods.calc.parsing.IExprNodeFactory;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.InfixCompiler;
import openmods.calc.parsing.PostfixCompiler;
import openmods.calc.parsing.PrefixCompiler;
import openmods.calc.parsing.Token;
import openmods.utils.Stack;

public abstract class Calculator<E> {

	protected interface Accumulator<E> {
		public E accumulate(E prev, E value);
	}

	protected abstract class AccumulatorFunction implements ISymbol<E> {
		@Override
		public void execute(ICalculatorFrame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			if (returnsCount.isPresent() && returnsCount.get() != 1) throw new StackValidationException("Invalid expected return values count");

			final Stack<E> stack = frame.stack();
			final int args = argumentsCount.or(2);

			if (args == 0) {
				stack.push(nullValue);
			} else {
				E result = stack.pop();

				for (int i = 1; i < args; i++) {
					final E value = stack.pop();
					result = accumulate(result, value);
				}

				stack.push(process(result, args));
			}
		}

		protected E process(E result, int argCount) {
			return result;
		}

		protected abstract E accumulate(E result, E value);
	}

	public static final String VAR_ANS = "$ans";

	public enum ExprType {
		PREFIX(true),
		INFIX(true),
		POSTFIX(false);

		public final boolean hasSingleResult;

		private ExprType(boolean hasSingleResult) {
			this.hasSingleResult = hasSingleResult;
		}

	}

	private final TopFrame<E> topFrame = new TopFrame<E>();

	private final ExprTokenizerFactory tokenizerFactory = new ExprTokenizerFactory();

	private final ICompiler<E> rpnCompiler;

	private final ICompiler<E> infixCompiler;

	private final ICompiler<E> pnCompiler; // almostLispCompiler

	private final E nullValue;

	public Calculator(IValueParser<E> parser, E nullValue) {
		this.nullValue = nullValue;

		final OperatorDictionary<E> operators = createOperatorDictionary();
		final IExprNodeFactory<E> exprNodeFactory = createExprNodeFactory();
		setupOperators(operators, exprNodeFactory);

		for (String operator : operators.allOperators())
			tokenizerFactory.addOperator(operator);

		this.pnCompiler = createPrefixCompiler(parser, operators, exprNodeFactory);
		this.rpnCompiler = createPostfixCompiler(parser, operators);
		this.infixCompiler = createInfixCompiler(parser, operators, exprNodeFactory);
		setupGenericFunctions(topFrame);
		setupGlobals(topFrame);
	}

	public E nullValue() {
		return nullValue;
	}

	protected IExprNodeFactory<E> createExprNodeFactory() {
		return new DefaultExprNodeFactory<E>();
	}

	protected OperatorDictionary<E> createOperatorDictionary() {
		return new OperatorDictionary<E>();
	}

	protected void setupOperators(OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {}

	protected ICompiler<E> createPrefixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		return new PrefixCompiler<E>(valueParser, operators, exprNodeFactory);
	}

	protected ICompiler<E> createInfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators, IExprNodeFactory<E> nodeFactory) {
		return new InfixCompiler<E>(valueParser, operators, nodeFactory);
	}

	protected ICompiler<E> createPostfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		return new PostfixCompiler<E>(valueParser, operators);
	}

	private static <E> void setupGenericFunctions(TopFrame<E> topFrame) {
		topFrame.setSymbol("swap", new FixedSymbol<E>(2, 2) {
			@Override
			public void execute(ICalculatorFrame<E> frame) {
				final Stack<E> stack = frame.stack();

				final E first = stack.pop();
				final E second = stack.pop();

				stack.push(first);
				stack.push(second);
			}
		});

		topFrame.setSymbol("pop", new ISymbol<E>() {
			@Override
			public void execute(ICalculatorFrame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				if (returnsCount.isPresent() && returnsCount.get() != 0) throw new StackValidationException("Invalid expected return values on 'pop'");

				final Stack<E> stack = frame.stack();

				final int count = argumentsCount.or(1);
				for (int i = 0; i < count; i++)
					stack.pop();
			}
		});

		topFrame.setSymbol("dup", new ISymbol<E>() {
			@Override
			public void execute(ICalculatorFrame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				final Stack<E> stack = frame.stack();

				List<E> values = Lists.newArrayList();

				final int in = argumentsCount.or(1);
				for (int i = 0; i < in; i++) {
					final E value = stack.pop();
					values.add(value);
				}

				values = Lists.reverse(values);

				final int out = returnsCount.or(2 * in);
				for (int i = 0; i < out; i++) {
					final E value = values.get(i % in);
					stack.push(value);
				}
			}
		});
	}

	public abstract String toString(E value);

	protected abstract void setupGlobals(TopFrame<E> globals);

	public IExecutable<E> compile(ExprType type, String input) {
		Iterable<Token> tokens = tokenizerFactory.tokenize(input);
		switch (type) {
			case PREFIX:
				return pnCompiler.compile(tokens);
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

	public int stackSize() {
		return topFrame.stack().size();
	}

	public Iterable<E> getStack() {
		return Iterables.unmodifiableIterable(topFrame.stack());
	}

	public Iterable<String> printStack() {
		return Iterables.transform(topFrame.stack(), new Function<E, String>() {
			@Override
			@Nullable
			public String apply(@Nullable E input) {
				return Calculator.this.toString(input);
			}
		});
	}

	public void execute(IExecutable<E> executable) {
		executable.execute(topFrame);
	}

	public E executeAndPop(IExecutable<E> executable) {
		executable.execute(topFrame);
		final Stack<E> stack = topFrame.stack();

		if (stack.isEmpty()) {
			topFrame.setSymbol(VAR_ANS, Constant.create(nullValue));
			return null;
		} else {
			final E result = stack.pop();
			topFrame.setSymbol(VAR_ANS, Constant.create(result));
			return result;
		}
	}

	public static String decorateBase(boolean allowCustom, int base, String value) {
		if (allowCustom) {
			switch (base) {
				case 2:
					return "0b" + value;
				case 8:
					return "0" + value;
				case 10:
					return value;
				case 16:
					return "0x" + value;
				default:
					return Integer.toString(base) + "#" + value;
			}
		} else {
			return Integer.toString(base) + "#" + value;
		}
	}
}
