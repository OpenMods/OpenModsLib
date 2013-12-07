package openmods.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.*;
import java.util.Map.Entry;

public class CollectionUtils {

	public static final Random rnd = new Random();

	public static <T> T getRandom(Collection<T> collection) {
		if (collection.size() == 0) { return null; }
		int randomIndex = rnd.nextInt(collection.size());
		int i = 0;
		for (T obj : collection) {
			if (i == randomIndex) return obj;
			i = i + 1;
		}
		return null;
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

	public static void writeSortedIdList(DataOutput output, SortedSet<Integer> idList) {
		ByteUtils.writeVLI(output, idList.size());

		int currentId = 0;
		for (Integer id : idList) {
			int delta = id - currentId;
			ByteUtils.writeVLI(output, delta);
			currentId = id;
		}
	}
}
