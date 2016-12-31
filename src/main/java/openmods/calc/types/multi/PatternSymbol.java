package openmods.calc.types.multi;

import openmods.calc.Environment;
import openmods.calc.SymbolMap;
import openmods.calc.UnaryFunction;

public class PatternSymbol {

	public static void register(final SymbolMap<TypedValue> coreMap, Environment<TypedValue> env) {
		final TypeDomain domain = env.nullValue().domain;
		final BindPatternTranslator patternTranslator = new BindPatternTranslator();
		final BindPatternEvaluator patternEvaluator = new BindPatternEvaluator(domain);

		final TypedValue patternType = domain.create(TypeUserdata.class, new TypeUserdata("pattern", IBindPattern.class),
				TypeUserdata.defaultMetaObject(domain)
						.set(MetaObjectUtils.callableAdapter(new UnaryFunction.Direct<TypedValue>() {
							@Override
							protected TypedValue call(TypedValue value) {
								final Code pattern = value.as(Code.class, "variable pattern");
								final TypedValue compiledPattern = patternEvaluator.evaluate(coreMap, pattern);
								final IBindPattern translatedPattern = patternTranslator.translatePattern(compiledPattern);
								return domain.create(IBindPattern.class, translatedPattern);
							}
						}))
						.build());

		env.setGlobalSymbol("pattern", patternType);
		domain.registerType(IBindPattern.class, "pattern",
				MetaObject.builder()
						.set(MetaObjectUtils.typeConst(patternType))
						.build());

		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_PATTERN, patternType);
	}

}
