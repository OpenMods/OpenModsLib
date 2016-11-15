package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.ISymbol;
import openmods.calc.LocalSymbolMap;
import openmods.calc.NestedSymbolMap;
import openmods.calc.SingleReturnCallable;
import openmods.calc.StackValidationException;
import openmods.calc.SymbolCall;
import openmods.calc.SymbolMap;
import openmods.calc.Value;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.utils.Stack;

public class MatchExpressionFactory {

	private final TypeDomain domain;
	private final BinaryOperator<TypedValue> split;
	private final BinaryOperator<TypedValue> lambda;
	private final BinaryOperator<TypedValue> cons;

	public MatchExpressionFactory(TypeDomain domain, BinaryOperator<TypedValue> split, BinaryOperator<TypedValue> lambda, BinaryOperator<TypedValue> cons) {
		this.domain = domain;
		this.split = split;
		this.lambda = lambda;
		this.cons = cons;
	}

	private static interface PatternPart {
		public boolean match(SymbolMap<TypedValue> env, TypedValue value);
	}

	private static class PatternAny implements PatternPart {

		public static final PatternAny INSTANCE = new PatternAny();

		@Override
		public boolean match(SymbolMap<TypedValue> env, TypedValue value) {
			return true;
		}

	}

	private static class PatternBindName implements PatternPart {

		private final String name;

		public PatternBindName(String name) {
			this.name = name;
		}

		@Override
		public boolean match(SymbolMap<TypedValue> env, TypedValue value) {
			env.put(name, value);
			return true;
		}

	}

	private static class PatternMatchExact implements PatternPart {

		private final TypedValue expected;

		public PatternMatchExact(TypedValue expected) {
			this.expected = expected;
		}

		@Override
		public boolean match(SymbolMap<TypedValue> env, TypedValue value) {
			return value.equals(expected);
		}

	}

	private static class PatternMatchCons implements PatternPart {

		private final PatternPart carMatcher;
		private final PatternPart cdrMatcher;

		public PatternMatchCons(PatternPart carMatcher, PatternPart cdrMatcher) {
			this.carMatcher = carMatcher;
			this.cdrMatcher = cdrMatcher;
		}

		@Override
		public boolean match(SymbolMap<TypedValue> env, TypedValue value) {
			if (!value.is(Cons.class)) return false;
			final Cons pair = value.as(Cons.class);
			return carMatcher.match(env, pair.car) && cdrMatcher.match(env, pair.cdr);
		}

	}

	private static interface IPattern extends ICompositeTrait {
		public Optional<Code> match(SymbolMap<TypedValue> outputSymbols, TypedValue value);
	}

	private static class UnguardedPattern implements IPattern {
		private final PatternPart pattern;
		private final Optional<Code> action;

		public UnguardedPattern(PatternPart pattern, Optional<Code> action) {
			this.pattern = pattern;
			this.action = action;
		}

		@Override
		public Optional<Code> match(SymbolMap<TypedValue> outputSymbols, TypedValue value) {
			if (pattern.match(outputSymbols, value)) return action;
			else return Optional.absent();
		}
	}

	private static class GuardedPatternClause {
		public final Code guard;
		public final Optional<Code> action;

		public GuardedPatternClause(Code guard, Code action) {
			this.guard = guard;
			this.action = Optional.of(action);
		}
	}

	private static class GuardedPattern implements IPattern {
		public final PatternPart pattern;
		public final List<GuardedPatternClause> guardedActions;
		public final Optional<Code> defaultAction;

		public GuardedPattern(PatternPart pattern, List<GuardedPatternClause> guardedActions, Optional<Code> defaultAction) {
			this.pattern = pattern;
			this.guardedActions = ImmutableList.copyOf(guardedActions);
			this.defaultAction = defaultAction;
		}

		@Override
		public Optional<Code> match(SymbolMap<TypedValue> outputSymbols, TypedValue value) {
			if (!pattern.match(outputSymbols, value)) return Optional.absent();

			final Frame<TypedValue> clauseEnv = FrameFactory.createProtectionFrame(outputSymbols);
			final Stack<TypedValue> clauseEnvStack = clauseEnv.stack();

			for (GuardedPatternClause clause : guardedActions) {
				clause.guard.execute(clauseEnv);
				Preconditions.checkState(clauseEnvStack.size() == 1, "Invalid guard expression - expected exactly one result");
				final TypedValue result = clauseEnvStack.pop();
				if (result.isTruthy()) return clause.action;
			}

			return defaultAction;
		}

	}

	private interface PatternActionCompiler {
		public void flatten(List<IExecutable<TypedValue>> output);
	}

	private class UnguardedPatternActionCompiler implements PatternActionCompiler {
		private final IExprNode<TypedValue> action;

		public UnguardedPatternActionCompiler(IExprNode<TypedValue> action) {
			this.action = action;
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			output.add(Value.create(Code.flattenAndWrap(domain, action)));
		}

	}

	private class GuardedPatternActionCompiler implements PatternActionCompiler {
		private final IExprNode<TypedValue> guard;
		private final IExprNode<TypedValue> action;

		public GuardedPatternActionCompiler(IExprNode<TypedValue> guard, IExprNode<TypedValue> action) {
			this.guard = guard;
			this.action = action;
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			output.add(Value.create(Code.flattenAndWrap(domain, guard)));
			output.add(Value.create(Code.flattenAndWrap(domain, action)));
			output.add(cons);
		}
	}

	private class PatternConstructionCompiler {
		private final IExprNode<TypedValue> patternConstructor;
		private final List<? extends PatternActionCompiler> patternActions;

		public PatternConstructionCompiler(IExprNode<TypedValue> patternConstructor, List<? extends PatternActionCompiler> patternActions) {
			this.patternConstructor = patternConstructor;
			this.patternActions = patternActions;
		}

		public void flatten(List<IExecutable<TypedValue>> output) {
			output.add(Value.create(Code.flattenAndWrap(domain, patternConstructor)));
			for (PatternActionCompiler action : patternActions)
				action.flatten(output);

			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_PATTERN, 1 + patternActions.size(), 1));
		}
	}

	private class MatchNode implements IExprNode<TypedValue> {

		private final List<PatternConstructionCompiler> patterns;

		public MatchNode(Iterable<PatternConstructionCompiler> patterns) {
			this.patterns = ImmutableList.copyOf(patterns);
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			for (PatternConstructionCompiler e : patterns)
				e.flatten(output);

			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_MATCH, patterns.size(), 1));
		}

		@Override
		public Iterable<IExprNode<TypedValue>> getChildren() {
			return ImmutableList.of();
		}

	}

	private final Function<IExprNode<TypedValue>, PatternConstructionCompiler> patternNodeCoverter = new Function<IExprNode<TypedValue>, PatternConstructionCompiler>() {
		@Override
		public PatternConstructionCompiler apply(IExprNode<TypedValue> input) {
			Preconditions.checkState(input instanceof BinaryOpNode, "Invalid 'match' syntax");
			final BinaryOpNode<TypedValue> patternNode = (BinaryOpNode<TypedValue>)input;
			if (patternNode.operator == lambda) {
				// pattern -> action
				return new PatternConstructionCompiler(patternNode.left, ImmutableList.of(new UnguardedPatternActionCompiler(patternNode.right)));
			} else if (patternNode.operator == split) {
				final List<PatternActionCompiler> compilers = Lists.newArrayList();
				extractGuards(compilers, patternNode.right);
				return new PatternConstructionCompiler(patternNode.left, compilers);
			} else
				throw new IllegalStateException("Invalid 'match' syntax, expected '->' between pattern and action or \\ between pattern and guarded actions, got" + patternNode.operator);
		}

		private void extractGuards(List<PatternActionCompiler> compilers, IExprNode<TypedValue> clause) {
			if (clause instanceof BinaryOpNode) {
				final BinaryOpNode<TypedValue> op = (BinaryOpNode<TypedValue>)clause;
				if (op.operator == split) {
					// split - on left should be guard -> action, on right - continuation
					Preconditions.checkState(op.left instanceof BinaryOpNode, "Invalid 'match' syntax: expected guard -> action on left side of \\, got: ", op.left);
					final BinaryOpNode<TypedValue> left = (BinaryOpNode<TypedValue>)op.left;
					Preconditions.checkState(left.operator == lambda, "Invalid 'match' syntax: expected guard -> action on left side of \\, got: %s", left.operator);
					compilers.add(extractGuardedPattern(left));

					// continue to next clause
					extractGuards(compilers, op.right);
				} else if (op.operator == lambda) {
					// last entry - guard -> action
					compilers.add(extractGuardedPattern(op));
				} else {
					// some other binary operator in default clause
					compilers.add(new UnguardedPatternActionCompiler(clause));
				}
			} else {
				// just code - assume default clause
				// may be bit weird for returning lambda from default (requires parens) but meh
				compilers.add(new UnguardedPatternActionCompiler(clause));
			}
		}

		private PatternActionCompiler extractGuardedPattern(BinaryOpNode<TypedValue> op) {
			return new GuardedPatternActionCompiler(op.left, op.right);
		}
	};

	private class MatchStateTransition extends SameStateSymbolTransition<TypedValue> {

		public MatchStateTransition(ICompilerState<TypedValue> compilerState) {
			super(compilerState);
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			Preconditions.checkArgument(children.size() > 0, "'match' expects at least one argument");
			return new MatchNode(Iterables.transform(children, patternNodeCoverter));
		}

	}

	public ISymbolCallStateTransition<TypedValue> createStateTransition(ICompilerState<TypedValue> compilerState) {
		return new MatchStateTransition(compilerState);
	}

	private static class VarPlaceholder implements ICompositeTrait {
		public final String var;

		public VarPlaceholder(String var) {
			this.var = var;
		}
	}

	private class PatternPlaceholdersSymbolMap extends NestedSymbolMap<TypedValue> {

		public PatternPlaceholdersSymbolMap(SymbolMap<TypedValue> parent) {
			super(parent);
		}

		@Override
		public void put(String name, ISymbol<TypedValue> symbol) {
			throw new UnsupportedOperationException("Can't create new symbols in match patterns");
		}

		@Override
		public ISymbol<TypedValue> get(String name) {
			final ISymbol<TypedValue> parentSymbol = super.get(name);
			if (parentSymbol != null) return parentSymbol;
			return createSymbol(domain.create(IComposite.class, new SingleTraitComposite("patternBind", new VarPlaceholder(name))));
		}

	}

	private class PatternSymbol extends SingleReturnCallable<TypedValue> {
		private final SymbolMap<TypedValue> placeholderSymbolMap;

		public PatternSymbol(SymbolMap<TypedValue> topSymbolMap) {
			this.placeholderSymbolMap = new PatternPlaceholdersSymbolMap(topSymbolMap);
		}

		@Override
		public TypedValue call(Frame<TypedValue> frame, Optional<Integer> argumentsCount) {
			// pattern, (guard:action)*, action
			final int args = argumentsCount.or(2);
			Preconditions.checkState(args >= 2, "'pattern' symbol expects more than one arg");

			final Stack<TypedValue> stack = frame.stack();

			Optional<Code> lastAction = Optional.absent();
			final List<GuardedPatternClause> guardedActions = Lists.newArrayList();
			for (int i = 0; i < args - 1; i++) {
				final TypedValue arg = stack.pop();
				if (arg.is(Code.class)) {
					Preconditions.checkState(i == 0, "'code' value allowed only on end of 'pattern' args");
					lastAction = Optional.of(arg.as(Code.class));
				} else if (arg.is(Cons.class)) {
					final Cons pair = arg.as(Cons.class);
					Preconditions.checkState(pair.car.is(Code.class) && pair.cdr.is(Code.class), "Expected code:code pair on 'pattern' args, got %s", pair);
					guardedActions.add(new GuardedPatternClause(pair.car.as(Code.class), pair.cdr.as(Code.class)));
				} else {
					throw new IllegalArgumentException("Only code:code pairs and code allowed in 'pattern' args");
				}
			}

			final Code pattern = stack.pop().as(Code.class, "first 'match' argument (pattern)");

			final TypedValue compiledPattern = evaluatePattern(pattern);
			final PatternPart translatedPattern = translatePattern(compiledPattern);

			final IPattern result;

			if (guardedActions.isEmpty()) {
				Preconditions.checkState(lastAction.isPresent(), "Invalid 'pattern' arguments"); // impossible?
				result = new UnguardedPattern(translatedPattern, lastAction);
			} else {
				result = new GuardedPattern(translatedPattern, Lists.reverse(guardedActions), lastAction);
			}

			return domain.create(IComposite.class, new SingleTraitComposite("pattern", result));
		}

		private PatternPart translatePattern(TypedValue compiledPattern) {
			if (compiledPattern.is(IComposite.class)) {
				final IComposite composite = compiledPattern.as(IComposite.class);
				if (composite.has(VarPlaceholder.class)) {
					final VarPlaceholder p = composite.get(VarPlaceholder.class);
					return p.var.equals(TypedCalcConstants.MATCH_ANY)
							? PatternAny.INSTANCE
							: new PatternBindName(p.var);
				}
			}

			if (compiledPattern.is(Cons.class)) {
				final Cons pair = compiledPattern.as(Cons.class);
				final PatternPart carPattern = translatePattern(pair.car);
				final PatternPart cdrPattern = translatePattern(pair.cdr);
				return new PatternMatchCons(carPattern, cdrPattern);
			}

			return new PatternMatchExact(compiledPattern);
		}

		private TypedValue evaluatePattern(Code pattern) {
			final Frame<TypedValue> patternFrame = FrameFactory.createTopFrame(placeholderSymbolMap);
			pattern.execute(patternFrame);
			final Stack<TypedValue> resultStack = patternFrame.stack();
			Preconditions.checkState(resultStack.size() == 1, "Invalid result of pattern compilation");
			return resultStack.pop();
		}
	}

	public static class MatchFailedException extends RuntimeException {
		private static final long serialVersionUID = -6897909046702800612L;

		public MatchFailedException(String message) {
			super(message);
		}

	}

	private static class MatchingFunction implements ICallable<TypedValue> {

		private final SymbolMap<TypedValue> defineScope;
		private final List<IPattern> patterns;

		public MatchingFunction(SymbolMap<TypedValue> defineScope, List<IPattern> patterns) {
			this.defineScope = defineScope;
			this.patterns = ImmutableList.copyOf(patterns);
		}

		@Override
		public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			if (argumentsCount.isPresent()) {
				final int args = argumentsCount.get();
				if (args != 1) throw new StackValidationException("Expected single argument but got %s", args);
			}

			final Stack<TypedValue> stack = frame.stack();
			final TypedValue valueToMatch = stack.pop();

			for (IPattern pattern : patterns) {
				final SymbolMap<TypedValue> matchedSymbols = new LocalSymbolMap<TypedValue>(defineScope);
				final Optional<Code> match = pattern.match(matchedSymbols, valueToMatch);
				if (match.isPresent()) {
					final Frame<TypedValue> matchedFrame = FrameFactory.newClosureFrame(matchedSymbols, frame, 0);
					match.get().execute(matchedFrame);
					if (returnsCount.isPresent()) {
						final int expected = returnsCount.get();
						final int actual = matchedFrame.stack().size();
						if (expected != actual) throw new StackValidationException("Has %s result(s) but expected %s", actual, expected);
					}
					return;
				}
			}

			throw new MatchFailedException("Unmatched value: " + valueToMatch);
		}

	}

	private class MatchSymbol extends SingleReturnCallable<TypedValue> {

		@Override
		public TypedValue call(Frame<TypedValue> frame, Optional<Integer> argumentsCount) {
			Preconditions.checkArgument(argumentsCount.isPresent(), "'match' must be called with argument count");

			final Stack<TypedValue> stack = frame.stack();

			final List<IPattern> patterns = Lists.newArrayList();
			for (int i = 0; i < argumentsCount.get(); i++) {
				final TypedValue arg = stack.pop();
				final IComposite patternComposite = arg.as(IComposite.class, "'match' argument");
				patterns.add(patternComposite.get(IPattern.class));
			}

			return domain.create(ICallable.class, new MatchingFunction(frame.symbols(), Lists.reverse(patterns)));
		}
	}

	public void registerSymbols(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_MATCH, new MatchSymbol());
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_PATTERN, new PatternSymbol(env.topFrame().symbols()));
	}

}
