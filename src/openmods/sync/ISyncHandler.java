package openmods.sync;

import java.util.Set;

import openmods.interfaces.IProxy;

public interface ISyncHandler {

	public SyncMap<?> getSyncMap();
	
	public IProxy getProxy();

	public void onSynced(Set<ISyncableObject> changes);

}
