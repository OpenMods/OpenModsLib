package openmods.model.eval;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import com.google.common.math.DoubleMath;
import info.openmods.calc.executable.OperatorDictionary;
import info.openmods.calc.parsing.ast.IAstParser;
import info.openmods.calc.parsing.ast.IModifierStateTransition;
import info.openmods.calc.parsing.ast.INodeFactory;
import info.openmods.calc.parsing.ast.IOperator;
import info.openmods.calc.parsing.ast.IParserState;
import info.openmods.calc.parsing.ast.ISymbolCallStateTransition;
import info.openmods.calc.parsing.ast.InfixParser;
import info.openmods.calc.parsing.ast.OperatorArity;
import info.openmods.calc.parsing.ast.SameStateSymbolTransition;
import info.openmods.calc.parsing.ast.SingleStateTransition;
import info.openmods.calc.parsing.token.Token;
import info.openmods.calc.parsing.token.TokenIterator;
import info.openmods.calc.parsing.token.TokenType;
import info.openmods.calc.parsing.token.Tokenizer;
import info.openmods.calc.types.fp.DoubleParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IClip;
import net.minecraftforge.common.model.animation.IJoint;

public class EvaluatorFactory {

	private static final int PRIORITY_UNARY = 400;

	private static final int NUMERIC_PRIORITY = 300;

	private static final int PRIORITY_POWER = NUMERIC_PRIORITY + 4;
	private static final String OPERATOR_POWER = "**";

	private static final int PRIORITY_MULTIPLY = NUMERIC_PRIORITY + 3;
	private static final String OPERATOR_MULTIPLY = "*";
	private static final String OPERATOR_DIVIDE = "/";
	private static final String OPERATOR_MOD = "%";

	private static final int PRIORITY_ADD = NUMERIC_PRIORITY + 2;
	private static final String OPERATOR_ADD = "+";
	private static final String OPERATOR_SUBTRACT = "-";

	private static final int COMPARISION_PRIORITY = 200;
	private static final String OPERATOR_LE = "<=";
	private static final String OPERATOR_LT = "<";
	private static final String OPERATOR_GE = ">=";
	private static final String OPERATOR_GT = ">";
	private static final String OPERATOR_EQ = "=";
	private static final String OPERATOR_NE = "<>";

	private static final int LOGIC_PRIORITY = 100;

	private static final String OPERATOR_NOT = "!";

	private static final int PRIORITY_AND = LOGIC_PRIORITY + 3; // &
	private static final String OPERATOR_AND = "&";

	private static final int PRIORITY_OR = LOGIC_PRIORITY + 2; // |
	private static final String OPERATOR_OR = "|";

	private static final int PRIORITY_COMPARE = LOGIC_PRIORITY + 1; // ^
	private static final String OPERATOR_XOR = "^";
	private static final String OPERATOR_IFF = "<=>";

	private static final int PRIORITY_ASSIGN = 0;
	private static final String OPERATOR_ASSIGN = ":=";

	private static final String MODIFIER_OP = "@";

	private static class Expr<T> {

		public Optional<T> getConstValue() {
			return Optional.empty();
		}
	}

	private abstract static class NumericExpr extends Expr<Float> {
		public abstract float evaluate(Map<String, Float> args);
	}

	private abstract static class BooleanExpr extends Expr<Boolean> {
		public abstract boolean evaluate(Map<String, Float> args);
	}

	private static class Scope {
		private final Map<String, ExprFactory> vals;

		public Scope(Map<String, ExprFactory> vals) {
			this.vals = ImmutableMap.copyOf(vals);
		}

		public ExprFactory get(String name) {
			return vals.get(name);
		}

		public Scope expand(Map<String, ExprFactory> patch) {
			return new Scope(patch) {
				@Override
				public ExprFactory get(String name) {
					final ExprFactory result = super.get(name);
					return result != null? result : Scope.this.get(name);
				}
			};
		}
	}

	private interface ExprFactory {

		public NumericExpr createNumericExpr(List<Node> children, Scope scope);

		public BooleanExpr createBooleanExpr(List<Node> children, Scope scope);

	}

	private interface NodeOp extends ExprFactory {
		public String toString(List<Node> children);
	}

	private static BooleanExpr noBooleanValue() {
		throw new UnsupportedOperationException("Expression does not yield boolean value");
	}

	private static NumericExpr noNumericValue() {
		throw new UnsupportedOperationException("Expression does not yield numeric value");
	}

	private abstract static class NumericExprFactory implements ExprFactory {
		@Override
		public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
			return noBooleanValue();
		}
	}

	private abstract static class BooleanExprFactory implements ExprFactory {
		@Override
		public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
			return noNumericValue();
		}
	}

	private abstract static class Operator implements IOperator<Operator>, NodeOp {

		private final String id;

		public final int precedence;

		public Operator(String id, int precedence) {
			this.id = id;
			this.precedence = precedence;
		}

		@Override
		public String id() {
			return id;
		}

		@Override
		public String toString(List<Node> children) {
			final StringBuilder result = new StringBuilder();
			result.append('(').append(id);
			for (Node child : children)
				result.append(' ').append(child.toString());
			return result.append(')').toString();
		}
	}

	private static Node bindNodeOpNodeToScope(Node node, final Scope newScope) {
		final NodeOp nodeOp = node.op;
		return new Node(new NodeOp() {

			@Override
			public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
				return nodeOp.createNumericExpr(children, newScope);
			}

			@Override
			public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
				return nodeOp.createBooleanExpr(children, newScope);
			}

			@Override
			public String toString(List<Node> children) {
				return nodeOp.toString(children);
			}
		}, node.children);
	}

	private abstract static class UnaryOperator<T, E extends Expr<T>> extends Operator {

		public UnaryOperator(String id, int precedence) {
			super(id, precedence);
		}

		@Override
		public OperatorArity arity() {
			return OperatorArity.UNARY;
		}

		@Override
		public boolean isLowerPriority(Operator other) {
			return precedence < other.precedence;
		}

		protected E createExpr(List<Node> children, Scope scope) {
			Preconditions.checkState(children.size() == 1);
			final E arg = createExpr(children.get(0), scope);

			final Optional<T> maybeConst = arg.getConstValue();

			if (maybeConst.isPresent()) {
				return createConstNode(maybeConst.get());
			} else {
				return createEvaluatingNode(arg);
			}
		}

		protected abstract E createExpr(Node node, Scope scope);

		protected abstract E createConstNode(T value);

		protected abstract E createEvaluatingNode(E arg);

	}

	private abstract static class NumericUnaryOperator extends UnaryOperator<Float, NumericExpr> {

		public NumericUnaryOperator(String id, int precedence) {
			super(id, precedence);
		}

		@Override
		public OperatorArity arity() {
			return OperatorArity.UNARY;
		}

		@Override
		public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
			return createExpr(children, scope);
		}

		@Override
		public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
			return noBooleanValue();
		}

		@Override
		protected NumericExpr createConstNode(Float value) {
			final float result = apply(value);
			return new NumericConstExpr(result);
		}

		@Override
		protected NumericExpr createEvaluatingNode(final NumericExpr arg) {
			return new NumericExpr() {
				@Override
				public float evaluate(Map<String, Float> args) {
					final float value = arg.evaluate(args);
					return apply(value);
				}
			};
		}

		@Override
		protected NumericExpr createExpr(Node node, Scope scope) {
			return node.createNumericExprFromNode(scope);
		}

		protected abstract float apply(float value);
	}

	private static abstract class BooleanUnaryOperator extends UnaryOperator<Boolean, BooleanExpr> {

		public BooleanUnaryOperator(String id, int precedence) {
			super(id, precedence);
		}

		@Override
		public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
			return noNumericValue();
		}

		@Override
		public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
			return createExpr(children, scope);
		}

		@Override
		protected BooleanExpr createExpr(Node node, Scope scope) {
			return node.createBooleanExprFromNode(scope);
		}

		@Override
		protected BooleanExpr createConstNode(Boolean value) {
			return BooleanConstExpr.valueOf(apply(value));
		}

		@Override
		protected BooleanExpr createEvaluatingNode(final BooleanExpr arg) {
			return new BooleanExpr() {
				@Override
				public boolean evaluate(Map<String, Float> args) {
					final boolean value = arg.evaluate(args);
					return apply(value);
				}
			};
		}

		protected abstract boolean apply(boolean arg);
	}

	private abstract static class BinaryOperator<T, ArgExpr extends Expr<T>, ResultExpr extends Expr<?>> extends Operator {

		public BinaryOperator(String id, int precedence) {
			super(id, precedence);
		}

		@Override
		public OperatorArity arity() {
			return OperatorArity.BINARY;
		}

		@Override
		public boolean isLowerPriority(Operator other) {
			return precedence <= other.precedence; // TODO associativity, when needed
		}

		public ResultExpr createExpr(List<Node> children, Scope scope) {
			Preconditions.checkState(children.size() == 2);
			final ArgExpr leftArg = createExpr(children.get(0), scope);
			final Optional<T> maybeLeftConst = leftArg.getConstValue();

			final ArgExpr rightArg = createExpr(children.get(1), scope);
			final Optional<T> maybeRightConst = rightArg.getConstValue();

			if (maybeLeftConst.isPresent()) {
				final T leftConst = maybeLeftConst.get();
				if (maybeRightConst.isPresent()) {
					final T rightConst = maybeRightConst.get();
					return bothConst(leftConst, rightConst);
				} else {
					return leftConst(leftConst, rightArg);
				}
			} else if (maybeRightConst.isPresent()) {
				final T rightConst = maybeRightConst.get();
				return rightConst(leftArg, rightConst);
			}

			return nonConst(leftArg, rightArg);
		}

		protected abstract ArgExpr createExpr(Node node, Scope scope);

		protected abstract ResultExpr bothConst(T leftConst, T rightConst);

		protected abstract ResultExpr rightConst(ArgExpr leftArg, T rightConst);

		protected abstract ResultExpr leftConst(T leftConst, ArgExpr rightArg);

		protected abstract ResultExpr nonConst(ArgExpr leftArg, ArgExpr rightArg);
	}

	private abstract static class NumericBinaryOperator extends BinaryOperator<Float, NumericExpr, NumericExpr> {

		public NumericBinaryOperator(String id, int precedence) {
			super(id, precedence);
		}

		@Override
		public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
			return createExpr(children, scope);
		}

		@Override
		public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
			return noBooleanValue();
		}

		@Override
		protected NumericExpr createExpr(Node node, Scope scope) {
			return node.createNumericExprFromNode(scope);
		}

		@Override
		protected NumericExpr bothConst(final Float leftConst, final Float rightConst) {
			final float value = apply(leftConst, rightConst);
			return new NumericConstExpr(value);
		}

		@Override
		protected NumericExpr rightConst(final NumericExpr leftArg, Float rightConst) {
			final float unpackedConst = rightConst.floatValue();
			return new NumericExpr() {
				@Override
				public float evaluate(Map<String, Float> args) {
					final float leftValue = leftArg.evaluate(args);
					return apply(leftValue, unpackedConst);
				}
			};
		}

		@Override
		protected NumericExpr leftConst(Float leftConst, final NumericExpr rightArg) {
			final float unpackedConst = leftConst.floatValue();
			return new NumericExpr() {
				@Override
				public float evaluate(Map<String, Float> args) {
					final float rightValue = rightArg.evaluate(args);
					return apply(unpackedConst, rightValue);
				}
			};
		}

		@Override
		protected NumericExpr nonConst(final NumericExpr leftArg, final NumericExpr rightArg) {
			return new NumericExpr() {
				@Override
				public float evaluate(Map<String, Float> args) {
					final float leftValue = leftArg.evaluate(args);
					final float rightValue = rightArg.evaluate(args);
					return apply(leftValue, rightValue);
				}
			};
		}

		protected abstract float apply(float left, float right);
	}

	private abstract static class BinaryOperatorWithRightNeutralElement extends NumericBinaryOperator {

		protected final float neutralElement;

		public BinaryOperatorWithRightNeutralElement(String id, int precedence, float neutralElement) {
			super(id, precedence);
			this.neutralElement = neutralElement;
		}

		@Override
		protected NumericExpr rightConst(NumericExpr leftArg, Float rightConst) {
			if (rightConst == neutralElement) return leftArg;
			return super.rightConst(leftArg, rightConst);
		}
	}

	private abstract static class BinaryOperatorWithNeutralElement extends BinaryOperatorWithRightNeutralElement {

		public BinaryOperatorWithNeutralElement(String id, int precedence, float neutralElement) {
			super(id, precedence, neutralElement);
		}

		@Override
		protected NumericExpr leftConst(Float leftConst, NumericExpr rightArg) {
			if (leftConst == neutralElement) return rightArg;
			return super.leftConst(leftConst, rightArg);
		}
	}

	private static abstract class BooleanBinaryOperator extends BinaryOperator<Boolean, BooleanExpr, BooleanExpr> {

		public BooleanBinaryOperator(String id, int precedence) {
			super(id, precedence);
		}

		@Override
		public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
			return noNumericValue();
		}

		@Override
		public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
			return createExpr(children, scope);
		}

		@Override
		protected BooleanExpr createExpr(Node node, Scope scope) {
			return node.createBooleanExprFromNode(scope);
		}

		@Override
		protected BooleanExpr bothConst(Boolean leftConst, Boolean rightConst) {
			final boolean value = apply(leftConst, rightConst);
			return BooleanConstExpr.valueOf(value);
		}

		@Override
		protected BooleanExpr rightConst(final BooleanExpr leftArg, Boolean rightConst) {
			return partialApply(rightConst, leftArg);
		}

		@Override
		protected BooleanExpr leftConst(Boolean leftConst, final BooleanExpr rightArg) {
			return partialApply(leftConst, rightArg);
		}

		@Override
		protected BooleanExpr nonConst(final BooleanExpr leftArg, final BooleanExpr rightArg) {
			return new BooleanExpr() {
				@Override
				public boolean evaluate(Map<String, Float> args) {
					final boolean leftValue = leftArg.evaluate(args);
					final boolean rightValue = rightArg.evaluate(args);
					return apply(leftValue, rightValue);
				}
			};
		}

		protected abstract BooleanExpr partialApply(boolean constArg, BooleanExpr arg);

		protected abstract boolean apply(boolean left, boolean right);
	}

	private static abstract class ComparisionOperator extends BinaryOperator<Float, NumericExpr, BooleanExpr> {

		public ComparisionOperator(String id, int precedence) {
			super(id, precedence);
		}

		@Override
		public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
			return noNumericValue();
		}

		@Override
		public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
			return createExpr(children, scope);
		}

		@Override
		protected NumericExpr createExpr(Node node, Scope scope) {
			return node.createNumericExprFromNode(scope);
		}

		@Override
		protected BooleanExpr bothConst(Float leftConst, Float rightConst) {
			final boolean value = apply(leftConst, rightConst);
			return BooleanConstExpr.valueOf(value);
		}

		@Override
		protected BooleanExpr rightConst(final NumericExpr leftArg, Float rightConst) {
			final float unpackedConst = rightConst.floatValue();
			return new BooleanExpr() {
				@Override
				public boolean evaluate(Map<String, Float> args) {
					final float leftValue = leftArg.evaluate(args);
					return apply(leftValue, unpackedConst);
				}
			};
		}

		@Override
		protected BooleanExpr leftConst(Float leftConst, final NumericExpr rightArg) {
			final float unpackedConst = leftConst.floatValue();
			return new BooleanExpr() {
				@Override
				public boolean evaluate(Map<String, Float> args) {
					final float rightValue = rightArg.evaluate(args);
					return apply(unpackedConst, rightValue);
				}
			};
		}

		@Override
		protected BooleanExpr nonConst(final NumericExpr leftArg, final NumericExpr rightArg) {
			return new BooleanExpr() {
				@Override
				public boolean evaluate(Map<String, Float> args) {
					final float leftValue = leftArg.evaluate(args);
					final float rightValue = rightArg.evaluate(args);
					return apply(leftValue, rightValue);
				}
			};
		}

		protected abstract boolean apply(float left, float right);
	}

	private static final OperatorDictionary<Operator> OPERATORS = new OperatorDictionary<>();

	private static final Operator OP_ASSIGN = new Operator(OPERATOR_ASSIGN, PRIORITY_ASSIGN) {

		@Override
		public OperatorArity arity() {
			return OperatorArity.BINARY;
		}

		@Override
		public boolean isLowerPriority(Operator other) {
			return precedence <= other.precedence;
		}

		@Override
		public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
			throw new UnsupportedOperationException("Assign can only be used as top operator");
		}

		@Override
		public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
			throw new UnsupportedOperationException("Assign can only be used as top operator");
		}

	};

	static {
		OPERATORS.registerOperator(new NumericUnaryOperator(OPERATOR_ADD, PRIORITY_UNARY) {
			@Override
			protected float apply(float value) {
				return +value;
			}
		});
		OPERATORS.registerOperator(new NumericUnaryOperator(OPERATOR_SUBTRACT, PRIORITY_UNARY) {
			@Override
			protected float apply(float value) {
				return -value;
			}
		});

		OPERATORS.registerOperator(new BinaryOperatorWithRightNeutralElement(OPERATOR_POWER, PRIORITY_POWER, 1) {
			@Override
			protected NumericExpr rightConst(NumericExpr leftArg, Float rightConst) {
				if (rightConst == 0) return new NumericConstExpr(1); // per docs, Math.pow(x, 0) == 1, even for inf and nan
				return super.rightConst(leftArg, rightConst);
			}

			@Override
			protected float apply(float left, float right) {
				return (float)Math.pow(left, right);
			}
		});

		OPERATORS.registerOperator(new BinaryOperatorWithNeutralElement(OPERATOR_MULTIPLY, PRIORITY_MULTIPLY, 1) {
			// not doing 0 * x == 0, since 0 * Infinity == NaN
			// even if 0 * 0, 0 * x and x * 0 are patched here, I can't patch x * y without performance loss

			@Override
			protected float apply(float left, float right) {
				return left * right;
			}
		});
		OPERATORS.registerOperator(new BinaryOperatorWithRightNeutralElement(OPERATOR_DIVIDE, PRIORITY_MULTIPLY, 1) {
			// same issue as multiplication, skipping 0 / x optimization
			@Override
			protected float apply(float left, float right) {
				return left / right;
			}
		});

		OPERATORS.registerOperator(new NumericBinaryOperator(OPERATOR_MOD, PRIORITY_MULTIPLY) {
			@Override
			protected float apply(float left, float right) {
				return left % right;
			}
		});

		OPERATORS.registerOperator(new BinaryOperatorWithNeutralElement(OPERATOR_ADD, PRIORITY_ADD, 0) {
			@Override
			protected float apply(float left, float right) {
				return left + right;
			}
		});
		OPERATORS.registerOperator(new BinaryOperatorWithNeutralElement(OPERATOR_SUBTRACT, PRIORITY_ADD, 0) {
			@Override
			protected float apply(float left, float right) {
				return left - right;
			}
		});

		OPERATORS.registerOperator(new ComparisionOperator(OPERATOR_EQ, COMPARISION_PRIORITY) {
			@Override
			protected boolean apply(float left, float right) {
				return left == right;
			}
		});
		OPERATORS.registerOperator(new ComparisionOperator(OPERATOR_NE, COMPARISION_PRIORITY) {
			@Override
			protected boolean apply(float left, float right) {
				return left != right;
			}
		});
		OPERATORS.registerOperator(new ComparisionOperator(OPERATOR_GT, COMPARISION_PRIORITY) {
			@Override
			protected boolean apply(float left, float right) {
				return left > right;
			}
		});
		OPERATORS.registerOperator(new ComparisionOperator(OPERATOR_GE, COMPARISION_PRIORITY) {
			@Override
			protected boolean apply(float left, float right) {
				return left >= right;
			}
		});
		OPERATORS.registerOperator(new ComparisionOperator(OPERATOR_LT, COMPARISION_PRIORITY) {
			@Override
			protected boolean apply(float left, float right) {
				return left < right;
			}
		});
		OPERATORS.registerOperator(new ComparisionOperator(OPERATOR_LE, COMPARISION_PRIORITY) {
			@Override
			protected boolean apply(float left, float right) {
				return left <= right;
			}
		});

		OPERATORS.registerOperator(new BooleanUnaryOperator(OPERATOR_NOT, COMPARISION_PRIORITY) {
			@Override
			protected boolean apply(boolean arg) {
				return !arg;
			}
		});
		OPERATORS.registerOperator(new BooleanBinaryOperator(OPERATOR_AND, PRIORITY_AND) {
			@Override
			protected BooleanExpr partialApply(boolean constArg, BooleanExpr arg) {
				return constArg? arg : EXPR_FALSE;
			}

			@Override
			protected boolean apply(boolean left, boolean right) {
				return left && right;
			}
		});
		OPERATORS.registerOperator(new BooleanBinaryOperator(OPERATOR_OR, PRIORITY_OR) {
			@Override
			protected BooleanExpr partialApply(boolean constArg, BooleanExpr arg) {
				return constArg? EXPR_TRUE : arg;
			}

			@Override
			protected boolean apply(boolean left, boolean right) {
				return left || right;
			}
		});
		OPERATORS.registerOperator(new BooleanBinaryOperator(OPERATOR_XOR, PRIORITY_COMPARE) {
			@Override
			protected BooleanExpr partialApply(boolean constArg, final BooleanExpr arg) {
				return constArg? new BooleanNotExpr(arg) : arg;
			}

			@Override
			protected boolean apply(boolean left, boolean right) {
				return left ^ right;
			}
		});
		OPERATORS.registerOperator(new BooleanBinaryOperator(OPERATOR_IFF, PRIORITY_COMPARE) {
			@Override
			protected BooleanExpr partialApply(boolean constArg, final BooleanExpr arg) {
				return constArg? arg : new BooleanNotExpr(arg);
			}

			@Override
			protected boolean apply(boolean left, boolean right) {
				return left ^ right;
			}
		});

		OPERATORS.registerOperator(OP_ASSIGN);
	}

	private abstract static class SymbolNodeOp implements NodeOp {
		public final String symbol;

		public SymbolNodeOp(String symbol) {
			this.symbol = symbol;
		}

		@Override
		public String toString(List<Node> children) {
			final StringBuilder result = new StringBuilder();
			result.append('(').append(symbol);
			for (Node child : children)
				result.append(' ').append(child.toString());
			return result.append(')').toString();
		}

		@Override
		public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
			final ExprFactory maybeMacro = scope.get(symbol);
			if (maybeMacro != null) {
				return maybeMacro.createBooleanExpr(children, scope);
			} else {
				throw new IllegalArgumentException("Unknown macro: " + symbol);
			}
		}
	}

	private static class NodeOpGet extends SymbolNodeOp {
		public NodeOpGet(String symbol) {
			super(symbol);
		}

		@Override
		public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
			final ExprFactory maybeMacro = scope.get(symbol);
			if (maybeMacro != null) {
				// may have children when placed via macro arg
				return maybeMacro.createNumericExpr(children, scope);
			} else {
				return new NumericExpr() {
					@Override
					public float evaluate(Map<String, Float> args) {
						final Float value = args.get(symbol);
						return value != null? value : 0;
					}
				};
			}
		}
	}

	private static class NodeOpCall extends SymbolNodeOp {
		public NodeOpCall(String symbol) {
			super(symbol);
		}

		@Override
		public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
			final ExprFactory maybeMacro = scope.get(symbol);
			if (maybeMacro != null) {
				return maybeMacro.createNumericExpr(children, scope);
			} else {
				throw new IllegalArgumentException("Unknown macro: " + symbol);
			}
		}
	}

	private static class NumericConstExpr extends NumericExpr {
		private final Optional<Float> maybeValue;
		private final float value;

		private NumericConstExpr(float value) {
			this.value = value;
			this.maybeValue = Optional.of(value);
		}

		@Override
		public float evaluate(Map<String, Float> args) {
			return value;
		}

		@Override
		public Optional<Float> getConstValue() {
			return maybeValue;
		}
	}

	private static BooleanExpr EXPR_TRUE = new BooleanConstExpr(true);
	private static BooleanExpr EXPR_FALSE = new BooleanConstExpr(false);

	private static class BooleanConstExpr extends BooleanExpr {
		private final Optional<Boolean> maybeValue;
		private final boolean value;

		private BooleanConstExpr(boolean value) {
			this.value = value;
			this.maybeValue = Optional.of(value);
		}

		@Override
		public boolean evaluate(Map<String, Float> args) {
			return value;
		}

		@Override
		public Optional<Boolean> getConstValue() {
			return maybeValue;
		}

		public static BooleanExpr valueOf(boolean value) {
			return value? EXPR_TRUE : EXPR_FALSE;
		}
	}

	private static class BooleanNotExpr extends BooleanExpr {
		private final BooleanExpr arg;

		private BooleanNotExpr(BooleanExpr arg) {
			this.arg = arg;
		}

		@Override
		public boolean evaluate(Map<String, Float> args) {
			return !arg.evaluate(args);
		}
	}

	private abstract static class ConstantNodeOp implements NodeOp {

		private final String stringValue;

		public ConstantNodeOp(String stringValue) {
			this.stringValue = stringValue;
		}

		@Override
		public String toString(List<Node> children) {
			if (children.isEmpty()) {
				return stringValue;
			} else {
				return stringValue + '?' + children.toString();
			}
		}

	}

	private static NodeOp createConstNodeOp(float value) {
		final NumericExpr expr = new NumericConstExpr(value);
		return new ConstantNodeOp(Float.toString(value)) {
			@Override
			public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
				Preconditions.checkState(children.isEmpty(), "Cannot call constant");
				return expr;
			}

			@Override
			public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
				return noBooleanValue();
			}

		};
	}

	private static NodeOp createConstNode(boolean value) {
		final BooleanExpr expr = BooleanConstExpr.valueOf(value);
		return new ConstantNodeOp(Boolean.toString(value)) {
			@Override
			public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
				Preconditions.checkState(children.isEmpty(), "Cannot call constant");
				return expr;
			}

			@Override
			public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
				return noNumericValue();
			}
		};
	}

	private static class Node {

		private final NodeOp op;

		private final List<Node> children;

		public Node(NodeOp op) {
			this.op = op;
			this.children = ImmutableList.of();
		}

		public Node(NodeOp op, List<Node> children) {
			this.op = op;
			this.children = ImmutableList.copyOf(children);
		}

		@Override
		public String toString() {
			return op.toString(children);
		}

		public NumericExpr createNumericExprFromNode(Scope scope) {
			return op.createNumericExpr(children, scope);
		}

		public BooleanExpr createBooleanExprFromNode(Scope scope) {
			return op.createBooleanExpr(children, scope);
		}
	}

	private static final DoubleParser NUMER_PARSER = new DoubleParser();

	private static final INodeFactory<Node, Operator> NODE_FACTORY = new INodeFactory<Node, Operator>() {

		@Override
		public Node createBracketNode(String openingBracket, String closingBracket, List<Node> children) {
			Preconditions.checkState(children.size() == 1, "Invalid number of elements in bracket");
			return children.get(0);
		}

		@Override
		public Node createOpNode(Operator op, List<Node> children) {
			return new Node(op, children);
		}

		@Override
		public Node createSymbolGetNode(String id) {
			return new Node(new NodeOpGet(id));
		}

		@Override
		public Node createValueNode(Token token) {
			final Double value = NUMER_PARSER.parseToken(token);
			return new Node(createConstNodeOp(value.floatValue()));
		}
	};

	private static final InfixParser<Node, Operator> PARSER = new InfixParser<>(OPERATORS, NODE_FACTORY);

	private final IParserState<Node> parserState = new IParserState<Node>() {

		@Override
		public IAstParser<Node> getParser() {
			return PARSER;
		}

		@Override
		public ISymbolCallStateTransition<Node> getStateForSymbolCall(final String symbol) {
			return new SameStateSymbolTransition<Node>(this) {
				@Override
				public Node createRootNode(List<Node> children) {
					return new Node(new NodeOpCall(symbol), children);
				}
			};
		}

		@Override
		public IModifierStateTransition<Node> getStateForModifier(String modifier) {
			if (MODIFIER_OP.equals(modifier)) {
				return new SingleStateTransition.ForModifier<Node>() {

					@Override
					public Node createRootNode(Node child) {
						return child;
					}

					@Override
					public Node parseSymbol(IParserState<Node> state, PeekingIterator<Token> input) {
						Preconditions.checkState(input.hasNext(), "Unexpected end out input");
						final Token token = input.next();
						Preconditions.checkState(token.type == TokenType.OPERATOR, "Unexpected token, expected operator, got %s", token);
						NodeOp operator = OPERATORS.getOperator(token.value, OperatorArity.BINARY);
						if (operator == null) operator = OPERATORS.getOperator(token.value, OperatorArity.UNARY);
						if (operator == null) throw new IllegalArgumentException("Unknown operator: " + token.value);
						return new Node(operator);
					}

				};
			} else {
				throw new UnsupportedOperationException("Modifier: " + modifier);
			}
		}
	};

	private Node parseExpression(PeekingIterator<Token> tokens) {
		return parserState.getParser().parse(parserState, tokens);
	}

	private static final Tokenizer TOKENIZER = new Tokenizer();

	static {
		TOKENIZER.addOperator(OPERATOR_ASSIGN);
		TOKENIZER.addOperator(OPERATOR_ADD);
		TOKENIZER.addOperator(OPERATOR_SUBTRACT);
		TOKENIZER.addOperator(OPERATOR_DIVIDE);
		TOKENIZER.addOperator(OPERATOR_MULTIPLY);
		TOKENIZER.addOperator(OPERATOR_MOD);
		TOKENIZER.addOperator(OPERATOR_POWER);

		TOKENIZER.addOperator(OPERATOR_GE);
		TOKENIZER.addOperator(OPERATOR_GT);
		TOKENIZER.addOperator(OPERATOR_LE);
		TOKENIZER.addOperator(OPERATOR_LT);
		TOKENIZER.addOperator(OPERATOR_EQ);
		TOKENIZER.addOperator(OPERATOR_NE);

		TOKENIZER.addOperator(OPERATOR_AND);
		TOKENIZER.addOperator(OPERATOR_OR);
		TOKENIZER.addOperator(OPERATOR_XOR);
		TOKENIZER.addOperator(OPERATOR_IFF);
		TOKENIZER.addOperator(OPERATOR_NOT);

		TOKENIZER.addModifier(MODIFIER_OP);
	}

	@FunctionalInterface
	public interface IClipProvider {
		public Optional<? extends IClip> get(String name);
	}

	private static interface ITransformExecutor {
		public TRSRTransformation apply(TRSRTransformation initial, IJoint joint, Map<String, Float> args);
	}

	private static final ITransformExecutor EMPTY_TRANSFORM_EXECUTOR = new ITransformExecutor() {
		@Override
		public TRSRTransformation apply(TRSRTransformation initial, IJoint joint, Map<String, Float> args) {
			return initial;
		}
	};

	private static interface IValueExecutor {
		public void apply(Map<String, Float> args);
	}

	private static final IValueExecutor EMPTY_VALUE_EXECUTOR = new IValueExecutor() {
		@Override
		public void apply(Map<String, Float> args) {}
	};

	private interface IStatement {
		public ITransformExecutor bind(IClipProvider provider);

		public IValueExecutor free();
	}

	private static class AssignStatement implements IStatement {
		private final String name;
		private final NumericExpr value;

		public AssignStatement(String name, NumericExpr value) {
			this.name = name;
			this.value = value;
		}

		private void eval(Map<String, Float> args) {
			final Float v = value.evaluate(args);
			args.put(name, v);
		}

		@Override
		public ITransformExecutor bind(IClipProvider provider) {
			return new ITransformExecutor() {
				@Override
				public TRSRTransformation apply(TRSRTransformation initial, IJoint joint, Map<String, Float> args) {
					eval(args);
					return initial;
				}
			};
		}

		@Override
		public IValueExecutor free() {
			return new IValueExecutor() {
				@Override
				public void apply(Map<String, Float> args) {
					eval(args);
				}
			};
		}
	}

	private static class ClipStatement implements IStatement {

		private final String clipName;
		private final NumericExpr param;

		public ClipStatement(String clipName, NumericExpr param) {
			this.clipName = clipName;
			this.param = param;
		}

		@Override
		public ITransformExecutor bind(IClipProvider provider) {
			final Optional<? extends IClip> clip = provider.get(clipName);
			Preconditions.checkState(clip.isPresent(), "Can't find clip '%s'", clipName);
			return createForClip(clip.get(), param);
		}

		@Override
		public IValueExecutor free() {
			throw new UnsupportedOperationException("Clip cannot be applied in this context");
		}

	}

	private static class Macro implements ExprFactory {
		public final String name;
		public final List<String> args;
		public final Node body;
		public final Scope defineSiteScope;

		public Macro(String name, List<String> args, Node node, Scope scope) {
			this.name = name;
			this.args = ImmutableList.copyOf(args);
			this.body = node;
			this.defineSiteScope = scope;
		}

		@Override
		public NumericExpr createNumericExpr(List<Node> children, final Scope scope) {
			final Map<String, ExprFactory> defineScopePatch = prepareScope(children, scope);
			return body.createNumericExprFromNode(defineSiteScope.expand(defineScopePatch));
		}

		@Override
		public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
			final Map<String, ExprFactory> defineScopePatch = prepareScope(children, scope);
			return body.createBooleanExprFromNode(defineSiteScope.expand(defineScopePatch));
		}

		private Map<String, ExprFactory> prepareScope(List<Node> children, final Scope callsiteScope) {
			final int expectedArgCount = args.size();
			final int actualArgCount = children.size();
			Preconditions.checkState(actualArgCount == expectedArgCount, "Invalid number of args: expected %s, got %s", expectedArgCount, actualArgCount);

			final Map<String, ExprFactory> defineScopePatch = Maps.newHashMap();
			defineScopePatch.put(name, this); // for recursion

			for (int i = 0; i < expectedArgCount; i++) {
				final String argName = args.get(i);
				final Node argNode = children.get(i);
				if (argNode.children.isEmpty()) {
					// no children = we substitute local children, to allow high-order macros
					// newly placed macro should be taken from macro callsite (since it's parameter evaluation site),
					// but children should be evaluated with scope from place of substitution
					defineScopePatch.put(argName, new ExprFactory() {
						private List<Node> rescopeChildren(List<Node> children, Scope scope) {
							final List<Node> rescopedChildren = Lists.newArrayList();
							for (Node child : children) {
								rescopedChildren.add(bindNodeOpNodeToScope(child, scope));
							}
							return rescopedChildren;
						}

						@Override
						public NumericExpr createNumericExpr(List<Node> children, Scope localScope) {
							final List<Node> rescopedChildren = rescopeChildren(children, localScope);
							return argNode.op.createNumericExpr(rescopedChildren, callsiteScope);
						}

						@Override
						public BooleanExpr createBooleanExpr(List<Node> children, Scope localScope) {
							final List<Node> rescopedChildren = rescopeChildren(children, localScope);
							return argNode.op.createBooleanExpr(rescopedChildren, callsiteScope);
						}
					});
				} else {
					defineScopePatch.put(argName, new NumericExprFactory() {
						private NumericExpr numericExpr;

						private BooleanExpr booleanExpr;

						@Override
						public NumericExpr createNumericExpr(List<Node> children, Scope localScope) {
							if (numericExpr == null) numericExpr = argNode.createNumericExprFromNode(callsiteScope);
							return numericExpr;
						}

						@Override
						public BooleanExpr createBooleanExpr(List<Node> children, Scope localScope) {
							if (booleanExpr == null) booleanExpr = argNode.createBooleanExprFromNode(callsiteScope);
							return booleanExpr;
						}
					});
				}

			}
			return defineScopePatch;
		}

		@Override
		public String toString() {
			return name + args.toString() + ":=" + body.toString();
		}
	}

	private abstract static class SimpleNumericNodeOp extends NumericExprFactory {

		@Override
		public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
			validateArgs(children);

			final ImmutableList.Builder<NumericExpr> argsBuilder = ImmutableList.builder();
			for (Node child : children)
				argsBuilder.add(child.createNumericExprFromNode(scope));

			return createExpr(argsBuilder.build());
		}

		protected abstract void validateArgs(List<Node> args);

		protected abstract NumericExpr createExpr(List<NumericExpr> args);
	}

	private abstract static class Function extends SimpleNumericNodeOp {

		@Override
		protected NumericExpr createExpr(final List<NumericExpr> args) {
			return new NumericExpr() {
				@Override
				public float evaluate(Map<String, Float> vars) {
					return Function.this.evaluate(vars, args);
				}
			};
		}

		protected abstract float evaluate(Map<String, Float> vars, List<NumericExpr> args);
	}

	private abstract static class UnaryFunction extends Function {
		@Override
		protected void validateArgs(List<Node> args) {
			final int argCount = args.size();
			Preconditions.checkArgument(argCount == 1, "Invalid number of args, expected 1, got %s", argCount);
		}

		@Override
		protected float evaluate(Map<String, Float> vars, List<NumericExpr> args) {
			final NumericExpr argExpr = args.get(0);
			final float arg = argExpr.evaluate(vars);
			return evaluate(arg);
		}

		protected abstract float evaluate(float arg);
	}

	private abstract static class BinaryFunction extends Function {

		@Override
		protected void validateArgs(List<Node> args) {
			final int argCount = args.size();
			Preconditions.checkArgument(argCount == 2, "Invalid number of args, expected 2, got %s", argCount);
		}

		@Override
		protected float evaluate(Map<String, Float> vars, List<NumericExpr> args) {
			final NumericExpr leftExpr = args.get(0);
			final float leftArg = leftExpr.evaluate(vars);

			final NumericExpr rightExpr = args.get(1);
			final float rightArg = rightExpr.evaluate(vars);
			return evaluate(leftArg, rightArg);
		}

		protected abstract float evaluate(float leftArg, float rightArg);
	}

	private abstract static class AggregateFunction extends SimpleNumericNodeOp {
		@Override
		protected void validateArgs(List<Node> args) {
			Preconditions.checkArgument(args.size() > 0, "Expected at least one arg");
		}

		@Override
		protected NumericExpr createExpr(List<NumericExpr> args) {
			if (args.size() == 1) {
				return args.get(0);
			} else if (args.size() == 2) {
				final NumericExpr left = args.get(0);
				final NumericExpr right = args.get(1);
				return new NumericExpr() {
					@Override
					public float evaluate(Map<String, Float> args) {
						final float leftValue = left.evaluate(args);
						final float rightValue = right.evaluate(args);
						return AggregateFunction.this.evaluate(leftValue, rightValue);
					}
				};
			} else {
				final NumericExpr head = args.get(0);
				final List<NumericExpr> tail = args.subList(1, args.size());
				return new NumericExpr() {
					@Override
					public float evaluate(Map<String, Float> args) {
						float result = head.evaluate(args);
						for (NumericExpr e : tail) {
							final float val = e.evaluate(args);
							result = AggregateFunction.this.evaluate(result, val);
						}

						return result;
					}
				};
			}
		}

		protected abstract float evaluate(float accumulator, float arg);
	}

	private static final Map<String, ExprFactory> BUILTINS;

	static {
		final ImmutableMap.Builder<String, ExprFactory> builder = ImmutableMap.builder();
		builder.put("PI", createConstNodeOp((float)Math.PI));
		builder.put("E", createConstNodeOp((float)Math.E));

		builder.put("true", createConstNode(true));
		builder.put("false", createConstNode(false));

		builder.put("abs", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return Math.abs(arg);
			}
		});
		builder.put("sin", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.sin(arg);
			}
		});
		builder.put("cos", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.cos(arg);
			}
		});
		builder.put("tan", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.tan(arg);
			}
		});
		builder.put("asin", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.asin(arg);
			}
		});
		builder.put("acos", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.acos(arg);
			}
		});
		builder.put("atan", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.atan(arg);
			}
		});
		builder.put("sinh", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.sinh(arg);
			}
		});
		builder.put("cosh", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.cosh(arg);
			}
		});
		builder.put("exp", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.exp(arg);
			}
		});
		builder.put("expm1", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.expm1(arg);
			}
		});
		builder.put("log", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.log(arg);
			}
		});
		builder.put("log2", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)DoubleMath.log2(arg);
			}
		});
		builder.put("logp1", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.log1p(arg);
			}
		});
		builder.put("log10", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.log10(arg);
			}
		});
		builder.put("floor", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.floor(arg);
			}
		});
		builder.put("ceil", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.ceil(arg);
			}
		});
		builder.put("round", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.rint(arg);
			}
		});
		builder.put("trunc", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				if (Float.isInfinite(arg) || Float.isNaN(arg)) return arg;
				return (int)(arg);
			}
		});
		builder.put("sgn", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return Math.signum(arg);
			}
		});
		builder.put("sqrt", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.sqrt(arg);
			}
		});
		builder.put("deg", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.toDegrees(arg);
			}
		});
		builder.put("rad", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.toRadians(arg);
			}
		});
		builder.put("wrap_deg", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return MathHelper.wrapDegrees(arg);
			}
		});

		builder.put("atan2", new BinaryFunction() {
			@Override
			protected float evaluate(float leftArg, float rightArg) {
				return (float)Math.atan2(leftArg, rightArg);
			}
		});
		builder.put("hypot", new BinaryFunction() {
			@Override
			protected float evaluate(float leftArg, float rightArg) {
				return (float)Math.hypot(leftArg, rightArg);
			}
		});

		builder.put("max", new AggregateFunction() {
			@Override
			protected float evaluate(float leftArg, float rightArg) {
				return Math.max(leftArg, rightArg);
			}
		});
		builder.put("min", new AggregateFunction() {
			@Override
			protected float evaluate(float leftArg, float rightArg) {
				return Math.min(leftArg, rightArg);
			}
		});

		builder.put("if", new ExprFactory() {
			@Override
			public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
				Preconditions.checkArgument(children.size() == 3, "Expected 3 arg for 'if'");
				final BooleanExpr cond = children.get(0).createBooleanExprFromNode(scope);
				final Node ifTrueNode = children.get(1);
				final Node ifFalseNode = children.get(2);

				final Optional<Boolean> constCond = cond.getConstValue();

				if (constCond.isPresent()) {
					return (constCond.get()? ifTrueNode : ifFalseNode).createNumericExprFromNode(scope);
				} else {
					final NumericExpr ifTrue = ifTrueNode.createNumericExprFromNode(scope);
					final NumericExpr ifFalse = ifFalseNode.createNumericExprFromNode(scope);

					return new NumericExpr() {
						@Override
						public float evaluate(Map<String, Float> args) {
							final boolean selector = cond.evaluate(args);
							return (selector? ifTrue : ifFalse).evaluate(args);
						}
					};
				}
			}

			@Override
			public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
				Preconditions.checkArgument(children.size() == 3, "Expected 3 arg for 'if'");
				final BooleanExpr cond = children.get(0).createBooleanExprFromNode(scope);
				final Node ifTrueNode = children.get(1);
				final Node ifFalseNode = children.get(2);

				final Optional<Boolean> constCond = cond.getConstValue();

				if (constCond.isPresent()) {
					return (constCond.get()? ifTrueNode : ifFalseNode).createBooleanExprFromNode(scope);
				} else {
					final BooleanExpr ifTrue = ifTrueNode.createBooleanExprFromNode(scope);
					final BooleanExpr ifFalse = ifFalseNode.createBooleanExprFromNode(scope);

					return new BooleanExpr() {
						@Override
						public boolean evaluate(Map<String, Float> args) {
							final boolean selector = cond.evaluate(args);
							return (selector? ifTrue : ifFalse).evaluate(args);
						}
					};
				}
			}
		});

		builder.put("bool", new BooleanExprFactory() {
			@Override
			public BooleanExpr createBooleanExpr(List<Node> children, Scope scope) {
				Preconditions.checkArgument(children.size() == 1, "Expected single arg for 'bool'");
				final NumericExpr arg = children.get(0).createNumericExprFromNode(scope);
				return new BooleanExpr() {
					@Override
					public boolean evaluate(Map<String, Float> args) {
						final float value = arg.evaluate(args);
						return value == 0? false : true;
					}
				};
			}
		});

		builder.put("number", new NumericExprFactory() {
			@Override
			public NumericExpr createNumericExpr(List<Node> children, Scope scope) {
				Preconditions.checkArgument(children.size() == 1, "Expected single arg for 'number'");
				final BooleanExpr arg = children.get(0).createBooleanExprFromNode(scope);
				return new NumericExpr() {
					@Override
					public float evaluate(Map<String, Float> args) {
						final boolean value = arg.evaluate(args);
						return value? 1 : 0;
					}
				};
			}
		});

		BUILTINS = builder.build();
	}

	private final Map<String, ExprFactory> globalScope = Maps.newHashMap(BUILTINS);

	private final List<IStatement> statements = Lists.newArrayList();

	public void appendStatement(String statement) {
		try {
			final TokenIterator tokens = TOKENIZER.tokenize(statement);
			final Node node = parseExpression(tokens);

			if (node.op == OP_ASSIGN) {
				Preconditions.checkState(node.children.size() == 2);
				final Node left = node.children.get(0);
				final Node right = node.children.get(1);
				if (left.op instanceof NodeOpGet) {
					final String key = ((NodeOpGet)left.op).symbol;
					final NumericExpr arg = right.createNumericExprFromNode(new Scope(globalScope));
					statements.add(new AssignStatement(key, arg));
				} else if (left.op instanceof NodeOpCall) {
					final String key = ((NodeOpCall)left.op).symbol;
					final List<String> args = Lists.newArrayList();
					for (Node argNode : left.children) {
						Preconditions.checkState(argNode.op instanceof NodeOpGet, "Only single symbols allowed as macro args");
						final String argName = ((NodeOpGet)argNode.op).symbol;
						args.add(argName);
					}

					//
					globalScope.put(key, new Macro(key, args, right, new Scope(globalScope)));
				} else {
					throw new UnsupportedOperationException("Expected single symbol or symbol call on left side of assignment");
				}
			} else if (node.op instanceof NodeOpCall) {
				Preconditions.checkState(node.children.size() == 1, "Invalid number of arguments for clip application");
				final Node arg = node.children.get(0);
				final String key = ((NodeOpCall)node.op).symbol;
				final NumericExpr argExpr = arg.createNumericExprFromNode(new Scope(globalScope));
				statements.add(new ClipStatement(key, argExpr));
			} else {
				throw new UnsupportedOperationException("Only statements in form 'clip(<expr>, ...)' or `value := <expr>` allowed");
			}

		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to parse: " + statement, e);
		}
	}

	private static ITransformExecutor composeTransformExecutors(List<ITransformExecutor> contents) {
		if (contents.isEmpty()) return EMPTY_TRANSFORM_EXECUTOR;
		if (contents.size() == 1)
			return contents.get(0);

		final List<ITransformExecutor> executors = ImmutableList.copyOf(contents);
		return new ITransformExecutor() {

			@Override
			public TRSRTransformation apply(TRSRTransformation initial, IJoint joint, Map<String, Float> args) {
				TRSRTransformation result = initial;
				for (ITransformExecutor e : executors)
					result = e.apply(result, joint, args);

				return result;
			}
		};
	}

	private static IValueExecutor composeValueExecutors(List<IValueExecutor> contents) {
		if (contents.isEmpty()) return EMPTY_VALUE_EXECUTOR;
		if (contents.size() == 1)
			return contents.get(0);

		final List<IValueExecutor> executors = ImmutableList.copyOf(contents);
		return new IValueExecutor() {
			@Override
			public void apply(Map<String, Float> args) {
				for (IValueExecutor executors : executors)
					executors.apply(args);
			}
		};
	}

	private static ITransformExecutor createForClip(final IClip clip, final NumericExpr param) {
		return new ITransformExecutor() {
			@Override
			public TRSRTransformation apply(TRSRTransformation initial, IJoint joint, Map<String, Float> args) {
				final float paramValue = param.evaluate(args);
				final TRSRTransformation clipTransform = clip.apply(joint).apply(paramValue);
				return initial.compose(clipTransform);
			}
		};
	}

	private static class EvaluatorImpl implements ITransformEvaluator {

		private final ITransformExecutor executor;

		public EvaluatorImpl(ITransformExecutor executor) {
			this.executor = executor;
		}

		@Override
		public TRSRTransformation evaluate(IJoint joint, Map<String, Float> args) {
			final Map<String, Float> mutableArgs = Maps.newHashMap(args);
			return executor.apply(TRSRTransformation.identity(), joint, mutableArgs);
		}
	}

	public ITransformEvaluator createEvaluator(IClipProvider provider) {
		if (statements.isEmpty())
			return (joint, args) -> TRSRTransformation.identity();

		final List<ITransformExecutor> executors = Lists.newArrayList();

		for (IStatement statement : statements)
			executors.add(statement.bind(provider));

		return new EvaluatorImpl(composeTransformExecutors(executors));
	}

	private static class ExpanderImpl implements IVarExpander {

		private final IValueExecutor executor;

		public ExpanderImpl(IValueExecutor executor) {
			this.executor = executor;
		}

		@Override
		public Map<String, Float> expand(Map<String, Float> args) {
			final Map<String, Float> mutableArgs = Maps.newHashMap(args);
			executor.apply(mutableArgs);
			return mutableArgs;
		}

	}

	public IVarExpander createExpander() {
		if (statements.isEmpty())
			return args -> args;

		final List<IValueExecutor> executors = Lists.newArrayList();

		for (IStatement statement : statements)
			executors.add(statement.free());

		return new ExpanderImpl(composeValueExecutors(executors));
	}

}
