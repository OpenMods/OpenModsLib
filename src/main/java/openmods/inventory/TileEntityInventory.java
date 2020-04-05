package openmods.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class TileEntityInventory extends GenericInventory {

	private final TileEntity owner;

	public TileEntityInventory(TileEntity owner, int size) {
		super(size);
		this.owner = owner;
	}

	@Override
	public boolean isUsableByPlayer(PlayerEntity player) {
		final BlockPos pos = owner.getPos();
		return (owner.getWorld().getTileEntity(pos) == owner)
				&& player.getDistanceSq((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D) <= 64.0D;
	}

}
