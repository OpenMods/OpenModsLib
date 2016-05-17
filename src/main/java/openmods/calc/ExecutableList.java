package openmods.calc;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

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

}
