package openmods.fixers;

import net.minecraft.util.datafix.DataFixer;

public interface IFixerFactory {
	void register(DataFixer registry, Class<?> registeringClass);
}
