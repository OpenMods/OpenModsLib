package openmods.calc;

import openmods.calc.parsing.PostfixCompiler;
import openmods.calc.parsing.Token;
import org.junit.Test;

public class PostfixCompilerTest extends CalcTestUtils {

	public final OperatorDictionary<String> operators = new OperatorDictionary<String>();
	{
		operators.registerBinaryOperator(PLUS);
		operators.registerUnaryOperator(UNARY_MINUS);
	}

	private CompilerResultTester given(Token... inputs) {
		return new CompilerResultTester(new PostfixCompiler<String>(VALUE_PARSER, operators), inputs);
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
