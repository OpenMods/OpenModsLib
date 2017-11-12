package openmods.world;

import java.util.Set;
import net.minecraft.world.gen.IChunkGenerator;

public interface IStructureGenProvider<T extends IChunkGenerator> {
	public Class<T> getGeneratorCls();

	public Set<String> listStructureNames(T provider);
}