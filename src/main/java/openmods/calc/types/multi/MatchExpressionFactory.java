package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.LocalSymbolMap;
import openmods.calc.SingleReturnCallable;
import openmods.calc.SymbolCall;
import openmods.calc.SymbolMap;
import openmods.calc.Value;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.BracketContainerNode;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.calc.parsing.SymbolCallNode;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class MatchExpressionFactory {

	private static final String SYMBOL_DEFAULT_ACTION = "default";
	private static final String SYMBOL_GUARDED_ACTION = "guarded";
	private static final String SYMBOL_PATTERN_VAR = "var";

	private final TypeDomain domain;
	private final BinaryOperator<TypedValue> split;
	private final BinaryOperator<TypedValue> lambda;

	private final BindPatternEvaluator patternEvaluator;
	private final BindPatternTranslator patternTranslator;

	public MatchExpressionFactory(TypeDomain domain, BinaryOperator<TypedValue> split, BinaryOperator<TypedValue> lambda) {
		this.domain = domain;
		this.split = split;
		this.lambda = lambda;

		this.domain.registerType(PatternMatcher.class, "case");

		this.patternEvaluator = new BindPatternEvaluator(this.domain);
		this.patternTranslator = new BindPatternTranslator();
	}

	private abstract static class PatternMatcher {
		private final List<IBindPattern> argPatterns;

		public PatternMatcher(List<IBindPattern> argPatterns) {
			this.argPatterns = argPatterns;
		}

		public Optional<Code> match(Frame<TypedValue> env, SymbolMap<TypedValue> output, List<TypedValue> values) {
			Preconditions.checkState(values.size() == argPatterns.size(), "Invalid usage: expected %s values, got %s", argPatterns.size(), values.size());
			for (int i = 0; i < values.size(); i++) {
				final TypedValue value = values.get(i);
				final IBindPattern pattern = argPatterns.get(i);
				if (!pattern.match(env, output, value)) return Optional.absent();
			}

			return matchGuard(env, output);
		}

		protected abstract Optional<Code> matchGuard(Frame<TypedValue> env, SymbolMap<TypedValue> output);

		public int requiredArgs() {
			return argPatterns.size();
		}
	}

	private static class UnguardedPatternMatcher extends PatternMatcher {
		private final Optional<Code> action;

		public UnguardedPatternMatcher(List<IBindPattern> patterns, Optional<Code> action) {
			super(patterns);
			this.action = action;
		}

		@Override
		protected Optional<Code> matchGuard(Frame<TypedValue> env, SymbolMap<TypedValue> output) {
			return action;
		}

	}

	private static class GuardedAction {
		public final Code guard;
		public final Optional<Code> action;

		public GuardedAction(Code guard, Code action) {
			this.guard = guard;
			this.action = Optional.of(action);
		}
	}

	private static class GuardedPatternMatcher extends PatternMatcher {
		public final List<GuardedAction> guardedActions;
		public final Optional<Code> defaultAction;

		public GuardedPatternMatcher(List<IBindPattern> patterns, List<GuardedAction> guardedActions, Optional<Code> defaultAction) {
			super(patterns);
			this.guardedActions = ImmutableList.copyOf(guardedActions);
			this.defaultAction = defaultAction;
		}

		@Override
		protected Optional<Code> matchGuard(Frame<TypedValue> env, SymbolMap<TypedValue> output) {
			final Frame<TypedValue> clauseEnv = FrameFactory.createProtectionFrame(output);
			final Stack<TypedValue> clauseEnvStack = clauseEnv.stack();

			for (GuardedAction clause : guardedActions) {
				clause.guard.execute(clauseEnv);
				final TypedValue result = clauseEnvStack.popAndExpectEmptyStack();
				if (MetaObjectUtils.boolValue(env, result)) return clause.action;
			}

			return defaultAction;
		}
	}

	private interface ActionCompiler {
		public void flatten(List<IExecutable<TypedValue>> output);
	}

	private class GuardedActionCompiler implements ActionCompiler {
		private final IExprNode<TypedValue> guard;
		private final IExprNode<TypedValue> action;

		public GuardedActionCompiler(IExprNode<TypedValue> guard, IExprNode<TypedValue> action) {
			this.guard = guard;
			this.action = action;
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			output.add(Value.create(Code.flattenAndWrap(domain, guard)));
			output.add(Value.create(Code.flattenAndWrap(domain, action)));
			output.add(new SymbolCall<TypedValue>(SYMBOL_GUARDED_ACTION, 2, 0));
		}
	}

	private class DefaultActionCompiler implements ActionCompiler {
		private final IExprNode<TypedValue> action;

		public DefaultActionCompiler(IExprNode<TypedValue> action) {
			this.action = action;
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			output.add(Value.create(Code.flattenAndWrap(domain, action)));
			output.add(new SymbolCall<TypedValue>(SYMBOL_DEFAULT_ACTION, 1, 0));
		}
	}

	private class PatternCompiler {
		private final List<IExprNode<TypedValue>> argBindPatterns;
		private final List<? extends ActionCompiler> actions;

		public PatternCompiler(List<IExprNode<TypedValue>> bindPatterns, List<? extends ActionCompiler> actions) {
			this.argBindPatterns = bindPatterns;
			this.actions = actions;
		}

		public void flatten(List<IExecutable<TypedValue>> output) {
			final List<IExecutable<TypedValue>> patternCompileCode = Lists.newArrayList();

			for (IExprNode<TypedValue> patternConstructor : argBindPatterns) {
				patternCompileCode.add(Value.create(Code.flattenAndWrap(domain, patternConstructor)));
				patternCompileCode.add(new SymbolCall<TypedValue>(SYMBOL_PATTERN_VAR, 1, 0));
			}

			for (ActionCompiler action : actions)
				action.flatten(patternCompileCode);

			output.add(Value.create(Code.wrap(domain, patternCompileCode)));
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_CASE, 1, 1));
		}
	}

	private class MatchNode extends SymbolCallNode<TypedValue> {

		public MatchNode(List<IExprNode<TypedValue>> children) {
			super(TypedCalcConstants.SYMBOL_MATCH, children);
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			int patternCount = 0;
			for (PatternCompiler e : Iterables.transform(getChildren(), caseNodeCoverter)) {
				e.flatten(output);
				patternCount++;
			}

			Preconditions.checkArgument(patternCount > 0, "'match' expects at least one argument");

			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_MATCH, patternCount, 1));
		}
	}

	private final Function<IExprNode<TypedValue>, PatternCompiler> caseNodeCoverter = new Function<IExprNode<TypedValue>, PatternCompiler>() {
		@Override
		public PatternCompiler apply(IExprNode<TypedValue> input) {
			Preconditions.checkState(input instanceof BinaryOpNode, "Invalid 'match' syntax");
			final BinaryOpNode<TypedValue> patternNode = (BinaryOpNode<TypedValue>)input;
			final List<IExprNode<TypedValue>> varMatchers = extractVarMatchers(patternNode.left);
			if (patternNode.operator == lambda) {
				// pattern -> action
				return new PatternCompiler(varMatchers, ImmutableList.of(new DefaultActionCompiler(patternNode.right)));
			} else if (patternNode.operator == split) {
				final List<ActionCompiler> compilers = Lists.newArrayList();
				extractGuards(compilers, patternNode.right);
				return new PatternCompiler(varMatchers, compilers);
			} else throw new IllegalStateException("Invalid 'match' syntax, expected '->' between pattern and action or \\ between pattern and guarded actions, got" + patternNode.operator);
		}

		private List<IExprNode<TypedValue>> extractVarMatchers(IExprNode<TypedValue> arg) {
			if (arg instanceof BracketContainerNode) {
				return ImmutableList.copyOf(arg.getChildren());
			} else {
				throw new IllegalStateException("Expected argument list, got " + arg);
			}
		}

		private void extractGuards(List<ActionCompiler> compilers, IExprNode<TypedValue> clause) {
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
					compilers.add(new DefaultActionCompiler(clause));
				}
			} else {
				// just code - assume default clause
				// may be bit weird for returning lambda from default (requires parens) but meh
				compilers.add(new DefaultActionCompiler(clause));
			}
		}

		private ActionCompiler extractGuardedPattern(BinaryOpNode<TypedValue> op) {
			return new GuardedActionCompiler(op.left, op.right);
		}
	};

	private class MatchStateTransition extends SameStateSymbolTransition<TypedValue> {

		public MatchStateTransition(ICompilerState<TypedValue> compilerState) {
			super(compilerState);
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			return new MatchNode(children);
		}
	}

	public ISymbolCallStateTransition<TypedValue> createStateTransition(ICompilerState<TypedValue> compilerState) {
		return new MatchStateTransition(compilerState);
	}

	private class PatternMatcherBuilderVarSymbol extends FixedCallable<TypedValue> {
		private final PatternMatcherBuilder parent;
		private final SymbolMap<TypedValue> topSymbolMap;

		public PatternMatcherBuilderVarSymbol(PatternMatcherBuilder parent, SymbolMap<TypedValue> topSymbolMap) {
			super(1, 0);
			this.parent = parent;
			this.topSymbolMap = topSymbolMap;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Code pattern = frame.stack().pop().as(Code.class, "variable pattern");
			final TypedValue compiledPattern = patternEvaluator.evaluate(topSymbolMap, pattern);
			final IBindPattern translatedPattern = patternTranslator.translatePattern(compiledPattern);
			parent.addVarPattern(translatedPattern);
		}

	}

	private static class PatternMatcherBuilderBuilderGuardedActionSymbol extends FixedCallable<TypedValue> {
		private final PatternMatcherBuilder parent;

		public PatternMatcherBuilderBuilderGuardedActionSymbol(PatternMatcherBuilder parent) {
			super(2, 0);
			this.parent = parent;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack();
			final Code action = stack.pop().as(Code.class, "case action");
			final Code guard = stack.pop().as(Code.class, "case guard");

			parent.addGuardedAction(new GuardedAction(guard, action));
		}
	}

	private static class PatternMatcherBuilderDefaultActionSymbol extends FixedCallable<TypedValue> {
		private final PatternMatcherBuilder parent;

		public PatternMatcherBuilderDefaultActionSymbol(PatternMatcherBuilder parent) {
			super(1, 0);
			this.parent = parent;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack();
			final Code action = stack.pop().as(Code.class, "case action");

			parent.addDefaultAction(action);
		}
	}

	private class PatternMatcherBuilder {
		private final List<IBindPattern> varPatterns = Lists.newArrayList();
		private final List<GuardedAction> guardedActions = Lists.newArrayList();
		private Optional<Code> defaultAction = Optional.absent();

		private boolean firstActionAdded;

		public void addVarPattern(IBindPattern pattern) {
			Preconditions.checkState(!firstActionAdded, "Trying to add variable pattern after action");
			varPatterns.add(pattern);
		}

		public void addGuardedAction(GuardedAction guardedPatternClause) {
			Preconditions.checkState(!defaultAction.isPresent(), "Trying to add guarded action after default");
			firstActionAdded = true;
			guardedActions.add(guardedPatternClause);
		}

		public void addDefaultAction(Code action) {
			firstActionAdded = true;
			defaultAction = Optional.of(action);
		}

		public PatternMatcher buildPattern() {
			if (guardedActions.isEmpty()) {
				Preconditions.checkState(defaultAction.isPresent(), "Invalid 'pattern' arguments"); // impossible?
				return new UnguardedPatternMatcher(varPatterns, defaultAction);
			} else {
				return new GuardedPatternMatcher(varPatterns, guardedActions, defaultAction);
			}
		}

	}

	private class CaseSymbol extends FixedCallable<TypedValue> {
		private final SymbolMap<TypedValue> topSymbolMap;

		public CaseSymbol(SymbolMap<TypedValue> topSymbolMap) {
			super(1, 1);
			this.topSymbolMap = topSymbolMap;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack();

			final Code pattern = stack.pop().as(Code.class, "pattern constructor code (first arg)");

			final PatternMatcher result = evaluatePattern(pattern, topSymbolMap);
			stack.push(domain.create(PatternMatcher.class, result));
		}

		private PatternMatcher evaluatePattern(Code pattern, SymbolMap<TypedValue> topSymbolMap) {
			final Frame<TypedValue> executionFrame = FrameFactory.createTopFrame();
			final SymbolMap<TypedValue> executionSymbols = executionFrame.symbols();
			final PatternMatcherBuilder builder = new PatternMatcherBuilder();

			executionSymbols.put(SYMBOL_PATTERN_VAR, new PatternMatcherBuilderVarSymbol(builder, topSymbolMap));
			executionSymbols.put(SYMBOL_GUARDED_ACTION, new PatternMatcherBuilderBuilderGuardedActionSymbol(builder));
			executionSymbols.put(SYMBOL_DEFAULT_ACTION, new PatternMatcherBuilderDefaultActionSymbol(builder));

			pattern.execute(executionFrame);
			executionFrame.stack().checkIsEmpty();
			return builder.buildPattern();
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
		private final List<PatternMatcher> cases;

		public MatchingFunction(SymbolMap<TypedValue> defineScope, List<PatternMatcher> cases) {
			this.defineScope = defineScope;
			this.cases = ImmutableList.copyOf(cases);
		}

		@Override
		public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			final Stack<TypedValue> stack = frame.stack();

			final Frame<TypedValue> env = FrameFactory.createProtectionFrame(defineScope);
			for (PatternMatcher matchCase : cases) {
				final int args = matchCase.requiredArgs();
				if (argumentsCount.isPresent()) {
					if (argumentsCount.get() != args) continue;
				} else {
					if (stack.size() < args) continue;
				}

				final Stack<TypedValue> valuesToMatchStack = stack.substack(args);
				final List<TypedValue> valuesToMatch = ImmutableList.copyOf(valuesToMatchStack);
				final SymbolMap<TypedValue> matchedSymbols = new LocalSymbolMap<TypedValue>(defineScope);
				final Optional<Code> match = matchCase.match(env, matchedSymbols, valuesToMatch);
				if (match.isPresent()) {
					valuesToMatchStack.clear();
					final Frame<TypedValue> matchedFrame = FrameFactory.newClosureFrame(matchedSymbols, frame, 0);
					match.get().execute(matchedFrame);
					TypedCalcUtils.expectExactReturnCount(returnsCount, matchedFrame.stack().size());
					return;
				}
			}

			throw new MatchFailedException("Can't find matching case");
		}

	}

	private class MatchSymbol extends SingleReturnCallable<TypedValue> {

		@Override
		public TypedValue call(Frame<TypedValue> frame, OptionalInt argumentsCount) {
			Preconditions.checkArgument(argumentsCount.isPresent(), "'match' must be called with argument count");

			final Stack<TypedValue> stack = frame.stack();

			final List<PatternMatcher> patterns = Lists.newArrayList();
			for (int i = 0; i < argumentsCount.get(); i++) {
				final TypedValue arg = stack.pop();
				patterns.add(arg.as(PatternMatcher.class));
			}

			return CallableValue.wrap(domain, new MatchingFunction(frame.symbols(), Lists.reverse(patterns)));
		}
	}

	public void registerSymbols(SymbolMap<TypedValue> env, SymbolMap<TypedValue> patternEnv) {
		env.put(TypedCalcConstants.SYMBOL_MATCH, new MatchSymbol());
		env.put(TypedCalcConstants.SYMBOL_CASE, new CaseSymbol(patternEnv));
	}

}
