package openmods.model.variant;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.PeekingIterator;
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
import info.openmods.calc.parsing.token.Token;
import info.openmods.calc.parsing.token.TokenType;
import info.openmods.calc.parsing.token.Tokenizer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Evaluator {

	private static final String MODIFIER_ASSIGN = ":=";

	private static final int PRIORITY_DOT = 5; // .
	private static final String OPERATOR_DOT = ".";

	private static final int PRIORITY_NOT = 4; // .
	private static final String OPERATOR_NOT = "!";

	private static final int PRIORITY_AND = 3; // &
	private static final String OPERATOR_AND = "&";

	private static final int PRIORITY_OR = 2; // |
	private static final String OPERATOR_OR = "|";

	private static final int PRIORITY_COMPARE = 1; // ^, =
	private static final String OPERATOR_XOR = "^";
	private static final String OPERATOR_EQ = "=";

	private static final Tokenizer tokenizer = new Tokenizer();

	static {
		tokenizer.addModifier(MODIFIER_ASSIGN);
		tokenizer.addOperator(OPERATOR_DOT);
		tokenizer.addOperator(OPERATOR_OR);
		tokenizer.addOperator(OPERATOR_AND);
		tokenizer.addOperator(OPERATOR_XOR);
		tokenizer.addOperator(OPERATOR_EQ);
		tokenizer.addOperator(OPERATOR_NOT);
	}

	private static String expectToken(Iterator<Token> tokens, TokenType type) {
		Preconditions.checkState(tokens.hasNext(), "Expected %s, got end of statement", type);
		Token result = tokens.next();
		Preconditions.checkState(result.type == type, "Expect %s, got %s", type, result);
		return result.value;
	}

	private static void expectToken(Iterator<Token> tokens, TokenType type, String value) {
		Preconditions.checkState(tokens.hasNext(), "Expected %s, got end of statement", type);
		Token result = tokens.next();
		Preconditions.checkState(result.type == type && result.value.equals(value), "Expect %s:%s, got %s", type, value, result);
	}

	private static Token expectTokens(Iterator<Token> tokens, TokenType... types) {
		Preconditions.checkState(tokens.hasNext(), "Expected %s, got end of statement", Arrays.toString(types));
		Token result = tokens.next();
		Preconditions.checkState(ImmutableSet.of(types).contains(types), "Expect %s, got %s", Arrays.toString(types), result);
		return result;
	}

	private abstract static class Operator implements IOperator<Operator> {

		private final String id;

		public final int precedence;

		public Operator(String id, int precedence) {
			this.id = id;
			this.precedence = precedence;
		}

		public abstract IExpr createNode(List<IExpr> children);

		@Override
		public String id() {
			return id;
		}

		@Override
		public boolean isLowerPriority(Operator other) {
			return precedence <= other.precedence; // all binary operators are left associative here
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

		protected abstract IExpr createNode(IExpr value);

		@Override
		public IExpr createNode(List<IExpr> children) {
			Preconditions.checkArgument(children.size() == 1, "Invalid arguments for unary operator %s", id());
			return createNode(children.get(0));
		}
	}

	private abstract static class BinaryOperator extends Operator {

		public BinaryOperator(String id, int precedence) {
			super(id, precedence);

		}

		@Override
		public OperatorArity arity() {
			return OperatorArity.BINARY;
		}

		protected abstract IExpr createNode(IExpr left, IExpr right);

		@Override
		public IExpr createNode(List<IExpr> children) {
			Preconditions.checkArgument(children.size() == 2, "Invalid arguments for binary operator %s", id());
			return createNode(children.get(0), children.get(1));
		}
	}

	private static final OperatorDictionary<Operator> operators = new OperatorDictionary<Operator>();

	static {
		operators.registerOperator(new UnaryOperator(OPERATOR_NOT, PRIORITY_NOT) {
			@Override
			protected IExpr createNode(final IExpr value) {
				return new IExpr() {
					@Override
					public boolean evaluate(Map<String, String> vars) {
						return !value.evaluate(vars);
					}
				};
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_AND, PRIORITY_AND) {
			@Override
			protected IExpr createNode(final IExpr left, final IExpr right) {
				return new IExpr() {
					@Override
					public boolean evaluate(Map<String, String> vars) {
						return left.evaluate(vars) && right.evaluate(vars);
					}
				};
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_OR, PRIORITY_OR) {
			@Override
			protected IExpr createNode(final IExpr left, final IExpr right) {
				return new IExpr() {
					@Override
					public boolean evaluate(Map<String, String> vars) {
						return left.evaluate(vars) || right.evaluate(vars);
					}
				};
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_EQ, PRIORITY_COMPARE) {
			@Override
			protected IExpr createNode(final IExpr left, final IExpr right) {
				return new IExpr() {
					@Override
					public boolean evaluate(Map<String, String> vars) {
						return left.evaluate(vars) == right.evaluate(vars);
					}
				};
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_XOR, PRIORITY_COMPARE) {
			@Override
			protected IExpr createNode(final IExpr left, final IExpr right) {
				return new IExpr() {
					@Override
					public boolean evaluate(Map<String, String> vars) {
						return left.evaluate(vars) ^ right.evaluate(vars);
					}
				};
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_DOT, PRIORITY_DOT) {
			@Override
			protected IExpr createNode(final IExpr left, final IExpr right) {
				Preconditions.checkState(left instanceof KeyGet, "Expected symbol on left side of dot");
				Preconditions.checkState(right instanceof KeyGet, "Expected symbol on right side of dot");
				final String key = ((KeyGet)left).key;
				final String value = ((KeyGet)right).key;
				return new KeyValueGet(key, value);
			}
		});
	}

	private static interface IExpr {
		public boolean evaluate(Map<String, String> vars);
	}

	private static class FunctionCall implements IExpr {

		private final List<IExpr> args;

		private final Function function;

		public FunctionCall(Function function, List<IExpr> args) {
			final int expectedArgGot = function.args.size();
			final int providedArgCount = args.size();
			Preconditions.checkState(expectedArgGot == providedArgCount, "Invalid number of arguments to function %s, expected %s, got %s", expectedArgGot, providedArgCount);

			this.function = function;
			this.args = args;
		}

		@Override
		public boolean evaluate(Map<String, String> vars) {
			final List<Boolean> params = Lists.newArrayList();
			for (IExpr arg : this.args)
				params.add(arg.evaluate(vars));

			return function.execute(params);
		}
	}

	private static class KeyGet implements IExpr {
		private final String key;

		public KeyGet(String key) {
			this.key = key;
		}

		@Override
		public boolean evaluate(Map<String, String> vars) {
			return vars.containsKey(key);
		}
	}

	private static class KeyValueGet implements IExpr {
		private final String key;

		private final String value;

		public KeyValueGet(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public boolean evaluate(Map<String, String> vars) {
			final String value = vars.get(key);
			return Objects.equal(value, this.value);
		}
	}

	private static class SeparatorExpr implements IExpr {
		private final IExpr expr;

		public SeparatorExpr(IExpr expr) {
			this.expr = expr;
		}

		@Override
		public boolean evaluate(Map<String, String> vars) {
			return expr.evaluate(vars);
		}
	}

	private static final INodeFactory<IExpr, Operator> nodeFactory = new INodeFactory<IExpr, Operator>() {

		@Override
		public IExpr createBracketNode(String openingBracket, String closingBracket, List<IExpr> children) {
			Preconditions.checkState(children.size() == 1, "Invalid number of elements in bracket");
			return new SeparatorExpr(children.get(0));
		}

		@Override
		public IExpr createOpNode(Operator op, List<IExpr> children) {
			return op.createNode(children);
		}

		@Override
		public IExpr createSymbolGetNode(String id) {
			return new KeyGet(id);
		}

		@Override
		public IExpr createValueNode(Token token) {
			throw new UnsupportedOperationException();
		}
	};

	private static IExpr parseExpression(final Map<String, Function> functions, PeekingIterator<Token> tokens) {
		final InfixParser<IExpr, Operator> parser = new InfixParser<IExpr, Operator>(operators, nodeFactory);

		final IParserState<IExpr> parserState = new IParserState<IExpr>() {
			@Override
			public IAstParser<IExpr> getParser() {
				return parser;
			}

			@Override
			public IModifierStateTransition<IExpr> getStateForModifier(String modifier) {
				throw new UnsupportedOperationException();
			}

			@Override
			public ISymbolCallStateTransition<IExpr> getStateForSymbolCall(final String symbol) {
				return new SameStateSymbolTransition<IExpr>(this) {
					@Override
					public IExpr createRootNode(List<IExpr> children) {
						final Function function = functions.get(symbol);
						Preconditions.checkState(function != null, "Can't find function %s", symbol);
						return new FunctionCall(function, children);
					}
				};
			}
		};

		return parser.parse(parserState, tokens);
	}

	private static class Function {
		private final List<String> args;
		private final IExpr body;

		public Function(List<String> args, IExpr body) {
			this.args = args;
			this.body = body;
		}

		public boolean execute(List<Boolean> params) {
			final Map<String, String> env = Maps.newHashMap();
			for (int i = 0; i < args.size(); i++) {
				final String arg = args.get(i);
				final Boolean value = params.get(i);
				if (value == Boolean.TRUE) env.put(arg, VariantModelState.DEFAULT_MARKER); // TODO somehow pass whole values
			}

			return body.evaluate(env);
		}
	}

	private static Function parseFunction(Map<String, Function> functions, PeekingIterator<Token> tokens) {
		final List<String> args = Lists.newArrayList();

		while (true) {
			final String arg = expectToken(tokens, TokenType.SYMBOL);
			args.add(arg);
			final Token token = expectTokens(tokens, TokenType.SEPARATOR, TokenType.RIGHT_BRACKET);
			if (token.type == TokenType.RIGHT_BRACKET) {
				Preconditions.checkState(token.value.equals(")"), "Unexpected bracket: '%s'", token.value);
				break;
			}
		}

		expectToken(tokens, TokenType.MODIFIER, MODIFIER_ASSIGN);

		final IExpr body = parseExpression(functions, tokens);
		return new Function(args, body);
	}

	private static interface IStatement {
		public void execute(Map<String, String> env);
	}

	private abstract static class SetVar implements IStatement {
		private final IExpr expr;

		public SetVar(IExpr expr) {
			this.expr = expr;
		}

		@Override
		public void execute(Map<String, String> vars) {
			final boolean result = expr.evaluate(vars);
			setValue(result, vars);
		}

		protected abstract void setValue(boolean result, Map<String, String> vars);
	}

	private static class SetKeyOnlyVar extends SetVar {
		private final String key;

		public SetKeyOnlyVar(IExpr expr, String key) {
			super(expr);
			this.key = key;
		}

		@Override
		protected void setValue(boolean result, Map<String, String> vars) {
			if (result) {
				vars.put(key, VariantModelState.DEFAULT_MARKER);
			} else {
				vars.remove(key);
			}
		}
	}

	private static class SetKeyValueVar extends SetVar {
		private final String key;
		private final String value;

		public SetKeyValueVar(IExpr expr, String key, String value) {
			super(expr);
			this.key = key;
			this.value = value;
		}

		@Override
		protected void setValue(boolean result, Map<String, String> vars) {
			if (result) {
				vars.put(key, value);
			} else {
				final String value = vars.get(key);
				if (Objects.equal(this.value, value))
					vars.remove(key);
			}
		}
	}

	private final List<IStatement> program = Lists.newArrayList();

	private Map<String, Function> visibleFunctions = Maps.newHashMap();

	public void addStatement(String statement) {
		try {
			final PeekingIterator<Token> tokens = tokenizer.tokenize(statement);
			final String definedSymbol = expectToken(tokens, TokenType.SYMBOL);
			final Token token = expectTokens(tokens, TokenType.LEFT_BRACKET, TokenType.MODIFIER, TokenType.OPERATOR);

			if (token.type == TokenType.LEFT_BRACKET) {
				Preconditions.checkState(token.value.equals("("), "Invalid bracket: '%s'", token.value);
				visibleFunctions.put(definedSymbol, parseFunction(visibleFunctions, tokens));
			} else if (token.type == TokenType.OPERATOR) {
				Preconditions.checkState(token.value.equals(OPERATOR_DOT), "Invalid token: ", token);
				final String value = expectToken(tokens, TokenType.SYMBOL);
				expectToken(tokens, TokenType.MODIFIER, MODIFIER_ASSIGN);
				final IExpr expr = parseExpression(visibleFunctions, tokens);
				program.add(new SetKeyValueVar(expr, definedSymbol, value));
			} else if (token.type == TokenType.MODIFIER) {
				Preconditions.checkState(token.value.equals(MODIFIER_ASSIGN), "Invalid token: ", token);
				final IExpr expr = parseExpression(visibleFunctions, tokens);
				program.add(new SetKeyOnlyVar(expr, definedSymbol));
			} else {
				throw new IllegalArgumentException("Unexpected token: " + token);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to parse: " + statement, e);
		}
	}

	public void expandVars(Map<String, String> vars) {
		for (IStatement statement : program)
			statement.execute(vars);
	}
}
