package openmods.state;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import java.util.Locale;
import java.util.Map;
import net.minecraft.block.properties.IProperty;

public class State {

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> Function<Map.Entry<IProperty<?>, Comparable<?>>, String> createConverter() {
		return entry -> {
			final IProperty<T> property = (IProperty<T>)entry.getKey();
			final T value = (T)entry.getValue();
			return property.getName() + "=" + property.getName(value);
		};
	}

	private final Map<IProperty<?>, Comparable<?>> values;
	private Table<IProperty<?>, Comparable<?>, State> neighbours;
	private final String variant;

	State(Map<IProperty<?>, Comparable<?>> values) {
		this.values = ImmutableMap.copyOf(values);

		final StringBuilder variantBuilder = new StringBuilder();
		Joiner.on(',').appendTo(variantBuilder, Iterables.transform(values.entrySet(), createConverter()));
		this.variant = variantBuilder.toString().toLowerCase(Locale.ROOT);
	}

	public <T extends Comparable<T>> T getValue(IProperty<T> property) {
		final Comparable<?> value = values.get(property);
		Preconditions.checkArgument(value != null, "Cannot get property %s", property);
		return property.getValueClass().cast(value);
	}

	public <T extends Comparable<T>, V extends T> State withProperty(IProperty<T> property, V newValue) {
		Comparable<?> value = this.values.get(property);

		Preconditions.checkArgument(value != null, "Cannot set property %s", property);

		if (newValue == value)
			return this;

		final State newState = this.neighbours.get(property, newValue);
		Preconditions.checkArgument(newState != null, "Cannot set property %s to value %s", property, newValue);
		return newState;

	}

	public String getVariant() {
		return variant;
	}

	void updateNeighbours(Map<Map<IProperty<?>, Comparable<?>>, State> valuesToStateMap) {
		Preconditions.checkState(neighbours == null);

		final Table<IProperty<?>, Comparable<?>, State> neighbours = HashBasedTable.create();

		for (Map.Entry<IProperty<?>, Comparable<?>> e : values.entrySet()) {
			final IProperty<?> property = e.getKey();
			final Comparable<?> currentValue = e.getValue();

			for (Comparable<?> value : property.getAllowedValues()) {
				if (value != currentValue) {
					neighbours.put(property, value, valuesToStateMap.get(updateSingleValue(property, value)));
				}
			}
		}

		this.neighbours = ImmutableTable.copyOf(neighbours);

	}

	private Map<IProperty<?>, Comparable<?>> updateSingleValue(IProperty<?> key, Comparable<?> value) {
		final ImmutableMap.Builder<IProperty<?>, Comparable<?>> result = ImmutableMap.builder();

		for (Map.Entry<IProperty<?>, Comparable<?>> e : values.entrySet()) {
			if (e.getKey() == key)
				result.put(key, value);
			else
				result.put(e);
		}

		return result.build();
	}

	@Override
	public String toString() {
		return "State[" + variant + "]";
	}
}
