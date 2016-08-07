package openmods.calc;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;

public class ExecutableList<E> implements IExecutable<E> {

	private final List<IExecutable<E>> commands;

	public ExecutableList(List<IExecutable<E>> commands) {
		this.commands = ImmutableList.copyOf(commands);
	}

	public ExecutableList(IExecutable<E>... commands) {
		this.commands = ImmutableList.copyOf(commands);
	}

	public List<IExecutable<E>> getCommands() {
		return commands;
	}

	@Override
	public void execute(ICalculatorFrame<E> frame) {
		for (IExecutable<E> e : commands)
			e.execute(frame);
	}

	private static final Function<IExecutable<?>, String> SERIALIZER = new Function<IExecutable<?>, String>() {
		@Override
		public String apply(IExecutable<?> input) {
			return input.serialize();
		}
	};

	@Override
	public String serialize() {
		return Joiner.on(' ').join(Iterables.transform(commands, SERIALIZER));
	}

	@Override
	public String toString() {
		return "{" + Joiner.on(' ').join(commands) + "}";
	}

	@Override
	public int hashCode() {
		return 31 + commands.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj ||
				((obj instanceof ExecutableList) && ((ExecutableList<?>)obj).commands.equals(this.commands));
	}

	public void deepFlatten(List<IExecutable<E>> output) {
		for (IExecutable<E> e : commands) {
			if (e instanceof ExecutableList) {
				((ExecutableList<E>)e).deepFlatten(output);
			} else {
				output.add(e);
			}
		}
	}

	public static <E> IExecutable<E> wrap(List<IExecutable<E>> exprs) {
		if (exprs.size() == 0) return new NoopExecutable<E>();
		if (exprs.size() == 1) return exprs.get(0);
		return new ExecutableList<E>(exprs);
	}
}
