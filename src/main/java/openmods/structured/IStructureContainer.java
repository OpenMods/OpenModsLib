package openmods.structured;

public interface IStructureContainer<E extends IStructureElement> {

	public interface IElementAddCallback<E extends IStructureElement> {
		public int addElement(E element);
	}

	public int getType();

	public void createElements(IElementAddCallback<E> callback);

}
