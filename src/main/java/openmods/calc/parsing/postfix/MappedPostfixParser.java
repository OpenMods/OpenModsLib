package openmods.calc.parsing.postfix;

import com.google.common.collect.Maps;
import java.util.Map;

public abstract class MappedPostfixParser<E> extends PostfixParser<E> {
	public interface IStateProvider<E> {
		public IPostfixParserState<E> createState();
	}

	private final Map<String, IStateProvider<E>> modifierStates = Maps.newHashMap();

	private final Map<String, IStateProvider<E>> bracketStates = Maps.newHashMap();

	@Override
	protected IPostfixParserState<E> createInitialState() {
		return new SimplePostfixParserState<E>(createListBuilder());
	}

	protected abstract IExecutableListBuilder<E> createListBuilder();

	@Override
	protected IPostfixParserState<E> createStateForModifier(String modifier) {
		final IStateProvider<E> stateProvider = modifierStates.get(modifier);
		return stateProvider != null? stateProvider.createState() : super.createStateForModifier(modifier);
	}

	public MappedPostfixParser<E> addModifierStateProvider(String modifier, IStateProvider<E> stateProvider) {
		modifierStates.put(modifier, stateProvider);
		return this;
	}

	@Override
	protected IPostfixParserState<E> createStateForBracket(String bracket) {
		final IStateProvider<E> stateProvider = bracketStates.get(bracket);
		return stateProvider != null? stateProvider.createState() : super.createStateForBracket(bracket);
	}

	public MappedPostfixParser<E> addBracketStateProvider(String modifier, IStateProvider<E> stateProvider) {
		bracketStates.put(modifier, stateProvider);
		return this;
	}
}