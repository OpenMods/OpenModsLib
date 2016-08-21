package openmods.calc;

import com.google.common.base.Optional;
import openmods.calc.types.multi.TypeDomain;
import openmods.calc.types.multi.TypeDomain.Coercion;
import openmods.calc.types.multi.TypedBinaryOperator;
import openmods.calc.types.multi.TypedBinaryOperator.ICoercedOperation;
import openmods.calc.types.multi.TypedBinaryOperator.IDefaultOperation;
import openmods.calc.types.multi.TypedBinaryOperator.ISimpleCoercedOperation;
import openmods.calc.types.multi.TypedBinaryOperator.ISimpleVariantOperation;
import openmods.calc.types.multi.TypedBinaryOperator.IVariantOperation;
import openmods.calc.types.multi.TypedValue;
import openmods.reflection.TypeVariableHolderHandler;
import org.junit.Assert;
import org.junit.Test;

public class TypedBinaryOperatorTest {

	private static void assertValueEquals(TypedValue value, TypeDomain expectedDomain, Class<?> expectedType, Object expectedValue) {
		Assert.assertEquals(expectedValue, value.value);
		Assert.assertEquals(expectedType, value.type);
		Assert.assertEquals(expectedDomain, value.domain);
	}

	private static TypedValue execute(BinaryOperator<TypedValue> op, TypedValue left, TypedValue right) {
		final Frame<TypedValue> frame = FrameFactory.createTopFrame();
		frame.stack().push(left);
		frame.stack().push(right);
		op.execute(frame);
		final TypedValue result = frame.stack().pop();
		Assert.assertTrue(frame.stack().isEmpty());
		return result;
	}

	static {
		TypeVariableHolderHandler.initializeClass(TypedBinaryOperator.TypeVariableHolders.class);
	}

	@Test
	public void testVariantOperation() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);
		domain.registerType(String.class);
		domain.registerType(Boolean.class);

		final TypedBinaryOperator op = new TypedBinaryOperator.Builder("+", 0)
				.registerOperation(new IVariantOperation<String, Integer>() {
					@Override
					public TypedValue apply(TypeDomain domain, String left, Integer right) {
						Assert.assertEquals("abc", left);
						Assert.assertEquals(Integer.valueOf(123), right);
						return domain.create(Boolean.class, Boolean.TRUE);
					}
				}).build(domain);

		final TypedValue result = execute(op, domain.create(String.class, "abc"), domain.create(Integer.class, 123));
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}

	@Test
	public void testSimpleVariantOperation() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);
		domain.registerType(String.class);
		domain.registerType(Boolean.class);

		final TypedBinaryOperator op = new TypedBinaryOperator.Builder("+", 0)
				.registerOperation(new ISimpleVariantOperation<String, Integer, Boolean>() {
					@Override
					public Boolean apply(String left, Integer right) {
						Assert.assertEquals("abc", left);
						Assert.assertEquals(Integer.valueOf(123), right);
						return Boolean.TRUE;
					}
				}).build(domain);

		final TypedValue result = execute(op, domain.create(String.class, "abc"), domain.create(Integer.class, 123));
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}

	@Test
	public void testCoercedOperator() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);
		domain.registerType(Number.class);
		domain.registerType(Boolean.class);

		domain.registerCast(Integer.class, Number.class);
		domain.registerCoercionRule(Integer.class, Number.class, Coercion.TO_RIGHT);

		final TypedBinaryOperator op = new TypedBinaryOperator.Builder("+", 0)
				.registerOperation(new ICoercedOperation<Number>() {
					@Override
					public TypedValue apply(TypeDomain domain, Number left, Number right) {
						return domain.create(Boolean.class, Boolean.TRUE);
					}
				}).build(domain);

		final TypedValue result = execute(op, domain.create(Integer.class, 123), domain.create(Number.class, 567));
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}

	@Test
	public void testSimpleCoercedOperator() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Number.class);
		domain.registerType(Integer.class);
		domain.registerType(Boolean.class);

		domain.registerCast(Integer.class, Number.class);
		domain.registerCoercionRule(Integer.class, Number.class, Coercion.TO_RIGHT);

		final TypedBinaryOperator op = new TypedBinaryOperator.Builder("+", 0)
				.registerOperation(new ISimpleCoercedOperation<Number, Boolean>() {
					@Override
					public Boolean apply(Number left, Number right) {
						return Boolean.TRUE;
					}
				}).build(domain);

		final TypedValue result = execute(op, domain.create(Integer.class, 123), domain.create(Number.class, 567));
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}

	@Test
	public void testSelfCoercedOperator() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);

		final TypedBinaryOperator op = new TypedBinaryOperator.Builder("+", 0)
				.registerOperation(new ISimpleCoercedOperation<Integer, Integer>() {
					@Override
					public Integer apply(Integer left, Integer right) {
						return left + right;
					}
				}).build(domain);

		final TypedValue result = execute(op, domain.create(Integer.class, 2), domain.create(Integer.class, 3));
		assertValueEquals(result, domain, Integer.class, 5);
	}

	@Test
	public void testDefaultOperation() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);
		domain.registerType(String.class);
		domain.registerType(Boolean.class);

		final TypedValue l = domain.create(Integer.class, 2);
		final TypedValue r = domain.create(String.class, "a");

		final TypedBinaryOperator op = new TypedBinaryOperator.Builder("+", 0)
				.setDefaultOperation(new IDefaultOperation() {
					@Override
					public Optional<TypedValue> apply(TypeDomain domain, TypedValue left, TypedValue right) {
						Assert.assertEquals(l, left);
						Assert.assertEquals(r, right);
						return Optional.of(domain.create(Boolean.class, Boolean.TRUE));
					}
				}).build(domain);

		final TypedValue result = execute(op, l, r);
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}
}
