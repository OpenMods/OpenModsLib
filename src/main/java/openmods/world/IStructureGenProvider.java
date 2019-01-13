package openmods.world;

import java.util.Set;
import net.minecraft.world.gen.IChunkGenerator;

public interface IStructureGenProvider<T extends IChunkGenerator> {
	Class<T> getGeneratorCls();

	Set<String> listStructureNames(T provider);
}