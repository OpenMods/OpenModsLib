package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.Environment;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.Value;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.BracketContainerNode;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.MappedExprNodeFactory.IBinaryExprNodeFactory;
import openmods.calc.parsing.SymbolGetNode;

public class LambdaExpressionFactory {

	private final TypeDomain domain;
	private final TypedValue nullValue;

	public LambdaExpressionFactory(TypeDomain domain, TypedValue nullValue) {
		this.domain = domain;
		this.nullValue = nullValue;
	}

	private class ClosureSymbol extends FixedCallable<TypedValue> {

		public ClosureSymbol() {
			super(2, 1);
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final TypedValue right = frame.stack().pop();
			final Code code = right.as(Code.class, "second argument of 'closure'");

			final TypedValue left = frame.stack().pop();

			final List<IBindPattern> args = Lists.newArrayList();
			if (left.is(Cons.class)) {
				final Cons argsList = left.as(Cons.class);

				argsList.visit(new Cons.LinearVisitor() {
					@Override
					public void value(TypedValue value, boolean isLast) {
						if (value.is(Symbol.class)) {
							args.add(BindPatternTranslator.createPatternForVarName(value.as(Symbol.class).value));
						} else if (value.is(IBindPattern.class)) {
							args.add(value.as(IBindPattern.class));
						} else {
							throw new IllegalArgumentException("Failed to parse lambda arg list, expected symbol or pattern, got " + value);
						}
					}

					@Override
					public void end(TypedValue terminator) {}

					@Override
					public void begin() {}
				});
			} else {
				Preconditions.checkState(left == nullValue, "Expected list of symbols as first argument of 'closure', got %s", left);
				// empty arg list
			}

			frame.stack().push(CallableValue.wrap(domain, new Closure(frame.symbols(), code, args)));
		}
	}

	private class LambdaExpr extends BinaryOpNode<TypedValue> {

		public LambdaExpr(BinaryOperator<TypedValue> operator, IExprNode<TypedValue> left, IExprNode<TypedValue> right) {
			super(operator, left, right);
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			extractArgNamesList(output);
			flattenClosureCode(output);
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_CLOSURE, 2, 1));
		}

		private void flattenClosureCode(List<IExecutable<TypedValue>> output) {
			if (right instanceof RawCodeExprNode) {
				right.flatten(output);
			} else {
				output.add(Value.create(Code.flattenAndWrap(domain, right)));
			}
		}

		private void extractArgNamesList(List<IExecutable<TypedValue>> output) {

			// yup, any bracket. I prefer (), but [] are only option in prefix
			if (left instanceof BracketContainerNode) {
				int count = 0;
				for (IExprNode<TypedValue> arg : left.getChildren()) {
					extractPatternFromNode(output, arg);
					count++;
				}
				output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, count, 1));
			} else {
				extractPatternFromNode(output, left);
				output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, 1, 1));
			}
		}

		private void extractPatternFromNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> arg) {
			if (arg instanceof SymbolGetNode) {
				// optimization - single variable -> use symbol
				final SymbolGetNode<TypedValue> var = (SymbolGetNode<TypedValue>)arg;
				output.add(Value.create(Symbol.get(domain, var.symbol())));
			} else {
				output.add(Value.create(Code.flattenAndWrap(domain, arg)));
				output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_PATTERN, 1, 1));
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
	}

}
