package openmods.calc;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import javax.annotation.Nullable;
import openmods.calc.parsing.AstCompiler;
import openmods.calc.parsing.DefaultExprNodeFactory;
import openmods.calc.parsing.IAstParser;
import openmods.calc.parsing.ICompiler;
import openmods.calc.parsing.IExprNodeFactory;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.InfixParser;
import openmods.calc.parsing.PostfixCompiler;
import openmods.calc.parsing.PrefixParser;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenUtils;
import openmods.calc.parsing.Tokenizer;
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

	private final Tokenizer tokenizer = new Tokenizer();

	private final Tokenizer extendedTokenizer = new Tokenizer();

	private final ICompiler<E> rpnCompiler;

	private final ICompiler<E> infixCompiler;

	private final ICompiler<E> pnCompiler; // almostLispCompiler

	private final E nullValue;

	// TODO try to remove it - should be possible after prefix - infix merge
	public interface IAstParserFactory<E> {
		public IAstParser<E> create(IExprNodeFactory<E> exprNodeFactory);
	}

	public Calculator(final IValueParser<E> valueParser, E nullValue, final OperatorDictionary<E> operators) {
		this.nullValue = nullValue;

		for (String operator : operators.allOperators()) {
			tokenizer.addOperator(operator);
			extendedTokenizer.addOperator(operator);
		}

		TokenUtils.setupTokenizerForQuoteNotation(extendedTokenizer);

		this.pnCompiler = createPrefixCompiler(createNodeFactory(new IAstParserFactory<E>() {
			@Override
			public IAstParser<E> create(IExprNodeFactory<E> exprNodeFactory) {
				return new PrefixParser<E>(valueParser, operators, exprNodeFactory);
			}
		}));
		this.rpnCompiler = createPostfixCompiler(valueParser, operators);
		this.infixCompiler = createInfixCompiler(createNodeFactory(new IAstParserFactory<E>() {
			@Override
			public IAstParser<E> create(IExprNodeFactory<E> exprNodeFactory) {
				return new InfixParser<E>(valueParser, operators, exprNodeFactory);
			}
		}));
	}

	public E nullValue() {
		return nullValue;
	}

	protected IExprNodeFactory<E> createNodeFactory(final IAstParserFactory<E> parserFactory) {
		return new DefaultExprNodeFactory<E>() {
			@Override
			public IAstParser<E> getParser() {
				return parserFactory.create(this);
			}
		};
	}

	protected ICompiler<E> createPrefixCompiler(IExprNodeFactory<E> exprNodeFactory) {
		return new AstCompiler<E>(exprNodeFactory);
	}

	protected ICompiler<E> createInfixCompiler(IExprNodeFactory<E> exprNodeFactory) {
		return new AstCompiler<E>(exprNodeFactory);
	}

	protected ICompiler<E> createPostfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		return new PostfixCompiler<E>(valueParser, operators);
	}

	public abstract String toString(E value);

	private static <E> IExecutable<E> compile(Tokenizer tokenizer, ICompiler<E> compiler, String input) {
		final PeekingIterator<Token> tokens = tokenizer.tokenize(input);
		final IExecutable<E> result = compiler.compile(tokens);
		if (tokens.hasNext())
			throw new IllegalStateException("Unconsumed tokens: " + Lists.newArrayList(tokens));

		return result;
	}

	public IExecutable<E> compile(ExprType type, String input) {
		switch (type) {
			case PREFIX:
				return compile(extendedTokenizer, pnCompiler, input);
			case INFIX:
				return compile(extendedTokenizer, infixCompiler, input);
			case POSTFIX: {
				return compile(tokenizer, rpnCompiler, input);
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

	protected PeekingIterator<Token> tokenize(String input) {
		return tokenizer.tokenize(input);
	}
}
