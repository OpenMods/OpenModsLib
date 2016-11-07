package openmods.calc;

import openmods.calc.CalcTestUtils.CalcCheck;
import openmods.calc.CalcTestUtils.SymbolStub;
import openmods.calc.types.fp.DoubleCalculatorFactory;
import org.junit.Test;

public class DoubleCalculatorTest {

	private final Calculator<Double, ExprType> sut = DoubleCalculatorFactory.createDefault();

	public CalcCheck<Double> prefix(String value) {
		return CalcCheck.create(sut, value, ExprType.PREFIX);
	}

	public CalcCheck<Double> infix(String value) {
		return CalcCheck.create(sut, value, ExprType.INFIX);
	}

	public CalcCheck<Double> postfix(String value) {
		return CalcCheck.create(sut, value, ExprType.POSTFIX);
	}

	public CalcCheck<Double> compiled(IExecutable<Double> expr) {
		return CalcCheck.create(sut, expr);
	}

	@Test
	public void testBasicPrefix() {
		prefix("(+ 1 2)").expectResult(3.0);
		prefix("(* 2 3)").expectResult(6.0);
		prefix("(- 1)").expectResult(-1.0);
		prefix("(* (- 1) (+ 2 3))").expectResult(-5.0);
		prefix("(/ 10 2)").expectResult(5.0);
		prefix("(^ 2 5)").expectResult(32.0);

		prefix("(max 1)").expectResult(1.0);
		prefix("(max 1 2)").expectResult(2.0);
		prefix("(max 1 2 3)").expectResult(3.0);
	}

	@Test
	public void testBasicPostfix() {
		postfix("1 2 +").expectResult(3.0);
		postfix("0.5 0.5 +").expectResult(1.0);
		postfix("0.25 0.25 +").expectResult(0.5);
		postfix("2 3 *").expectResult(6.0);
		postfix("10 2 /").expectResult(5.0);
		postfix("2 5 ^").expectResult(32.0);
	}

	@Test
	public void testPostfixSymbols() {
		sut.environment.setGlobalSymbol("a", 5.0);
		postfix("a").expectResult(5.0);
		postfix("a$0").expectResult(5.0);
		postfix("a$,1").expectResult(5.0);
		postfix("a$0,1").expectResult(5.0);
		postfix("@a").expectResult(5.0);
	}

	@Test(expected = RuntimeException.class)
	public void testPostfixFunctionGet() {
		postfix("@abs").execute();
	}

	@Test
	public void testPostfixStackOperations() {
		postfix("2 dup +").expectResult(4.0);
		postfix("1 2 3 pop +").expectResult(3.0);
		postfix("2 3 swap -").expectResult(1.0);
	}

	@Test
	public void testPostfixDupWithArgs() {
		postfix("0 1 2 dup$2").expectResults(0.0, 1.0, 2.0, 1.0, 2.0);
	}

	@Test
	public void testPostfixDupWithReturns() {
		postfix("1 2 dup$,4").expectResults(1.0, 2.0, 2.0, 2.0, 2.0);
	}

	@Test
	public void testPostfixDupWithArgsAndReturns() {
		postfix("1 2 3 4 dup$3,5").expectResults(1.0, 2.0, 3.0, 4.0, 2.0, 3.0);
	}

	@Test
	public void testPostfixPopWithArgs() {
		postfix("1 2 3 4 pop$3").expectResults(1.0);
	}

	@Test
	public void testVariadicPostfixFunctions() {
		postfix("max$0").expectResult(0.0);
		postfix("1 max$1").expectResult(1.0);
		postfix("1 2 max$2").expectResult(2.0);

		postfix("3 2 1 sum$3").expectResult(6.0);
		postfix("3 2 1 avg$3").expectResult(2.0);

		postfix("2 4 INF 1 max$4").expectResult(Double.POSITIVE_INFINITY);
	}

	@Test
	public void testBasicInfix() {
		infix("1 + 2").expectResult(3.0);
		infix("2 * 3").expectResult(6.0);
		infix("10 / 2").expectResult(5.0);
		infix("2 ^ 5").expectResult(32.0);
		infix("-PI").expectResult(-Math.PI);
		infix("2*E").expectResult(2 * Math.E);
		infix("2*-E").expectResult(2 * -Math.E);
		infix("2E").expectResult(2 * Math.E);
		infix("2(E)").expectResult(2 * Math.E);
		infix("-2*-3*10^3").expectResult(6e3);
		infix("-2*-3*10^+3").expectResult(6e3);
		infix("-2*-3*10^-3").expectResult(6e-3);
		infix("2*10^2+3*10^3").expectResult(3200.0);
		infix("2*10^2*3*10^3").expectResult(6e5);
	}

	@Test
	public void testCallOfGetterSymbol() {
		infix("PI()").expectResult(Math.PI);
	}

	@Test
	public void testBasicOrdering() {
		infix("1 + 2 - 3").expectResult(0.0);

		infix("1 + 2 * 3").expectResult(7.0);
		infix("1 + (2 * 3)").expectResult(7.0);
		infix("(1 + 2) * 3").expectResult(9.0);
		infix("-(1 + 2) * 3").expectResult(-9.0);
		infix("(1 + 2) * -3").expectResult(-9.0);
		infix("--3").expectResult(3.0);

		infix("2 * 2 ^ 2").expectResult(8.0);
		infix("2 * (2 ^ 2)").expectResult(8.0);
		infix("(2 * 2) ^ 2").expectResult(16.0);
	}

	@Test
	public void testBasicInfixFunctions() {
		infix("sin(0)").expectResult(0.0);
		infix("cos(0)").expectResult(1.0);
		infix("1+cos(0)").expectResult(2.0);
		infix("5cos(0)").expectResult(5.0);
		infix("min(2,3)").expectResult(2.0);
		infix("max(2,3)").expectResult(3.0);
		infix("2-max(2,3)").expectResult(-1.0);
	}

	@Test
	public void testVariadicInfixFunctions() {
		infix("max()").expectResult(0.0);
		infix("max(1)").expectResult(1.0);
		infix("max(1,2)").expectResult(2.0);
		infix("max(3,2,1)").expectResult(3.0);
		infix("max(2,4,INF,1)").expectResult(Double.POSITIVE_INFINITY);
	}

	@Test(expected = Exception.class)
	public void testTooManyParameters() {
		infix("sin(0, 1)").execute();
	}

	@Test(expected = Exception.class)
	public void testTooFewParameters() {
		infix("atan2(0)").execute();
	}

	@Test
	public void testParserSwitch() {
		infix("2 + prefix(5)").expectResult(7.0);
		infix("2 + prefix((+ 5 6))").expectResult(13.0);

		prefix("(+ 2 (infix 5))").expectResult(7.0);
		prefix("(+ 2 (infix 5 + 6))").expectResult(13.0);
	}

	@Test
	public void testNestedParserSwitch() {
		infix("infix(5 + 2)").expectResult(7.0);
		infix("infix(infix(5 + 2))").expectResult(7.0);

		prefix("(prefix (+ 2 5))").expectResult(7.0);
		prefix("(prefix (prefix (+ 2 5)))").expectResult(7.0);

		infix("prefix((infix 2 + 5))").expectResult(7.0);
		prefix("(infix prefix((+ 2 5)))").expectResult(7.0);
	}

	@Test
	public void testConstantEvaluatingBrackets() {
		final SymbolStub<Double> stub = new SymbolStub<Double>()
				.allowCalls()
				.expectArgs(1.0, 2.0)
				.verifyArgCount()
				.setReturns(5.0, 6.0, 7.0)
				.verifyReturnCount();
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<Double> expr = sut.compilers.compile(ExprType.POSTFIX, "[1 2 dummy$2,3]");
		stub.checkCallCount(1);
		compiled(expr).expectResults(5.0, 6.0, 7.0);
		stub.checkCallCount(1);
	}

	@Test
	public void testNestedConstantEvaluatingBrackets() {
		final SymbolStub<Double> stub = new SymbolStub<Double>()
				.allowGets()
				.setGetValue(5.0);
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<Double> expr = sut.compilers.compile(ExprType.POSTFIX, "[4 [@dummy 3 -] *]");
		stub.checkGetCount(1);
		compiled(expr).expectResults(8.0);
		stub.checkGetCount(1);
	}

	@Test
	public void testSimpleLet() {
		infix("let([x:2,y:3], x + y)").expectResult(5.0);
		prefix("(let [(:x 2) (:y 3)] (+ x y))").expectResult(5.0);
	}

	@Test
	public void testLetWithExpression() {
		infix("let([x:1 + 2,y:3 + 4], x + y)").expectResult(10.0);
		prefix("(let [(:x (+ 1 2)) (:y (+ 3 4))] (+ x y))").expectResult(10.0);
	}

	@Test
	public void testNestedLet() {
		infix("let([x:2,y:3], let([w:x+y,z:x-y], w + z))").expectResult(4.0);
		prefix("(let [(:x 2) (:y 3)] (let [(:w (+ x y)) (:z (- x y))] (+ w z)))").expectResult(4.0);
	}

	@Test
	public void testFunctionLet() {
		infix("let([x():2], x())").expectResult(2.0);
		prefix("(let [(:(x) 2)] (x))").expectResult(2.0);

		infix("let([x(a,b):a+b], x(1,2))").expectResult(3.0);
		prefix("(let [(: (x a b) (+ a b))], (x 1 2))").expectResult(3.0);
	}

	@Test
	public void testLetScoping() {
		infix("let([x:2], let([y:x], let([x:3], y)))").expectResult(2.0);
		infix("let([x:2], let([f(a):a+x], let([x:3], f(4))))").expectResult(6.0);
		infix("let([x:5], let([x:2, y:x], x + y))").expectResult(7.0);
	}

	@Test
	public void testFailSymbol() {
		infix("fail('welp')").expectThrow(ExecutionErrorException.class, "welp");
		infix("fail()").expectThrow(ExecutionErrorException.class, null);
	}
}
