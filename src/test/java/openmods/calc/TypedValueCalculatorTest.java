package openmods.calc;

import java.math.BigInteger;
import openmods.calc.CalcTestUtils.CalcCheck;
import openmods.calc.Calculator.ExprType;
import openmods.calc.types.multi.TypeDomain;
import openmods.calc.types.multi.TypedBinaryOperator;
import openmods.calc.types.multi.TypedFunction;
import openmods.calc.types.multi.TypedUnaryOperator;
import openmods.calc.types.multi.TypedValue;
import openmods.calc.types.multi.TypedValueCalculator;
import openmods.reflection.MethodAccess;
import openmods.reflection.TypeVariableHolderHandler;
import org.junit.Test;

public class TypedValueCalculatorTest {

	static {
		final TypeVariableHolderHandler filler = new TypeVariableHolderHandler();
		filler.fillHolders(TypedBinaryOperator.TypeVariableHolders.class);
		filler.fillHolders(TypedUnaryOperator.TypeVariableHolders.class);
		filler.fillHolders(TypeDomain.TypeVariableHolders.class);
		filler.fillHolders(MethodAccess.TypeVariableHolders.class);
		filler.fillHolders(TypedFunction.class);
	}

	private final TypedValueCalculator sut = TypedValueCalculator.create();

	public CalcCheck<TypedValue> prefix(String value) {
		return CalcCheck.create(sut, value, ExprType.PREFIX);
	}

	public CalcCheck<TypedValue> infix(String value) {
		return CalcCheck.create(sut, value, ExprType.INFIX);
	}

	public CalcCheck<TypedValue> postfix(String value) {
		return CalcCheck.create(sut, value, ExprType.POSTFIX);
	}

	private final TypedValue NULL = sut.nullValue();

	private final TypeDomain domain = NULL.domain;

	private TypedValue s(String value) {
		return domain.create(String.class, value);
	}

	private TypedValue i(long value) {
		return domain.create(BigInteger.class, BigInteger.valueOf(value));
	}

	private TypedValue d(double value) {
		return domain.create(Double.class, value);
	}

	private TypedValue b(boolean value) {
		return domain.create(Boolean.class, value);
	}

	private final TypedValue TRUE = b(true);

	private final TypedValue FALSE = b(false);

	@Test
	public void testBasicPrefix() {
		prefix("(+ 1 2)").expectResult(i(3)).expectEmptyStack();
		prefix("(* 2 3)").expectResult(i(6)).expectEmptyStack();
		prefix("(- 1)").expectResult(i(-1)).expectEmptyStack();
		prefix("(* (- 1) (+ 2 3))").expectResult(i(-5)).expectEmptyStack();
		prefix("(/ 10 2)").expectResult(d(5.0)).expectEmptyStack();
		prefix("(** 2 5)").expectResult(i(32)).expectEmptyStack();
		prefix("(| 0b010 0b101)").expectResult(i(7)).expectEmptyStack();

		// TODO once functions are done
		// prefix("(max 1)").expectResult(i(1)).expectEmptyStack();
		// prefix("(max 1 2)").expectResult(i(2)).expectEmptyStack();
		// prefix("(max 1 2 3)").expectResult(i(3)).expectEmptyStack();
	}

	@Test
	public void testBasicPostfix() {
		postfix("1 2 +").expectResult(i(3)).expectEmptyStack();
		postfix("0.5 0.5 +").expectResult(d(1)).expectEmptyStack();
		postfix("0.25 0.25 +").expectResult(d(0.5)).expectEmptyStack();
		postfix("2 3 *").expectResult(i(6)).expectEmptyStack();
		postfix("10 2 /").expectResult(d(5.0)).expectEmptyStack();
		postfix("10 2 //").expectResult(i(5)).expectEmptyStack();
		postfix("1 2 +").expectResult(i(3)).expectEmptyStack();
		postfix("true true &&").expectResult(TRUE).expectEmptyStack();
		postfix("false true &&").expectResult(FALSE).expectEmptyStack();
		postfix("'abc' 'def' +").expectResult(s("abcdef")).expectEmptyStack();
		postfix("'abc' 'def' <=").expectResult(TRUE).expectEmptyStack();
	}

	@Test
	public void testCoercionPostfix() {
		postfix("0.5 1 +").expectResult(d(1.5)).expectEmptyStack();

		postfix("2 5.0 **").expectResult(d(32)).expectEmptyStack();
		postfix("2 5 **").expectResult(i(32)).expectEmptyStack();

		postfix("true 2 +").expectResult(i(3)).expectEmptyStack();
		postfix("true 2.0 +").expectResult(d(3.0)).expectEmptyStack();
	}

	@Test
	public void testArithmeticInfix() {
		infix("1 + 2").expectResult(i(3)).expectEmptyStack();
		infix("2 * 3").expectResult(i(6)).expectEmptyStack();
		infix("10 / 2").expectResult(d(5.0)).expectEmptyStack();

		infix("10 // 2").expectResult(i(5)).expectEmptyStack();
		infix("10.7 // 2").expectResult(d(5.0)).expectEmptyStack();
		infix("-2.3 // 3").expectResult(d(-1.0)).expectEmptyStack();

		infix("2 ** 5").expectResult(i(32)).expectEmptyStack();
		infix("2 ** 0").expectResult(i(1)).expectEmptyStack();
		infix("2 ** -5").expectResult(d(1.0 / 32.0)).expectEmptyStack();

		infix("5 % 2").expectResult(i(1)).expectEmptyStack();
		infix("5.125 % 1").expectResult(d(0.125)).expectEmptyStack();
		infix("5 % 2.0").expectResult(d(1.0)).expectEmptyStack();
		infix("5.125 % 1.0").expectResult(d(0.125)).expectEmptyStack();

		infix("-true").expectResult(i(-1)).expectEmptyStack();
		infix("2*true").expectResult(i(2)).expectEmptyStack();
		infix("2*-true").expectResult(i(-2)).expectEmptyStack();
		infix("2true").expectResult(i(2)).expectEmptyStack();
		infix("2(true)").expectResult(i(2)).expectEmptyStack();

		infix("-2*-3*10**3").expectResult(i(6000)).expectEmptyStack();
		infix("-2*-3*10**+3").expectResult(i(6000)).expectEmptyStack();
		infix("-2*-3*10**-3").expectResult(d(6e-3)).expectEmptyStack();
		infix("2*10**2+3*10**3").expectResult(i(3200)).expectEmptyStack();
		infix("2*10**2*3*10**3").expectResult(i(600000)).expectEmptyStack();

		infix("0.1 + true").expectResult(d(1.1)).expectEmptyStack();

		infix("'abc' * 2").expectResult(s("abcabc")).expectEmptyStack();
	}

	@Test
	public void testLogicInfix() {
		infix("!true").expectResult(FALSE).expectEmptyStack();
		infix("!1").expectResult(FALSE).expectEmptyStack();
		infix("!'hello'").expectResult(FALSE).expectEmptyStack();
		infix("!''").expectResult(TRUE).expectEmptyStack();
		infix("!0").expectResult(TRUE).expectEmptyStack();

		infix("'abc' && 5").expectResult(i(5)).expectEmptyStack();
		infix("0 && 'abc'").expectResult(i(0)).expectEmptyStack();
		infix("'' && 4").expectResult(s("")).expectEmptyStack();

		infix("'abc' || 5").expectResult(s("abc")).expectEmptyStack();
		infix("'' || 5").expectResult(i(5)).expectEmptyStack();
		infix("'' || 0").expectResult(i(0)).expectEmptyStack();
	}

	@Test
	public void testBitwiseInfix() {
		infix("~true").expectResult(i(0)).expectEmptyStack();
		infix("~0b10").expectResult(i(-3)).expectEmptyStack();
		infix("0b10110 ^ 0b101101").expectResult(i(0x3B)).expectEmptyStack();
		infix("0b1010 << 0b10").expectResult(i(40)).expectEmptyStack();
	}

	@Test
	public void testCompare() {
		infix("2 < 3").expectResult(TRUE).expectEmptyStack();
		infix("3 != 3").expectResult(FALSE).expectEmptyStack();
		infix("3 <= 3").expectResult(TRUE).expectEmptyStack();

		infix("3 <=> 3").expectResult(i(0)).expectEmptyStack();
		infix("2 <=> 3").expectResult(i(-1)).expectEmptyStack();
		infix("3 <=> 2").expectResult(i(+1)).expectEmptyStack();
	}

	@Test
	public void testBasicOrdering() {
		infix("1 + 2 - 3").expectResult(i(0)).expectEmptyStack();

		infix("1 + 2 * 3").expectResult(i(7)).expectEmptyStack();
		infix("1 + (2 * 3)").expectResult(i(7)).expectEmptyStack();
		infix("(1 + 2) * 3").expectResult(i(9)).expectEmptyStack();
		infix("-(1 + 2) * 3").expectResult(i(-9)).expectEmptyStack();
		infix("(1 + 2) * -3").expectResult(i(-9)).expectEmptyStack();
		infix("--3").expectResult(i(3)).expectEmptyStack();

		infix("2 * 2 ** 2").expectResult(i(8)).expectEmptyStack();
		infix("2 * (2 ** 2)").expectResult(i(8)).expectEmptyStack();
		infix("(2 * 2) ** 2").expectResult(i(16)).expectEmptyStack();

		infix("2 == 4 || 5 <= 6").expectResult(TRUE).expectEmptyStack();
		infix("2 << 3 + 1").expectResult(i(32)).expectEmptyStack();
	}

	@Test
	public void testTypeFunctions() {
		infix("type(null)").expectResult(s("<null>")).expectEmptyStack();
		infix("type(true)").expectResult(s("bool")).expectEmptyStack();
		infix("type(5)").expectResult(s("int")).expectEmptyStack();
		infix("type(5.0)").expectResult(s("float")).expectEmptyStack();
		infix("type('a')").expectResult(s("str")).expectEmptyStack();

		infix("isint(null)").expectResult(b(false)).expectEmptyStack();
		infix("isint(true)").expectResult(b(false)).expectEmptyStack();
		infix("isint(5)").expectResult(b(true)).expectEmptyStack();
		infix("isint(5.0)").expectResult(b(false)).expectEmptyStack();
		infix("isint('hello')").expectResult(b(false)).expectEmptyStack();

		infix("isnumber(null)").expectResult(b(false)).expectEmptyStack();
		infix("isnumber(true)").expectResult(b(true)).expectEmptyStack();
		infix("isnumber(5)").expectResult(b(true)).expectEmptyStack();
		infix("isnumber(5.0)").expectResult(b(true)).expectEmptyStack();
		infix("isnumber('hello')").expectResult(b(false)).expectEmptyStack();

		infix("int(true)").expectResult(i(1)).expectEmptyStack();
		infix("int(5)").expectResult(i(5)).expectEmptyStack();
		infix("int(5.2)").expectResult(i(5)).expectEmptyStack();
		infix("int('6')").expectResult(i(6)).expectEmptyStack();
		infix("int('29A', 16)").expectResult(i(666)).expectEmptyStack();

		infix("float(true)").expectResult(d(1)).expectEmptyStack();
		infix("float(5)").expectResult(d(5)).expectEmptyStack();
		infix("float(5.2)").expectResult(d(5.2)).expectEmptyStack();
		infix("float('6.1')").expectResult(d(6.1)).expectEmptyStack();
		infix("float('29A.1', 16)").expectResult(d(666.0625)).expectEmptyStack();

		infix("bool(true)").expectResult(b(true)).expectEmptyStack();
		infix("bool(5)").expectResult(b(true)).expectEmptyStack();
		infix("bool(0)").expectResult(b(false)).expectEmptyStack();
		infix("bool('')").expectResult(b(false)).expectEmptyStack();
		infix("bool('a')").expectResult(b(true)).expectEmptyStack();
		infix("bool(null)").expectResult(b(false)).expectEmptyStack();

		sut.printTypes = false;
		infix("str(true)").expectResult(s("true")).expectEmptyStack();
		infix("str(5)").expectResult(s("5")).expectEmptyStack();
		infix("str(5.2)").expectResult(s("5.2")).expectEmptyStack();
		infix("str('aaa')").expectResult(s("aaa")).expectEmptyStack();

		infix("parse('\"aaa\"')").expectResult(s("aaa")).expectEmptyStack();
		infix("parse('0x29A')").expectResult(i(666)).expectEmptyStack();
		infix("parse('0x29A.1')").expectResult(d(666.0625)).expectEmptyStack();
		infix("parse('100#10')").expectResult(i(100)).expectEmptyStack();
	}
}
