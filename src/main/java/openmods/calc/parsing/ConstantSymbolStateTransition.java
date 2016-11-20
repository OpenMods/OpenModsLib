package openmods.calc.parsing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.Environment;
import openmods.calc.ExecutableList;
import openmods.calc.Frame;
import openmods.calc.IExecutable;
import openmods.calc.Value;

public class ConstantSymbolStateTransition<E> extends SameStateSymbolTransition<E> {

	private final Environment<E> env;

	public ConstantSymbolStateTransition(ICompilerState<E> parentState, Environment<E> env) {
		super(parentState);
		this.env = env;
	}

	private class ConstantsNode implements IExprNode<E> {

		private final List<E> constants;

		public ConstantsNode(Iterable<E> constants) {
			this.constants = ImmutableList.copyOf(constants);
		}

		@Override
		public void flatten(List<IExecutable<E>> output) {
			for (E constant : constants)
				output.add(Value.create(constant));
		}

		@Override
		public Iterable<IExprNode<E>> getChildren() {
			return ImmutableList.of();
		}

	}

	@Override
	public IExprNode<E> createRootNode(List<IExprNode<E>> children) {
		final List<IExecutable<E>> ops = Lists.newArrayList();
		for (IExprNode<E> child : children)
			child.flatten(ops);

		final Frame<E> resultFrame = env.executeIsolated(ExecutableList.wrap(ops));
		return new ConstantsNode(resultFrame.stack());
	}

}
