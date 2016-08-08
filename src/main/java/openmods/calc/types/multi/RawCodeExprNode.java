package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.Value;
import openmods.calc.parsing.ExprUtils;
import openmods.calc.parsing.IExprNode;

public class RawCodeExprNode implements IExprNode<TypedValue> {
	private final TypeDomain domain;
	private final IExprNode<TypedValue> arg;

	RawCodeExprNode(TypeDomain domain, List<IExprNode<TypedValue>> children) {
		Preconditions.checkState(children.size() == 1, "Expected only one expression in curly brackets");
		this.arg = children.get(0);

		this.domain = domain;
	}

	@Override
	public void flatten(List<IExecutable<TypedValue>> output) {
		final IExecutable<TypedValue> flattenChild = ExprUtils.flattenNode(arg);
		output.add(Value.create(domain.create(Code.class, new Code(flattenChild))));
	}

	@Override
	public Iterable<IExprNode<TypedValue>> getChildren() {
		return ImmutableList.of(arg);
	}
}