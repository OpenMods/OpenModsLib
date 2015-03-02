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
import openmods.structured.Command.SetVersion;
import openmods.structured.Command.Update;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

public abstract class StructuredDataSlave<C extends IStructureContainer<E>, E extends IStructureElement> extends StructuredData<C, E> {

	public final IStructureContainerFactory<C> factory;

	protected StructuredDataSlave(IStructureContainerFactory<C> factory) {
		this.factory = factory;
	}

	protected abstract void onConsistencyCheckFail();

	protected void onElementUpdated(E element) {}

	public void interpretCommandList(List<Command> commands) {
		for (Command c : commands) {
			if (c.isEnd()) break;
			else if (c instanceof ConsistencyCheck) {
				final ConsistencyCheck msg = (ConsistencyCheck)c;

				SortedSet<Integer> containers = containerToElement.keySet();

				final int containerCount = containers.size();
				final int maxContainerId = containerCount == 0? 0 : containers.last();
				final int elementCount = elements.size();
				final int maxElementId = elementCount == 0? 0 : elements.lastKey();

				if (msg.version != version ||
						msg.containerCount != containerCount ||
						msg.maxContainerId != maxContainerId ||
						msg.elementCount != elementCount ||
						msg.maxElementId != maxElementId) {
					onConsistencyCheckFail();
					break;
				}
			} else if (c instanceof SetVersion) {
				SetVersion msg = (SetVersion)c;
				version = msg.version;
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
		}

		updateVersion(commands);
	}

	protected C findContainer(E element) {
		C container = elementToContainer.get(element);
		if (container == null) onConsistencyCheckFail();
		return container;
	}

	private SortedSet<Integer> addReplaceContainer(int type, int containerId, int start) {
		C container = factory.createContainer(containerId, type);
		if (containerToElement.containsEntry(containerId, start)) {
			onConsistencyCheckFail();
			return ImmutableSortedSet.of();
		}

		addContainer(containerId, container, start);
		return containerToElement.get(containerId);
	}

	private void readPayload(SortedSet<Integer> ids, byte[] payload) {
		try {
			DataInput input = ByteStreams.newDataInput(payload);
			for (Integer id : ids) {
				final E element = elements.get(id);
				if (element == null) onConsistencyCheckFail();
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
