package openmods.calc.types.multi;

import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SquareBracketContainerNode;

public class ListBracketNode extends SquareBracketContainerNode<TypedValue> {

	public ListBracketNode(List<IExprNode<TypedValue>> args) {
		super(args);
	}

	@Override
	public void flatten(List<IExecutable<TypedValue>> output) {
		for (IExprNode<TypedValue> node : args)
			node.flatten(output);

		output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, args.size(), 1));
	}
}
