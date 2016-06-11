package openmods.words;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class Sequence implements IGenerator {

	protected final List<IGenerator> parts;

	public Sequence(IGenerator... parts) {
		this.parts = ImmutableList.copyOf(parts);
	}

	protected List<String> generateParts(Random random, Map<String, String> params) {
		List<String> result = Lists.newArrayList();
		for (IGenerator part : parts)
			result.add(part.generate(random, params));
		return result;
	}

	@Override
	public BigInteger count() {
		BigInteger result = BigInteger.ONE;
		for (IGenerator part : parts)
			result = result.multiply(part.count());
		return result;
	}

	public static class Phrase extends Sequence {
		public Phrase(IGenerator... parts) {
			super(parts);
		}

		@Override
		public String generate(Random random, Map<String, String> params) {
			List<String> results = Lists.newArrayList();

			for (IGenerator part : parts) {
				String result = part.generate(random, params);
				if (!Strings.isNullOrEmpty(result)) results.add(result);
			}

			return Joiner.on(' ').join(results);
		}
	}

	public static class Word extends Sequence {
		public Word(IGenerator... parts) {
			super(parts);
		}

		@Override
		public String generate(Random random, Map<String, String> params) {
			StringBuilder builder = new StringBuilder();
			for (IGenerator part : parts)
				builder.append(part.generate(random, params));
			return builder.toString();
		}
	}
}
