package openmods.words;

import com.google.common.base.MoreObjects;
import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

public class Substitution implements IGenerator {

	private final String key;
	private final String defaultValue;

	public Substitution(String key, String defaultValue) {
		this.key = key;
		this.defaultValue = defaultValue;
	}

	@Override
	public String generate(Random random, Map<String, String> params) {
		return MoreObjects.firstNonNull(params.get(key), defaultValue);
	}

	@Override
	public BigInteger count() {
		return BigInteger.ONE;
	}

}
