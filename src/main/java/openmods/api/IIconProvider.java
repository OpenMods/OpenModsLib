package openmods.api;

import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

public interface IIconProvider {
	public IIcon getIcon(ForgeDirection rotatedDir);
}
