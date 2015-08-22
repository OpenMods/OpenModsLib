package openmods.structured;

import java.util.List;

public interface IStructureContainer<E extends IStructureElement> {
	public int getId();

	public void setId(int id);

	public int getType();

	public List<E> createElements();

	public void onElementAdded(E element);

	public void onUpdate();

	public void onElementUpdated(E element);
}
