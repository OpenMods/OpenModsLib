package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.Environment;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.Value;
import openmods.calc.parsing.BracketContainerNode;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.MappedExprNodeFactory.IBinaryExprNodeFactory;

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

			final List<String> args = Lists.newArrayList();
			if (left.is(Cons.class)) {
				final Cons argsList = left.as(Cons.class);

				argsList.visit(new Cons.LinearVisitor() {
					@Override
					public void value(TypedValue value, boolean isLast) {
						args.add(value.as(Symbol.class, "lambda args list").value);
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

			frame.stack().push(domain.create(ICallable.class, new Closure(frame.symbols(), code, args)));
		}
	}

	private class LambdaExpr implements IExprNode<TypedValue> {
		private final IExprNode<TypedValue> argNames;
		private final IExprNode<TypedValue> code;

		public LambdaExpr(IExprNode<TypedValue> argNames, IExprNode<TypedValue> code) {
			this.argNames = argNames;
			this.code = code;
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			output.add(Value.create(extractArgNamesList()));
			flattenClosureCode(output);
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_CLOSURE, 2, 1));
		}

		private void flattenClosureCode(List<IExecutable<TypedValue>> output) {
			if (code instanceof RawCodeExprNode) {
				code.flatten(output);
			} else {
				output.add(Value.create(Code.flattenAndWrap(domain, code)));
			}
		}

		private TypedValue extractArgNamesList() {
			final List<TypedValue> args = Lists.newArrayList();
			// yup, any bracket. I prefer (), but [] are only option in prefix
			if (argNames instanceof BracketContainerNode) {
				for (IExprNode<TypedValue> arg : argNames.getChildren())
					args.add(extractNameFromNode(arg));
			} else {
				args.add(extractNameFromNode(argNames));
			}

			return Cons.createList(args, nullValue);
		}

		private TypedValue extractNameFromNode(IExprNode<TypedValue> arg) {
			try {
				return TypedCalcUtils.extractNameFromNode(domain, arg);
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("Expected single symbol or list of symbols on left side of lambda operator, got " + arg);
			}
		}

		@Override
		public Iterable<IExprNode<TypedValue>> getChildren() {
			return ImmutableList.of(argNames, code);
		}

	}

	public IBinaryExprNodeFactory<TypedValue> createLambdaExprNodeFactory() {
		return new IBinaryExprNodeFactory<TypedValue>() {

			@Override
			public IExprNode<TypedValue> create(IExprNode<TypedValue> leftChild, IExprNode<TypedValue> rightChild) {
				return new LambdaExpr(leftChild, rightChild);
			}
		};
	}

	public void registerSymbol(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_CLOSURE, new ClosureSymbol());
	}

}
