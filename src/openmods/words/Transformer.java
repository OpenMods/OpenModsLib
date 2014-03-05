package openmods.words;

import java.util.Random;

public abstract class Transformer implements IGenerator {

	protected final IGenerator root;

	public Transformer(IGenerator root) {
		this.root = root;
	}

	protected abstract String transform(String input);

	@Override
	public String generate(Random random) {
		String result = root.generate(random);
		return transform(result);
	}

}
