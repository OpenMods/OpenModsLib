package openmods.structured;

public interface IStructureObserver<C extends IStructureContainer<E>, E extends IStructureElement> {

	public void onContainerAdded(int containerId, C container);

	public void onElementAdded(int containerId, C container, int elementId, E element);

	public void onUpdateStarted();

	public void onDataUpdate();

	public void onStructureUpdate();

	public void onContainerUpdated(int containerId, C container);

	public void onElementUpdated(int containerId, C container, int elementId, E element);

	public void onUpdateFinished();

	public void onElementRemoved(int containerId, C container, int elementId, E element);

	public void onContainerRemoved(int containerId, C container);

}
