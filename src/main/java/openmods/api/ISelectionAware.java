package openmods.api;

import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

public interface ISelectionAware {
	boolean onSelected(World world, int x, int y, int z, DrawBlockHighlightEvent evt);
}
