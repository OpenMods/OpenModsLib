package openmods.events.network;

import java.util.List;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
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
		this(tile.getWorld().provider.getDimension(), tile.getPos());
	}

	@Override
	protected void readFromStream(PacketBuffer input) {
		dimension = input.readInt();
		blockPos = input.readBlockPos();
	}

	@Override
	protected void writeToStream(PacketBuffer output) {
		output.writeInt(dimension);
		output.writeBlockPos(blockPos);
	}

	@Override
	protected void appendLogInfo(List<String> info) {
		info.add(String.format("%d -> %s", dimension, blockPos));
	}

	public void sendToWatchers() {
		NetworkEventManager.INSTANCE.dispatcher().senders.block.sendMessage(this, getDimCoords());
	}

	public DimCoord getDimCoords() {
		return new DimCoord(dimension, blockPos);
	}

	public World getWorld() {
		return WorldUtils.getWorld(side, dimension);
	}
}
