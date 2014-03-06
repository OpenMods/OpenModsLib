package openmods.words;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

public abstract class Transformer implements IGenerator {

	protected final IGenerator root;

	public Transformer(IGenerator root) {
		this.root = root;
	}

	protected abstract String transform(String input);

	@Override
	public String generate(Random random, Map<String, String> params) {
		String result = root.generate(random, params);
		return transform(result);
	}

	@Override
	public BigInteger count() {
		return root.count();
	}
}
