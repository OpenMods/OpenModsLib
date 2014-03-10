package openmods.words;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

public class Range implements IGenerator {

	private final int start;
	private final int range;

	public Range(int start, int end) {
		this.start = start;
		this.range = end - start;
	}

	@Override
	public String generate(Random random, Map<String, String> params) {
		return Integer.toString(start + random.nextInt(range));
	}

	@Override
	public BigInteger count() {
		return BigInteger.valueOf(range);
	}

}
