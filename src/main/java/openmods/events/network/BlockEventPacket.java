package openmods.events.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ChunkManager;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkDirection;
import openmods.network.event.NetworkEvent;
import openmods.network.event.NetworkEventManager;
import openmods.utils.WorldUtils;

public abstract class BlockEventPacket extends NetworkEvent {
	public DimensionType dimension;
	public BlockPos blockPos;

	public BlockEventPacket() {}

	public BlockEventPacket(DimensionType dimension, BlockPos blockPos) {
		this.dimension = dimension;
		this.blockPos = blockPos;
	}

	public BlockEventPacket(TileEntity tile) {
		this(tile.getWorld().getDimension().getType(), tile.getPos());
	}

	@Override
	protected void readFromStream(PacketBuffer input) {
		DimensionType.getById(input.readInt());
		blockPos = input.readBlockPos();
	}

	@Override
	protected void writeToStream(PacketBuffer output) {
		output.writeInt(dimension.getId());
		output.writeBlockPos(blockPos);
	}

	private static MinecraftServer getServer() {
		return LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
	}

	public void sendToWatchers() {
		final ChunkPos pos = new ChunkPos(blockPos);
		final ChunkManager chunkManager = getServer().getWorld(dimension).getChunkProvider().chunkManager;
		NetworkEventManager.dispatcher().send(this, NetworkDirection.PLAY_TO_CLIENT, p -> chunkManager.getTrackingPlayers(pos, false).forEach(e -> e.connection.sendPacket(p)));
	}
}
