package openmods.network.rpc.targets;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.IRpcTargetProvider;
import openmods.sync.ISyncMapProvider;
import openmods.sync.ISyncableObject;
import openmods.sync.SyncMap;
import openmods.utils.ByteUtils;

public abstract class SyncRpcTarget implements IRpcTarget {

	private final IRpcTarget syncProvider;

	private ISyncableObject object;

	private int objectId;

	protected SyncRpcTarget(IRpcTarget syncProvider) {
		this.syncProvider = syncProvider;
	}

	protected SyncRpcTarget(IRpcTarget syncProvider, SyncMap<?> map, ISyncableObject object) {
		this(syncProvider);
		this.object = object;
		this.objectId = map.getId(object);
	}

	protected <P extends ISyncMapProvider & IRpcTargetProvider> SyncRpcTarget(P provider, ISyncableObject object) {
		this(provider.createRpcTarget(), provider.getSyncMap(), object);
	}

	@Override
	public Object getTarget() {
		return object;
	}

	@Override
	public void writeToStream(DataOutput output) throws IOException {
		syncProvider.writeToStream(output);
		ByteUtils.writeVLI(output, objectId);
	}

	private SyncMap<?> getSyncMap() {
		ISyncMapProvider provider = (ISyncMapProvider)syncProvider.getTarget();
		return provider.getSyncMap();
	}

	@Override
	public void readFromStreamStream(EntityPlayer player, DataInput input) throws IOException {
		syncProvider.readFromStreamStream(player, input);

		SyncMap<?> map = getSyncMap();
		objectId = ByteUtils.readVLI(input);
		object = map.get(objectId);
	}

	@Override
	public void afterCall() {
		getSyncMap().sync();
	}

	public static class SyncTileEntityRpcTarget extends SyncRpcTarget {
		public SyncTileEntityRpcTarget() {
			super(new TileEntityRpcTarget());
		}

		public <P extends TileEntity & ISyncMapProvider & IRpcTargetProvider> SyncTileEntityRpcTarget(P provider, ISyncableObject object) {
			super(provider, object);
		}
	}

	public static class SyncEntityRpcTarget extends SyncRpcTarget {
		public SyncEntityRpcTarget() {
			super(new EntityRpcTarget());
		}

		public <P extends Entity & ISyncMapProvider & IRpcTargetProvider> SyncEntityRpcTarget(P provider, ISyncableObject object) {
			super(provider, object);
		}
	}

}
