package openmods.calc;

import java.math.BigInteger;
import openmods.calc.CalcTestUtils.CalcCheck;
import openmods.calc.CalcTestUtils.SymbolStub;
import openmods.calc.types.bigint.BigIntCalculatorFactory;
import org.junit.Test;

public class BigIntCalculatorTest {

	private final Calculator<BigInteger, ExprType> sut = BigIntCalculatorFactory.createDefault();

	public CalcCheck<BigInteger> prefix(String value) {
		return CalcCheck.create(sut, value, ExprType.PREFIX);
	}

	public CalcCheck<BigInteger> infix(String value) {
		return CalcCheck.create(sut, value, ExprType.INFIX);
	}

	public CalcCheck<BigInteger> postfix(String value) {
		return CalcCheck.create(sut, value, ExprType.POSTFIX);
	}

	public CalcCheck<BigInteger> compiled(IExecutable<BigInteger> expr) {
		return CalcCheck.create(sut, expr);
	}

	public static BigInteger v(long value) {
		return BigInteger.valueOf(value);
	}

	@Test
	public void testBasicPrefix() {
		prefix("(+ 1 2)").expectResult(v(3));
		prefix("(* 2 3)").expectResult(v(6));
		prefix("(- 1)").expectResult(v(-1));
		prefix("(* (- 1) (+ 2 3))").expectResult(v(-5));
		prefix("(/ 10 2)").expectResult(v(5));
		prefix("(** 2 5)").expectResult(v(32));
		prefix("(| 0b010 0b101)").expectResult(v(7));

		prefix("(max 1)").expectResult(v(1));
		prefix("(max 1 2)").expectResult(v(2));
		prefix("(max 1 2 3)").expectResult(v(3));
	}

	@Test
	public void testBasicPostfix() {
		postfix("1 2 +").expectResult(v(3));
		postfix("2 3 *").expectResult(v(6));
		postfix("10 2 /").expectResult(v(5));
		postfix("2 5 **").expectResult(v(32));
		postfix("0b010 0b101 |").expectResult(v(7));
		postfix("0b100 0b101 &").expectResult(v(4));
		postfix("0b110 0b101 ^").expectResult(v(3));
		postfix("0b100 2 <<").expectResult(v(16));
		postfix("0b100 2 >>").expectResult(v(1));
		postfix("45 4 %").expectResult(v(1));
	}

	@Test
	public void testPostfixSymbols() {
		sut.environment.setGlobalSymbol("a", v(5));
		postfix("a").expectResult(v(5));
		postfix("a$0").expectResult(v(5));
		postfix("a$,1").expectResult(v(5));
		postfix("a$0,1").expectResult(v(5));
		postfix("@a").expectResult(v(5));
	}

	@Test(expected = RuntimeException.class)
	public void testPostfixFunctionGet() {
		postfix("@abs").execute();
	}

	@Test
	public void testPostfixStackOperations() {
		postfix("2 dup +").expectResult(v(4));
		postfix("1 2 3 pop +").expectResult(v(3));
		postfix("2 3 swap -").expectResult(v(1));
	}

	@Test
	public void testPostfixDupWithArgs() {
		postfix("0 1 2 dup$2").expectResults(v(0), v(1), v(2), v(1), v(2));
	}

	@Test
	public void testPostfixDupWithReturns() {
		postfix("1 2 dup$,4").expectResults(v(1), v(2), v(2), v(2), v(2));
	}

	@Test
	public void testPostfixDupWithArgsAndReturns() {
		postfix("1 2 3 4 dup$3,5").expectResults(v(1), v(2), v(3), v(4), v(2), v(3));
	}

	@Test
	public void testPostfixPopWithArgs() {
		postfix("1 2 3 4 pop$3").expectResults(v(1));
	}

	@Test
	public void testVariadicPostfixFunctions() {
		postfix("max$0").expectResult(v(0));
		postfix("1 max$1").expectResult(v(1));
		postfix("1 2 max$2").expectResult(v(2));

		postfix("3 2 1 sum$3").expectResult(v(6));
		postfix("3 2 1 avg$3").expectResult(v(2));
	}

	@Test
	public void testBasicInfix() {
		infix("1+2").expectResult(v(3));
		infix("2*3").expectResult(v(6));
		infix("10/2").expectResult(v(5));
		infix("2**5").expectResult(v(32));
		infix("2(5)").expectResult(v(10));
		infix("0b010|0b101").expectResult(v(7));
		infix("0b100&0b101").expectResult(v(4));
		infix("0b110^0b101").expectResult(v(3));
		infix("0b100<<2").expectResult(v(16));
		infix("0b100>>2").expectResult(v(1));
		infix("45 % 4").expectResult(v(1));
	}

	@Test
	public void testBasicOrdering() {
		infix("1 + 2 - 3").expectResult(v(0));

		infix("1 + 2 * 3").expectResult(v(7));
		infix("1 + (2 * 3)").expectResult(v(7));
		infix("(1 + 2) * 3").expectResult(v(9));
		infix("-(1 + 2) * 3").expectResult(v(-9));
		infix("(1 + 2) * -3").expectResult(v(-9));

		infix("--3").expectResult(v(3));
		infix("-~2").expectResult(v(3));

		infix("2 * 2 ** 2").expectResult(v(8));
		infix("2 * (2 ** 2)").expectResult(v(8));
		infix("(2 * 2) ** 2").expectResult(v(16));
	}

	@Test
	public void testBasicInfixFunctions() {
		infix("gcd(6, 8)").expectResult(v(2));
		infix("abs(-2)").expectResult(v(2));
		infix("5*abs(-2)").expectResult(v(10));
		infix("1+abs(-2)").expectResult(v(3));
		infix("min(2,3)").expectResult(v(2));
		infix("max(2,3)").expectResult(v(3));
		infix("2-max(2,3)").expectResult(v(-1));
	}

	@Test
	public void testVariadicInfixFunctions() {
		infix("max()").expectResult(v(0));
		infix("max(1)").expectResult(v(1));
		infix("max(1,2)").expectResult(v(2));
		infix("max(3,2,1)").expectResult(v(3));
	}

	@Test(expected = Exception.class)
	public void testTooManyParameters() {
		infix("abs(0, 1)").execute();
	}

	@Test(expected = Exception.class)
	public void testTooFewParameters() {
		infix("gcd(0)").execute();
	}

	@Test
	public void testParserSwitch() {
		infix("2 + prefix(5)").expectResult(v(7));
		infix("2 + prefix((+ 5 6))").expectResult(v(13));

		prefix("(+ 2 (infix 5))").expectResult(v(7));
		prefix("(+ 2 (infix 5 + 6))").expectResult(v(13));
	}

	@Test
	public void testNestedParserSwitch() {
		infix("infix(5 + 2)").expectResult(v(7));
		infix("infix(infix(5 + 2))").expectResult(v(7));

		prefix("(prefix (+ 2 5))").expectResult(v(7));
		prefix("(prefix (prefix (+ 2 5)))").expectResult(v(7));

		infix("prefix((infix 2 + 5))").expectResult(v(7));
		prefix("(infix prefix((+ 2 5)))").expectResult(v(7));
	}

	@Test
	public void testConstantEvaluatingBrackets() {
		final SymbolStub<BigInteger> stub = new SymbolStub<BigInteger>()
				.allowCalls()
				.expectArgs(v(1), v(2))
				.verifyArgCount()
				.setReturns(v(5), v(6), v(7))
				.verifyReturnCount();
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<BigInteger> expr = sut.compilers.compile(ExprType.POSTFIX, "[1 2 dummy$2,3]");
		stub.checkCallCount(1);
		compiled(expr).expectResults(v(5), v(6), v(7));
		stub.checkCallCount(1);
	}

	@Test
	public void testNestedConstantEvaluatingBrackets() {
		final SymbolStub<BigInteger> stub = new SymbolStub<BigInteger>()
				.allowGets()
				.setGetValue(v(5));
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<BigInteger> expr = sut.compilers.compile(ExprType.POSTFIX, "[4 [@dummy 3 -] *]");
		stub.checkGetCount(1);
		compiled(expr).expectResults(v(8));
		stub.checkGetCount(1);
	}

	@Test
	public void testConstantEvaluatingSymbol() {
		final SymbolStub<BigInteger> stub = new SymbolStub<BigInteger>()
				.allowCalls()
				.expectArgs(v(1), v(2))
				.verifyArgCount()
				.setReturns(v(5))
				.verifyReturnCount();
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<BigInteger> expr = sut.compilers.compile(ExprType.INFIX, "9 + const(dummy(1,2) + 3)");
		stub.checkCallCount(1);

		compiled(expr).expectResults(v(17));
		stub.checkCallCount(1);

		compiled(expr).expectResults(v(17));
		stub.checkCallCount(1);
	}

	@Test
	public void testSimpleLet() {
		infix("let([x:2,y:3], x + y)").expectResult(v(5));
		prefix("(let [(:x 2) (:y 3)] (+ x y))").expectResult(v(5));
	}

	@Test
	public void testLetWithExpression() {
		infix("let([x:1 + 2,y:3 + 4], x + y)").expectResult(v(10));
		prefix("(let [(:x (+ 1 2)) (:y (+ 3 4))] (+ x y))").expectResult(v(10));
	}

	@Test
	public void testNestedLet() {
		infix("let([x:2,y:3], let([w:x+y,z:x-y], w + z))").expectResult(v(4));
		prefix("(let [(:x 2) (:y 3)] (let [(:w (+ x y)) (:z (- x y))] (+ w z)))").expectResult(v(4));
	}

	@Test
	public void testFunctionLet() {
		infix("let([x():2], x())").expectResult(v(2));
		prefix("(let [(:(x) 2)] (x))").expectResult(v(2));

		infix("let([x(a,b):a+b], x(1,2))").expectResult(v(3));
		prefix("(let [(: (x a b) (+ a b))], (x 1 2))").expectResult(v(3));
	}

	@Test
	public void testLetScoping() {
		infix("let([x:2], let([y:x], let([x:3], y)))").expectResult(v(2));
		infix("let([x:2], let([f(a):a+x], let([x:3], f(4))))").expectResult(v(6));
		infix("let([x:5], let([x:2, y:x], x + y))").expectResult(v(7));
	}

	@Test
	public void testFailSymbol() {
		infix("fail('welp')").expectThrow(ExecutionErrorException.class, "welp");
		infix("fail()").expectThrow(ExecutionErrorException.class, null);
	}
}
