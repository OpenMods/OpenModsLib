package openmods.calc;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import openmods.calc.executable.BinaryOperator.Associativity;
import openmods.calc.parsing.ast.IAstParser;
import openmods.calc.parsing.ast.IModifierStateTransition;
import openmods.calc.parsing.ast.INodeFactory;
import openmods.calc.parsing.ast.IOperator;
import openmods.calc.parsing.ast.IParserState;
import openmods.calc.parsing.ast.ISymbolCallStateTransition;
import openmods.calc.parsing.ast.OperatorArity;
import openmods.calc.parsing.ast.QuotedParser;
import openmods.calc.parsing.ast.QuotedParser.IQuotedExprNodeFactory;
import openmods.calc.parsing.token.Token;
import org.junit.Assert;

public class AstParserTestUtils extends CalcTestUtils {

	public static abstract class DummyOperator implements IOperator<DummyOperator> {

		private final String id;

		public DummyOperator(String id) {
			this.id = id;
		}

		@Override
		public String id() {
			return id;
		}
	}

	public static class DummyBinaryOperator extends DummyOperator {

		public final int precedence;

		public final Associativity associativity;

		public DummyBinaryOperator(int precedence, String id, Associativity associativity) {
			super(id);
			this.precedence = precedence;
			this.associativity = associativity;
		}

		public DummyBinaryOperator(int precedence, String id) {
			this(precedence, id, Associativity.LEFT);
		}

		@Override
		public boolean isLowerPriority(DummyOperator other) {
			if (other.arity() == OperatorArity.UNARY) return true; // unary operators always have higher precedence than binary

			final DummyBinaryOperator o = (DummyBinaryOperator)other;
			return associativity.isLessThan(precedence, o.precedence);
		}

		@Override
		public OperatorArity arity() {
			return OperatorArity.BINARY;
		}

	}

	public static class DummyUnaryOperator extends DummyOperator {

		public DummyUnaryOperator(String id) {
			super(id);
		}

		@Override
		public OperatorArity arity() {
			return OperatorArity.UNARY;
		}

		@Override
		public boolean isLowerPriority(DummyOperator other) {
			return false; // every other operator has lower or equal precendence
		}

	}

	public static final DummyOperator PLUS = new DummyBinaryOperator(1, "+");

	public static final DummyOperator UNARY_PLUS = new DummyUnaryOperator("+");

	public static final DummyOperator MINUS = new DummyBinaryOperator(1, "-");

	public static final DummyOperator UNARY_MINUS = new DummyUnaryOperator("-");

	public static final DummyOperator UNARY_NEG = new DummyUnaryOperator("!");

	public static final DummyOperator MULTIPLY = new DummyBinaryOperator(2, "*");

	public static final DummyOperator DEFAULT = new DummyBinaryOperator(2, "<*>");

	public static final DummyOperator ASSIGN = new DummyBinaryOperator(1, "=", Associativity.RIGHT);

	public static class TestAstNode {
		public final String type;

		public final String value;

		public final List<TestAstNode> children;

		public TestAstNode(String type, String value, List<TestAstNode> children) {
			this.type = type;
			this.value = value;
			this.children = ImmutableList.copyOf(children);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((children == null)? 0 : children.hashCode());
			result = prime * result + ((type == null)? 0 : type.hashCode());
			result = prime * result + ((value == null)? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;

			if (obj instanceof TestAstNode) {
				final TestAstNode other = (TestAstNode)obj;
				return other.type.equals(this.type) &&
						other.value.equals(this.value) &&
						other.children.equals(this.children);
			}

			return false;
		}

		@Override
		public String toString() {
			return "[" + type + ":" + value + "->" + children + "]";
		}
	}

	public static TestAstNode node(String type, String value, List<TestAstNode> children) {
		return new TestAstNode(type, value, children);
	}

	public static TestAstNode node(String type, String value, TestAstNode... children) {
		return new TestAstNode(type, value, Arrays.asList(children));
	}

	public static TestAstNode value(Token token) {
		Assert.assertTrue(token.type.isValue());
		return node("value", token.type + ":" + token.value);
	}

	public static TestAstNode valueDec(String value) {
		return value(dec(value));
	}

	public static TestAstNode quotedToken(Token token) {
		return node("quotedToken", token.type + ":" + token.value);
	}

	public static TestAstNode value(String value) {
		return node("valueStr", value);
	}

	public static TestAstNode modifier(String type, List<TestAstNode> children) {
		return node("modifier", type, children);
	}

	public static TestAstNode modifier(String type, TestAstNode... children) {
		return node("modifier", type, children);
	}

	public static TestAstNode quoteModifier(TestAstNode... children) {
		return modifier(QUOTE_MODIFIER.value, children);
	}

	public static TestAstNode quoteSymbol(TestAstNode... children) {
		return modifier(QUOTE_SYMBOL.value, children);
	}

	public static TestAstNode quoteRoot(List<TestAstNode> children) {
		return modifier("quote", children);
	}

	public static TestAstNode brackets(String opening, String closing, TestAstNode... children) {
		return node("brackets", opening + closing, children);
	}

	public static TestAstNode brackets(TestAstNode... children) {
		return brackets("(", ")", children);
	}

	public static TestAstNode squareBrackets(TestAstNode... children) {
		return brackets("[", "]", children);
	}

	public static TestAstNode brackets(String opening, String closing, List<TestAstNode> children) {
		return node("brackets", opening + closing, children);
	}

	public static TestAstNode quotedBrackets(String opening, String closing, TestAstNode... children) {
		return node("quotedBrackets", opening + closing, children);
	}

	public static TestAstNode quotedBrackets(String opening, String closing, List<TestAstNode> children) {
		return node("quotedBrackets", opening + closing, children);
	}

	public static TestAstNode call(String symbol, List<TestAstNode> children) {
		return node("call", symbol, children);
	}

	public static TestAstNode call(String symbol, TestAstNode... children) {
		return node("call", symbol, children);
	}

	public static TestAstNode get(String symbol) {
		return node("get", symbol);
	}

	public static TestAstNode operator(DummyOperator op, TestAstNode... children) {
		Assert.assertEquals(op.arity().args, children.length);
		return node("op", op.id, children);
	}

	public static TestAstNode operator(DummyOperator op, List<TestAstNode> children) {
		Assert.assertEquals(op.arity().args, children.size());
		return node("op", op.id, children);
	}

	public static class QuoteNodeTestFactory implements IQuotedExprNodeFactory<TestAstNode> {

		@Override
		public TestAstNode createValueNode(Token token) {
			return quotedToken(token);
		}

		@Override
		public TestAstNode createBracketNode(final String openingBracket, final String closingBracket, final List<TestAstNode> children) {
			return quotedBrackets(openingBracket, closingBracket, children);
		}
	}

	public static class QuotedParserState implements IParserState<TestAstNode> {
		private final QuotedParser<TestAstNode> quotedParser = new QuotedParser<TestAstNode>(new QuoteNodeTestFactory());

		@Override
		public IAstParser<TestAstNode> getParser() {
			return quotedParser;
		}

		@Override
		public ISymbolCallStateTransition<TestAstNode> getStateForSymbolCall(String symbol) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IModifierStateTransition<TestAstNode> getStateForModifier(String modifier) {
			throw new UnsupportedOperationException();
		}

	}

	public static class ModifierQuoteTransition implements IModifierStateTransition<TestAstNode> {
		private final String modifier;

		public ModifierQuoteTransition(String modifier) {
			this.modifier = modifier;
		}

		@Override
		public TestAstNode createRootNode(TestAstNode child) {
			return modifier(modifier, child);
		}

		@Override
		public IParserState<TestAstNode> getState() {
			return new QuotedParserState();
		}
	}

	public static class SymbolQuoteTransition implements ISymbolCallStateTransition<TestAstNode> {
		private final String symbol;

		public SymbolQuoteTransition(String symbol) {
			this.symbol = symbol;
		}

		@Override
		public TestAstNode createRootNode(List<TestAstNode> children) {
			return modifier(symbol, children);
		}

		@Override
		public IParserState<TestAstNode> getState() {
			return new QuotedParserState();
		}
	}

	public abstract static class TestParserState implements IParserState<TestAstNode> {

		@Override
		public IModifierStateTransition<TestAstNode> getStateForModifier(String modifier) {
			if (modifier.equals(QUOTE_MODIFIER.value)) return new ModifierQuoteTransition(modifier);
			throw new UnsupportedOperationException(modifier);
		}

		@Override
		public ISymbolCallStateTransition<TestAstNode> getStateForSymbolCall(final String symbol) {
			if (symbol.equals(QUOTE_SYMBOL.value)) return new SymbolQuoteTransition(symbol);
			return new ISymbolCallStateTransition<TestAstNode>() {
				@Override
				public IParserState<TestAstNode> getState() {
					return TestParserState.this;
				}

				@Override
				public TestAstNode createRootNode(List<TestAstNode> children) {
					return call(symbol, children);
				}
			};
		}
	}

	public static final INodeFactory<TestAstNode, DummyOperator> EXPR_FACTORY = new INodeFactory<TestAstNode, DummyOperator>() {

		@Override
		public TestAstNode createValueNode(Token token) {
			return value(token);
		}

		@Override
		public TestAstNode createSymbolGetNode(String id) {
			return get(id);
		}

		@Override
		public TestAstNode createOpNode(DummyOperator op, List<TestAstNode> children) {
			return operator(op, children);
		}

		@Override
		public TestAstNode createBracketNode(String openingBracket, String closingBracket, List<TestAstNode> children) {
			return brackets(openingBracket, closingBracket, children);
		}
	};

	public static class ParserResultTester {
		private final IParserState<TestAstNode> initialState;
		private final TestAstNode actual;

		public ParserResultTester(IParserState<TestAstNode> initialState, Token... inputs) {
			this.initialState = initialState;
			this.actual = initialState.getParser().parse(initialState, tokenIterator(inputs));
		}

		public ParserResultTester expect(TestAstNode expected) {
			Assert.assertEquals(expected, actual);
			return this;
		}

		public ParserResultTester expectSameAs(Token... inputs) {
			TestAstNode result = initialState.getParser().parse(initialState, tokenIterator(inputs));
			Assert.assertEquals(result, actual);
			return this;
		}
	}

}
