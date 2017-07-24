package openmods.world;

import java.util.Set;
import net.minecraft.world.chunk.IChunkGenerator;

public interface IStructureGenProvider<T extends IChunkGenerator> {
	public Class<T> getGeneratorCls();

	public Set<String> listStructureNames(T provider);
}