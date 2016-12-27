package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import openmods.calc.BinaryOperator;
import openmods.calc.Environment;
import openmods.calc.ExecutionErrorException;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.ISymbol;
import openmods.calc.LocalSymbolMap;
import openmods.calc.SymbolCall;
import openmods.calc.SymbolMap;
import openmods.calc.UnaryOperator;
import openmods.calc.Value;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.calc.parsing.SymbolCallNode;
import openmods.calc.parsing.SymbolGetNode;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class LetExpressionFactory {

	private final TypeDomain domain;
	private final TypedValue nullValue;
	private final BinaryOperator<TypedValue> colonOperator;
	private final BinaryOperator<TypedValue> assignOperator;
	private final BinaryOperator<TypedValue> lambdaOperator;
	private final ClosureCompilerHelper closureCompiler;

	public LetExpressionFactory(TypeDomain domain, TypedValue nullValue, BinaryOperator<TypedValue> colonOperator, BinaryOperator<TypedValue> assignOperator, BinaryOperator<TypedValue> lambdaOperator, UnaryOperator<TypedValue> varArgMarker) {
		this.domain = domain;
		this.nullValue = nullValue;
		this.colonOperator = colonOperator;
		this.assignOperator = assignOperator;
		this.lambdaOperator = lambdaOperator;
		this.closureCompiler = new ClosureCompilerHelper(domain, varArgMarker);
	}

	private class LetNode extends ScopeModifierNode {
		public LetNode(String letSymbol, List<IExprNode<TypedValue>> children) {
			super(domain, letSymbol, colonOperator, assignOperator, children);
		}

		@Override
		protected void handlePairOp(List<IExecutable<TypedValue>> output, BinaryOpNode<TypedValue> opNode) {
			if (opNode.operator == lambdaOperator) {
				flattenLambdaDefinition(output, opNode);
			} else {
				throw new UnsupportedOperationException("Expected '=', ':' or '->' as pair separators, got " + opNode.operator);
			}
		}

		@Override
		protected void flattenNameAndValue(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> bindPattern, IExprNode<TypedValue> value) {
			flattenBindPattern(output, bindPattern);
			output.add(Value.create(Code.flattenAndWrap(domain, value)));
		}

		private void flattenLambdaDefinition(List<IExecutable<TypedValue>> output, BinaryOpNode<TypedValue> opNode) {
			final IExprNode<TypedValue> nameNode = opNode.left;
			final IExprNode<TypedValue> lambdaBody = opNode.right;

			final TypedValue varName;
			final Iterable<IExprNode<TypedValue>> lambdaArgs;
			if (nameNode instanceof SymbolCallNode) {
				// f(...) -> <lambda body>
				final String symbolName = ((SymbolCallNode<TypedValue>)nameNode).symbol();
				varName = Symbol.get(domain, symbolName);
				lambdaArgs = nameNode.getChildren();
			} else if (nameNode instanceof SymbolGetNode) {
				varName = Symbol.get(domain, ((SymbolGetNode<TypedValue>)nameNode).symbol());
				lambdaArgs = ImmutableList.of();
			} else {
				throw new IllegalArgumentException("Cannot extract value name from " + nameNode);
			}

			output.add(Value.create(varName));
			output.add(Value.create(createLambdaWrapperCode(lambdaArgs, lambdaBody)));
		}

		private TypedValue createLambdaWrapperCode(Iterable<IExprNode<TypedValue>> args, IExprNode<TypedValue> body) {
			final List<IExecutable<TypedValue>> result = Lists.newArrayList();
			closureCompiler.compile(result, args, body);
			return Code.wrap(domain, result);
		}

		private void flattenBindPattern(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> bindPattern) {
			if (bindPattern instanceof SymbolGetNode) {
				// optimization - single variable -> use symbol
				final SymbolGetNode<TypedValue> var = (SymbolGetNode<TypedValue>)bindPattern;
				output.add(Value.create(Symbol.get(domain, var.symbol())));
			} else {
				output.add(Value.create(Code.flattenAndWrap(domain, bindPattern)));
				output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_PATTERN, 1, 1));
			}
		}
	}

	private class LetStateTransition extends SameStateSymbolTransition<TypedValue> {
		private final String letState;

		public LetStateTransition(String letState, ICompilerState<TypedValue> parentState) {
			super(parentState);
			this.letState = letState;
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			return new LetNode(letState, children);
		}
	}

	public ISymbolCallStateTransition<TypedValue> createLetStateTransition(ICompilerState<TypedValue> parentState) {
		return new LetStateTransition(TypedCalcConstants.SYMBOL_LET, parentState);
	}

	public ISymbolCallStateTransition<TypedValue> createLetSeqStateTransition(ICompilerState<TypedValue> parentState) {
		return new LetStateTransition(TypedCalcConstants.SYMBOL_LETSEQ, parentState);
	}

	public ISymbolCallStateTransition<TypedValue> createLetRecStateTransition(ICompilerState<TypedValue> parentState) {
		return new LetStateTransition(TypedCalcConstants.SYMBOL_LETREC, parentState);
	}

	private static class PlaceholderSymbol implements ISymbol<TypedValue> {
		@Override
		public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			throw new ExecutionErrorException("Cannot call symbol during definition");
		}

		@Override
		public TypedValue get() {
			throw new ExecutionErrorException("Cannot reference symbol during definition");
		}
	}

	@SuppressWarnings("serial")
	private static class InvalidArgsException extends RuntimeException {}

	private static abstract class LetSymbolBase implements ICallable<TypedValue> {

		@Override
		public void call(Frame<TypedValue> currentFrame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			TypedCalcUtils.expectExactArgCount(argumentsCount, 2);

			final Frame<TypedValue> letFrame = FrameFactory.newLocalFrameWithSubstack(currentFrame, 2);
			final Stack<TypedValue> letStack = letFrame.stack();
			final Code code = letStack.pop().as(Code.class, "second (code) 'let' parameter");
			final Cons vars = letStack.pop().as(Cons.class, "first (var list) 'let'  parameter");

			try {
				prepareFrame(letFrame.symbols(), currentFrame.symbols(), vars);
			} catch (InvalidArgsException e) {
				throw new IllegalArgumentException("Expected list of name:value pairs on second 'let' parameter, got " + vars, e);
			}

			code.execute(letFrame);

			TypedCalcUtils.expectExactReturnCount(returnsCount, letStack.size());
		}

		protected abstract void prepareFrame(SymbolMap<TypedValue> outputFrame, SymbolMap<TypedValue> callSymbols, Cons vars);
	}

	private abstract class ArgPairVisitor extends Cons.ListVisitor {
		public ArgPairVisitor() {
			super(nullValue);
		}

		@Override
		public void value(TypedValue value, boolean isLast) {
			if (!value.is(Cons.class)) throw new InvalidArgsException();
			final Cons pair = value.as(Cons.class);

			final TypedValue patternValue = pair.car;
			final IBindPattern pattern;
			if (patternValue.is(IBindPattern.class)) {
				pattern = patternValue.as(IBindPattern.class);
			} else if (patternValue.is(Symbol.class)) {
				pattern = BindPatternTranslator.createPatternForVarName(patternValue.as(Symbol.class).value);
			} else {
				throw new IllegalArgumentException("Invalid bind pattern: " + patternValue);
			}

			if (!pair.cdr.is(Code.class)) throw new InvalidArgsException();
			final Code valueExpr = pair.cdr.as(Code.class);

			acceptVar(pattern, valueExpr);
		}

		protected abstract void acceptVar(IBindPattern pattern, Code value);

		@Override
		public void end(TypedValue terminator) {}

		@Override
		public void begin() {}
	}

	private static void copySymbols(Set<String> names, SymbolMap<TypedValue> from, SymbolMap<TypedValue> to) {
		for (String bindName : names) {
			final ISymbol<TypedValue> outputSymbol = from.get(bindName);
			Preconditions.checkState(outputSymbol != null, "Symbol not defined: %s", bindName);
			to.put(bindName, outputSymbol);
		}
	}

	private static void fillPlaceholders(Set<String> bindNames, SymbolMap<TypedValue> symbols) {
		for (String bindName : bindNames)
			symbols.put(bindName, new PlaceholderSymbol());
	}

	private static Set<String> extractBindNames(IBindPattern pattern) {
		final Set<String> bindNames = Sets.newHashSet();
		pattern.listBoundVars(bindNames);
		return bindNames;
	}

	private static TypedValue executeForSingleResult(Frame<TypedValue> frame, Code expr) {
		expr.execute(frame);
		return frame.stack().popAndExpectEmptyStack();
	}

	private class LetSymbol extends LetSymbolBase {
		@Override
		protected void prepareFrame(final SymbolMap<TypedValue> outputSymbols, final SymbolMap<TypedValue> callSymbols, Cons vars) {
			vars.visit(new ArgPairVisitor() {
				@Override
				protected void acceptVar(IBindPattern pattern, Code expr) {
					final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrame(callSymbols);
					final SymbolMap<TypedValue> executionSymbols = executionFrame.symbols();

					final Set<String> bindNames = extractBindNames(pattern);
					fillPlaceholders(bindNames, executionSymbols);

					final TypedValue result = executeForSingleResult(executionFrame, expr);

					TypedCalcUtils.matchPattern(pattern, executionFrame, outputSymbols, result);

					// make new values visible to intializer body
					copySymbols(bindNames, outputSymbols, executionSymbols);
				}
			});
		}
	}

	private class LetSeqSymbol extends LetSymbolBase {
		@Override
		protected void prepareFrame(final SymbolMap<TypedValue> outputSymbols, SymbolMap<TypedValue> callSymbols, Cons vars) {
			vars.visit(new ArgPairVisitor() {
				@Override
				protected void acceptVar(IBindPattern pattern, Code expr) {
					final Set<String> bindNames = extractBindNames(pattern);
					fillPlaceholders(bindNames, outputSymbols);

					final Frame<TypedValue> executionFrame = FrameFactory.symbolsToFrame(outputSymbols);

					final TypedValue result = executeForSingleResult(executionFrame, expr);

					TypedCalcUtils.matchPattern(pattern, executionFrame, outputSymbols, result);

					// make new values visible to intializer body
					copySymbols(bindNames, outputSymbols, executionFrame.symbols());
				}
			});
		}
	}

	private static class PatternInitializerCodePair {
		public final IBindPattern pattern;
		public final Code code;

		public PatternInitializerCodePair(IBindPattern pattern, Code code) {
			this.pattern = pattern;
			this.code = code;
		}
	}

	private class LetRecSymbol extends LetSymbolBase {
		@Override
		protected void prepareFrame(final SymbolMap<TypedValue> outputSymbols, SymbolMap<TypedValue> callSymbols, Cons vars) {
			// collect data, including var names
			final Set<String> bindNames = Sets.newHashSet();
			final List<PatternInitializerCodePair> varsToExecute = Lists.newArrayList();
			vars.visit(new ArgPairVisitor() {
				@Override
				protected void acceptVar(IBindPattern pattern, Code expr) {
					pattern.listBoundVars(bindNames);
					varsToExecute.add(new PatternInitializerCodePair(pattern, expr));
				}
			});

			final SymbolMap<TypedValue> placeholderSymbols = new LocalSymbolMap<TypedValue>(callSymbols);
			fillPlaceholders(bindNames, placeholderSymbols);

			// evaluate and unpack expressions
			for (PatternInitializerCodePair e : varsToExecute) {
				final Frame<TypedValue> executionFrame = FrameFactory.newLocalFrame(placeholderSymbols);
				final TypedValue result = executeForSingleResult(executionFrame, e.code);
				TypedCalcUtils.matchPattern(e.pattern, executionFrame, outputSymbols, result);
			}

			// expose results to namespaces - must be done after evaluations, since all symbols must be executed with dummy values in place
			// IMO this is more consistent than "each id is initialized immediately after the corresponding val-expr is evaluated"
			copySymbols(bindNames, outputSymbols, placeholderSymbols);
		}
	}

	public void registerSymbol(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_LET, new LetSymbol());
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_LETSEQ, new LetSeqSymbol());
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_LETREC, new LetRecSymbol());
	}
}
