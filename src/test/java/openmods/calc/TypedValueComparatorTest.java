package openmods.calc;

import openmods.calc.types.multi.IConverter;
import openmods.calc.types.multi.TypeDomain;
import openmods.calc.types.multi.TypeDomain.Coercion;
import openmods.calc.types.multi.TypedValue;
import openmods.calc.types.multi.TypedValueComparator;
import openmods.reflection.TypeVariableHolderHandler;
import org.junit.Assert;
import org.junit.Test;

public class TypedValueComparatorTest {

	static {
		TypeVariableHolderHandler.initializeClass(TypeDomain.TypeVariableHolders.class);
	}

	private static class NotComparable {}

	private final TypeDomain domain = new TypeDomain();
	{
		domain.registerType(Integer.class, "int");
		domain.registerType(String.class, "str");
		domain.registerType(NotComparable.class, "not_comparable");

		domain.registerConverter(new IConverter<String, Integer>() {
			@Override
			public Integer convert(String value) {
				return Integer.valueOf(value);
			}
		});
	}

	private TypedValue i(int value) {
		return domain.create(Integer.class, value);
	}

	private TypedValue s(String value) {
		return domain.create(String.class, value);
	}

	private TypedValue nc() {
		return domain.create(NotComparable.class, new NotComparable());
	}

	private final TypedValueComparator compator = new TypedValueComparator();

	@Test
	public void compareSameType() {
		Assert.assertEquals(1, compator.compare(i(2), i(1)));
		Assert.assertEquals(0, compator.compare(i(1), i(1)));
		Assert.assertEquals(-1, compator.compare(i(0), i(1)));

		Assert.assertEquals(1, compator.compare(s("c"), s("b")));
		Assert.assertEquals(0, compator.compare(s("b"), s("b")));
		Assert.assertEquals(-1, compator.compare(s("a"), s("b")));
	}

	@Test
	public void testCoercedTypes() {
		domain.registerSymmetricCoercionRule(Integer.class, String.class, Coercion.TO_LEFT);

		Assert.assertEquals(-1, compator.compare(s("00"), i(1)));
		Assert.assertEquals(-1, compator.compare(i(0), s("01")));

		Assert.assertEquals(0, compator.compare(s("00"), i(0)));
		Assert.assertEquals(0, compator.compare(i(0), s("00")));

		Assert.assertEquals(1, compator.compare(s("01"), i(0)));
		Assert.assertEquals(1, compator.compare(i(1), s("00")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotCoercedTypes() {
		Assert.assertEquals(0, compator.compare(s("0"), i(0)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotComparableType() {
		Assert.assertEquals(0, compator.compare(nc(), nc()));
	}
}
