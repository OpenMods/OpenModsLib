package openmods.words;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

public interface IGenerator {
	public String generate(Random random, Map<String, String> params);

	public BigInteger count();
}
