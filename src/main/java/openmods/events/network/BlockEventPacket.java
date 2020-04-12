package openmods.events.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ChunkManager;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkDirection;
import openmods.network.event.NetworkEvent;
import openmods.network.event.NetworkEventManager;

public abstract class BlockEventPacket extends NetworkEvent {
	public RegistryKey<World> dimension;
	public BlockPos blockPos;

	public BlockEventPacket() {}

	public BlockEventPacket(RegistryKey<World> dimension, BlockPos blockPos) {
		this.dimension = dimension;
		this.blockPos = blockPos;
	}

	public BlockEventPacket(TileEntity tile) {
		this(tile.getWorld().getDimensionKey(), tile.getPos());
	}

	@Override
	protected void readFromStream(PacketBuffer input) {
		dimension = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, input.readResourceLocation());
		blockPos = input.readBlockPos();
	}

	@Override
	protected void writeToStream(PacketBuffer output) {
		output.writeResourceLocation(dimension.getLocation());
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
