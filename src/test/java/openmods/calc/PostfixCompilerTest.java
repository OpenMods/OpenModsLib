package openmods.calc;

import com.google.common.collect.PeekingIterator;
import openmods.calc.parsing.DefaultPostfixCompiler;
import openmods.calc.parsing.IExecutableListBuilder;
import openmods.calc.parsing.Token;
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

	private CompilerResultTester given(Token... inputs) {
		return new CompilerResultTester(new DefaultPostfixCompiler<String>(VALUE_PARSER, operators) {

			@Override
			protected void parseModifier(String modifier, PeekingIterator<Token> input, IExecutableListBuilder<String> output) {
				if (modifier.equals(TEST_MODIFIER)) {
					output.appendValue(rawTokenString(input.next()));
				} else {
					super.parseModifier(modifier, input, output);
				}
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
		given(mod(TEST_MODIFIER), mod("#")).expect(c(rawTokenString(mod("#"))));

		given(dec("1"), mod(TEST_MODIFIER), symbol("a"), symbol("b")).expect(c("1"), c(rawTokenString(symbol("a"))), s("b"));
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
