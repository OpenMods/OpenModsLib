package openmods.events.network;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import openmods.network.DimCoord;
import openmods.network.event.NetworkEvent;
import openmods.network.event.NetworkEventManager;
import openmods.utils.WorldUtils;

public abstract class BlockEventPacket extends NetworkEvent {
	public int dimension;
	public int xCoord;
	public int yCoord;
	public int zCoord;

	public BlockEventPacket() {}

	public BlockEventPacket(int dimension, int xCoord, int yCoord, int zCoord) {
		this.dimension = dimension;
		this.xCoord = xCoord;
		this.yCoord = yCoord;
		this.zCoord = zCoord;
	}

	public BlockEventPacket(TileEntity tile) {
		this(tile.getWorldObj().provider.dimensionId, tile.xCoord, tile.yCoord, tile.zCoord);
	}

	@Override
	protected void readFromStream(DataInput input) throws IOException {
		dimension = input.readInt();
		xCoord = input.readInt();
		yCoord = input.readInt();
		zCoord = input.readInt();
	}

	@Override
	protected void writeToStream(DataOutput output) throws IOException {
		output.writeInt(dimension);
		output.writeInt(xCoord);
		output.writeInt(yCoord);
		output.writeInt(zCoord);
	}

	@Override
	protected void appendLogInfo(List<String> info) {
		info.add(String.format("%d -> %d,%d,%d", dimension, xCoord, yCoord, zCoord));
	}

	public void sendToWatchers() {
		NetworkEventManager.INSTANCE.dispatcher().senders.block.sendPacket(this, getDimCoords());
	}

	public DimCoord getDimCoords() {
		return new DimCoord(dimension, xCoord, yCoord, zCoord);
	}

	public World getWorld() {
		return WorldUtils.getWorld(dimension);
	}
}
