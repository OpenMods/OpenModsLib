package openmods.calc;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import javax.annotation.Nullable;
import openmods.calc.parsing.ExprTokenizerFactory;
import openmods.calc.parsing.ICompiler;
import openmods.calc.parsing.IExprNodeFactory;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.InfixCompiler;
import openmods.calc.parsing.PostfixCompiler;
import openmods.calc.parsing.PrefixCompiler;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenUtils;
import openmods.utils.Stack;

public abstract class Calculator<E> {

	protected interface Accumulator<E> {
		public E accumulate(E prev, E value);
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

	private final ExprTokenizerFactory prefixTokenizerFactory = new ExprTokenizerFactory();

	private final ICompiler<E> rpnCompiler;

	private final ICompiler<E> infixCompiler;

	private final ICompiler<E> pnCompiler; // almostLispCompiler

	private final E nullValue;

	public Calculator(IValueParser<E> parser, E nullValue, OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		this.nullValue = nullValue;

		for (String operator : operators.allOperators()) {
			tokenizerFactory.addOperator(operator);
			prefixTokenizerFactory.addOperator(operator);
		}

		TokenUtils.setupTokenizerForQuoteNotation(prefixTokenizerFactory);

		this.pnCompiler = createPrefixCompiler(parser, operators, exprNodeFactory);
		this.rpnCompiler = createPostfixCompiler(parser, operators);
		this.infixCompiler = createInfixCompiler(parser, operators, exprNodeFactory);
	}

	public E nullValue() {
		return nullValue;
	}

	protected ICompiler<E> createPrefixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators, IExprNodeFactory<E> exprNodeFactory) {
		return new PrefixCompiler<E>(valueParser, operators, exprNodeFactory);
	}

	protected ICompiler<E> createInfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators, IExprNodeFactory<E> nodeFactory) {
		return new InfixCompiler<E>(valueParser, operators, nodeFactory);
	}

	protected ICompiler<E> createPostfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		return new PostfixCompiler<E>(valueParser, operators);
	}

	public abstract String toString(E value);

	public IExecutable<E> compile(ExprType type, String input) {
		switch (type) {
			case PREFIX: {
				Iterable<Token> tokens = prefixTokenizerFactory.tokenize(input);
				return pnCompiler.compile(tokens);
			}
			case INFIX: {
				Iterable<Token> tokens = tokenizerFactory.tokenize(input);
				return infixCompiler.compile(tokens);
			}
			case POSTFIX: {
				Iterable<Token> tokens = tokenizerFactory.tokenize(input);
				return rpnCompiler.compile(tokens);
			}
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

	protected Iterable<Token> tokenize(String input) {
		return tokenizerFactory.tokenize(input);
	}
}
