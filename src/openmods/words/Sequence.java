package openmods.words;

import java.util.List;
import java.util.Random;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public abstract class Sequence implements IGenerator {

	protected final List<IGenerator> parts;

	public Sequence(IGenerator... parts) {
		this.parts = ImmutableList.copyOf(parts);
	}

	protected List<String> generateParts(Random random) {
		List<String> result = Lists.newArrayList();
		for (IGenerator part : parts)
			result.add(part.generate(random));
		return result;
	}

	private static final Predicate<String> SKIP = new Predicate<String>() {
		@Override
		public boolean apply(String input) {
			return !Strings.isNullOrEmpty(input);
		}
	};

	public static class Phrase extends Sequence {
		public Phrase(IGenerator... parts) {
			super(parts);
		}

		@Override
		public String generate(Random random) {
			return Joiner.on(' ').join(Iterables.filter(generateParts(random), SKIP));
		}
	}

	public static class Word extends Sequence {
		public Word(IGenerator... parts) {
			super(parts);
		}

		@Override
		public String generate(Random random) {
			StringBuilder builder = new StringBuilder();
			for (IGenerator part : parts)
				builder.append(part.generate(random));
			return builder.toString();
		}
	}
}
