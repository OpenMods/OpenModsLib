package openmods.gui.logic;

import java.util.Set;

import openmods.sync.ISyncListener;
import openmods.sync.ISyncableObject;

public class SyncObjectUpdateDispatcher extends ValueUpdateDispatcher implements ISyncListener {
	@Override
	public void onSync(Set<ISyncableObject> changes) {
		trigger(changes);
	}
}
