package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.SymbolCall;
import openmods.calc.executable.UnaryOperator;
import openmods.calc.parsing.node.IExprNode;
import openmods.calc.parsing.node.UnaryOpNode;

public abstract class ArgUnpackCompilerHelper {

	private final UnaryOperator<TypedValue> unpackMarker;

	public ArgUnpackCompilerHelper(UnaryOperator<TypedValue> unpackMarker) {
		this.unpackMarker = unpackMarker;
	}

	public void compileArgUnpack(Iterable<IExprNode<TypedValue>> args) {
		final List<IExecutable<TypedValue>> compiledArgs = Lists.newArrayList();
		int normalArgCount = 0;
		int varArgCount = 0;
		int singleArgWrapCount = 0;
		boolean wrapSingleArgs = false;
		for (IExprNode<TypedValue> arg : args) {
			final Optional<IExprNode<TypedValue>> unpackNode = tryExtractVarArgNode(arg);
			if (unpackNode.isPresent()) { // *arg found
				if (singleArgWrapCount > 0) { // this may be after string of normal args, e.g. *a, b, c, d, *e, so wrap those
					compiledArgs.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, singleArgWrapCount, 1));
					singleArgWrapCount = 0;
					varArgCount++;
				}

				unpackNode.get().flatten(compiledArgs);
				varArgCount++;
				wrapSingleArgs = true; // from now all args have to be lists
			} else if (wrapSingleArgs) {
				arg.flatten(compiledArgs);
				singleArgWrapCount++;
			} else {
				arg.flatten(compiledArgs);
				normalArgCount++;
			}
		}

		if (singleArgWrapCount > 0) { // leftovers, in case of *a, b, c, d
			compiledArgs.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, singleArgWrapCount, 1));
			varArgCount++;
		}

		if (varArgCount > 0) {
			if (varArgCount > 1)
				compiledArgs.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_FLATTEN, varArgCount, 1));

			compileWithVarArgs(normalArgCount, compiledArgs);
		} else {
			compileWithoutVarArgs(normalArgCount, compiledArgs);
		}
	}

	protected abstract void compileWithVarArgs(int normalArgCount, List<IExecutable<TypedValue>> compiledArgs);

	protected abstract void compileWithoutVarArgs(int argCount, List<IExecutable<TypedValue>> compiledArgs);

	private Optional<IExprNode<TypedValue>> tryExtractVarArgNode(IExprNode<TypedValue> arg) {
		if (arg instanceof UnaryOpNode) {
			final UnaryOpNode<TypedValue> opNode = (UnaryOpNode<TypedValue>)arg;
			if (opNode.operator == unpackMarker) return Optional.of(opNode.argument);
		}
		return Optional.absent();
	}
}
