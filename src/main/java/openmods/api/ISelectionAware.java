package openmods.api;

import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

public interface ISelectionAware {
	boolean onSelected(World world, BlockPos blockPos, DrawBlockHighlightEvent evt);
}
