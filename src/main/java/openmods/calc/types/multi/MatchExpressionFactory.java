package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryFunction;
import openmods.calc.BinaryOperator;
import openmods.calc.Environment;
import openmods.calc.Frame;
import openmods.calc.FrameFactory;
import openmods.calc.ICallable;
import openmods.calc.IExecutable;
import openmods.calc.ISymbol;
import openmods.calc.LocalSymbolMap;
import openmods.calc.NestedSymbolMap;
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

	public MatchExpressionFactory(TypeDomain domain, BinaryOperator<TypedValue> splitOperator) {
		this.domain = domain;
		this.split = splitOperator;
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

	private static class Pattern implements ICompositeTrait {
		public final PatternPart pattern;
		public final Code action;

		public Pattern(PatternPart pattern, Code action) {
			this.pattern = pattern;
			this.action = action;
		}
	}

	private class PatternConstructionCompiler {
		private final IExprNode<TypedValue> patternConstructor;
		private final IExprNode<TypedValue> patternAction;

		public PatternConstructionCompiler(IExprNode<TypedValue> patternConstructor, IExprNode<TypedValue> patternAction) {
			this.patternConstructor = patternConstructor;
			this.patternAction = patternAction;
		}

		public void flatten(List<IExecutable<TypedValue>> output) {
			output.add(Value.create(Code.flattenAndWrap(domain, patternConstructor)));
			output.add(Value.create(Code.flattenAndWrap(domain, patternAction)));
			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_PATTERN, 2, 1));
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
			Preconditions.checkState(patternNode.operator == split, "Invalid 'match' syntax, expected split between pattern and code");
			return new PatternConstructionCompiler(patternNode.left, patternNode.right);
		}
	};

	private class MatchStateTransition extends SameStateSymbolTransition<TypedValue> {

		public MatchStateTransition(ICompilerState<TypedValue> compilerState) {
			super(compilerState);
		}

		@Override
		public IExprNode<TypedValue> createRootNode(List<IExprNode<TypedValue>> children) {
			Preconditions.checkArgument(children.size() > 0, "'delay' expects at least one argument");
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

	private class PatternSymbol extends BinaryFunction<TypedValue> {
		private final SymbolMap<TypedValue> placeholderSymbolMap;

		public PatternSymbol(SymbolMap<TypedValue> topSymbolMap) {
			this.placeholderSymbolMap = new PatternPlaceholdersSymbolMap(topSymbolMap);
		}

		@Override
		protected TypedValue call(TypedValue left, TypedValue right) {
			final Code pattern = left.as(Code.class);
			final Code action = right.as(Code.class);

			final TypedValue compiledPattern = evaluatePattern(pattern);
			final PatternPart translatedPatter = translatePattern(compiledPattern);

			return left.domain.create(IComposite.class, new SingleTraitComposite("pattern", new Pattern(translatedPatter, action)));
		}

		private PatternPart translatePattern(TypedValue compiledPattern) {
			if (compiledPattern.is(IComposite.class)) {
				final IComposite composite = compiledPattern.as(IComposite.class);
				if (composite.has(VarPlaceholder.class))	 {
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

		private TypedValue evaluatePattern(final Code pattern) {
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
		private final List<Pattern> patterns;

		public MatchingFunction(SymbolMap<TypedValue> defineScope, List<Pattern> patterns) {
			this.defineScope = defineScope;
			this.patterns = ImmutableList.copyOf(patterns);
		}

		@Override
		public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			if (argumentsCount.isPresent()) {
				final int args = argumentsCount.get();
				if (args != 1) throw new StackValidationException("Expected single argument but got %s", args);
			}

			if (returnsCount.isPresent()) {
				final int returns = returnsCount.get();
				if (returns != 1) throw new StackValidationException("Has single result but expected %s", returns);
			}

			final Stack<TypedValue> stack = frame.stack();
			final TypedValue valueToMatch = stack.pop();

			for (Pattern pattern : patterns) {
				final SymbolMap<TypedValue> matchedSymbols = new LocalSymbolMap<TypedValue>(defineScope);
				if (pattern.pattern.match(matchedSymbols, valueToMatch)) {
					final Frame<TypedValue> matchedFrame = FrameFactory.newClosureFrame(matchedSymbols, frame, 0);
					pattern.action.execute(matchedFrame);
					return;
				}
			}

			throw new MatchFailedException("Unmatched value: " + valueToMatch);
		}

	}

	private class MatchSymbol implements ICallable<TypedValue> {

		@Override
		public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
			if (returnsCount.isPresent()) {
				final int returns = returnsCount.get();
				if (returns != 1) throw new StackValidationException("Has single result but expected %s", returns);
			}

			Preconditions.checkArgument(argumentsCount.isPresent(), "'match' must be called with argument count");

			final Stack<TypedValue> stack = frame.stack();

			final List<Pattern> patterns = Lists.newArrayList();
			for (int i = 0; i < argumentsCount.get(); i++) {
				final TypedValue arg = stack.pop();
				final IComposite patternComposite = arg.as(IComposite.class, "'match' argument");
				patterns.add(patternComposite.get(Pattern.class));
			}

			stack.push(domain.create(ICallable.class, new MatchingFunction(frame.symbols(), Lists.reverse(patterns))));
		}
	}

	public void registerSymbols(Environment<TypedValue> env) {
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_MATCH, new MatchSymbol());
		env.setGlobalSymbol(TypedCalcConstants.SYMBOL_PATTERN, new PatternSymbol(env.topFrame().symbols()));
	}

}
