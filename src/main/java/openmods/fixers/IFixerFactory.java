package openmods.fixers;

import net.minecraft.util.datafix.DataFixer;

public interface IFixerFactory {
	public void register(DataFixer registry, Class<?> registeringClass);
}
