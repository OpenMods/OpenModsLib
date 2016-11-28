package openmods.calc;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.Arrays;
import java.util.List;
import openmods.calc.BinaryOperator.Associativity;
import openmods.calc.parsing.ContainerNode;
import openmods.calc.parsing.DummyNode;
import openmods.calc.parsing.IAstParser;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.IModifierStateTransition;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.ITokenStreamCompiler;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.QuotedParser;
import openmods.calc.parsing.QuotedParser.IQuotedExprNodeFactory;
import openmods.calc.parsing.SymbolCallNode;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;
import openmods.calc.types.multi.TypedCalcConstants;
import openmods.utils.Stack;
import org.junit.Assert;

public class CalcTestUtils {

	public static class ValueParserHelper<E> {
		public final IValueParser<E> parser;

		public ValueParserHelper(IValueParser<E> parser) {
			this.parser = parser;
		}

		private E parse(TokenType type, String value) {
			return parser.parseToken(new Token(type, value));
		}

		public E bin(String value) {
			return parse(TokenType.BIN_NUMBER, value);
		}

		public void testBin(E expected, String input) {
			Assert.assertEquals(expected, bin(input));
		}

		public E oct(String value) {
			return parse(TokenType.OCT_NUMBER, value);
		}

		public void testOct(E expected, String input) {
			Assert.assertEquals(expected, oct(input));
		}

		public E dec(String value) {
			return parse(TokenType.DEC_NUMBER, value);
		}

		public void testDec(E expected, String input) {
			Assert.assertEquals(expected, dec(input));
		}

		public E hex(String value) {
			return parse(TokenType.HEX_NUMBER, value);
		}

		public void testHex(E expected, String input) {
			Assert.assertEquals(expected, hex(input));
		}

		public E quoted(String value) {
			return parse(TokenType.QUOTED_NUMBER, value);
		}

		public void testQuoted(E expected, String input) {
			Assert.assertEquals(expected, quoted(input));
		}
	}

	public static Token t(TokenType type, String value) {
		return new Token(type, value);
	}

	public static Token dec(String value) {
		return t(TokenType.DEC_NUMBER, value);
	}

	public static Token oct(String value) {
		return t(TokenType.OCT_NUMBER, value);
	}

	public static Token hex(String value) {
		return t(TokenType.HEX_NUMBER, value);
	}

	public static Token bin(String value) {
		return t(TokenType.BIN_NUMBER, value);
	}

	public static Token quoted(String value) {
		return t(TokenType.QUOTED_NUMBER, value);
	}

	public static Token string(String value) {
		return t(TokenType.STRING, value);
	}

	public static Token symbol(String value) {
		return t(TokenType.SYMBOL, value);
	}

	public static Token symbol_args(String value) {
		return t(TokenType.SYMBOL_WITH_ARGS, value);
	}

	public static Token op(String value) {
		return t(TokenType.OPERATOR, value);
	}

	public static Token mod(String value) {
		return t(TokenType.MODIFIER, value);
	}

	public static Token leftBracket(String value) {
		return t(TokenType.LEFT_BRACKET, value);
	}

	public static Token rightBracket(String value) {
		return t(TokenType.RIGHT_BRACKET, value);
	}

	public static final Token COMMA = t(TokenType.SEPARATOR, ",");
	public static final Token RIGHT_BRACKET = rightBracket(")");
	public static final Token LEFT_BRACKET = leftBracket("(");

	public static final Token QUOTE_MODIFIER = mod(TypedCalcConstants.MODIFIER_QUOTE);

	public static final Token QUOTE_SYMBOL = symbol(TypedCalcConstants.SYMBOL_QUOTE);

	public static class DummyBinaryOperator<E> extends BinaryOperator<E> {

		public DummyBinaryOperator(int precendence, String id) {
			super(id, precendence);
		}

		public DummyBinaryOperator(int precendence, String id, Associativity associativity) {
			super(id, precendence, associativity);
		}

		@Override
		public E execute(E left, E right) {
			return null;
		}
	}

	public static class DummyUnaryOperator<E> extends UnaryOperator<E> {
		public DummyUnaryOperator(String id) {
			super(id);
		}

		@Override
		public E execute(E value) {
			return null;
		}
	}

	public static final BinaryOperator<String> PLUS = new DummyBinaryOperator<String>(1, "+");

	public static final Token OP_PLUS = op("+");

	public static final UnaryOperator<String> UNARY_PLUS = new DummyUnaryOperator<String>("+");

	public static final BinaryOperator<String> MINUS = new DummyBinaryOperator<String>(1, "-");

	public static final Token OP_MINUS = op("-");

	public static final UnaryOperator<String> UNARY_MINUS = new DummyUnaryOperator<String>("-");

	public static final UnaryOperator<String> UNARY_NEG = new DummyUnaryOperator<String>("!");

	public static final Token OP_NEG = op("!");

	public static final BinaryOperator<String> MULTIPLY = new DummyBinaryOperator<String>(2, "*");

	public static final Token OP_MULTIPLY = op("*");

	public static final BinaryOperator<String> ASSIGN = new DummyBinaryOperator<String>(1, "=", Associativity.RIGHT);

	public static final Token OP_ASSIGN = op("=");

	public static IExecutable<String> c(String value) {
		return new Value<String>(value);
	}

	public static SymbolGet<String> get(String value) {
		return new SymbolGet<String>(value);
	}

	public static SymbolCall<String> call(String value) {
		return new SymbolCall<String>(value);
	}

	public static SymbolCall<String> call(String value, int args) {
		return new SymbolCall<String>(value, args, 1);
	}

	public static SymbolCall<String> call(String value, int args, int rets) {
		return new SymbolCall<String>(value, args, rets);
	}

	public static SymbolCall<String> call(String value, Optional<Integer> args, Optional<Integer> rets) {
		return new SymbolCall<String>(value, args, rets);
	}

	public static ExecutableList<String> list(IExecutable<String>... elements) {
		return new ExecutableList<String>(elements);
	}

	public static class MarkerExecutable<E> implements IExecutable<E> {

		public String tag;

		public MarkerExecutable(String tag) {
			this.tag = Strings.nullToEmpty(tag);
		}

		@Override
		public void execute(Frame<E> frame) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			return tag.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			return obj instanceof MarkerExecutable
					&& ((MarkerExecutable<?>)obj).tag.equals(this.tag);
		}

		@Override
		public String toString() {
			return "!!!" + tag;
		}

	}

	public static IExecutable<String> marker(String tag) {
		return new MarkerExecutable<String>(tag);
	}

	public static final IValueParser<String> VALUE_PARSER = new IValueParser<String>() {
		@Override
		public String parseToken(Token token) {
			return token.value;
		}
	};

	public interface Acceptor<E> {
		public void accept(E value);
	}

	public static class StackCheck<E> {
		private final Calculator<E, ExprType> sut;

		public StackCheck(Calculator<E, ExprType> sut) {
			this.sut = sut;
		}

		public StackCheck<E> expectStack(E... values) {
			Assert.assertEquals(Arrays.asList(values), Lists.newArrayList(sut.environment.topFrame().stack()));
			return this;
		}

		public StackCheck<E> expectEmptyStack() {
			Assert.assertTrue(sut.environment.topFrame().stack().isEmpty());
			return this;
		}
	}

	public static class CalcCheck<E> {
		private final Calculator<E, ExprType> sut;

		private final IExecutable<E> expr;

		public CalcCheck(Calculator<E, ExprType> sut, IExecutable<E> expr) {
			this.expr = expr;
			this.sut = sut;
		}

		public CalcCheck<E> expectResult(E value) {
			final Frame<E> frame = sut.environment.executeIsolated(expr);
			final E top = frame.stack().pop();
			Assert.assertEquals(value, top);
			if (!frame.stack().isEmpty())
				Assert.fail("Extra values on stack: " + Lists.newArrayList(frame.stack()));

			return this;
		}

		public CalcCheck<E> expectResults(E... values) {
			final Frame<E> frame = sut.environment.executeIsolated(expr);
			Assert.assertEquals(Arrays.asList(values), Lists.newArrayList(frame.stack()));
			return this;
		}

		public StackCheck<E> execute() {
			sut.environment.execute(expr);
			return new StackCheck<E>(sut);
		}

		public E executeAndPop() {
			final Frame<E> frame = sut.environment.executeIsolated(expr);
			final E top = frame.stack().pop();
			if (!frame.stack().isEmpty())
				Assert.fail("Extra values on stack: " + Lists.newArrayList(frame.stack()));
			return top;
		}

		public Stack<E> executeAndGetStack() {
			final Frame<E> frame = sut.environment.executeIsolated(expr);
			return frame.stack();
		}

		public void expectThrow(Class<? extends Throwable> cls) {
			try {
				sut.environment.execute(expr);
			} catch (Throwable t) {
				if (!cls.isInstance(t)) {
					final AssertionError assertionError = new AssertionError("Expected " + cls);
					assertionError.initCause(t);
					throw assertionError;
				}
				return;
			}

			throw new AssertionError("Expected exception " + cls + ", got nothing");
		}

		public void expectThrow(Class<? extends Throwable> cls, String message) {
			try {
				sut.environment.execute(expr);
			} catch (Throwable t) {
				Assert.assertTrue("Expected " + cls + " got " + t, cls.isInstance(t));
				Assert.assertEquals(message, t.getMessage());
				return;
			}

			throw new AssertionError("Expected exception " + cls + ", got nothing");
		}

		public static <E> CalcCheck<E> create(Calculator<E, ExprType> sut, String value, ExprType exprType) {
			final IExecutable<E> expr = sut.compilers.compile(exprType, value);
			return new CalcCheck<E>(sut, expr);
		}

		public static <E> CalcCheck<E> create(Calculator<E, ExprType> sut, IExecutable<E> expr) {
			return new CalcCheck<E>(sut, expr);
		}

		public void expectSameAs(CalcCheck<E> other) {
			Assert.assertEquals(this.expr, other.expr);
		}
	}

	public static PeekingIterator<Token> tokenIterator(Token... inputs) {
		return Iterators.peekingIterator(Iterators.forArray(inputs));
	}

	public static class CompilerResultTester {
		private final List<IExecutable<String>> actual;
		private final ITokenStreamCompiler<String> compiler;

		@SuppressWarnings("unchecked")
		public CompilerResultTester(ITokenStreamCompiler<String> compiler, Token... inputs) {
			this.compiler = compiler;
			final IExecutable<String> result = compiler.compile(tokenIterator(inputs));
			if (result instanceof NoopExecutable) {
				this.actual = Lists.newArrayList();
			} else if (result instanceof ExecutableList) {
				this.actual = ((ExecutableList<String>)result).getCommands();
			} else {
				this.actual = Arrays.asList(result);
			}
		}

		public CompilerResultTester expectSameAs(Token... inputs) {
			final IExecutable<?> result = compiler.compile(tokenIterator(inputs));
			Assert.assertTrue(result instanceof ExecutableList);
			Assert.assertEquals(((ExecutableList<?>)result).getCommands(), actual);
			return this;
		}

		public CompilerResultTester expect(IExecutable<?>... expected) {
			Assert.assertEquals(Arrays.asList(expected), actual);
			return this;
		}
	}

	public static final IExecutable<String> CLOSE_QUOTE = rightBracketMarker(")");
	public static final IExecutable<String> OPEN_QUOTE = leftBracketMarker("(");
	public static final IExecutable<String> OPEN_ROOT_QUOTE_M = marker("<<" + TypedCalcConstants.MODIFIER_QUOTE);
	public static final IExecutable<String> CLOSE_ROOT_QUOTE_M = marker(TypedCalcConstants.MODIFIER_QUOTE + ">>");

	public static final IExecutable<String> OPEN_ROOT_QUOTE_S = marker("((" + TypedCalcConstants.SYMBOL_QUOTE);
	public static final IExecutable<String> CLOSE_ROOT_QUOTE_S = marker(TypedCalcConstants.SYMBOL_QUOTE + "))");

	public static IExecutable<String> leftBracketMarker(String value) {
		return marker("<" + value);
	}

	public static IExecutable<String> rightBracketMarker(String value) {
		return marker(value + ">");
	}

	public static IExecutable<String> valueMarker(String value) {
		return marker("value:" + value);
	}

	public static IExecutable<String> rawValueMarker(Token token) {
		return rawValueMarker(token.type, token.value);
	}

	public static IExecutable<String> rawValueMarker(TokenType type, String value) {
		return marker("raw:" + type + ":" + value);
	}

	public static class MarkerNode implements IExprNode<String> {
		private final IExecutable<String> value;

		public MarkerNode(IExecutable<String> value) {
			this.value = value;
		}

		@Override
		public void flatten(List<IExecutable<String>> output) {
			output.add(value);
		}

		@Override
		public Iterable<IExprNode<String>> getChildren() {
			return ImmutableList.of();
		}
	}

	public static class QuoteNodeTestFactory implements IQuotedExprNodeFactory<String> {

		@Override
		public IExprNode<String> createValueNode(String value) {
			return new MarkerNode(valueMarker(value));
		}

		@Override
		public IExprNode<String> createValueNode(Token token) {
			return new MarkerNode(token.type.isValue()? valueMarker(token.value) : rawValueMarker(token));
		}

		@Override
		public IExprNode<String> createBracketNode(final String openingBracket, final String closingBracket, final List<IExprNode<String>> children) {
			return new IExprNode<String>() {

				@Override
				public void flatten(List<IExecutable<String>> output) {
					output.add(leftBracketMarker(openingBracket));
					for (IExprNode<String> child : children)
						child.flatten(output);
					output.add(rightBracketMarker(closingBracket));
				}

				@Override
				public Iterable<IExprNode<String>> getChildren() {
					return children;
				}
			};
		}
	}

	public static class QuotedCompilerState implements ICompilerState<String> {
		private final QuotedParser<String> quotedParser = new QuotedParser<String>(VALUE_PARSER, new QuoteNodeTestFactory());

		@Override
		public IAstParser<String> getParser() {
			return quotedParser;
		}

		@Override
		public ISymbolCallStateTransition<String> getStateForSymbolCall(String symbol) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IModifierStateTransition<String> getStateForModifier(String modifier) {
			throw new UnsupportedOperationException();
		}

	}

	public static class ModifierQuoteTransition implements IModifierStateTransition<String> {
		private final String modifier;

		public ModifierQuoteTransition(String modifier) {
			this.modifier = modifier;
		}

		@Override
		public IExprNode<String> createRootNode(IExprNode<String> child) {
			return new DummyNode<String>(child) {
				@Override
				public void flatten(List<IExecutable<String>> output) {
					output.add(marker("<<" + modifier));
					super.flatten(output);
					output.add(marker(modifier + ">>"));
				}
			};
		}

		@Override
		public ICompilerState<String> getState() {
			return new QuotedCompilerState();
		}
	}

	public static class SymbolQuoteTransition implements ISymbolCallStateTransition<String> {
		private final String symbol;

		public SymbolQuoteTransition(String symbol) {
			this.symbol = symbol;
		}

		@Override
		public IExprNode<String> createRootNode(List<IExprNode<String>> children) {
			return new ContainerNode<String>(children) {
				@Override
				public void flatten(List<IExecutable<String>> output) {
					output.add(marker("((" + symbol));
					for (IExprNode<String> child : args)
						child.flatten(output);
					output.add(marker(symbol + "))"));
				}

			};
		}

		@Override
		public ICompilerState<String> getState() {
			return new QuotedCompilerState();
		}
	}

	public abstract static class TestCompilerState implements ICompilerState<String> {

		@Override
		public IModifierStateTransition<String> getStateForModifier(String modifier) {
			if (modifier.equals(QUOTE_MODIFIER.value)) return new ModifierQuoteTransition(modifier);
			throw new UnsupportedOperationException(modifier);
		}

		@Override
		public ISymbolCallStateTransition<String> getStateForSymbolCall(final String symbol) {
			if (symbol.equals(QUOTE_SYMBOL.value)) return new SymbolQuoteTransition(symbol);
			return new ISymbolCallStateTransition<String>() {
				@Override
				public ICompilerState<String> getState() {
					return TestCompilerState.this;
				}

				@Override
				public IExprNode<String> createRootNode(List<IExprNode<String>> children) {
					return new SymbolCallNode<String>(symbol, children);
				}
			};
		}
	}

	public static class SymbolStub<E> implements ISymbol<E> {
		private int callCount;
		private int getCount;

		private List<E> expectedArgs = Lists.newArrayList();
		private boolean exactArgCount = false;
		private List<E> returns = Lists.newArrayList();
		private E getValue;
		private boolean exactReturnCount = false;

		private boolean allowCalls = false;

		private boolean allowGets = false;

		public SymbolStub<E> expectArgs(E... args) {
			expectedArgs = Lists.reverse(Arrays.asList(args));
			return this;
		}

		public SymbolStub<E> verifyArgCount() {
			this.exactArgCount = true;
			return this;
		}

		public SymbolStub<E> setReturns(E... rets) {
			returns = Arrays.asList(rets);
			return this;
		}

		public SymbolStub<E> setGetValue(E value) {
			this.getValue = value;
			return this;
		}

		public SymbolStub<E> verifyReturnCount() {
			this.exactReturnCount = true;
			return this;
		}

		@Override
		public void call(Frame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			Assert.assertTrue(allowCalls);
			if (exactArgCount) Assert.assertEquals(Optional.of(expectedArgs.size()), argumentsCount);
			if (exactReturnCount) Assert.assertEquals(Optional.of(returns.size()), returnsCount);

			for (E expectedArg : expectedArgs)
				Assert.assertEquals(frame.stack().pop(), expectedArg);

			for (E ret : returns)
				frame.stack().push(ret);
			callCount++;
		}

		@Override
		public E get() {
			Assert.assertTrue(allowGets);
			getCount++;
			return getValue;
		}

		public SymbolStub<E> checkCallCount(int expectedCallCount) {
			Assert.assertEquals(expectedCallCount, this.callCount);
			return this;
		}

		public SymbolStub<E> resetCallCount() {
			this.callCount = 0;
			return this;
		}

		public SymbolStub<E> allowCalls() {
			this.allowCalls = true;
			return this;
		}

		public SymbolStub<E> checkGetCount(int expectedGetCount) {
			Assert.assertEquals(expectedGetCount, this.getCount);
			return this;
		}

		public SymbolStub<E> resetGetCount() {
			this.getCount = 0;
			return this;
		}

		public SymbolStub<E> allowGets() {
			this.allowGets = true;
			return this;
		}

	}

	public static class CallableStub<E> implements ICallable<E> {
		private int callCount;

		private List<E> expectedArgs = Lists.newArrayList();
		private boolean exactArgCount = false;
		private List<E> returns = Lists.newArrayList();
		private boolean exactReturnCount = false;

		public CallableStub<E> expectArgs(E... args) {
			expectedArgs = Lists.reverse(Arrays.asList(args));
			return this;
		}

		public CallableStub<E> verifyArgCount() {
			this.exactArgCount = true;
			return this;
		}

		public CallableStub<E> setReturns(E... rets) {
			returns = Arrays.asList(rets);
			return this;
		}

		public CallableStub<E> verifyReturnCount() {
			this.exactReturnCount = true;
			return this;
		}

		@Override
		public void call(Frame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			if (exactArgCount) Assert.assertEquals(Optional.of(expectedArgs.size()), argumentsCount);
			if (exactReturnCount) Assert.assertEquals(Optional.of(returns.size()), returnsCount);

			for (E expectedArg : expectedArgs)
				Assert.assertEquals(frame.stack().pop(), expectedArg);

			for (E ret : returns)
				frame.stack().push(ret);
			callCount++;
		}

		public CallableStub<E> checkCallCount(int expectedCallCount) {
			Assert.assertEquals(expectedCallCount, this.callCount);
			return this;
		}

		public CallableStub<E> resetCallCount() {
			this.callCount = 0;
			return this;
		}

	}
}
