package openmods.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;

public interface IActivateAwareTile {
	boolean onBlockActivated(PlayerEntity player, Hand hand, BlockRayTraceResult hit);
}
