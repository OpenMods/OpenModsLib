package openmods.structured;

import java.io.IOException;
import java.util.*;

import openmods.structured.Command.ConsistencyCheck;
import openmods.structured.Command.ContainerInfo;
import openmods.structured.Command.Create;
import openmods.structured.Command.Delete;
import openmods.structured.Command.SetVersion;
import openmods.structured.Command.UpdateSingle;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class StructuredDataMaster<C extends IStructureContainer<E>, E extends IStructureElement> extends StructuredData<C, E> {
	private Set<Integer> newContainers = Sets.newTreeSet();
	private Set<Integer> deletedContainers = Sets.newTreeSet();
	private Set<Integer> modifiedElements = Sets.newTreeSet();
	private byte checkCount;

	private int elementCounter;
	private int containerCounter;

	public synchronized void appendUpdateCommands(List<Command> commands) {
		boolean addCheck = (checkCount++) % 10 == 0;

		if (!deletedContainers.isEmpty()) {
			addCheck = true;
			Command.Delete delete = new Delete();
			delete.idList.addAll(deletedContainers);
			commands.add(delete);
			newContainers.removeAll(deletedContainers);
		}

		if (!newContainers.isEmpty()) {
			addCheck = true;
			Set<Integer> newElements = appendContainersCreate(commands, newContainers);
			modifiedElements.removeAll(newElements);
		}

		if (!modifiedElements.isEmpty()) {
			Command.UpdateSingle update = new UpdateSingle();
			update.idList.addAll(modifiedElements);
			update.payload = createPayload(modifiedElements);
			commands.add(update);
		}

		updateVersion(commands);

		if (addCheck) commands.add(createConsistencyCheck());

		clearUpdates();
	}

	public synchronized void appendFullCommands(List<Command> commands) {
		commands.add(Command.RESET_INST);

		if (!containers.isEmpty()) {
			appendContainersCreate(commands, containers.keySet());
			commands.add(createConsistencyCheck());
		}

		SetVersion msg = new SetVersion();
		msg.version = version;
		commands.add(msg);

		updateVersion(commands);
		clearUpdates();
	}

	private synchronized Set<Integer> appendContainersCreate(List<Command> commands, final Set<Integer> containersToSend) {
		Set<Integer> newElements = Sets.newTreeSet();
		Command.Create create = new Create();
		for (Integer containerId : containersToSend) {
			C container = containers.get(containerId);
			SortedSet<Integer> containerContents = containerToElement.get(containerId);
			newElements.addAll(containerContents);
			int firstContainerElement = containerContents.first();
			create.containers.add(new ContainerInfo(containerId, container.getType(), firstContainerElement));
		}

		create.payload = createPayload(newElements);
		commands.add(create);
		return newElements;
	}

	private synchronized ConsistencyCheck createConsistencyCheck() {
		ConsistencyCheck check = new ConsistencyCheck();
		check.version = version;

		SortedSet<Integer> containers = containerToElement.keySet();
		if (!containers.isEmpty()) {
			check.containerCount = containers.size();
			check.maxContainerId = containers.last();
		}

		if (!elements.isEmpty()) {
			check.elementCount = elements.size();
			check.maxElementId = elements.lastKey();
		}
		return check;
	}

	@Override
	public synchronized void reset() {
		super.reset();
		clearUpdates();
	}

	public synchronized void removeAll() {
		deletedContainers.addAll(containers.keySet());
		deletedContainers.removeAll(newContainers);

		newContainers.clear();
		modifiedElements.clear();

		super.reset();
	}

	private synchronized void clearUpdates() {
		newContainers.clear();
		deletedContainers.clear();
		modifiedElements.clear();
	}

	public boolean hasUpdates() {
		return !(newContainers.isEmpty() && deletedContainers.isEmpty() && modifiedElements.isEmpty());
	}

	public synchronized void markElementModified(IStructureElement element) {
		markElementModified(element.getId());
	}

	public synchronized void markElementModified(int id) {
		Preconditions.checkArgument(elements.containsKey(id), "No element with id %s", id);
		modifiedElements.add(id);
	}

	public synchronized int addContainer(C container) {
		int containerId = containerCounter++;
		elementCounter = addContainer(containerId, container, elementCounter++);
		newContainers.add(containerId);
		return containerId;
	}

	@Override
	public synchronized SortedSet<Integer> removeContainer(int containerId) {
		SortedSet<Integer> removedElements = super.removeContainer(containerId);
		boolean isNewContainer = newContainers.remove(containerId);
		if (!isNewContainer) deletedContainers.add(containerId);

		modifiedElements.removeAll(removedElements);
		return removedElements;
	}

	private byte[] createPayload(Collection<Integer> ids) {
		try {
			ByteArrayDataOutput output = ByteStreams.newDataOutput();
			for (Integer id : ids) {
				IStructureElement element = elements.get(id);
				element.writeToStream(output);
			}

			return output.toByteArray();
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}
}
