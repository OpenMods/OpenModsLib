package openmods.config.game;

import javax.annotation.Nullable;
import net.minecraft.item.BlockItem;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.ModContainer;

@Deprecated
public interface IRegisterableBlock {
	void setupBlock(ModContainer container, String id, Class<? extends TileEntity> tileEntity, @Nullable BlockItem itemBlock);
}
