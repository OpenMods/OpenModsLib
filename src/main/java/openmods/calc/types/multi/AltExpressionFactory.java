package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.IValuePrinter;
import openmods.calc.StackValidationException;
import openmods.calc.SymbolCall;
import openmods.calc.SymbolMap;
import openmods.calc.Value;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.calc.parsing.SymbolCallNode;
import openmods.calc.parsing.SymbolGetNode;
import openmods.utils.Stack;

public class AltExpressionFactory {

	private final TypeDomain domain;
	private final TypedValue nullValue;
	private final BinaryOperator<TypedValue> colonOperator;
	private final BinaryOperator<TypedValue> assignOperator;
	private final BinaryOperator<TypedValue> splitOperator;

	public AltExpressionFactory(TypeDomain domain, TypedValue nullValue, BinaryOperator<TypedValue> colonOperator, BinaryOperator<TypedValue> assignOperator, BinaryOperator<TypedValue> splitOperator) {
		this.domain = domain;
		this.nullValue = nullValue;
		this.colonOperator = colonOperator;
		this.assignOperator = assignOperator;
		this.splitOperator = splitOperator;
	}

	private class AltConstructorCompiler {
		private final List<String> parts;

		public AltConstructorCompiler(String name) {
			this.parts = Lists.newArrayList(name);
		}

		public void addMember(String name) {
			this.parts.add(name);
		}

		public void flatten(List<IExecutable<TypedValue>> output) {
			final List<TypedValue> result = Lists.newArrayList();
			for (String s : parts)
				result.add(domain.create(Symbol.class, Symbol.get(s)));

			output.add(Value.create(Cons.createList(result, nullValue)));
		}
	}

	private class AltNode extends ScopeModifierNode {

		public AltNode(IExprNode<TypedValue> altDefinitionNode, IExprNode<TypedValue> codeNode) {
			super(domain, TypedCalcConstants.SYMBOL_ALT, colonOperator, assignOperator, altDefinitionNode, codeNode);
		}

		@Override
		protected void flattenNameAndValue(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> name, IExprNode<TypedValue> value) {
			flattenTypeName(output, name);
			flattenConstructorDefinitions(output, value);
		}

		private void flattenTypeName(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> name) {
			try {
				// f:..., 'f':..., #f:...
				output.add(Value.create(TypedCalcUtils.extractNameFromNode(domain, name)));
			} catch (IllegalArgumentException e) {
				// hopefully something that evaluates to symbol
				name.flatten(output);
			}
		}

		private void flattenConstructorDefinitions(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> value) {
			if (value instanceof BinaryOpNode) {
				final BinaryOpNode<TypedValue> op = (BinaryOpNode<TypedValue>)value;
				if (op.operator == splitOperator) {
					final List<AltConstructorCompiler> ctors = Lists.newArrayList();
					flattenConstructorDefinitionList(ctors, op);
					for (AltConstructorCompiler ctor : ctors)
						ctor.flatten(output);

					output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, ctors.size(), 1));
				} else {
					// list: #ctorName, #ctorArgs...
					value.flatten(output);
				}
			} else {
				// list: #ctorName, #ctorArgs...
				value.flatten(output);
			}
		}

		private void flattenConstructorDefinitionList(List<AltConstructorCompiler> output, BinaryOpNode<TypedValue> op) {
			flattenConstructorDefinition(output, op.left);
			if (op.right instanceof BinaryOpNode) {
				final BinaryOpNode<TypedValue> rightOp = (BinaryOpNode<TypedValue>)op.right;
				Preconditions.checkState(rightOp.operator == splitOperator, "Malformed constructor list, expected '\\', got %s", rightOp);
				flattenConstructorDefinitionList(output, rightOp);
			} else {
				flattenConstructorDefinition(output, op.right);
			}
		}

		private void flattenConstructorDefinition(List<AltConstructorCompiler> output, IExprNode<TypedValue> ctorNode) {
			if (ctorNode instanceof SymbolGetNode) {
				final SymbolGetNode<TypedValue> getNode = (SymbolGetNode<TypedValue>)ctorNode;
				output.add(new AltConstructorCompiler(getNode.symbol()));
			} else if (ctorNode instanceof SymbolCallNode) {
				final SymbolCallNode<TypedValue> callNode = (SymbolCallNode<TypedValue>)ctorNode;
				final AltConstructorCompiler result = new AltConstructorCompiler(callNode.symbol());
				for (IExprNode<TypedValue> ctorArg : callNode.getChildren()) {
					Preconditions.checkState(ctorArg instanceof SymbolGetNode, "Expected bare symbol, got %s", ctorArg);
					result.addMember(((SymbolGetNode<TypedValue>)ctorArg).symbol());
				}
				output.add(result);
			} else {
				throw new IllegalStateException("Expected alt type constructor, got " + ctorNode);
			}
		}

	}

	public ISymbolCallStateTransition<TypedValue> createStateTransition(ICompilerState<TypedValue> parentState) {
		class AltStateTransition extends SameStateSymbolTransition<TypedValue> {
			public AltStateTransition(ICompilerState<TypedValue> parentState) {
				super(parentState);
			}

			@Override
			public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
				Preconditions.checkState(children.size() == 2, "Expected two args for 'alt' expression");
				return new AltNode(children.get(0), children.get(1));
			}
		}

		return new AltStateTransition(parentState);
	}

	private static interface AltTypeVariantTrait extends ICompositeTrait {
		public AltType getType();
	}

	private static interface AltContainerTrait extends ICompositeTrait {
		public AltTypeVariant getVariant();

		public List<TypedValue> values();
	}

	private static class AltType extends SimpleComposite implements CompositeTraits.Decomposable, CompositeTraits.Printable {

		private final String name;

		public AltType(String name) {
			this.name = name;
		}

		@Override
		public String type() {
			return name;
		}

		@Override
		public String str(IValuePrinter<TypedValue> printer) {
			return "<alt " + name + ">";
		}

		@Override
		public Optional<List<TypedValue>> tryDecompose(final TypedValue input, int variableCount) {
			Preconditions.checkArgument(variableCount == 1, "Invalid number of values to unpack, expected 1 got %s", variableCount);

			return TypedCalcUtils.tryDecomposeTrait(input, AltTypeVariantTrait.class, new Function<AltTypeVariantTrait, Optional<List<TypedValue>>>() {
				@Override
				public Optional<List<TypedValue>> apply(AltTypeVariantTrait trait) {
					if (trait.getType() == AltType.this) {
						final List<TypedValue> result = ImmutableList.of(input);
						return Optional.of(result);
					} else {
						return Optional.absent();
					}
				}
			});
		}

	}

	private class AltTypeVariant extends SimpleComposite implements CompositeTraits.Decomposable, CompositeTraits.Printable, AltTypeVariantTrait, CompositeTraits.Callable {

		private final String name;

		private final AltType type;

		private final List<String> members;

		public AltTypeVariant(String name, AltType type, List<String> members) {
			this.name = name;
			this.type = type;
			this.members = members;
		}

		@Override
		public String type() {
			return type.name + ":" + name;
		}

		@Override
		public AltType getType() {
			return type;
		}

		@Override
		public String str(IValuePrinter<TypedValue> printer) {
			return "<alt " + type.name + ":" + name + ">";
		}

		@Override
		public Optional<List<TypedValue>> tryDecompose(TypedValue input, int variableCount) {
			Preconditions.checkArgument(variableCount == members.size(), "Invalid number of values to unpack, expected %s got %s", members.size(), variableCount);
			return TypedCalcUtils.tryDecomposeTrait(input, AltContainerTrait.class, new Function<AltContainerTrait, Optional<List<TypedValue>>>() {
				@Override
				public Optional<List<TypedValue>> apply(AltContainerTrait trait) {
					if (trait.getVariant() == AltTypeVariant.this) {
						final List<TypedValue> values = trait.values();
						Preconditions.checkState(values.size() == members.size(), "Mismatched size in container: names: %s, values: %s", members, values);
						return Optional.of(values);
					} else {
						return Optional.absent();
					}
				}
			});
		}

		@Override
		public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			if (argumentsCount.isPresent()) {
				final int args = argumentsCount.get();
				if (args != members.size()) throw new StackValidationException("Expected %s argument(s) but got %s", members.size(), args);
			}

			if (returnsCount.isPresent()) {
				final int returns = returnsCount.get();
				if (returns != 1) throw new StackValidationException("Has single result but expected %s", returns);
			}

			final Stack<TypedValue> substack = frame.stack().substack(members.size());
			final AltContainer container = new AltContainer(this, ImmutableList.copyOf(substack));
			substack.clear();
			substack.push(domain.create(IComposite.class, container));
		}

	}

	private static class AltContainer extends SimpleComposite implements CompositeTraits.Printable, AltContainerTrait {

		private final AltTypeVariant variant;

		private final List<TypedValue> values;

		public AltContainer(AltTypeVariant variant, List<TypedValue> values) {
			this.variant = variant;
			this.values = values;
		}

		@Override
		public String type() {
			return "instance " + variant.type();
		}

		@Override
		public AltTypeVariant getVariant() {
			return variant;
		}

		@Override
		public List<TypedValue> values() {
			return values;
		}

		@Override
		public String str(IValuePrinter<TypedValue> printer) {
			// TODO somehow mix member names here?
			return "<alt " + variant.type.name + ":" + variant.name + "(" + Joiner.on(',').join(values) + ")>";
		}

	}

	private class AltSymbol implements ICallable<TypedValue> {

		@Override
		public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			if (argumentsCount.isPresent()) {
				final int args = argumentsCount.get();
				if (args != 2) throw new StackValidationException("Expected 2 arguments but got %s", args);
			}

			final Frame<TypedValue> subframe = FrameFactory.newLocalFrameWithSubstack(frame, 2);
			final Stack<TypedValue> substack = subframe.stack();
			final Code code = substack.pop().as(Code.class, "second (code) 'alt' parameter");
			final Cons vars = substack.pop().as(Cons.class, "first (definitions list) 'alt' parameter");

			try {
				prepareFrame(subframe.symbols(), vars);
			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to extract alt types description from: " + vars, e);
			}
			code.execute(subframe);

			if (returnsCount.isPresent())

			{
				final int expected = returnsCount.get();
				final int actual = substack.size();
				if (expected != actual) throw new StackValidationException("Has %s result(s) but expected %s", actual, expected);
			}
		}

		private void prepareFrame(final SymbolMap<TypedValue> symbols, Cons vars) {
			vars.visit(new Cons.ListVisitor(nullValue) {
				@Override
				public void value(TypedValue value, boolean isLast) {
					visitTypeDefinition(symbols, value);
				}
			});
		}

		private void visitTypeDefinition(final SymbolMap<TypedValue> symbols, TypedValue typeDefinition) {
			final Cons nameAndCtors = typeDefinition.as(Cons.class);
			final Symbol name = nameAndCtors.car.as(Symbol.class);
			final Cons ctors = nameAndCtors.cdr.as(Cons.class);

			final AltType newType = new AltType(name.value);
			symbols.put(name.value, domain.create(IComposite.class, newType));

			ctors.visit(new Cons.ListVisitor(nullValue) {
				@Override
				public void value(TypedValue value, boolean isLast) {
					visitConstructor(symbols, newType, value);
				}
			});
		}

		private void visitConstructor(SymbolMap<TypedValue> symbols, AltType type, TypedValue ctorDefinition) {
			final Cons nameAndMembers = ctorDefinition.as(Cons.class);

			final Symbol name = nameAndMembers.car.as(Symbol.class);
			final List<String> memberNames = Lists.newArrayList();

			final TypedValue membersList = nameAndMembers.cdr;
			if (membersList.is(Cons.class)) {
				final Cons members = membersList.as(Cons.class);

				members.visit(new Cons.ListVisitor(nullValue) {
					@Override
					public void value(TypedValue value, boolean isLast) {
						memberNames.add(value.as(Symbol.class).value);
					}
				});
			} else {
				Preconditions.checkState(membersList == nullValue, "Expected list or null (empty list), got %s", membersList);
			}

			final AltTypeVariant ctor = new AltTypeVariant(name.value, type, memberNames);
			symbols.put(name.value, domain.create(IComposite.class, ctor));
		}

	}

	public void registerSymbol(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_ALT, new AltSymbol());
	}

}
