package openmods.serializable;

import java.io.DataInput;
import java.io.IOException;

public interface IStreamReadable {

	public void readFromStream(DataInput input) throws IOException;

}
