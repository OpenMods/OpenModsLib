package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Locale;
import java.util.Map;
import openmods.calc.types.multi.MetaObject.Builder;
import openmods.calc.types.multi.MetaObject.Slot;
import openmods.calc.types.multi.MetaObject.SlotAdapter;
import openmods.calc.types.multi.MetaObject.SlotField;
import openmods.reflection.TypeVariableHolder;

public class MetaObjectInfo {

	@TypeVariableHolder(SlotAdapter.class)
	public static class SlotAdapterVars {
		public static TypeVariable<?> T;
	}

	public static class SlotAccess {
		public final String name;
		public final Predicate<MetaObject> isPresent;
		public final SlotAdapter<Slot> adapter;

		private final Field field;
		private final Method builderMethod;

		public SlotAccess(final Field field, Method builderMethod, String name, SlotAdapter<Slot> adapter) {
			this.field = field;
			this.name = name;

			this.isPresent = new Predicate<MetaObject>() {
				@Override
				public boolean apply(MetaObject input) {
					try {
						return field.get(input) != null;
					} catch (Exception e) {
						throw Throwables.propagate(e);
					}
				}
			};

			this.adapter = adapter;
			this.builderMethod = builderMethod;
		}

		public Slot get(MetaObject mo) {
			try {
				return (Slot)field.get(mo);
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}
		}

		public void set(MetaObject.Builder builder, Slot slot) {
			try {
				builderMethod.invoke(builder, slot);
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}
		}
	}

	public static final Map<String, SlotAccess> slots;

	static {
		final ImmutableMap.Builder<String, SlotAccess> slotsBuilder = ImmutableMap.builder();

		final Map<Class<?>, Method> builderMethods = Maps.newHashMap();
		for (Method m : Builder.class.getDeclaredMethods()) {
			if (m.getName().equals("set")) {
				final Class<?>[] parameterTypes = m.getParameterTypes();
				Preconditions.checkState(parameterTypes.length == 1, "Invalid builder method: %s", m);
				final Class<?> slotCls = parameterTypes[0];
				Preconditions.checkState(Slot.class.isAssignableFrom(slotCls), "Invalid builder method: %s", m);
				builderMethods.put(slotCls, m);
			}
		}

		for (Field f : MetaObject.class.getDeclaredFields()) {
			final SlotField annotation = f.getAnnotation(SlotField.class);
			if (annotation != null) {
				final String fieldName = f.getName();
				if (!fieldName.startsWith("slot")) throw new AssertionError("Invalid slot name: " + fieldName);
				final String slotName = fieldName.substring("slot".length());
				final String lcSlotName = slotName.toLowerCase(Locale.ROOT);
				final Class<?> slotCls = f.getType();
				final SlotAdapter<Slot> adapter = createAdapterInstance(slotCls, annotation);
				final Method builderMethod = builderMethods.get(slotCls);
				Preconditions.checkState(builderMethod != null, "Missing builder method for %s", lcSlotName);
				slotsBuilder.put(lcSlotName, new SlotAccess(f, builderMethod, lcSlotName, adapter));
			}
		}

		slots = slotsBuilder.build();
	}

	@SuppressWarnings("unchecked")
	private static SlotAdapter<Slot> createAdapterInstance(Class<?> slotCls, SlotField annotation) {
		final Class<? extends SlotAdapter<? extends Slot>> adapterCls = annotation.adapter();
		final Class<?> slotAdapterTarget = TypeToken.of(adapterCls).resolveType(SlotAdapterVars.T).getRawType();
		Preconditions.checkState(slotAdapterTarget == slotCls, "Invalid slot adapter type: expected %s, got %s", slotCls, slotAdapterTarget);
		try {

			return (SlotAdapter<Slot>)adapterCls.newInstance();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

}
