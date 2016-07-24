package openmods.calc.parsing;

import java.util.EnumSet;
import java.util.Set;

public class AstCompilerBehaviour {

	private enum Flags {
		QUOTE
	}

	public static final AstCompilerBehaviour NORMAL = new AstCompilerBehaviour(EnumSet.noneOf(Flags.class));
	public static final AstCompilerBehaviour QUOTED = new AstCompilerBehaviour(EnumSet.of(Flags.QUOTE));

	private final Set<Flags> commands;

	private AstCompilerBehaviour(Set<Flags> flags) {
		this.commands = flags;
	}

	public boolean shouldQuote() {
		return commands.contains(Flags.QUOTE);
	}
}