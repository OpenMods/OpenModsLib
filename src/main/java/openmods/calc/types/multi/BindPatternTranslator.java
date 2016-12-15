package openmods.calc.types.multi;

import openmods.calc.Frame;
import openmods.calc.SymbolMap;

public class BindPatternTranslator {

	public static interface PatternPart {
		public boolean match(Frame<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value);
	}

	public interface IPatternProvider {
		public PatternPart getPattern(BindPatternTranslator translator);
	}

	private static class PatternMatchConst implements PatternPart {
		private final TypedValue expected;

		public PatternMatchConst(TypedValue expected) {
			this.expected = expected;
		}

		@Override
		public boolean match(Frame<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value) {
			return value.equals(expected);
		}
	}

	private static class PatternMatchCons implements PatternPart {
		private final PatternPart carMatcher;
		private final PatternPart cdrMatcher;

		public PatternMatchCons(PatternPart carMatcher, PatternPart cdrMatcher) {
			this.carMatcher = carMatcher;
			this.cdrMatcher = cdrMatcher;
		}

		@Override
		public boolean match(Frame<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value) {
			if (!value.is(Cons.class)) return false;
			final Cons pair = value.as(Cons.class);
			return carMatcher.match(env, output, pair.car) && cdrMatcher.match(null, output, pair.cdr);
		}
	}

	public PatternPart translatePattern(TypedValue value) {
		if (value.is(IPatternProvider.class)) {
			final IPatternProvider p = value.as(IPatternProvider.class);
			return p.getPattern(this);
		}

		if (value.is(Cons.class)) {
			final Cons pair = value.as(Cons.class);
			final PatternPart carPattern = translatePattern(pair.car);
			final PatternPart cdrPattern = translatePattern(pair.cdr);
			return new PatternMatchCons(carPattern, cdrPattern);
		}

		return new PatternMatchConst(value);
	}

	public static void registerType(TypeDomain domain) {
		domain.registerType(IPatternProvider.class, "patternPlaceholder");
	}
}
