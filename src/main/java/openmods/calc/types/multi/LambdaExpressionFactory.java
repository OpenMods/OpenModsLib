package openmods.calc.types.multi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.executable.BinaryOperator;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.UnaryOperator;
import openmods.calc.parsing.node.BinaryOpNode;
import openmods.calc.parsing.node.BracketContainerNode;
import openmods.calc.parsing.node.IExprNode;
import openmods.calc.parsing.node.MappedExprNodeFactory.IBinaryExprNodeFactory;
import openmods.calc.symbol.FixedCallable;
import openmods.utils.Stack;

public class LambdaExpressionFactory {

	private final TypeDomain domain;
	private final TypedValue nullValue;
	private final ClosureCompilerHelper closureCompiler;

	public LambdaExpressionFactory(TypedValue nullValue, UnaryOperator<TypedValue> varArgMarker) {
		this.nullValue = nullValue;
		this.domain = nullValue.domain;
		closureCompiler = new ClosureCompilerHelper(domain, varArgMarker);
	}

	private static IBindPattern extractPatternFromValue(TypedValue arg) {
		if (arg.is(Symbol.class)) {
			return BindPatternTranslator.createPatternForVarName(arg.as(Symbol.class).value);
		} else if (arg.is(IBindPattern.class)) { return arg.as(IBindPattern.class); }

		throw new IllegalArgumentException("Failed to parse lambda arg list, expected symbol or pattern, got " + arg);
	}

	private List<IBindPattern> extractPatternFromValues(TypedValue argValues) {
		final List<IBindPattern> args = Lists.newArrayList();
		for (TypedValue arg : Cons.toIterable(argValues, nullValue))
			args.add(extractPatternFromValue(arg));
		return args;
	}

	private class ClosureSymbol extends FixedCallable<TypedValue> {

		public ClosureSymbol() {
			super(2, 1);
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack();
			final Code code = stack.pop().as(Code.class, "second argument of 'closure'");

			final TypedValue argValues = stack.pop();
			final List<IBindPattern> args = extractPatternFromValues(argValues);

			frame.stack().push(CallableValue.wrap(domain, new Closure(frame.symbols(), code, args)));
		}
	}

	private class ClosureVarSymbol extends FixedCallable<TypedValue> {

		public ClosureVarSymbol() {
			super(3, 1);
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack();
			final Code code = stack.pop().as(Code.class, "third argument of 'closurevar'");
			final String varArgName = stack.pop().as(String.class, "second argument of 'closurevar'");
			final TypedValue argValues = stack.pop();

			final List<IBindPattern> args = extractPatternFromValues(argValues);
			stack.push(CallableValue.wrap(domain, new ClosureVar(nullValue, frame.symbols(), code, args, varArgName)));
		}
	}

	private class LambdaExpr extends BinaryOpNode<TypedValue> {

		public LambdaExpr(BinaryOperator<TypedValue> operator, IExprNode<TypedValue> left, IExprNode<TypedValue> right) {
			super(operator, left, right);
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			if (left instanceof BracketContainerNode) {
				closureCompiler.compile(output, left.getChildren(), right);
			} else {
				closureCompiler.compile(output, ImmutableList.of(left), right);
			}
		}
	}

	public IBinaryExprNodeFactory<TypedValue> createLambdaExprNodeFactory(final BinaryOperator<TypedValue> lambdaOp) {
		return new IBinaryExprNodeFactory<TypedValue>() {
			@Override
			public IExprNode<TypedValue> create(IExprNode<TypedValue> leftChild, IExprNode<TypedValue> rightChild) {
				return new LambdaExpr(lambdaOp, leftChild, rightChild);
			}
		};
	}

	public void registerSymbol(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_CLOSURE, new ClosureSymbol());
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_CLOSURE_VAR, new ClosureVarSymbol());
	}

}
