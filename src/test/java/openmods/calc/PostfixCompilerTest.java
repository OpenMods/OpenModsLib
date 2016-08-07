package openmods.calc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.parsing.DefaultPostfixCompiler;
import openmods.calc.parsing.IPostfixCompilerState;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;
import org.junit.Test;

public class PostfixCompilerTest extends CalcTestUtils {

	public final OperatorDictionary<String> operators = new OperatorDictionary<String>();
	{
		operators.registerBinaryOperator(PLUS);
		operators.registerUnaryOperator(UNARY_MINUS);
	}

	private static final String TEST_MODIFIER = "!!!";

	private static String rawTokenString(Token token) {
		return TEST_MODIFIER + ":" + token.type + ":" + token.value;
	}

	private static class TestModifierState implements IPostfixCompilerState<String> {
		private IExecutable<String> result;

		@Override
		public Result acceptToken(Token token) {
			Preconditions.checkState(result == null);
			result = Value.create(rawTokenString(token));
			return Result.ACCEPTED_AND_FINISHED;
		}

		@Override
		public Result acceptExecutable(IExecutable<String> executable) {
			return Result.REJECTED;
		}

		@Override
		public IExecutable<String> exit() {
			Preconditions.checkState(result != null);
			return result;
		}
	}

	private static final IExecutable<String> OPEN_BRACKET = marker("((");
	private static final IExecutable<String> CLOSE_BRACKET = marker("))");

	private static class TestBracketState implements IPostfixCompilerState<String> {
		private final List<IExecutable<String>> result = Lists.newArrayList();

		{
			result.add(OPEN_BRACKET);
		}

		@Override
		public Result acceptToken(Token token) {
			if (token.type == TokenType.RIGHT_BRACKET) return Result.ACCEPTED_AND_FINISHED;
			result.add(Value.create(token.value));
			return Result.ACCEPTED;
		}

		@Override
		public Result acceptExecutable(IExecutable<String> executable) {
			result.add(executable);
			return Result.ACCEPTED;
		}

		@Override
		public IExecutable<String> exit() {
			result.add(CLOSE_BRACKET);
			return new ExecutableList<String>(result);
		}

	}

	private CompilerResultTester given(Token... inputs) {
		return new CompilerResultTester(new DefaultPostfixCompiler<String>(VALUE_PARSER, operators) {

			@Override
			protected IPostfixCompilerState<String> createStateForModifier(String modifier) {
				if (modifier.equals(TEST_MODIFIER))
					return new TestModifierState();
				else
					return super.createStateForModifier(modifier);
			}

			@Override
			protected IPostfixCompilerState<String> createStateForBracket(String modifier) {
				return new TestBracketState();
			}
		}, inputs);
	}

	@Test
	public void testSinglValue() {
		given(dec("1")).expect(c("1"));
		given(oct("2")).expect(c("2"));
		given(hex("3")).expect(c("3"));
		given(bin("10")).expect(c("10"));
		given(quoted("10")).expect(c("10"));
	}

	@Test
	public void testSingleOp() {
		given(OP_PLUS).expect(PLUS);
	}

	@Test
	public void testSymbol() {
		given(symbol("a")).expect(s("a"));
	}

	@Test
	public void testModifier() {
		given(mod(TEST_MODIFIER), dec("3")).expect(c(rawTokenString(dec("3"))));
		given(mod(TEST_MODIFIER), symbol("a")).expect(c(rawTokenString(symbol("a"))));

		given(dec("1"), mod(TEST_MODIFIER), symbol("a"), symbol("b")).expect(c("1"), c(rawTokenString(symbol("a"))), s("b"));
	}

	@Test
	public void testBrackets() {
		given(LEFT_BRACKET, RIGHT_BRACKET).expect(OPEN_BRACKET, CLOSE_BRACKET);
		given(LEFT_BRACKET, dec("3"), RIGHT_BRACKET).expect(OPEN_BRACKET, c("3"), CLOSE_BRACKET);
		given(LEFT_BRACKET, dec("3"), dec("4"), RIGHT_BRACKET).expect(OPEN_BRACKET, c("3"), c("4"), CLOSE_BRACKET);

		given(dec("1"), LEFT_BRACKET, dec("2"), RIGHT_BRACKET, dec("3")).expect(c("1"), OPEN_BRACKET, c("2"), CLOSE_BRACKET, c("3"));
		given(dec("1"), LEFT_BRACKET, dec("2"), dec("3"), RIGHT_BRACKET, dec("4")).expect(c("1"), OPEN_BRACKET, c("2"), c("3"), CLOSE_BRACKET, c("4"));
	}

	@Test
	public void testNestedBracketFlattening() {
		given(LEFT_BRACKET, LEFT_BRACKET, dec("4"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_BRACKET, OPEN_BRACKET, c("4"), CLOSE_BRACKET, CLOSE_BRACKET);
		given(LEFT_BRACKET, LEFT_BRACKET, LEFT_BRACKET, dec("4"), RIGHT_BRACKET, RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_BRACKET, OPEN_BRACKET, OPEN_BRACKET, c("4"), CLOSE_BRACKET, CLOSE_BRACKET, CLOSE_BRACKET);

		given(LEFT_BRACKET, dec("3"), LEFT_BRACKET, dec("4"), RIGHT_BRACKET, RIGHT_BRACKET).expect(OPEN_BRACKET, c("3"), OPEN_BRACKET, c("4"), CLOSE_BRACKET, CLOSE_BRACKET);
	}

	@Test(expected = IllegalStateException.class)
	public void testUnclosedBrackers() {
		given(LEFT_BRACKET, dec("4"));
	}

	@Test
	public void testSingleExpr() {
		given(symbol("a"), dec("3"), OP_PLUS).expect(s("a"), c("3"), PLUS);
	}

	@Test
	public void testSymbolWithArgs() {
		given(symbol_args("a@2")).expect(s("a").setArgumentsCount(2));
		given(symbol_args("a@2,")).expect(s("a").setArgumentsCount(2));
		given(symbol_args("a@,3")).expect(s("a").setReturnsCount(3));
		given(symbol_args("a@,")).expect(s("a"));
		given(symbol_args("b@3,4")).expect(s("b").setArgumentsCount(3).setReturnsCount(4));
		given(symbol_args("b@35,45")).expect(s("b").setArgumentsCount(35).setReturnsCount(45));
	}

}
