package openmods.sync;

import net.minecraft.world.World;
import openmods.OpenMods;
import openmods.interfaces.IProxy;

public abstract class SyncableObjectBase implements ISyncableObject {

	protected long lastChangeTime = 0;
	protected boolean dirty = false;

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void markClean() {
		dirty = false;
	}

	@Override
	public void markDirty() {
		dirty = true;
	}

	@Override
	public void resetChangeTimer(World world) {
		lastChangeTime = OpenMods.proxy.getTicks(world);
	}

	@Override
	public int getTicksSinceChange(World world) {
		return (int)(OpenMods.proxy.getTicks(world) - lastChangeTime);
	}
}
