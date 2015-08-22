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

	protected final IStructureObserver<C, E> observer;

	public StructuredData(IStructureObserver<C, E> observer) {
		this.observer = observer;
	}

	public StructuredData() {
		this(new StructureObserver<C, E>());
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

		final C container = containers.remove(containerId);
		observer.onContainerRemoved(containerId, container);

		for (Integer elementId : removedElements) {
			final E element = elements.remove(elementId);
			elementToContainer.remove(elementId);
			observer.onElementRemoved(containerId, container, elementId, element);
		}

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

			observer.onElementAdded(containerId, container, elementId, element);
		}
		containers.put(containerId, container);
		observer.onContainerAdded(containerId, container);

		return firstElementId;
	}
}
