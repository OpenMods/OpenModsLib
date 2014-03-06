package openmods.words;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

public class Optional implements IGenerator {

	private final IGenerator part;
	private final float probability;

	public Optional(IGenerator part, float probability) {
		this.part = part;
		this.probability = probability;
	}

	@Override
	public String generate(Random random, Map<String, String> params) {
		return (random.nextFloat() < probability)? part.generate(random, params) : "";
	}

	@Override
	public BigInteger count() {
		return part.count().add(BigInteger.ONE);
	}

}
