package openmods.words;

import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;

public class Alternative implements IGenerator {

	private final List<IGenerator> alts;

	public Alternative(IGenerator... alts) {
		this.alts = ImmutableList.copyOf(alts);
	}

	@Override
	public String generate(Random random) {
		if (alts.isEmpty()) return "";
		int choice = random.nextInt(alts.size());
		return alts.get(choice).generate(random);
	}

}
