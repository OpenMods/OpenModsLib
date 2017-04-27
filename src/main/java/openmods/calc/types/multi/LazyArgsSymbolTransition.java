package openmods.calc.types.multi;

import java.util.List;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.SymbolCall;
import openmods.calc.executable.Value;
import openmods.calc.parsing.ast.IParserState;
import openmods.calc.parsing.ast.SameStateSymbolTransition;
import openmods.calc.parsing.node.IExprNode;
import openmods.calc.parsing.node.SymbolCallNode;

public class LazyArgsSymbolTransition extends SameStateSymbolTransition<IExprNode<TypedValue>> {

	private final TypeDomain domain;
	private final String symbolName;

	public LazyArgsSymbolTransition(IParserState<IExprNode<TypedValue>> parentState, TypeDomain domain, String symbolName) {
		super(parentState);
		this.domain = domain;
		this.symbolName = symbolName;
	}

	private class LazyArgsSymbolNode extends SymbolCallNode<TypedValue> {

		public LazyArgsSymbolNode(List<IExprNode<TypedValue>> args) {
			super(symbolName, args);
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
