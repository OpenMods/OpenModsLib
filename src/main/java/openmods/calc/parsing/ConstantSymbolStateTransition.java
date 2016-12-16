package openmods.calc.parsing;

import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.Environment;
import openmods.calc.ExecutableList;
import openmods.calc.Frame;
import openmods.calc.IExecutable;
import openmods.calc.Value;

public class ConstantSymbolStateTransition<E> extends SameStateSymbolTransition<E> {

	private final String selfSymbol;
	private final Environment<E> env;

	public ConstantSymbolStateTransition(ICompilerState<E> parentState, Environment<E> env, String selfSymbol) {
		super(parentState);
		this.env = env;
		this.selfSymbol = selfSymbol;
	}

	private class ConstantsNode extends SymbolCallNode<E> {
		public ConstantsNode(List<IExprNode<E>> constants) {
			super(selfSymbol, constants);
		}

		@Override
		public void flatten(List<IExecutable<E>> output) {
			final List<IExecutable<E>> ops = Lists.newArrayList();
			for (IExprNode<E> child : getChildren())
				child.flatten(ops);

			final Frame<E> resultFrame = env.executeIsolated(ExecutableList.wrap(ops));

			for (E constant : resultFrame.stack())
				output.add(Value.create(constant));
		}

	}

	@Override
	public IExprNode<E> createRootNode(List<IExprNode<E>> children) {
		return new ConstantsNode(children);
	}

}
