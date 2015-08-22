package openmods.structured;

import java.util.List;

public interface IStructureContainer<E extends IStructureElement> {

	public int getType();

	public List<E> createElements();

}
