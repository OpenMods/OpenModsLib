package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.Value;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.calc.parsing.SymbolCallNode;

public class CodeStateTransition extends SameStateSymbolTransition<TypedValue> {

	private TypeDomain domain;

	public CodeStateTransition(TypeDomain domain, ICompilerState<TypedValue> parentParserState) {
		super(parentParserState);
		this.domain = domain;
	}

	@Override
	public IExprNode<TypedValue> createRootNode(final List<IExprNode<TypedValue>> children) {
		class CodeSymbol extends SymbolCallNode<TypedValue> {

			public CodeSymbol() {
				super(TypedCalcConstants.SYMBOL_CODE, children);
			}

			@Override
			public void flatten(List<IExecutable<TypedValue>> output) {
				Preconditions.checkArgument(children.size() == 1, "'code' expects single argument");
				output.add(Value.create(Code.flattenAndWrap(domain, children.get(0))));
			}

		}

		return new CodeSymbol();
	}

}
