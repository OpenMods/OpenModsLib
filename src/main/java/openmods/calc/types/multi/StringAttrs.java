package openmods.calc.types.multi;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import openmods.calc.Frame;
import openmods.calc.UnaryFunction;
import openmods.calc.types.multi.TypedFunction.DispatchArg;
import openmods.calc.types.multi.TypedFunction.RawReturn;
import openmods.calc.types.multi.TypedFunction.Variant;
import org.apache.commons.lang3.StringUtils;

public class StringAttrs {

	private interface StringAttr {
		public TypedValue get(TypeDomain domain, String value);
	}

	private final Map<String, StringAttr> attrs = Maps.newHashMap();

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	public StringAttrs(final TypedValue nullValue) {
		attrs.put("lower", new StringAttr() {
			@Override
			public TypedValue get(TypeDomain domain, String value) {
				return domain.create(String.class, value.toLowerCase(Locale.ROOT));
			}
		});

		attrs.put("upper", new StringAttr() {
			@Override
			public TypedValue get(TypeDomain domain, String value) {
				return domain.create(String.class, value.toUpperCase(Locale.ROOT));
			}
		});

		attrs.put("strip", new StringAttr() {
			@Override
			public TypedValue get(TypeDomain domain, String value) {
				return domain.create(String.class, StringUtils.strip(value));
			}
		});

		attrs.put("startsWith", new StringAttr() {
			@Override
			public TypedValue get(final TypeDomain domain, final String value) {
				return CallableValue.wrap(domain, new UnaryFunction.Direct<TypedValue>() {
					@Override
					protected TypedValue call(TypedValue prefix) {
						final boolean result = value.startsWith(prefix.as(String.class));
						return domain.create(Boolean.class, result);
					}
				});
			}
		});

		attrs.put("endsWith", new StringAttr() {
			@Override
			public TypedValue get(final TypeDomain domain, final String value) {
				return CallableValue.wrap(domain, new UnaryFunction.Direct<TypedValue>() {
					@Override
					protected TypedValue call(TypedValue prefix) {
						final boolean result = value.endsWith(prefix.as(String.class));
						return domain.create(Boolean.class, result);
					}
				});
			}
		});

		attrs.put("indexOf", new StringAttr() {
			@Override
			public TypedValue get(final TypeDomain domain, final String value) {
				return CallableValue.wrap(domain, new UnaryFunction.Direct<TypedValue>() {
					@Override
					protected TypedValue call(TypedValue prefix) {
						final int result = value.indexOf(prefix.as(String.class));
						return domain.create(BigInteger.class, BigInteger.valueOf(result));
					}
				});
			}
		});

		attrs.put("split", new StringAttr() {
			@Override
			public TypedValue get(final TypeDomain domain, final String value) {
				return CallableValue.wrap(domain, new SimpleTypedFunction(domain) {
					@Variant
					@RawReturn
					public TypedValue split() {
						final Iterable<String> result = Splitter.on(WHITESPACE).split(value);
						return toList(nullValue, result);
					}

					@Variant
					@RawReturn
					public TypedValue split(@DispatchArg String separator) {
						final Iterable<String> result = Splitter.on(separator).split(value);
						return toList(nullValue, result);
					}

					@Variant
					@RawReturn
					public TypedValue split(String separator, @DispatchArg BigInteger maxSplit) {
						final Iterable<String> result = Splitter.on(separator).limit(maxSplit.intValue()).split(value);
						return toList(nullValue, result);
					}
				});
			}
		});

		attrs.put("join", new StringAttr() {
			@Override
			public TypedValue get(final TypeDomain domain, String value) {
				final Joiner joiner = Joiner.on(value);
				return CallableValue.wrap(domain, new UnaryFunction.Direct<TypedValue>() {

					@Override
					protected TypedValue call(TypedValue list) {
						final String result = joiner.join(Iterables.transform(Cons.toIterable(list, nullValue), domain.createUnwrappingTransformer(String.class)));
						return domain.create(String.class, result);
					}
				});
			}
		});

		attrs.put("ord", new StringAttr() {
			@Override
			public TypedValue get(TypeDomain domain, String value) {
				Preconditions.checkArgument(value.codePointCount(0, value.length()) == 1, "Not a single character: '%s'", value);
				return domain.create(BigInteger.class, BigInteger.valueOf(value.codePointAt(0)));
			}
		});
	}

	private static TypedValue toList(TypedValue nullValue, Iterable<String> parts) {
		final TypeDomain domain = nullValue.domain;
		final List<TypedValue> result = Lists.newArrayList();
		for (String part : parts)
			result.add(domain.create(String.class, part));

		return Cons.createList(result, nullValue);
	}

	public MetaObject.SlotAttr createAttrSlot() {
		return new MetaObject.SlotAttr() {
			@Override
			public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
				final StringAttr attr = attrs.get(key);
				if (attr == null) return Optional.absent();

				final String value = self.as(String.class);
				return Optional.of(attr.get(self.domain, value));
			}
		};
	}

	public MetaObject.SlotDir createDirSlot() {
		return MetaObjectUtils.dirFromIterable(attrs.keySet());
	}
}
