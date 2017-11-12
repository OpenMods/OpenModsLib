package openmods.utils;

import net.minecraftforge.common.property.IUnlistedProperty;

public class SimpleUnlistedProperty<T> implements IUnlistedProperty<T> {

	private final Class<T> type;
	private final String name;

	public SimpleUnlistedProperty(Class<T> type, String name) {
		this.type = type;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isValid(T value) {
		return true;
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public String valueToString(T value) {
		return value.toString();
	}

	public static <T> IUnlistedProperty<T> create(Class<T> type, String name) {
		return new SimpleUnlistedProperty<>(type, name);
	}

}
