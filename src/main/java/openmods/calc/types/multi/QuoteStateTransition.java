package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.Value;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.ast.IAstParser;
import openmods.calc.parsing.ast.IModifierStateTransition;
import openmods.calc.parsing.ast.IParserState;
import openmods.calc.parsing.ast.ISymbolCallStateTransition;
import openmods.calc.parsing.ast.QuotedParser;
import openmods.calc.parsing.ast.QuotedParser.IQuotedExprNodeFactory;
import openmods.calc.parsing.node.ContainerNode;
import openmods.calc.parsing.node.IExprNode;
import openmods.calc.parsing.node.NullNode;
import openmods.calc.parsing.node.ValueNode;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;

public class QuoteStateTransition {

	private static final IExprNode<TypedValue> QUOTED_DOT_MARKER = new IExprNode<TypedValue>() {
		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			throw new UnsupportedOperationException(); // should be captured before serialization;
		}

		@Override
		public Iterable<IExprNode<TypedValue>> getChildren() {
			throw new UnsupportedOperationException();
		}
	};

	private class QuotedRoot implements IExprNode<TypedValue> {
		private final IExprNode<TypedValue> arg;

		public QuotedRoot(IExprNode<TypedValue> arg) {
			this.arg = arg;
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			final TypedValue node = unwrapNode(arg);
			output.add(Value.create(node));
		}

		private TypedValue unwrapNode(IExprNode<TypedValue> arg) {
			if (arg instanceof ValueNode) {
				return ((ValueNode<TypedValue>)arg).value;
			} else if (arg instanceof ContainerNode) {
				return unwrapList(arg);
			} else {
				throw new IllegalStateException(arg.toString());
			}
		}

		private TypedValue unwrapList(IExprNode<TypedValue> arg) {
			final List<TypedValue> elements = Lists.newArrayList();
			boolean dotFound = false;
			boolean firstAfterDot = false;
			for (IExprNode<TypedValue> childArg : arg.getChildren()) {
				if (childArg instanceof NullNode) continue;
				if (childArg == QUOTED_DOT_MARKER) {
					Preconditions.checkState(!dotFound, "Duplicated dot in quoted statement");
					dotFound = true;
				} else {
					if (dotFound) {
						Preconditions.checkState(!firstAfterDot, "More than one element after dot");
						firstAfterDot = true;
					}

					elements.add(unwrapNode(childArg));
				}
			}

			if (dotFound) {
				final int lastElement = elements.size() - 1;
				final TypedValue customTerminatorValue = elements.get(lastElement);
				return Cons.createList(elements.subList(0, lastElement), customTerminatorValue);
			} else {
				return Cons.createList(elements, terminatorValue);
			}
		}

		@Override
		public Iterable<IExprNode<TypedValue>> getChildren() {
			return ImmutableList.of(); // hide children - will be flattened
		}
	}

	private class QuotedExprNodeFactory implements IQuotedExprNodeFactory<IExprNode<TypedValue>> {

		@Override
		public IExprNode<TypedValue> createValueNode(Token token) {
			if (token.type == TokenType.MODIFIER && token.value.equals(TypedCalcConstants.MODIFIER_CDR))
				return QUOTED_DOT_MARKER;
			if (token.type == TokenType.SEPARATOR) return new NullNode<TypedValue>();
			if (token.type.isValue()) {
				final TypedValue value = valueParser.parseToken(token);
				return new ValueNode<TypedValue>(value);
			}
			return new ValueNode<TypedValue>(Symbol.get(domain, token.value));
		}

		@Override
		public IExprNode<TypedValue> createBracketNode(String openingBracket, String closingBracket, List<IExprNode<TypedValue>> children) {
			return new ContainerNode<TypedValue>(children);
		}
	}

	private class QuotedState implements IParserState<IExprNode<TypedValue>> {

		private final QuotedParser<IExprNode<TypedValue>> quotedParser = new QuotedParser<IExprNode<TypedValue>>(new QuotedExprNodeFactory());

		@Override
		public IAstParser<IExprNode<TypedValue>> getParser() {
			return quotedParser;
		}

		@Override
		public ISymbolCallStateTransition<IExprNode<TypedValue>> getStateForSymbolCall(String symbol) {
			throw new UnsupportedOperationException(symbol);
		}

		@Override
		public IModifierStateTransition<IExprNode<TypedValue>> getStateForModifier(String modifier) {
			throw new UnsupportedOperationException(modifier);
		}

	}

	private final TypeDomain domain;
	private final TypedValue terminatorValue;
	private final IValueParser<TypedValue> valueParser;

	public QuoteStateTransition(TypeDomain domain, TypedValue terminatorValue, IValueParser<TypedValue> valueParser) {
		this.domain = domain;
		this.terminatorValue = terminatorValue;
		this.valueParser = valueParser;
	}

	public static class ForSymbol extends QuoteStateTransition implements ISymbolCallStateTransition<IExprNode<TypedValue>> {
		public ForSymbol(TypeDomain domain, TypedValue terminatorValue, IValueParser<TypedValue> valueParser) {
			super(domain, terminatorValue, valueParser);
		}

		@Override
		public IParserState<IExprNode<TypedValue>> getState() {
			return new QuotedState();
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			Preconditions.checkArgument(children.size() == 1, "Expected exactly one child for quote, got %s", children);
			return new QuotedRoot(children.get(0));
		}
	}

	public static class ForModifier extends QuoteStateTransition implements IModifierStateTransition<IExprNode<TypedValue>> {
		public ForModifier(TypeDomain domain, TypedValue terminatorValue, IValueParser<TypedValue> valueParser) {
			super(domain, terminatorValue, valueParser);
		}

		@Override
		public IParserState<IExprNode<TypedValue>> getState() {
			return new QuotedState();
		}

		@Override
		public IExprNode<TypedValue> createRootNode(IExprNode<TypedValue> child) {
			return new QuotedRoot(child);
		}
	}
}