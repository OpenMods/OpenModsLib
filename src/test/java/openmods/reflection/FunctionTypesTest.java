package openmods.reflection;

import openmods.reflection.MethodAccess.Function0;
import openmods.reflection.MethodAccess.Function1;
import openmods.reflection.MethodAccess.Function2;
import openmods.reflection.MethodAccess.Function3;
import openmods.reflection.MethodAccess.Function4;
import openmods.reflection.MethodAccess.Function5;
import openmods.reflection.MethodAccess.FunctionVar;
import org.junit.Assert;
import org.junit.Test;

public class FunctionTypesTest {

	static {
		TypeVariableHolderHandler.initializeClass(MethodAccess.TypeVariableHolders.class);
	}

	private static Class<?>[] wrap(Class<?>... cls) {
		return cls;
	}

	@Test
	public void testFunctionVar() {
		abstract class F implements FunctionVar<Integer> {}

		Assert.assertEquals(Integer.class, MethodAccess.resolveReturnType(F.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFunctionVarParams() {
		abstract class F implements FunctionVar<Integer> {}
		MethodAccess.resolveParameterTypes(F.class);
	}

	@Test
	public void testFunction0() {
		abstract class F implements Function0<Integer> {}

		Assert.assertEquals(Integer.class, MethodAccess.resolveReturnType(F.class));
		Assert.assertArrayEquals(wrap(), MethodAccess.resolveParameterTypes(F.class));
	}

	@Test
	public void testFunction1() {
		abstract class F implements Function1<Boolean, Integer> {}

		Assert.assertEquals(Boolean.class, MethodAccess.resolveReturnType(F.class));
		Assert.assertArrayEquals(wrap(Integer.class), MethodAccess.resolveParameterTypes(F.class));
	}

	@Test
	public void testFunction2() {
		abstract class F implements Function2<Integer, Boolean, Character> {}

		Assert.assertEquals(Integer.class, MethodAccess.resolveReturnType(F.class));
		Assert.assertArrayEquals(wrap(Boolean.class, Character.class), MethodAccess.resolveParameterTypes(F.class));
	}

	@Test
	public void testFunction3() {
		abstract class F implements Function3<Character, Integer, Long, String> {}

		Assert.assertEquals(Character.class, MethodAccess.resolveReturnType(F.class));
		Assert.assertArrayEquals(wrap(Integer.class, Long.class, String.class), MethodAccess.resolveParameterTypes(F.class));
	}

	@Test
	public void testFunction4() {
		abstract class F implements Function4<String, Double, Integer, Boolean, Character> {}

		Assert.assertEquals(String.class, MethodAccess.resolveReturnType(F.class));
		Assert.assertArrayEquals(wrap(Double.class, Integer.class, Boolean.class, Character.class), MethodAccess.resolveParameterTypes(F.class));
	}

	@Test
	public void testFunction5() {
		abstract class F implements Function5<Void, Integer, Double, Long, Float, Boolean> {}

		Assert.assertEquals(Void.class, MethodAccess.resolveReturnType(F.class));
		Assert.assertArrayEquals(wrap(Integer.class, Double.class, Long.class, Float.class, Boolean.class), MethodAccess.resolveParameterTypes(F.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMultibaseFunction() {
		abstract class F implements Function0<Integer>, Function1<Integer, Boolean> {}
		MethodAccess.resolveParameterTypes(F.class);
	}

}
