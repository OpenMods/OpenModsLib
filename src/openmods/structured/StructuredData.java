package openmods.structured;

import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultimap;

public abstract class StructuredData<C extends IStructureContainer<E>, E extends IStructureElement> {
	protected byte version;

	protected int elementCounter;
	protected int containerCounter;

	protected final SortedMap<Integer, E> elements = Maps.newTreeMap();
	protected final SortedMap<Integer, C> containers = Maps.newTreeMap();
	protected final TreeMultimap<Integer, Integer> containerToElement = TreeMultimap.create();

	public void reset() {
		elements.clear();
		containers.clear();
		containerToElement.clear();
	}

	protected SortedSet<Integer> removeContainer(int containerId) {
		Preconditions.checkArgument(containerToElement.containsKey(containerId), "Container %s doesn't exists", containerId);
		SortedSet<Integer> removedElements = containerToElement.removeAll(containerId);

		for (Integer id : removedElements)
			elements.remove(id);

		containers.remove(containerId);

		version++;
		return removedElements;
	}

	protected int addContainer(int containerId, C container, int firstElementId) {
		final List<E> newElements = container.createElements();
		Preconditions.checkArgument(!newElements.isEmpty(), "New container %s has no elements", container);
		for (E element : newElements) {
			int elementId = firstElementId++;
			elements.put(elementId, element);
			containerToElement.put(containerId, elementId);
			container.onElementAdded(element, elementId);
		}
		containers.put(containerId, container);
		version++;
		return firstElementId;
	}

}
