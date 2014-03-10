package openmods.words;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.ImmutableList;

public class Alternative implements IGenerator {

	private final List<IGenerator> alts;

	public Alternative(IGenerator... alts) {
		this.alts = ImmutableList.copyOf(alts);
	}

	@Override
	public String generate(Random random, Map<String, String> params) {
		if (alts.isEmpty()) return "";
		int choice = random.nextInt(alts.size());
		return alts.get(choice).generate(random, params);
	}

	@Override
	public BigInteger count() {
		BigInteger result = BigInteger.ZERO;
		for (IGenerator alt : alts)
			result = result.add(alt.count());
		return result;
	}

}
