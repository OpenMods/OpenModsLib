package openmods.calc.types.multi;

import com.google.common.collect.Maps;
import java.util.Map;

public class Symbol {
	public final String value;

	private Symbol(String value) {
		this.value = value;
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

	public static TypedValue get(TypeDomain domain, String value) {
		return domain.create(Symbol.class, get(value));
	}

	@Override
	public String toString() {
		return "#" + value;
	}

}
