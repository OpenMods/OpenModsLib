package openmods.utils.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface IStreamSerializable<T> {
	public void writeToStream(T o, DataOutput output) throws IOException;

	public T readFromStream(DataInput input) throws IOException;

}