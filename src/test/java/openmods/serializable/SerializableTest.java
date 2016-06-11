package openmods.serializable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.reflect.TypeToken;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import openmods.serializable.cls.SerializableClass;
import openmods.serializable.cls.Serialize;
import openmods.utils.io.IStreamSerializer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class SerializableTest {

	private final SerializerRegistry registry = new SerializerRegistry();

	private static void assertFullyRead(ByteArrayDataInput input) {
		Assert.assertEquals(0, input.skipBytes(256));
	}

	public <T> T serializeDeserialize(Class<? extends T> cls, T value) throws IOException {
		ByteArrayDataOutput output = ByteStreams.newDataOutput();
		registry.writeToStream(output, cls, value);

		ByteArrayDataInput input = ByteStreams.newDataInput(output.toByteArray());
		final T result = registry.createFromStream(input, cls);
		assertFullyRead(input);
		return result;
	}

	public Object genericSerializeDeserialize(Type type, Object value) throws IOException {
		ByteArrayDataOutput output = ByteStreams.newDataOutput();
		registry.writeToStream(output, type, value);

		ByteArrayDataInput input = ByteStreams.newDataInput(output.toByteArray());
		final Object result = registry.createFromStream(input, type);
		assertFullyRead(input);
		return result;
	}

	public <T> T testValue(T v) throws IOException {
		@SuppressWarnings("unchecked")
		final Class<? extends T> cls = (Class<? extends T>)v.getClass();

		T result = serializeDeserialize(cls, v);
		Assert.assertTrue(cls.isInstance(result));
		Assert.assertEquals(result, v);
		return result;
	}

	public Object testValueGeneric(Type type, Object value) throws IOException {
		Object result = genericSerializeDeserialize(type, value);
		TypeToken<?> token = TypeToken.of(type);
		Assert.assertTrue(token.getRawType().isInstance(result));
		Assert.assertEquals(result, value);
		return result;
	}

	public <T> T[] testArray(T[] v) throws IOException {
		@SuppressWarnings("unchecked")
		final Class<? extends T[]> cls = (Class<? extends T[]>)v.getClass();

		T[] result = serializeDeserialize(cls, v);
		Assert.assertTrue(cls.isInstance(result));
		Assert.assertTrue(Arrays.deepEquals(v, result));
		return result;
	}

	public int[] testIntArray(int[] v) throws IOException {
		int[] result = serializeDeserialize(int[].class, v);
		Assert.assertTrue(int[].class.isInstance(result));
		Assert.assertArrayEquals(v, result);
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

	@Test
	public void testArrayPrimitive() throws IOException {
		testIntArray(new int[] {});
		testIntArray(new int[] { 1, 2, 3 });
		testIntArray(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });
	}

	@Test
	public void testArrayNullable() throws IOException {
		testArray(new String[] {});
		testArray(new String[] { "aa", "", "ccc" });
		testArray(new String[] { null });
		testArray(new String[] { "aa", null, "ccc" });
		testArray(new String[] { "a", "b", "c", "d", "e", "f", "g", "h" });
	}

	@Test
	public void testMultidimensionalArrayNullable() throws IOException {
		testArray(new String[][] {});
		testArray(new String[][] { null });
		testArray(new String[][] { {} });
		testArray(new String[][] { { null } });
		testArray(new String[][] { { "a", "b" }, {}, { "c" } });
		testArray(new String[][] { { "a", "b" }, null, { "c" } });
		testArray(new String[][] { { "a", null }, {}, { "c" } });
	}

	@Test
	public void testMultidimensionalArrayPrimitive() throws IOException {
		testArray(new int[][] {});
		testArray(new int[][] { null });
		testArray(new int[][] { {} });
		testArray(new int[][] { { 1, 2 }, null, { 3 } });
		testArray(new int[][] { { 1, 2 }, {}, { 3 } });
	}

	@Test
	public void testMultidimensionalArrayEnum() throws IOException {
		testArray(new SingleClassEnum[][] {});
		testArray(new SingleClassEnum[][] { null });
		testArray(new SingleClassEnum[][] { {} });
		testArray(new SingleClassEnum[][] { { null } });
		testArray(new SingleClassEnum[][] { { SingleClassEnum.A, SingleClassEnum.B }, null, { SingleClassEnum.C } });
		testArray(new SingleClassEnum[][] { { SingleClassEnum.A, SingleClassEnum.B }, {}, { SingleClassEnum.C } });
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	private @interface FieldTest {}

	protected void testGenericFields(Object obj) {
		for (Field f : obj.getClass().getFields()) {
			FieldTest ann = f.getAnnotation(FieldTest.class);
			final String name = f.getName();
			Preconditions.checkNotNull(ann, "Field without annotation: " + name);

			try {
				testValueGeneric(f.getGenericType(), f.get(obj));
			} catch (Exception e) {
				throw new RuntimeException("Field " + name, e);
			}
		}
	}

	public static class TestGenericList {

		@FieldTest
		public List<Integer> empty = Lists.newArrayList();

		@FieldTest
		public List<Integer> nonNull = Lists.newArrayList(1, 2, 3);

		@FieldTest
		public List<Integer> nulls = Lists.newArrayList(1, null, 2);

		@FieldTest
		public List<MultipleClassEnum> enums = Lists.newArrayList(null, MultipleClassEnum.A, MultipleClassEnum.C);

		@FieldTest
		public List<List<Integer>> nested = Lists.newArrayList();

		{
			nested.add(Lists.newArrayList(1, 2, 3));
			nested.add(null);
			nested.add(Lists.<Integer> newArrayList());
		}
	}

	@Test
	public void testGenericList() throws Exception {
		testGenericFields(new TestGenericList());
	}

	public static class TestGenericSet {

		@FieldTest
		public Set<Integer> empty = Sets.newHashSet();

		@FieldTest
		public Set<Boolean> nonNull = Sets.newHashSet(true, false, true);

		@FieldTest
		public Set<Boolean> nulls = Sets.newHashSet(false, null, true);

		@FieldTest
		public Set<MultipleClassEnum> enums = Sets.newHashSet(null, MultipleClassEnum.A, MultipleClassEnum.C);

		@FieldTest
		public Set<Set<Integer>> nested = Sets.newHashSet();

		{
			nested.add(Sets.newHashSet(1, 3, 3, null));
			nested.add(null);
			nested.add(Sets.<Integer> newHashSet());
		}
	}

	@Test
	public void testGenericSet() throws Exception {
		testGenericFields(new TestGenericSet());
	}

	public static class TestGenericMap {

		@FieldTest
		public Map<Integer, String> empty = Maps.newHashMap();

		@FieldTest
		public Map<String, Boolean> nonNull = Maps.newHashMap();

		{
			for (int i = 0; i < 11; i++)
				nonNull.put("aaaa" + i, (i & 2) == 0);
		}

		@FieldTest
		public Map<Float, String> nulls = Maps.newHashMap();

		{
			nulls.put(null, "aaaa");
			nulls.put(5.0f, null);
			nulls.put(6.0f, "zzz");

			for (int i = 0; i < 11; i++)
				nulls.put(99.0f + i, (i & 4) == 0? null : "aaa");

			nulls.put(999.0f, "bvbb");
		}

		@FieldTest
		public Map<Float, String> doubleNull = Maps.newHashMap();

		{
			doubleNull.put(null, null);
		}

		@FieldTest
		public Map<String, MultipleClassEnum> enums = ImmutableMap.of("fff", MultipleClassEnum.A, "bbb", MultipleClassEnum.C);

		@FieldTest
		public Map<Map<String, Integer>, Map<Float, Boolean>> nested = Maps.newHashMap();

		{
			nested.put(ImmutableMap.of("a", 3, "f", 5), Maps.<Float, Boolean> newHashMap());
			nested.put(null, ImmutableMap.of(4.0f, true, 5.0f, false));
			nested.put(ImmutableMap.of("zzz", 2, "ddd", 4), null);

			Map<Float, Boolean> valueWithNull = Maps.newHashMap();
			valueWithNull.put(null, true);
			valueWithNull.put(5.0f, null);
			valueWithNull.put(6.0f, false);

			Map<String, Integer> keyWithNull = Maps.newHashMap();
			keyWithNull.put(null, 4);
			keyWithNull.put("zzz", null);
			keyWithNull.put("zffd", 9);

			nested.put(keyWithNull, valueWithNull);
		}
	}

	@Test
	public void testGenericMap() throws Exception {
		testGenericFields(new TestGenericMap());
	}

	public static class TestGenericMixed {

		@FieldTest
		public List<Set<String>> listOfSets = Lists.newArrayList();

		{
			listOfSets.add(Sets.newHashSet("A", null, "c"));
			listOfSets.add(null);
			listOfSets.add(Sets.<String> newHashSet());
		}

		@FieldTest
		public List<Map<String, Integer>> listOfMaps = Lists.newArrayList();

		{
			listOfMaps.add(Maps.<String, Integer> newHashMap());

			Map<String, Integer> k = Maps.newHashMap();
			k.put(null, 3);
			k.put("zzz", null);

			listOfMaps.add(k);

			listOfMaps.add(ImmutableMap.of("a", 1, "d", 3));
		}

		@FieldTest
		public Set<List<Float>> setOfLists = Sets.newHashSet();

		{
			setOfLists.add(Lists.<Float> newArrayList());
			setOfLists.add(null);
			setOfLists.add(Lists.newArrayList(1.0f, null, 3.0f));
		}

		@FieldTest
		public Set<Map<String, Boolean>> setOfMaps = Sets.newHashSet();

		{
			setOfMaps.add(ImmutableMap.of("a", true, "d", false));

			Map<String, Boolean> k = Maps.newHashMap();
			k.put(null, true);
			k.put("zzz", null);

			setOfMaps.add(k);

			setOfMaps.add(Maps.<String, Boolean> newHashMap());
		}

		@FieldTest
		public Map<Set<Integer>, List<Boolean>> mapOfSetsToLists = Maps.newHashMap();

		{
			mapOfSetsToLists.put(Sets.newHashSet(1, 2, 3), Lists.newArrayList(false, true));
			mapOfSetsToLists.put(Sets.newHashSet(5, 9, 7), Lists.newArrayList(false, true));
			mapOfSetsToLists.put(Sets.<Integer> newHashSet(), Lists.<Boolean> newArrayList());
			mapOfSetsToLists.put(null, Lists.<Boolean> newArrayList(false, true, false));
			mapOfSetsToLists.put(Sets.newHashSet(4, 2, 5), null);
		}
	}

	@Test
	public void testGenericMixed() throws Exception {
		testGenericFields(new TestGenericMixed());
	}

	private static final int DUMMY_INT = 45;

	@SerializableClass
	public static class SimpleSerializableClass {
		@Serialize
		public int intField = 4;

		@Serialize(nullable = false)
		public String nonNullField = "hello";

		@Serialize
		public String nullField = "dummy";

		public int ignoredField = DUMMY_INT;
	}

	@Test
	public void testSerializableClass() throws IOException {
		SimpleSerializableClass target = new SimpleSerializableClass();
		target.ignoredField = DUMMY_INT + 444;
		target.intField = 6;
		target.nonNullField = "gsgfd";
		target.nullField = null;

		SimpleSerializableClass result = serializeDeserialize(SimpleSerializableClass.class, target);
		Assert.assertEquals(DUMMY_INT, result.ignoredField);
		Assert.assertEquals(target.intField, result.intField);
		Assert.assertNull(result.nullField);
		Assert.assertEquals(target.nonNullField, result.nonNullField);
	}

	@SerializableClass
	public static class NestedSerializableClass {
		@Serialize
		public int intField = 9;

		@Serialize
		public SimpleSerializableClass nonNullField = new SimpleSerializableClass();

		@Serialize
		public SimpleSerializableClass nullField = new SimpleSerializableClass();
	}

	@Test
	public void testNestedSerializableClass() throws IOException {
		NestedSerializableClass target = new NestedSerializableClass();
		target.intField = 534;
		target.nonNullField.ignoredField = DUMMY_INT + 444;
		target.nonNullField.intField = 6;
		target.nonNullField.nonNullField = "gsgfdf";
		target.nonNullField.nullField = null;
		target.nullField = null;

		NestedSerializableClass result = serializeDeserialize(NestedSerializableClass.class, target);
		Assert.assertEquals(DUMMY_INT, result.nonNullField.ignoredField);
		Assert.assertEquals(target.nonNullField.intField, result.nonNullField.intField);
		Assert.assertNull(result.nonNullField.nullField);
		Assert.assertEquals(target.nonNullField.nonNullField, result.nonNullField.nonNullField);

		Assert.assertEquals(target.intField, result.intField);
		Assert.assertNull(result.nullField);
	}
}
