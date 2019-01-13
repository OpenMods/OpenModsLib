package openmods.structured;

public interface IStructureContainer<E extends IStructureElement> {

	interface IElementAddCallback<E extends IStructureElement> {
		int addElement(E element);
	}

	int getType();

	void createElements(IElementAddCallback<E> callback);

}
