package openmods.sync;

import net.minecraft.world.World;
import openblocks.OpenBlocks;
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
	public void resetChangeTimer(IProxy proxy, World world) {
		lastChangeTime = proxy.getTicks(world);
	}

	@Override
	public int getTicksSinceChange(IProxy proxy, World world) {
		return (int)(proxy.getTicks(world) - lastChangeTime);
	}
}
