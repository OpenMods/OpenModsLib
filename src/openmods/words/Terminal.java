package openmods.words;

import java.util.Random;

public class Terminal implements IGenerator {

	private final String value;

	public Terminal(String value) {
		this.value = value;
	}

	@Override
	public String generate(Random random) {
		return value;
	}

}
