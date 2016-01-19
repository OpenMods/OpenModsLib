package openmods.renderer;

import net.minecraft.tileentity.TileEntity;

public interface ITileEntityModel<T extends TileEntity> {

	public void render(T te, float partialTicks);

}
