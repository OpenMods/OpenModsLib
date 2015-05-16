package openmods.serializable;

import java.io.DataOutput;
import java.io.IOException;

public interface IStreamWriteable {

	public void writeToStream(DataOutput output) throws IOException;

}
