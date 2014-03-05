package openmods.words;

import java.util.Random;

public class Optional implements IGenerator {

	private final IGenerator part;
	private final float probability;

	public Optional(IGenerator part, float probability) {
		this.part = part;
		this.probability = probability;
	}

	@Override
	public String generate(Random random) {
		return (random.nextFloat() < probability)? part.generate(random) : "";
	}

}
