package openmods.words;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Strings;

public class Substitution implements IGenerator {

	private final String key;

	public Substitution(String key) {
		this.key = key;
	}

	@Override
	public String generate(Random random, Map<String, String> params) {
		return Strings.nullToEmpty(params.get(key));
	}

	@Override
	public BigInteger count() {
		return BigInteger.ONE;
	}

}
