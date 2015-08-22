package openmods.structured;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface ICustomCreateData {

	public void readCustomDataFromStream(DataInput input) throws IOException;

	public void writeCustomDataFromStream(DataOutput output) throws IOException;
}
