package openmods.calc;

import java.util.List;

import com.google.common.collect.ImmutableList;

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
}
