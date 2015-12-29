package openmods.events.network;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import openmods.network.DimCoord;
import openmods.network.event.NetworkEvent;
import openmods.network.event.NetworkEventManager;
import openmods.utils.WorldUtils;

public abstract class BlockEventPacket extends NetworkEvent {
	public int dimension;
	public BlockPos blockPos;

	public BlockEventPacket() {}

	public BlockEventPacket(int dimension, BlockPos blockPos) {
		this.dimension = dimension;
		this.blockPos = blockPos;
	}

	public BlockEventPacket(TileEntity tile) {
		this(tile.getWorld().provider.dimensionId, tile.getPos());
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
		NetworkEventManager.INSTANCE.dispatcher().senders.block.sendMessage(this, getDimCoords());
	}

	public DimCoord getDimCoords() {
		return new DimCoord(dimension, xCoord, yCoord, zCoord);
	}

	public World getWorld() {
		return WorldUtils.getWorld(dimension);
	}
}
