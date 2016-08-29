package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import java.util.List;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.calc.parsing.ValueNode;

public class CodeStateTransition extends SameStateSymbolTransition<TypedValue> {

	private TypeDomain domain;

	public CodeStateTransition(TypeDomain domain, ICompilerState<TypedValue> parentParserState) {
		super(parentParserState);
		this.domain = domain;
	}

	@Override
	public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
		Preconditions.checkArgument(children.size() == 1, "'code' expects single argument");
		return new ValueNode<TypedValue>(Code.flattenAndWrap(domain, children.get(0)));
	}

}
