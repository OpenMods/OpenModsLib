package openmods.words;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

public interface IGenerator {
	String generate(Random random, Map<String, String> params);

	BigInteger count();
}
