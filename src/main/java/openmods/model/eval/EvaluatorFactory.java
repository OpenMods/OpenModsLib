package openmods.model.eval;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
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

	private static final int PRIORITY_ADD = 2;
	private static final String OPERATOR_ADD = "+";
	private static final String OPERATOR_SUBTRACT = "-";

	private static final int PRIORITY_ASSIGN = 1;
	private static final String OPERATOR_ASSIGN = ":=";

	private interface Expr {
		public float evaluate(Map<String, Float> args);
	}

	private interface NodeOp {}

	private interface ExprFactory extends NodeOp {
		public Expr createExpr(List<Node> children);
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
	}

	private static Expr createExprFromNode(Node node) {
		if (!(node.op instanceof ExprFactory)) throw new UnsupportedOperationException("Can't compile " + node.op.getClass().getSimpleName() + " to expression");
		return ((ExprFactory)node.op).createExpr(node.children);
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
		public Expr createExpr(List<Node> children) {
			Preconditions.checkState(children.size() == 1);
			final Expr arg = createExprFromNode(children.get(0));
			return new Expr() {
				@Override
				public float evaluate(Map<String, Float> args) {
					final float value = arg.evaluate(args);
					return apply(value);
				}
			};
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
		public Expr createExpr(List<Node> children) {
			Preconditions.checkState(children.size() == 2);
			final Expr leftArg = createExprFromNode(children.get(0));
			final Expr rightArg = createExprFromNode(children.get(1));
			return new Expr() {
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

		operators.registerOperator(new BinaryOperator(OPERATOR_POWER, PRIORITY_POWER) {
			@Override
			protected float apply(float left, float right) {
				return (float)Math.pow(left, right);
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_MULTIPLY, PRIORITY_MULTIPLY) {
			@Override
			protected float apply(float left, float right) {
				return left * right;
			}
		});
		operators.registerOperator(new BinaryOperator(OPERATOR_DIVIDE, PRIORITY_MULTIPLY) {
			@Override
			protected float apply(float left, float right) {
				return left / right;
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_ADD, PRIORITY_ADD) {
			@Override
			protected float apply(float left, float right) {
				return left + right;
			}
		});
		operators.registerOperator(new BinaryOperator(OPERATOR_SUBTRACT, PRIORITY_ADD) {
			@Override
			protected float apply(float left, float right) {
				return left - right;
			}
		});

		operators.registerOperator(OP_ASSIGN);
	}

	private static class SymbolNodeOp implements NodeOp {
		public final String symbol;

		public SymbolNodeOp(String symbol) {
			this.symbol = symbol;
		}
	}

	private static class NodeOpGet extends SymbolNodeOp implements ExprFactory {
		public NodeOpGet(String symbol) {
			super(symbol);
		}

		@Override
		public Expr createExpr(List<Node> children) {
			return new Expr() {
				@Override
				public float evaluate(Map<String, Float> args) {
					final Float value = args.get(symbol);
					return value != null? value : 0;
				}
			};
		}
	}

	private static class NodeOpCall extends SymbolNodeOp {
		public NodeOpCall(String symbol) {
			super(symbol);
		}
	}

	private static NodeOp createConstNode(final float value) {
		return new ExprFactory() {
			@Override
			public Expr createExpr(List<Node> children) {
				return new Expr() {
					@Override
					public float evaluate(Map<String, Float> args) {
						return value;
					}
				};
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

	private final List<IStatement> statements = Lists.newArrayList();

	public void appendStatement(String statement) {
		try {
			final TokenIterator tokens = tokenizer.tokenize(statement);
			final Node node = parseExpression(tokens);

			if (node.op == OP_ASSIGN) {
				Preconditions.checkState(node.children.size() == 2);
				final Node left = node.children.get(0);
				final Node right = node.children.get(1);
				Preconditions.checkState(left.op instanceof NodeOpGet, "Only symbols allowed on left side of assign");
				final String key = ((NodeOpGet)left.op).symbol;
				final Expr arg = createExprFromNode(right);
				statements.add(new AssignStatement(key, arg));
			} else if (node.op instanceof NodeOpCall) {
				Preconditions.checkState(node.children.size() == 1, "Invalid number of arguments for clip application");
				final Node arg = node.children.get(0);
				final String key = ((NodeOpCall)node.op).symbol;
				final Expr argExpr = createExprFromNode(arg);
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
