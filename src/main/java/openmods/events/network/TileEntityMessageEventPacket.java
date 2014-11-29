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

public abstract class TileEntityMessageEventPacket extends NetworkEvent {
	public int dimension;
	public int xCoord;
	public int yCoord;
	public int zCoord;

	public TileEntityMessageEventPacket() {}

	public TileEntityMessageEventPacket(TileEntity tile) {
		dimension = tile.getWorldObj().provider.dimensionId;
		xCoord = tile.xCoord;
		yCoord = tile.yCoord;
		zCoord = tile.zCoord;
	}

	@Override
	protected final void readFromStream(DataInput input) throws IOException {
		dimension = input.readInt();
		xCoord = input.readInt();
		yCoord = input.readInt();
		zCoord = input.readInt();
		readPayload(input);
	}

	protected void readPayload(DataInput input) {
		/**
		 * An empty block should be documented!
		 * Am I doing this right?
		 */
	}

	@Override
	protected final void writeToStream(DataOutput output) throws IOException {
		output.writeInt(dimension);
		output.writeInt(xCoord);
		output.writeInt(yCoord);
		output.writeInt(zCoord);
		writePayload(output);
	}

	protected void writePayload(DataOutput output) {
		/**
		 * An empty block should be documented!
		 * Am I doing this right?
		 */

		/**
		 * LOL NOPE
		 */
	}

	@Override
	protected void appendLogInfo(List<String> info) {
		info.add(String.format("%d,%d,%d", xCoord, yCoord, zCoord));
	}

	public TileEntity getTileEntity() {
		World world = WorldUtils.getWorld(dimension);
		return world.getTileEntity(xCoord, yCoord, zCoord);
	}

	public void sendToWatchers() {
		NetworkEventManager.INSTANCE.dispatcher().senders.block.sendPacket(this, getDimCoords());
	}

	public DimCoord getDimCoords() {
		return new DimCoord(dimension, xCoord, yCoord, zCoord);
	}
}
