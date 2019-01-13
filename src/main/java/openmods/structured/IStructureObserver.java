package openmods.structured;

public interface IStructureObserver<C extends IStructureContainer<E>, E extends IStructureElement> {

	void onContainerAdded(int containerId, C container);

	void onElementAdded(int containerId, C container, int elementId, E element);

	void onUpdateStarted();

	void onDataUpdate();

	void onStructureUpdate();

	void onContainerUpdated(int containerId, C container);

	void onElementUpdated(int containerId, C container, int elementId, E element);

	void onUpdateFinished();

	void onElementRemoved(int containerId, C container, int elementId, E element);

	void onContainerRemoved(int containerId, C container);

}
