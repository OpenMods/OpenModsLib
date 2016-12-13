package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import openmods.calc.BinaryFunction;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.UnaryFunction;
import openmods.calc.types.multi.MetaObject.Slot;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class MetaObjectSymbols {

	private static final String ATTR_NAME = "name";
	private static final String ATTR_INFO = "info";
	private static final String ATTR_CAPABILITIES = "slots";

	private static class SlotCheckSymbol extends BinaryFunction<TypedValue> {
		@Override
		protected TypedValue call(TypedValue left, TypedValue right) {
			final MetaObject mo = left.getMetaObject();
			final MetaObjectInfo.SlotAccess info = right.as(MetaObjectInfo.SlotAccess.class, "second 'can' argument");
			return left.domain.create(Boolean.class, info.isPresent.apply(mo));
		}
	}

	private static class MetaObjectSlot {
		private final MetaObject.Slot slot;

		private final MetaObjectInfo.SlotAccess info;

		public MetaObjectSlot(MetaObject.Slot slot, MetaObjectInfo.SlotAccess info) {
			this.slot = slot;
			this.info = info;
		}
	}

	private static MetaObject.Builder createBuilderFromArgs(Iterable<TypedValue> args) {
		final MetaObject.Builder result = MetaObject.builder();

		for (TypedValue arg : args) {
			if (arg.is(MetaObjectSlot.class)) {
				final MetaObjectSlot nativeSlot = arg.as(MetaObjectSlot.class);
				nativeSlot.info.set(result, nativeSlot.slot);
			} else if (arg.is(Cons.class)) {
				final Cons pair = arg.as(Cons.class);
				final MetaObjectInfo.SlotAccess slotInfo = pair.car.as(MetaObjectInfo.SlotAccess.class, "slot:value pair");
				final TypedValue slotValue = pair.cdr;

				if (slotValue.is(MetaObjectSlot.class)) {
					final MetaObjectSlot nativeSlot = slotValue.as(MetaObjectSlot.class);
					Preconditions.checkState(nativeSlot.info == slotInfo, "Invalid slot type for name %s, got %s", slotInfo.name, nativeSlot.info.name);
					slotInfo.set(result, nativeSlot.slot);
				} else if (MetaObjectUtils.isCallable(slotValue)) {
					final Slot slot = slotInfo.adapter.wrap(slotValue);
					slotInfo.set(result, slot);
				} else {
					throw new IllegalArgumentException("Slot value must be native slot or callable");
				}
			} else {
				throw new IllegalArgumentException("Expected native slot or slot:value pair");
			}
		}

		return result;
	}

	public static void register(Environment<TypedValue> env) {
		final TypedValue nullValue = env.nullValue();
		final TypeDomain domain = nullValue.domain;
		domain.registerType(MetaObjectInfo.SlotAccess.class, "metaobjectslot", createCapabilityMetaObject());

		{
			final TypedValue metaObjectSlotType = domain.create(TypeUserdata.class, new TypeUserdata("metaobjectslotvalue"));
			env.setGlobalSymbol("metaobjectslotvalue", metaObjectSlotType);
			domain.registerType(MetaObjectSlot.class, "metaobjectslotvalue", createMetaObjectSlotMetaObject(metaObjectSlotType));
		}

		{
			final TypedValue metaObjectType = domain.create(TypeUserdata.class, new TypeUserdata("metaobject"),
					TypeUserdata.defaultMetaObject(domain)
							.set(new MetaObject.SlotCall() {
								@Override
								public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
									Preconditions.checkState(argumentsCount.isPresent(), "'metaobject' symbol requires arguments count");
									final Stack<TypedValue> stack = frame.stack().substack(argumentsCount.get());
									final MetaObject.Builder builder = createBuilderFromArgs(stack);
									stack.clear();
									stack.push(domain.create(MetaObject.class, builder.build()));

								}
							}).build());

			env.setGlobalSymbol("metaobject", metaObjectType);
			domain.registerType(MetaObject.class, "metaobject", createMetaObjectMetaObject(domain, metaObjectType, nullValue));
		}

		final Map<String, TypedValue> slots = Maps.newHashMap();

		for (Map.Entry<String, MetaObjectInfo.SlotAccess> e : MetaObjectInfo.slots.entrySet())
			slots.put(e.getKey(), domain.create(MetaObjectInfo.SlotAccess.class, e.getValue()));

		env.setGlobalSymbol(ATTR_CAPABILITIES, domain.create(SimpleNamespace.class, new SimpleNamespace(slots)));
		env.setGlobalSymbol("has", new SlotCheckSymbol());

		env.setGlobalSymbol("getmetaobject", new UnaryFunction<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue value) {
				return value.domain.create(MetaObject.class, value.getMetaObject());
			}
		});

		env.setGlobalSymbol("setmetaobject", new BinaryFunction<TypedValue>() {
			@Override
			protected TypedValue call(TypedValue left, TypedValue right) {
				final MetaObject mo = right.as(MetaObject.class, "second 'setmetaobject' arg");
				return left.updateMetaObject(mo);
			}
		});

	}

	private static MetaObject createCapabilityMetaObject() {
		return MetaObject.builder()
				.set(new MetaObject.SlotDecompose() {
					@Override
					public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
						final MetaObjectInfo.SlotAccess info = self.as(MetaObjectInfo.SlotAccess.class);
						final MetaObject mo = input.getMetaObject();

						if (info.isPresent.apply(mo)) {
							List<TypedValue> result = ImmutableList.of(input);
							return Optional.of(result);
						}

						return Optional.absent();
					}
				})
				.set(new MetaObject.SlotStr() {
					@Override
					public String str(TypedValue self, Frame<TypedValue> frame) {
						return ATTR_CAPABILITIES + "." + self.as(MetaObjectInfo.SlotAccess.class).name;
					}
				})
				.set(new MetaObject.SlotRepr() {
					@Override
					public String repr(TypedValue self, Frame<TypedValue> frame) {
						return ATTR_CAPABILITIES + "." + self.as(MetaObjectInfo.SlotAccess.class).name;
					}
				})
				.build();
	}

	private static MetaObject createMetaObjectSlotMetaObject(TypedValue metaObjectSlotType) {
		return MetaObject.builder()
				.set(new MetaObject.SlotCall() {
					@Override
					public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
						final MetaObjectSlot slot = self.as(MetaObjectSlot.class);
						slot.info.adapter.call(slot.slot, frame, argumentsCount, returnsCount);
					}
				})
				.set(new MetaObject.SlotAttr() {
					@Override
					public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
						final MetaObjectSlot slot = self.as(MetaObjectSlot.class);
						final TypeDomain domain = self.domain;
						if (key.equals(ATTR_NAME)) return Optional.of(domain.create(String.class, slot.info.name));
						if (key.equals(ATTR_INFO)) return Optional.of(domain.create(MetaObjectInfo.SlotAccess.class, slot.info));
						return Optional.absent();
					}
				})
				.set(MetaObjectUtils.typeConst(metaObjectSlotType))
				.build();
	}

	// much meta
	private static MetaObject createMetaObjectMetaObject(final TypeDomain domain, TypedValue metaObjectType, final TypedValue nullValue) {
		return MetaObject.builder()
				.set(new MetaObject.SlotAttr() {
					@Override
					public Optional<TypedValue> attr(TypedValue self, String key, Frame<TypedValue> frame) {
						final MetaObject mo = self.as(MetaObject.class);
						final MetaObjectInfo.SlotAccess slotInfo = MetaObjectInfo.slots.get(key);
						if (slotInfo == null) return Optional.absent();

						final MetaObject.Slot slot = slotInfo.get(mo);
						final TypedValue result;
						if (slot == null) {
							result = nullValue;
						} else if (slot instanceof MetaObject.SlotWithValue) {
							result = ((MetaObject.SlotWithValue)slot).getValue();
						} else {
							result = domain.create(MetaObjectSlot.class, new MetaObjectSlot(slot, slotInfo));
						}

						return Optional.of(result);
					}
				})
				.set(new MetaObject.SlotCall() {
					@Override
					public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
						Preconditions.checkState(argumentsCount.isPresent(), "'metaobject' symbol requires arguments count");
						final MetaObject mo = self.as(MetaObject.class);
						final Stack<TypedValue> stack = frame.stack().substack(argumentsCount.get());
						final MetaObject.Builder builder = createBuilderFromArgs(stack);
						stack.clear();
						stack.push(domain.create(MetaObject.class, builder.update(mo)));
					}
				})
				.set(MetaObjectUtils.typeConst(metaObjectType))
				.build();
	}

}
