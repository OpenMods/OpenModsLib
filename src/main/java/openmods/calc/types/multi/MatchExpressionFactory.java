package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.ISymbol;
import openmods.calc.LocalSymbolMap;
import openmods.calc.NestedSymbolMap;
import openmods.calc.ProtectionSymbolMap;
import openmods.calc.SingleReturnCallable;
import openmods.calc.StackValidationException;
import openmods.calc.SymbolCall;
import openmods.calc.SymbolMap;
import openmods.calc.Value;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.BracketContainerNode;
import openmods.calc.parsing.ICompilerState;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.ISymbolCallStateTransition;
import openmods.calc.parsing.SameStateSymbolTransition;
import openmods.calc.types.multi.CompositeTraits.Typed;
import openmods.utils.Stack;

public class MatchExpressionFactory {

	private static final String SYMBOL_DEFAULT_ACTION = "default";
	private static final String SYMBOL_GUARDED_ACTION = "guarded";
	private static final String SYMBOL_PATTERN_VAR = "var";

	private final TypeDomain domain;
	private final BinaryOperator<TypedValue> split;
	private final BinaryOperator<TypedValue> lambda;

	public MatchExpressionFactory(TypeDomain domain, BinaryOperator<TypedValue> split, BinaryOperator<TypedValue> lambda) {
		this.domain = domain;
		this.split = split;
		this.lambda = lambda;
	}

	private static interface PatternPart {
		public boolean match(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value);
	}

	private static class PatternAny implements PatternPart {

		public static final PatternAny INSTANCE = new PatternAny();

		@Override
		public boolean match(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value) {
			return true;
		}

	}

	private static class PatternBindName implements PatternPart {

		private final String name;

		public PatternBindName(String name) {
			this.name = name;
		}

		@Override
		public boolean match(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value) {
			output.put(name, value);
			return true;
		}

	}

	private static class PatternMatchExact implements PatternPart {

		private final TypedValue expected;

		public PatternMatchExact(TypedValue expected) {
			this.expected = expected;
		}

		@Override
		public boolean match(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value) {
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
		public boolean match(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value) {
			if (!value.is(Cons.class)) return false;
			final Cons pair = value.as(Cons.class);
			return carMatcher.match(env, output, pair.car) && cdrMatcher.match(null, output, pair.cdr);
		}

	}

	private abstract static class PatternMatchDecomposableBase implements PatternPart {

		private final List<PatternPart> argMatchers;

		public PatternMatchDecomposableBase(List<PatternPart> argMatchers) {
			this.argMatchers = argMatchers;
		}

		@Override
		public boolean match(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output, TypedValue value) {
			final TypedValue typeValue = findConstructor(env);

			if (typeValue.is(IComposite.class)) {
				final IComposite compositeType = typeValue.as(IComposite.class);

				// case 1: maybe type knows how to decompose this value
				final Optional<CompositeTraits.Decomposable> maybeDecomposable = compositeType.getOptional(CompositeTraits.Decomposable.class);
				if (maybeDecomposable.isPresent()) {
					final CompositeTraits.Decomposable decomposableCtor = maybeDecomposable.get();
					final int expectedValueCount = argMatchers.size();
					final Optional<List<TypedValue>> maybeDecomposition = decomposableCtor.tryDecompose(value, expectedValueCount);
					if (!maybeDecomposition.isPresent()) return false;

					final List<TypedValue> decomposition = maybeDecomposition.get();
					final int actualValueCount = decomposition.size();
					Preconditions.checkState(actualValueCount == expectedValueCount, "Decomposable contract broken - returned different number of values: expected: %s, got %s", expectedValueCount, actualValueCount);

					for (int i = 0; i < actualValueCount; i++) {
						final PatternPart pattern = argMatchers.get(i);
						final TypedValue var = decomposition.get(i);

						if (!pattern.match(env, output, var)) return false;
					}

					return true;
				}

				// case 2: check for value type, compare with matcher type
				if (argMatchers.size() == 1 && compositeType.has(CompositeTraits.TypeMarker.class)) {
					if (value.is(IComposite.class)) {
						final Optional<Typed> maybeTyped = value.as(IComposite.class).getOptional(CompositeTraits.Typed.class);
						if (maybeTyped.isPresent()) {
							final TypedValue expectedTypeValue = maybeTyped.get().getType();
							if (TypedCalcUtils.areEqual(typeValue, expectedTypeValue)) {
								final PatternPart pattern = argMatchers.get(0);
								return pattern.match(env, output, value);
							}
						}
					}
					return false;
				}
			}

			throw new IllegalStateException("Value " + typeValue + " does not describe constructor or type");
		}

		protected abstract TypedValue findConstructor(SymbolMap<TypedValue> env);
	}

	private static class PatternMatchDecomposable extends PatternMatchDecomposableBase {

		private final String typeName;

		public PatternMatchDecomposable(List<PatternPart> argMatchers, String typeName) {
			super(argMatchers);
			this.typeName = typeName;
		}

		@Override
		protected TypedValue findConstructor(SymbolMap<TypedValue> env) {
			final ISymbol<TypedValue> type = env.get(typeName);
			Preconditions.checkState(type != null, "Can't find decomposable constructor %s", typeName);
			return type.get();
		}
	}

	private class PatternMatchNamespaceDecomposable extends PatternMatchDecomposableBase {

		private final String pathStart;

		private final List<String> path;

		public PatternMatchNamespaceDecomposable(List<PatternPart> argMatchers, String pathStart, List<String> path) {
			super(argMatchers);
			this.pathStart = pathStart;
			this.path = path;
		}

		@Override
		protected TypedValue findConstructor(SymbolMap<TypedValue> env) {
			final ISymbol<TypedValue> initialSymbol = env.get(pathStart);
			Preconditions.checkState(initialSymbol != null, "Can't find symbol %s", pathStart);

			TypedValue result = initialSymbol.get();

			for (String p : path) {
				Preconditions.checkState(result.is(IComposite.class), "Value %s is not composite", result);
				final IComposite composite = result.as(IComposite.class);
				final Optional<CompositeTraits.Structured> maybeStructured = composite.getOptional(CompositeTraits.Structured.class);
				Preconditions.checkState(maybeStructured.isPresent(), "Value %s is not structured composite", result);

				final Optional<TypedValue> maybeNewResult = maybeStructured.get().get(domain, p);
				Preconditions.checkState(maybeNewResult.isPresent(), "Can't find value %s in in %s", p, maybeStructured);
				result = maybeNewResult.get();
			}

			return result;
		}
	}

	private static interface IPattern extends ICompositeTrait {
		public int requiredArgs();

		public Optional<Code> match(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output, List<TypedValue> values);
	}

	private abstract static class PatternBase implements IPattern {
		private final List<PatternPart> patterns;

		public PatternBase(List<PatternPart> patterns) {
			this.patterns = patterns;
		}

		@Override
		public Optional<Code> match(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output, List<TypedValue> values) {
			Preconditions.checkState(values.size() == patterns.size(), "Invalid usage: expected %s values, got %s", patterns.size(), values.size());
			for (int i = 0; i < values.size(); i++) {
				final TypedValue value = values.get(i);
				final PatternPart pattern = patterns.get(i);
				if (!pattern.match(env, output, value)) return Optional.absent();

			}

			return matchGuard(env, output);
		}

		protected abstract Optional<Code> matchGuard(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output);

		@Override
		public int requiredArgs() {
			return patterns.size();
		}
	}

	private static class UnguardedPattern extends PatternBase {
		private final Optional<Code> action;

		public UnguardedPattern(List<PatternPart> patterns, Optional<Code> action) {
			super(patterns);
			this.action = action;
		}

		@Override
		protected Optional<Code> matchGuard(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output) {
			return action;
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

	private static class GuardedPattern extends PatternBase {
		public final List<GuardedPatternClause> guardedActions;
		public final Optional<Code> defaultAction;

		public GuardedPattern(List<PatternPart> patterns, List<GuardedPatternClause> guardedActions, Optional<Code> defaultAction) {
			super(patterns);
			this.guardedActions = ImmutableList.copyOf(guardedActions);
			this.defaultAction = defaultAction;
		}

		@Override
		protected Optional<Code> matchGuard(SymbolMap<TypedValue> env, SymbolMap<TypedValue> output) {
			final Frame<TypedValue> clauseEnv = FrameFactory.createProtectionFrame(output);
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
			output.add(new SymbolCall<TypedValue>(SYMBOL_DEFAULT_ACTION, 1, 0));
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
			output.add(new SymbolCall<TypedValue>(SYMBOL_GUARDED_ACTION, 2, 0));
		}
	}

	private class PatternConstructionCompiler {
		private final List<IExprNode<TypedValue>> patternConstructors;
		private final List<? extends PatternActionCompiler> patternActions;

		public PatternConstructionCompiler(List<IExprNode<TypedValue>> patternConstructors, List<? extends PatternActionCompiler> patternActions) {
			this.patternConstructors = patternConstructors;
			this.patternActions = patternActions;
		}

		public void flatten(List<IExecutable<TypedValue>> output) {
			final List<IExecutable<TypedValue>> patternCompileCode = Lists.newArrayList();

			for (IExprNode<TypedValue> patternConstructor : patternConstructors) {
				patternCompileCode.add(Value.create(Code.flattenAndWrap(domain, patternConstructor)));
				patternCompileCode.add(new SymbolCall<TypedValue>(SYMBOL_PATTERN_VAR, 1, 0));
			}

			for (PatternActionCompiler action : patternActions)
				action.flatten(patternCompileCode);

			output.add(Value.create(Code.wrap(domain, patternCompileCode)));
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_PATTERN, 1, 1));
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
			final List<IExprNode<TypedValue>> varMatchers = extractVarMatchers(patternNode.left);
			if (patternNode.operator == lambda) {
				// pattern -> action
				return new PatternConstructionCompiler(varMatchers, ImmutableList.of(new UnguardedPatternActionCompiler(patternNode.right)));
			} else if (patternNode.operator == split) {
				final List<PatternActionCompiler> compilers = Lists.newArrayList();
				extractGuards(compilers, patternNode.right);
				return new PatternConstructionCompiler(varMatchers, compilers);
			} else throw new IllegalStateException("Invalid 'match' syntax, expected '->' between pattern and action or \\ between pattern and guarded actions, got" + patternNode.operator);
		}

		private List<IExprNode<TypedValue>> extractVarMatchers(IExprNode<TypedValue> arg) {
			if (arg instanceof BracketContainerNode) {
				return ImmutableList.copyOf(arg.getChildren());
			} else {
				throw new IllegalStateException("Expected argument list, got " + arg);
			}
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

	private interface PatternProvider extends ICompositeTrait {
		public PatternPart getPattern(IPatternTranslator translator);
	}

	private static class VarPlaceholder implements PatternProvider {
		public final String var;

		public VarPlaceholder(String var) {
			this.var = var;
		}

		@Override
		public PatternPart getPattern(IPatternTranslator translator) {
			return var.equals(TypedCalcConstants.MATCH_ANY)
					? PatternAny.INSTANCE
					: new PatternBindName(var);
		}
	}

	private class NamespaceConstructorStruct implements CompositeTraits.Structured {
		private final String var;
		private final List<String> path;

		private NamespaceConstructorStruct(String var, List<String> path) {
			this.var = var;
			this.path = path;
		}

		@Override
		public Optional<TypedValue> get(TypeDomain domain, String component) {
			final List<String> newPath = Lists.newArrayList(path);
			newPath.add(component);
			return Optional.of(domain.create(IComposite.class, createNamespaceProvider(var, newPath)));
		}
	}

	private IComposite createNamespaceProvider(final String var, final List<String> path) {
		class NamespaceCtorCallable extends SingleReturnCallable<TypedValue> implements CompositeTraits.Callable {
			@Override
			public TypedValue call(Frame<TypedValue> frame, Optional<Integer> argumentsCount) {
				Preconditions.checkArgument(argumentsCount.isPresent(), "Type constructor must be always called with arg count");

				final Stack<TypedValue> args = frame.stack().substack(argumentsCount.get());
				final NamespaceCtorPlaceholder placeholder = new NamespaceCtorPlaceholder(var, path, args);
				args.clear();
				return domain.create(IComposite.class, new SingleTraitComposite("patternCtor", placeholder));
			}
		}

		return new MappedComposite.Builder()
				.put(PatternProvider.class, new PatternProvider() {
					@Override
					public PatternPart getPattern(IPatternTranslator translator) {
						throw new IllegalStateException("Unfinished namespace constructor matcher: " + var + "." + Joiner.on(".").join(path));
					}
				})
				.put(CompositeTraits.Structured.class, new NamespaceConstructorStruct(var, path))
				.put(CompositeTraits.Callable.class, new NamespaceCtorCallable())
				.build("namespaceCtorPlaceholder");
	}

	private static List<PatternPart> translatePatterns(IPatternTranslator translator, List<TypedValue> args) {
		final List<PatternPart> varMatchers = Lists.newArrayList();

		for (TypedValue m : args)
			varMatchers.add(translator.translatePattern(m));

		return varMatchers;
	}

	private class NamespaceCtorPlaceholder implements PatternProvider {
		private final String var;
		private final List<String> path;
		private final List<TypedValue> args;

		public NamespaceCtorPlaceholder(String var, List<String> path, Iterable<TypedValue> args) {
			this.var = var;
			this.path = path;
			this.args = ImmutableList.copyOf(args);
		}

		@Override
		public PatternPart getPattern(IPatternTranslator translator) {
			return new PatternMatchNamespaceDecomposable(translatePatterns(translator, args), var, path);
		}
	}

	private static class CtorPlaceholder implements PatternProvider {
		private final String var;

		private final List<TypedValue> args;

		public CtorPlaceholder(String var, Iterable<TypedValue> args) {
			this.var = var;
			this.args = ImmutableList.copyOf(args);
		}

		@Override
		public PatternPart getPattern(IPatternTranslator translator) {
			return new PatternMatchDecomposable(translatePatterns(translator, args), var);
		}
	}

	private class PlaceholderSymbol extends SingleReturnCallable<TypedValue> implements ISymbol<TypedValue> {
		private final String var;

		public PlaceholderSymbol(String var) {
			this.var = var;
		}

		@Override
		public TypedValue get() {
			return domain.create(IComposite.class,
					new MappedComposite.Builder()
							.put(PatternProvider.class, new VarPlaceholder(var))
							.put(CompositeTraits.Structured.class, new NamespaceConstructorStruct(var, Collections.<String> emptyList()))
							.build("patternBind"));
		}

		@Override
		public TypedValue call(Frame<TypedValue> frame, Optional<Integer> argumentsCount) {
			Preconditions.checkArgument(argumentsCount.isPresent(), "Type constructor must be always called with arg count");

			final Stack<TypedValue> args = frame.stack().substack(argumentsCount.get());
			final CtorPlaceholder placeholder = new CtorPlaceholder(var, args);
			args.clear();
			return domain.create(IComposite.class, new SingleTraitComposite("patternCtor", placeholder));
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
			return new PlaceholderSymbol(name);
		}

	}

	private static interface IPatternTranslator {
		public PatternPart translatePattern(TypedValue value);
	}

	private static class PatternBuilderVarSymbol extends FixedCallable<TypedValue> implements IPatternTranslator {
		private final PatternBuilderEnv parent;
		private final SymbolMap<TypedValue> placeholderSymbolMap;

		public PatternBuilderVarSymbol(PatternBuilderEnv parent, SymbolMap<TypedValue> placeholderSymbolMap) {
			super(1, 0);
			this.parent = parent;
			this.placeholderSymbolMap = placeholderSymbolMap;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Code pattern = frame.stack().pop().as(Code.class, "variable pattern");
			final TypedValue compiledPattern = evaluatePattern(pattern);
			final PatternPart translatedPattern = translatePattern(compiledPattern);
			parent.addVarPattern(translatedPattern);
		}

		private TypedValue evaluatePattern(Code pattern) {
			final Frame<TypedValue> patternFrame = FrameFactory.symbolsToFrame(placeholderSymbolMap);
			pattern.execute(patternFrame);
			final Stack<TypedValue> resultStack = patternFrame.stack();
			Preconditions.checkState(resultStack.size() == 1, "Invalid result of pattern compilation");
			return resultStack.pop();
		}

		@Override
		public PatternPart translatePattern(TypedValue compiledPattern) {
			if (compiledPattern.is(IComposite.class)) {
				final IComposite composite = compiledPattern.as(IComposite.class);
				if (composite.has(PatternProvider.class)) {
					final PatternProvider p = composite.get(PatternProvider.class);
					return p.getPattern(this);
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
	}

	private static class PatternBuilderGuardedActionSymbol extends FixedCallable<TypedValue> {
		private final PatternBuilderEnv parent;

		public PatternBuilderGuardedActionSymbol(PatternBuilderEnv parent) {
			super(2, 0);
			this.parent = parent;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack();
			final Code action = stack.pop().as(Code.class, "pattern action");
			final Code guard = stack.pop().as(Code.class, "pattern guard");

			parent.addGuardedAction(new GuardedPatternClause(guard, action));
		}
	}

	private static class PatternBuilderDefaultActionSymbol extends FixedCallable<TypedValue> {
		private final PatternBuilderEnv parent;

		public PatternBuilderDefaultActionSymbol(PatternBuilderEnv parent) {
			super(1, 0);
			this.parent = parent;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack();
			final Code action = stack.pop().as(Code.class, "pattern action");

			parent.addDefaultAction(action);
		}
	}

	private class PatternBuilderEnv {
		private final List<PatternPart> varPatterns = Lists.newArrayList();
		private final List<GuardedPatternClause> guardedActions = Lists.newArrayList();
		private Optional<Code> defaultAction = Optional.absent();

		private boolean firstActionAdded;

		public void addVarPattern(PatternPart pattern) {
			Preconditions.checkState(!firstActionAdded, "Trying to add variable pattern after action");
			varPatterns.add(pattern);
		}

		public void addGuardedAction(GuardedPatternClause guardedPatternClause) {
			Preconditions.checkState(!defaultAction.isPresent(), "Trying to add guarded action after default");
			firstActionAdded = true;
			guardedActions.add(guardedPatternClause);
		}

		public void addDefaultAction(Code action) {
			firstActionAdded = true;
			defaultAction = Optional.of(action);
		}

		public IPattern buildPattern() {
			if (guardedActions.isEmpty()) {
				Preconditions.checkState(defaultAction.isPresent(), "Invalid 'pattern' arguments"); // impossible?
				return new UnguardedPattern(varPatterns, defaultAction);
			} else {
				return new GuardedPattern(varPatterns, guardedActions, defaultAction);
			}
		}

	}

	private class PatternSymbol extends FixedCallable<TypedValue> {
		private final SymbolMap<TypedValue> topSymbolMap;

		public PatternSymbol(SymbolMap<TypedValue> topSymbolMap) {
			super(1, 1);
			this.topSymbolMap = topSymbolMap;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			final Stack<TypedValue> stack = frame.stack();

			final Code pattern = stack.pop().as(Code.class, "pattern constructor code (first arg)");

			final IPattern result = evaluatePattern(pattern, topSymbolMap);
			stack.push(domain.create(IComposite.class, new SingleTraitComposite("pattern", result)));
		}

		private IPattern evaluatePattern(Code pattern, SymbolMap<TypedValue> topSymbolMap) {
			final Frame<TypedValue> executionFrame = FrameFactory.createTopFrame();
			final SymbolMap<TypedValue> executionSymbols = executionFrame.symbols();
			final PatternBuilderEnv builder = new PatternBuilderEnv();

			executionSymbols.put(SYMBOL_PATTERN_VAR, new PatternBuilderVarSymbol(builder, new PatternPlaceholdersSymbolMap(topSymbolMap)));
			executionSymbols.put(SYMBOL_GUARDED_ACTION, new PatternBuilderGuardedActionSymbol(builder));
			executionSymbols.put(SYMBOL_DEFAULT_ACTION, new PatternBuilderDefaultActionSymbol(builder));

			pattern.execute(executionFrame);
			Preconditions.checkState(executionFrame.stack().isEmpty(), "Leftovers on pattern execution stack: %s", executionFrame.stack());
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
		private final List<IPattern> patterns;

		public MatchingFunction(SymbolMap<TypedValue> defineScope, List<IPattern> patterns) {
			this.defineScope = defineScope;
			this.patterns = ImmutableList.copyOf(patterns);
		}

		@Override
		public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			final Stack<TypedValue> stack = frame.stack();
			final SymbolMap<TypedValue> env = new ProtectionSymbolMap<TypedValue>(defineScope);
			for (IPattern pattern : patterns) {
				final int args = pattern.requiredArgs();
				if (argumentsCount.isPresent()) {
					if (argumentsCount.get() != args) continue;
				} else {
					if (stack.size() < args) continue;
				}

				final Stack<TypedValue> valuesToMatchStack = stack.substack(args);
				final List<TypedValue> valuesToMatch = ImmutableList.copyOf(valuesToMatchStack);
				final SymbolMap<TypedValue> matchedSymbols = new LocalSymbolMap<TypedValue>(defineScope);
				final Optional<Code> match = pattern.match(env, matchedSymbols, valuesToMatch);
				if (match.isPresent()) {
					valuesToMatchStack.clear();
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

			throw new MatchFailedException("Can't find matching variant");
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

	public void registerSymbols(SymbolMap<TypedValue> env, SymbolMap<TypedValue> patternEnv) {
		env.put(TypedCalcConstants.SYMBOL_MATCH, new MatchSymbol());
		env.put(TypedCalcConstants.SYMBOL_PATTERN, new PatternSymbol(patternEnv));
	}

}
