package openmods.calc;

import openmods.calc.CalcTestUtils.CalcCheck;
import openmods.calc.CalcTestUtils.SymbolStub;
import openmods.calc.executable.IExecutable;
import openmods.calc.types.bool.BoolCalculatorFactory;
import org.junit.Test;

public class BoolCalculatorTest {

	private final Calculator<Boolean, ExprType> sut = BoolCalculatorFactory.createDefault();

	public CalcCheck<Boolean> prefix(String value) {
		return CalcCheck.create(sut, value, ExprType.PREFIX);
	}

	public CalcCheck<Boolean> infix(String value) {
		return CalcCheck.create(sut, value, ExprType.INFIX);
	}

	public CalcCheck<Boolean> postfix(String value) {
		return CalcCheck.create(sut, value, ExprType.POSTFIX);
	}

	public CalcCheck<Boolean> compiled(IExecutable<Boolean> expr) {
		return CalcCheck.create(sut, expr);
	}

	@Test
	public void testBasicPrefix() {
		prefix("(& true false)").expectResult(false);
		prefix("(& 1 0)").expectResult(false);

		prefix("(and true false)").expectResult(false);
		prefix("(and 1 0)").expectResult(false);

		prefix("(| true false)").expectResult(true);
		prefix("(| 1 0)").expectResult(true);
		prefix("(or true false)").expectResult(true);
		prefix("(or 1 0)").expectResult(true);

	}

	@Test
	public void testBasicPostfix() {
		postfix("true false &").expectResult(false);
		postfix("1 0 &").expectResult(false);

		postfix("true false and").expectResult(false);
		postfix("1 0 and").expectResult(false);

		postfix("true false |").expectResult(true);
		postfix("1 0 |").expectResult(true);
		postfix("true false or").expectResult(true);
		postfix("1 0 or").expectResult(true);
	}

	@Test
	public void testPostfixSymbols() {
		sut.environment.setGlobalSymbol("a", false);
		postfix("a").expectResult(false);
		postfix("a$0").expectResult(false);
		postfix("a$,1").expectResult(false);
		postfix("a$0,1").expectResult(false);
		postfix("@a").expectResult(false);
	}

	@Test(expected = RuntimeException.class)
	public void testPostfixFunctionGet() {
		postfix("@abs").execute();
	}

	@Test
	public void testPostfixStackOperations() {
		postfix("true dup &").expectResult(true);
		postfix("false dup |").expectResult(false);

		postfix("true false false pop ^").expectResult(true);
		postfix("true false =>").expectResult(false);
		postfix("true false swap =>").expectResult(true);
	}

	@Test
	public void testPostfixDupWithArgs() {
		postfix("true true false dup$2").expectResults(true, true, false, true, false);
	}

	@Test
	public void testPostfixDupWithReturns() {
		postfix("true false dup$,4").expectResults(true, false, false, false, false);
	}

	@Test
	public void testPostfixPopWithArgs() {
		postfix("true false false true pop$3").expectResults(true);
	}

	@Test
	public void testBasicInfix() {
		infix("true & false").expectResult(false);
		infix("1 & 0").expectResult(false);

		infix("true and false").expectResult(false);
		infix("1 and 0").expectResult(false);

		infix("true | false").expectResult(true);
		infix("1 | 0").expectResult(true);
		infix("true or false").expectResult(true);
		infix("1 or 0").expectResult(true);
	}

	@Test
	public void testParserSwitch() {
		infix("true & prefix(false)").expectResult(false);
		infix("true & prefix((| false true))").expectResult(true);

		prefix("(& true (infix false))").expectResult(false);
		prefix("(& true (infix false | true))").expectResult(true);
	}

	@Test
	public void testConstantEvaluatingBrackets() {
		final SymbolStub<Boolean> stub = new SymbolStub<Boolean>()
				.allowCalls()
				.expectArgs(true, false)
				.verifyArgCount()
				.setReturns(false, false, true)
				.verifyReturnCount();
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<Boolean> expr = sut.compilers.compile(ExprType.POSTFIX, "[true false dummy$2,3]");
		stub.checkCallCount(1);
		compiled(expr).expectResults(false, false, true);
		stub.checkCallCount(1);
	}

	@Test
	public void testConstantEvaluatingSymbol() {
		final SymbolStub<Boolean> stub = new SymbolStub<Boolean>()
				.allowCalls()
				.expectArgs(false, true)
				.verifyArgCount()
				.setReturns(true)
				.verifyReturnCount();
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<Boolean> expr = sut.compilers.compile(ExprType.INFIX, "const(dummy(false, true) => false)");
		stub.checkCallCount(1);

		compiled(expr).expectResults(false);
		stub.checkCallCount(1);

		compiled(expr).expectResults(false);
		stub.checkCallCount(1);
	}

	@Test
	public void testSimpleLet() {
		infix("let([x:true,y:false], x | y)").expectResult(true);
		prefix("(let [(:x false) (:y false)] (| x y))").expectResult(false);
	}

	@Test
	public void testFailSymbol() {
		infix("fail('welp')").expectThrow(ExecutionErrorException.class, "welp");
		infix("fail()").expectThrow(ExecutionErrorException.class, null);
	}
}
