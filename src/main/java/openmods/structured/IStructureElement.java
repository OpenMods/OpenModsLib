package openmods.structured;

import openmods.serializable.IStreamSerializable;

public interface IStructureElement extends IStreamSerializable {
	public int getId();

	public void setId(int id);
}
