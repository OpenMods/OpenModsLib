package openmods.structured;

import java.util.*;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultimap;

public abstract class StructuredData<C extends IStructureContainer<E>, E extends IStructureElement> {
	protected final SortedMap<Integer, E> elements = Maps.newTreeMap();
	protected final SortedMap<Integer, C> containers = Maps.newTreeMap();
	protected final TreeMultimap<Integer, Integer> containerToElement = TreeMultimap.create();
	protected final Map<Integer, Integer> elementToContainer = Maps.newHashMap();

	public boolean isEmpty() {
		return elements.isEmpty() && containers.isEmpty();
	}

	public void reset() {
		elements.clear();
		containers.clear();
		containerToElement.clear();
		elementToContainer.clear();
	}

	protected SortedSet<Integer> removeContainer(int containerId) {
		Preconditions.checkArgument(containerToElement.containsKey(containerId), "Container %s doesn't exists", containerId);
		SortedSet<Integer> removedElements = containerToElement.removeAll(containerId);

		for (Integer id : removedElements) {
			E element = elements.remove(id);
			elementToContainer.remove(element);
		}

		containers.remove(containerId);

		return removedElements;
	}

	protected int addContainer(int containerId, C container, int firstElementId) {
		final List<E> newElements = container.createElements();
		Preconditions.checkArgument(!newElements.isEmpty(), "New container %s has no elements", container);
		for (E element : newElements) {
			int elementId = firstElementId++;
			elements.put(elementId, element);
			containerToElement.put(containerId, elementId);
			elementToContainer.put(elementId, containerId);
			element.setId(elementId);

			container.onElementAdded(element);
		}
		containers.put(containerId, container);
		return firstElementId;
	}
}
