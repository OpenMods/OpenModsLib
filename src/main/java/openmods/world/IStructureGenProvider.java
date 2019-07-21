package openmods.world;

import java.util.Set;
import net.minecraft.world.gen.ChunkGenerator;

public interface IStructureGenProvider<T extends ChunkGenerator> {
	Class<T> getGeneratorCls();

	Set<String> listStructureNames(T provider);
}