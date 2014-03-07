package openmods.words;

import openmods.words.Sequence.Phrase;
import openmods.words.Sequence.Word;

import org.apache.commons.lang3.text.WordUtils;

public class Words {

	private static IGenerator[] convert(Object... args) {
		IGenerator[] result = new IGenerator[args.length];
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (arg instanceof IGenerator) result[i] = (IGenerator)arg;
			else result[i] = terminal(arg);
		}
		return result;
	}

	public static IGenerator terminal(Object object) {
		return new Terminal(object.toString());
	}

	public static IGenerator alt(Object... obj) {
		return new Alternative(convert(obj));
	}

	public static IGenerator seq(Object... obj) {
		return new Phrase(convert(obj));
	}

	public static IGenerator word(Object... obj) {
		return new Word(convert(obj));
	}

	public static IGenerator capitalize(IGenerator gen) {
		return new Transformer(gen) {
			@Override
			protected String transform(String input) {
				return WordUtils.capitalize(input);
			}
		};
	}

	public static IGenerator capitalizeFully(IGenerator gen) {
		return new Transformer(gen) {
			@Override
			protected String transform(String input) {
				return WordUtils.capitalizeFully(input);
			}
		};
	}

	public static IGenerator upper(IGenerator gen) {
		return new Transformer(gen) {
			@Override
			protected String transform(String input) {
				return input.toUpperCase();
			}
		};
	}

	public static IGenerator lower(IGenerator gen) {
		return new Transformer(gen) {
			@Override
			protected String transform(String input) {
				return input.toLowerCase();
			}
		};
	}

	public static IGenerator opt(float probability, IGenerator gen) {
		return new Optional(gen, probability);
	}

	public static IGenerator sub(String key) {
		return new Substitution(key, "");
	}

	public static IGenerator sub(String key, String defaultValue) {
		return new Substitution(key, defaultValue);
	}
}
