package openmods.calc.types.multi;

import com.google.common.collect.Maps;
import java.util.Map;

public class Symbol {
	public final String value;

	private Symbol(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		return (obj instanceof Symbol) && ((Symbol)obj).value == this.value;
	}

	private static Map<String, Symbol> pool = Maps.newIdentityHashMap();

	public static Symbol get(String value) {
		value = value.intern();
		Symbol result = pool.get(value);
		if (result == null) {
			result = new Symbol(value);
			pool.put(value, result);
		}

		return result;
	}

	@Override
	public String toString() {
		return "#" + value;
	}
}
