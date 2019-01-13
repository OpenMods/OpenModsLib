package openmods.utils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import net.minecraft.network.PacketBuffer;
import openmods.reflection.TypeUtils;
import openmods.utils.io.IStreamReader;
import openmods.utils.io.IStreamWriter;

public class CollectionUtils {

	public static final Random rnd = new Random();

	public static <T> T getFirst(Collection<T> collection) {
		Preconditions.checkArgument(!collection.isEmpty(), "Collection cannot be empty");
		return collection.iterator().next();
	}

	public static <T> T getRandom(Collection<T> collection) {
		return getRandom(collection, rnd);
	}

	public static <T> T getRandom(Collection<T> collection, Random rand) {
		final int size = collection.size();
		Preconditions.checkArgument(size > 0, "Can't select from empty collection");
		if (size == 1) return getFirst(collection);
		int randomIndex = rnd.nextInt(size);
		int i = 0;
		for (T obj : collection) {
			if (i == randomIndex) return obj;
			i = i + 1;
		}
		return null;
	}

	public static <T> T getRandom(List<T> list) {
		return getRandom(list, rnd);
	}

	public static <T> T getRandom(List<T> list, Random rand) {
		final int size = list.size();
		Preconditions.checkArgument(size > 0, "Can't select from empty list");
		if (size == 1) return list.get(0);
		int randomIndex = rnd.nextInt(list.size());
		return list.get(randomIndex);
	}

	public static <T> T getWeightedRandom(Map<T, Integer> collection) {
		int totalWeight = 0;
		Collection<Integer> values = collection.values();
		for (Integer i : values) {
			totalWeight += i;
		}

		int r = rnd.nextInt(totalWeight);
		for (Entry<T, Integer> entry : collection.entrySet()) {
			r -= entry.getValue();
			if (r <= 0) { return entry.getKey(); }
		}
		return null;
	}

	public static void readSortedIdList(DataInput input, Collection<Integer> output) {
		int elemCount = ByteUtils.readVLI(input);

		int currentId = 0;
		for (int i = 0; i < elemCount; i++) {
			currentId += ByteUtils.readVLI(input);
			output.add(currentId);
		}
	}

	public static void readSortedIdList(PacketBuffer input, Collection<Integer> output) {
		final int elemCount = input.readVarInt();

		int currentId = 0;
		for (int i = 0; i < elemCount; i++) {
			currentId += input.readVarInt();
			output.add(currentId);
		}
	}

	public static void writeSortedIdList(DataOutput output, SortedSet<Integer> idList) {
		ByteUtils.writeVLI(output, idList.size());

		int currentId = 0;
		for (Integer id : idList) {
			int delta = id - currentId;
			ByteUtils.writeVLI(output, delta);
			currentId = id;
		}
	}

	public static void writeSortedIdList(PacketBuffer output, SortedSet<Integer> idList) {
		output.writeVarInt(idList.size());

		int currentId = 0;
		for (Integer id : idList) {
			int delta = id - currentId;
			output.writeVarInt(delta);
			currentId = id;
		}
	}

	public static <D> void readSortedIdMap(PacketBuffer input, Map<Integer, D> output, IStreamReader<D> reader) {
		final int elemCount = input.readVarInt();

		int currentId = 0;
		try {
			for (int i = 0; i < elemCount; i++) {
				currentId += input.readVarInt();
				D data = reader.readFromStream(input);
				output.put(currentId, data);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <D> void writeSortedIdMap(PacketBuffer output, SortedMap<Integer, D> input, IStreamWriter<D> writer) {
		output.writeVarInt(input.size());

		int currentId = 0;
		try {
			for (Map.Entry<Integer, D> e : input.entrySet()) {
				final int id = e.getKey();
				final int delta = id - currentId;
				output.writeVarInt(delta);
				writer.writeToStream(e.getValue(), output);
				currentId = id;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static <A, B> Object allocateArray(Function<A, B> transformer, final int length) {
		final Class<?> transformerCls = transformer.getClass();
		Class<?> componentType = findTypeFromGenericInterface(transformerCls);
		if (componentType == null)
			componentType = findTypeFromMethod(transformerCls);

		Preconditions.checkState(componentType != null, "Failed to find type for class %s", transformer);
		return Array.newInstance(componentType, length);
	}

	private static Class<?> findTypeFromGenericInterface(Class<?> cls) {
		final TypeToken<?> token = TypeToken.of(cls);
		final TypeToken<?> typeB = token.resolveType(TypeUtils.FUNCTION_B_PARAM);
		if (typeB.getType() instanceof Class<?>) { return typeB.getRawType(); }

		return null;
	}

	private static Class<?> findTypeFromMethod(Class<?> cls) {
		for (Method m : cls.getDeclaredMethods()) {
			if (m.getName().equals("apply")) {
				final Class<?>[] parameterTypes = m.getParameterTypes();
				if (parameterTypes.length == 1) {
					final Class<?> parameterType = parameterTypes[0];
					if (parameterType != Object.class)
						return parameterType;
				}
			}
		}

		return null;
	}

	private static <B, A> void transform(A[] input, Function<A, B> transformer, final Object result) {
		for (int i = 0; i < input.length; i++) {
			final B o = transformer.apply(input[i]);
			Array.set(result, i, o);
		}
	}

	@SuppressWarnings("unchecked")
	public static <A, B> B[] transform(A[] input, Function<A, B> transformer) {
		final Object result = allocateArray(transformer, input.length);
		transform(input, transformer, result);
		return (B[])result;
	}

	@SuppressWarnings("unchecked")
	public static <A, B> B[] transform(Class<? extends B> cls, A[] input, Function<A, B> transformer) {
		final Object result = Array.newInstance(cls, input.length);
		transform(input, transformer, result);
		return (B[])result;
	}

	@SuppressWarnings("unchecked")
	public static <A, B> B[] transform(Collection<A> input, Function<A, B> transformer) {
		final Object result = allocateArray(transformer, input.size());

		int i = 0;
		for (A a : input) {
			final B o = transformer.apply(a);
			Array.set(result, i++, o);
		}

		return (B[])result;
	}

	public static <K, V> void putOnce(Map<K, V> map, K key, V value) {
		final V prev = map.put(key, value);
		Preconditions.checkState(prev == null, "Duplicate value on key %s: %s -> %s", key, prev, value);
	}

	public static <T> Set<T> asSet(Optional<T> value) {
		return value.map(Collections::singleton).orElseGet(Collections::emptySet);
	}
}
