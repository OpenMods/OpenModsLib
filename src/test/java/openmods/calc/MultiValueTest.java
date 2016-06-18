package openmods.calc;

import com.google.common.base.Optional;
import openmods.calc.types.multi.IConverter;
import openmods.calc.types.multi.TypeDomain;
import openmods.calc.types.multi.TypeDomain.Coercion;
import openmods.calc.types.multi.TypeDomain.ITruthEvaluator;
import openmods.calc.types.multi.TypedValue;
import openmods.reflection.TypeVariableHolderHandler;
import org.junit.Assert;
import org.junit.Test;

public class MultiValueTest {

	private static void assertValueEquals(TypedValue value, TypeDomain expectedDomain, Class<?> expectedType, Object expectedValue) {
		Assert.assertEquals(expectedValue, value.value);
		Assert.assertEquals(expectedType, value.type);
		Assert.assertEquals(expectedDomain, value.domain);
	}

	static {
		TypeVariableHolderHandler.initializeClass(TypeDomain.TypeVariableHolders.class);
	}

	@Test
	public void testConversion() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);
		domain.registerType(String.class);

		domain.registerConverter(new IConverter<Integer, String>() {
			@Override
			public String convert(Integer value) {
				return value.toString();
			}
		});

		final TypedValue intValue = domain.create(Integer.class, 123);
		final TypedValue stringValue = intValue.cast(String.class);
		assertValueEquals(stringValue, domain, String.class, "123");
	}

	@Test
	public void testSelfConversion() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);

		final TypedValue intValue = domain.create(Integer.class, 123);
		final TypedValue result = intValue.cast(Integer.class);
		Assert.assertEquals(intValue, result);
	}

	@Test
	public void testCast() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);
		domain.registerType(Number.class);

		domain.registerCast(Integer.class, Number.class);

		final TypedValue intValue = domain.create(Integer.class, 123);
		final TypedValue numberValue = intValue.cast(Number.class);
		assertValueEquals(numberValue, domain, Number.class, Integer.valueOf(123));
	}

	@Test
	public void testCoercion() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);
		domain.registerType(Float.class);

		domain.registerConverter(new IConverter<Integer, Float>() {
			@Override
			public Float convert(Integer value) {
				return value.floatValue();
			}
		});
		domain.registerSymmetricCoercionRule(Float.class, Integer.class, Coercion.TO_LEFT);

		Assert.assertEquals(Coercion.TO_LEFT, domain.getCoercionRule(Float.class, Integer.class));
		Assert.assertEquals(Coercion.TO_RIGHT, domain.getCoercionRule(Integer.class, Float.class));
	}

	@Test
	public void testSelfCoercion() {
		final TypeDomain domain = new TypeDomain();
		Assert.assertEquals(Coercion.TO_LEFT, domain.getCoercionRule(Float.class, Float.class));
	}

	@Test
	public void testTruthiness() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Boolean.class);
		domain.registerType(Double.class);
		domain.registerType(Integer.class);

		domain.registerTruthEvaluator(new ITruthEvaluator<Boolean>() {
			@Override
			public boolean isTruthy(Boolean value) {
				return value.booleanValue();
			}
		});

		domain.registerTruthEvaluator(new ITruthEvaluator<Integer>() {

			@Override
			public boolean isTruthy(Integer value) {
				return value != 0;
			}
		});

		Assert.assertEquals(Optional.of(Boolean.TRUE), domain.create(Integer.class, 1).isTruthy());
		Assert.assertEquals(Optional.of(Boolean.TRUE), domain.create(Boolean.class, Boolean.TRUE).isTruthy());

		Assert.assertEquals(Optional.of(Boolean.FALSE), domain.create(Integer.class, 0).isTruthy());
		Assert.assertEquals(Optional.of(Boolean.FALSE), domain.create(Boolean.class, Boolean.FALSE).isTruthy());

		Assert.assertEquals(Optional.absent(), domain.create(Double.class, 0.0).isTruthy());
	}

}
