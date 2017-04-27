package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.SymbolCall;
import openmods.calc.executable.Value;
import openmods.calc.parsing.ast.IParserState;
import openmods.calc.parsing.ast.ISymbolCallStateTransition;
import openmods.calc.parsing.ast.SameStateSymbolTransition;
import openmods.calc.parsing.node.IExprNode;
import openmods.calc.parsing.node.SymbolCallNode;
import openmods.calc.symbol.ICallable;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class DoExpressionFactory {

	private final TypeDomain domain;

	public DoExpressionFactory(TypeDomain domain) {
		this.domain = domain;
	}

	private class DoNode extends SymbolCallNode<TypedValue> {

		public DoNode(List<IExprNode<TypedValue>> args) {
			super(TypedCalcConstants.SYMBOL_DO, args);
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			int argCount = 0;
			for (IExprNode<TypedValue> child : getChildren()) {
				output.add(Value.create(Code.flattenAndWrap(domain, child)));
				argCount++;
			}

			Preconditions.checkState(argCount > 1, "'do' expects at least one argument");
			output.add(new SymbolCall<TypedValue>(symbol, argCount, 1));
		}
	}

	private class DoExpr extends SameStateSymbolTransition<IExprNode<TypedValue>> {

		public DoExpr(IParserState<IExprNode<TypedValue>> parentState) {
			super(parentState);
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			return new DoNode(children);
		}
	}

	public ISymbolCallStateTransition<IExprNode<TypedValue>> createStateTransition(IParserState<IExprNode<TypedValue>> compilerState) {
		return new DoExpr(compilerState);
	}

	private class DoSymbol implements ICallable<TypedValue> {
		@Override
		public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			Preconditions.checkState(argumentsCount.isPresent(), "'do' symbol requires arguments count");
			final Integer argCount = argumentsCount.get();
			Preconditions.checkState(argCount > 1, "'do' expects at least one argument");
			final Stack<TypedValue> stack = frame.stack().substack(argCount);

			for (TypedValue expr : ImmutableList.copyOf(stack)) {
				stack.clear();
				final Code exprCode = expr.as(Code.class);
				exprCode.execute(frame);
			}

			TypedCalcUtils.expectExactReturnCount(returnsCount, stack.size());
		}
	}

	public void registerSymbol(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_DO, new DoSymbol());
	}
}
