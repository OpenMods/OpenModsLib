package openmods.network.events;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import openmods.network.EventPacket;
import openmods.network.IEventPacketType;
import openmods.network.PacketHandler;
import openmods.tileentity.OpenTileEntity;

import com.google.common.base.Preconditions;

public class TileEntityMessageEventPacket extends EventPacket {

	public int xCoord;
	public int yCoord;
	public int zCoord;

	public TileEntityMessageEventPacket() {}

	public TileEntityMessageEventPacket(OpenTileEntity tile) {
		xCoord = tile.xCoord;
		yCoord = tile.yCoord;
		zCoord = tile.zCoord;
	}

	@Override
	protected final void readFromStream(DataInput input) throws IOException {
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

	protected World getWorld() {
		return ((EntityPlayer)player).worldObj;
	}

	public OpenTileEntity getTileEntity() {
		World world = getWorld();
		Preconditions.checkNotNull(world, "Invalid packet data");

		TileEntity te = world.getTileEntity(xCoord, yCoord, zCoord);
		return (te instanceof OpenTileEntity)? (OpenTileEntity)te : null;
	}

	public void sendToWatchers(WorldServer world) {
		sendToPlayers(PacketHandler.getPlayersWatchingBlock(world, xCoord, zCoord));
	}

	@Override
	public IEventPacketType getType() {
		return CoreEventTypes.TILE_ENTITY_NOTIFY;
	}
}
