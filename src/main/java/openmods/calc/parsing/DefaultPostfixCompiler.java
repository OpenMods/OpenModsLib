package openmods.calc.parsing;

import com.google.common.collect.Maps;
import java.util.Map;
import openmods.calc.OperatorDictionary;

public class DefaultPostfixCompiler<E> extends PostfixCompiler<E> {
	public interface IStateProvider<E> {
		public IPostfixCompilerState<E> createState();
	}

	private final IValueParser<E> valueParser;
	private final OperatorDictionary<E> operators;

	private final Map<String, IStateProvider<E>> modifierStates = Maps.newHashMap();

	private final Map<String, IStateProvider<E>> bracketStates = Maps.newHashMap();

	public DefaultPostfixCompiler(IValueParser<E> valueParser, OperatorDictionary<E> operators) {
		this.valueParser = valueParser;
		this.operators = operators;
	}

	@Override
	protected IPostfixCompilerState<E> createInitialState() {
		return new SimplePostfixCompilerState<E>(new DefaultExecutableListBuilder<E>(valueParser, operators));
	}

	@Override
	protected IPostfixCompilerState<E> createStateForModifier(String modifier) {
		final IStateProvider<E> stateProvider = modifierStates.get(modifier);
		return stateProvider != null? stateProvider.createState() : super.createStateForModifier(modifier);
	}

	public DefaultPostfixCompiler<E> addModifierStateProvider(String modifier, IStateProvider<E> stateProvider) {
		modifierStates.put(modifier, stateProvider);
		return this;
	}

	@Override
	protected IPostfixCompilerState<E> createStateForBracket(String bracket) {
		final IStateProvider<E> stateProvider = bracketStates.get(bracket);
		return stateProvider != null? stateProvider.createState() : super.createStateForBracket(bracket);
	}

	public DefaultPostfixCompiler<E> addBracketStateProvider(String modifier, IStateProvider<E> stateProvider) {
		bracketStates.put(modifier, stateProvider);
		return this;
	}
}