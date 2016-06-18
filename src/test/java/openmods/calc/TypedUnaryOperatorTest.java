package openmods.calc;

import com.google.common.base.Optional;
import openmods.calc.types.multi.TypeDomain;
import openmods.calc.types.multi.TypedUnaryOperator;
import openmods.calc.types.multi.TypedUnaryOperator.IDefaultOperation;
import openmods.calc.types.multi.TypedUnaryOperator.IOperation;
import openmods.calc.types.multi.TypedUnaryOperator.ISimpleOperation;
import openmods.calc.types.multi.TypedValue;
import openmods.reflection.TypeVariableHolderHandler;
import org.junit.Assert;
import org.junit.Test;

public class TypedUnaryOperatorTest {

	private static void assertValueEquals(TypedValue value, TypeDomain expectedDomain, Class<?> expectedType, Object expectedValue) {
		Assert.assertEquals(expectedValue, value.value);
		Assert.assertEquals(expectedType, value.type);
		Assert.assertEquals(expectedDomain, value.domain);
	}

	private static TypedValue execute(UnaryOperator<TypedValue> op, TypedValue value) {
		final TopFrame<TypedValue> frame = new TopFrame<TypedValue>();
		frame.stack().push(value);
		op.execute(frame);
		return frame.stack().pop();
	}

	static {
		TypeVariableHolderHandler.initializeClass(TypedUnaryOperator.TypeVariableHolders.class);
	}

	@Test
	public void testTypedOperation() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);
		domain.registerType(Number.class);
		domain.registerType(Boolean.class);

		final TypedUnaryOperator op = new TypedUnaryOperator.Builder("*")
				.registerOperation(new IOperation<Integer>() {
					@Override
					public TypedValue apply(TypeDomain domain, Integer value) {
						return domain.create(Boolean.class, Boolean.TRUE);
					}
				})
				.registerOperation(new IOperation<Number>() {
					@Override
					public TypedValue apply(TypeDomain domain, Number value) {
						return domain.create(Boolean.class, Boolean.FALSE);
					}
				})
				.build(domain);

		{
			final TypedValue result = execute(op, domain.create(Integer.class, 123));
			assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
		}

		{
			final TypedValue result = execute(op, domain.create(Number.class, 123));
			assertValueEquals(result, domain, Boolean.class, Boolean.FALSE);
		}
	}

	@Test
	public void testSimpleTypedOperation() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);
		domain.registerType(Boolean.class);

		final TypedUnaryOperator op = new TypedUnaryOperator.Builder("*")
				.registerOperation(new ISimpleOperation<Integer, Boolean>() {
					@Override
					public Boolean apply(Integer value) {
						return Boolean.TRUE;
					}
				})
				.build(domain);

		final TypedValue result = execute(op, domain.create(Integer.class, 123));
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}

	@Test
	public void testDefaultOperation() {
		final TypeDomain domain = new TypeDomain();
		domain.registerType(Integer.class);
		domain.registerType(Boolean.class);

		final TypedValue value = domain.create(Integer.class, 2);

		final TypedUnaryOperator op = new TypedUnaryOperator.Builder("*")
				.setDefaultOperation(new IDefaultOperation() {
					@Override
					public Optional<TypedValue> apply(TypeDomain domain, TypedValue v) {
						Assert.assertEquals(v, value);
						return Optional.of(domain.create(Boolean.class, Boolean.TRUE));
					}
				})
				.build(domain);

		final TypedValue result = execute(op, value);
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}
}
