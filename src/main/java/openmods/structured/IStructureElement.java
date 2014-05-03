package openmods.structured;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface IStructureElement {
	public void writeToStream(DataOutput output) throws IOException;

	public void readFromStream(DataInput input) throws IOException;
}
