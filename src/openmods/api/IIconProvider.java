package openmods.api;

import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;

public interface IIconProvider {
	public Icon getIcon(ForgeDirection rotatedDir);
}
