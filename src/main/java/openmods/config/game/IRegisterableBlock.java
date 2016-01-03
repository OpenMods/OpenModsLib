package openmods.config.game;

import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.ModContainer;

public interface IRegisterableBlock {
	public void setupBlock(ModContainer container, String blockName, Class<? extends TileEntity> tileEntity, Class<? extends ItemBlock> itemClass);
}
