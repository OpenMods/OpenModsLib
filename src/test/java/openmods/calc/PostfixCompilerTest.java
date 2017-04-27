package openmods.calc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import openmods.calc.parsing.postfix.IExecutableListBuilder;
import openmods.calc.parsing.postfix.IPostfixParserState;
import openmods.calc.parsing.postfix.MappedPostfixParser;
import openmods.calc.parsing.postfix.PostfixParser;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;
import openmods.utils.OptionalInt;
import openmods.utils.StackUnderflowException;
import org.junit.Assert;
import org.junit.Test;

public class PostfixCompilerTest extends CalcTestUtils {

	private static final String TEST_MODIFIER = "!!!";

	private static class CompileResult {
		public final String type;

		public final String value;

		public CompileResult(String type, String value) {
			this.type = type;
			this.value = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((type == null)? 0 : type.hashCode());
			result = prime * result + ((value == null)? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;

			if (obj instanceof CompileResult) {
				final CompileResult other = (CompileResult)obj;
				return other.type.equals(this.type) &&
						other.value.equals(this.value);
			}

			return false;
		}

		@Override
		public String toString() {
			return type + ":" + value;
		}
	}

	private static CompileResult modifiedValue(Token token) {
		return new CompileResult("modified", token.type + ":" + token.value);
	}

	private static CompileResult value(Token token) {
		Assert.assertTrue(token.type.isValue());
		return new CompileResult("value", token.type + ":" + token.value);
	}

	private static CompileResult valueDec(String v) {
		return value(dec(v));
	}

	private static CompileResult operator(Token op) {
		Assert.assertEquals(TokenType.OPERATOR, op.type);
		return new CompileResult("operator", op.value);
	}

	private static CompileResult operator(String op) {
		return new CompileResult("operator", op);
	}

	private static CompileResult symbolGet(String id) {
		return new CompileResult("get", id);
	}

	private static CompileResult symbolCall(String id, OptionalInt args, OptionalInt rets) {
		return new CompileResult("callExact", id + ":" + args + ":" + rets);
	}

	private static CompileResult symbolCall(String id) {
		return symbolCall(id, OptionalInt.ABSENT, OptionalInt.ABSENT);
	}

	private static final CompileResult OPEN_BRACKET = new CompileResult("bracket", "(");

	private static final CompileResult CLOSE_BRACKET = new CompileResult("bracket", ")");

	private static final CompileResult SUBLIST_START = new CompileResult("list", "<");

	private static final CompileResult SUBLIST_END = new CompileResult("list", ">");

	private static class TestModifierState implements IPostfixParserState<List<CompileResult>> {
		private CompileResult result;

		@Override
		public Result acceptToken(Token token) {
			Preconditions.checkState(result == null);
			result = modifiedValue(token);
			return Result.ACCEPTED_AND_FINISHED;
		}

		@Override
		public Result acceptChildResult(List<CompileResult> executable) {
			return Result.REJECTED;
		}

		@Override
		public List<CompileResult> getResult() {
			Preconditions.checkState(result != null);
			return Lists.newArrayList(result);
		}
	}

	private static class TestBracketState implements IPostfixParserState<List<CompileResult>> {
		private final List<CompileResult> result = Lists.newArrayList();

		{
			result.add(OPEN_BRACKET);
		}

		@Override
		public Result acceptToken(Token token) {
			if (token.type == TokenType.RIGHT_BRACKET) return Result.ACCEPTED_AND_FINISHED;
			result.add(value(token));
			return Result.ACCEPTED;
		}

		@Override
		public Result acceptChildResult(List<CompileResult> executable) {
			result.add(SUBLIST_START);
			result.addAll(executable);
			result.add(SUBLIST_END);
			return Result.ACCEPTED;
		}

		@Override
		public List<CompileResult> getResult() {
			result.add(CLOSE_BRACKET);
			return result;
		}
	}

	private static class TestCompileResultCollector implements IExecutableListBuilder<List<CompileResult>> {

		private final List<CompileResult> result = Lists.newArrayList();

		@Override
		public void appendValue(Token value) {
			result.add(value(value));
		}

		@Override
		public void appendOperator(String id) {
			result.add(operator(id));
		}

		@Override
		public void appendSymbolGet(String id) {
			result.add(symbolGet(id));
		}

		@Override
		public void appendSymbolCall(String id, OptionalInt argCount, OptionalInt returnCount) {
			result.add(symbolCall(id, argCount, returnCount));
		}

		@Override
		public void appendSubList(List<CompileResult> subList) {
			result.add(SUBLIST_START);
			result.addAll(subList);
			result.add(SUBLIST_END);
		}

		@Override
		public List<CompileResult> build() {
			return result;
		}

	}

	private static PostfixParser<List<CompileResult>> createTestTarget() {
		return new MappedPostfixParser<List<CompileResult>>() {

			@Override
			protected IPostfixParserState<List<CompileResult>> createStateForModifier(String modifier) {
				if (modifier.equals(TEST_MODIFIER))
					return new TestModifierState();
				else
					return super.createStateForModifier(modifier);
			}

			@Override
			protected IPostfixParserState<List<CompileResult>> createStateForBracket(String modifier) {
				return new TestBracketState();
			}

			@Override
			protected IExecutableListBuilder<List<CompileResult>> createListBuilder() {
				return new TestCompileResultCollector();
			}
		};
	}

	private static class ParserResultTester {
		private final List<CompileResult> actual;

		public ParserResultTester(PostfixParser<List<CompileResult>> parser, Token... inputs) {
			this.actual = parser.parse(tokenIterator(inputs));
		}

		public ParserResultTester expect(CompileResult... expected) {
			Assert.assertEquals(Arrays.asList(expected), actual);
			return this;
		}
	}

	private static ParserResultTester given(Token... inputs) {
		return new ParserResultTester(createTestTarget(), inputs);
	}

	@Test
	public void testSingleValue() {
		given(dec("1")).expect(value(dec("1")));
		given(oct("2")).expect(value(oct("2")));
		given(hex("3")).expect(value(hex("3")));
		given(bin("10")).expect(value(bin("10")));
		given(quoted("10")).expect(value(quoted("10")));
	}

	@Test
	public void testSingleOp() {
		given(OP_PLUS).expect(operator(OP_PLUS));
	}

	@Test
	public void testSymbol() {
		given(symbol("a")).expect(symbolCall("a"));
	}

	@Test
	public void testModifier() {
		given(mod(TEST_MODIFIER), dec("3")).expect(SUBLIST_START, modifiedValue(dec("3")), SUBLIST_END);
		given(mod(TEST_MODIFIER), symbol("a")).expect(SUBLIST_START, modifiedValue(symbol("a")), SUBLIST_END);

		given(dec("1"), mod(TEST_MODIFIER), symbol("a"), symbol("b")).expect(value(dec("1")), SUBLIST_START, modifiedValue(symbol("a")), SUBLIST_END, symbolCall("b"));
	}

	@Test
	public void testBrackets() {
		given(LEFT_BRACKET, RIGHT_BRACKET).expect(SUBLIST_START, OPEN_BRACKET, CLOSE_BRACKET, SUBLIST_END);
		given(LEFT_BRACKET, dec("3"), RIGHT_BRACKET).expect(SUBLIST_START, OPEN_BRACKET, valueDec("3"), CLOSE_BRACKET, SUBLIST_END);
		given(LEFT_BRACKET, dec("3"), dec("4"), RIGHT_BRACKET).expect(SUBLIST_START, OPEN_BRACKET, valueDec("3"), valueDec("4"), CLOSE_BRACKET, SUBLIST_END);

		given(dec("1"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, dec("3")).expect(valueDec("1"), SUBLIST_START, OPEN_BRACKET, valueDec("2"), CLOSE_BRACKET, SUBLIST_END, valueDec("3"));
		given(dec("1"), LEFT_BRACKET, dec("2"), dec("3"), RIGHT_BRACKET, dec("4")).expect(valueDec("1"), SUBLIST_START, OPEN_BRACKET, valueDec("2"), valueDec("3"), CLOSE_BRACKET, SUBLIST_END, valueDec("4"));
	}

	@Test
	public void testNestedBracketFlattening() {
		given(LEFT_BRACKET, LEFT_BRACKET, dec("4"), RIGHT_BRACKET, RIGHT_BRACKET).expect(SUBLIST_START, OPEN_BRACKET, SUBLIST_START, OPEN_BRACKET, valueDec("4"), CLOSE_BRACKET, SUBLIST_END, CLOSE_BRACKET, SUBLIST_END);
		given(LEFT_BRACKET, LEFT_BRACKET, LEFT_BRACKET, dec("4"), RIGHT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET).expect(SUBLIST_START, OPEN_BRACKET, SUBLIST_START, OPEN_BRACKET, SUBLIST_START, OPEN_BRACKET, valueDec("4"), CLOSE_BRACKET, SUBLIST_END, CLOSE_BRACKET, SUBLIST_END, CLOSE_BRACKET, SUBLIST_END);

		given(LEFT_BRACKET, dec("3"), LEFT_BRACKET, dec("4"), RIGHT_BRACKET, RIGHT_BRACKET).expect(SUBLIST_START, OPEN_BRACKET, valueDec("3"), SUBLIST_START, OPEN_BRACKET, valueDec("4"), CLOSE_BRACKET, SUBLIST_END, CLOSE_BRACKET, SUBLIST_END);
	}

	@Test(expected = StackUnderflowException.class)
	public void testUnclosedBrackers() {
		given(LEFT_BRACKET, dec("4"));
	}

	@Test
	public void testSingleExpr() {
		given(symbol("a"), dec("3"), OP_PLUS).expect(symbolCall("a"), valueDec("3"), operator(OP_PLUS));
	}

	@Test
	public void testSymbolWithArgs() {
		given(symbol_args("a$2")).expect(symbolCall("a", OptionalInt.of(2), OptionalInt.absent()));
		given(symbol_args("a$2,")).expect(symbolCall("a", OptionalInt.of(2), OptionalInt.absent()));
		given(symbol_args("a$,3")).expect(symbolCall("a", OptionalInt.absent(), OptionalInt.of(3)));
		given(symbol_args("a$,")).expect(symbolCall("a", OptionalInt.absent(), OptionalInt.absent()));
		given(symbol_args("b$3,4")).expect(symbolCall("b", OptionalInt.of(3), OptionalInt.of(4)));
		given(symbol_args("b$35,45")).expect(symbolCall("b", OptionalInt.of(35), OptionalInt.of(45)));
	}

}
