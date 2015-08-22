package openmods.structured;

import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import openmods.structured.IStructureContainer.IElementAddCallback;

import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultimap;

public abstract class StructuredData<C extends IStructureContainer<E>, E extends IStructureElement> {
	protected static final int NULL = -1;

	protected final SortedMap<Integer, E> elements = Maps.newTreeMap();
	protected final SortedMap<Integer, C> containers = Maps.newTreeMap();
	protected final TreeMultimap<Integer, Integer> containerToElement = TreeMultimap.create();
	protected final TIntIntHashMap elementToContainer = new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, NULL, NULL);

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

	public void removeAll() {
		for (Map.Entry<Integer, C> c : containers.entrySet()) {
			final int containerId = c.getKey();
			final C container = c.getValue();
			observer.onContainerRemoved(containerId, container);

			for (Integer elementId : containerToElement.get(containerId)) {
				E element = elements.get(elementId);
				Preconditions.checkNotNull(element);
				observer.onElementRemoved(containerId, container, elementId, element);
			}
		}

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

	protected int addContainer(final int containerId, final C container, int firstElementId) {
		final MutableInt nextElementId = new MutableInt(firstElementId);

		container.createElements(new IElementAddCallback<E>() {
			@Override
			public int addElement(E element) {
				final int elementId = nextElementId.intValue();
				nextElementId.increment();

				elements.put(elementId, element);
				containerToElement.put(containerId, elementId);
				elementToContainer.put(elementId, containerId);

				observer.onElementAdded(containerId, container, elementId, element);

				return elementId;
			}
		});

		containers.put(containerId, container);
		observer.onContainerAdded(containerId, container);

		return nextElementId.intValue();
	}
}
