package openmods.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;

public interface IActivateAwareTile {
	ActionResultType onBlockActivated(PlayerEntity player, Hand hand, BlockRayTraceResult hit);
}
