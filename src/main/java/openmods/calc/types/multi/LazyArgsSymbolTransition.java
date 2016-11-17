package openmods.calc.types.multi;

import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.Value;
import openmods.calc.parsing.ContainerNode;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SameStateSymbolTransition;

public class LazyArgsSymbolTransition extends SameStateSymbolTransition<TypedValue> {

	private final TypeDomain domain;
	private final String symbolName;

	public LazyArgsSymbolTransition(ICompilerState<TypedValue> parentState, TypeDomain domain, String symbolName) {
		super(parentState);
		this.domain = domain;
		this.symbolName = symbolName;
	}

	private class LazyArgsSymbolNode extends ContainerNode<TypedValue> {

		public LazyArgsSymbolNode(List<IExprNode<TypedValue>> args) {
			super(args);
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			int count = 0;
			for (IExprNode<TypedValue> arg : getChildren()) {
				output.add(Value.create(Code.flattenAndWrap(domain, arg)));
				count++;
			}

			output.add(new SymbolCall<TypedValue>(symbolName, count, 1));
		}

	}

	@Override
	public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
		return new LazyArgsSymbolNode(children);
	}
}
