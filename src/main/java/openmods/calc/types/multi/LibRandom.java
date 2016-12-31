package openmods.calc.types.multi;

import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.Map;
import java.util.Random;
import openmods.calc.NullaryFunction;
import openmods.calc.types.multi.TypedFunction.DispatchArg;
import openmods.calc.types.multi.TypedFunction.RawReturn;
import openmods.calc.types.multi.TypedFunction.Variant;

public class LibRandom {

	private final TypedValue typeValue;

	private final TypeDomain domain;

	private static class RandomValue {
		private static final RandomValue INSTANCE = new RandomValue();
	}

	public LibRandom(TypeDomain domain) {
		this.domain = domain;

		final TypeUserdata type = new TypeUserdata("random", RandomValue.class);

		domain.registerType(RandomValue.class, "random");

		this.typeValue = domain.create(TypeUserdata.class, type,
				TypeUserdata.defaultMetaObject(domain)
						.set(MetaObjectUtils.callableAdapter(new SimpleTypedFunction(domain) {
							@Variant
							@RawReturn
							public TypedValue create() {
								return LibRandom.this.domain.create(RandomValue.class, RandomValue.INSTANCE,
										createRandomValueMetaobject(new Random()));
							}

							@Variant
							@RawReturn
							public TypedValue create(@DispatchArg BigInteger seed) {
								return LibRandom.this.domain.create(RandomValue.class, RandomValue.INSTANCE,
										createRandomValueMetaobject(new Random(seed.longValue())));
							}

						})).build());
	}

	private Map<String, TypedValue> createMembers(final Random random) {
		final Map<String, TypedValue> result = Maps.newHashMap();
		result.put("nextInt", CallableValue.wrap(domain, new SimpleTypedFunction(domain) {

			@Variant
			public BigInteger next(@DispatchArg BigInteger range) {
				return BigInteger.valueOf(random.nextInt(range.intValue()));
			}

			@Variant
			public BigInteger next() {
				return BigInteger.valueOf(random.nextInt());
			}
		}));

		result.put("nextFloat", CallableValue.wrap(domain, new NullaryFunction.Direct<TypedValue>() {
			@Override
			protected TypedValue call() {
				return domain.create(Double.class, random.nextDouble());
			}
		}));

		result.put("nextBoolean", CallableValue.wrap(domain, new NullaryFunction.Direct<TypedValue>() {
			@Override
			protected TypedValue call() {
				return domain.create(Boolean.class, random.nextBoolean());
			}
		}));

		result.put("nextGaussian", CallableValue.wrap(domain, new NullaryFunction.Direct<TypedValue>() {
			@Override
			protected TypedValue call() {
				return domain.create(Double.class, random.nextGaussian());
			}
		}));

		return result;
	}

	private MetaObject createRandomValueMetaobject(Random random) {
		return MetaObject.builder()
				.set(MetaObjectUtils.typeConst(typeValue))
				.set(MetaObjectUtils.attrFromMap(createMembers(random)))
				.build();
	}

	public TypedValue type() {
		return typeValue;
	}
}
