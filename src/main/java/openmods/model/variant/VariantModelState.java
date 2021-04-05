package openmods.model.variant;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraftforge.client.model.data.ModelProperty;

public class VariantModelState {
	public static final String DEFAULT_MARKER = "<default>";

	public static final ModelProperty<Supplier<VariantModelState>> PROPERTY = new ModelProperty<>();

	public static final VariantModelState EMPTY = new VariantModelState();

	private VariantModelState(Map<String, String> selectors) {
		this.selectors = ImmutableMap.copyOf(selectors);
		hash = selectors.hashCode();
	}

	private VariantModelState() {
		this(ImmutableMap.of());
	}

	public static VariantModelState create() {
		return EMPTY;
	}

	public static VariantModelState create(Map<String, String> selectors) {
		return new VariantModelState(selectors);
	}

	private final Map<String, String> selectors;

	private final int hash;

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
		for (String key : keys) {
			copy.put(key, DEFAULT_MARKER);
		}

		return new VariantModelState(copy);
	}

	public boolean testKey(String name) {
		return selectors.containsKey(name);
	}

	public boolean testKeyValue(String name, String value) {
		String v = selectors.get(name);
		return v != null && v.equals(value);
	}

	public VariantModelState expand(Evaluator evaluator) {
		Map<String, String> expanded = Maps.newHashMap(selectors);
		evaluator.expandVars(expanded);
		return new VariantModelState(expanded);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		return (o instanceof VariantModelState) && selectors.equals(((VariantModelState)o).selectors);
	}

	@Override
	public int hashCode() {
		return hash;
	}
}
