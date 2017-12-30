package openmods.state;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.block.properties.IProperty;
import net.minecraft.util.math.Cartesian;

public class StateContainer {

	private final State baseState;

	private final List<State> allStates;

	private static final Comparator<IProperty<?>> PROPERTY_NAME_COMPARATOR = (o1, o2) -> o1.getName().compareTo(o2.getName());

	public StateContainer(IProperty<?>... properties) {
		this(Arrays.asList(properties));
	}

	public StateContainer(List<IProperty<?>> properties) {
		List<IProperty<?>> sortedProperties = Lists.newArrayList(properties);

		Collections.sort(sortedProperties, PROPERTY_NAME_COMPARATOR);

		List<Collection<? extends Comparable<?>>> allAlowedValues = Lists.newArrayList();

		for (IProperty<?> property : sortedProperties)
			allAlowedValues.add(property.getAllowedValues());

		final Map<Map<IProperty<?>, Comparable<?>>, State> valuesToStateMap = Maps.newHashMap();
		final List<State> allStates = Lists.newArrayList();

		for (List<Comparable<?>> values : Cartesian.cartesianProduct(allAlowedValues)) {
			final Map<IProperty<?>, Comparable<?>> propertyValueMap = joinLists(sortedProperties, values);
			final State itemState = new State(propertyValueMap);
			allStates.add(itemState);
			valuesToStateMap.put(propertyValueMap, itemState);
		}

		for (State state : allStates)
			state.updateNeighbours(valuesToStateMap);

		this.allStates = ImmutableList.copyOf(allStates);
		this.baseState = allStates.get(0);
	}

	private static Map<IProperty<?>, Comparable<?>> joinLists(List<IProperty<?>> properties, List<Comparable<?>> values) {
		final ImmutableMap.Builder<IProperty<?>, Comparable<?>> result = ImmutableMap.builder();

		final Iterator<IProperty<?>> keysIt = properties.iterator();
		final Iterator<Comparable<?>> valuesIt = values.iterator();

		while (keysIt.hasNext() && valuesIt.hasNext()) {
			result.put(keysIt.next(), valuesIt.next());
		}

		return result.build();
	}

	public State getBaseState() {
		return baseState;
	}

	public List<State> getAllStates() {
		return allStates;
	}

}
