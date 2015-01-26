package openmods.utils.io;

import java.io.DataOutput;
import java.io.IOException;

public interface IStreamWriter<T> {

	public abstract void writeToStream(T o, DataOutput output) throws IOException;

}