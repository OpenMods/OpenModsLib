package openmods.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

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
		    if (r <= 0) {
		    	return entry.getKey();
		    }
		}
		return null;
	}
}
