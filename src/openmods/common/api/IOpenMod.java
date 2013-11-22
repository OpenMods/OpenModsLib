package openmods.common.api;

import net.minecraft.creativetab.CreativeTabs;
import openmods.Log;
import openmods.interfaces.IProxy;

public interface IOpenMod {
	public IProxy getProxy();
	public Log getLog();
	public CreativeTabs getCreativeTab();
	public String getId();
	public int getRenderId();
}
