package openmods.structured;

import java.util.List;

public interface IStructureContainer<I extends IStructureElement> {
	public int getType();

	public List<I> createElements();

	public void onElementAdded(I element, int index);
}
