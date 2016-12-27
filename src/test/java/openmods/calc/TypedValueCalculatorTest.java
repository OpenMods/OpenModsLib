package openmods.calc;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import openmods.calc.CalcTestUtils.CalcCheck;
import openmods.calc.CalcTestUtils.CallableStub;
import openmods.calc.CalcTestUtils.SymbolStub;
import openmods.calc.types.multi.CallableValue;
import openmods.calc.types.multi.Cons;
import openmods.calc.types.multi.MetaObject;
import openmods.calc.types.multi.MetaObjectInfo;
import openmods.calc.types.multi.MetaObjectUtils;
import openmods.calc.types.multi.Symbol;
import openmods.calc.types.multi.TypeDomain;
import openmods.calc.types.multi.TypedBinaryOperator;
import openmods.calc.types.multi.TypedCalcConstants;
import openmods.calc.types.multi.TypedFunction;
import openmods.calc.types.multi.TypedUnaryOperator;
import openmods.calc.types.multi.TypedValue;
import openmods.calc.types.multi.TypedValueCalculatorFactory;
import openmods.math.Complex;
import openmods.reflection.MethodAccess;
import openmods.reflection.TypeVariableHolderHandler;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;
import org.junit.Assert;
import org.junit.Test;

public class TypedValueCalculatorTest {

	static {
		final TypeVariableHolderHandler filler = new TypeVariableHolderHandler();
		filler.fillHolders(TypedBinaryOperator.TypeVariableHolders.class);
		filler.fillHolders(TypedUnaryOperator.TypeVariableHolders.class);
		filler.fillHolders(TypeDomain.TypeVariableHolders.class);
		filler.fillHolders(MethodAccess.TypeVariableHolders.class);
		filler.fillHolders(TypedFunction.class);
		filler.fillHolders(MetaObjectInfo.SlotAdapterVars.class);
	}

	private final Calculator<TypedValue, ExprType> sut = TypedValueCalculatorFactory.create();

	private static class DummyObject {}

	public static final DummyObject DUMMY = new DummyObject();

	public TypedValueCalculatorTest() {
		domain.registerType(DummyObject.class, "dummy");
	}

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

	private TypedValue complex(double real, double imag) {
		return domain.create(Complex.class, Complex.create(real, imag));
	}

	private TypedValue list() {
		return nil();
	}

	private TypedValue list(TypedValue... values) {
		TypedValue res = nil();
		for (int i = values.length - 1; i >= 0; i--)
			res = domain.create(Cons.class, new Cons(values[i], res));

		return res;
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
	public void testPostfixSymbols() {
		sut.environment.setGlobalSymbol("a", i(5));
		postfix("a").expectResult(i(5));
		postfix("a$0").expectResult(i(5));
		postfix("a$,1").expectResult(i(5));
		postfix("a$0,1").expectResult(i(5));
		postfix("@a").expectResult(i(5));
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
	public void testEquality() {
		infix("3 != 3").expectResult(FALSE);
		infix("3 == 3").expectResult(TRUE);

		infix("'a' != 'a'").expectResult(FALSE);
		infix("'a' == 'a'").expectResult(TRUE);

		infix("1 != 'a'").expectResult(TRUE);
		infix("1 == 'a'").expectResult(FALSE);
	}

	@Test
	public void testInequality() {
		infix("2 < 3").expectResult(TRUE);
		infix("3 < 2").expectResult(FALSE);
		infix("2 < 3.1").expectResult(TRUE);
		infix("3.1 < 2").expectResult(FALSE);

		infix("'a' < 'b'").expectResult(TRUE);
		infix("'b' < 'a'").expectResult(FALSE);

		infix("3 > 2.9").expectResult(TRUE);
		infix("2 > 3").expectResult(FALSE);

		infix("3 <= 3").expectResult(TRUE);
		infix("3 <= 3.1").expectResult(TRUE);

		infix("3.1 >= 3").expectResult(TRUE);
		infix("3.1 >= 3").expectResult(TRUE);
	}

	@Test
	public void testSpaceshipComparision() {
		infix("3 <=> 3").expectResult(i(0));
		infix("2 <=> 3").expectResult(i(-1));
		infix("3 <=> 2").expectResult(i(+1));

		infix("3.1 <=> 3.1").expectResult(i(0));
		infix("2.1 <=> 3").expectResult(i(-1));
		infix("3 <=> 2.2").expectResult(i(+1));
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
	public void testTypeFunction() {
		infix("type(#test) == symbol").expectResult(b(true));
		infix("type(#test).name").expectResult(s("symbol"));
		infix("type(#+) == symbol").expectResult(b(true));

		infix("type(null).name").expectResult(s("<null>"));

		infix("type(true).name").expectResult(s("bool"));
		infix("type(true) == bool").expectResult(TRUE);

		infix("type(5).name").expectResult(s("int"));
		infix("type(5) == int").expectResult(TRUE);
		infix("type(#5) == int").expectResult(TRUE);

		infix("type(5.0).name").expectResult(s("float"));
		infix("type(5.0) == float").expectResult(TRUE);

		infix("type('a').name").expectResult(s("str"));
		infix("type('a') == str").expectResult(TRUE);

		infix("type(I).name").expectResult(s("complex"));
		infix("type(I) == complex").expectResult(TRUE);
		infix("type(2 + 3I) == complex").expectResult(TRUE);
		infix("type(2 + 3*I) == complex").expectResult(TRUE);
	}

	@Test
	public void testTypeConversionFunctions() {
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

		infix("type(symbol('test')) == symbol").expectResult(TRUE);
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
	public void testTruthyCompositeObjects() {

		sut.environment.setGlobalSymbol("trueComposite", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(MetaObjectUtils.BOOL_ALWAYS_TRUE).build()));
		sut.environment.setGlobalSymbol("falseComposite", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(MetaObjectUtils.BOOL_ALWAYS_FALSE).build()));

		infix("bool(trueComposite)").expectResult(TRUE);
		infix("bool(falseComposite)").expectResult(FALSE);
	}

	@Test
	public void testCountableComposite() {
		class LengthSlot implements MetaObject.SlotLength {
			private final int length;

			public LengthSlot(int length) {
				this.length = length;
			}

			@Override
			public int length(TypedValue self, Frame<TypedValue> frame) {
				return length;
			}
		}

		sut.environment.setGlobalSymbol("zeroLength", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new LengthSlot(0)).build()));

		infix("len(zeroLength)").expectResult(i(0));

		sut.environment.setGlobalSymbol("nonZeroLength", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new LengthSlot(5)).build()));

		infix("len(nonZeroLength)").expectResult(i(5));
	}

	@Test
	public void testEquatableComposite() {
		// not very good example but meh...
		class EqualsSlot implements MetaObject.SlotEquals {
			private final TypedValue target;

			public EqualsSlot(TypedValue target) {
				this.target = target;
			}

			@Override
			public boolean equals(TypedValue self, TypedValue value, Frame<TypedValue> frame) {
				return target.equals(value);
			}

		}

		sut.environment.setGlobalSymbol("equalsTo5", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new EqualsSlot(i(5))).build()));

		infix("equalsTo5 == 5").expectResult(TRUE);
		infix("equalsTo5 != 5").expectResult(FALSE);

		infix("equalsTo5 == 6").expectResult(FALSE);
		infix("equalsTo5 != 6").expectResult(TRUE);

		infix("equalsTo5 == '5'").expectResult(FALSE);
		infix("equalsTo5 != '5'").expectResult(TRUE);

		// 'int' comparator is called first, so comparision fails
		// normally it shouldn't be problem, since 'equals' is expected to be symmetric
		infix("5 == equalsTo5").expectResult(FALSE);
		infix("5 != equalsTo5").expectResult(TRUE);

		infix("6 == equalsTo5").expectResult(FALSE);
		infix("6 != equalsTo5").expectResult(TRUE);

		infix("'5'== equalsTo5").expectResult(FALSE);
		infix("'5'!= equalsTo5").expectResult(TRUE);
	}

	@Test
	public void testCallableComposite() {
		class CallableSlot implements MetaObject.SlotCall {
			@Override
			public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
				Assert.assertTrue(argumentsCount.isPresent());
				for (int i = 0; i < argumentsCount.get(); i++)
					frame.stack().pop();

				Assert.assertTrue(returnsCount.isPresent());
				Assert.assertEquals(1, returnsCount.get());
				frame.stack().push(s("call:" + argumentsCount.get()));
			}
		}

		sut.environment.setGlobalSymbol("test", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new CallableSlot()).build()));

		infix("iscallable(test)").expectResult(TRUE);
		infix("test(2.0, 5)").expectResult(s("call:2"));
		infix("apply(test, 4, 7, 4)").expectResult(s("call:3"));
		infix("let([tmp = test], tmp(1,2))").expectResult(s("call:2"));
		infix("let([delayed = ()->test], delayed()(1,2,3,4))").expectResult(s("call:4"));
	}

	private class TestStructuredComposite implements MetaObject.SlotAttr {
		private final List<String> path;
		private final String prefix;

		public TestStructuredComposite(String prefix) {
			this.path = ImmutableList.of();
			this.prefix = Strings.nullToEmpty(prefix);
		}

		private TestStructuredComposite(String prefix, List<String> parentPath, String elem) {
			this.prefix = prefix;
			this.path = ImmutableList.<String> builder().addAll(parentPath).add(elem).build();
		}

		@Override
		public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
			if (key.equals("path")) return Optional.of(domain.create(String.class, Joiner.on("/").join(path)));
			else if (!key.startsWith(prefix)) return Optional.absent();
			else return Optional.of(domain.create(DummyObject.class, DUMMY,
					MetaObject.builder()
							.set(new TestStructuredComposite(prefix, path, key))
							.build()));
		}
	}

	@Test
	public void testDotOperator() {
		sut.environment.setGlobalSymbol("root", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructuredComposite("")).build()));

		infix("root.path").expectResult(s(""));

		prefix("(. root path)").expectResult(s(""));

		infix("root.a.path").expectResult(s("a"));
		prefix("(. root a path)").expectResult(s("a"));

		infix("root.a.b.path").expectResult(s("a/b"));

		infix("root.'a'.path").expectResult(s("a"));
		prefix("(. root 'a' path)").expectResult(s("a"));

		infix("root.('a').path").expectResult(s("a"));

		infix("(root.a).b.path").expectResult(s("a/b"));
		prefix("(. (. root a) b path)").expectResult(s("a/b"));
	}

	@Test
	public void testDotOperatorWithCustomExprSymbolGet() {
		sut.environment.setGlobalSymbol("root", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructuredComposite("")).build()));
		// 'code' has special transition, but nothing should happen since it compiles to SymbolGet here
		infix("root.code.path").expectResult(s("code"));
	}

	@Test
	public void testDotOperatorWithExpressionResult() {
		sut.environment.setGlobalSymbol("root", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructuredComposite("")).build()));

		infix("let([key = 'a'], root.(key).path)").expectResult(s("a"));
		infix("let([key -> 'a'], root.(key()).path)").expectResult(s("a"));
	}

	@Test
	public void testObjectIndexingWithBrackets() {
		sut.environment.setGlobalSymbol("test", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructuredComposite("")).build()));

		infix("test['path']").expectResult(s(""));

		infix("test['a']['b']['path']").expectResult(s("a/b"));

		infix("test.a['b']['path']").expectResult(s("a/b"));
		infix("test['a'].b['path']").expectResult(s("a/b"));
		infix("test['a']['b'].path").expectResult(s("a/b"));

		infix("test.a['b'].path").expectResult(s("a/b"));
		infix("test.a.b['path']").expectResult(s("a/b"));
		infix("test['a'].b.path").expectResult(s("a/b"));

		infix("test.'a'['b'].path").expectResult(s("a/b"));
		infix("test.('a')['b'].path").expectResult(s("a/b"));
		infix("test.('a')['b']['path']").expectResult(s("a/b"));

		infix("test.(str('b')).path").expectResult(s("b"));
		infix("test.((str)('b')).path").expectResult(s("b"));
		infix("test.a.((str)('b')).path").expectResult(s("a/b"));

		sut.environment.setGlobalSymbol("key", s("a"));
		infix("test.(key)['b'].path").expectResult(s("a/b"));
	}

	private class TestStructuredCompositeWithCallableReturn implements MetaObject.SlotAttr {
		@Override
		public Optional<TypedValue> attr(TypedValue self, final String key, Frame<TypedValue> frame) {
			return Optional.of(domain.create(DummyObject.class, DUMMY,
					MetaObject.builder().set(MetaObjectUtils.callableAdapter(new UnaryFunction.Direct<TypedValue>() {
						@Override
						protected TypedValue call(TypedValue value) {
							final String result = key + ":" + value.as(String.class);
							return domain.create(String.class, result);
						}
					})).build()));
		}
	}

	@Test
	public void testDotOperatorResultCall() {
		sut.environment.setGlobalSymbol("test", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructuredCompositeWithCallableReturn()).build()));

		infix("test.a('b')").expectResult(s("a:b"));
		infix("test.a('c')").expectResult(s("a:c"));
		infix("test.a('c')[0:-1]").expectResult(s("a:"));

		infix("test.'b'('c')").expectResult(s("b:c"));
		infix("test.('b')('c')").expectResult(s("b:c"));

		infix("test['c']('d')").expectResult(s("c:d"));
		infix("test['c']('d')[0:-1]").expectResult(s("c:")); // last bracket is totally unrelated, but... meh

		sut.environment.setGlobalSymbol("key", s("a"));
		infix("test.(key)('b')").expectResult(s("a:b"));
	}

	private class CallableLoggerStruct implements MetaObject.SlotAttr {
		@Override
		public Optional<TypedValue> attr(TypedValue self, final String key, Frame<TypedValue> frame) {
			return Optional.of(domain.create(DummyObject.class, DUMMY,
					MetaObject.builder()
							.set(new MetaObject.SlotCall() {
								@Override
								public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
									final Stack<TypedValue> substack = frame.stack().substack(argumentsCount.get());
									final List<String> result = Lists.newArrayList();
									for (TypedValue v : substack)
										result.add(v.as(String.class));
									substack.clear();
									final String args = Joiner.on(",").join(result);
									substack.push(domain.create(String.class, key + "(" + args + ")"));
								}
							})
							.build()));
		}
	}

	private void testCustomSymbolCall(String symbol) {
		sut.environment.setGlobalSymbol("test", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new CallableLoggerStruct()).build()));

		infix("test." + symbol + "()").expectResult(s(symbol + "()"));
		infix("test." + symbol + "('a' + 'b' * 3)").expectResult(s(symbol + "(abbb)"));
		infix("test." + symbol + "('a' * 2, 'b' * 4)").expectResult(s(symbol + "(aa,bbbb)"));
	}

	@Test
	public void testDotOperatorWithCustomExprSymbolCall() {

		// following symbols have custom state transition and symbol, but it works with dot as long as it extends SymbolCallNode

		testCustomSymbolCall(TypedCalcConstants.SYMBOL_CODE);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_IF);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_LET);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_LETSEQ);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_LETREC);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_DELAY);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_MATCH);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_AND_THEN);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_OR_ELSE);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_NON_NULL);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_CONSTANT);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_ALT);
		testCustomSymbolCall(TypedCalcConstants.SYMBOL_DO);
	}

	private class TestStructuredCompositeWithCompositeReturn implements MetaObject.SlotAttr {
		@Override
		public Optional<TypedValue> attr(TypedValue self, final String key, Frame<TypedValue> frame) {
			return Optional.of(domain.create(DummyObject.class, DUMMY,
					MetaObject.builder().set(MetaObjectUtils.callableAdapter(new UnaryFunction.Direct<TypedValue>() {
						@Override
						protected TypedValue call(TypedValue value) {
							final String path = key + ":" + value.as(String.class);
							class InnerAttrSlot implements MetaObject.SlotAttr {
								@Override
								public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
									return Optional.of(domain.create(String.class, path + ":" + key));
								}
							}
							return domain.create(DummyObject.class, DUMMY,
									MetaObject.builder().set(new InnerAttrSlot()).build());
						}
					})).build()));
		}
	}

	@Test
	public void testNestedDotOperatorOnDotResultCall() {
		// just some crazy brackets
		sut.environment.setGlobalSymbol("test", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructuredCompositeWithCompositeReturn()).build()));

		infix("test.b('c').d").expectResult(s("b:c:d"));

		infix("test.'b'('c').d").expectResult(s("b:c:d"));
		infix("test.'b'('c').('d')").expectResult(s("b:c:d"));
		infix("test.'b'('c')['d']").expectResult(s("b:c:d"));

		infix("test['b']('c').d").expectResult(s("b:c:d"));
		infix("test['b']('c').('d')").expectResult(s("b:c:d"));
		infix("test['b']('c')['d']").expectResult(s("b:c:d"));
	}

	@Test
	public void testCompositeWithSyntax() {
		sut.environment.setGlobalSymbol("test", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructuredComposite("m_")).build()));

		infix("test.m_a.m_b.{ path }").expectResult(s("m_a/m_b"));
		infix("test.m_a.m_b.{ m_c }.path").expectResult(s("m_a/m_b/m_c"));
		infix("test.{ m_a }.m_b.m_c.path").expectResult(s("m_a/m_b/m_c"));
		infix("test.{ m_a.m_b }.m_c.path").expectResult(s("m_a/m_b/m_c"));

		infix("test.m_a.m_b.{ cons(path, m_c.path) }").expectResult(cons(s("m_a/m_b"), s("m_a/m_b/m_c")));
	}

	@Test
	public void testIndexableComposite() {
		class TestIndexableComposite implements MetaObject.SlotSlice {
			private int count;

			@Override
			public TypedValue slice(TypedValue self, TypedValue range, Frame<TypedValue> frame) {
				return cons(i(count++), range);
			}

		}

		final TypedValue test = domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestIndexableComposite()).build());

		sut.environment.setGlobalSymbol("test", test);

		infix("test['ab']").expectResult(cons(i(0), s("ab")));
		infix("test[4.01]").expectResult(cons(i(1), d(4.01)));
		infix("test[test]").expectResult(cons(i(2), test));

		infix("cdr(test[test])[-2]").expectResult(cons(i(4), i(-2)));
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
		postfix("#a type @symbol == ").expectResult(b(true));
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
		infix("type(list(1,2,3)) == cons").expectResult(b(true));
		infix("list(1,2,3) == cons(1, cons(2, cons(3, null)))").expectResult(b(true));
		prefix("(== (list 1,2,3) #(1 2 3))").expectResult(b(true));
		infix("car(cons(1, 2))").expectResult(i(1));
		infix("cdr(cons(1, 2))").expectResult(i(2));
	}

	@Test
	public void testListBrackets() {
		infix("[]").expectResult(nil());
		infix("[1]").expectResult(cons(i(1), nil()));
		infix("[1,2,3]").expectResult(cons(i(1), cons(i(2), cons(i(3), nil()))));
		infix("[1,2,3] == cons(1, cons(2, cons(3, null)))").expectResult(b(true));
		prefix("(==  [1 2 3] #(1 2 3))").expectResult(b(true));
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

		infix("(1:2:3:null) == list(1,2,3)").expectResult(b(true));
		prefix("(== (: 1 2 3 null) (list 1 2 3))").expectResult(b(true));

		infix("(1:2:3:4) == #(1 2 3 ... 4)").expectResult(b(true));
		prefix("(== (: 1 2 3 4) #(1 2 3 ... 4))").expectResult(b(true));
	}

	@Test
	public void testConstantEvaluatingBrackets() {
		final SymbolStub<TypedValue> stub = new SymbolStub<TypedValue>()
				.allowCalls()
				.expectArgs(i(1), i(2))
				.verifyArgCount()
				.setReturns(i(5), i(6), i(7))
				.verifyReturnCount();
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<TypedValue> expr = sut.compilers.compile(ExprType.POSTFIX, "[1 2 dummy$2,3]");
		stub.checkCallCount(1);
		compiled(expr).expectResults(i(5), i(6), i(7));
		stub.checkCallCount(1);
	}

	@Test
	public void testNestedConstantEvaluatingBrackets() {
		final SymbolStub<TypedValue> stub = new SymbolStub<TypedValue>()
				.allowGets()
				.setGetValue(i(5));
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<TypedValue> expr = sut.compilers.compile(ExprType.POSTFIX, "[4 [@dummy 3 -] *]");
		stub.checkGetCount(1);
		compiled(expr).expectResults(i(8));
		stub.checkGetCount(1);
	}

	@Test
	public void testConstantEvaluatingSymbol() {
		final SymbolStub<TypedValue> stub = new SymbolStub<TypedValue>()
				.allowCalls()
				.expectArgs(i(1), i(2))
				.verifyArgCount()
				.setReturns(i(5))
				.verifyReturnCount();
		sut.environment.setGlobalSymbol("dummy", stub);

		final IExecutable<TypedValue> expr = sut.compilers.compile(ExprType.INFIX, "9 + const(dummy(1,2) + 3)");
		stub.checkCallCount(1);

		compiled(expr).expectResults(i(17));
		stub.checkCallCount(1);

		compiled(expr).expectResults(i(17));
		stub.checkCallCount(1);
	}

	@Test
	public void testCodeParsingInPostfixParser() {
		postfix("{ 1 2 +} type @code ==").expectResult(b(true));

		postfix("{ 1 2 +} execute").expectResult(i(3));
		postfix("{ 1 2 +} execute$1,1").expectResult(i(3));

		postfix("{ 1 2 + 3 4 -} execute").expectResults(i(3), i(-1));
		postfix("{ 1 2 + 3 4 -} execute$1,2").expectResults(i(3), i(-1));
	}

	@Test
	public void testCodeSymbol() {
		infix("type(code(2 + 3)) == code").expectResult(TRUE);
		infix("type(code(2 + 3)).name == 'code'").expectResult(TRUE);

		infix("execute(code(2 + 3))").expectResult(i(5));
		prefix("(execute (code (+ 2 3)))").expectResult(i(5));
	}

	@Test
	public void testCodeBrackets() {
		infix("type({2 + 3}) == code").expectResult(TRUE);
		infix("execute({2 + 3})").expectResult(i(5));

		prefix("(execute {6})").expectResult(i(6));
		prefix("(execute {(+ 2 3)})").expectResult(i(5));
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
		final SymbolStub<TypedValue> leftStub = new SymbolStub<TypedValue>().setGetValue(i(2)).allowGets();
		final SymbolStub<TypedValue> rightStub = new SymbolStub<TypedValue>().setGetValue(i(3)).allowGets();

		sut.environment.setGlobalSymbol("left", leftStub);
		sut.environment.setGlobalSymbol("right", rightStub);

		infix("if(2 == 2, left, right)").expectResult(i(2));
		rightStub.checkGetCount(0).resetGetCount();
		leftStub.checkGetCount(1).resetGetCount();

		infix("if(2 != 2, left, right)").expectResult(i(3));
		rightStub.checkGetCount(1).resetGetCount();
		leftStub.checkGetCount(0).resetGetCount();
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

		infix("'abc'[1][0]").expectResult(s("b"));
		infix("'abc'[2][0][0]").expectResult(s("c"));

		infix("'abc'[0:3][0]").expectResult(s("a"));
		infix("'abc'[1:3][0]").expectResult(s("b"));
		infix("'abc'[2:3][0]").expectResult(s("c"));
	}

	@Test
	public void testStringSliceInExpressions() {
		sut.environment.setGlobalSymbol("test", s("abc"));

		infix("test[-1]").expectResult(s("c"));
		infix("test[-3] + test[-2] + test[-1]").expectResult(s("abc"));

		infix("('a' + 'b' + 'c')[0:3]").expectResult(s("abc"));
		infix("str(123)[2]").expectResult(s("3"));
	}

	@Test
	public void testLetExpressionBasicForm() {
		infix("let([x:2,y:3], x + y)").expectResult(i(5));
		infix("let([x:1+2,y:3], x + y)").expectResult(i(6));
		infix("let([x:1:2], x)").expectResult(cons(i(1), i(2)));
		prefix("(let [(:x 2) (:y 3)] (+ x y))").expectResult(i(5));
		prefix("(let [(:x (+ 1 2)) (:y 3)] (+ x y))").expectResult(i(6));
		prefix("(let [(:x (: 1 2))] x)").expectResult(cons(i(1), i(2)));
	}

	@Test
	public void testLetExpressionBasicFormWithLowPrioOperator() {
		infix("let([x = 2, y = 3], x + y)").expectResult(i(5));
		infix("let([x = 1+2,y = 3], x + y)").expectResult(i(6));
		infix("let([x = 1:2], x)").expectResult(cons(i(1), i(2)));
		prefix("(let [(= x 2) (= y 3)] (+ x y))").expectResult(i(5));
		prefix("(let [(= x (+ 1 2)) (= y 3)] (+ x y))").expectResult(i(6));
		prefix("(let [(= x (: 1 2))] x)").expectResult(cons(i(1), i(2)));
	}

	@Test
	public void testValueSymbolCall() {
		infix("let([x:2], x())").expectResult(i(2));
		infix("let([x:2], x)").expectResult(i(2));
		infix("let([x:2], x() == x)").expectResult(b(true));
	}

	@Test
	public void testNestedLet() {
		infix("let([x:2], let([y:x+2], x + y))").expectResult(i(6));
	}

	@Test
	public void testLetWithExplicitList() {
		infix("@let([#x:{2}, #y:{1+2}], {x+y})").expectResult(i(5));
		infix("let([l:[#x:{2}, #y:{1+2}]], @let(l, {x+y}))").expectResult(i(5));
	}

	@Test
	public void testLetInPostfix() {
		postfix("#x {1} : #y {2} : list$2 {x y +} let").expectResult(i(3));
	}

	@Test
	public void testLetScoping() {
		infix("let([x:2], let([y:x], let([x:3], y)))").expectResult(i(2));
		infix("let([x:5], let([x:2, y:x], cons(x, y)))").expectResult(cons(i(2), i(5)));
	}

	@Test
	public void testLetUnpacking() {
		infix("let([x:y = 1:2], x - y)").expectResult(i(-1));
		infix("let([x:y:z = 1:2:3], x - y + z)").expectResult(i(2));
		infix("let([x:_:z = 1:2:3], x - z)").expectResult(i(-2));
		infix("let([int(x):float(y) = 1:2.1], x - y)").expectResult(d(-1.1));
		infix("let([x:f = 2:((y)->x+y)], f(7))").expectResult(i(9));

		infix("let([f(int(x)) -> x + 2], f(5))").expectResult(i(7));
		infix("let([f(w:x,y:z) -> w-x:y-z], f(1:2,4:3))").expectResult(cons(i(-1), i(1)));
	}

	@Test
	public void testLetCompilationShortcut() {
		infix("let([x = 2], x)").expectSameAs(postfix("#x {2} : list$1,1 {@x} let$2,1"));
		infix("let([x:y = 1:2], x + y)").expectSameAs(postfix("{@x @y :} pattern$1,1 {1 2 :} : list$1,1 {@x @y +} let$2,1"));
	}

	@Test
	public void testFailedLetUnpacking() {
		infix("let([int(x) = 2.1], false)").expectThrow(RuntimeException.class);
		infix("let([x:y:z = 1:2], false)").expectThrow(RuntimeException.class);
	}

	@Test
	public void testLetSeq() {
		infix("letseq([x:2, y:x+3], y)").expectResult(i(5));

		infix("letseq([x:2, y->x+3], y())").expectResult(i(5));
		infix("letseq([x->2, y:x()+3], y)").expectResult(i(5));

		infix("let([z:9], letseq([x:2, y:x+z], y))").expectResult(i(11));
		infix("let([x:5], letseq([x:2, y:x+3], y))").expectResult(i(5));
		infix("let([x:5], letseq([y:x+3, x:2], y))").expectResult(i(8));
		infix("let([x:5], letseq([y:x+3, x:2, y:x+3], y))").expectResult(i(5));
	}

	@Test
	public void testLetSeqUnpacking() {
		infix("letseq([x:y = 1:2, f -> x - y], f())").expectResult(i(-1));
		infix("letseq([x:xs = 1:2:3, y:z = xs], x - y + z)").expectResult(i(2));

		infix("letseq([f(int(x)) -> x + 2], f(5))").expectResult(i(7));
		infix("letseq([f(w:x,y:z) -> w-x:y-z], f(1:2,4:3))").expectResult(cons(i(-1), i(1)));
	}

	@Test
	public void testLetSeqFailedUnpacking() {
		infix("letseq([x:xs = 1:2, y:z = xs], false)").expectThrow(RuntimeException.class);
		infix("letseq([f(int(x)) -> x], f('z'))").expectThrow(RuntimeException.class);
	}

	@Test(expected = ExecutionErrorException.class)
	public void testLetSeqSelfSymbolCall() {
		infix("letseq([x:2], letseq([x:x], x))").expectResult(i(2));
	}

	@Test
	public void testLetRec() {
		infix("letrec([odd(v)->if(v==0,false,even(v-1)), even(v)->if(v==0,true,odd(v-1))], even(6))").expectResult(TRUE);
		infix("letrec([odd(v)->if(v==0,false,even(v-1)), even(v)->if(v==0,true,odd(v-1))], odd(5))").expectResult(TRUE);
		infix("letrec([odd(v)->if(v==0,false,even(v-1)), even(v)->if(v==0,true,odd(v-1))], odd(4))").expectResult(FALSE);
		infix("letrec([odd(v)->if(v==0,false,even(v-1)), even(v)->if(v==0,true,odd(v-1))], even(3))").expectResult(FALSE);

		infix("letrec([x:2], letrec([y:x], x))").expectResult(i(2));
	}

	@Test
	public void testLetRecUnpacking() {
		infix("letrec([x:y = (() -> z() + 'x'):(() -> 'y'), w:z = (()->x() + 'w'):(() -> y() + 'z')], w())").expectResult(s("yzxw"));
	}

	@Test
	public void testLetRecFailedUnpacking() {
		infix("letrec([int(x) = z(), z -> 1.1], false)").expectThrow(RuntimeException.class);
	}

	@Test(expected = ExecutionErrorException.class)
	public void testLetRecSelfSymbolCall() {
		infix("letrec([x:2], letrec([x:x], x))").expectResult(i(2));
	}

	@Test(expected = ExecutionErrorException.class)
	public void testLetRecDefinedSymbolCallBeforeDefine() {
		infix("letrec([x:2], letrec([y:x, x:2], x))").expectResult(i(2));
	}

	@Test(expected = ExecutionErrorException.class)
	public void testLetRecDefinedSymbolCallAfterDefine() {
		infix("letrec([x:2], letrec([x:2, y:x], x))").expectResult(i(2));
	}

	@Test
	public void testCallableLetVariables() {
		infix("let([f:symbol],f('test1'))").expectResult(sym("test1"));
		infix("let([f:symbol],let([g:f], g('test2')))").expectResult(sym("test2"));
	}

	@Test
	public void testCallableApply() {
		infix("apply(symbol, 'test')").expectResult(sym("test"));
		infix("apply(max, 1, 3, 2)").expectResult(i(3));
	}

	@Test
	public void testCallableDefaultOpApply() {
		infix("(symbol)('test')").expectSameAs(infix("apply(symbol, 'test')")).expectResult(sym("test"));
		infix("(max)(1, 3, 2)").expectSameAs(infix("apply(max, 1, 3, 2)")).expectResult(i(3));
	}

	@Test
	public void testHighOrderCallable() {
		sut.environment.setGlobalSymbol("test", new BinaryFunction.Direct<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue left, TypedValue right) {
				final BigInteger closure = left.as(BigInteger.class).subtract(right.as(BigInteger.class));
				return CallableValue.wrap(domain, new UnaryFunction.Direct<TypedValue>() {
					@Override
					protected TypedValue call(TypedValue value) {
						final BigInteger arg = value.as(BigInteger.class);
						return domain.create(BigInteger.class, closure.multiply(arg));
					}

				});
			}
		});

		infix("test(1,2)(3)").expectResult(i(-3));
		infix("(test)(1,2)(3)").expectResult(i(-3));
		infix("let([f:test(1,2)], list(f(3), f(4), f(5)))").expectResult(list(i(-3), i(-4), i(-5)));
	}

	@Test
	public void testCallableApplyInPostfix() {
		postfix("@symbol 'test' apply$2,1").expectResult(sym("test"));
	}

	@Test
	public void testCallableOperatorWrappersInPostfix() {
		postfix("@! iscallable").expectResult(TRUE);
		postfix("@! false apply$2").expectResult(TRUE);

		postfix("@+ iscallable").expectResult(TRUE);
		postfix("@+ 1 2 apply$3").expectResult(i(3));
	}

	@Test
	public void testCallableOperatorWrappersInAstParsers() {
		infix("iscallable(@!)").expectResult(TRUE);
		prefix("(iscallable @&&)").expectResult(TRUE);

		infix("apply(@!, false)").expectResult(TRUE);
		prefix("(apply @neg 2)").expectResult(i(-2));

		infix("apply(@+, 1, 2)").expectResult(i(3));
		prefix("(apply @** 2 5)").expectResult(i(32));

		infix("@+(1, 2)").expectResult(i(3));

		infix("let([plus:@+], plus(12,34))").expectResult(i(46));
		prefix("(let [(:minus @-)] (minus 12 34))").expectResult(i(-22));
	}

	@Test
	public void testForcedSymbolGetApply() {
		infix("@if(true, {2+2}, {'else'})")
				.expectSameAs(infix("if(true, 2+2, 'else')"))
				.expectSameAs(prefix("(@if true {(+ 2 2)} {'else'})"))
				.expectSameAs(prefix("(if true (+ 2 2) 'else')"))
				.expectResult(i(4));
	}

	@Test
	public void testNullaryLambdaOperator() {
		infix("iscallable(()->2)").expectResult(TRUE);
		infix("iscallable([]->2)").expectResult(TRUE);

		infix("(()->2)()").expectResult(i(2));
		infix("([]->2)()").expectResult(i(2));
		infix("(()->{2})()").expectResult(i(2));
		prefix("(apply (-> [] 2))").expectResult(i(2));
	}

	@Test
	public void testUnaryLambdaOperator() {
		infix("iscallable(a->a+2)").expectResult(TRUE);

		infix("(a->a+2)(2)").expectResult(i(4));
		infix("(a->{a+2})(2)").expectResult(i(4));
		infix("((a)->a+2)(3)").expectResult(i(5));
		infix("([a]->a+2)(3)").expectResult(i(5));

		prefix("(apply (-> a (+ a 2)) 2)").expectResult(i(4));
		prefix("(apply (-> a {(+ a 2)}) 2)").expectResult(i(4));
		prefix("(apply (->[a] (+ a 2)) 2)").expectResult(i(4));
	}

	@Test
	public void testBinaryArgLambdaOperator() {
		infix("iscallable((a, b) -> a + b)").expectResult(TRUE);

		infix("((a, b)->a-b)(1, 2)").expectResult(i(-1));

		prefix("(apply (-> [a, b] (- a b)) 2 3)").expectResult(i(-1));
	}

	@Test
	public void testLambdaOperatorInLetScope() {
		infix("let([f = (a,b)->a+b], f(3, 4))").expectResult(i(7));
		prefix("(let [(= f (-> [a,b] (+ a b)))] (f 4 5))").expectResult(i(9));
	}

	@Test
	public void testLambdaUnpacking() {
		infix("(a:b-> a-b)(1:2)").expectResult(i(-1));
		infix("((a:b)-> a-b)(1:2)").expectResult(i(-1));

		infix("(int(a)-> a + 2)(5)").expectResult(i(7));
		infix("((int(a))-> a + 2)(5)").expectResult(i(7));

		infix("((a:b,c:d)-> a-b:c-d)(1:2,4:3)").expectResult(cons(i(-1), i(1)));
	}

	@Test
	public void testLambdaUnpackingFailue() {
		infix("((a:b)-> a-b)(5)").expectThrow(RuntimeException.class);
		infix("((a:b:c)-> a-b)(1:2)").expectThrow(RuntimeException.class);
		infix("((int(a))-> a)('z')").expectThrow(RuntimeException.class);
		infix("((_)-> _)('z')").expectThrow(RuntimeException.class); // wildchar is not bound
	}

	@Test
	public void testLambdaCompilation() {
		infix("(a)->a").expectSameAs(infix("a->a")).expectSameAs(postfix("#a list$1,1 {@a} closure$2,1"));
		infix("(a:b)->a+b").expectSameAs(infix("a:b->a+b")).expectSameAs(postfix("{@a @b :} pattern$1,1 list$1,1 {@a @b +} closure$2,1"));
	}

	@Test
	public void testNestedLambdaOperator() {
		infix("iscallable(a->b->a+b)").expectResult(TRUE);
		infix("iscallable((a->b->a+b)(5))").expectResult(TRUE);
		infix("(a->b->a+b)(5)(6)").expectResult(i(11));
	}

	@Test
	public void testLambdaScoping() {
		infix("let([a = 2], let([f = ()->a], let([a = 3], f())))").expectResult(i(2));
		infix("let([f = a->b->a+b], let([f1 = f(3)], let([a = 4], f1(10))))").expectResult(i(13));
	}

	@Test
	public void testHighOrderLambda() {
		infix("(a->a(1)+2)(a->a-4)").expectResult(i(-1));
		infix("let([f = a->a(3)+2], f(a->a-4))").expectResult(i(1));
	}

	@Test
	public void testFailSymbol() {
		infix("if(false, fail('welp'), 'ok')").expectResult(s("ok"));
		infix("if(true, fail('welp'), 'ok')").expectThrow(ExecutionErrorException.class, "\"welp\"");
		infix("if(true, fail(), 'ok')").expectThrow(ExecutionErrorException.class, null);
	}

	@Test
	public void testLambdaRecursion() {
		infix("let([f=n->if(n<=0,1,f(n-1)*n)], f(5))").expectResult(i(120));
		infix("let([f=n->if(n<=2,1,f(n-1)+f(n-2))], f(9))").expectResult(i(34));
	}

	@Test
	public void testGettingSymbolInDefinition() {
		infix("let([f:f], f)").expectThrow(ExecutionErrorException.class);
	}

	@Test
	public void testCallingSymbolInDefinition() {
		infix("let([f:f()], f)").expectThrow(ExecutionErrorException.class);
	}

	@Test
	public void testLetFunctionDefinition() {
		infix("let([a() -> -5], a())").expectResult(i(-5));
		infix("let([a -> -5], a())").expectResult(i(-5));

		infix("let([a(b,c) -> b-c], a(1,2))").expectResult(i(-1));

		infix("let([f(n)->if(n<=0,1,f(n-1)*n)], f(6))").expectResult(i(720));
	}

	@Test
	public void testPromiseSyntax() {
		infix("let([p:delay(2)], iscallable(p))").expectResult(TRUE);

		infix("let([p:delay(1 + 2)], force(p))").expectResult(i(3));
		infix("let([p:delay(1 + 2)], p())").expectResult(i(3));
	}

	@Test
	public void testPromiseScoping() {
		infix("let([a:1], let([p:delay(a + 2)], let([a:3], force(p))))").expectResult(i(3));
	}

	@Test
	public void testPromiseContact() {
		final SymbolStub<TypedValue> stub = new SymbolStub<TypedValue>().setGetValue(i(2)).allowGets();
		sut.environment.setGlobalSymbol("test", stub);

		final Stack<TypedValue> resultStack = infix("let([p:delay(test + 1)], p)").executeAndGetStack();
		Assert.assertEquals(1, resultStack.size());
		stub.checkGetCount(0);

		sut.environment.setGlobalSymbol("promise", resultStack.pop());
		infix("force(promise)").expectResult(i(3));
		stub.checkGetCount(1);

		infix("force(promise)").expectResult(i(3));
		stub.checkGetCount(1);
	}

	@Test
	public void testSimpleValueMatch() {
		infix("match(('a') -> 1, (2) -> 'b')('a')").expectResult(i(1));
		infix("match(('a') -> 1, (2) -> 'b')(2)").expectResult(s("b"));
	}

	@Test
	public void testMatchWithGlobalSymbols() {
		infix("match((true) -> 1, (false) -> 0, (null) -> -1)(true)").expectResult(i(1));
		infix("match((true) -> 1, (false) -> 0, (null) -> -1)(false)").expectResult(i(0));
		infix("match((true) -> 1, (false) -> 0, (null) -> -1)(null)").expectResult(i(-1));
	}

	@Test
	public void testWildcardMatch() {
		infix("match(('a') -> 1, (_) -> '?')('a')").expectResult(i(1));
		infix("match(('a') -> 1, (_) -> '?')('dummy')").expectResult(s("?"));
	}

	@Test
	public void testBindMatch() {
		infix("match((a) -> a)('b')").expectResult(s("b"));
		infix("match((a) -> a + 1)(2)").expectResult(i(3));
		infix("match((a) -> 'd')(2)").expectResult(s("d"));
	}

	@Test
	public void testSimpleListMatch() {
		infix("match((1:2:3) -> 'ok', (_) -> 'fail')(1:2:3)").expectResult(s("ok"));
		infix("match((1:2:3) -> 'ok', (_) -> 'fail')(1:2:4)").expectResult(s("fail"));
		infix("match((1:2:3) -> 'ok', (_) -> 'fail')(1:2)").expectResult(s("fail"));
		infix("match((1:2:3) -> 'ok', (_) -> 'fail')(1:2:3:4)").expectResult(s("fail"));
	}

	@Test
	public void testSimpleTerminatedListMatch() {
		infix("match(([1,2,3]) -> 'ok', (_) -> 'fail')([1,2,3])").expectResult(s("ok"));
		infix("match(([1,2,3]) -> 'ok', (_) -> 'fail')(1:2:3:null)").expectResult(s("ok"));
		infix("match(([1,2,3]) -> 'ok', (_) -> 'fail')(1:2:3)").expectResult(s("fail"));
	}

	@Test
	public void testEmptyListMatch() {
		infix("match(([]) -> 'ok', (_) -> 'fail')([])").expectResult(s("ok"));
		infix("match(([]) -> 'ok', (_) -> 'fail')(null)").expectResult(s("ok"));
		infix("match(([]) -> 'ok', (_) -> 'fail')([1])").expectResult(s("fail"));
	}

	@Test
	public void testListMatchWithWildcard() {
		infix("match((1:2:_) -> 'ok', (_) -> 'fail')(1:2:3)").expectResult(s("ok"));
		infix("match((1:2:_) -> 'ok', (_) -> 'fail')(1:2:4)").expectResult(s("ok"));
		infix("match((1:2:_) -> 'ok', (_) -> 'fail')(1:2)").expectResult(s("fail"));
		infix("match((1:2:_) -> 'ok', (_) -> 'fail')(1:2:3:4)").expectResult(s("ok"));
	}

	@Test
	public void testNestedListMatch() {
		infix("match((1:(2:3):4) -> 'ok', (_) -> 'fail')(1:(2:3):4)").expectResult(s("ok"));
		infix("match((1:(2:3):4) -> 'ok', (_) -> 'fail')(1:2:3:4)").expectResult(s("fail"));
	}

	@Test
	public void testListMatchWithBinding() {
		infix("match((1:2:xs) -> xs, (_) -> 'fail')(1:2:3)").expectResult(i(3));
		infix("match((1:2:xs) -> xs, (_) -> 'fail')(1:2:4)").expectResult(i(4));
		infix("match((1:2:xs) -> xs, (_) -> 'fail')(1:2)").expectResult(s("fail"));
		infix("match((1:2:xs) -> xs, (_) -> 'fail')(1:2:3:4)").expectResult(cons(i(3), i(4)));
	}

	@Test
	public void testNestedListMatchWithBinding() {
		infix("match((1:(2:a):b) -> a:b, (_) -> 'fail')(1:(2:3):4)").expectResult(cons(i(3), i(4)));
		infix("match((1:(2:a):b) -> a:b, (_) -> 'fail')(1:(2:3:5):4)").expectResult(cons(cons(i(3), i(5)), i(4)));
		infix("match((1:(2:a):b) -> a:b, (_) -> 'fail')(1:(2:3:5):4:6)").expectResult(cons(cons(i(3), i(5)), cons(i(4), i(6))));
	}

	@Test
	public void testMatchScope() {
		infix("let([a:2], match((a) -> a)(5))").expectResult(i(5));
		infix("let([b:2], match((a) -> b)(5))").expectResult(i(2));

		infix("let([x: 'pre'], let([b: match((1) -> x, (_) -> false)], let([x: 'post'], b(1))))").expectResult(s("pre"));
	}

	@Test
	public void testRecursiveMatch() {
		infix("letrec([f(l, c) -> match((x:xs) -> f(xs, c+1), ([]) -> c)(l)], f([], 0))").expectResult(i(0));
		infix("letrec([f(l, c) -> match((x:xs) -> f(xs, c+1), ([]) -> c)(l)], f([1], 0))").expectResult(i(1));
		infix("letrec([f(l, c) -> match((x:xs) -> f(xs, c+1), ([]) -> c)(l)], f([1,2,3], 0))").expectResult(i(3));
	}

	@Test
	public void testGuardedMatchWithNoDefaults() {
		infix("match((a) \\ a > 2 -> a \\ a < 2 -> -a)(3)").expectResult(i(3));
		infix("match((a) \\ a > 2 -> a \\ a < 2 -> -a)(1)").expectResult(i(-1));
	}

	@Test
	public void testGuardedMatchWithDefaultOnly() {
		infix("match((a) \\ 2)(5)").expectResult(i(2));
		infix("match((a) \\ a + 1)(5)").expectResult(i(6));
	}

	@Test
	public void testGuardedMatchWithDefault() {
		infix("match((a) \\ a > 5 -> 5\\ a)(6)").expectResult(i(5));
		infix("match((a) \\ a > 5 -> 5\\ a)(5)").expectResult(i(5));
		infix("match((a) \\ a > 5 -> 5\\ a)(4)").expectResult(i(4));
	}

	@Test
	public void testGuardedMatchWithoutValidClause() {
		infix("match((a) \\ a > 5 -> 5)(3)").expectThrow(RuntimeException.class);
	}

	@Test(expected = IllegalStateException.class)
	public void testGuardedMatchWithDefaultInMiddle() {
		infix("match((a) \\ a \\ a > 5 -> 5)(3)");
	}

	@Test
	public void testGuardedMatchOrder() {
		infix("match((a) \\ a > 10 -> 10 \\ a > 5 -> 5 \\ a)(11)").expectResult(i(10));
		infix("match((a) \\ a > 10 -> 10 \\ a > 5 -> 5 \\ a)(10)").expectResult(i(5));
		infix("match((a) \\ a > 10 -> 10 \\ a > 5 -> 5 \\ a)(4)").expectResult(i(4));

		infix("match((a) \\ a > 5 -> 5 \\ a > 10 -> 10 \\ a)(11)").expectResult(i(5));

		infix("match((a) \\ true -> 'first' \\ true -> 'second' \\ 'third')('whatever')").expectResult(s("first"));
	}

	@Test
	public void testGuardedMatchScope() {
		infix("let([r=2,l=3], let([f = match((a) \\ a > l -> r \\ a)], let([r=8,l=10], f(4))))").expectResult(i(2));
	}

	private class TestDeconstructor implements MetaObject.SlotDecompose {
		@Override
		public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
			final List<TypedValue> result = Lists.newArrayList();
			for (int i = 0; i < variableCount; i++)
				result.add(input);

			return Optional.of(result);
		}
	}

	public TypedValue createTestDeconstructor() {
		return domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestDeconstructor()).build());
	}

	@Test
	public void testDeconstructingMatchWithConstants() {
		sut.environment.setGlobalSymbol("Test", createTestDeconstructor());

		infix("match((Test(1)) -> 'left', (Test(2, b)) -> 'right')(1)").expectResult(s("left"));
		infix("match((Test(1)) -> 'left', (Test(2, b)) -> 'right')(2)").expectResult(s("right"));
	}

	@Test
	public void testDeconstructingMatchWithVarBinding() {
		sut.environment.setGlobalSymbol("Test", createTestDeconstructor());

		infix("match((Test(a)) -> a + 3)(2)").expectResult(i(5));
		infix("match((Test(a, b)) -> a + b)(2)").expectResult(i(4));

		infix("match((Test(1, a)) -> a + 1, (Test(2, b)) -> b + 2)(1)").expectResult(i(2));
		infix("match((Test(1, a)) -> a + 1, (Test(2, b)) -> b + 2)(2)").expectResult(i(4));

		infix("match((Test(1, a)) -> a + 1, (Test(2, b)) -> b + 2)(2)").expectResult(i(4));
	}

	@Test
	public void testDeconstructingMatchWithConsVarBinding() {
		sut.environment.setGlobalSymbol("Test", createTestDeconstructor());

		infix("match((Test(1:a)) -> cons('left', a), (Test(2:a)) -> cons('right', a))(1:5)").expectResult(cons(s("left"), i(5)));
		infix("match((Test(1:a)) -> cons('left', a), (Test(2:a)) -> cons('right', a))(2:7)").expectResult(cons(s("right"), i(7)));
	}

	@Test
	public void testNestedDeconstructingMatch() {
		sut.environment.setGlobalSymbol("Test", createTestDeconstructor());

		infix("match((Test(Test(a), Test(b))) -> cons(a + 1, b + 2))(6)").expectResult(cons(i(7), i(8)));
	}

	@Test
	public void testMultipleArgMatcherSameCount() {
		infix("match((1,2) -> 'left', (3,4) -> 'right', (_,_) -> 'other')(1,2)").expectResult(s("left"));
		infix("match((1,2) -> 'left', (3,4) -> 'right', (_,_) -> 'other')(3,4)").expectResult(s("right"));
		infix("match((1,2) -> 'left', (3,4) -> 'right', (_,_) -> 'other')(1,4)").expectResult(s("other"));
	}

	@Test
	public void testMultipleArgMatcherBinding() {
		infix("match((1,a) -> 'left':a, (a,1) -> 'right':a, (a,b) -> a:b)(1,2)").expectResult(cons(s("left"), i(2)));
		infix("match((1,a) -> 'left':a, (a,1) -> 'right':a, (a,b) -> a:b)(3,1)").expectResult(cons(s("right"), i(3)));
		infix("match((1,a) -> 'left':a, (a,1) -> 'right':a, (a,b) -> a:b)(3,2)").expectResult(cons(i(3), i(2)));
	}

	@Test
	public void testMultipleArgMatcherBindingWithGuards() {
		infix("match((1,a) \\ a > 2 -> 'left', (a,b) \\ a < b -> 'right', (_,_) -> 'other')(1,3)").expectResult(s("left"));
		infix("match((1,a) \\ a > 2 -> 'left', (a,b) \\ a < b -> 'right', (_,_) -> 'other')(1,2)").expectResult(s("right"));
		infix("match((1,a) \\ a > 2 -> 'left', (a,b) \\ a < b -> 'right', (_,_) -> 'other')(1,1)").expectResult(s("other"));
	}

	@Test
	public void testMultipleArgMatcherWithDifferentArgCount() {
		infix("match(() -> 0, (_) -> 1, (_, _) -> 2, (_, _, _) -> 3)()").expectResult(i(0));
		infix("match(() -> 0, (_) -> 1, (_, _) -> 2, (_, _, _) -> 3)('a')").expectResult(i(1));
		infix("match(() -> 0, (_) -> 1, (_, _) -> 2, (_, _, _) -> 3)('a','b')").expectResult(i(2));
		infix("match(() -> 0, (_) -> 1, (_, _) -> 2, (_, _, _) -> 3)('a','b','c')").expectResult(i(3));
	}

	@Test
	public void testDeconstructingMatchDifferentConstructors() {
		class SingleValueDeconstructor implements MetaObject.SlotDecompose {
			private final TypedValue pattern;

			public SingleValueDeconstructor(TypedValue pattern) {
				this.pattern = pattern;
			}

			@Override
			public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
				if (!input.equals(pattern)) return Optional.absent();

				final List<TypedValue> result = Lists.newArrayList();
				for (int i = 0; i < variableCount; i++)
					result.add(input);

				return Optional.of(result);
			}
		}

		sut.environment.setGlobalSymbol("Test2", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new SingleValueDeconstructor(i(2))).build()));

		sut.environment.setGlobalSymbol("Test3", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new SingleValueDeconstructor(i(3))).build()));

		infix("match((Test2(a)) -> cons('left', a), (Test3(a)) -> cons('right', a))(2)").expectResult(cons(s("left"), i(2)));
		infix("match((Test2(a)) -> cons('left', a), (Test3(a)) -> cons('right', a))(3)").expectResult(cons(s("right"), i(3)));

		infix("match((Test2(a, b)) -> cons(a, b), (Test3(a, b)) -> cons(a, b))(2)").expectResult(cons(i(2), i(2)));
		infix("match((Test2(a, b)) -> cons(a, b), (Test3(a, b)) -> cons(a, b))(3)").expectResult(cons(i(3), i(3)));
	}

	@Test
	public void testPrimitiveTypeMatch() {
		infix("match((int(x)) -> 'int':x, (str(x)) -> 'str':x, (bool(x)) -> 'bool':x, (float(x)) -> 'float':x)(5)").expectResult(cons(s("int"), i(5)));
		infix("match((int(x)) -> 'int':x, (str(x)) -> 'str':x, (bool(x)) -> 'bool':x, (float(x)) -> 'float':x)(5.0)").expectResult(cons(s("float"), d(5.0)));
		infix("match((int(x)) -> 'int':x, (str(x)) -> 'str':x, (bool(x)) -> 'bool':x, (float(x)) -> 'float':x)('abc')").expectResult(cons(s("str"), s("abc")));
		infix("match((int(x)) -> 'int':x, (str(x)) -> 'str':x, (bool(x)) -> 'bool':x, (float(x)) -> 'float':x)(true)").expectResult(cons(s("bool"), TRUE));
	}

	@Test
	public void testAltTypes() {
		infix("alt([Maybe=Just(x) \\ Nothing], match((Just(y)) -> y + 1, (Nothing()) -> 'nope')(Just(2)))").expectResult(i(3));
		infix("alt([Maybe=Just(x) \\ Nothing], match((Just(y)) -> y + 1, (Nothing()) -> 'nope')(Nothing()))").expectResult(s("nope"));

		infix("alt([Maybe=Just(x) \\ Nothing], match((Maybe.Just(y)) -> y + 1, (Maybe.Nothing()) -> 'nope')(Maybe.Just(2)))").expectResult(i(3));
		infix("alt([Maybe=Just(x) \\ Nothing], match((Maybe.Just(y)) -> y + 1, (Maybe.Nothing()) -> 'nope')(Maybe.Nothing()))").expectResult(s("nope"));

		infix("alt([Tree=Leaf(value)\\Node(left, right)], letrec([f = match((Leaf(v)) -> v, (Node(l,r)) -> f(l) + f(r))], f(Node(Node(Leaf(1), Leaf(4)), Leaf(6)))))").expectResult(i(11));

		infix("alt([Tree=Leaf(value)\\Node(left, right)], Leaf.fields)").expectResult(list(s("value")));
		infix("alt([Tree=Leaf(value)\\Node(left, right)], Node.fields)").expectResult(list(s("left"), s("right")));

		infix("alt([Tree=Leaf(value)\\Node(left, right)], Node(1,2).left)").expectResult(i(1));
		infix("alt([Tree=Leaf(value)\\Node(left, right)], Node(1,2).right)").expectResult(i(2));
		infix("alt([Tree=Leaf(value)\\Node(left, right)], Leaf(3).value)").expectResult(i(3));
	}

	@Test
	public void testAltTypesSameTypeComparision() {
		infix("alt([Maybe=Just(x) \\ Nothing], Just(1) == Just(1))").expectResult(TRUE);
		infix("alt([Maybe=Just(x) \\ Nothing], Just(1) != Just(5))").expectResult(TRUE);

		infix("alt([Maybe=Just(x) \\ Nothing], Nothing() == Nothing())").expectResult(TRUE);
		infix("alt([Maybe=Just(x) \\ Nothing], Just(1) != Nothing())").expectResult(TRUE);
	}

	@Test
	public void testAltTypesMetaobject() {
		infix("alt([Maybe=Just(x) \\ Nothing], getmetaobject(Just(1)) == getmetaobject(Just(2)))").expectResult(TRUE);
	}

	@Test
	public void testAltTypesTyping() {
		infix("alt([Maybe=Just(x) \\ Nothing], type(Just(1)) == type(Nothing()))").expectResult(TRUE);
		infix("alt([Maybe=Just(x) \\ Nothing], type(Just(1)) == type(Just(2)))").expectResult(TRUE);
		infix("alt([Maybe=Just(x) \\ Nothing], type(Nothing()) == type(Nothing()))").expectResult(TRUE);

		infix("alt([Maybe=Just(x) \\ Nothing], match((Maybe(x)) -> 'maybe', (_) -> 'other')(Just(1)))").expectResult(s("maybe"));
		infix("alt([Maybe=Just(x) \\ Nothing], match((Maybe(x)) -> 'maybe', (_) -> 'other')(Nothing()))").expectResult(s("maybe"));
		infix("alt([Maybe=Just(x) \\ Nothing], match((Maybe(x)) -> 'maybe', (_) -> 'other')(5))").expectResult(s("other"));
	}

	@Test
	public void testAltTypesDifferentTypeSameNameComparision() {
		infix("alt([Alt=V], let([tmp = V()], alt([Alt=V], tmp != V())))").expectResult(TRUE);
		infix("alt([Alt=V], let([tmp = V()], alt([Alt=V], type(tmp) != type(V()))))").expectResult(TRUE);
	}

	@Test
	public void testManualAltTypesDefinition() {
		infix("@alt([#V:[#A1:[#x], #A2:[#x,#y]]], {A1(2) == A1(2) && A2(1,2) == A2(1,2)})").expectResult(TRUE);
		infix("@alt([#V:[#A1:[#x], #A2:[#x,#y]]], {type(A1(2)) == type(A2(1,2))})").expectResult(TRUE);
	}

	@Test
	public void testInfixStringInterpolation() {
		infix("let([a:2, b:'test'], $'a = {a}, b = {b}')").expectResult(s("a = 2, b = test"));
	}

	@Test
	public void testPrefixStringInterpolation() {
		prefix("(let [(: a 2) (: b 'test')] $'a = {a}, b = {b}')").expectResult(s("a = 2, b = test"));
	}

	@Test
	public void testPosfixStringInterpolation() {
		postfix("#a {2} : #b {'test'} : list$2  {$'a = {a}, b = {b}'} let$2").expectResult(s("a = 2, b = test"));
	}

	@Test
	public void testFormatOperator() {
		infix("'hello %s' % 'world'").expectResult(s("hello world"));
		infix("'hello %s' % ['world']").expectResult(s("hello world"));
		infix("'%s %s' % ['hello', 'world']").expectResult(s("hello world"));

		infix("'2 + 2 = %d' % (2 + 2)").expectResult(s("2 + 2 = 4"));
		infix("'1 / 2 = %f, 1 / 4 = %f' % [1 / 2, 1 / 4]").expectResult(s(String.format("1 / 2 = %f, 1 / 4 = %f", 0.5, 0.25)));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testAssignOperatorCompileFail() {
		infix("a = b = 2");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testSplitterCompileFail() {
		infix("a \\ 2");
	}

	private SymbolStub<TypedValue> createConstFunction(String name, TypedValue value) {
		SymbolStub<TypedValue> result = new SymbolStub<TypedValue>().allowCalls().setReturns(value);
		sut.environment.setGlobalSymbol(name, result);
		return result;
	}

	private void assertAllCalled(String function, TypedValue a, TypedValue b, TypedValue c, TypedValue result) {
		final SymbolStub<TypedValue> stubA = createConstFunction("a", a);
		final SymbolStub<TypedValue> stubB = createConstFunction("b", b);
		final SymbolStub<TypedValue> stubC = createConstFunction("c", c);

		infix(String.format("%s(a(), b(), c())", function)).expectResult(result);

		stubA.checkCallCount(1);
		stubB.checkCallCount(1);
		stubC.checkCallCount(1);
	}

	@Test
	public void testEagerLogic() {
		infix("and()").expectResult(NULL);
		infix("and(5)").expectResult(i(5));

		infix("or()").expectResult(NULL);
		infix("or(6)").expectResult(i(6));

		assertAllCalled("and", i(1), i(2), i(3), i(3));
		assertAllCalled("and", i(1), i(2), TRUE, TRUE);

		assertAllCalled("and", i(1), i(2), i(0), i(0));
		assertAllCalled("and", i(1), i(0), NULL, i(0));

		assertAllCalled("and", i(1), i(0), i(2), i(0));
		assertAllCalled("and", i(1), FALSE, i(2), FALSE);

		assertAllCalled("or", i(0), NULL, FALSE, FALSE);
		assertAllCalled("or", i(0), NULL, i(1), i(1));

		assertAllCalled("or", i(0), TRUE, FALSE, TRUE);
		assertAllCalled("or", i(1), TRUE, FALSE, i(1));
	}

	private void assertFirstFewCalledPostfix(String function, TypedValue a, TypedValue b, TypedValue c, TypedValue result, int argsCalled) {
		final SymbolStub<TypedValue> stubA = createConstFunction("a", a);
		final SymbolStub<TypedValue> stubB = createConstFunction("b", b);
		final SymbolStub<TypedValue> stubC = createConstFunction("c", c);

		postfix(String.format("{a} {b} {c} %s$3", function)).expectResult(result);

		stubA.checkCallCount(argsCalled > 0? 1 : 0);
		stubB.checkCallCount(argsCalled > 1? 1 : 0);
		stubC.checkCallCount(argsCalled > 2? 1 : 0);
	}

	@Test
	public void testShortCuttingLogicInPosfix() {
		postfix("andthen$0").expectResult(NULL);
		postfix("{5} andthen$1").expectResult(i(5));

		postfix("orelse$0").expectResult(NULL);
		postfix("{6} orelse$1").expectResult(i(6));

		final int firstArgEvaluated = 1;
		final int firstTwoArgsEvaluated = 2;
		final int allArgsEvaluated = 3;

		assertFirstFewCalledPostfix("andthen", i(1), i(2), i(3), i(3), allArgsEvaluated);
		assertFirstFewCalledPostfix("andthen", i(1), i(2), TRUE, TRUE, allArgsEvaluated);

		assertFirstFewCalledPostfix("andthen", i(1), i(2), i(0), i(0), allArgsEvaluated);
		assertFirstFewCalledPostfix("andthen", i(1), i(0), NULL, i(0), firstTwoArgsEvaluated);

		assertFirstFewCalledPostfix("andthen", i(1), i(0), i(2), i(0), firstTwoArgsEvaluated);
		assertFirstFewCalledPostfix("andthen", i(1), FALSE, i(2), FALSE, firstTwoArgsEvaluated);

		assertFirstFewCalledPostfix("andthen", i(0), FALSE, i(2), i(0), firstArgEvaluated);

		assertFirstFewCalledPostfix("orelse", i(0), NULL, FALSE, FALSE, allArgsEvaluated);
		assertFirstFewCalledPostfix("orelse", i(0), NULL, i(1), i(1), allArgsEvaluated);

		assertFirstFewCalledPostfix("orelse", i(0), TRUE, FALSE, TRUE, firstTwoArgsEvaluated);

		assertFirstFewCalledPostfix("orelse", i(1), TRUE, FALSE, i(1), firstArgEvaluated);
	}

	private void assertFirstFewCalledInfix(String function, TypedValue a, TypedValue b, TypedValue c, TypedValue result, int argsCalled) {
		final SymbolStub<TypedValue> stubA = createConstFunction("a", a);
		final SymbolStub<TypedValue> stubB = createConstFunction("b", b);
		final SymbolStub<TypedValue> stubC = createConstFunction("c", c);

		infix(String.format("%s(a(), b(), c())", function)).expectResult(result);

		stubA.checkCallCount(argsCalled > 0? 1 : 0);
		stubB.checkCallCount(argsCalled > 1? 1 : 0);
		stubC.checkCallCount(argsCalled > 2? 1 : 0);
	}

	@Test
	public void testShortCuttingLogicInInfix() {
		infix("andthen()").expectResult(NULL);
		infix("andthen(5)").expectResult(i(5));

		infix("orelse()").expectResult(NULL);
		infix("orelse(6)").expectResult(i(6));

		final int firstArgEvaluated = 1;
		final int firstTwoArgsEvaluated = 2;
		final int allArgsEvaluated = 3;

		assertFirstFewCalledInfix("andthen", i(1), i(2), i(3), i(3), allArgsEvaluated);
		assertFirstFewCalledInfix("andthen", i(1), FALSE, i(3), FALSE, firstTwoArgsEvaluated);
		assertFirstFewCalledInfix("andthen", FALSE, i(2), i(3), FALSE, firstArgEvaluated);

		assertFirstFewCalledInfix("orelse", i(1), i(2), i(3), i(1), firstArgEvaluated);
		assertFirstFewCalledInfix("orelse", i(0), i(2), i(3), i(2), firstTwoArgsEvaluated);
		assertFirstFewCalledInfix("orelse", i(0), FALSE, i(3), i(3), allArgsEvaluated);
	}

	@Test
	public void testShortCircuitingLogicOps() {
		infix("a && b").expectSameAs(infix("andthen(a, b)"));
		infix("a && b && 'c'").expectSameAs(infix("andthen(a, b, 'c')"));
		infix("1 && (b && c) && 'c'").expectSameAs(infix("andthen(1, andthen(b, c), 'c')"));

		infix("a || b").expectSameAs(infix("orelse(a, b)"));
		infix("a || b || 'c'").expectSameAs(infix("orelse(a, b, 'c')"));
		infix("1 || (b || c) || 'c'").expectSameAs(infix("orelse(1, orelse(b, c), 'c')"));

		infix("1 && b || 'c'").expectSameAs(infix("orelse(andthen(1, b), 'c')"));
		infix("1 && b && c || 'd' || 'e'").expectSameAs(infix("orelse(andthen(1, b, c), 'd', 'e')"));

		infix("1 && a && 'b' || 2 && c && 'd' || 3 && e && 'f'").expectSameAs(infix("orelse(andthen(1, a, 'b'), andthen(2, c, 'd'), andthen(3, e, 'f'))"));
	}

	@Test
	public void testShortCircuitingNonNullSymbol() {
		infix("nonnull()").expectThrow(RuntimeException.class);
		infix("nonnull(null)").expectThrow(RuntimeException.class);
		infix("nonnull(false)").expectResult(FALSE);
		infix("nonnull(2)").expectResult(i(2));

		infix("nonnull(null, 2, 3)").expectResult(i(2));
		infix("nonnull(false, null, 2)").expectResult(FALSE);

		final int firstArgEvaluated = 1;
		final int firstTwoArgsEvaluated = 2;
		final int allArgsEvaluated = 3;

		assertFirstFewCalledInfix("nonnull", i(1), i(2), i(3), i(1), firstArgEvaluated);
		assertFirstFewCalledInfix("nonnull", FALSE, i(2), i(3), FALSE, firstArgEvaluated);
		assertFirstFewCalledInfix("nonnull", NULL, i(2), i(3), i(2), firstTwoArgsEvaluated);
		assertFirstFewCalledInfix("nonnull", NULL, NULL, i(3), i(3), allArgsEvaluated);
	}

	@Test
	public void testShortCircuitingNonNullOp() {
		infix("a ?? b").expectSameAs(infix("nonnull(a, b)"));
		infix("a ?? b ?? c").expectSameAs(infix("nonnull(a, b, c)"));
		infix("1 ?? (b ?? c) ?? 'c'").expectSameAs(infix("nonnull(1, nonnull(b, c), 'c')"));
	}

	@Test
	public void testLiftFunctionOnExecutable() {
		final SymbolStub<TypedValue> f = createConstFunction("a", i(4));
		postfix("5 { a + } nexecute").expectResult(i(9));
		f.checkCallCount(1).resetCallCount();

		postfix("null { a + } nexecute").expectResult(NULL);
		f.checkCallCount(0);
	}

	@Test
	public void testNullAwareDefaultOpWithNoArgCallable() {
		CallableStub<TypedValue> result = new CallableStub<TypedValue>().setReturns(i(4));
		sut.environment.setGlobalSymbol("a", result);
		final CallableStub<TypedValue> f = result;

		infix("let([t=a], t?())").expectResult(i(4));
		f.checkCallCount(1).resetCallCount();

		infix("let([t=null], t?())").expectResult(NULL);
		f.checkCallCount(0);
	}

	@Test
	public void testNullAwareDefaultOpWithArgsCallable() {
		final CallableStub<TypedValue> f = new CallableStub<TypedValue>().expectArgs(TRUE, s("hello")).setReturns(i(4));
		sut.environment.setGlobalSymbol("a", f);

		infix("let([t=a], t?(true, 'hello'))").expectResult(i(4));
		infix("let([t=null], t?(true, 'hello'))").expectResult(NULL);
	}

	@Test
	public void testNullAwareDefaultOpWithArgsCallableLaziness() {
		final CallableStub<TypedValue> f = new CallableStub<TypedValue>().expectArgs(TRUE, s("hello")).setReturns(i(4));
		sut.environment.setGlobalSymbol("a", f);

		final SymbolStub<TypedValue> c1 = createConstFunction("f_true", TRUE);
		final SymbolStub<TypedValue> c2 = createConstFunction("f_hello", s("hello"));

		infix("let([t=a], t?(f_true(), f_hello()))").expectResult(i(4));
		c1.checkCallCount(1).resetCallCount();
		c2.checkCallCount(1).resetCallCount();

		infix("let([t=null], t?(f_true(), f_hello()))").expectResult(NULL);
		c1.checkCallCount(0);
		c2.checkCallCount(0);
	}

	@Test
	public void testNullAwareDefaultOpWithIndexable() {
		class TestIndexableComposite implements MetaObject.SlotSlice {
			@Override
			public TypedValue slice(TypedValue self, TypedValue range, Frame<TypedValue> frame) {
				return cons(s("call"), range);
			}

		}

		sut.environment.setGlobalSymbol("a", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestIndexableComposite()).build()));

		infix("let([t=a], t?['hello'])").expectResult(cons(s("call"), s("hello")));
		infix("let([t=null], t?['hello'])").expectResult(NULL);
	}

	@Test
	public void testNullAwareDefaultOpChaining() {
		class TestSlotCall implements MetaObject.SlotCall {
			private final TypedValue path;

			public TestSlotCall(TypedValue path) {
				this.path = path;
			}

			@Override
			public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
				frame.stack().push(path);
			}
		}

		class TestSlotSlice implements MetaObject.SlotSlice {

			private final TypedValue path;

			public TestSlotSlice(TypedValue path) {
				this.path = path;
			}

			@Override
			public TypedValue slice(TypedValue self, TypedValue range, Frame<TypedValue> frame) {
				final TypedValue newPath = cons(range, path);
				return domain.create(DummyObject.class, DUMMY,
						MetaObject.builder()
								.set(new TestSlotSlice(newPath))
								.set(new TestSlotCall(newPath))
								.build());
			}
		}

		sut.environment.setGlobalSymbol("a", domain.create(DummyObject.class, DUMMY, MetaObject.builder().set(new TestSlotSlice(NULL)).set(new TestSlotCall(NULL)).build()));

		infix("let([t=a], t?['hello']?['world']?())").expectResult(cons(s("world"), cons(s("hello"), NULL)));
		infix("let([t=null], t?['hello']?['world']?())").expectResult(NULL);
	}

	@Test
	public void testNullAwareAccessOperator() {
		class TestStructured implements MetaObject.SlotAttr {
			@Override
			public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
				return Optional.of(cons(s("get"), s(key)));
			}
		}

		sut.environment.setGlobalSymbol("a", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructured()).build()));

		infix("let([t=a], t?.test)").expectResult(cons(s("get"), s("test")));
		infix("let([t=a], t?.'str')").expectResult(cons(s("get"), s("str")));

		infix("let([t=null], t?.test)").expectResult(NULL);
		infix("let([t=null], t?.'str')").expectResult(NULL);
	}

	@Test
	public void testNullAwareAccessOperatorLaziness() {
		class TestStructured implements MetaObject.SlotAttr {
			@Override
			public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
				return Optional.of(cons(s("get"), s(key)));
			}
		}

		sut.environment.setGlobalSymbol("a", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructured()).build()));

		final SymbolStub<TypedValue> c = createConstFunction("c", s("member"));

		infix("let([t=a], t?.(c()))").expectResult(cons(s("get"), s("member")));
		c.checkCallCount(1).resetCallCount();

		infix("let([t=null], t?.(c()))").expectResult(NULL);
		c.checkCallCount(0);
	}

	@Test
	public void testNullAwareAccessIndexOperator() {
		class TestIndexable implements MetaObject.SlotSlice {

			private final String value;

			public TestIndexable(String value) {
				this.value = value;
			}

			@Override
			public TypedValue slice(TypedValue self, TypedValue range, Frame<TypedValue> frame) {
				return cons(s(value), range);
			}

		}

		class TestStructured implements MetaObject.SlotAttr {
			@Override
			public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
				return Optional.of(domain.create(DummyObject.class, DUMMY,
						MetaObject.builder().set(new TestIndexable(key)).build()));
			}
		}

		sut.environment.setGlobalSymbol("a", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructured()).build()));

		// not ideal with that second '?', but for now should be enough; caused by [] being implemented by default op
		infix("let([t=a], t?.test?[5])").expectResult(cons(s("test"), i(5)));
		infix("let([t=a], t?.'hello'?['world'])").expectResult(cons(s("hello"), s("world")));

		infix("let([t=null], t?.test?[5])").expectResult(NULL);
		infix("let([t=null], t?.'hello'?['world'])").expectResult(NULL);
	}

	@Test
	public void testNullAwareAccessCallOperator() {
		class TestCallable implements MetaObject.SlotCall {

			private final String value;

			public TestCallable(String value) {
				this.value = value;
			}

			@Override
			public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
				frame.stack().push(cons(s(value), frame.stack().pop()));
			}

		}

		class TestStructured implements MetaObject.SlotAttr {

			@Override
			public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
				return Optional.of(domain.create(DummyObject.class, DUMMY,
						MetaObject.builder().set(new TestCallable(key)).build()));
			}
		}

		sut.environment.setGlobalSymbol("a", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructured()).build()));

		// not ideal with that second '?', but for now should be enough - done for symmetry with []
		infix("let([t=a], t?.test?(5))").expectResult(cons(s("test"), i(5)));
		infix("let([t=a], t?.'hello'?('world'))").expectResult(cons(s("hello"), s("world")));

		infix("let([t=null], t?.test?(5))").expectResult(NULL);
		infix("let([t=null], t?.'hello'?('world'))").expectResult(NULL);
	}

	@Test
	public void testNullAwareWithOperator() {
		class TestStructured implements MetaObject.SlotAttr {

			@Override
			public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
				if (!key.startsWith("m_")) return Optional.absent();
				return Optional.of(cons(s("get"), s(key)));
			}
		}

		sut.environment.setGlobalSymbol("a", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new TestStructured()).build()));

		infix("let([t=a], t?.{ cons(m_test1, m_test2) })").expectResult(cons(cons(s("get"), s("m_test1")), cons(s("get"), s("m_test2"))));
		infix("let([t=null], t?.{ cons(m_test, m_test2) })").expectResult(NULL);
	}

	@Test
	public void testEvalSymbol() {
		infix("eval('infix', '2+2')").expectResult(i(4));
		infix("eval('prefix', '(+ 2 3)')").expectResult(i(5));
		postfix("'postfix' '3 4 + 5' eval$2").expectResults(i(7), i(5));
	}

	@Test
	public void testEvalSymbolScope() {
		infix("let([x = 2], eval('infix', 'x + 3'))").expectResult(i(5));
		infix("let([x = 2], eval('infix', '(y) -> x + y'))(10)").expectResult(i(12));
	}

	@Test
	public void testStructSymbol() {
		infix("letseq([Point=struct(#x,#y), p=Point()], cons(p.x, p.y))").expectResult(cons(nil(), nil()));
		infix("letseq([Point=struct(#x,#y), p=Point(#y:5)], cons(p.x, p.y))").expectResult(cons(nil(), i(5)));
		infix("letseq([Point=struct(#x,#y), p=Point(#x:1,#y:5)], cons(p.x, p.y))").expectResult(cons(i(1), i(5)));

		infix("let([Point=struct(#x,#y,#z)], Point.name)").expectResult(s("struct"));
		infix("let([Point=struct(#x,#y,#z)], Point.fields)").expectResult(list(s("x"), s("y"), s("z")));
		infix("let([Point=struct(#x,#y,#z)], type(Point()).fields)").expectResult(list(s("x"), s("y"), s("z")));

		infix("let([Point=struct(#x,#y)], match((Point(x)) -> 'point', (_) -> 'other')(Point()))").expectResult(s("point"));
		infix("let([Point=struct(#x,#y)], match((Point(x)) -> 'point', (_) -> 'other')(5))").expectResult(s("other"));

		infix("let([Point=struct(#x,#y)], type(Point()) == Point)").expectResult(TRUE);
		infix("let([Point=struct(#x,#y)], type(Point()) == type(Point(#x=2)))").expectResult(TRUE);

		infix("let([Point=struct(#x,#y)], type(Point()).metaobject == getmetaobject(Point(#x=2)))").expectResult(TRUE);

		infix("letseq([Point=struct(#x,#y), p1 = Point(#y:3), p2 = p1(#x:2), p3 = p1(#y:8)], list(p1.x:p1.y, p2.x:p2.y, p3.x:p3.y))")
				.expectResult(list(
						cons(nil(), i(3)),
						cons(i(2), i(3)),
						cons(nil(), i(8))));

		infix("let([Point=struct(#x,#y)], Point(#x:5,#y:6) ==  Point(#y:6,#x:5))").expectResult(TRUE);
		infix("let([Point=struct(#x,#y)], Point(#x:5) ==  Point(#x:5))").expectResult(TRUE);
		infix("let([Point=struct(#x,#y)], Point(#x:4,#y:6) ==  Point(#x:5,#y:6))").expectResult(FALSE);
		infix("let([Point1=struct(#x,#y), Point2=struct(#x,#y)], Point1(#x:5,#y:6) ==  Point2(#x:5,#y:6))").expectResult(FALSE);
	}

	private void assertSameListContents(Set<TypedValue> expected, TypedValue actual) {
		final Cons list = actual.as(Cons.class);

		final Set<TypedValue> actualValues = Sets.newHashSet();
		list.visit(new Cons.ListVisitor(nil()) {
			@Override
			public void value(TypedValue value, boolean isLast) {
				actualValues.add(value);
			}
		});

		Assert.assertEquals(expected, actualValues);
	}

	@Test
	public void testDict() {
		infix("len(dict())").expectResult(i(0));
		infix("bool(dict())").expectResult(FALSE);

		infix("len(dict(1:'a','a':#b,2I:5))").expectResult(i(3));
		infix("len(dict(1 = 'a','a' = #b,2I = 5))").expectResult(i(3)); // alternative notation
		infix("bool(dict(1:'a','a':#b,2I:5))").expectResult(TRUE);

		infix("let([d = dict(1:'a','a':#b,2I:5)], cons(d[1], d[2I]))").expectResult(cons(s("a"), i(5)));

		infix("dict(1:'a','a':#b,2I:5) == dict(1:'a','a':#b,2I:5)").expectResult(TRUE);

		infix("dict(1:'a','a':#b,2I:5)['what']").expectResult(nil());

		infix("dict(1:'a','a':#b,2I:5).hasKey(1)").expectResult(TRUE);
		infix("dict(1:'a','a':#b,2I:5).hasKey('what')").expectResult(FALSE);

		infix("dict(1:'a','a':#b,2I:5).hasValue(#b)").expectResult(TRUE);
		infix("dict(1:'a','a':#b,2I:5).hasValue('c')").expectResult(FALSE);
		infix("dict(1:'a','a':#b,2I:5).hasValue(#b)").expectResult(TRUE);

		infix("match((dict(x)) -> 'dict', (_) -> 'other')(dict())").expectResult(s("dict"));
		infix("match((dict(x)) -> 'dict', (_) -> 'other')(5)").expectResult(s("other"));

		infix("type(dict()) == dict").expectResult(TRUE);
		infix("type(dict()).name").expectResult(s("dict"));
		infix("type(dict()).metaobject == getmetaobject(dict())").expectResult(TRUE);

		assertSameListContents(Sets.newHashSet(i(1), s("a"), complex(0, 2)), infix("dict(1:'a','a':#b,2I:5).keys").executeAndPop());
		assertSameListContents(Sets.newHashSet(s("a"), sym("b"), i(5)), infix("dict(1:'a','a':#b,2I:5).values").executeAndPop());
		assertSameListContents(Sets.newHashSet(cons(i(1), s("a")), cons(s("a"), sym("b")), cons(complex(0, 2), i(5))), infix("dict(1:'a','a':#b,2I:5).items").executeAndPop());

		infix("let([d = dict(1:'a','a':#b)], (d.update(#b:4,3:'4') == dict(1:'a','a':#b, #b:4, 3:'4')) && (d == dict(1:'a','a':#b)))").expectResult(TRUE);
		assertSameListContents(Sets.newHashSet(cons(i(1), s("a")), cons(s("a"), sym("b")), cons(sym("b"), i(4)), cons(i(3), s("4"))), infix("dict(1:'a','a':#b).update(#b:4,3:'4').items").executeAndPop());
		assertSameListContents(Sets.newHashSet(cons(i(1), s("a")), cons(s("a"), sym("b")), cons(sym("b"), i(4)), cons(i(3), s("4"))), infix("dict(1:'a','a':#b)(#b:4,3:'4').items").executeAndPop());

		infix("let([d = dict(1:'a','a':#b)], (d.remove(1,#what) == dict('a':#b)) && (d == dict(1:'a','a':#b)))").expectResult(TRUE);
		assertSameListContents(Sets.newHashSet(cons(s("a"), sym("b"))), infix("dict(1:'a','a':#b).remove(1,#what).items").executeAndPop());

		infix("dict(1:'a',#b:3).getOptional(#b) == optional.present(3)").expectResult(TRUE);
		infix("dict(1:'a',#b:3).getOptional(#blah) == optional.absent()").expectResult(TRUE);

		infix("dict(1:'a',#b:3).getOr(#b, 'bye')").expectResult(i(3));
		infix("dict(1:'a',#b:3).getOr(#blah, 'bye')").expectResult(s("bye"));

		infix("dict(1:'a',#b:3).getOrCall(1, ()->'nope')").expectResult(s("a"));
		infix("dict(1:'a',#b:3).getOrCall(2, ()->'nope')").expectResult(s("nope"));
	}

	@Test
	public void testOptional() {
		infix("optional.present(1) == optional.present(1)").expectResult(TRUE);
		infix("optional.absent() == optional.absent()").expectResult(TRUE);

		infix("optional.from(null) == optional.absent()").expectResult(TRUE);
		infix("optional.from(4) == optional.present(4)").expectResult(TRUE);

		infix("optional.from(null) != optional.from(1)").expectResult(TRUE);
		infix("optional.from(5) != optional.from(1)").expectResult(TRUE);
		infix("optional.from(2) == optional.from(2)").expectResult(TRUE);

		infix("optional.from(1).get()").expectResult(i(1));
		infix("optional.present(2).get()").expectResult(i(2));

		infix("optional.from(1).map((x)->x+5).get()").expectResult(i(6));
		infix("optional.from(null).map((x)->x+5) == optional.absent()").expectResult(TRUE);

		infix("optional.from(2).orCall(()->fail('nope'))").expectResult(i(2));
		infix("optional.from(null).orCall(()->#b)").expectResult(sym("b"));

		infix("optional.from(3).or(9)").expectResult(i(3));
		infix("optional.from(null).or('d')").expectResult(s("d"));

		infix("match((optional.present(v)) -> 'present':v, (optional.absent()) -> 'absent')(optional.from(5))").expectResult(cons(s("present"), i(5)));
		infix("match((optional.present(v)) -> 'present':v, (optional.absent()) -> 'absent')(optional.from(null))").expectResult(s("absent"));

		infix("match((optional(x)) -> 'optional', (_) -> 'other')(optional.absent())").expectResult(s("optional"));
		infix("match((optional(x)) -> 'optional', (_) -> 'other')(optional.present(1))").expectResult(s("optional"));
		infix("match((optional(x)) -> 'optional', (_) -> 'other')(4)").expectResult(s("other"));

		infix("type(optional.absent()) == optional").expectResult(TRUE);
		infix("optional.absent.name").expectResult(s("optional.absent"));

		infix("type(optional.present(4)) == optional").expectResult(TRUE);
		infix("optional.present.name").expectResult(s("optional.present"));
	}

	@Test
	public void testDoExpression() {
		infix("do(2+2,3+3,4+4)").expectResult(i(8));
		postfix("{1} {2 3} {4 5 6} do$3").expectResults(i(4), i(5), i(6));
	}

	@Test
	public void testGlobalsSymbol() {
		infix("globals().max == max").expectResult(TRUE);
		infix("let([oldmax = max, max = ()->'nope'], globals().max != max)").expectResult(TRUE);
		infix("let([oldmax = max, max = ()->'nope'], globals().max == oldmax)").expectResult(TRUE);
		infix("let([oldmax = max, max = ()->'nope'], globals().{max == oldmax})").expectResult(TRUE);
	}

	@Test
	public void testLocalsSymbol() {
		infix("locals().max == max").expectResult(TRUE);
		infix("let([test = 5], let([l = locals(), test = 6], test == 6 && l.test == 5))").expectResult(TRUE);
	}

	@Test
	public void testSlotPredicates() {
		sut.environment.setGlobalSymbol("test", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder()
						.set(new MetaObject.SlotBool() {
							@Override
							public boolean bool(TypedValue self, Frame<TypedValue> frame) {
								return false;
							}
						})
						.set(new MetaObject.SlotStr() {
							@Override
							public String str(TypedValue self, Frame<TypedValue> frame) {
								return "dummy";
							}
						})
						.build()));

		infix("has(test, slots.str)").expectResult(TRUE);
		infix("has(test, slots.repr)").expectResult(FALSE);
		infix("has(test, slots.bool)").expectResult(TRUE);

		infix("match((slots.str(x)) -> 'can', (_) -> 'cannot')(test)").expectResult(s("can"));
		infix("match((slots.str(x)) -> 'can', (_) -> 'cannot')(4)").expectResult(s("can"));
		infix("match((slots.str(x)) -> 'can', (_) -> 'cannot')(locals())").expectResult(s("cannot"));
	}

	@Test
	public void testMetaObjectFromBuiltInObject() {
		infix("type(getmetaobject(1)) == metaobject").expectResult(TRUE);
		infix("type(getmetaobject(1).str) == metaobjectslotvalue").expectResult(TRUE);

		infix("getmetaobject(1) == getmetaobject(2)").expectResult(TRUE);
		infix("getmetaobject(1) == int.metaobject").expectResult(TRUE);
		infix("getmetaobject(1) != getmetaobject(I)").expectResult(TRUE);

		infix("getmetaobject(1).str.name").expectResult(s("str"));
		infix("getmetaobject(1).str.info == slots.str").expectResult(TRUE);

		infix("getmetaobject(1).str(123)").expectResult(s("123"));
		infix("int.metaobject.str(123)").expectResult(s("123"));
		infix("slots.str(123)").expectResult(s("123"));

		infix("let([f = (a,b)->a-b], getmetaobject(f).call(f, 3, 5))").expectResult(i(-2));
		infix("let([f = (a,b)->a-b], function.metaobject.call(f, 3, 6))").expectResult(i(-3));
		infix("slots.call((a,b)->a-b, 3, 7)").expectResult(i(-4));

		infix("getmetaobject(1).call").expectResult(NULL);
	}

	@Test
	public void testCustomMetaCreationAndSetting() {
		infix("type(metaobject(slots.str=(s) -> 'hello')) == metaobject").expectResult(TRUE);
		infix("let([callableInt=setmetaobject(13, metaobject(slots.call = (self, other) -> self + other))], callableInt(21))").expectResult(i(34));
	}

	@Test
	public void testCustomMetaObjectSlotRetrievalOptimization() {
		infix("let([f = (s) -> 'hello'], metaobject(slots.str=f).str == f)").expectResult(TRUE);
	}

	@Test
	public void testCustomMetaObjectWithNativeSlots() {
		infix("letseq([originalMetaobject = getmetaobject(3), callableInt=setmetaobject(13, metaobject(slots.call = (self) -> self + 1, slots.str=originalMetaobject.str))], str(callableInt) + ' ' + str(callableInt()))").expectResult(s("13 14"));
	}

	@Test
	public void testCustomMetaObjectUpdateFromNative() {
		infix("letseq([originalMetaobject = getmetaobject('s'), callableStr=setmetaobject('hello', originalMetaobject(slots.call = (self) -> 'world'))], callableStr + ' ' + callableStr())").expectResult(s("hello world"));
	}

	@Test
	public void testCustomMetaObjectAttrSlot() {
		infix("letseq([mo = metaobject(slots.attr = (self, key) -> optional.present('m_' + self + '_' + key)), o1=setmetaobject('a', mo), o2=setmetaobject('b', mo)], (o1.x:o1.y):(o2.x:o2.y))").expectResult(cons(cons(s("m_a_x"), s("m_a_y")), cons(s("m_b_x"), s("m_b_y"))));
		infix("let([mo = getmetaobject(int)], mo.attr(int, 'name') == optional.present('int'))").expectResult(TRUE);
	}

	@Test
	public void testCustomMetaObjectBoolSlot() {
		infix("letseq([mo = metaobject(slots.bool = (self) -> self == 'yup'), o_true=setmetaobject('yup', mo), o_false=setmetaobject('nope', mo)], bool(o_true):bool(o_false))").expectResult(cons(TRUE, FALSE));
		infix("int.metaobject.bool(0)").expectResult(FALSE);
		infix("int.metaobject.bool(1)").expectResult(TRUE);
	}

	@Test
	public void testCustomMetaObjectCallSlot() {
		infix("letseq([mo = metaobject(slots.call = (self, a, b) -> if(self == 'left', a, b)), left=setmetaobject('left', mo), right=setmetaobject('right', mo)], left(1,2):right(1,2))").expectResult(cons(i(1), i(2)));
		infix("letseq([v = (a,b) -> a - b, mo = getmetaobject(v)], mo.call(v, 1, 2))").expectResult(i(-1));
	}

	@Test
	public void testCustomMetaObjectEqualsSlot() {
		infix("letseq([s='abcd', l=len(s), mo = metaobject(slots.equals = (self, other) -> l == len(other)), o=setmetaobject('abcd', mo)], (o == 'defg'):(o == 'abc'))").expectResult(cons(TRUE, FALSE));
		infix("letseq([mo = int.metaobject], mo.equals(5, 5):mo.equals(5,6))").expectResult(cons(TRUE, FALSE));
	}

	@Test
	public void testCustomMetaObjectLengthSlot() {
		infix("letseq([mo = metaobject(slots.length = (self) -> int(self) + 7), o=setmetaobject(5, mo)], len(o))").expectResult(i(12));
		infix("str.metaobject.length('abcde')").expectResult(i(5));
	}

	@Test
	public void testCustomMetaObjectReprSlot() {
		infix("letseq([mo = metaobject(slots.repr = (self) -> $'<{self}>'), o=setmetaobject('test', mo)], repr(o))").expectResult(s("<test>"));
		infix("str.metaobject.repr('abc')").expectResult(s("\"abc\""));
	}

	@Test
	public void testCustomMetaObjectStrSlot() {
		infix("letseq([mo = metaobject(slots.str = (self) -> 'hello'), o=setmetaobject('123', mo)], str(o))").expectResult(s("hello"));
		infix("str.metaobject.str('abc')").expectResult(s("abc"));
	}

	@Test
	public void testCustomMetaObjectSliceSlot() {
		infix("letseq([mo = metaobject(slots.slice = (self, index) -> self + index), o=setmetaobject(15, mo)], o[20])").expectResult(i(35));
		infix("dict.metaobject.slice(dict('3':-5), '3')").expectResult(i(-5));
	}

	@Test
	public void testCustomMetaObjectTypeSlot() {
		infix("letseq([mo = metaobject(slots.type = (self) -> 'what?'), o=setmetaobject(15, mo)], type(o))").expectResult(s("what?"));
		infix("str.metaobject.type('hi') == str").expectResult(TRUE);
	}

	@Test
	public void testCustomMetaObjectDecomposeSlot() {
		infix("letseq([mo = metaobject(slots.decompose = (self, value, count) -> if(value == 'go', optional.present((self + 1):(self + 3)), optional.absent())), O=setmetaobject(15, mo)], match((O(a, b)) -> a:b, (_) -> 'other')('go'))").expectResult(cons(i(16), i(18)));
		infix("letseq([mo = metaobject(slots.decompose = (self, value, count) -> if(value == 'go', optional.present((self + 1):(self + 3)), optional.absent())), O=setmetaobject(15, mo)], match((O(a, b)) -> a:b, (_) -> 'other')('no go'))").expectResult(s("other"));
		infix("letseq([mo = getmetaobject(int)], mo.decompose(int, 5, 1) == optional.present(5))").expectResult(TRUE);
		infix("letseq([mo = getmetaobject(int)], mo.decompose(int, '5', 1) == optional.absent())").expectResult(TRUE);
	}

	@Test
	public void testRegexCompilation() {
		infix("type(regex('test+')) == regex.pattern").expectResult(TRUE);

		infix("regex('test+').pattern").expectResult(s("test+"));
		infix("regex('test+', regex.i | regex.x).flags").expectResult(i(Pattern.CASE_INSENSITIVE | Pattern.COMMENTS));
	}

	@Test
	public void testRegexMatch() {
		infix("type(regex('test+').match) == regex.matcher").expectResult(TRUE);
		infix("type(regex('test+').match('test')) == optional").expectResult(TRUE);
		infix("type(regex('test+').match('test').get()) == regex.match").expectResult(TRUE);

		infix("regex('test+').match('tost') == optional.absent()").expectResult(TRUE);
		infix("regex('test+').match('atest') == optional.absent()").expectResult(TRUE);
		infix("regex('test+').match('testa') == optional.absent()").expectResult(TRUE);

		infix("regex('test+').match('test').get().start").expectResult(i(0));
		infix("regex('test+').match('test').get().end").expectResult(i(4));
		infix("regex('test+').match('test').get().matched").expectResult(s("test"));

		infix("regex('test+').match('testtt').get().start").expectResult(i(0));
		infix("regex('test+').match('testtt').get().end").expectResult(i(6));
		infix("regex('test+').match('testtt').get().matched").expectResult(s("testtt"));
	}

	@Test
	public void testRegexGroups() {
		infix("len(regex('a(.?)b(.?)c').match('abc').get())").expectResult(i(2));
		infix("regex('a(.?)b(.?)c').match('abc').get()[0]").expectResult(s("abc"));
		infix("regex('a(.?)b(.?)c').match('abc').get()[1]").expectResult(s(""));
		infix("regex('a(.?)b(.?)c').match('abc').get()[2]").expectResult(s(""));

		infix("len(regex('a(.?)b(.?)c').match('a1b2c').get())").expectResult(i(2));
		infix("regex('a(.?)b(.?)c').match('a1b2c').get()[0]").expectResult(s("a1b2c"));
		infix("regex('a(.?)b(.?)c').match('a1b2c').get()[1]").expectResult(s("1"));
		infix("regex('a(.?)b(.?)c').match('a1b2c').get()[2]").expectResult(s("2"));
	}

	@Test
	public void testRegexSearch() {
		infix("type(regex('test+').search) == regex.matcher").expectResult(TRUE);
		infix("type(regex('test+').search('test')) == optional").expectResult(TRUE);
		infix("type(regex('test+').search('test')) == optional").expectResult(TRUE);
		infix("type(regex('test+').search('test').get()) == regex.match").expectResult(TRUE);

		infix("regex('test+').search('tost') == optional.absent()").expectResult(TRUE);

		infix("regex('test+').search('atest').get().start").expectResult(i(1));
		infix("regex('test+').search('atest').get().end").expectResult(i(5));
		infix("regex('test+').search('atest').get().matched").expectResult(s("test"));

		infix("regex('test+').search('testa').get().start").expectResult(i(0));
		infix("regex('test+').search('testa').get().end").expectResult(i(4));
		infix("regex('test+').search('testa').get().matched").expectResult(s("test"));
	}

	@Test
	public void testComplexUnpack() {
		infix("let([polar(r,theta) = cartesian(3, 0)], r:theta)").expectResult(cons(d(3), d(0)));
		infix("let([cartesian(x,y) = polar(3, 0)], x:y)").expectResult(cons(d(3), d(0)));
	}

	@Test
	public void testMapFunction() {
		infix("map((x) -> x + 2, [])").expectResult(list());
		infix("map((x) -> x + 2, [1])").expectResult(list(i(3)));
		infix("map((x) -> x + 2, [1, 2])").expectResult(list(i(3), i(4)));
	}

	@Test
	public void testFilterFunction() {
		infix("filter((x) -> true, [])").expectResult(list());
		infix("filter((x) -> false, [1,2,3])").expectResult(list());
		infix("filter((x) -> true, [1,2,3])").expectResult(list(i(1), i(2), i(3)));
		infix("filter((x) -> x % 2, [1,2,3,4])").expectResult(list(i(1), i(3)));
	}

	@Test
	public void testReduceFunction() {
		infix("reduce((x,y) -> x + '-' + y, 'a', [])").expectResult(s("a"));
		infix("reduce((x,y) -> x + '-' + y, 'a', ['b'])").expectResult(s("a-b"));
		infix("reduce((x,y) -> x + '-' + y, 'a', ['b', 'c'])").expectResult(s("a-b-c"));
	}

	@Test
	public void testTakeFunction() {
		infix("take([1,2,3], 0)").expectResult(list());
		infix("take([1,2,3], 1)").expectResult(list(i(1)));
		infix("take([1,2,3], 2)").expectResult(list(i(1), i(2)));
		infix("take([1,2,3], 3)").expectResult(list(i(1), i(2), i(3)));
		infix("take([1,2,3], 4)").expectResult(list(i(1), i(2), i(3)));
	}

	@Test
	public void testTakeWhileFunction() {
		infix("takeWhile([1,2,3,4], (x) -> false)").expectResult(list());
		infix("takeWhile([1,2,3,4], (x) -> true)").expectResult(list(i(1), i(2), i(3), i(4)));

		infix("takeWhile([], (x) -> x <= 2)").expectResult(list());
		infix("takeWhile([1,2,3,4], (x) -> x <= 2)").expectResult(list(i(1), i(2)));
	}

	@Test
	public void testDropFunction() {
		infix("drop([1,2,3], 0)").expectResult(list(i(1), i(2), i(3)));
		infix("drop([1,2,3], 1)").expectResult(list(i(2), i(3)));
		infix("drop([1,2,3], 2)").expectResult(list(i(3)));
		infix("drop([1,2,3], 3)").expectResult(list());
		infix("drop([1,2,3], 4)").expectResult(list());
	}

	@Test
	public void testDropWhileFunction() {
		infix("dropWhile([1,2,3,4], (x) -> true)").expectResult(list());
		infix("dropWhile([1,2,3,4], (x) -> false)").expectResult(list(i(1), i(2), i(3), i(4)));

		infix("dropWhile([], (x) -> x <= 2)").expectResult(list());
		infix("dropWhile([1,2,3,4], (x) -> x <= 2)").expectResult(list(i(3), i(4)));
	}

	@Test
	public void testAnyFunction() {
		infix("any([])").expectResult(FALSE);
		infix("any([0])").expectResult(FALSE);
		infix("any([1])").expectResult(TRUE);
		infix("any([0, 0])").expectResult(FALSE);
		infix("any([0, 1])").expectResult(TRUE);
		infix("any([1, 0])").expectResult(TRUE);
		infix("any([1, 1])").expectResult(TRUE);
	}

	@Test
	public void testAllFunction() {
		infix("all([])").expectResult(TRUE);
		infix("all([0])").expectResult(FALSE);
		infix("all([1])").expectResult(TRUE);
		infix("all([0, 0])").expectResult(FALSE);
		infix("all([0, 1])").expectResult(FALSE);
		infix("all([1, 0])").expectResult(FALSE);
		infix("all([1, 1])").expectResult(TRUE);
	}

	@Test
	public void testEnumerateFunction() {
		infix("enumerate([])").expectResult(list());
		infix("enumerate(['a'])").expectResult(list(cons(i(0), s("a"))));
		infix("enumerate(['a','b','c'])").expectResult(list(cons(i(0), s("a")), cons(i(1), s("b")), cons(i(2), s("c"))));
	}

	@Test
	public void testRangeFunction() {
		infix("range(0)").expectResult(list());
		infix("range(1)").expectResult(list(i(0)));
		infix("range(2)").expectResult(list(i(0), i(1)));

		infix("range(1,1)").expectResult(list());
		infix("range(1,2)").expectResult(list(i(1)));
		infix("range(1,3)").expectResult(list(i(1), i(2)));

		infix("range(-1,3)").expectResult(list(i(-1), i(0), i(1), i(2)));

		infix("range(0,3,2)").expectResult(list(i(0), i(2)));
		infix("range(0,4,2)").expectResult(list(i(0), i(2)));
		infix("range(0,5,2)").expectResult(list(i(0), i(2), i(4)));

		infix("range(3,0)").expectResult(list());
		infix("range(3,2)").expectResult(list());
		infix("range(3,0,-1)").expectResult(list(i(3), i(2), i(1)));

		infix("range(3,-3,-2)").expectResult(list(i(3), i(1), i(-1)));
	}

	@Test
	public void testZipFunction() {
		infix("zip([],[])").expectResult(list());

		infix("zip([1],[])").expectResult(list());
		infix("zip([],['a'])").expectResult(list());

		infix("zip([1],['a'])").expectResult(list(cons(i(1), s("a"))));
		infix("zip([1, 2],['a'])").expectResult(list(cons(i(1), s("a"))));
		infix("zip([1],['a', 'b'])").expectResult(list(cons(i(1), s("a"))));
		infix("zip([1, 2],['a', 'b'])").expectResult(list(cons(i(1), s("a")), cons(i(2), s("b"))));

		infix("map((a:b)->a*b, zip(['a','b','c'],[1,2,3]))").expectResult(list(s("a"), s("bb"), s("ccc")));
		infix("map((a:b:c)->b*c+a, zip(['o','m','g'], zip(['a','b','c'],[1,2,3])))").expectResult(list(s("ao"), s("bbm"), s("cccg")));
	}

	@Test
	public void testSortFunction() {
		infix("sort([])").expectResult(list());
		infix("sort(['a'])").expectResult(list(s("a")));

		infix("sort([3,2,1])").expectResult(list(i(1), i(2), i(3)));
		infix("sort([3,2,2,1])").expectResult(list(i(1), i(2), i(2), i(3)));
		infix("sort([3,2.0,2,1])").expectResult(list(i(1), d(2.0), i(2), i(3)));

		infix("sort([2.1, 3,true])").expectResult(list(TRUE, d(2.1), i(3)));
		infix("sort(['ab','aa','a'])").expectResult(list(s("a"), s("aa"), s("ab")));

		infix("sort([true,5.1,8,3], #reverse=true)").expectResult(list(i(8), d(5.1), i(3), TRUE));

		infix("sort(['10','20','100'])").expectResult(list(s("10"), s("100"), s("20")));
		infix("sort(['10','20','100'], #key=(x)->int(x))").expectResult(list(s("10"), s("20"), s("100")));

		infix("sort(['31','12','53'])").expectResult(list(s("12"), s("31"), s("53")));
		infix("sort(['31','12','53'], #cmp=(a,b)->a[1] <=> b[1])").expectResult(list(s("31"), s("12"), s("53")));

		infix("sort([10.1,20,'30.1','30.0'], #key=(x)->float(x), #cmp=(a,b)->int(b-a))").expectResult(list(s("30.1"), s("30.0"), i(20), d(10.1)));
	}

	@Test
	public void testVarargLambda() {
		infix("((*arg) -> 'args':arg)()").expectResult(list(s("args")));
		infix("((*arg) -> 'args':arg)(1)").expectResult(list(s("args"), i(1)));
		infix("((*arg) -> 'args':arg)(1,2,3)").expectResult(list(s("args"), i(1), i(2), i(3)));

		infix("(*arg -> 'args':arg)()").expectResult(list(s("args")));
		infix("(*arg -> 'args':arg)(1)").expectResult(list(s("args"), i(1)));
		infix("(*arg -> 'args':arg)(1,2,3)").expectResult(list(s("args"), i(1), i(2), i(3)));

		infix("((a, *arg) -> a:'var':arg)(1)").expectResult(list(i(1), s("var")));
		infix("((a, *arg) -> a:'var':arg)(1,2,3)").expectResult(list(i(1), s("var"), i(2), i(3)));
	}

	@Test
	public void testVarargLambdaInLet() {
		infix("let([f = (*arg) -> 'args':arg], f())").expectResult(list(s("args")));
		infix("let([f = (*arg) -> 'args':arg], f(1,2,3))").expectResult(list(s("args"), i(1), i(2), i(3)));

		infix("let([f(*arg) -> 'args':arg], f())").expectResult(list(s("args")));
		infix("let([f(*arg) -> 'args':arg], f(1,2,3))").expectResult(list(s("args"), i(1), i(2), i(3)));

		infix("let([f(a, *arg) -> a:'var':arg], f('a'))").expectResult(list(s("a"), s("var")));
		infix("let([f(a, *arg) -> a:'var':arg], f('b', 1, 2, 3))").expectResult(list(s("b"), s("var"), i(1), i(2), i(3)));
	}

	@Test(expected = IllegalStateException.class)
	public void testVarargLambdaPositionalAfterVararg() {
		infix("(*arg, a) -> 'fail'").execute();
	}

	@Test(expected = IllegalStateException.class)
	public void testVarargLambdaTwoVarargs() {
		infix("(*a, *b) -> 'fail'").execute();
	}

	@Test
	public void testFlatten() {
		infix("flatten()").expectResult(list());
		infix("flatten([])").expectResult(list());
		infix("flatten([],[])").expectResult(list());
		infix("flatten([1])").expectResult(list(i(1)));
		infix("flatten([],[1])").expectResult(list(i(1)));
		infix("flatten([1],[])").expectResult(list(i(1)));
		infix("flatten([1],[2])").expectResult(list(i(1), i(2)));
		infix("flatten([1,2],[3,4])").expectResult(list(i(1), i(2), i(3), i(4)));
		infix("flatten([1,2],[],[3,4])").expectResult(list(i(1), i(2), i(3), i(4)));
	}

	private class CallableLogger implements ICallable<TypedValue> {
		@Override
		public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			final Stack<TypedValue> substack = frame.stack().substack(argumentsCount.get());
			final List<String> result = Lists.newArrayList();
			for (TypedValue v : substack)
				result.add(v.as(String.class));
			substack.clear();
			final String args = Joiner.on(",").join(result);
			substack.push(domain.create(String.class, "(" + args + ")"));
		}
	}

	@Test
	public void testApplyVar() {
		sut.environment.setGlobalSymbol("test", new CallableLogger());
		infix("applyvar(test, [])").expectResult(s("()"));
		infix("applyvar(test, ['x'])").expectResult(s("(x)"));
		infix("applyvar(test, ['3','2','1'])").expectResult(s("(3,2,1)"));

		infix("applyvar(test, 'a', [])").expectResult(s("(a)"));
		infix("applyvar(test, 'a', ['x'])").expectResult(s("(a,x)"));
		infix("applyvar(test, 'a', ['3','2','1'])").expectResult(s("(a,3,2,1)"));

		infix("applyvar(test, 'a', 'b', 'c', [])").expectResult(s("(a,b,c)"));
		infix("applyvar(test, 'a', 'b', 'c', ['x'])").expectResult(s("(a,b,c,x)"));
		infix("applyvar(test, 'a', 'b', 'c', ['3','2','1'])").expectResult(s("(a,b,c,3,2,1)"));
	}

	@Test
	public void testArgUnpackCompilationAndRuntime() {
		sut.environment.setGlobalSymbol("test", new CallableLogger());
		infix("test(*[])")
				.expectSameAs(postfix("@test list$0,1 applyvar$2,1"))
				.expectResult(s("()"));

		infix("test('a',*[])")
				.expectSameAs(postfix("@test 'a' list$0,1 applyvar$3,1"))
				.expectResult(s("(a)"));

		infix("test(*['1'])")
				.expectSameAs(postfix("@test '1' list$1,1 applyvar$2,1"))
				.expectResult(s("(1)"));

		infix("test(*['1','2'])")
				.expectSameAs(postfix("@test '1' '2' list$2,1 applyvar$2,1"))
				.expectResult(s("(1,2)"));

		infix("test('a',*['1','2'])")
				.expectSameAs(postfix("@test 'a' '1' '2' list$2,1 applyvar$3,1"))
				.expectResult(s("(a,1,2)"));

		infix("test(*['1'], 'a')")
				.expectSameAs(postfix("@test '1' list$1,1 'a' list$1,1 flatten$2,1 applyvar$2,1"))
				.expectResult(s("(1,a)"));

		infix("test(*['1'], 'a', *['2','3'])")
				.expectSameAs(postfix("@test '1' list$1,1 'a' list$1,1 '2' '3' list$2,1 flatten$3,1 applyvar$2,1"))
				.expectResult(s("(1,a,2,3)"));

		infix("test(*['1'], 'a', 'b')")
				.expectSameAs(postfix("@test '1' list$1,1 'a' 'b' list$2,1 flatten$2,1 applyvar$2,1"))
				.expectResult(s("(1,a,b)"));

		infix("test('a', *['1'], 'b', 'c', *['2','3'])")
				.expectSameAs(postfix("@test 'a' '1' list$1,1 'b' 'c' list$2,1 '2' '3' list$2,1 flatten$3,1 applyvar$3,1"))
				.expectResult(s("(a,1,b,c,2,3)"));
	}

	@Test
	public void testArgUnpackOnLocalVars() {
		sut.environment.setGlobalSymbol("test", new CallableLogger());

		infix("let([v=['1','2']], test(*v))")
				.expectResult(s("(1,2)"));

		infix("let([a1='a', a2='b', v=['1','2']], test(a1, *v, a2))")
				.expectResult(s("(a,1,2,b)"));
	}

	@Test
	public void testArgUnpackOnSymbolGetCalls() {
		sut.environment.setGlobalSymbol("test", new CallableLogger());

		infix("@test('a', *['1'], 'b', 'c', *['2','3'])").expectResult(s("(a,1,b,c,2,3)"));
	}

	@Test
	public void testArgUnpackOnApply() {
		sut.environment.setGlobalSymbol("test", new NullaryFunction.Direct<TypedValue>() {
			@Override
			protected TypedValue call() {
				return domain.create(CallableValue.class, CallableValue.from(new CallableLogger()));
			}
		});

		infix("test()('a', *['1'], 'b', 'c', *['2','3'])").expectResult(s("(a,1,b,c,2,3)"));
	}

	@Test
	public void testArgUnpackWithNullAwareCall() {
		sut.environment.setGlobalSymbol("test", new CallableLogger());

		infix("test?('a', *['1'], 'b', 'c', *['2','3'])").expectResult(s("(a,1,b,c,2,3)"));
	}

	@Test
	public void testArgUnpackWithDotCall() {
		sut.environment.setGlobalSymbol("test", domain.create(DummyObject.class, DUMMY,
				MetaObject.builder().set(new CallableLoggerStruct()).build()));

		infix("test.f('a', *['1'], 'b', 'c', *['2','3'])").expectResult(s("f(a,1,b,c,2,3)"));
	}

	@Test
	public void testRandom() {
		{
			final Random testRandom = new Random(3071);
			infix("let([r=random(3071)], r.nextInt():r.nextInt())").expectResult(cons(i(testRandom.nextInt()), i(testRandom.nextInt())));
		}

		{
			final Random testRandom = new Random(1103);
			infix("let([r=random(1103)], r.nextInt(6):r.nextInt(6))").expectResult(cons(i(testRandom.nextInt(6)), i(testRandom.nextInt(6))));
		}

		{
			final Random testRandom = new Random(1677);
			infix("let([r=random(1677)], r.nextBoolean():r.nextBoolean())").expectResult(cons(b(testRandom.nextBoolean()), b(testRandom.nextBoolean())));
		}
	}

	@Test
	public void testCurry() {
		infix("curry(list, 1, '2', 3.0)()").expectResult(list(i(1), s("2"), d(3.0)));
		infix("curry(list, 1, '2', 3.0)('5', 6)").expectResult(list(i(1), s("2"), d(3.0), s("5"), i(6)));

		infix("curry(curry(list, 1, '2'), 3.0)()").expectResult(list(i(1), s("2"), d(3.0)));
		infix("curry(curry(list, 1, '2'), 3.0)('5', 6)").expectResult(list(i(1), s("2"), d(3.0), s("5"), i(6)));
	}

	@Test
	public void testChain() {
		infix("chain(x->x+'a', x->x+'b')('c')").expectResult(s("cba"));
		infix("chain(chain(x->x+'a', x->x+'b'), x->x+'c')('d')").expectResult(s("dcba"));
	}

	@Test
	public void testId() {
		infix("id('test')").expectResult(s("test"));
		postfix("1 '2' 3.0 id$3,3").expectResults(i(1), s("2"), d(3.0));
		postfix("1 2 id$3,3").expectThrow(RuntimeException.class);
	}

	@Test
	public void testStringLower() {
		infix("''.lower").expectResult(s(""));
		infix("'abc'.lower").expectResult(s("abc"));
		infix("'AbC'.lower").expectResult(s("abc"));
		infix("'ABC'.lower").expectResult(s("abc"));
	}

	@Test
	public void testStringUpper() {
		infix("''.upper").expectResult(s(""));
		infix("'abc'.upper").expectResult(s("ABC"));
		infix("'AbC'.upper").expectResult(s("ABC"));
		infix("'ABC'.upper").expectResult(s("ABC"));
	}

	@Test
	public void testStringStrip() {
		infix("''.strip").expectResult(s(""));
		infix("'aA'.strip").expectResult(s("aA"));
		infix("'    aB'.strip").expectResult(s("aB"));
		infix("'cB      '.strip").expectResult(s("cB"));
		infix("'\tdE      '.strip").expectResult(s("dE"));
	}

	@Test
	public void testStringStartsWith() {
		infix("''.startsWith('')").expectResult(TRUE);
		infix("'abc'.startsWith('ab')").expectResult(TRUE);
		infix("'abc'.startsWith('bc')").expectResult(FALSE);
	}

	@Test
	public void testStringEndsWith() {
		infix("''.endsWith('')").expectResult(TRUE);
		infix("'abc'.endsWith('ab')").expectResult(FALSE);
		infix("'abc'.endsWith('bc')").expectResult(TRUE);
	}

	@Test
	public void testStringIndexOf() {
		infix("''.indexOf('')").expectResult(i(0));
		infix("'abc'.indexOf('ab')").expectResult(i(0));
		infix("'abc'.indexOf('bc')").expectResult(i(1));
		infix("'abc'.indexOf('xyz')").expectResult(i(-1));
	}

	@Test
	public void testStringSplit() {
		infix("''.split()").expectResult(list(s("")));
		infix("'a bc d'.split()").expectResult(list(s("a"), s("bc"), s("d")));
		infix("'a|bc|d|ef'.split('|')").expectResult(list(s("a"), s("bc"), s("d"), s("ef")));

		infix("'a|bc|d|ef'.split('|', 1)").expectResult(list(s("a|bc|d|ef")));
		infix("'a|bc|d|ef'.split('|', 2)").expectResult(list(s("a"), s("bc|d|ef")));
		infix("'a|bc|d|ef'.split('|', 3)").expectResult(list(s("a"), s("bc"), s("d|ef")));
	}

	@Test
	public void testStringJoin() {
		infix("''.join([])").expectResult(s(""));
		infix("','.join([])").expectResult(s(""));

		infix("''.join(['a','b','c'])").expectResult(s("abc"));
		infix("','.join(['a','b','c'])").expectResult(s("a,b,c"));
	}

	@Test
	public void testStringOrd() {
		infix("'a'.ord").expectResult(i('a'));
		infix("'\\uD83D\\uDE08'.ord").expectResult(i(0x1F608));
		infix("'\uD83D\uDE08'.ord").expectResult(i(0x1F608));
	}
}
