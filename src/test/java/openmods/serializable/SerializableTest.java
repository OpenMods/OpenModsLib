package openmods.serializable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import openmods.utils.io.IStreamSerializer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class SerializableTest {

	private final SerializerRegistry registry = new SerializerRegistry();

	public <T> T testValue(T v) throws IOException {
		ByteArrayDataOutput output = ByteStreams.newDataOutput();

		registry.writeToStream(output, v);

		ByteArrayDataInput input = ByteStreams.newDataInput(output.toByteArray());

		@SuppressWarnings("unchecked")
		final Class<? extends T> cls = (Class<? extends T>)v.getClass();

		T result = registry.createFromStream(input, cls);
		Assert.assertTrue(cls.isInstance(result));
		Assert.assertEquals(result, v);
		return result;
	}

	private static IStreamSerializer<TestCls> createSerializer() throws IOException {
		IStreamSerializer<TestCls> serializer = Mockito.mock(TestSerializer.class);

		Mockito.when(serializer.readFromStream(Matchers.any(DataInput.class))).thenReturn(new TestCls());
		return serializer;
	}

	@Test
	public void testInteger() throws IOException {
		testValue(1);
	}

	@Test
	public void testString() throws IOException {
		testValue("hello");
	}

	public static class TestCls {
		@Override
		public boolean equals(Object obj) {
			return obj instanceof TestCls;
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}

	public interface TestSerializer extends IStreamSerializer<TestCls> {

	}

	@Test
	public void testRegister() throws IOException {
		TestCls testInstance = new TestCls();
		IStreamSerializer<TestCls> serializer = createSerializer();

		registry.register(serializer);

		testValue(testInstance);

		Mockito.verify(serializer).writeToStream(Matchers.eq(testInstance), Matchers.any(DataOutput.class));
		Mockito.verify(serializer).readFromStream(Matchers.any(DataInput.class));
	}

	@Test
	public void testAnonymous() throws IOException {
		TestCls testInstance = new TestCls();

		final IStreamSerializer<TestCls> wrappedSerializer = createSerializer();

		registry.register(new IStreamSerializer<TestCls>() {

			@Override
			public TestCls readFromStream(DataInput input) throws IOException {
				return wrappedSerializer.readFromStream(input);
			}

			@Override
			public void writeToStream(TestCls o, DataOutput output) throws IOException {
				wrappedSerializer.writeToStream(o, output);
			}
		});

		testValue(testInstance);

		Mockito.verify(wrappedSerializer).writeToStream(Matchers.eq(testInstance), Matchers.any(DataOutput.class));
		Mockito.verify(wrappedSerializer).readFromStream(Matchers.any(DataInput.class));
	}

	public class TestSerializable implements IStreamSerializable {

		public TestSerializable(IStreamSerializable delegate) {
			this.delegate = delegate;
		}

		public IStreamSerializable delegate;

		@Override
		public void readFromStream(DataInput input) throws IOException {
			delegate.readFromStream(input);
		}

		@Override
		public void writeToStream(DataOutput output) throws IOException {
			delegate.writeToStream(output);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TestSerializable;
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}

	@Test
	public void testClass() throws IOException {
		TestSerializable inputInstance = new TestSerializable(Mockito.mock(IStreamSerializable.class));
		final TestSerializable outputInstance = new TestSerializable(Mockito.mock(IStreamSerializable.class));

		registry.registerSerializable(new IInstanceFactory<TestSerializable>() {
			@Override
			public TestSerializable create() {
				return outputInstance;
			}
		});

		testValue(inputInstance);

		Mockito.verify(inputInstance.delegate).writeToStream(Matchers.any(DataOutput.class));
		Mockito.verify(outputInstance.delegate).readFromStream(Matchers.any(DataInput.class));
	}

	public static enum SingleClassEnum {
		A,
		B,
		C
	}

	public static enum MultipleClassEnum {
		A {},
		B {},
		C {}
	}

	@Test
	public void testEnum() throws IOException {
		testValue(SingleClassEnum.A);
		testValue(SingleClassEnum.B);
		testValue(SingleClassEnum.C);

		testValue(MultipleClassEnum.A);
		testValue(MultipleClassEnum.B);
		testValue(MultipleClassEnum.C);
	}
}
