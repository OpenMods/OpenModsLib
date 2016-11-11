package openmods.calc;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import openmods.calc.CalcTestUtils.CalcCheck;
import openmods.calc.CalcTestUtils.SymbolStub;
import openmods.calc.types.multi.CompositeTraits;
import openmods.calc.types.multi.Cons;
import openmods.calc.types.multi.IComposite;
import openmods.calc.types.multi.ICompositeTrait;
import openmods.calc.types.multi.SimpleComposite;
import openmods.calc.types.multi.SingleTraitComposite;
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
	public void testEmptyCompositeObject() {
		class EmptyComposite implements IComposite {

			@Override
			public String type() {
				return "empty";
			}

			@Override
			public boolean has(Class<? extends ICompositeTrait> cls) {
				return false;
			}

			@Override
			public <T extends ICompositeTrait> T get(Class<T> cls) {
				return null;
			}

			@Override
			public <T extends ICompositeTrait> Optional<T> getOptional(Class<T> cls) {
				return Optional.absent();
			}

		}

		sut.environment.setGlobalSymbol("root", domain.create(IComposite.class, new EmptyComposite()));
		infix("type(root)=='object'").expectResult(b(true));
		infix("isobject(root)").expectResult(b(true));
		infix("bool(root)").expectResult(b(true));
	}

	@Test
	public void testTruthyCompositeObjects() {
		class TruthyComposite extends SimpleComposite implements CompositeTraits.Truthy {
			private final boolean value;

			public TruthyComposite(boolean value) {
				this.value = value;
			}

			@Override
			public String type() {
				return "truthy";
			}

			@Override
			public boolean isTruthy() {
				return value;
			}
		}

		sut.environment.setGlobalSymbol("trueComposite", domain.create(IComposite.class, new TruthyComposite(true)));
		sut.environment.setGlobalSymbol("falseComposite", domain.create(IComposite.class, new TruthyComposite(false)));

		infix("bool(trueComposite)").expectResult(TRUE);
		infix("bool(falseComposite)").expectResult(FALSE);
	}

	@Test
	public void testEmptyableCompositeObjects() {
		class EmptyableComposite extends SimpleComposite implements CompositeTraits.Emptyable {
			private final boolean value;

			public EmptyableComposite(boolean value) {
				this.value = value;
			}

			@Override
			public String type() {
				return "truthy";
			}

			@Override
			public boolean isEmpty() {
				return value;
			}
		}

		sut.environment.setGlobalSymbol("emptyComposite", domain.create(IComposite.class, new EmptyableComposite(true)));
		sut.environment.setGlobalSymbol("nonEmptyComposite", domain.create(IComposite.class, new EmptyableComposite(false)));

		infix("bool(emptyComposite)").expectResult(FALSE);
		infix("bool(nonEmptyComposite)").expectResult(TRUE);
	}

	@Test
	public void testCountableComposite() {
		class CountableComposite extends SimpleComposite implements CompositeTraits.Countable {

			private final int value;

			public CountableComposite(int value) {
				this.value = value;
			}

			@Override
			public String type() {
				return "countable";
			}

			@Override
			public int count() {
				return value;
			}
		}

		sut.environment.setGlobalSymbol("zeroLength", domain.create(IComposite.class, new CountableComposite(0)));
		sut.environment.setGlobalSymbol("nonZeroLength", domain.create(IComposite.class, new CountableComposite(5)));

		infix("bool(zeroLength)").expectResult(FALSE);
		infix("len(zeroLength)").expectResult(i(0));

		infix("bool(nonZeroLength)").expectResult(TRUE);
		infix("len(nonZeroLength)").expectResult(i(5));
	}

	@Test
	public void testCallableComposite() {
		class CallableTestTrait implements CompositeTraits.Callable {
			@Override
			public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
				Assert.assertTrue(argumentsCount.isPresent());
				for (int i = 0; i < argumentsCount.get(); i++)
					frame.stack().pop();

				Assert.assertTrue(returnsCount.isPresent());
				Assert.assertEquals(1, returnsCount.get().intValue());
				frame.stack().push(s("call:" + argumentsCount.get()));
			}
		}

		sut.environment.setGlobalSymbol("test", domain.create(IComposite.class, new SingleTraitComposite("callable", new CallableTestTrait())));

		infix("iscallable(test)").expectResult(TRUE);
		infix("test(2.0, 5)").expectResult(s("call:2"));
		infix("apply(test, 4, 7, 4)").expectResult(s("call:3"));
		infix("let([tmp = test], tmp(1,2))").expectResult(s("call:2"));
		infix("let([delayed = ()->test], delayed()(1,2,3,4))").expectResult(s("call:4"));
	}

	private static class TestStructuredComposite extends SimpleComposite implements CompositeTraits.Structured {
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
		public Optional<TypedValue> get(TypeDomain domain, String component) {
			if (component.equals("path")) return Optional.of(domain.create(String.class, Joiner.on("/").join(path)));
			else if (!component.startsWith(prefix)) return Optional.absent();
			else return Optional.of(domain.create(IComposite.class, new TestStructuredComposite(prefix, path, component)));
		}

		@Override
		public String type() {
			return "nested:" + path.size();
		}
	}

	@Test
	public void testDotOperator() {
		sut.environment.setGlobalSymbol("root", domain.create(IComposite.class, new TestStructuredComposite("")));
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
	public void testObjectIndexingWithBrackets() {
		sut.environment.setGlobalSymbol("test", domain.create(IComposite.class, new TestStructuredComposite("")));
		infix("test['path']").expectResult(s(""));
		infix("isobject(test['a'])").expectResult(TRUE);

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

	private class TestStructuredCompositeWithCallableReturn extends SimpleComposite implements CompositeTraits.Structured {
		@Override
		public Optional<TypedValue> get(final TypeDomain domain, final String component) {
			return Optional.of(domain.create(ICallable.class, new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue value) {
					final String result = component + ":" + value.as(String.class);
					return domain.create(String.class, result);
				}
			}));
		}

		@Override
		public String type() {
			return "callable_composite";
		}
	}

	@Test
	public void testDotOperatorResultCall() {
		sut.environment.setGlobalSymbol("test", domain.create(IComposite.class, new TestStructuredCompositeWithCallableReturn()));
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

	private class TestStructuredCompositeWithCompositeReturn extends SimpleComposite implements CompositeTraits.Structured {
		@Override
		public Optional<TypedValue> get(final TypeDomain domain, final String component) {
			return Optional.of(domain.create(ICallable.class, new UnaryFunction<TypedValue>() {
				@Override
				protected TypedValue call(TypedValue value) {
					final String path = component + ":" + value.as(String.class);
					class InnerComposite extends SimpleComposite implements CompositeTraits.Structured {
						@Override
						public String type() {
							return "whatever";
						}

						@Override
						public Optional<TypedValue> get(TypeDomain domain, String component) {
							return Optional.of(domain.create(String.class, path + ":" + component));
						}
					}
					return domain.create(IComposite.class, new InnerComposite());
				}
			}));
		}

		@Override
		public String type() {
			return "callable_composite";
		}
	}

	@Test
	public void testNestedDotOperatorOnDotResultCall() {
		// just some crazy brackets
		sut.environment.setGlobalSymbol("test", domain.create(IComposite.class, new TestStructuredCompositeWithCompositeReturn()));
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
		sut.environment.setGlobalSymbol("test", domain.create(IComposite.class, new TestStructuredComposite("m_")));

		infix("test.m_a.m_b.{ path }").expectResult(s("m_a/m_b"));
		infix("test.m_a.m_b.{ m_c }.path").expectResult(s("m_a/m_b/m_c"));
		infix("test.{ m_a }.m_b.m_c.path").expectResult(s("m_a/m_b/m_c"));
		infix("test.{ m_a.m_b }.m_c.path").expectResult(s("m_a/m_b/m_c"));

		infix("test.m_a.m_b.{ cons(path, m_c.path) }").expectResult(cons(s("m_a/m_b"), s("m_a/m_b/m_c")));
	}

	@Test
	public void testIndexableComposite() {
		class TestIndexableComposite extends SimpleComposite implements CompositeTraits.Indexable {
			private int count;

			@Override
			public Optional<TypedValue> get(TypedValue index) {
				return Optional.of(cons(i(count++), index));
			}

			@Override
			public String type() {
				return "indexable";
			}
		}

		final TypedValue test = domain.create(IComposite.class, new TestIndexableComposite());
		sut.environment.setGlobalSymbol("test", test);

		infix("test['ab']").expectResult(cons(i(0), s("ab")));
		infix("test[4.01]").expectResult(cons(i(1), d(4.01)));
		infix("test[test]").expectResult(cons(i(2), test));

		infix("cdr(test[test])[-2]").expectResult(cons(i(4), i(-2)));
	}

	@Test
	public void testEnumerableComposite() {
		class TestEnumerableComposite extends SimpleComposite implements CompositeTraits.Enumerable {
			@Override
			public TypedValue get(TypeDomain domain, int index) {
				return domain.create(BigInteger.class, BigInteger.valueOf(index + 2));
			}

			@Override
			public String type() {
				return "enumerable";
			}

			@Override
			public int count() {
				return 5;
			}
		}

		final TypedValue test = domain.create(IComposite.class, new TestEnumerableComposite());
		sut.environment.setGlobalSymbol("test", test);

		infix("test[5]").expectResult(i(7));
		infix("len(test)").expectResult(i(5));
		infix("bool(test)").expectResult(TRUE);
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
	public void testCodeParsingInPostfixParser() {
		postfix("{ 1 2 +} iscode").expectResult(b(true));

		postfix("{ 1 2 +} execute").expectResult(i(3));
		postfix("{ 1 2 +} execute$1,1").expectResult(i(3));

		postfix("{ 1 2 + 3 4 -} execute").expectResults(i(3), i(-1));
		postfix("{ 1 2 + 3 4 -} execute$1,2").expectResults(i(3), i(-1));
	}

	@Test
	public void testCodeSymbol() {
		infix("iscode(code(2 + 3))").expectResult(TRUE);
		infix("(iscode (code (+ 2 3)))").expectResult(TRUE);

		infix("execute(code(2 + 3))").expectResult(i(5));
		prefix("(execute (code (+ 2 3)))").expectResult(i(5));
	}

	@Test
	public void testCodeBrackets() {
		infix("iscode({2 + 3})").expectResult(TRUE);
		infix("execute({2 + 3})").expectResult(i(5));

		prefix("(iscode {6})").expectResult(TRUE);
		prefix("(execute {6})").expectResult(i(6));

		prefix("(iscode {(+ 2 3)})").expectResult(TRUE);
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
	public void testLetExplicitSymbolVariableNames() {
		infix("let([#x:2, #y:3], x + y)").expectResult(i(5));
	}

	@Test
	public void testLetSymbolVariableNamesFromCall() {
		sut.environment.setGlobalSymbol("x", domain.create(Symbol.class, Symbol.get("a")));
		sut.environment.setGlobalSymbol("y", domain.create(Symbol.class, Symbol.get("b")));

		infix("let([(x()):2,(y()):3], a + b)").expectResult(i(5));
	}

	@Test
	public void testLetSymbolStringVariableNames() {
		infix("let(['x':2,'y':3], x + y)").expectResult(i(5));
		prefix("(let [(:'x' 2), (:'y' 3)] (+ x y))").expectResult(i(5));
	}

	@Test
	public void testNestedLet() {
		infix("let([x:2], let([y:x+2], x + y))").expectResult(i(6));
		infix("let([x:#y], let([(x):2], y + 1))").expectResult(i(3));
	}

	@Test
	public void testLetWithExplicitList() {
		infix("let(list(#x:{2}, #y:{1+2}), x+y)").expectResult(i(5));
		infix("let([l:list(#x:{2}, #y:{1+2})], let(l, x+y))").expectResult(i(5));
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
	public void testLetSeq() {
		infix("letseq([x:2, y:x+3], y)").expectResult(i(5));

		infix("letseq([x:2, y():x+3], y())").expectResult(i(5));
		infix("letseq([x():2, y:x()+3], y)").expectResult(i(5));

		infix("let([z:9], letseq([x:2, y:x+z], y))").expectResult(i(11));
		infix("let([x:5], letseq([x:2, y:x+3], y))").expectResult(i(5));
		infix("let([x:5], letseq([y:x+3, x:2], y))").expectResult(i(8));
		infix("let([x:5], letseq([y:x+3, x:2, y:x+3], y))").expectResult(i(5));
	}

	@Test(expected = ExecutionErrorException.class)
	public void testLetSeqSelfSymbolCall() {
		infix("letseq([x:2], letseq([x:x], x))").expectResult(i(2));
	}

	@Test
	public void testLetRec() {
		infix("letrec([odd(v):if(v==0,false,even(v-1)), even(v):if(v==0,true,odd(v-1))], even(6))").expectResult(TRUE);
		infix("letrec([odd(v):if(v==0,false,even(v-1)), even(v):if(v==0,true,odd(v-1))], odd(5))").expectResult(TRUE);
		infix("letrec([odd(v):if(v==0,false,even(v-1)), even(v):if(v==0,true,odd(v-1))], odd(4))").expectResult(FALSE);
		infix("letrec([odd(v):if(v==0,false,even(v-1)), even(v):if(v==0,true,odd(v-1))], even(3))").expectResult(FALSE);

		infix("letrec([x:2], letrec([y:x], x))").expectResult(i(2));
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
		infix("(symbol)('test')").expectResult(sym("test"));
		infix("(max)(1, 3, 2)").expectResult(i(3));
	}

	@Test
	public void testHighOrderCallable() {
		sut.environment.setGlobalSymbol("test", new BinaryFunction<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue left, TypedValue right) {
				final BigInteger closure = left.as(BigInteger.class).subtract(right.as(BigInteger.class));
				return domain.create(ICallable.class, new UnaryFunction<TypedValue>() {
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

		infix("let([plus:@+], plus(12,34))").expectResult(i(46));
		prefix("(let [(:minus @-)] (minus 12 34))").expectResult(i(-22));
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
		infix("('a'->a+2)(5)").expectResult(i(7));

		prefix("(apply (-> a (+ a 2)) 2)").expectResult(i(4));
		prefix("(apply (-> a {(+ a 2)}) 2)").expectResult(i(4));
		prefix("(apply (->[a] (+ a 2)) 2)").expectResult(i(4));
	}

	@Test
	public void testBinaryArgLambdaOperator() {
		infix("iscallable((a, b) -> a + b)").expectResult(TRUE);

		infix("((a, b)->a-b)(1, 2)").expectResult(i(-1));
		infix("(('a', b)->a-b)(1, 3)").expectResult(i(-2));

		prefix("(apply (-> [a, b] (- a b)) 2 3)").expectResult(i(-1));
		prefix("(apply (-> ['a', b] (- a b)) 3 5)").expectResult(i(-2));
	}

	@Test
	public void testLambdaOperatorInLetScope() {
		infix("let([f = (a,b)->a+b], f(3, 4))").expectResult(i(7));
		prefix("(let [(= f (-> [a,b] (+ a b)))] (f 4 5))").expectResult(i(9));
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
		infix("let([a(b,c):b-c], a(1,2))").expectResult(i(-1));
		infix("let([f(n):if(n<=0,1,f(n-1)*n)], f(6))").expectResult(i(720));
	}

	@Test
	public void testPromiseSyntax() {
		infix("let([p:delay(2)], ispromise(p))").expectResult(TRUE);
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
		infix("match('a' -> 1, 2 -> 'b')('a')").expectResult(i(1));
		infix("match('a' -> 1, 2 -> 'b')(2)").expectResult(s("b"));
	}

	@Test
	public void testMatchWithGlobalSymbols() {
		infix("match(true -> 1, false -> 0)(true)").expectResult(i(1));
		infix("match(true -> 1, false -> 0)(false)").expectResult(i(0));

		infix("match(str(2) -> 1, 2 -> 0)('2')").expectResult(i(1));
		infix("match(str(2) -> 1, 2 -> 0)(2)").expectResult(i(0));
	}

	@Test
	public void testWildcardMatch() {
		infix("match('a' -> 1, _ -> '?')('a')").expectResult(i(1));
		infix("match('a' -> 1, _ -> '?')('dummy')").expectResult(s("?"));
	}

	@Test
	public void testBindMatch() {
		infix("match(a -> a)('b')").expectResult(s("b"));
		infix("match(a -> a + 1)(2)").expectResult(i(3));
		infix("match(a -> 'd')(2)").expectResult(s("d"));
	}

	@Test
	public void testSimpleListMatch() {
		infix("match(1:2:3 -> 'ok', _ -> 'fail')(1:2:3)").expectResult(s("ok"));
		infix("match(1:2:3 -> 'ok', _ -> 'fail')(1:2:4)").expectResult(s("fail"));
		infix("match(1:2:3 -> 'ok', _ -> 'fail')(1:2)").expectResult(s("fail"));
		infix("match(1:2:3 -> 'ok', _ -> 'fail')(1:2:3:4)").expectResult(s("fail"));
	}

	@Test
	public void testSimpleTerminatedListMatch() {
		infix("match([1,2,3] -> 'ok', _ -> 'fail')([1,2,3])").expectResult(s("ok"));
		infix("match([1,2,3] -> 'ok', _ -> 'fail')(1:2:3:null)").expectResult(s("ok"));
		infix("match([1,2,3] -> 'ok', _ -> 'fail')(1:2:3)").expectResult(s("fail"));
	}

	@Test
	public void testEmptyListMatch() {
		infix("match([] -> 'ok', _ -> 'fail')([])").expectResult(s("ok"));
		infix("match([] -> 'ok', _ -> 'fail')(null)").expectResult(s("ok"));
		infix("match([] -> 'ok', _ -> 'fail')([1])").expectResult(s("fail"));
	}

	@Test
	public void testListMatchWithWildcard() {
		infix("match(1:2:_ -> 'ok', _ -> 'fail')(1:2:3)").expectResult(s("ok"));
		infix("match(1:2:_ -> 'ok', _ -> 'fail')(1:2:4)").expectResult(s("ok"));
		infix("match(1:2:_ -> 'ok', _ -> 'fail')(1:2)").expectResult(s("fail"));
		infix("match(1:2:_ -> 'ok', _ -> 'fail')(1:2:3:4)").expectResult(s("ok"));
	}

	@Test
	public void testNestedListMatch() {
		infix("match(1:(2:3):4 -> 'ok', _ -> 'fail')(1:(2:3):4)").expectResult(s("ok"));
		infix("match(1:(2:3):4 -> 'ok', _ -> 'fail')(1:2:3:4)").expectResult(s("fail"));
	}

	@Test
	public void testListMatchWithBinding() {
		infix("match(1:2:xs -> xs, _ -> 'fail')(1:2:3)").expectResult(i(3));
		infix("match(1:2:xs -> xs, _ -> 'fail')(1:2:4)").expectResult(i(4));
		infix("match(1:2:xs -> xs, _ -> 'fail')(1:2)").expectResult(s("fail"));
		infix("match(1:2:xs -> xs, _ -> 'fail')(1:2:3:4)").expectResult(cons(i(3), i(4)));
	}

	@Test
	public void testNestedListMatchWithBinding() {
		infix("match(1:(2:a):b -> a:b, _ -> 'fail')(1:(2:3):4)").expectResult(cons(i(3), i(4)));
		infix("match(1:(2:a):b -> a:b, _ -> 'fail')(1:(2:3:5):4)").expectResult(cons(cons(i(3), i(5)), i(4)));
		infix("match(1:(2:a):b -> a:b, _ -> 'fail')(1:(2:3:5):4:6)").expectResult(cons(cons(i(3), i(5)), cons(i(4), i(6))));
	}

	@Test
	public void testMatchScope() {
		infix("let([a:2], match(a -> a)(5))").expectResult(i(5));
		infix("let([b:2], match(a -> b)(5))").expectResult(i(2));

		infix("let([x: 'pre'], let([b: match(1 -> x, _ -> false)], let([x: 'post'], b(1))))").expectResult(s("pre"));
	}

	@Test
	public void testRecursiveMatch() {
		infix("letrec([f(l, c) : match(x:xs -> f(xs, c+1), [] -> c)(l)], f([], 0))").expectResult(i(0));
		infix("letrec([f(l, c) : match(x:xs -> f(xs, c+1), [] -> c)(l)], f([1], 0))").expectResult(i(1));
		infix("letrec([f(l, c) : match(x:xs -> f(xs, c+1), [] -> c)(l)], f([1,2,3], 0))").expectResult(i(3));
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
}
