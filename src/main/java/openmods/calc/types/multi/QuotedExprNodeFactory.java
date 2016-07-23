package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.Value;
import openmods.calc.parsing.ContainerNode;
import openmods.calc.parsing.EmptyExprNodeFactory;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.NullNode;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;
import openmods.calc.parsing.ValueNode;

class QuotedExprNodeFactory extends EmptyExprNodeFactory<TypedValue> {

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
			final TypedValue node = transformNode(arg);
			output.add(Value.create(node));
		}

		private TypedValue transformNode(IExprNode<TypedValue> arg) {
			final List<TypedValue> elements = Lists.newArrayList();
			boolean dotFound = false;
			boolean firstAfterDot = false;
			for (IExprNode<TypedValue> childArg : arg.getChildren()) {
				if (childArg == QUOTED_DOT_MARKER) {
					Preconditions.checkState(!dotFound, "Duplicated dot in quoted statement");
					dotFound = true;
				} else {
					if (dotFound) {
						Preconditions.checkState(!firstAfterDot, "More than one element after dot");
						firstAfterDot = true;
					}

					if (childArg instanceof ValueNode) {
						elements.add(((ValueNode<TypedValue>)childArg).value);
					} else if (childArg instanceof ContainerNode) {
						elements.add(transformNode(childArg));
					}
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

	private final TypeDomain domain;
	private final TypedValue terminatorValue;

	public QuotedExprNodeFactory(TypeDomain domain, TypedValue terminatorValue) {
		this.domain = domain;
		this.terminatorValue = terminatorValue;
	}

	@Override
	public IExprNode<TypedValue> createRawValueNode(Token token) {
		if (token.type == TokenType.MODIFIER && token.value.equals("."))
			return QUOTED_DOT_MARKER;
		if (token.type == TokenType.SEPARATOR) return new NullNode<TypedValue>();
		return new ValueNode<TypedValue>(domain.create(String.class, token.value));
	}

	@Override
	public IExprNode<TypedValue> createValueNode(TypedValue value) {
		return new ValueNode<TypedValue>(value);
	}

	@Override
	public IExprNode<TypedValue> createBracketNode(String openingBracket, String closingBracket, List<IExprNode<TypedValue>> children) {
		return new ContainerNode<TypedValue>(children);
	}

	@Override
	public IExprNode<TypedValue> createRootNode(IExprNode<TypedValue> child) {
		return new QuotedRoot(child);
	}
}