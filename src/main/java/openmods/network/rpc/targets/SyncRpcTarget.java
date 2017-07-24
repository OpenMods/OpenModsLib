package openmods.network.rpc.targets;

import java.io.IOException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.IRpcTargetProvider;
import openmods.sync.ISyncMapProvider;
import openmods.sync.ISyncableObject;
import openmods.sync.SyncMap;

public abstract class SyncRpcTarget implements IRpcTarget {

	private final IRpcTarget syncProvider;

	private ISyncableObject object;

	private int objectId;

	protected SyncRpcTarget(IRpcTarget syncProvider) {
		this.syncProvider = syncProvider;
	}

	protected SyncRpcTarget(IRpcTarget syncProvider, SyncMap map, ISyncableObject object) {
		this(syncProvider);
		this.object = object;
		this.objectId = map.getObjectId(object);
	}

	protected <P extends ISyncMapProvider & IRpcTargetProvider> SyncRpcTarget(P provider, ISyncableObject object) {
		this(provider.createRpcTarget(), provider.getSyncMap(), object);
	}

	@Override
	public Object getTarget() {
		return object;
	}

	@Override
	public void writeToStream(PacketBuffer output) throws IOException {
		syncProvider.writeToStream(output);
		output.writeVarInt(objectId);
	}

	private SyncMap getSyncMap() {
		ISyncMapProvider provider = (ISyncMapProvider)syncProvider.getTarget();
		return provider.getSyncMap();
	}

	@Override
	public void readFromStreamStream(Side side, EntityPlayer player, PacketBuffer input) throws IOException {
		syncProvider.readFromStreamStream(side, player, input);

		SyncMap map = getSyncMap();
		objectId = input.readVarInt();
		object = map.getObjectById(objectId);
	}

	@Override
	public void afterCall() {
		getSyncMap().sendUpdates();
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
