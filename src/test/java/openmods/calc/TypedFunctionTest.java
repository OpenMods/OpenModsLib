package openmods.calc;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import openmods.calc.types.multi.IConverter;
import openmods.calc.types.multi.TypeDomain;
import openmods.calc.types.multi.TypedFunction;
import openmods.calc.types.multi.TypedFunction.AmbiguousDispatchException;
import openmods.calc.types.multi.TypedFunction.DispatchArg;
import openmods.calc.types.multi.TypedFunction.DispatchException;
import openmods.calc.types.multi.TypedFunction.MultiReturn;
import openmods.calc.types.multi.TypedFunction.MultipleReturn;
import openmods.calc.types.multi.TypedFunction.OptionalArgs;
import openmods.calc.types.multi.TypedFunction.RawArg;
import openmods.calc.types.multi.TypedFunction.RawReturn;
import openmods.calc.types.multi.TypedFunction.Variant;
import openmods.calc.types.multi.TypedValue;
import openmods.reflection.TypeVariableHolderHandler;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class TypedFunctionTest {

	static {
		TypeVariableHolderHandler.initializeClass(TypeDomain.TypeVariableHolders.class);
		TypeVariableHolderHandler.initializeClass(TypedFunction.class);
	}

	private static final Optional<Integer> SKIP = Optional.<Integer> absent();

	private static final TypeDomain domain = new TypeDomain();
	static {
		domain.registerType(Integer.class);
		domain.registerType(Boolean.class);
		domain.registerType(String.class);
		domain.registerType(Number.class);
		domain.registerCast(Integer.class, Number.class);
		domain.registerConverter(new IConverter<Boolean, Integer>() {
			@Override
			public Integer convert(Boolean value) {
				return value? 1 : 0;
			}
		});
	}

	private static TypedValue wrap(int v) {
		return domain.create(Integer.class, v);
	}

	private static TypedValue wrap(boolean v) {
		return domain.create(Boolean.class, v);
	}

	private static TypedValue wrap(String v) {
		return domain.create(String.class, v);
	}

	private static <T> void assertValueEquals(TypedValue value, Class<? extends T> expectedType, T expectedValue) {
		Assert.assertEquals(expectedValue, value.value);
		Assert.assertEquals(expectedType, value.type);
		Assert.assertEquals(domain, value.domain);
	}

	private static void assertValueEquals(TypedValue value, TypedValue expected) {
		assertValueEquals(value, expected.type, expected.value);
	}

	private static TypedValue execute(ISymbol<TypedValue> f, TypedValue... values) {
		return execute(f, Optional.of(values.length), values);
	}

	private static TypedValue execute(ISymbol<TypedValue> f, Optional<Integer> argCount, TypedValue... values) {
		final TopFrame<TypedValue> frame = new TopFrame<TypedValue>();
		for (TypedValue v : values)
			frame.stack().push(v);
		f.execute(frame, argCount, SKIP);
		final TypedValue result = frame.stack().pop();
		Assert.assertTrue(frame.stack().isEmpty());
		return result;
	}

	private static List<TypedValue> execute(ISymbol<TypedValue> f, Optional<Integer> argCount, int rets, TypedValue... values) {
		final TopFrame<TypedValue> frame = new TopFrame<TypedValue>();
		for (TypedValue v : values)
			frame.stack().push(v);
		f.execute(frame, argCount, Optional.of(rets));
		List<TypedValue> results = Lists.newArrayList();
		for (int i = 0; i < rets; i++)
			results.add(frame.stack().pop());

		Assert.assertTrue(frame.stack().isEmpty());
		return Lists.reverse(results);
	}

	private static <T> TypedFunction createFunction(T target, Class<? extends T> cls) {
		TypedFunction.Builder builder = new TypedFunction.Builder(domain);
		builder.addVariants(target, cls);
		return builder.build();
	}

	@Test
	public void testSingleMethodAllMandatoryArgs() {
		abstract class Intf {
			@Variant
			public abstract Integer test(Boolean a, String b, Number c);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		final TypedValue arg1 = wrap(true);
		final TypedValue arg2 = wrap("Hello world");
		final TypedValue arg3 = wrap(7);
		Mockito.when(mock.test(anyBoolean(), anyString(), anyInt())).thenReturn(5);
		assertValueEquals(execute(target, arg1, arg2, arg3), Integer.class, 5);
		Mockito.verify(mock).test(true, "Hello world", 7);

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodMandatoryRawArgs() {
		abstract class Intf {
			@Variant
			public abstract Integer test(Boolean a, @RawArg TypedValue b, Number c);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		final TypedValue arg1 = wrap(true);
		final TypedValue arg2 = wrap("Hello world");
		final TypedValue arg3 = wrap(7);
		Mockito.when(mock.test(anyBoolean(), any(TypedValue.class), anyInt())).thenReturn(5);
		assertValueEquals(execute(target, arg1, arg2, arg3), Integer.class, 5);
		Mockito.verify(mock).test(true, arg2, 7);

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodOptionalArgs() {
		abstract class Intf {
			@Variant
			public abstract Integer test(Boolean a, @OptionalArgs Optional<String> b, Optional<Number> c);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		final TypedValue arg1 = wrap(true);
		final TypedValue arg2 = wrap("Hello world");
		final TypedValue arg3 = wrap(6);
		Mockito.when(mock.test(anyBoolean(), Matchers.<Optional<String>> any(), Matchers.<Optional<Number>> any())).thenReturn(7);

		assertValueEquals(execute(target, arg1, arg2, arg3), Integer.class, 7);
		Mockito.verify(mock).test(true, Optional.of("Hello world"), Optional.<Number> of(6));

		assertValueEquals(execute(target, arg1, arg2), Integer.class, 7);
		Mockito.verify(mock).test(true, Optional.of("Hello world"), Optional.<Number> absent());

		assertValueEquals(execute(target, arg1), Integer.class, 7);
		Mockito.verify(mock).test(true, Optional.<String> absent(), Optional.<Number> absent());

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodOptionalRawArgs() {
		abstract class Intf {
			@Variant
			public abstract Integer test(Boolean a, @RawArg @OptionalArgs Optional<TypedValue> b, Optional<Number> c);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		final TypedValue arg1 = wrap(true);
		final TypedValue arg2 = wrap("Hello world");
		final TypedValue arg3 = wrap(6);
		Mockito.when(mock.test(anyBoolean(), Matchers.<Optional<TypedValue>> any(), Matchers.<Optional<Number>> any())).thenReturn(7);

		assertValueEquals(execute(target, arg1, arg2, arg3), Integer.class, 7);
		Mockito.verify(mock).test(true, Optional.of(arg2), Optional.<Number> of(6));

		assertValueEquals(execute(target, arg1, arg2), Integer.class, 7);
		Mockito.verify(mock).test(true, Optional.of(arg2), Optional.<Number> absent());

		assertValueEquals(execute(target, arg1), Integer.class, 7);
		Mockito.verify(mock).test(true, Optional.<TypedValue> absent(), Optional.<Number> absent());

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodAllOptionalArgs() {
		abstract class Intf {
			@Variant
			public abstract Integer test(@OptionalArgs Optional<String> a, Optional<Number> b);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		final TypedValue arg1 = wrap("Hello world");
		final TypedValue arg2 = wrap(6);
		Mockito.when(mock.test(Matchers.<Optional<String>> any(), Matchers.<Optional<Number>> any())).thenReturn(7);

		assertValueEquals(execute(target, arg1, arg2), Integer.class, 7);
		Mockito.verify(mock).test(Optional.of("Hello world"), Optional.<Number> of(6));

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodVariadicArgs() {
		abstract class Intf {
			@Variant
			public abstract Integer test(Boolean a, Integer... bs);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		final TypedValue arg1 = wrap(false);
		final TypedValue arg2a = wrap(6);
		final TypedValue arg2b = wrap(7);

		Mockito.when(mock.test(anyBoolean(), Matchers.<Integer[]> anyVararg())).thenReturn(5);

		assertValueEquals(execute(target, arg1), Integer.class, 5);
		Mockito.verify(mock).test(false);

		assertValueEquals(execute(target, arg1, arg2a), Integer.class, 5);
		Mockito.verify(mock).test(false, 6);

		assertValueEquals(execute(target, arg1, arg2a, arg2b), Integer.class, 5);
		Mockito.verify(mock).test(false, 6, 7);

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodVariadicRawArgs() {
		abstract class Intf {
			@Variant
			public abstract Integer test(Boolean a, @RawArg TypedValue... bs);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		final TypedValue arg1 = wrap(false);
		final TypedValue arg2a = wrap(6);
		final TypedValue arg2b = wrap(7);

		Mockito.when(mock.test(anyBoolean(), Matchers.<TypedValue[]> anyVararg())).thenReturn(5);

		assertValueEquals(execute(target, arg1), Integer.class, 5);
		Mockito.verify(mock).test(false);

		assertValueEquals(execute(target, arg1, arg2a), Integer.class, 5);
		Mockito.verify(mock).test(false, arg2a);

		assertValueEquals(execute(target, arg1, arg2a, arg2b), Integer.class, 5);
		Mockito.verify(mock).test(false, arg2a, arg2b);

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodOptionalAndVariadicArgs() {
		abstract class Intf {
			@Variant
			public abstract Integer test(Boolean a, @OptionalArgs Optional<Number> b, Integer... cs);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		final TypedValue arg1 = wrap(false);
		final TypedValue arg2 = wrap(5);
		final TypedValue arg3a = wrap(6);
		final TypedValue arg3b = wrap(7);

		Mockito.when(mock.test(anyBoolean(), Matchers.<Optional<Number>> any(), Matchers.<Integer[]> anyVararg())).thenReturn(5);

		assertValueEquals(execute(target, arg1), Integer.class, 5);
		Mockito.verify(mock).test(false, Optional.<Number> absent());

		assertValueEquals(execute(target, arg1, arg2), Integer.class, 5);
		Mockito.verify(mock).test(false, Optional.<Number> of(5));

		assertValueEquals(execute(target, arg1, arg2, arg3a), Integer.class, 5);
		Mockito.verify(mock).test(false, Optional.<Number> of(5), 6);

		assertValueEquals(execute(target, arg1, arg2, arg3a, arg3b), Integer.class, 5);
		Mockito.verify(mock).test(false, Optional.<Number> of(5), 6, 7);

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodRawReturn() {
		abstract class Intf {
			@Variant
			@RawReturn
			public abstract TypedValue test();
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		final TypedValue ret = wrap("Hello world");
		Mockito.when(mock.test()).thenReturn(ret);
		assertValueEquals(execute(target, SKIP), ret);
		Mockito.verify(mock).test();

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodMultiReturn() {
		abstract class Intf {
			@Variant
			public abstract MultipleReturn test();
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		final TypedValue ret1 = wrap("Hello world");
		final TypedValue ret2 = wrap(7);
		Mockito.when(mock.test()).thenReturn(MultipleReturn.wrap(ret1, ret2));
		Assert.assertEquals(execute(target, SKIP, 2), Arrays.asList(ret1, ret2));
		Mockito.verify(mock).test();

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodMultiArrayReturn() {
		abstract class Intf {
			@Variant
			@MultiReturn
			public abstract Integer[] test();
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		Mockito.when(mock.test()).thenReturn(new Integer[] { 3, 1, 5 });
		Assert.assertEquals(execute(target, SKIP, 3), Arrays.asList(wrap(3), wrap(1), wrap(5)));
		Mockito.verify(mock).test();

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleMethodMultiIterableReturn() {
		abstract class Intf {
			@Variant
			@MultiReturn
			public abstract List<String> test();
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		Mockito.when(mock.test()).thenReturn(Lists.newArrayList("b", "c", "a", "d"));
		Assert.assertEquals(execute(target, SKIP, 4), Arrays.asList(wrap("b"), wrap("c"), wrap("a"), wrap("d")));
		Mockito.verify(mock).test();

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleArgumentDispatch() {
		abstract class Intf {
			@Variant
			public abstract Integer test(@DispatchArg Integer v);

			@Variant
			public abstract String test(@DispatchArg String v);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		Mockito.when(mock.test(anyInt())).thenReturn(7);
		Mockito.when(mock.test(anyString())).thenReturn("b");

		assertValueEquals(execute(target, wrap(6)), Integer.class, 7);
		Mockito.verify(mock).test(6);

		assertValueEquals(execute(target, wrap("a")), String.class, "b");
		Mockito.verify(mock).test("a");

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testOneDispatchOneNonDispatchArguments() {
		abstract class Intf {
			@Variant
			public abstract Integer test(@DispatchArg Integer v, Boolean n);

			@Variant
			public abstract String test(@DispatchArg String v, Boolean n);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		Mockito.when(mock.test(anyInt(), anyBoolean())).thenReturn(7);
		Mockito.when(mock.test(anyString(), anyBoolean())).thenReturn("b");

		assertValueEquals(execute(target, wrap(6), wrap(true)), Integer.class, 7);
		Mockito.verify(mock).test(6, true);

		assertValueEquals(execute(target, wrap("a"), wrap(true)), String.class, "b");
		Mockito.verify(mock).test("a", true);

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testSingleArgumentDispatchWithExtraEntries() {
		abstract class Intf {
			@Variant
			public abstract Integer test(@DispatchArg(extra = { Boolean.class }) Integer v);

			@Variant
			public abstract String test(@DispatchArg String v);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		Mockito.when(mock.test(anyInt())).thenReturn(7);
		Mockito.when(mock.test(anyString())).thenReturn("b");

		assertValueEquals(execute(target, wrap(6)), Integer.class, 7);
		Mockito.verify(mock).test(6);

		assertValueEquals(execute(target, wrap(true)), Integer.class, 7);
		Mockito.verify(mock).test(1);

		assertValueEquals(execute(target, wrap("a")), String.class, "b");
		Mockito.verify(mock).test("a");

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testDoubleArgumentDispatch() {
		abstract class Intf {
			@Variant
			public abstract Integer test(@DispatchArg Integer v1, @DispatchArg Boolean v2);

			@Variant
			public abstract String test(@DispatchArg String v, @DispatchArg Integer v2);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		Mockito.when(mock.test(anyInt(), anyBoolean())).thenReturn(7);
		Mockito.when(mock.test(anyString(), anyInt())).thenReturn("b");

		assertValueEquals(execute(target, wrap(6), wrap(false)), Integer.class, 7);
		Mockito.verify(mock).test(6, false);

		try {
			execute(target, wrap(6), wrap(5));
			Assert.fail();
		} catch (DispatchException e) {}

		assertValueEquals(execute(target, wrap("a"), wrap(5)), String.class, "b");
		Mockito.verify(mock).test("a", 5);

		try {
			execute(target, wrap("a"), wrap(true));
			Assert.fail();
		} catch (DispatchException e) {}

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testDifferentLengthDispatch() {
		abstract class Intf {
			@Variant
			public abstract Integer test(@DispatchArg Integer v1, @DispatchArg Integer v2);

			@Variant
			public abstract Integer test(@DispatchArg Integer v1);

			@Variant
			public abstract String test();
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		Mockito.when(mock.test(anyInt(), anyInt())).thenReturn(2);
		Mockito.when(mock.test(anyInt())).thenReturn(1);
		Mockito.when(mock.test()).thenReturn("zero");

		assertValueEquals(execute(target, wrap(1), wrap(2)), Integer.class, 2);
		Mockito.verify(mock).test(1, 2);

		assertValueEquals(execute(target, wrap(1)), Integer.class, 1);
		Mockito.verify(mock).test(1);

		assertValueEquals(execute(target), String.class, "zero");
		Mockito.verify(mock).test();

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testOptionalArgumentDispatch() {
		abstract class Intf {
			@Variant
			public abstract Integer test(@OptionalArgs @DispatchArg Optional<Integer> v);

			@Variant
			public abstract String test(@DispatchArg String v);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		Mockito.when(mock.test(Matchers.<Optional<Integer>> any())).thenReturn(7);
		Mockito.when(mock.test(anyString())).thenReturn("b");

		assertValueEquals(execute(target, wrap(6)), Integer.class, 7);
		Mockito.verify(mock).test(Optional.of(6));

		assertValueEquals(execute(target), Integer.class, 7);
		Mockito.verify(mock).test(Optional.<Integer> absent());

		assertValueEquals(execute(target, wrap("a")), String.class, "b");
		Mockito.verify(mock).test("a");

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test
	public void testDoubleArgumentDispatchMixedMatch() {
		abstract class Intf {
			@Variant
			public abstract Integer test(@DispatchArg Integer v1, @DispatchArg Boolean v2);

			@Variant
			public abstract Integer test(@DispatchArg Integer v1, @DispatchArg String v2);

			@Variant
			public abstract String test(@DispatchArg String v, @DispatchArg Integer v2);

			@Variant
			public abstract String test(@DispatchArg Boolean v, @DispatchArg Integer v2);
		}

		final Intf mock = Mockito.mock(Intf.class);
		TypedFunction target = createFunction(mock, Intf.class);

		Mockito.when(mock.test(anyInt(), anyBoolean())).thenReturn(7);
		Mockito.when(mock.test(anyInt(), anyString())).thenReturn(8);
		Mockito.when(mock.test(anyString(), anyInt())).thenReturn("b");
		Mockito.when(mock.test(anyBoolean(), anyInt())).thenReturn("c");

		assertValueEquals(execute(target, wrap(7), wrap(false)), Integer.class, 7);
		Mockito.verify(mock).test(7, false);

		assertValueEquals(execute(target, wrap(8), wrap("c")), Integer.class, 8);
		Mockito.verify(mock).test(8, "c");

		assertValueEquals(execute(target, wrap("a"), wrap(5)), String.class, "b");
		Mockito.verify(mock).test("a", 5);

		assertValueEquals(execute(target, wrap(true), wrap(6)), String.class, "c");
		Mockito.verify(mock).test(true, 6);

		Mockito.verifyNoMoreInteractions(mock);
	}

	@Test(expected = AmbiguousDispatchException.class)
	public void testTwoMethodsNoDispatch() {
		class Intf {
			@Variant
			public Integer test(Integer v) {
				return null;
			}

			@Variant
			public Integer test(String v) {
				return null;
			}
		}

		createFunction(new Intf(), Intf.class);
	}

	@Test(expected = AmbiguousDispatchException.class)
	public void testTwoMethodsSameDispatch() {
		class Intf {
			@Variant
			public Integer test(Boolean pre, @DispatchArg Integer v, String post) {
				return null;
			}

			@Variant
			public Integer test(String pre, @DispatchArg Integer v, Integer post) {
				return null;
			}
		}

		createFunction(new Intf(), Intf.class);
	}

	@Test(expected = AmbiguousDispatchException.class)
	public void testTwoMethodsSameDispatchExtraDispatch() {
		class Intf {
			@Variant
			public Integer test(Boolean pre, @DispatchArg Integer v, @DispatchArg String post) {
				return null;
			}

			@Variant
			public Integer test(@DispatchArg String pre, @DispatchArg Integer v, Integer post) {
				return null;
			}
		}

		createFunction(new Intf(), Intf.class);
	}

	@Test(expected = AmbiguousDispatchException.class)
	public void testDispatchConflictOptionalVsArgMissing() {
		class Intf {
			@Variant
			public Integer test(Boolean pre, @OptionalArgs @DispatchArg Optional<Integer> v) {
				return null;
			}

			@Variant
			public Integer test(String pre, @OptionalArgs @DispatchArg Optional<Boolean> v) {
				return null;
			}
		}

		createFunction(new Intf(), Intf.class);
	}

	@Test(expected = AmbiguousDispatchException.class)
	public void testDispatchConflictOptionalVsMandatorySameType() {
		class Intf {
			@Variant
			public Integer test(@OptionalArgs @DispatchArg Optional<Integer> v) {
				return null;
			}

			@Variant
			public Integer test(@DispatchArg Integer v) {
				return null;
			}
		}

		createFunction(new Intf(), Intf.class);
	}

	@Test(expected = AmbiguousDispatchException.class)
	public void testOptionalDispatchVsMissingArg() {
		class Intf {
			@Variant
			public Integer test(@OptionalArgs @DispatchArg Optional<Integer> v) {
				return null;
			}

			@Variant
			public Integer test() {
				return null;
			}
		}

		createFunction(new Intf(), Intf.class);
	}

	@Test(expected = RuntimeException.class)
	public void testMissingConversionOnMandatoryDispatchArg() {
		class Intf {
			@Variant
			public Integer test(@DispatchArg(extra = { String.class }) Integer v) {
				return null;
			}
		}

		createFunction(new Intf(), Intf.class);
	}

	@Test(expected = RuntimeException.class)
	public void testMissingConversionOnOptionalDispatchArg() {
		class Intf {
			@Variant
			public Integer test(@OptionalArgs @DispatchArg(extra = { String.class }) Optional<Integer> v) {
				return null;
			}
		}

		createFunction(new Intf(), Intf.class);
	}

}
