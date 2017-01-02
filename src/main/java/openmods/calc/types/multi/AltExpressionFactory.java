package openmods.calc.types.multi;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import openmods.calc.BinaryOperator;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
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
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class AltExpressionFactory {

	private static final String ATTR_FIELDS = "fields";

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

		this.domain.registerType(AltType.class, "alt_type", createAltTypeMetaObject());
		this.domain.registerType(AltTypeVariant.class, "alt_variant", createAltVariantMetaObject());
		this.domain.registerType(AltValue.class, "alt_value", createAltValueMetaObject());
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

		public AltNode(List<IExprNode<TypedValue>> children) {
			super(domain, TypedCalcConstants.SYMBOL_ALT, colonOperator, assignOperator, children);
		}

		@Override
		protected void flattenNameAndValue(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> nameNode, IExprNode<TypedValue> ctorsNode) {
			output.add(Value.create(TypedCalcUtils.extractNameFromNode(domain, nameNode)));

			final List<AltConstructorCompiler> ctors = Lists.newArrayList();
			flattenConstructorDefinitionList(ctors, ctorsNode);
			for (AltConstructorCompiler ctor : ctors)
				ctor.flatten(output);

			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, ctors.size(), 1));
		}

		private void flattenConstructorDefinitionList(List<AltConstructorCompiler> output, IExprNode<TypedValue> ctorsNode) {
			if (ctorsNode instanceof BinaryOpNode) {
				final BinaryOpNode<TypedValue> opNode = (BinaryOpNode<TypedValue>)ctorsNode;
				Preconditions.checkState(opNode.operator == splitOperator, "Malformed constructor list, expected '\\', got %s", opNode.operator);
				flattenConstructorDefinition(output, opNode.left);
				flattenConstructorDefinitionList(output, opNode.right);
			} else {
				flattenConstructorDefinition(output, ctorsNode);
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
				return new AltNode(children);
			}
		}

		return new AltStateTransition(parentState);
	}

	private class AltType extends TypeUserdata {
		private final String id;
		private final TypedValue selfValue;
		private Map<String, TypedValue> members = Maps.newHashMap();

		public AltType(String id) {
			super("alt_type_" + id, AltValue.class);
			this.id = id;
			this.selfValue = domain.create(AltType.class, this);
		}

		public Optional<TypedValue> attr(String key) {
			return Optional.fromNullable(members.get(key));
		}

		@Override
		public Iterable<String> dir() {
			return members.keySet();
		}
	}

	private static MetaObject createAltTypeMetaObject() {
		return MetaObject.builder()
				.set(new MetaObject.SlotStr() {
					@Override
					public String str(TypedValue self, Frame<TypedValue> frame) {
						return "<alt " + self.as(AltType.class) + ">";
					}
				})
				.set(new MetaObject.SlotRepr() {
					@Override
					public String repr(TypedValue self, Frame<TypedValue> frame) {
						return "<alt " + self.as(AltType.class) + ">";
					}
				})
				.set(new MetaObject.SlotAttr() {
					@Override
					public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
						return self.as(AltType.class).attr(key);
					}
				})
				.set(new MetaObject.SlotDir() {
					@Override
					public Iterable<String> dir(TypedValue self, Frame<TypedValue> frame) {
						return self.as(AltType.class).dir();
					}
				})
				.set(MetaObjectUtils.DECOMPOSE_ON_TYPE)
				.build();
	}

	private class AltTypeVariant extends TypeUserdata {
		private final String id;

		private final AltType type;

		private final List<String> members;

		private final TypedValue membersList;

		public AltTypeVariant(String name, AltType type, List<String> members) {
			super("alt_variant_" + type.id + "_" + name, AltValue.class);
			this.id = name;
			this.type = type;
			this.members = members;

			final List<TypedValue> convertedMembers = ImmutableList.copyOf(Iterables.transform(members, domain.createWrappingTransformer(String.class)));
			this.membersList = Cons.createList(convertedMembers, nullValue);
		}
	}

	private MetaObject createAltVariantMetaObject() {
		return MetaObject.builder()
				.set(new MetaObject.SlotDecompose() {
					@Override
					public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
						final AltTypeVariant variant = self.as(AltTypeVariant.class);

						Preconditions.checkArgument(variableCount == variant.members.size(), "Invalid number of values to unpack, expected %s got %s", variant.members.size(), variableCount);

						if (input.is(AltValue.class)) {
							final AltValue value = input.as(AltValue.class);

							if (value.variant == variant) {
								final List<TypedValue> values = value.values;
								Preconditions.checkState(values.size() == variant.members.size(), "Mismatched size in container: names: %s, values: %s", variant.members, values);
								return Optional.of(values);
							}
						}

						return Optional.absent();
					}

				})
				.set(new MetaObject.SlotStr() {

					@Override
					public String str(TypedValue self, Frame<TypedValue> frame) {
						final AltTypeVariant variant = self.as(AltTypeVariant.class);
						return "<alt " + variant.type.id + ":" + variant.id + ">";
					}
				})
				.set(new MetaObject.SlotRepr() {

					@Override
					public String repr(TypedValue self, Frame<TypedValue> frame) {
						final AltTypeVariant variant = self.as(AltTypeVariant.class);
						return "<alt " + variant.type.id + ":" + variant.id + ">";
					}
				})
				.set(new MetaObject.SlotCall() {

					@Override
					public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
						final AltTypeVariant variant = self.as(AltTypeVariant.class);

						TypedCalcUtils.expectExactArgCount(argumentsCount, variant.members.size());
						TypedCalcUtils.expectSingleReturn(returnsCount);

						final Stack<TypedValue> substack = frame.stack().substack(variant.members.size());
						final AltValue container = new AltValue(variant, ImmutableList.copyOf(substack));
						substack.clear();
						substack.push(domain.create(AltValue.class, container));
					}
				})
				.set(new MetaObject.SlotAttr() {
					@Override
					public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
						if (TypeUserdata.ATTR_TYPE_NAME.equals(key)) return Optional.of(domain.create(String.class, self.as(AltTypeVariant.class).name));
						else if (ATTR_FIELDS.equals(key)) return Optional.of(self.as(AltTypeVariant.class).membersList);
						return Optional.absent();
					}
				})
				.set(MetaObjectUtils.dirFromArray(TypeUserdata.ATTR_TYPE_NAME, ATTR_FIELDS))
				.build();
	}

	private static class AltValue {

		private final AltTypeVariant variant;

		private final List<TypedValue> values;

		public AltValue(AltTypeVariant variant, List<TypedValue> values) {
			this.variant = variant;
			this.values = values;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((values == null)? 0 : values.hashCode());
			result = prime * result + ((variant == null)? 0 : variant.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;

			if (obj instanceof AltValue) {
				final AltValue other = (AltValue)obj;
				return (other.variant == this.variant) &&
						(other.values.equals(this.values));
			}

			return false;
		}

	}

	private static MetaObject createAltValueMetaObject() {
		return MetaObject.builder()
				.set(new MetaObject.SlotStr() {
					@Override
					public String str(TypedValue self, Frame<TypedValue> frame) {
						final AltValue value = self.as(AltValue.class);
						final List<String> values = Lists.newArrayList();
						for (int i = 0; i < values.size(); i++) {
							final String k = value.variant.members.get(i);
							final TypedValue v = value.values.get(i);
							values.add(k + "=" + MetaObjectUtils.callStrSlot(frame, v));
						}
						return "<alt " + value.variant.type.id + ":" + value.variant.id + "(" + Joiner.on(',').join(values) + ")>";
					}

				})
				.set(new MetaObject.SlotRepr() {
					@Override
					public String repr(TypedValue self, Frame<TypedValue> frame) {
						final AltValue value = self.as(AltValue.class);
						final List<String> values = Lists.newArrayList();
						for (int i = 0; i < values.size(); i++) {
							final String k = value.variant.members.get(i);
							final TypedValue v = value.values.get(i);
							values.add(k + "=" + MetaObjectUtils.callReprSlot(frame, v));
						}
						return value.variant.type.id + "." + value.variant.id + "(" + Joiner.on(',').join(values) + ")>";
					}
				})
				.set(new MetaObject.SlotType() {
					@Override
					public TypedValue type(TypedValue self, Frame<TypedValue> frame) {
						return self.as(AltValue.class).variant.type.selfValue;
					}
				})
				.set(new MetaObject.SlotAttr() {
					@Override
					public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
						final AltValue value = self.as(AltValue.class);
						final int memberIndex = value.variant.members.indexOf(key);
						if (memberIndex == -1) return Optional.absent();
						return Optional.of(value.values.get(memberIndex));
					}
				})
				.set(new MetaObject.SlotDir() {
					@Override
					public List<String> dir(TypedValue self, Frame<TypedValue> frame) {
						final AltValue value = self.as(AltValue.class);
						return value.variant.members;
					}
				})
				.build();
	}

	private class AltSymbol implements ICallable<TypedValue> {

		@Override
		public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			TypedCalcUtils.expectExactArgCount(argumentsCount, 2);

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
			TypedCalcUtils.expectExactReturnCount(returnsCount, substack.size());
		}

		private void prepareFrame(final SymbolMap<TypedValue> symbols, Cons vars) {
			vars.visit(new Cons.ListVisitor(nullValue) {
				@Override
				public void value(TypedValue value, boolean isLast) {
					visitTypeDefinition(symbols, value);
				}
			});
		}

		private void visitTypeDefinition(SymbolMap<TypedValue> symbols, TypedValue typeDefinition) {
			final Cons nameAndCtors = typeDefinition.as(Cons.class);
			final Symbol name = nameAndCtors.car.as(Symbol.class);
			final Cons ctors = nameAndCtors.cdr.as(Cons.class);

			final AltType newType = new AltType(name.value);
			symbols.put(name.value, newType.selfValue);

			ctors.visit(new Cons.ListVisitor(nullValue) {
				@Override
				public void value(TypedValue value, boolean isLast) {
					visitConstructor(newType.members, newType, value);
				}
			});

			for (Map.Entry<String, TypedValue> e : newType.members.entrySet())
				symbols.put(e.getKey(), e.getValue());

			newType.members.put(TypeUserdata.ATTR_TYPE_NAME, domain.create(String.class, newType.id));
		}

		private void visitConstructor(Map<String, TypedValue> variants, AltType type, TypedValue ctorDefinition) {
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

			variants.put(name.value, domain.create(AltTypeVariant.class, new AltTypeVariant(name.value, type, memberNames)));
		}

	}

	public void registerSymbol(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_ALT, new AltSymbol());
	}

}
