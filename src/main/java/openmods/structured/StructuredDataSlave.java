package openmods.structured;

import java.io.DataInput;
import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import openmods.structured.Command.ConsistencyCheck;
import openmods.structured.Command.ContainerInfo;
import openmods.structured.Command.Create;
import openmods.structured.Command.Delete;
import openmods.structured.Command.Reset;
import openmods.structured.Command.Update;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
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
	}

	public final IStructureContainerFactory<C> factory;

	protected StructuredDataSlave(IStructureContainerFactory<C> factory) {
		this.factory = factory;
	}

	protected abstract void onConsistencyCheckFail();

	protected void onElementUpdated(E element) {}

	public void interpretCommandList(List<Command> commands) {
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

					SortedSet<Integer> elements = Sets.newTreeSet();

					for (ContainerInfo pair : msg.containers) {
						SortedSet<Integer> newElementsId = addReplaceContainer(pair.type, pair.id, pair.start);
						elements.addAll(newElementsId);
					}

					readPayload(elements, msg.payload);
				} else if (c instanceof Delete) {
					final Delete msg = (Delete)c;
					for (int i : msg.idList)
						removeContainer(i);
				} else if (c instanceof Update) {
					final Update msg = (Update)c;
					readPayload(msg.idList, msg.payload);
				}
			} catch (ConsistencyCheckFailed e) {
				onConsistencyCheckFail();
				break;
			}
		}
	}

	protected C findContainer(E element) {
		C container = elementToContainer.get(element);
		if (container == null) throw new ConsistencyCheckFailed("Orphaned element %d", element.getId());
		return container;
	}

	private SortedSet<Integer> addReplaceContainer(int type, int containerId, int start) {
		C container = factory.createContainer(containerId, type);
		if (containerToElement.containsEntry(containerId, start)) throw new ConsistencyCheckFailed("Container %d already exists", containerId);
		addContainer(containerId, container, start);
		return containerToElement.get(containerId);
	}

	private void readPayload(SortedSet<Integer> ids, byte[] payload) {
		try {
			DataInput input = ByteStreams.newDataInput(payload);
			for (Integer id : ids) {
				final E element = elements.get(id);
				if (element == null) throw new ConsistencyCheckFailed("Element %d not found", id);
				else {
					element.readFromStream(input);
					onElementUpdated(element);

					C owner = findContainer(element);
					if (owner != null) owner.onElementUpdated(element);
				}
			}
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}
}
