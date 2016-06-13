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
import org.junit.Assert;
import org.junit.Test;

public class TypedBinaryOperatorTest {

	private static void assertValueEquals(TypedValue value, TypeDomain expectedDomain, Class<?> expectedType, Object expectedValue) {
		Assert.assertEquals(expectedValue, value.value);
		Assert.assertEquals(expectedType, value.type);
		Assert.assertEquals(expectedDomain, value.domain);
	}

	private static TypedValue execute(BinaryOperator<TypedValue> op, TypedValue left, TypedValue right) {
		final TopFrame<TypedValue> frame = new TopFrame<TypedValue>();
		frame.stack().push(left);
		frame.stack().push(right);
		op.execute(frame);
		return frame.stack().pop();
	}

	@Test
	public void testVariantOperation() {
		final TypeDomain domain = new TypeDomain();

		final TypedBinaryOperator op = new TypedBinaryOperator("+", 0);
		op.registerOperation(new IVariantOperation<String, Integer>() {
			@Override
			public TypedValue apply(TypeDomain domain, String left, Integer right) {
				Assert.assertEquals("abc", left);
				Assert.assertEquals(Integer.valueOf(123), right);
				return domain.create(Boolean.class, Boolean.TRUE);
			}
		});

		final TypedValue result = execute(op, domain.create(String.class, "abc"), domain.create(Integer.class, 123));
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}

	@Test
	public void testSimpleVariantOperation() {
		final TypeDomain domain = new TypeDomain();

		final TypedBinaryOperator op = new TypedBinaryOperator("+", 0);
		op.registerOperation(new ISimpleVariantOperation<String, Integer, Boolean>() {
			@Override
			public Boolean apply(String left, Integer right) {
				Assert.assertEquals("abc", left);
				Assert.assertEquals(Integer.valueOf(123), right);
				return Boolean.TRUE;
			}
		});

		final TypedValue result = execute(op, domain.create(String.class, "abc"), domain.create(Integer.class, 123));
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}

	@Test
	public void testCoercedOperator() {
		final TypeDomain domain = new TypeDomain();
		domain.registerCast(Integer.class, Number.class);
		domain.registerCoercionRule(Integer.class, Number.class, Coercion.TO_RIGHT);

		final TypedBinaryOperator op = new TypedBinaryOperator("+", 0);
		op.registerOperation(new ICoercedOperation<Number>() {
			@Override
			public TypedValue apply(TypeDomain domain, Number left, Number right) {
				return domain.create(Boolean.class, Boolean.TRUE);
			}
		});

		final TypedValue result = execute(op, domain.create(Integer.class, 123), domain.create(Number.class, 567));
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}

	@Test
	public void testSimpleCoercedOperator() {
		final TypeDomain domain = new TypeDomain();
		domain.registerCast(Integer.class, Number.class);
		domain.registerCoercionRule(Integer.class, Number.class, Coercion.TO_RIGHT);

		final TypedBinaryOperator op = new TypedBinaryOperator("+", 0);
		op.registerOperation(new ISimpleCoercedOperation<Number>() {
			@Override
			public Number apply(Number left, Number right) {
				return left.intValue() + right.intValue();
			}
		});

		final TypedValue result = execute(op, domain.create(Integer.class, 123), domain.create(Number.class, 567));
		assertValueEquals(result, domain, Number.class, 690);
	}

	@Test
	public void testSelfCoercedOperator() {
		final TypeDomain domain = new TypeDomain();
		final TypedBinaryOperator op = new TypedBinaryOperator("+", 0);
		op.registerOperation(new ISimpleCoercedOperation<Integer>() {
			@Override
			public Integer apply(Integer left, Integer right) {
				return left + right;
			}
		});

		final TypedValue result = execute(op, domain.create(Integer.class, 2), domain.create(Integer.class, 3));
		assertValueEquals(result, domain, Integer.class, 5);
	}

	@Test
	public void testDefaultOperation() {
		final TypeDomain domain = new TypeDomain();
		final TypedBinaryOperator op = new TypedBinaryOperator("+", 0);

		final TypedValue l = domain.create(Integer.class, 2);
		final TypedValue r = domain.create(String.class, "a");
		op.setDefaultOperation(new IDefaultOperation() {
			@Override
			public Optional<TypedValue> apply(TypeDomain domain, TypedValue left, TypedValue right) {
				Assert.assertEquals(l, left);
				Assert.assertEquals(r, right);
				return Optional.of(domain.create(Boolean.class, Boolean.TRUE));
			}
		});

		final TypedValue result = execute(op, l, r);
		assertValueEquals(result, domain, Boolean.class, Boolean.TRUE);
	}
}
