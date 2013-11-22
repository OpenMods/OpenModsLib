package openmods.api;

import net.minecraft.creativetab.CreativeTabs;
import openmods.Log;

public interface IOpenMod {

	public Log getLog();

	public CreativeTabs getCreativeTab();

	public String getId();

	public int getRenderId();
}
