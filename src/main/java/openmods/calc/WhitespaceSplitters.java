package openmods.calc;

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhitespaceSplitters {

	private static class ArrayBasedWhitespaceSplitter implements IWhitespaceSplitter {
		private final Iterator<String> parts;

		public ArrayBasedWhitespaceSplitter(String[] parts) {
			this.parts = Arrays.asList(parts).iterator();
		}

		@Override
		public String getNextPart() {
			if (!parts.hasNext()) throw new CommandSyntaxException("openmodslib.command.not_enough_arguments");
			return parts.next();
		}

		@Override
		public String getTail() {
			if (!parts.hasNext()) throw new CommandSyntaxException("openmodslib.command.not_enough_arguments");
			return Joiner.on(" ").join(parts);
		}

		@Override
		public boolean isFinished() {
			return !parts.hasNext();
		}

	}

	public static IWhitespaceSplitter fromSplitArray(String... args) {
		return new ArrayBasedWhitespaceSplitter(args);
	}

	private static final Pattern SPLITTER_PATTERN = Pattern.compile("\\s*(\\S+)\\s*");

	private static class StringWhitespaceSplitter implements IWhitespaceSplitter {

		private final String contents;

		private final Matcher matcher;

		private boolean matchResult;

		public StringWhitespaceSplitter(String contents) {
			this.contents = contents;
			this.matcher = SPLITTER_PATTERN.matcher(contents);
			this.matchResult = this.matcher.find();
		}

		@Override
		public String getNextPart() {
			if (!matchResult) throw new CommandSyntaxException("openmodslib.command.not_enough_arguments");
			final String result = matcher.group(1);
			matchResult = matcher.find();
			return result;
		}

		@Override
		public String getTail() {
			if (!matchResult) throw new CommandSyntaxException("openmodslib.command.not_enough_arguments");
			matchResult = false;
			return contents.substring(matcher.start(1));
		}

		@Override
		public boolean isFinished() {
			return !matchResult;
		}

	}

	public static IWhitespaceSplitter fromString(String contents) {
		return new StringWhitespaceSplitter(contents);
	}
}
