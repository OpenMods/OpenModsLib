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

	public Optional<String> compileMultipleArgs(List<IExecutable<TypedValue>> output, Iterable<IExprNode<TypedValue>> args) {
		Optional<String> varArgName = Optional.absent();
		int count = 0;
		for (IExprNode<TypedValue> arg : args) {
			final Optional<String> newVarArgName = tryExtractVarArg(arg);

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

	public Optional<String> compileSingleArg(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> arg) {
		Optional<String> varArgName = tryExtractVarArg(arg);
		if (!varArgName.isPresent()) {
			extractPatternFromNode(output, arg);
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, 1, 1));
		} else {
			// dummy arg list
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, 0, 1));
		}
		return varArgName;
	}

	public void compileClosureCall(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> lambdaBody, Optional<String> varArgName) {
		if (varArgName.isPresent())
			output.add(Value.create(domain.create(String.class, varArgName.get())));

		flattenClosureCode(output, lambdaBody);

		if (varArgName.isPresent())
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_CLOSURE_VAR, 3, 1));
		else
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_CLOSURE, 2, 1));
	}

	private Optional<String> tryExtractVarArg(IExprNode<TypedValue> arg) {
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

	private void flattenClosureCode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> lambdaBody) {
		if (lambdaBody instanceof RawCodeExprNode) {
			lambdaBody.flatten(output);
		} else {
			output.add(Value.create(Code.flattenAndWrap(domain, lambdaBody)));
		}
	}
}