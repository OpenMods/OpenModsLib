package openmods.calc.command;

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.Iterator;

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
}
