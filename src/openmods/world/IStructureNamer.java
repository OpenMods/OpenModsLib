package openmods.world;

import net.minecraft.world.gen.structure.MapGenStructure;

public interface IStructureNamer {
	public String identify(MapGenStructure structure);
}