package openmods.calc;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import openmods.calc.CalcTestUtils.CalcCheck;
import openmods.calc.CalcTestUtils.SymbolStub;
import openmods.calc.types.multi.Cons;
import openmods.calc.types.multi.IComposite;
import openmods.calc.types.multi.Symbol;
import openmods.calc.types.multi.TypeDomain;
import openmods.calc.types.multi.TypedBinaryOperator;
import openmods.calc.types.multi.TypedFunction;
import openmods.calc.types.multi.TypedUnaryOperator;
import openmods.calc.types.multi.TypedValue;
import openmods.calc.types.multi.TypedValueCalculatorFactory;
import openmods.math.Complex;
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

	private final Calculator<TypedValue, ExprType> sut = TypedValueCalculatorFactory.create();

	public CalcCheck<TypedValue> prefix(String value) {
		return CalcCheck.create(sut, value, ExprType.PREFIX);
	}

	public CalcCheck<TypedValue> infix(String value) {
		return CalcCheck.create(sut, value, ExprType.INFIX);
	}

	public CalcCheck<TypedValue> postfix(String value) {
		return CalcCheck.create(sut, value, ExprType.POSTFIX);
	}

	public CalcCheck<TypedValue> compiled(IExecutable<TypedValue> expr) {
		return CalcCheck.create(sut, expr);
	}

	private final TypedValue NULL = sut.environment.nullValue();

	private final TypeDomain domain = NULL.domain;

	private TypedValue s(String value) {
		return domain.create(String.class, value);
	}

	private TypedValue sym(String value) {
		return domain.create(Symbol.class, Symbol.get(value));
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

	private TypedValue nil() {
		return sut.environment.nullValue();
	}

	private TypedValue cons(TypedValue car, TypedValue cdr) {
		return domain.create(Cons.class, new Cons(car, cdr));
	}

	private final TypedValue TRUE = b(true);

	private final TypedValue FALSE = b(false);

	private TypedValue c(double re, double im) {
		return domain.create(Complex.class, Complex.cartesian(re, im));
	}

	@Test
	public void testBasicPrefix() {
		prefix("(+ 1 2)").expectResult(i(3));
		prefix("(* 2 3)").expectResult(i(6));
		prefix("(- 1)").expectResult(i(-1));
		prefix("(* (- 1) (+ 2 3))").expectResult(i(-5));
		prefix("(/ 10 2)").expectResult(d(5.0));
		prefix("(** 2 5)").expectResult(i(32));
		prefix("(| 0b010 0b101)").expectResult(i(7));
	}

	@Test
	public void testPrefixFunctions() {
		prefix("(max 1)").expectResult(i(1));
		prefix("(max 1 2)").expectResult(i(2));
		prefix("(max 1 2 3)").expectResult(i(3));

		prefix("(max 1 2.0 3)").expectResult(i(3));
		prefix("(max true 2 3.0)").expectResult(d(3));

		prefix("(min true 2 3.0)").expectResult(b(true));

		prefix("(sum 1 2.0 3)").expectResult(d(6.0));
		prefix("(sum 'a' 'b' 'c')").expectResult(s("abc"));

		prefix("(avg 1 2 3)").expectResult(d(2.0));
	}

	@Test
	public void testBasicPostfix() {
		postfix("1 2 +").expectResult(i(3));
		postfix("0.5 0.5 +").expectResult(d(1));
		postfix("0.25 0.25 +").expectResult(d(0.5));
		postfix("2 3 *").expectResult(i(6));
		postfix("10 2 /").expectResult(d(5.0));
		postfix("10 2 //").expectResult(i(5));
		postfix("1 2 +").expectResult(i(3));
		postfix("true true &&").expectResult(TRUE);
		postfix("false true &&").expectResult(FALSE);
		postfix("'abc' 'def' +").expectResult(s("abcdef"));
		postfix("'abc' 'def' <=").expectResult(TRUE);
	}

	@Test
	public void testCoercionPostfix() {
		postfix("0.5 1 +").expectResult(d(1.5));

		postfix("2 5.0 **").expectResult(d(32));
		postfix("2 5 **").expectResult(i(32));

		postfix("true 2 +").expectResult(i(3));
		postfix("true 2.0 +").expectResult(d(3.0));
	}

	@Test
	public void testArithmeticInfix() {
		infix("1 + 2").expectResult(i(3));
		infix("2 * 3").expectResult(i(6));
		infix("10 / 2").expectResult(d(5.0));

		infix("10 // 2").expectResult(i(5));
		infix("10.7 // 2").expectResult(d(5.0));
		infix("-2.3 // 3").expectResult(d(-1.0));

		infix("2 ** 5").expectResult(i(32));
		infix("2 ** 0").expectResult(i(1));
		infix("2 ** -5").expectResult(d(1.0 / 32.0));

		infix("5 % 2").expectResult(i(1));
		infix("5.125 % 1").expectResult(d(0.125));
		infix("5 % 2.0").expectResult(d(1.0));
		infix("5.125 % 1.0").expectResult(d(0.125));

		infix("-true").expectResult(i(-1));
		infix("2*true").expectResult(i(2));
		infix("2*-true").expectResult(i(-2));
		infix("2true").expectResult(i(2));
		infix("2(true)").expectResult(i(2));

		infix("-2*-3*10**3").expectResult(i(6000));
		infix("-2*-3*10**+3").expectResult(i(6000));
		infix("-2*-3*10**-3").expectResult(d(6e-3));
		infix("2*10**2+3*10**3").expectResult(i(3200));
		infix("2*10**2*3*10**3").expectResult(i(600000));

		infix("0.1 + true").expectResult(d(1.1));

		infix("'abc' * 2").expectResult(s("abcabc"));
	}

	@Test
	public void testLogicInfix() {
		infix("!true").expectResult(FALSE);
		infix("!1").expectResult(FALSE);
		infix("!'hello'").expectResult(FALSE);
		infix("!''").expectResult(TRUE);
		infix("!0").expectResult(TRUE);

		infix("'abc' && 5").expectResult(i(5));
		infix("0 && 'abc'").expectResult(i(0));
		infix("'' && 4").expectResult(s(""));

		infix("'abc' || 5").expectResult(s("abc"));
		infix("'' || 5").expectResult(i(5));
		infix("'' || 0").expectResult(i(0));
	}

	@Test
	public void testBitwiseInfix() {
		infix("~true").expectResult(i(0));
		infix("~0b10").expectResult(i(-3));
		infix("0b10110 ^ 0b101101").expectResult(i(0x3B));
		infix("0b1010 << 0b10").expectResult(i(40));
	}

	@Test
	public void testCompare() {
		infix("2 < 3").expectResult(TRUE);
		infix("3 != 3").expectResult(FALSE);
		infix("3 <= 3").expectResult(TRUE);

		infix("3 <=> 3").expectResult(i(0));
		infix("2 <=> 3").expectResult(i(-1));
		infix("3 <=> 2").expectResult(i(+1));
	}

	@Test
	public void testEquals() {
		infix("2 == 2").expectResult(b(true));
		infix("null == null").expectResult(b(true));
		infix("2 == null").expectResult(b(false));

		infix("2 != null").expectResult(b(true));
		infix("null != 2").expectResult(b(true));
		infix("null != null").expectResult(b(false));
	}

	@Test
	public void testBasicOrdering() {
		infix("1 + 2 - 3").expectResult(i(0));

		infix("1 + 2 * 3").expectResult(i(7));
		infix("1 + (2 * 3)").expectResult(i(7));
		infix("(1 + 2) * 3").expectResult(i(9));
		infix("-(1 + 2) * 3").expectResult(i(-9));
		infix("(1 + 2) * -3").expectResult(i(-9));
		infix("--3").expectResult(i(3));

		infix("2 * 2 ** 2").expectResult(i(8));
		infix("2 * (2 ** 2)").expectResult(i(8));
		infix("(2 * 2) ** 2").expectResult(i(16));

		infix("2 == 4 || 5 <= 6").expectResult(TRUE);
		infix("2 << 3 + 1").expectResult(i(32));
	}

	@Test
	public void testTypeFunctions() {
		infix("issymbol(#test)").expectResult(b(true));
		infix("issymbol(#+)").expectResult(b(true));
		infix("issymbol(#2)").expectResult(b(false));

		infix("type(null)").expectResult(s("<null>"));
		infix("type(true)").expectResult(s("bool"));
		infix("type(5)").expectResult(s("int"));
		infix("type(5.0)").expectResult(s("float"));
		infix("type('a')").expectResult(s("str"));
		infix("type(I)").expectResult(s("complex"));
		infix("type(2 + 3I)").expectResult(s("complex"));
		infix("type(2 + 3*I)").expectResult(s("complex"));

		infix("isint(null)").expectResult(b(false));
		infix("isint(true)").expectResult(b(false));
		infix("isint(5)").expectResult(b(true));
		infix("isint(5.0)").expectResult(b(false));
		infix("isint('hello')").expectResult(b(false));
		infix("isint('I')").expectResult(b(false));
		infix("isint(#2)").expectResult(b(true));

		infix("iscomplex(1)").expectResult(b(false));
		infix("iscomplex(I)").expectResult(b(true));
		infix("iscomplex(1 + I)").expectResult(b(true));

		infix("isnumber(null)").expectResult(b(false));
		infix("isnumber(true)").expectResult(b(true));
		infix("isnumber(5)").expectResult(b(true));
		infix("isnumber(5.0)").expectResult(b(true));
		infix("isnumber(I)").expectResult(b(true));
		infix("isnumber(3 + 4I)").expectResult(b(true));
		infix("isnumber('hello')").expectResult(b(false));

		infix("int(true)").expectResult(i(1));
		infix("int(5)").expectResult(i(5));
		infix("int(5.2)").expectResult(i(5));
		infix("int('6')").expectResult(i(6));
		infix("int('29A', 16)").expectResult(i(666));

		infix("float(true)").expectResult(d(1));
		infix("float(5)").expectResult(d(5));
		infix("float(5.2)").expectResult(d(5.2));
		infix("float('6.1')").expectResult(d(6.1));
		infix("float('29A.1', 16)").expectResult(d(666.0625));

		infix("number(true)").expectResult(b(true));
		infix("number(5)").expectResult(i(5));
		infix("number(5.2)").expectResult(d(5.2));
		infix("number('6')").expectResult(i(6));
		infix("number('6.1')").expectResult(d(6.1));
		infix("number('29A', 16)").expectResult(i(666));
		infix("number('29A.1', 16)").expectResult(d(666.0625));
		infix("number(3I)").expectResult(c(0, 3));
		infix("number(3 + 4I)").expectResult(c(3, 4));

		infix("bool(true)").expectResult(b(true));
		infix("bool(5)").expectResult(b(true));
		infix("bool(0)").expectResult(b(false));
		infix("bool(I)").expectResult(b(true));
		infix("bool(0I)").expectResult(b(false));
		infix("bool('')").expectResult(b(false));
		infix("bool('a')").expectResult(b(true));
		infix("bool(null)").expectResult(b(false));

		infix("str(true)").expectResult(s("true"));
		infix("str(5)").expectResult(s("5"));
		infix("str(5.2)").expectResult(s("5.2"));
		infix("str('aaa')").expectResult(s("aaa"));
		infix("str(I)").expectResult(s("0.0+1.0I"));
		infix("str(3 + 4I)").expectResult(s("3.0+4.0I"));

		infix("parse('\"aaa\"')").expectResult(s("aaa"));
		infix("parse('0x29A')").expectResult(i(666));
		infix("parse('0x29A.1')").expectResult(d(666.0625));
		infix("parse('100#10')").expectResult(i(100));
	}

	@Test
	public void testArithmeticFunctions() {
		infix("isnan(NAN)").expectResult(b(true));
		infix("isnan(5)").expectResult(b(false));

		infix("isinf(INF)").expectResult(b(true));
		infix("isinf(-INF)").expectResult(b(true));
		infix("isnan(4)").expectResult(b(false));

		infix("ceil(true)").expectResult(b(true));
		infix("ceil(2)").expectResult(i(2));
		infix("ceil(2.0)").expectResult(d(2));
		infix("ceil(2.4)").expectResult(d(3));

		infix("abs(true)").expectResult(b(true));
		infix("abs(-2)").expectResult(i(2));
		infix("abs(+2)").expectResult(i(2));
		infix("abs(2.0)").expectResult(d(2));
		infix("abs(-2.4)").expectResult(d(2.4));
		infix("abs(3+4I)").expectResult(d(5));

		infix("exp(false)").expectResult(d(1.0));
		// infix("exp(true)").expectResult(d(Math.E));
		// infix("exp(1)").expectResult(d(Math.E));

		// infix("ln(I)").expectResult(c(0, Math.PI / 2));

		infix("log(true)").expectResult(d(0));
		infix("log(1)").expectResult(d(0));
		infix("log(1.0)").expectResult(d(0));
		infix("log(10)").expectResult(d(1));
		infix("log(100)").expectResult(d(2));
		infix("log(E, E)").expectResult(d(1));
		infix("log(2, E) == ln(2)").expectResult(b(true));
	}

	@Test
	public void testDotOperator() {
		class TestComposite implements IComposite {
			private final List<String> path;

			public TestComposite() {
				this.path = ImmutableList.of();
			}

			public TestComposite(List<String> parentPath, String elem) {
				this.path = ImmutableList.<String> builder().addAll(parentPath).add(elem).build();
			}

			@Override
			public TypedValue get(TypeDomain domain, String component) {
				if (component.equals("path")) return domain.create(String.class, Joiner.on("/").join(path));
				else return domain.create(IComposite.class, new TestComposite(path, component));
			}

			@Override
			public String subtype() {
				return "nested:" + path.size();
			}
		}

		sut.environment.setGlobalSymbol("root", Constant.create(sut.environment.nullValue().domain.create(IComposite.class, new TestComposite())));
		infix("type(root)=='object'").expectResult(b(true));
		infix("isobject(root)").expectResult(b(true));
		infix("bool(root)").expectResult(b(true));
		infix("root.path").expectResult(s(""));

		prefix("(== (type root) 'object')").expectResult(b(true));
		prefix("(isobject root)").expectResult(b(true));
		prefix("(. root path)").expectResult(s(""));

		infix("isobject(root.a)").expectResult(b(true));
		infix("root.a.path").expectResult(s("a"));
		prefix("(. root a path)").expectResult(s("a"));

		infix("isobject(root.a.b)").expectResult(b(true));
		infix("root.a.b.path").expectResult(s("a/b"));

		infix("root.'a'.path").expectResult(s("a"));
		prefix("(. root 'a' path)").expectResult(s("a"));

		infix("root.('a').path").expectResult(s("a"));

		infix("(root.a).b.path").expectResult(s("a/b"));
		prefix("(. (. root a) b path)").expectResult(s("a/b"));
	}

	@Test
	public void testInfixFunctions() {
		infix("max(true, 2, 3.0)").expectResult(d(3));

		infix("min(true, 2, 3.0)").expectResult(b(true));

		infix("sum(1, 2.0, 3)").expectResult(d(6.0));
		infix("sum('a', 'b', 'c')").expectResult(s("abc"));

		infix("avg(1, 2, 3)").expectResult(d(2.0));
	}

	@Test
	public void testPrefixModifierQuotes() {
		prefix("#()").expectResult(nil());
		prefix("#2").expectResult(i(2));
		prefix("#'hello'").expectResult(s("hello"));
		prefix("#(1)").expectResult(cons(i(1), nil()));
		prefix("#(1 2)").expectResult(cons(i(1), cons(i(2), nil())));
	}

	@Test
	public void testInfixModifierQuotes() {
		infix("#()").expectResult(nil());
		infix("#2").expectResult(i(2));
		infix("#'hello'").expectResult(s("hello"));
		infix("#(1)").expectResult(cons(i(1), nil()));
		infix("#(1 2)").expectResult(cons(i(1), cons(i(2), nil())));
	}

	@Test
	public void testPrefixSymbolQuotes() {
		prefix("(quote ())").expectResult(nil());
		prefix("(quote 2)").expectResult(i(2));
		prefix("(quote 'hello')").expectResult(s("hello"));
		prefix("(quote (1))").expectResult(cons(i(1), nil()));
		prefix("(quote (1 2))").expectResult(cons(i(1), cons(i(2), nil())));
	}

	@Test
	public void testInfixSymbolQuotes() {
		infix("quote(())").expectResult(nil());
		infix("quote(2)").expectResult(i(2));
		infix("quote('hello')").expectResult(s("hello"));
		infix("quote((1))").expectResult(cons(i(1), nil()));
		infix("quote((1 2))").expectResult(cons(i(1), cons(i(2), nil())));
	}

	@Test
	public void testCommaWhitespaceInQuotes() {
		prefix("#(1,2)").expectResult(cons(i(1), cons(i(2), nil())));
		prefix("(quote (1,2))").expectResult(cons(i(1), cons(i(2), nil())));
		infix("quote((1,2))").expectResult(cons(i(1), cons(i(2), nil())));
	}

	@Test
	public void testPostfixQuotes() {
		postfix("#a").expectResult(sym("a"));
		postfix("# a").expectResult(sym("a"));
		postfix("#a issymbol").expectResult(b(true));
		postfix("#abc 'abc' symbol ==").expectResult(b(true));

		postfix("#+").expectResult(sym("+"));
		postfix("# +").expectResult(sym("+"));
	}

	@Test
	public void testPrefixModifierQuotesWithSpecialTokens() {
		prefix("#+").expectResult(sym("+"));
		prefix("#test").expectResult(sym("test"));
		prefix("#(max)").expectResult(cons(sym("max"), nil()));
		prefix("#(+)").expectResult(cons(sym("+"), nil()));
		prefix("#(1 + max)").expectResult(cons(i(1), cons(sym("+"), cons(sym("max"), nil()))));
	}

	@Test
	public void testInfixModifierQuotesWithSpecialTokens() {
		infix("#+").expectResult(sym("+"));
		infix("#test").expectResult(sym("test"));
		infix("#(max)").expectResult(cons(sym("max"), nil()));
		infix("#(+)").expectResult(cons(sym("+"), nil()));
		infix("#(1 + max)").expectResult(cons(i(1), cons(sym("+"), cons(sym("max"), nil()))));
	}

	@Test
	public void testPrefixSymbolQuotesWithSpecialTokens() {
		prefix("(quote +)").expectResult(sym("+"));
		prefix("(quote test)").expectResult(sym("test"));
		prefix("(quote (max))").expectResult(cons(sym("max"), nil()));
		prefix("(quote (+))").expectResult(cons(sym("+"), nil()));
		prefix("(quote (1 + max))").expectResult(cons(i(1), cons(sym("+"), cons(sym("max"), nil()))));
	}

	@Test
	public void testInfixSymbolQuotesWithSpecialTokens() {
		infix("quote(+)").expectResult(sym("+"));
		infix("quote(test)").expectResult(sym("test"));
		infix("quote((max))").expectResult(cons(sym("max"), nil()));
		infix("quote((+))").expectResult(cons(sym("+"), nil()));
		infix("quote((1 + max))").expectResult(cons(i(1), cons(sym("+"), cons(sym("max"), nil()))));
	}

	@Test
	public void testPrefixNestedModifierQuotes() {
		prefix("#(())").expectResult(cons(nil(), nil()));
		prefix("#((1))").expectResult(cons(cons(i(1), nil()), nil()));
		prefix("#((1), 2)").expectResult(cons(cons(i(1), nil()), cons(i(2), nil())));
		prefix("#(1 (2))").expectResult(cons(i(1), cons(cons(i(2), nil()), nil())));
	}

	@Test
	public void testInfixNestedModifierQuotes() {
		infix("#(())").expectResult(cons(nil(), nil()));
		infix("#((1))").expectResult(cons(cons(i(1), nil()), nil()));
		infix("#((1), 2)").expectResult(cons(cons(i(1), nil()), cons(i(2), nil())));
		infix("#(1 (2))").expectResult(cons(i(1), cons(cons(i(2), nil()), nil())));
	}

	@Test
	public void testPrefixDottedModifierQuotes() {
		prefix("#(3 ... ())").expectResult(cons(i(3), nil()));
		prefix("#(3 ... 2)").expectResult(cons(i(3), i(2)));
		prefix("#(3 ... (2 ... 1))").expectResult(cons(i(3), cons(i(2), i(1))));
	}

	@Test
	public void testPrefixDottedSymbolQuotes() {
		prefix("(quote (3 ... ()))").expectResult(cons(i(3), nil()));
		prefix("(quote (3 ... 2))").expectResult(cons(i(3), i(2)));
		prefix("(quote (3 ... (2 ... 1)))").expectResult(cons(i(3), cons(i(2), i(1))));
	}

	@Test
	public void testInfixDottedModifierQuotes() {
		infix("#(3 ... ())").expectResult(cons(i(3), nil()));
		infix("#(3 ... 2)").expectResult(cons(i(3), i(2)));
		infix("#(3 ... (2 ... 1))").expectResult(cons(i(3), cons(i(2), i(1))));
	}

	@Test
	public void testInfixDottedSymbolQuotes() {
		infix("quote((3 ... ()))").expectResult(cons(i(3), nil()));
		infix("quote((3 ... 2))").expectResult(cons(i(3), i(2)));
		infix("quote((3 ... (2 ... 1)))").expectResult(cons(i(3), cons(i(2), i(1))));
	}

	@Test
	public void testMixedQuotes() {
		prefix("(quote #)").expectResult(sym("#"));
		prefix("#(quote 2)").expectResult(cons(sym("quote"), cons(i(2), nil())));

		infix("quote(#)").expectResult(sym("#"));
		infix("#(quote 2)").expectResult(cons(sym("quote"), cons(i(2), nil())));
	}

	@Test
	public void testArgQuotes() {
		prefix("(str (quote test))").expectResult(s("test"));
		prefix("(len (quote (a + c)))").expectResult(i(3));

		prefix("(str #test)").expectResult(s("test"));
		prefix("(len #(a + c))").expectResult(i(3));

		infix("str(quote(test))").expectResult(s("test"));
		infix("len(quote((a + c)))").expectResult(i(3));

		infix("str(#test)").expectResult(s("test"));
		infix("len(#(a + c))").expectResult(i(3));
	}

	@Test
	public void testListFunctions() {
		infix("list()").expectResult(nil());
		infix("list(1,2,3)").expectResult(cons(i(1), cons(i(2), cons(i(3), nil()))));
		infix("iscons(list(1,2,3))").expectResult(b(true));
		infix("list(1,2,3) == cons(1, cons(2, cons(3, null)))").expectResult(b(true));
		prefix("(== (list 1,2,3) #(1 2 3))").expectResult(b(true));
		infix("car(cons(1, 2))").expectResult(i(1));
		infix("cdr(cons(1, 2))").expectResult(i(2));
	}

	@Test
	public void testLengthFunction() {
		infix("len('')").expectResult(i(0));
		infix("len('a')").expectResult(i(1));
		infix("len('ab')").expectResult(i(2));

		infix("len(list())").expectResult(i(0));
		infix("len(list(1))").expectResult(i(1));
		infix("len(list(1,2))").expectResult(i(2));
	}

	@Test
	public void testSymbols() {
		prefix("(== #test # test)").expectResult(b(true));
		prefix("(== #a # b)").expectResult(b(false));
		prefix("(str #test)").expectResult(s("test"));
		prefix("(str #+)").expectResult(s("+"));
	}

	@Test
	public void testParserSwitch() {
		infix("2 + prefix(5)").expectResult(i(7));
		infix("2 + prefix((+ 5 6))").expectResult(i(13));

		prefix("(+ 2 (infix 5))").expectResult(i(7));
		prefix("(+ 2 (infix 5 + 6))").expectResult(i(13));
	}

	@Test
	public void testNestedParserSwitch() {
		infix("infix(5 + 2)").expectResult(i(7));
		infix("infix(infix(5 + 2))").expectResult(i(7));

		prefix("(prefix (+ 2 5))").expectResult(i(7));
		prefix("(prefix (prefix (+ 2 5)))").expectResult(i(7));

		infix("prefix((infix 2 + 5))").expectResult(i(7));
		prefix("(infix prefix((+ 2 5)))").expectResult(i(7));
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
	public void testConsOperator() {
		infix("1:2").expectResult(cons(i(1), i(2)));
		infix("true:'2':3").expectResult(cons(b(true), cons(s("2"), i(3))));

		infix("1:2:3:null == list(1,2,3)").expectResult(b(true));
		prefix("(== (: 1 2 3 null) (list 1 2 3))").expectResult(b(true));

		infix("1:2:3:4 == #(1 2 3 ... 4)").expectResult(b(true));
		prefix("(== (: 1 2 3 4) #(1 2 3 ... 4))").expectResult(b(true));
	}

	@Test
	public void testConstantEvaluatingBrackets() {
		final SymbolStub<TypedValue> stub = new SymbolStub<TypedValue>()
				.expectArgs(i(1), i(2))
				.checkArgCount()
				.setReturns(i(5), i(6), i(7))
				.checkReturnCount();
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<TypedValue> expr = sut.compilers.compile(ExprType.POSTFIX, "[1 2 dummy@2,3]");
		stub.checkCallCount(1);
		compiled(expr).expectResults(i(5), i(6), i(7));
		stub.checkCallCount(1);
	}

	@Test
	public void testCodeParsingInPostfixParser() {
		postfix("{ 1 2 +} iscode").expectResult(b(true));

		postfix("{ 1 2 +} execute").expectResult(i(3));
		postfix("{ 1 2 +} execute@1,1").expectResult(i(3));

		postfix("{ 1 2 + 3 4 -} execute").expectResults(i(3), i(-1));
		postfix("{ 1 2 + 3 4 -} execute@1,2").expectResults(i(3), i(-1));
	}

	@Test
	public void testCodeSymbol() {
		infix("iscode(code(2 + 3))").expectResult(b(true));
		infix("(iscode (code (+ 2 3)))").expectResult(b(true));

		infix("execute(code(2 + 3))").expectResult(i(5));
		prefix("(execute (code (+ 2 3)))").expectResult(i(5));
	}

	@Test
	public void testIfExpression() {
		infix("if(true, 2, 3)").expectResult(i(2));
		infix("if(false, 2, 3)").expectResult(i(3));

		prefix("(if true 2 3)").expectResult(i(2));
		prefix("(if false 2 3)").expectResult(i(3));

		infix("if(2 == 2, 2 + 3, 2 + 2)").expectResult(i(5));
		infix("if(2 == 3, 2 + 3, 2 + 2)").expectResult(i(4));

		prefix("(if (== 2 2) (+ 2 3) (+ 2 2))").expectResult(i(5));
		prefix("(if (== 2 3), (+ 2 3), (+ 2 2))").expectResult(i(4));
	}

	@Test
	public void testIfExpressionEvaluation() {
		final SymbolStub<TypedValue> leftStub = new SymbolStub<TypedValue>().setReturns(i(2));
		final SymbolStub<TypedValue> rightStub = new SymbolStub<TypedValue>().setReturns(i(3));

		sut.environment.setGlobalSymbol("left", leftStub);
		sut.environment.setGlobalSymbol("right", rightStub);

		infix("if(2 == 2, left, right)").expectResult(i(2));
		rightStub.checkCallCount(0).resetCallCount();
		leftStub.checkCallCount(1).resetCallCount();

		infix("if(2 != 2, left, right)").expectResult(i(3));
		rightStub.checkCallCount(1).resetCallCount();
		leftStub.checkCallCount(0).resetCallCount();
	}

	@Test
	public void testStringSlice() {
		infix("'abc'[0]").expectResult(s("a"));
		infix("'abc'[1]").expectResult(s("b"));
		infix("'abc'[2]").expectResult(s("c"));

		infix("'abc'[-1]").expectResult(s("c"));
		infix("'abc'[-2]").expectResult(s("b"));
		infix("'abc'[-3]").expectResult(s("a"));

		infix("'abc'[0:0]").expectResult(s(""));
		infix("'abc'[0:1]").expectResult(s("a"));
		infix("'abc'[0:2]").expectResult(s("ab"));
		infix("'abc'[0:3]").expectResult(s("abc"));

		infix("'abc'[0:-1]").expectResult(s("ab"));
		infix("'abc'[0:-2]").expectResult(s("a"));
		infix("'abc'[0:-3]").expectResult(s(""));

		infix("'abc'[-2:-1]").expectResult(s("b"));
		infix("'abc'[-3:-1]").expectResult(s("ab"));
	}

	@Test
	public void testStringSliceInExpressions() {
		sut.environment.setGlobalSymbol("test", Constant.create(s("abc")));

		infix("test[-1]").expectResult(s("c"));
		infix("test[-3] + test[-2] + test[-1]").expectResult(s("abc"));

		infix("('a' + 'b' + 'c')[0:3]").expectResult(s("abc"));
		infix("str(123)[2]").expectResult(s("3"));
	}
}
