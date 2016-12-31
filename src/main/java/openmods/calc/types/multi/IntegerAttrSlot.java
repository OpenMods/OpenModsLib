package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.Map;
import openmods.calc.Frame;

public class IntegerAttrSlot implements MetaObject.SlotAttr {

	private interface IntAttr {
		public TypedValue get(TypeDomain domain, BigInteger value);
	}

	private final Map<String, IntAttr> attrs = Maps.newHashMap();

	{
		attrs.put("bitLength", new IntAttr() {
			@Override
			public TypedValue get(TypeDomain domain, BigInteger value) {
				return domain.create(BigInteger.class, BigInteger.valueOf(value.bitLength()));
			}
		});

		attrs.put("chr", new IntAttr() {
			@Override
			public TypedValue get(TypeDomain domain, BigInteger value) {
				final char[] chars = Character.toChars(value.intValue());
				return domain.create(String.class, new String(chars));
			}
		});
	}

	@Override
	public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
		final IntAttr attr = attrs.get(key);
		if (attr == null) return Optional.absent();

		final BigInteger value = self.as(BigInteger.class);
		return Optional.of(attr.get(self.domain, value));
	}
}
