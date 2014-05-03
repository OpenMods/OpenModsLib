package openmods.words;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Objects;

public class Substitution implements IGenerator {

	private final String key;
	private final String defaultValue;

	public Substitution(String key, String defaultValue) {
		this.key = key;
		this.defaultValue = defaultValue;
	}

	@Override
	public String generate(Random random, Map<String, String> params) {
		return Objects.firstNonNull(params.get(key), defaultValue);
	}

	@Override
	public BigInteger count() {
		return BigInteger.ONE;
	}

}
