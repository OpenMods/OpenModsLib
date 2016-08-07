package openmods.calc.types.multi;

import openmods.calc.IExecutable;
import openmods.calc.Value;
import openmods.calc.parsing.BracketPostfixCompilerStateBase;
import openmods.calc.parsing.IExecutableListBuilder;

public class CodePostfixCompilerState extends BracketPostfixCompilerStateBase<TypedValue> {

	private final TypeDomain domain;

	public CodePostfixCompilerState(TypeDomain domain, IExecutableListBuilder<TypedValue> builder, String openingBracket) {
		super(builder, openingBracket);
		this.domain = domain;
	}

	@Override
	protected IExecutable<TypedValue> processCompiledBracket(IExecutable<TypedValue> compiledExpr) {
		return Value.create(domain.create(Code.class, new Code(compiledExpr)));
	}

}
