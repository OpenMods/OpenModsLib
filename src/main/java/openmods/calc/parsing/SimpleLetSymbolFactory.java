package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import openmods.calc.BinaryOperator;
import openmods.calc.Constant;
import openmods.calc.ExprType;
import openmods.calc.ICalculatorFrame;
import openmods.calc.ICompilerMapFactory;
import openmods.calc.IExecutable;
import openmods.calc.LocalFrame;
import openmods.utils.Stack;

public class SimpleLetSymbolFactory<E> {

	public static final String SYMBOL_LET = "let";

	public class ExtendedCompilerMapFactory extends BasicCompilerMapFactory<E> {
		@Override
		protected DefaultExprNodeFactory<E> createExprNodeFactory(IValueParser<E> valueParser) {
			return SquareBracketContainerNode.install(new MappedExprNodeFactory<E>(valueParser));
		}

		@Override
		protected void configureCompilerStateCommon(MappedCompilerState<E> compilerState) {
			super.configureCompilerStateCommon(compilerState);
			compilerState.addStateTransition(SYMBOL_LET, createParserTransition(compilerState));
		}
	}

	private static class KeyValueSeparator<E> extends BinaryOperator<E> {
		private KeyValueSeparator(String id, int precendence) {
			super(id, precendence);
		}

		@Override
		public E execute(E left, E right) {
			throw new UnsupportedOperationException(); // not supposed to be used directly;
		}
	}

	private final BinaryOperator<E> keyValueSeparator;

	public SimpleLetSymbolFactory(String keyValueSeparatorId, int keyValueSeparatorPriority) {
		this.keyValueSeparator = new KeyValueSeparator<E>(keyValueSeparatorId, keyValueSeparatorPriority);
	}

	public BinaryOperator<E> getKeyValueSeparator() {
		return keyValueSeparator;
	}

	private class LetExecutable implements IExecutable<E> {

		private final Map<String, IExecutable<E>> variables;

		private final IExecutable<E> expr;

		public LetExecutable(Map<String, IExecutable<E>> variables, IExecutable<E> expr) {
			this.variables = variables;
			this.expr = expr;
		}

		@Override
		public void execute(ICalculatorFrame<E> frame) {
			final LocalFrame<E> letFrame = new LocalFrame<E>(frame);

			for (Map.Entry<String, IExecutable<E>> e : variables.entrySet()) {
				final String varlId = e.getKey();
				final LocalFrame<E> varFrame = new LocalFrame<E>(frame);
				e.getValue().execute(varFrame);
				final Stack<E> varExprResult = varFrame.stack();
				Preconditions.checkState(varExprResult.size() == 1, "Invalid expression in 'let' argument %s: expected one return, got %s", varlId, varExprResult.size());
				letFrame.setLocalSymbol(varlId, Constant.create(varExprResult.pop()));
			}

			expr.execute(letFrame);

			for (E result : letFrame.stack())
				frame.stack().push(result);
		}

		@Override
		public String serialize() {
			return "<let>"; // unserializable in most calculators
		}

	}

	private class LetNode implements IExprNode<E> {
		private final IExprNode<E> argsNode;
		private final IExprNode<E> codeNode;

		public LetNode(IExprNode<E> argsNode, IExprNode<E> codeNode) {
			this.argsNode = argsNode;
			this.codeNode = codeNode;
		}

		@Override
		public void flatten(List<IExecutable<E>> output) {
			// expecting [a:b:,c:1+2]. If correctly formed, arg name (symbol) will be transformed into symbol atom
			Preconditions.checkState(argsNode instanceof SquareBracketContainerNode, "Malformed 'let' expressions: expected brackets, got %s", argsNode);
			final SquareBracketContainerNode<E> bracketNode = (SquareBracketContainerNode<E>)argsNode;

			ImmutableMap.Builder<String, IExecutable<E>> vars = ImmutableMap.builder();
			for (IExprNode<E> argNode : bracketNode.getChildren())
				flattenArgNode(vars, argNode);

			IExecutable<E> code = ExprUtils.flattenNode(codeNode);
			output.add(new LetExecutable(vars.build(), code));
		}

		private void flattenArgNode(ImmutableMap.Builder<String, IExecutable<E>> output, IExprNode<E> argNode) {
			Preconditions.checkState(argNode instanceof BinaryOpNode, "Expected expression in from <name>:<expr>, got %s", argNode);
			final BinaryOpNode<E> opNode = (BinaryOpNode<E>)argNode;
			Preconditions.checkState(opNode.operator == keyValueSeparator, "Expected operator %s as separator, got %s", keyValueSeparator.id, opNode.operator.id);

			final IExprNode<E> nameNode = opNode.left;
			Preconditions.checkState(nameNode instanceof SymbolGetNode, "Expected symbol, got %s", nameNode);

			final IExprNode<E> valueNode = opNode.right;
			output.put(((SymbolGetNode<E>)nameNode).symbol(), ExprUtils.flattenNode(valueNode));
		}

		@Override
		public Iterable<IExprNode<E>> getChildren() {
			return ImmutableList.of(argsNode, codeNode);
		}
	}

	public ISymbolCallStateTransition<E> createParserTransition(final ICompilerState<E> currentState) {
		return new ISymbolCallStateTransition<E>() {

			@Override
			public ICompilerState<E> getState() {
				return currentState;
			}

			@Override
			public IExprNode<E> createRootNode(List<IExprNode<E>> children) {
				Preconditions.checkState(children.size() == 2, "Expected two args for 'let' expression");
				return new LetNode(children.get(0), children.get(1));
			}
		};
	}

	public ICompilerMapFactory<E, ExprType> createCompilerFactory() {
		return new ExtendedCompilerMapFactory();
	}
}
