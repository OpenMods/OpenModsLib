package openmods.model.eval;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraftforge.common.property.IUnlistedProperty;

public class EvalModelState {

	public static final IUnlistedProperty<EvalModelState> PROPERTY = new IUnlistedProperty<EvalModelState>() {

		@Override
		public String valueToString(EvalModelState value) {
			return value.args.toString();
		}

		@Override
		public boolean isValid(EvalModelState value) {
			return true;
		}

		@Override
		public Class<EvalModelState> getType() {
			return EvalModelState.class;
		}

		@Override
		public String getName() {
			return "eval_args";
		}
	};

	public static final EvalModelState EMPTY = new EvalModelState();

	private final Map<String, Float> args;

	private EvalModelState(Map<String, Float> args) {
		this.args = ImmutableMap.copyOf(args);
	}

	private EvalModelState() {
		this(ImmutableMap.<String, Float> of());
	}

	public static EvalModelState create() {
		return EMPTY;
	}

	public static EvalModelState create(Map<String, Float> args) {
		return new EvalModelState(args);
	}

	public EvalModelState withArg(String name, float value) {
		Map<String, Float> copy = Maps.newHashMap(args);
		copy.put(name, value);
		return new EvalModelState(copy);
	}

	Map<String, Float> getArgs() {
		return args;
	}
}
