package openmods.structured;

import java.io.DataInput;
import java.io.IOException;
import java.util.*;

import openmods.structured.Command.ConsistencyCheck;
import openmods.structured.Command.ContainerInfo;
import openmods.structured.Command.Create;
import openmods.structured.Command.Delete;
import openmods.structured.Command.Reset;
import openmods.structured.Command.Update;

import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

public abstract class StructuredDataSlave<C extends IStructureContainer<E>, E extends IStructureElement> extends StructuredData<C, E> {

	public static class ConsistencyCheckFailed extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ConsistencyCheckFailed(String message) {
			super(message);
		}

		public ConsistencyCheckFailed(String format, Object... args) {
			super(String.format(format, args));
		}

		public ConsistencyCheckFailed(Throwable cause, String format, Object... args) {
			super(String.format(format, args), cause);
		}
	}

	public final IStructureContainerFactory<C> factory;

	protected StructuredDataSlave(IStructureContainerFactory<C> factory) {
		this.factory = factory;
	}

	protected abstract void onConsistencyCheckFail();

	protected void onElementUpdated(E element) {}

	protected void onUpdate() {}

	public void interpretCommandList(List<Command> commands) {
		Multimap<Integer, Integer> updatedContainers = HashMultimap.create();

		for (Command c : commands) {
			try {
				if (c.isEnd()) break;
				else if (c instanceof ConsistencyCheck) {
					final ConsistencyCheck msg = (ConsistencyCheck)c;

					SortedSet<Integer> containers = containerToElement.keySet();

					final int containerCount = containers.size();
					final int minContainerId = containerCount == 0? 0 : containers.first();
					final int maxContainerId = containerCount == 0? 0 : containers.last();
					final int elementCount = elements.size();
					final int minElementId = elementCount == 0? 0 : elements.firstKey();
					final int maxElementId = elementCount == 0? 0 : elements.lastKey();

					if (msg.containerCount != containerCount ||
							msg.minContainerId != minContainerId ||
							msg.maxContainerId != maxContainerId ||
							msg.elementCount != elementCount ||
							msg.minElementId != minElementId ||
							msg.maxElementId != maxElementId) throw new ConsistencyCheckFailed("Validation packet not matched");
				} else if (c instanceof Reset) {
					reset();
				} else if (c instanceof Create) {
					final Create msg = (Create)c;

					SortedSet<Integer> containers = Sets.newTreeSet();
					SortedSet<Integer> elements = Sets.newTreeSet();

					final ByteArrayDataInput input = ByteStreams.newDataInput(msg.containerPayload);

					for (ContainerInfo info : msg.containers) {
						SortedSet<Integer> newElementsId = createAndAddContainer(input, info.type, info.id, info.start);
						elements.addAll(newElementsId);
						updatedContainers.putAll(info.id, newElementsId);
						containers.add(info.id);
					}

					if (input.skipBytes(1) != 0) throw new ConsistencyCheckFailed("Container payload not fully consumed");

					readElementPayload(elements, msg.elementPayload);

				} else if (c instanceof Delete) {
					final Delete msg = (Delete)c;
					for (int i : msg.idList)
						removeContainer(i);

				} else if (c instanceof Update) {
					final Update msg = (Update)c;
					readElementPayload(msg.idList, msg.elementPayload);

					for (Integer elementId : msg.idList) {
						Integer containerId = elementToContainer.get(elementId);
						if (containerId == null) throw new ConsistencyCheckFailed("Orphaned element %d", elementId);
						updatedContainers.put(containerId, elementId);
					}
				}
			} catch (ConsistencyCheckFailed e) {
				onConsistencyCheckFail();
				break;
			}
		}

		if (!updatedContainers.isEmpty()) onUpdate();

		for (Map.Entry<Integer, Collection<Integer>> e : updatedContainers.asMap().entrySet()) {
			final C container = containers.get(e.getKey());
			container.onUpdate();

			for (Integer elementId : e.getValue()) {
				final E element = elements.get(elementId);
				onElementUpdated(element);
				container.onElementUpdated(element);
			}
		}
	}

	private SortedSet<Integer> createAndAddContainer(ByteArrayDataInput input, int type, int containerId, int start) {
		C container = factory.createContainer(type);
		try {
			if (container instanceof ICustomCreateData) ((ICustomCreateData)container).readCustomDataFromStream(input);
		} catch (IOException e) {
			throw new ConsistencyCheckFailed(e, "Failed to read element %d, type %d", containerId, type);
		}
		container.setId(containerId);
		if (containerToElement.containsEntry(containerId, start)) throw new ConsistencyCheckFailed("Container %d already exists", containerId);
		addContainer(containerId, container, start);
		return containerToElement.get(containerId);
	}

	private void readElementPayload(SortedSet<Integer> ids, byte[] payload) {
		try {
			DataInput input = ByteStreams.newDataInput(payload);
			for (Integer id : ids) {
				final E element = elements.get(id);
				if (element == null) throw new ConsistencyCheckFailed("Element %d not found", id);
				element.readFromStream(input);
			}

			if (input.skipBytes(1) != 0) throw new ConsistencyCheckFailed("Element payload not fully consumed");
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}
}
