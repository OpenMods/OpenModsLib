package openmods.sync;

import java.util.Set;

public interface ISyncListener {
	public void onSync(Set<ISyncableObject> changes);
}
