package openmods.structured;

public class StructureObserver<C extends IStructureContainer<E>, E extends IStructureElement> implements IStructureObserver<C, E> {

	@Override
	public void onContainerAdded(int containerId, C container) {}

	@Override
	public void onElementAdded(int containerId, C container, int elementId, E element) {}

	@Override
	public void onUpdateStarted() {}

	@Override
	public void onDataUpdate() {}

	@Override
	public void onStructureUpdate() {}

	@Override
	public void onContainerUpdated(int containerId, C container) {}

	@Override
	public void onElementUpdated(int containerId, C container, int elementId, E element) {}

	@Override
	public void onUpdateFinished() {}

	@Override
	public void onElementRemoved(int containerId, C container, int elementId, E element) {}

	@Override
	public void onContainerRemoved(int containerId, C container) {}

}
