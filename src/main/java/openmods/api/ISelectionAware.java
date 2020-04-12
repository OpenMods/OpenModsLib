package openmods.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawHighlightEvent;

public interface ISelectionAware {
	boolean onSelected(World world, BlockPos blockPos, DrawHighlightEvent evt);
}
