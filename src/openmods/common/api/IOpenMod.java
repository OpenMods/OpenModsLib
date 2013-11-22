package openmods.common.api;

import openmods.Log;
import openmods.interfaces.IProxy;

public interface IOpenMod {
	public IProxy getProxy();
	public Log getLog();
}
