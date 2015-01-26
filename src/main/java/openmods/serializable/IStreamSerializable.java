package openmods.serializable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface IStreamSerializable {
	public void readFromStream(DataInput input) throws IOException;

	public void writeToStream(DataOutput output) throws IOException;
}
