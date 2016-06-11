package openmods.serializable;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import openmods.serializable.cls.ClassSerializersProvider;
import openmods.serializable.cls.Serialize;
import org.junit.Assert;
import org.junit.Test;

public class ClassSerializerTest {

	private static final int DUMMY_INT = -1;

	private static <T> void testSerializer(final IObjectSerializer<T> serializer, T source, T target) {
		try {
			ByteArrayDataOutput output = ByteStreams.newDataOutput();
			serializer.writeToStream(source, output);
			ByteArrayDataInput input = ByteStreams.newDataInput(output.toByteArray());
			serializer.readFromStream(target, input);
			assertFullyRead(input);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void assertFullyRead(ByteArrayDataInput input) {
		Assert.assertEquals(0, input.skipBytes(256));
	}

	public static class NonNullableClass {
		public int notSerialized = DUMMY_INT;

		@Serialize(nullable = false)
		public String stringField = "dummy";

		@Serialize(nullable = false)
		public int intField = -2;
	}

	@Test
	public void testNonNullable() {
		IObjectSerializer<NonNullableClass> serializer = ClassSerializersProvider.instance.getSerializer(NonNullableClass.class);

		NonNullableClass source = new NonNullableClass();
		source.intField = 4;
		source.stringField = "blarg";
		source.notSerialized = 999;

		NonNullableClass target = new NonNullableClass();

		testSerializer(serializer, source, target);

		Assert.assertEquals(source.stringField, target.stringField);
		Assert.assertEquals(source.intField, target.intField);
		Assert.assertEquals(DUMMY_INT, target.notSerialized);
	}

	@Test(expected = NullPointerException.class)
	public void testNonNullableFail() {
		IObjectSerializer<NonNullableClass> serializer = ClassSerializersProvider.instance.getSerializer(NonNullableClass.class);

		NonNullableClass source = new NonNullableClass();
		source.stringField = null;

		NonNullableClass target = new NonNullableClass();

		testSerializer(serializer, source, target);
	}

	public static class NullableClass {
		public int notSerialized = DUMMY_INT;

		@Serialize
		public String stringField = "dummy";

		@Serialize
		public String nullField = "dummy2";

		@Serialize
		public int intField = -2;
	}

	@Test
	public void testNullable() {
		IObjectSerializer<NullableClass> serializer = ClassSerializersProvider.instance.getSerializer(NullableClass.class);

		NullableClass source = new NullableClass();
		source.intField = 4;
		source.stringField = "blarg";
		source.nullField = null;
		source.notSerialized = 999;

		NullableClass target = new NullableClass();

		testSerializer(serializer, source, target);

		Assert.assertEquals(source.stringField, target.stringField);
		Assert.assertEquals(source.intField, target.intField);
		Assert.assertEquals(source.nullField, target.nullField);
		Assert.assertEquals(DUMMY_INT, target.notSerialized);
	}

	public static class CompatibleSourceClass {
		@Serialize(rank = 1, nullable = false)
		public int field1 = 10;

		public boolean field2A = false;

		@Serialize(rank = 2)
		public boolean field2 = true;

		@Serialize(rank = 3)
		public String field3 = "hello";

		@Serialize(rank = 4)
		public int field4 = 6210;
	}

	public static class CompatibleTargetClass {
		@Serialize(rank = 4)
		public int field0 = 6532;

		@Serialize(rank = 2)
		public boolean field1 = false;

		@Serialize(rank = 3)
		public String field2 = "zomg";

		public String field2A = "fff";

		@Serialize(rank = 1)
		public int field3 = 4424;
	}

	@Test
	public void testReorderedClasses() throws IOException {
		IObjectSerializer<CompatibleSourceClass> serializerA = ClassSerializersProvider.instance.getSerializer(CompatibleSourceClass.class);

		CompatibleSourceClass source = new CompatibleSourceClass();
		ByteArrayDataOutput output = ByteStreams.newDataOutput();
		serializerA.writeToStream(source, output);

		IObjectSerializer<CompatibleTargetClass> serializerB = ClassSerializersProvider.instance.getSerializer(CompatibleTargetClass.class);

		CompatibleTargetClass target = new CompatibleTargetClass();
		ByteArrayDataInput input = ByteStreams.newDataInput(output.toByteArray());
		serializerB.readFromStream(target, input);

		assertFullyRead(input);

		Assert.assertEquals(source.field1, target.field3);
		Assert.assertEquals(source.field2, target.field1);
		Assert.assertEquals(source.field3, target.field2);
		Assert.assertEquals(source.field4, target.field0);
	}

	public static class GenericBase1<A, B> {
		@Serialize
		public A fieldA;

		@Serialize
		public B fieldB;
	}

	public static class GenericBase2<A, B, C, D> extends GenericBase1<C, D> {
		@Serialize
		public A fieldC;

		@Serialize
		public B fieldD;
	}

	public static class GenericDerrived extends GenericBase2<Integer, String, Boolean, Float> {

	}

	@Test
	public void testClassWithGenericBase() {
		IObjectSerializer<GenericDerrived> serializer = ClassSerializersProvider.instance.getSerializer(GenericDerrived.class);

		GenericDerrived source = new GenericDerrived();
		source.fieldA = false;
		source.fieldB = 3.5f;
		source.fieldC = 34;
		source.fieldD = "Hello";

		GenericDerrived target = new GenericDerrived();
		source.fieldA = true;
		source.fieldB = 99.2f;
		source.fieldC = 545;
		source.fieldD = "Bye";

		testSerializer(serializer, source, target);
	}
}
