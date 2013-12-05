package openmods.config;

import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;

public interface IRegisterableBlock {
	public void setupBlock(String modId, String blockName, Class<? extends TileEntity> tileEntity, Class<? extends ItemBlock> itemClass);
}
