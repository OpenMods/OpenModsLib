package openmods.calc.types.multi;

import java.util.Collection;
import openmods.calc.Frame;
import openmods.calc.SymbolMap;

public class BindPatternTranslator {

	public interface IBindPatternProvider {
		public IBindPattern getPattern(BindPatternTranslator translator);
	}

	private static class PatternAny implements IBindPattern {
		public static final PatternAny INSTANCE = new PatternAny();

		@Override
		public boolean match(Frame<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value) {
			return true;
		}

		@Override
		public void listBoundVars(Collection<String> output) {}
	}

	private static class PatternBindName implements IBindPattern {
		private final String name;

		public PatternBindName(String name) {
			this.name = name;
		}

		@Override
		public boolean match(Frame<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value) {
			output.put(name, value);
			return true;
		}

		@Override
		public void listBoundVars(Collection<String> output) {
			output.add(name);
		}
	}

	public static IBindPattern createPatternForVarName(String var) {
		return var.equals(TypedCalcConstants.MATCH_ANY)
				? PatternAny.INSTANCE
				: new PatternBindName(var);
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

		@Override
		public void listBoundVars(Collection<String> output) {}
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
			return carMatcher.match(env, output, pair.car) && cdrMatcher.match(env, output, pair.cdr);
		}

		@Override
		public void listBoundVars(Collection<String> output) {
			carMatcher.listBoundVars(output);
			cdrMatcher.listBoundVars(output);
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
