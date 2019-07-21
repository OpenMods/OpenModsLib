package openmods.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class TileEntityInventory extends GenericInventory {

	private final TileEntity owner;

	public TileEntityInventory(TileEntity owner, String name, boolean isInvNameLocalized, int size) {
		super(name, isInvNameLocalized, size);
		this.owner = owner;
	}

	@Override
	public boolean isUsableByPlayer(PlayerEntity player) {
		final BlockPos pos = owner.getPos();
		return (owner.getWorld().getTileEntity(pos) == owner)
				&& (player.getDistanceSq(pos) <= 64.0D);
	}

}
