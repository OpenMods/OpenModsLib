package openmods.serializable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface IObjectSerializer<T> {
	public void readFromStream(T object, DataInput input) throws IOException;

	public void writeToStream(T object, DataOutput output) throws IOException;
}
