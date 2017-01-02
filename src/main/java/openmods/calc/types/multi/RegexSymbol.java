package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.types.multi.TypedFunction.DispatchArg;
import openmods.calc.types.multi.TypedFunction.Variant;
import openmods.utils.OptionalInt;

public class RegexSymbol {

	private static final String ATTR_MATCHER = "matcher";
	private static final String ATTR_MATCH = "match";
	private static final String ATTR_SEARCH = "search";
	private static final String ATTR_FLAGS = "flags";
	private static final String ATTR_PATTERN = "pattern";
	private static final String ATTR_MATCHED = "matched";
	private static final String ATTR_END = "end";
	private static final String ATTR_START = "start";

	private static TypedValue wrap(TypeDomain domain, int value) {
		return domain.create(BigInteger.class, BigInteger.valueOf(value));
	}

	private static TypedValue wrap(TypeDomain domain, String value) {
		return domain.create(String.class, value);
	}

	private static TypedValue wrap(TypeDomain domain, TypeUserdata value) {
		return domain.create(TypeUserdata.class, value);
	}

	private static void addFlag(Map<String, TypedValue> output, TypeDomain domain, int value, String... names) {
		for (String name : names)
			output.put(name, wrap(domain, value));
	}

	private static class PatternWrapper {
		private final Pattern pattern;

		public PatternWrapper(Pattern pattern) {
			this.pattern = pattern;
		}

		public MatcherWrapper search() {
			return new PartialMatcherWrapper(pattern);
		}

		public MatcherWrapper match() {
			return new FullMatcherWrapper(pattern);
		}
	}

	private static MetaObject createPatternWrapperMetaObject(final TypeDomain domain, TypedValue patternType) {
		return MetaObject.builder()
				.set(new MetaObject.SlotAttr() {
					@Override
					public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
						final PatternWrapper patternWrapper = self.as(PatternWrapper.class);
						if (key.equals(ATTR_PATTERN)) return Optional.of(domain.create(String.class, patternWrapper.pattern.pattern()));
						if (key.equals(ATTR_FLAGS)) return Optional.of(wrap(domain, patternWrapper.pattern.flags()));
						if (key.equals(ATTR_SEARCH)) return Optional.of(domain.create(MatcherWrapper.class, patternWrapper.search()));
						if (key.equals(ATTR_MATCH)) return Optional.of(domain.create(MatcherWrapper.class, patternWrapper.match()));

						return Optional.absent();
					}
				})
				.set(MetaObjectUtils.dirFromArray(ATTR_PATTERN, ATTR_FLAGS, ATTR_SEARCH, ATTR_MATCH))
				.set(MetaObjectUtils.typeConst(patternType))
				.set(new MetaObject.SlotStr() {
					@Override
					public String str(TypedValue self, Frame<TypedValue> frame) {
						return "compiled pattern: " + self.as(PatternWrapper.class).pattern.toString();
					}
				})
				.set(new MetaObject.SlotRepr() {
					@Override
					public String repr(TypedValue self, Frame<TypedValue> frame) {
						return "regex(" + self.as(PatternWrapper.class).pattern.toString() + ")";
					}
				})
				.build();
	}

	private abstract static class MatcherWrapper {
		public final Pattern pattern;

		public MatcherWrapper(Pattern pattern) {
			this.pattern = pattern;
		}

		public abstract String type();

		protected abstract boolean check(Matcher matcher);

		public Optional<MatchWrapper> match(String value) {
			final Matcher matcher = pattern.matcher(value);
			if (check(matcher)) return Optional.of(new MatchWrapper(matcher.toMatchResult()));
			return Optional.absent();
		}
	}

	private static class FullMatcherWrapper extends MatcherWrapper {
		public FullMatcherWrapper(Pattern pattern) {
			super(pattern);
		}

		@Override
		protected boolean check(Matcher matcher) {
			return matcher.matches();
		}

		@Override
		public String type() {
			return ATTR_MATCH;
		}
	}

	private static class PartialMatcherWrapper extends MatcherWrapper {
		public PartialMatcherWrapper(Pattern pattern) {
			super(pattern);
		}

		@Override
		protected boolean check(Matcher matcher) {
			return matcher.find();
		}

		@Override
		public String type() {
			return ATTR_SEARCH;
		}
	}

	private static MetaObject createMatcherWrapperMetaObject(final TypeDomain domain, TypedValue matcherType) {
		return MetaObject.builder()
				.set(new MetaObject.SlotCall() {
					@Override
					public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
						argumentsCount.compareIfPresent(2);
						returnsCount.compareIfPresent(1);

						final MatcherWrapper matcher = self.as(MatcherWrapper.class);
						final String value = frame.stack().pop().as(String.class);

						final TypedValue result;
						final Optional<MatchWrapper> match = matcher.match(value);

						if (match.isPresent()) {
							result = OptionalType.present(domain, domain.create(MatchWrapper.class, match.get()));
						} else {
							result = OptionalType.absent(domain);
						}

						frame.stack().push(result);
					}
				})
				.set(new MetaObject.SlotDecompose() {
					@Override
					public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
						Preconditions.checkArgument(variableCount == 1, "Invalid number of variables, expected one, got %s", variableCount);
						final MatcherWrapper matcher = self.as(MatcherWrapper.class);
						final Optional<MatchWrapper> result = matcher.match(input.as(String.class));
						if (result.isPresent()) {
							final TypedValue wrappedResult = domain.create(MatchWrapper.class, result.get());
							final List<TypedValue> resultValues = ImmutableList.of(wrappedResult);
							return Optional.of(resultValues);
						}

						return Optional.absent();
					}
				})
				.set(MetaObjectUtils.typeConst(matcherType))
				.set(new MetaObject.SlotStr() {
					@Override
					public String str(TypedValue self, Frame<TypedValue> frame) {
						final MatcherWrapper s = self.as(MatcherWrapper.class);
						return s.type() + " matcher for pattern " + s.pattern.pattern();
					}
				})
				.set(new MetaObject.SlotRepr() {
					@Override
					public String repr(TypedValue self, Frame<TypedValue> frame) {
						final MatcherWrapper s = self.as(MatcherWrapper.class);
						return "regex(" + s.pattern.toString() + ")." + s.type();
					}
				})
				.build();
	}

	private static class MatchWrapper {
		public final MatchResult matcher;

		public MatchWrapper(MatchResult matcher) {
			this.matcher = matcher;
		}
	}

	private static MetaObject createMatchWrapperMetaObject(final TypeDomain domain, final TypedValue nullValue, TypedValue matchType) {
		return MetaObject.builder()
				.set(new MetaObject.SlotAttr() {
					@Override
					public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
						final MatchWrapper s = self.as(MatchWrapper.class);
						if (key.equals(ATTR_START)) return Optional.of(wrap(domain, s.matcher.start()));
						if (key.equals(ATTR_END)) return Optional.of(wrap(domain, s.matcher.end()));
						if (key.equals(ATTR_MATCHED)) return Optional.of(wrap(domain, s.matcher.group()));

						return Optional.absent();
					}
				})
				.set(MetaObjectUtils.dirFromArray(ATTR_START, ATTR_END, ATTR_MATCHED))
				.set(new MetaObject.SlotSlice() {
					@Override
					public TypedValue slice(TypedValue self, TypedValue range, Frame<TypedValue> frame) {
						// TODO: group start,end? Maybe via string MO manipulation
						final int group = range.as(BigInteger.class).intValue();
						final String groupContents = self.as(MatchWrapper.class).matcher.group(group);
						return groupContents != null? domain.create(String.class, groupContents) : nullValue;
					}
				})
				.set(new MetaObject.SlotLength() {
					@Override
					public int length(TypedValue self, Frame<TypedValue> frame) {
						return self.as(MatchWrapper.class).matcher.groupCount();
					}
				})
				.set(MetaObjectUtils.typeConst(matchType))
				.build();
	}

	public static void register(Environment<TypedValue> env) {
		final TypedValue nullValue = env.nullValue();
		final TypeDomain domain = nullValue.domain;

		final Map<String, TypedValue> values = Maps.newHashMap();

		addFlag(values, domain, Pattern.CASE_INSENSITIVE, "i", "case_insensitive");
		addFlag(values, domain, Pattern.MULTILINE, "m", "multiline");
		addFlag(values, domain, Pattern.DOTALL, "s", "dotall");
		addFlag(values, domain, Pattern.COMMENTS, "x", "comments");
		addFlag(values, domain, Pattern.UNIX_LINES, "n", "unix_lines");

		{
			final TypedValue patternType = wrap(domain, new TypeUserdata("regex.pattern", PatternWrapper.class));
			values.put(ATTR_PATTERN, patternType);
			domain.registerType(PatternWrapper.class, "regex.pattern", createPatternWrapperMetaObject(domain, patternType));
		}

		{
			final TypedValue matcherType = wrap(domain, new TypeUserdata("regex.matcher", MatcherWrapper.class));
			values.put(ATTR_MATCHER, matcherType);
			domain.registerType(MatcherWrapper.class, "regex.matcher", createMatcherWrapperMetaObject(domain, matcherType));
		}

		{
			final TypedValue matchType = wrap(domain, new TypeUserdata("regex.match", MatchWrapper.class));
			values.put(ATTR_MATCH, matchType);
			domain.registerType(MatchWrapper.class, "regex.match", createMatchWrapperMetaObject(domain, nullValue, matchType));
		}

		final TypedValue regex = domain.create(SimpleNamespace.class, new SimpleNamespace(values),
				SimpleNamespace.defaultMetaObject()
						.set(MetaObjectUtils.callableAdapter(new SimpleTypedFunction(domain) {
							@Variant
							public PatternWrapper create(String value, @DispatchArg BigInteger flags) {
								final Pattern pattern = Pattern.compile(value, flags.intValue());
								return new PatternWrapper(pattern);
							}

							@Variant
							public PatternWrapper create(String value) {
								final Pattern pattern = Pattern.compile(value);
								return new PatternWrapper(pattern);
							}
						}))
						.build());

		env.setGlobalSymbol("regex", regex);
	}

}
