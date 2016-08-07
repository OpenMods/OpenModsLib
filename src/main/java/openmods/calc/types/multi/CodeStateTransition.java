package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.parsing.ExprUtils;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.ICompilerState.ISymbolStateTransition;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ValueNode;

public class CodeStateTransition implements ISymbolStateTransition<TypedValue> {

	private TypeDomain domain;
	private ICompilerState<TypedValue> parentParserState;

	public CodeStateTransition(TypeDomain domain, ICompilerState<TypedValue> parentParserState) {
		this.domain = domain;
		this.parentParserState = parentParserState;
	}

	@Override
	public ICompilerState<TypedValue> getState() {
		return parentParserState;
	}

	@Override
	public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
		Preconditions.checkArgument(children.size() == 1, "'code' expects single argument");
		final IExecutable<TypedValue> expr = ExprUtils.flattenNode(children.get(0));
		return new ValueNode<TypedValue>(domain.create(Code.class, new Code(expr)));
	}

}
