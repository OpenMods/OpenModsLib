package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import java.util.List;
import openmods.calc.executable.IExecutable;
import openmods.calc.parsing.node.BracketContainerNode;
import openmods.calc.parsing.node.IExprNode;

public class ArgBracketNode extends BracketContainerNode<TypedValue> {

	public ArgBracketNode(List<IExprNode<TypedValue>> args) {
		super(args, "(", ")");
	}

	@Override
	public void flatten(List<IExecutable<TypedValue>> output) {
		// Multivalued brackets should be handled by default operator
		Preconditions.checkState(args.size() == 1, "Invalid number of expressions in bracket: %s", args);
		args.get(0).flatten(output);
	}
}
