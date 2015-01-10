package openmods.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

public class TileEntityInventory extends GenericInventory {

	private final TileEntity owner;

	public TileEntityInventory(TileEntity owner, String name, boolean isInvNameLocalized, int size) {
		super(name, isInvNameLocalized, size);
		this.owner = owner;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return (owner.getWorldObj().getTileEntity(owner.xCoord, owner.yCoord, owner.zCoord) == owner)
				&& (player.getDistanceSq(owner.xCoord + 0.5, owner.yCoord + 0.5, owner.zCoord + 0.5) <= 64.0D);
	}

}
