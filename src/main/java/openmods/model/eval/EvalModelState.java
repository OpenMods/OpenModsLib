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

	private final boolean shortLived;

	private EvalModelState(Map<String, Float> args, boolean quickCache) {
		this.args = ImmutableMap.copyOf(args);
		this.shortLived = quickCache;
	}

	private EvalModelState() {
		this(ImmutableMap.of(), false);
	}

	public static EvalModelState create() {
		return EMPTY;
	}

	public static EvalModelState create(Map<String, Float> args) {
		return create(args, false);
	}

	public static EvalModelState create(Map<String, Float> args, boolean shortLived) {
		return new EvalModelState(args, false);
	}

	public EvalModelState withArg(String name, float value) {
		Map<String, Float> copy = Maps.newHashMap(args);
		copy.put(name, value);
		return new EvalModelState(copy, this.shortLived);
	}

	public EvalModelState withArg(String name, float value, boolean isRapidChanging) {
		Map<String, Float> copy = Maps.newHashMap(args);
		copy.put(name, value);
		return new EvalModelState(copy, this.shortLived || isRapidChanging);
	}

	public EvalModelState markShortLived() {
		return new EvalModelState(args, true);
	}

	Map<String, Float> getArgs() {
		return args;
	}

	boolean isShortLived() {
		return shortLived;
	}
}
