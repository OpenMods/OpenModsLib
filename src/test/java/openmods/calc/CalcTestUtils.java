package openmods.calc;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.Arrays;
import java.util.List;
import openmods.calc.executable.IExecutable;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;
import openmods.calc.symbol.ICallable;
import openmods.calc.symbol.ISymbol;
import openmods.calc.types.multi.TypedCalcConstants;
import openmods.utils.OptionalInt;
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

	public static final Token OP_PLUS = op("+");

	public static final Token OP_MULTIPLY = op("*");
	public static final Token OP_MINUS = op("-");

	public static final Token OP_NEG = op("!");

	public static final Token OP_DEFAULT = op("<*>");

	public static final Token OP_ASSIGN = op("=");

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

		public CalcCheck<E> expectSameAs(CalcCheck<E> other) {
			Assert.assertEquals(this.expr, other.expr);
			return this;
		}
	}

	public static PeekingIterator<Token> tokenIterator(Token... inputs) {
		return Iterators.peekingIterator(Iterators.forArray(inputs));
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
		public void call(Frame<E> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			Assert.assertTrue(allowCalls);
			if (exactArgCount) Assert.assertEquals(OptionalInt.of(expectedArgs.size()), argumentsCount);
			if (exactReturnCount) Assert.assertEquals(OptionalInt.of(returns.size()), returnsCount);

			for (E expectedArg : expectedArgs)
				Assert.assertEquals(frame.stack().pop(), expectedArg);

			frame.stack().pushAll(returns);
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
		public void call(Frame<E> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			if (exactArgCount) Assert.assertEquals(OptionalInt.of(expectedArgs.size()), argumentsCount);
			if (exactReturnCount) Assert.assertEquals(OptionalInt.of(returns.size()), returnsCount);

			for (E expectedArg : expectedArgs)
				Assert.assertEquals(frame.stack().pop(), expectedArg);

			frame.stack().pushAll(returns);
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
