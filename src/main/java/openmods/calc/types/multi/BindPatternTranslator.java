package openmods.calc.types.multi;

import openmods.calc.Frame;
import openmods.calc.SymbolMap;

public class BindPatternTranslator {

	public interface IBindPatternProvider {
		public IBindPattern getPattern(BindPatternTranslator translator);
	}

	private static class PatternMatchConst implements IBindPattern {
		private final TypedValue expected;

		public PatternMatchConst(TypedValue expected) {
			this.expected = expected;
		}

		@Override
		public boolean match(Frame<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value) {
			return value.equals(expected);
		}
	}

	private static class PatternMatchCons implements IBindPattern {
		private final IBindPattern carMatcher;
		private final IBindPattern cdrMatcher;

		public PatternMatchCons(IBindPattern carMatcher, IBindPattern cdrMatcher) {
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

	public IBindPattern translatePattern(TypedValue value) {
		if (value.is(IBindPatternProvider.class)) {
			final IBindPatternProvider p = value.as(IBindPatternProvider.class);
			return p.getPattern(this);
		}

		if (value.is(Cons.class)) {
			final Cons pair = value.as(Cons.class);
			final IBindPattern carPattern = translatePattern(pair.car);
			final IBindPattern cdrPattern = translatePattern(pair.cdr);
			return new PatternMatchCons(carPattern, cdrPattern);
		}

		return new PatternMatchConst(value);
	}

	public static void registerType(TypeDomain domain) {
		domain.registerType(IBindPatternProvider.class, "patternPlaceholder");
	}
}
