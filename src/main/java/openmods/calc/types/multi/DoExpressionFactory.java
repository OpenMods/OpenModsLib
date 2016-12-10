package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.calc.parsing.SymbolCallNode;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class DoExpressionFactory {

	private final TypeDomain domain;

	public DoExpressionFactory(TypeDomain domain) {
		this.domain = domain;
	}

	private class DoExpr extends SameStateSymbolTransition<TypedValue> {

		public DoExpr(ICompilerState<TypedValue> parentState) {
			super(parentState);
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			Preconditions.checkState(children.size() > 1, "'do' expects at least one argument");

			final List<IExprNode<TypedValue>> args = Lists.newArrayList();
			for (IExprNode<TypedValue> child : children)
				args.add(new RawCodeExprNode(domain, child));

			return new SymbolCallNode<TypedValue>(TypedCalcConstants.SYMBOL_DO, args);
		}
	}

	public ISymbolCallStateTransition<TypedValue> createStateTransition(ICompilerState<TypedValue> compilerState) {
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
