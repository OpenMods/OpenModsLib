package openmods.calc.parsing;

import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.ExecutableList;
import openmods.calc.IExecutable;

public class ExprUtils {

	public static <E> IExecutable<E> flattenNode(IExprNode<E> node) {
		final List<IExecutable<E>> commands = Lists.newArrayList();
		node.flatten(commands);
		return ExecutableList.wrap(commands);
	}

}
