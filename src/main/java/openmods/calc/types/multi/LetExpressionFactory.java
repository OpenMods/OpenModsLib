package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.Constant;
import openmods.calc.FixedFunctionSymbol;
import openmods.calc.ICalculatorFrame;
import openmods.calc.IExecutable;
import openmods.calc.ISymbol;
import openmods.calc.LocalFrame;
import openmods.calc.SymbolCall;
import openmods.calc.Value;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.BracketContainerNode;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.calc.parsing.SymbolGetNode;

public class LetExpressionFactory {

	private final TypeDomain domain;
	private final String letSymbolName;
	private final String listSymbolName;
	private final BinaryOperator<TypedValue> colonOperator;

	public LetExpressionFactory(TypeDomain domain, String letSymbolName, String listSymbolName, BinaryOperator<TypedValue> colonOperator) {
		this.domain = domain;
		this.letSymbolName = letSymbolName;
		this.listSymbolName = listSymbolName;
		this.colonOperator = colonOperator;
	}

	private class LetNode implements IExprNode<TypedValue> {

		private static final String BRACKET_ARG_LIST = "[";
		private final IExprNode<TypedValue> argsNode;
		private final IExprNode<TypedValue> codeNode;

		public LetNode(IExprNode<TypedValue> argsNode, IExprNode<TypedValue> codeNode) {
			this.argsNode = argsNode;
			this.codeNode = codeNode;
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			// expecting [a:b:,c:1+2]. If correctly formed, arg name (symbol) will be transformed into symbol atom
			if (argsNode instanceof BracketContainerNode) {
				final BracketContainerNode<TypedValue> bracketNode = (BracketContainerNode<TypedValue>)argsNode;
				Preconditions.checkState(bracketNode.openingBracket.equals(BRACKET_ARG_LIST), "Expected list of arguments in square brackets on first argument of 'let'");

				int argumentCount = 0;
				for (IExprNode<TypedValue> argNode : bracketNode.getChildren()) {
					flattenArgNode(output, argNode);
					argumentCount++;
				}

				Preconditions.checkState(argumentCount > 0, "'let' expects at least one argument");
				// slighly inefficient, but compatible with hand-called instruction
				output.add(new SymbolCall<TypedValue>(listSymbolName, argumentCount, 1));
			} else { // assume list of arg pairs
				argsNode.flatten(output);
			}

			output.add(Value.create(Code.flattenAndWrap(domain, codeNode)));
			output.add(new SymbolCall<TypedValue>(letSymbolName, 2, 1));
		}

		private void flattenArgNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> argNode) {
			if (argNode instanceof BinaryOpNode) {
				final BinaryOpNode<TypedValue> opNode = (BinaryOpNode<TypedValue>)argNode;
				if (opNode.operator == colonOperator) {
					flattenArgNameNode(output, opNode.left);
					opNode.right.flatten(output);
					output.add(colonOperator);
					return;
				}
			}
			argNode.flatten(output); // not directly arg pair, but may still produce valid one
		}

		private void flattenArgNameNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> argNameNode) {
			if (argNameNode instanceof SymbolGetNode) {
				final SymbolGetNode<TypedValue> argSymbolNode = (SymbolGetNode<TypedValue>)argNameNode;
				final String symbolId = argSymbolNode.symbol();
				output.add(Value.create(domain.create(Symbol.class, Symbol.get(symbolId))));
			} else {
				// either something we have to call or expression resulting in symbol
				argNameNode.flatten(output);
			}
		}

		@Override
		public Iterable<IExprNode<TypedValue>> getChildren() {
			return ImmutableList.of(argsNode, codeNode);
		}

	}

	private class LetStateTransition extends SameStateSymbolTransition<TypedValue> {
		public LetStateTransition(ICompilerState<TypedValue> parentState) {
			super(parentState);
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			Preconditions.checkState(children.size() == 2, "Expected two arg for 'let' expression");
			return new LetNode(children.get(0), children.get(1));
		}
	}

	public ISymbolCallStateTransition<TypedValue> createStateTransition(ICompilerState<TypedValue> parentState) {
		return new LetStateTransition(parentState);
	}

	private class LetSymbol extends FixedFunctionSymbol<TypedValue> {

		public LetSymbol() {
			super(2, 1);
		}

		@Override
		public void call(ICalculatorFrame<TypedValue> currentFrame) {
			final TypedValue code = currentFrame.stack().pop();
			Preconditions.checkState(code.is(Code.class), "Expected code on second 'if' parameter, got %s", code);

			final TypedValue paramPairs = currentFrame.stack().pop();
			Preconditions.checkState(paramPairs.is(Cons.class), "Expected list or args on first 'if' parameter, got %s", paramPairs);

			final LocalFrame<TypedValue> letFrame = new LocalFrame<TypedValue>(currentFrame);

			paramPairs.unwrap(Cons.class).visit(new Cons.LinearVisitor() {

				@Override
				public void value(TypedValue value, boolean isLast) {
					final Cons pair = value.unwrap(Cons.class);
					final Symbol name = pair.car.unwrap(Symbol.class);
					letFrame.setLocalSymbol(name.value, Constant.create(pair.cdr));
				}

				@Override
				public void end(TypedValue terminator) {}

				@Override
				public void begin() {}
			});

			code.unwrap(Code.class).execute(letFrame);
			for (TypedValue ret : letFrame.stack())
				currentFrame.stack().push(ret);
		}
	}

	public ISymbol<TypedValue> createSymbol() {
		return new LetSymbol();
	}
}
