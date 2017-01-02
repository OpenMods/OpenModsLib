package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.UnaryOperator;
import openmods.calc.Value;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SymbolGetNode;
import openmods.calc.parsing.UnaryOpNode;

public class ClosureCompilerHelper {
	private final TypeDomain domain;
	private final UnaryOperator<TypedValue> varArgMarker;

	public ClosureCompilerHelper(TypeDomain domain, UnaryOperator<TypedValue> varArgMarker) {
		this.domain = domain;
		this.varArgMarker = varArgMarker;
	}

	public void compile(List<IExecutable<TypedValue>> output, Iterable<IExprNode<TypedValue>> args, IExprNode<TypedValue> lambdaBody) {
		final Optional<String> varArgName = compileArgs(output, args);
		if (varArgName.isPresent())
			compileClosureVarCall(output, lambdaBody, varArgName.get());
		else
			compileClosureCall(output, lambdaBody);
	}

	private Optional<String> compileArgs(List<IExecutable<TypedValue>> output, Iterable<IExprNode<TypedValue>> args) {
		Optional<String> varArgName = Optional.absent();
		int count = 0;
		for (IExprNode<TypedValue> arg : args) {
			final Optional<String> newVarArgName = tryExtractVarArgName(arg);

			if (!newVarArgName.isPresent()) {
				if (varArgName.isPresent())
					throw new IllegalStateException("Positional args after vararg: " + varArgName.get());

				Preconditions.checkState(!varArgName.isPresent(), "", varArgName);
				extractPatternFromNode(output, arg);
				count++;
			} else {
				if (varArgName.isPresent())
					throw new IllegalStateException("Duplicate vararg: " + varArgName.get() + " - > " + newVarArgName.get());

				varArgName = newVarArgName;
			}
		}
		output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, count, 1));
		return varArgName;
	}

	private Optional<String> tryExtractVarArgName(IExprNode<TypedValue> arg) {
		if (arg instanceof UnaryOpNode) {
			final UnaryOpNode<TypedValue> opNode = (UnaryOpNode<TypedValue>)arg;
			if (opNode.operator == varArgMarker) {
				final SymbolGetNode<TypedValue> argNameNode = (SymbolGetNode<TypedValue>)opNode.argument;
				return Optional.of(argNameNode.symbol());
			}
		}
		return Optional.absent();
	}

	private void extractPatternFromNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> arg) {
		if (arg instanceof SymbolGetNode) {
			// optimization - single variable -> use symbol
			final SymbolGetNode<TypedValue> var = (SymbolGetNode<TypedValue>)arg;
			output.add(Value.create(Symbol.get(domain, var.symbol())));
		} else {
			output.add(Value.create(Code.flattenAndWrap(domain, arg)));
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_PATTERN, 1, 1));
		}
	}

	private void compileClosureCall(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> lambdaBody) {
		flattenClosureCode(output, lambdaBody);
		output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_CLOSURE, 2, 1));
	}

	private void compileClosureVarCall(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> lambdaBody, String varArgName) {
		output.add(Value.create(domain.create(String.class, varArgName)));
		flattenClosureCode(output, lambdaBody);
		output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_CLOSURE_VAR, 3, 1));
	}

	private void flattenClosureCode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> lambdaBody) {
		if (lambdaBody instanceof RawCodeExprNode) {
			lambdaBody.flatten(output);
		} else {
			output.add(Value.create(Code.flattenAndWrap(domain, lambdaBody)));
		}
	}
}