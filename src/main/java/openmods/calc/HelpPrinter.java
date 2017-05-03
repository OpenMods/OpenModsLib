package openmods.calc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;

public class HelpPrinter {

	private final List<String> path = Lists.newArrayList();

	private boolean first = true;

	private final StringBuilder result = new StringBuilder();

	public void push(String name) {
		path.add(name);
	}

	public void pop() {
		Preconditions.checkState(!path.isEmpty());
		path.remove(path.size() - 1);
	}

	public void print(String contents) {
		if (!first)
			result.append(" OR\n");

		first = false;

		for (String p : path) {
			result.append(p);
			result.append(' ');
		}
		result.append(contents);
	}

	public String generate() {
		return result.toString();
	}

}
