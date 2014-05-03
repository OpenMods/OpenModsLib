package openmods.utils.io;

import java.io.DataInput;
import java.io.IOException;

public interface IStreamReadable<T> {

	public abstract T readFromStream(DataInput input) throws IOException;

}