package openmods.model.variant;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
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

		@Override
		public boolean isLowerPriority(Operator other) {
			return precedence < other.precedence;
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

		@Override
		public boolean isLowerPriority(Operator other) {
			return precedence <= other.precedence; // all binary operators are left associative here
		}
	}

	private static final OperatorDictionary<Operator> operators = new OperatorDictionary<Operator>();

	private static abstract class Constant implements IExpr {

		protected abstract boolean value();

		@Override
		public boolean evaluate(Map<String, String> vars) {
			return value();
		}

		@Override
		public IExpr rebind(Map<String, IExpr> vars) {
			return this;
		}

		@Override
		public Optional<Boolean> getConstantValue() {
			return Optional.of(value());
		}

		@Override
		public IExpr fold() {
			return this;
		}

		@Override
		public boolean equals(IExpr other) {
			return other == this;
		}
	}

	private static final IExpr TRUE = new Constant() {
		@Override
		protected boolean value() {
			return true;
		}
	};

	private static final IExpr FALSE = new Constant() {
		@Override
		protected boolean value() {
			return false;
		}
	};

	public static IExpr constant(boolean value) {
		return value? TRUE : FALSE;
	}

	private static abstract class UnaryOperatorExpr implements IExpr {
		protected final IExpr value;

		private UnaryOperatorExpr(IExpr value) {
			this.value = value;
		}

		protected abstract IExpr create(IExpr value);

		@Override
		public IExpr rebind(Map<String, IExpr> vars) {
			return create(value.rebind(vars));
		}

		@Override
		public Optional<Boolean> getConstantValue() {
			return Optional.absent();
		}

		@Override
		public boolean equals(IExpr other) {
			if (this == other) return true;

			if (other.getClass() == this.getClass()) {
				final UnaryOperatorExpr otherOp = (UnaryOperatorExpr)other;
				return otherOp.value.equals(this.value);
			}

			return false;
		}
	}

	private static class OperatorNot extends UnaryOperatorExpr {
		private OperatorNot(IExpr value) {
			super(value);
		}

		@Override
		public boolean evaluate(Map<String, String> vars) {
			return !value.evaluate(vars);
		}

		@Override
		protected IExpr create(IExpr value) {
			return new OperatorNot(value);
		}

		@Override
		public IExpr fold() {
			final IExpr foldedArg = value.fold();

			if (foldedArg instanceof OperatorNot) return ((OperatorNot)value).value;

			final Optional<Boolean> argValue = foldedArg.getConstantValue();
			if (argValue.isPresent()) return constant(!argValue.get());

			return create(foldedArg);
		}
	}

	private static abstract class BinaryOperatorExpr implements IExpr {
		protected final IExpr left;
		protected final IExpr right;

		public BinaryOperatorExpr(IExpr left, IExpr right) {
			this.left = left;
			this.right = right;
		}

		protected abstract IExpr create(IExpr left, IExpr right);

		protected abstract boolean evaluate(boolean left, boolean right);

		protected abstract IExpr fold(boolean value, IExpr arg);

		protected abstract IExpr foldSameExpr(IExpr var);

		protected abstract IExpr foldLessSpecific(IVar lessSpecificVar, IVar moreSpecificVar);

		@Override
		public final boolean evaluate(Map<String, String> vars) {
			return evaluate(left.evaluate(vars), right.evaluate(vars));
		}

		@Override
		public final IExpr rebind(Map<String, IExpr> vars) {
			return create(left.rebind(vars), right.rebind(vars));
		}

		@Override
		public final IExpr fold() {
			final IExpr foldedLeft = left.fold();
			final IExpr foldedRight = right.fold();

			final Optional<Boolean> leftValue = foldedLeft.getConstantValue();
			final Optional<Boolean> rightValue = foldedRight.getConstantValue();

			if (leftValue.isPresent()) {
				if (rightValue.isPresent()) {
					return constant(evaluate(leftValue.get(), rightValue.get()));
				} else {
					return fold(leftValue.get(), foldedRight);
				}
			} else {
				if (rightValue.isPresent()) return fold(rightValue.get(), foldedLeft);
			}

			if (foldedLeft.equals(foldedRight)) return foldSameExpr(foldedLeft);

			if ((foldedLeft instanceof IVar) && (foldedRight instanceof IVar)) {
				final IVar leftVar = (IVar)foldedLeft;
				final IVar rightVar = (IVar)foldedRight;

				if (leftVar.isLessSpecific(rightVar))
					return foldLessSpecific(leftVar, rightVar);

				if (rightVar.isLessSpecific(leftVar))
					return foldLessSpecific(rightVar, leftVar);
			}

			return create(foldedLeft, foldedRight);
		}

		@Override
		public Optional<Boolean> getConstantValue() {
			return Optional.absent();
		}

		@Override
		public boolean equals(IExpr other) {
			if (this == other) return true;

			if (other.getClass() == this.getClass()) {
				final BinaryOperatorExpr otherOp = (BinaryOperatorExpr)other;
				return otherOp.left.equals(this.left) &&
						otherOp.right.equals(this.right);
			}

			return false;
		}
	}

	private static class AndOperator extends BinaryOperatorExpr {
		public AndOperator(IExpr left, IExpr right) {
			super(left, right);
		}

		@Override
		protected IExpr create(IExpr left, IExpr right) {
			return new AndOperator(left, right);
		}

		@Override
		protected IExpr fold(boolean arg, IExpr node) {
			return arg? node : FALSE;
		}

		@Override
		protected IExpr foldSameExpr(IExpr expr) {
			return expr;
		}

		@Override
		protected IExpr foldLessSpecific(IVar lessSpecificVar, IVar moreSpecificVar) {
			return moreSpecificVar;
		}

		@Override
		protected boolean evaluate(boolean left, boolean right) {
			return left && right;
		}

	}

	private static class OrOperator extends BinaryOperatorExpr {
		public OrOperator(IExpr left, IExpr right) {
			super(left, right);
		}

		@Override
		protected IExpr create(IExpr left, IExpr right) {
			return new OrOperator(left, right);
		}

		@Override
		protected IExpr fold(boolean arg, IExpr node) {
			return arg? TRUE : node;
		}

		@Override
		protected IExpr foldSameExpr(IExpr expr) {
			return expr;
		}

		@Override
		protected IExpr foldLessSpecific(IVar lessSpecificVar, IVar moreSpecificVar) {
			return lessSpecificVar;
		}

		@Override
		protected boolean evaluate(boolean left, boolean right) {
			return left || right;
		}
	}

	private static class EqOperator extends BinaryOperatorExpr {
		public EqOperator(IExpr left, IExpr right) {
			super(left, right);
		}

		@Override
		protected IExpr create(IExpr left, IExpr right) {
			return new EqOperator(left, right);
		}

		@Override
		protected IExpr fold(boolean arg, IExpr node) {
			return arg? node : new OperatorNot(node);
		}

		@Override
		protected IExpr foldSameExpr(IExpr expr) {
			return TRUE;
		}

		@Override
		protected IExpr foldLessSpecific(IVar lessSpecificVar, IVar moreSpecificVar) {
			return create(lessSpecificVar, moreSpecificVar);
		}

		@Override
		protected boolean evaluate(boolean left, boolean right) {
			return left == right;
		}

	}

	private static class XorOperator extends BinaryOperatorExpr {
		public XorOperator(IExpr left, IExpr right) {
			super(left, right);
		}

		@Override
		protected IExpr create(IExpr left, IExpr right) {
			return new XorOperator(left, right);
		}

		@Override
		protected IExpr fold(boolean value, IExpr arg) {
			return value? new OperatorNot(arg) : arg;
		}

		@Override
		protected IExpr foldSameExpr(IExpr expr) {
			return FALSE;
		}

		@Override
		protected IExpr foldLessSpecific(IVar lessSpecificVar, IVar moreSpecificVar) {
			return create(lessSpecificVar, moreSpecificVar);
		}

		@Override
		protected boolean evaluate(boolean left, boolean right) {
			return left ^ right;
		}

	}

	static {
		operators.registerOperator(new UnaryOperator(OPERATOR_NOT, PRIORITY_NOT) {
			@Override
			protected IExpr createNode(IExpr value) {
				return new OperatorNot(value);
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_AND, PRIORITY_AND) {
			@Override
			protected IExpr createNode(IExpr left, IExpr right) {
				return new AndOperator(left, right);
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_OR, PRIORITY_OR) {
			@Override
			protected IExpr createNode(IExpr left, IExpr right) {
				return new OrOperator(left, right);
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_EQ, PRIORITY_COMPARE) {
			@Override
			protected IExpr createNode(IExpr left, IExpr right) {
				return new EqOperator(left, right);
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_XOR, PRIORITY_COMPARE) {
			@Override
			protected IExpr createNode(IExpr left, IExpr right) {
				return new XorOperator(left, right);
			}
		});

		operators.registerOperator(new BinaryOperator(OPERATOR_DOT, PRIORITY_DOT) {
			@Override
			protected IExpr createNode(IExpr left, IExpr right) {
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

		public IExpr rebind(Map<String, IExpr> vars);

		public Optional<Boolean> getConstantValue();

		public IExpr fold();

		public boolean equals(IExpr other);
	}

	private interface IVar extends IExpr {
		public boolean isLessSpecific(IVar other);
	}

	private static class KeyGet implements IVar {
		private final String key;

		public KeyGet(String key) {
			this.key = key;
		}

		@Override
		public boolean evaluate(Map<String, String> vars) {
			return vars.containsKey(key);
		}

		@Override
		public IExpr rebind(Map<String, IExpr> vars) {
			final IExpr var = vars.get(key);
			return var != null? var : this;
		}

		@Override
		public Optional<Boolean> getConstantValue() {
			return Optional.absent();
		}

		@Override
		public IExpr fold() {
			return this;
		}

		@Override
		public boolean equals(IExpr other) {
			if (other == this) return true;

			if (other instanceof KeyGet)
				return ((KeyGet)other).key.equals(this.key);

			return false;
		}

		@Override
		public boolean isLessSpecific(IVar other) {
			return false;
		}
	}

	private static class KeyValueGet implements IVar {
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

		@Override
		public IExpr rebind(Map<String, IExpr> vars) {
			final IExpr var = vars.get(key);
			if (var == null) return this;

			Preconditions.checkState(var instanceof KeyGet, "Tried to extract value '%s' from expression expanded from key '%s'", value, key);
			final String newKey = ((KeyGet)var).key;
			return new KeyValueGet(newKey, value);
		}

		@Override
		public Optional<Boolean> getConstantValue() {
			return Optional.absent();
		}

		@Override
		public IExpr fold() {
			return this;
		}

		@Override
		public boolean equals(IExpr other) {
			if (other == this) return true;

			if (other instanceof KeyValueGet) {
				final KeyValueGet o = (KeyValueGet)other;
				return o.key.equals(this.key)
						&& o.value.equals(this.value);
			}

			return false;
		}

		@Override
		public boolean isLessSpecific(IVar other) {
			if (other instanceof KeyGet)
				return ((KeyGet)other).key.equals(this.key);

			return false;
		}
	}

	private static class SeparatorExpr implements IExpr {
		private final IExpr expr;

		public SeparatorExpr(IExpr expr) {
			this.expr = expr;
		}

		@Override
		public boolean evaluate(Map<String, String> vars) {
			throw new AssertionError(); // should be optimized before use
		}

		@Override
		public IExpr rebind(Map<String, IExpr> vars) {
			return expr.rebind(vars);
		}

		@Override
		public Optional<Boolean> getConstantValue() {
			return Optional.absent();
		}

		@Override
		public IExpr fold() {
			return expr.fold();
		}

		@Override
		public boolean equals(IExpr other) {
			if (this == other) return true;
			return (other instanceof SeparatorExpr) && ((SeparatorExpr)other).expr.equals(this.expr);
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
			if (token.type.isNumber()) {
				if (token.value.equals("1"))
					return TRUE;
				else if (token.value.equals("0"))
					return FALSE;
			}

			throw new UnsupportedOperationException();
		}
	};

	private static final InfixParser<IExpr, Operator> parser = new InfixParser<IExpr, Operator>(operators, nodeFactory);

	private final SimpleParserState<IExpr> parserState = new SimpleParserState<IExpr>(parser) {
		@Override
		protected IExpr createModifierNode(String modifier, IExpr child) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected IExpr createSymbolNode(String symbol, List<IExpr> children) {
			final Macro macro = macros.get(symbol);
			Preconditions.checkState(macro != null, "Can't find macro %s", symbol);
			return macro.rebind(children);
		}
	};

	private IExpr parseExpression(PeekingIterator<Token> tokens) {
		return parserState.parse(tokens).fold();
	}

	private static class Macro {
		private final List<String> args;
		private final IExpr body;

		public Macro(List<String> args, IExpr body) {
			this.args = args;
			this.body = body;
		}

		public IExpr rebind(List<IExpr> children) {
			final int actualArgCount = children.size();
			final int expectedArgCount = args.size();
			Preconditions.checkState(actualArgCount == expectedArgCount, "Invalid numer of arguments: expected %s, got %s", expectedArgCount, actualArgCount);

			final Map<String, IExpr> env = Maps.newHashMap();
			for (int i = 0; i < expectedArgCount; i++) {
				final String arg = args.get(i);
				final IExpr value = children.get(i);
				env.put(arg, value);
			}

			return body.rebind(env);
		}
	}

	private static List<String> parseMacroArgList(PeekingIterator<Token> tokens) {
		final Token firstToken = expectTokens(tokens, TokenType.SYMBOL, TokenType.RIGHT_BRACKET);

		final List<String> args = Lists.newArrayList();
		if (firstToken.type == TokenType.RIGHT_BRACKET) {
			Preconditions.checkState(firstToken.value.equals(")"), "Unexpected bracket: '%s'", firstToken.value);
			return args;
		}

		args.add(firstToken.value);

		while (true) {
			final Token token = expectTokens(tokens, TokenType.SEPARATOR, TokenType.RIGHT_BRACKET);
			if (token.type == TokenType.RIGHT_BRACKET) {
				Preconditions.checkState(token.value.equals(")"), "Unexpected bracket: '%s'", token.value);
				break;
			}

			final String arg = expectToken(tokens, TokenType.SYMBOL);
			args.add(arg);
		}
		return args;
	}

	private Macro parseMacro(PeekingIterator<Token> tokens) {
		final List<String> args = parseMacroArgList(tokens);
		expectToken(tokens, TokenType.MODIFIER, MODIFIER_ASSIGN);

		final IExpr body = parseExpression(tokens);
		return new Macro(args, body);
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

	private Map<String, Macro> macros = Maps.newHashMap();

	public void addStatement(String statement) {
		try {
			final PeekingIterator<Token> tokens = tokenizer.tokenize(statement);
			final String definedSymbol = expectToken(tokens, TokenType.SYMBOL);
			final Token token = expectTokens(tokens, TokenType.LEFT_BRACKET, TokenType.MODIFIER, TokenType.OPERATOR);

			if (token.type == TokenType.LEFT_BRACKET) {
				Preconditions.checkState(token.value.equals("("), "Invalid bracket: '%s'", token.value);
				macros.put(definedSymbol, parseMacro(tokens));
			} else if (token.type == TokenType.OPERATOR) {
				Preconditions.checkState(token.value.equals(OPERATOR_DOT), "Invalid token: ", token);
				final String value = expectToken(tokens, TokenType.SYMBOL);
				expectToken(tokens, TokenType.MODIFIER, MODIFIER_ASSIGN);
				final IExpr expr = parseExpression(tokens);
				program.add(new SetKeyValueVar(expr, definedSymbol, value));
			} else if (token.type == TokenType.MODIFIER) {
				Preconditions.checkState(token.value.equals(MODIFIER_ASSIGN), "Invalid token: ", token);
				final IExpr expr = parseExpression(tokens);
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
