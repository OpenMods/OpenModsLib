package openmods.model.variant;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.common.property.IUnlistedProperty;

public class VariantModelState {

	public static final String DEFAULT_MARKER = "<default>";

	public static final IUnlistedProperty<VariantModelState> PROPERTY = new IUnlistedProperty<VariantModelState>() {

		@Override
		public String valueToString(VariantModelState value) {
			return value.selectors.toString();
		}

		@Override
		public boolean isValid(VariantModelState value) {
			return true;
		}

		@Override
		public Class<VariantModelState> getType() {
			return VariantModelState.class;
		}

		@Override
		public String getName() {
			return "selectors";
		}
	};

	public static final VariantModelState EMPTY = new VariantModelState();

	private VariantModelState(Map<String, String> selectors) {
		this.selectors = ImmutableMap.copyOf(selectors);
	}

	private VariantModelState() {
		this(ImmutableMap.<String, String> of());
	}

	public static VariantModelState create() {
		return EMPTY;
	}

	public static VariantModelState create(Map<String, String> selectors) {
		return new VariantModelState(selectors);
	}

	private final Map<String, String> selectors;

	public VariantModelState withKey(String key, String value) {
		Map<String, String> copy = Maps.newHashMap(selectors);
		copy.put(key, value);
		return new VariantModelState(copy);
	}

	public VariantModelState withKey(String key) {
		return withKey(key, DEFAULT_MARKER);
	}

	public VariantModelState withKeys(Set<String> keys) {
		Map<String, String> copy = Maps.newHashMap(selectors);
		for (String key : keys)
			copy.put(key, DEFAULT_MARKER);

		return new VariantModelState(copy);
	}

	Map<String, String> getSelectors() {
		return selectors;
	}
}
