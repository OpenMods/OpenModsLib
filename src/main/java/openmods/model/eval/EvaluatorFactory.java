package openmods.model.eval;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
import info.openmods.calc.executable.OperatorDictionary;
import info.openmods.calc.parsing.ast.INodeFactory;
import info.openmods.calc.parsing.ast.IOperator;
import info.openmods.calc.parsing.ast.InfixParser;
import info.openmods.calc.parsing.ast.OperatorArity;
import info.openmods.calc.parsing.ast.SimpleParserState;
import info.openmods.calc.parsing.token.Token;
import info.openmods.calc.parsing.token.TokenIterator;
import info.openmods.calc.parsing.token.Tokenizer;
import info.openmods.calc.types.fp.DoubleParser;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IClip;
import net.minecraftforge.common.model.animation.IJoint;

public class EvaluatorFactory {

	private static final int PRIORITY_UNARY = 5;

	private static final int PRIORITY_POWER = 4;
	private static final String OPERATOR_POWER = "^";

	private static final int PRIORITY_MULTIPLY = 3;
	private static final String OPERATOR_MULTIPLY = "*";
	private static final String OPERATOR_DIVIDE = "/";
	private static final String OPERATOR_MOD = "%";

	private static final int PRIORITY_ADD = 2;
	private static final String OPERATOR_ADD = "+";
	private static final String OPERATOR_SUBTRACT = "-";

	private static final int PRIORITY_ASSIGN = 1;
	private static final String OPERATOR_ASSIGN = ":=";

	private abstract static class Expr {
		public abstract float evaluate(Map<String, Float> args);

		public Optional<Float> getConstValue() {
			return Optional.absent();
		}
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

	private interface NodeOp {
		public String toString(List<Node> children);
	}

	private interface ExprFactory extends NodeOp {
		public Expr createExpr(List<Node> children, Scope scope);
	}

	private abstract static class Operator implements IOperator<Operator>, ExprFactory {

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

	private static ExprFactory getExprFactoryFromNode(NodeOp nodeOp) {
		if (!(nodeOp instanceof ExprFactory)) throw new UnsupportedOperationException("Can't compile " + nodeOp.getClass().getSimpleName() + " to expression");
		return (ExprFactory)nodeOp;
	}

	private static Expr createExprFromNode(Node node, Scope scope) {
		return getExprFactoryFromNode(node.op).createExpr(node.children, scope);
	}

	private static Node bindExprFactoryNodeToScope(Node node, final Scope newScope) {
		if (node.op instanceof ExprFactory) {
			final ExprFactory childExprFactory = ((ExprFactory)node.op);
			return new Node(new ExprFactory() {

				@Override
				public Expr createExpr(List<Node> children, Scope scope) {
					return childExprFactory.createExpr(children, newScope);
				}

				@Override
				public String toString(List<Node> children) {
					return childExprFactory.toString(children);
				}
			}, node.children);
		} else {
			return node;
		}
	}

	private abstract static class UnaryOperator extends Operator {

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

		@Override
		public Expr createExpr(List<Node> children, Scope scope) {
			Preconditions.checkState(children.size() == 1);
			final Expr arg = createExprFromNode(children.get(0), scope);

			final Optional<Float> maybeConst = arg.getConstValue();

			if (maybeConst.isPresent()) {
				final float value = apply(maybeConst.get());
				return new ConstExpr(value);
			} else {
				return new Expr() {
					@Override
					public float evaluate(Map<String, Float> args) {
						final float value = arg.evaluate(args);
						return apply(value);
					}
				};
			}
		}

		protected abstract float apply(float value);
	}

	private abstract static class BinaryOperator extends Operator {

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

		@Override
		public Expr createExpr(List<Node> children, Scope scope) {
			Preconditions.checkState(children.size() == 2);
			final Expr leftArg = createExprFromNode(children.get(0), scope);
			final Optional<Float> maybeLeftConst = leftArg.getConstValue();

			final Expr rightArg = createExprFromNode(children.get(1), scope);
			final Optional<Float> maybeRightConst = rightArg.getConstValue();

			if (maybeLeftConst.isPresent()) {
				final float leftConst = maybeLeftConst.get();
				if (maybeRightConst.isPresent()) {
					final float rightConst = maybeRightConst.get();
					return bothConst(leftConst, rightConst);
				} else {
					return leftConst(leftConst, rightArg);
				}
			} else if (maybeRightConst.isPresent()) {
				final float rightConst = maybeRightConst.get();
				return rightConst(leftArg, rightConst);
			}

			return new Expr() {
				@Override
				public float evaluate(Map<String, Float> args) {
					final float leftValue = leftArg.evaluate(args);
					final float rightValue = rightArg.evaluate(args);
					return apply(leftValue, rightValue);
				}
			};
		}

		protected Expr bothConst(final float leftConst, final float rightConst) {
			final float value = apply(leftConst, rightConst);
			return new ConstExpr(value);
		}

		protected Expr rightConst(final Expr leftArg, final float rightConst) {
			return new Expr() {
				@Override
				public float evaluate(Map<String, Float> args) {
					final float leftValue = leftArg.evaluate(args);
					return apply(leftValue, rightConst);
				}
			};
		}

		protected Expr leftConst(final float leftConst, final Expr rightArg) {
			return new Expr() {
				@Override
				public float evaluate(Map<String, Float> args) {
					final float rightValue = rightArg.evaluate(args);
					return apply(leftConst, rightValue);
				}
			};
		}

		protected abstract float apply(float left, float right);
	}

	private abstract static class BinaryOperatorWithRightNeutralElement extends BinaryOperator {

		protected final float neutralElement;

		public BinaryOperatorWithRightNeutralElement(String id, int precedence, float neutralElement) {
			super(id, precedence);
			this.neutralElement = neutralElement;
		}

		@Override
		protected Expr rightConst(Expr leftArg, float rightConst) {
			if (rightConst == neutralElement) return leftArg;
			return super.rightConst(leftArg, rightConst);
		}
	}

	private abstract static class BinaryOperatorWithNeutralElement extends BinaryOperatorWithRightNeutralElement {

		public BinaryOperatorWithNeutralElement(String id, int precedence, float neutralElement) {
			super(id, precedence, neutralElement);
		}

		@Override
		protected Expr leftConst(float leftConst, Expr rightArg) {
			if (leftConst == neutralElement) return rightArg;
			return super.leftConst(leftConst, rightArg);
		}
	}

	private static final OperatorDictionary<Operator> operators = new OperatorDictionary<Operator>();

	private static final BinaryOperator OP_ASSIGN = new BinaryOperator(OPERATOR_ASSIGN, PRIORITY_ASSIGN) {
		@Override
		protected float apply(float left, float right) {
			throw new UnsupportedOperationException("Assign can only be used as top operator");
		}
	};

	static {
		operators.registerOperator(new UnaryOperator(OPERATOR_ADD, PRIORITY_UNARY) {
			@Override
			protected float apply(float value) {
				return +value;
			}
		});
		operators.registerOperator(new UnaryOperator(OPERATOR_SUBTRACT, PRIORITY_UNARY) {
			@Override
			protected float apply(float value) {
				return -value;
			}
		});

		operators.registerOperator(new BinaryOperatorWithRightNeutralElement(OPERATOR_POWER, PRIORITY_POWER, 1) {
			@Override
			protected Expr rightConst(Expr leftArg, float rightConst) {
				if (rightConst == 0) return new ConstExpr(1); // per docs, Math.pow(x, 0) == 1, even for inf and nan
				return super.rightConst(leftArg, rightConst);
			}

			@Override
			protected float apply(float left, float right) {
				return (float)Math.pow(left, right);
			}
		});

		operators.registerOperator(new BinaryOperatorWithNeutralElement(OPERATOR_MULTIPLY, PRIORITY_MULTIPLY, 1) {
			// not doing 0 * x == 0, since 0 * Infinity == NaN
			// even if 0 * 0, 0 * x and x * 0 are patched here, I can't patch x * y without performance loss

			@Override
			protected float apply(float left, float right) {
				return left * right;
			}
		});
		operators.registerOperator(new BinaryOperatorWithRightNeutralElement(OPERATOR_DIVIDE, PRIORITY_MULTIPLY, 1) {
			// same issue as multiplication, skipping 0 / x optimization
			@Override
			protected float apply(float left, float right) {
				return left / right;
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_MOD, PRIORITY_MULTIPLY) {
			@Override
			protected float apply(float left, float right) {
				return left % right;
			}
		});

		operators.registerOperator(new BinaryOperatorWithNeutralElement(OPERATOR_ADD, PRIORITY_ADD, 0) {
			@Override
			protected float apply(float left, float right) {
				return left + right;
			}
		});
		operators.registerOperator(new BinaryOperatorWithNeutralElement(OPERATOR_SUBTRACT, PRIORITY_ADD, 0) {
			@Override
			protected float apply(float left, float right) {
				return left - right;
			}
		});

		operators.registerOperator(OP_ASSIGN);
	}

	private abstract static class SymbolNodeOp implements ExprFactory {
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
	}

	private static class NodeOpGet extends SymbolNodeOp {
		public NodeOpGet(String symbol) {
			super(symbol);
		}

		@Override
		public Expr createExpr(List<Node> children, Scope scope) {
			final ExprFactory maybeMacro = scope.get(symbol);
			if (maybeMacro != null) {
				// may have children when placed via macro arg
				return maybeMacro.createExpr(children, scope);
			} else {
				return new Expr() {
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
		public Expr createExpr(List<Node> children, Scope scope) {
			final ExprFactory maybeMacro = scope.get(symbol);
			if (maybeMacro != null) {
				return maybeMacro.createExpr(children, scope);
			} else {
				throw new IllegalArgumentException("Unknown macro: " + symbol);
			}
		}
	}

	private static class ConstExpr extends Expr {
		private final Optional<Float> maybeValue;
		private final float value;

		private ConstExpr(float value) {
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

	private static ExprFactory createConstNode(final float value) {
		return new ExprFactory() {
			@Override
			public Expr createExpr(List<Node> children, Scope scope) {
				Preconditions.checkState(children.isEmpty(), "Cannot call constant");
				return new ConstExpr(value);
			}

			@Override
			public String toString(List<Node> children) {
				if (children.isEmpty()) {
					return Float.toString(value);
				} else {
					return Float.toString(value) + '?' + children.toString();
				}
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
	}

	private static final DoubleParser numberParser = new DoubleParser();

	private static final INodeFactory<Node, Operator> nodeFactory = new INodeFactory<Node, Operator>() {

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
			final Double value = numberParser.parseToken(token);
			return new Node(createConstNode(value.floatValue()));
		}
	};

	private static final InfixParser<Node, Operator> parser = new InfixParser<Node, Operator>(operators, nodeFactory);

	private final SimpleParserState<Node> parserState = new SimpleParserState<Node>(parser) {
		@Override
		protected Node createModifierNode(String modifier, Node child) {
			throw new UnsupportedOperationException("Modifier: " + modifier);
		}

		@Override
		protected Node createSymbolNode(String symbol, List<Node> children) {
			return new Node(new NodeOpCall(symbol), children);
		}
	};

	private Node parseExpression(PeekingIterator<Token> tokens) {
		return parserState.parse(tokens);
	}

	private static final Tokenizer tokenizer = new Tokenizer();

	static {
		tokenizer.addOperator(OPERATOR_ASSIGN);
		tokenizer.addOperator(OPERATOR_ADD);
		tokenizer.addOperator(OPERATOR_SUBTRACT);
		tokenizer.addOperator(OPERATOR_DIVIDE);
		tokenizer.addOperator(OPERATOR_MULTIPLY);
		tokenizer.addOperator(OPERATOR_MOD);
		tokenizer.addOperator(OPERATOR_POWER);
	}

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
		private final Expr value;

		public AssignStatement(String name, Expr value) {
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
		private final Expr param;

		public ClipStatement(String clipName, Expr param) {
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
		public Expr createExpr(List<Node> children, final Scope callsiteScope) {
			final int expectedArgCount = args.size();
			final int actualArgCount = children.size();
			Preconditions.checkState(actualArgCount == expectedArgCount, "Invalid number of args: expected %s, got %s", expectedArgCount, actualArgCount);

			final Map<String, ExprFactory> defineScopePatch = Maps.newHashMap();
			defineScopePatch.put(name, this); // for recursion

			for (int i = 0; i < expectedArgCount; i++) {
				final String argName = args.get(i);
				final Node argNode = children.get(i);
				if (argNode.children.isEmpty()) {
					// no children = we substitute local chilren, to allow high-order macros
					// newly placed macro should be taken from macro callsite (since it's parameter evaluation site),
					// but children should be evaluated with scope from place of substitution
					defineScopePatch.put(argName, new ExprFactory() {
						@Override
						public Expr createExpr(List<Node> children, final Scope localScope) {
							final List<Node> rescopedChildren = Lists.newArrayList();
							for (Node child : children) {
								rescopedChildren.add(bindExprFactoryNodeToScope(child, localScope));
							}

							return getExprFactoryFromNode(argNode.op).createExpr(rescopedChildren, callsiteScope);
						}

						@Override
						public String toString(List<Node> children) {
							return "<" + argNode.op.toString(children) + ">";
						}

					});
				} else {
					final Expr argExpr = createExprFromNode(argNode, callsiteScope);
					defineScopePatch.put(argName, new ExprFactory() {

						@Override
						public Expr createExpr(List<Node> children, Scope localScope) {
							return argExpr;
						}

						@Override
						public String toString(List<Node> children) {
							return "<" + argNode.toString() + ">";
						}
					});
				}

			}

			return createExprFromNode(body, defineSiteScope.expand(defineScopePatch));
		}

		@Override
		public String toString(List<Node> children) {
			return toString();
		}

		@Override
		public String toString() {
			return name + args.toString() + ":=" + body.toString();
		}
	}

	private abstract static class SimpleExprFactory implements ExprFactory {

		@Override
		public Expr createExpr(List<Node> children, Scope scope) {
			validateArgs(children);

			final ImmutableList.Builder<Expr> argsBuilder = ImmutableList.builder();
			for (Node child : children)
				argsBuilder.add(createExprFromNode(child, scope));

			return createExpr(argsBuilder.build());
		}

		protected abstract void validateArgs(List<Node> args);

		protected abstract Expr createExpr(List<Expr> args);

		@Override
		public String toString(List<Node> children) {
			return "<built-in>";
		}

	}

	private abstract static class Function extends SimpleExprFactory {

		@Override
		protected Expr createExpr(final List<Expr> args) {
			return new Expr() {
				@Override
				public float evaluate(Map<String, Float> vars) {
					return Function.this.evaluate(vars, args);
				}
			};
		}

		protected abstract float evaluate(Map<String, Float> vars, List<Expr> args);

		@Override
		public String toString(List<Node> children) {
			return "<built-in>";
		}

	}

	private abstract static class UnaryFunction extends Function {
		@Override
		protected void validateArgs(List<Node> args) {
			final int argCount = args.size();
			Preconditions.checkArgument(argCount == 1, "Invalid number of args, expected 1, got %s", argCount);
		}

		@Override
		protected float evaluate(Map<String, Float> vars, List<Expr> args) {
			final Expr argExpr = args.get(0);
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
		protected float evaluate(Map<String, Float> vars, List<Expr> args) {
			final Expr leftExpr = args.get(0);
			final float leftArg = leftExpr.evaluate(vars);

			final Expr rightExpr = args.get(1);
			final float rightArg = rightExpr.evaluate(vars);
			return evaluate(leftArg, rightArg);
		}

		protected abstract float evaluate(float leftArg, float rightArg);
	}

	private abstract static class AggregateFunction extends SimpleExprFactory {
		@Override
		protected void validateArgs(List<Node> args) {
			Preconditions.checkArgument(args.size() > 0, "Expected at least one arg");
		}

		@Override
		protected Expr createExpr(List<Expr> args) {
			if (args.size() == 1) {
				return args.get(0);
			} else if (args.size() == 2) {
				final Expr left = args.get(0);
				final Expr right = args.get(1);
				return new Expr() {
					@Override
					public float evaluate(Map<String, Float> args) {
						final float leftValue = left.evaluate(args);
						final float rightValue = right.evaluate(args);
						return AggregateFunction.this.evaluate(leftValue, rightValue);
					}
				};
			} else {
				final Expr head = args.get(0);
				final List<Expr> tail = args.subList(1, args.size());
				return new Expr() {
					@Override
					public float evaluate(Map<String, Float> args) {
						float result = head.evaluate(args);
						for (Expr e : tail) {
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

	private static final Map<String, ExprFactory> builtIns;

	static {
		final ImmutableMap.Builder<String, ExprFactory> builder = ImmutableMap.builder();
		builder.put("PI", createConstNode((float)Math.PI));
		builder.put("E", createConstNode((float)Math.E));
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
		builder.put("log", new UnaryFunction() {
			@Override
			protected float evaluate(float arg) {
				return (float)Math.log(arg);
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
				return Math.round(arg);
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

		builder.put("atan2", new BinaryFunction() {
			@Override
			protected float evaluate(float leftArg, float rightArg) {
				return (float)Math.atan2(leftArg, rightArg);
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

		builtIns = builder.build();
	}

	private final Map<String, ExprFactory> globalScope = Maps.newHashMap(builtIns);

	private final List<IStatement> statements = Lists.newArrayList();

	public void appendStatement(String statement) {
		try {
			final TokenIterator tokens = tokenizer.tokenize(statement);
			final Node node = parseExpression(tokens);

			if (node.op == OP_ASSIGN) {
				Preconditions.checkState(node.children.size() == 2);
				final Node left = node.children.get(0);
				final Node right = node.children.get(1);
				if (left.op instanceof NodeOpGet) {
					final String key = ((NodeOpGet)left.op).symbol;
					final Expr arg = createExprFromNode(right, new Scope(globalScope));
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
				final Expr argExpr = createExprFromNode(arg, new Scope(globalScope));
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

	private static ITransformExecutor createForClip(final IClip clip, final Expr param) {
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

	private static final ITransformEvaluator EMPTY_EVALUATOR = new ITransformEvaluator() {
		@Override
		public TRSRTransformation evaluate(IJoint joint, Map<String, Float> args) {
			return TRSRTransformation.identity();
		}
	};

	public ITransformEvaluator createEvaluator(IClipProvider provider) {
		if (statements.isEmpty())
			return EMPTY_EVALUATOR;

		final List<ITransformExecutor> executors = Lists.newArrayList();

		for (IStatement statement : statements)
			executors.add(statement.bind(provider));

		return new EvaluatorImpl(composeTransformExecutors(executors));
	}

	private static final IVarExpander EMPTY_EXPANDER = new IVarExpander() {
		@Override
		public Map<String, Float> expand(Map<String, Float> args) {
			return args;
		}
	};

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
			return EMPTY_EXPANDER;
		final List<IValueExecutor> executors = Lists.newArrayList();

		for (IStatement statement : statements)
			executors.add(statement.free());

		return new ExpanderImpl(composeValueExecutors(executors));
	}

}
