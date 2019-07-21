package openmods.network.rpc.targets;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import openmods.network.rpc.IRpcTarget;
import openmods.utils.WorldUtils;

public class TileEntityRpcTarget implements IRpcTarget {

	private TileEntity te;

	public TileEntityRpcTarget() {}

	public TileEntityRpcTarget(TileEntity te) {
		this.te = te;
	}

	@Override
	public Object getTarget() {
		return te;
	}

	@Override
	public void writeToStream(PacketBuffer output) {
		output.writeInt(te.getWorld().provider.getDimension());
		output.writeBlockPos(te.getPos());
	}

	@Override
	public void readFromStreamStream(Side side, PlayerEntity player, PacketBuffer input) {
		int worldId = input.readInt();
		BlockPos pos = input.readBlockPos();

		World world = WorldUtils.getWorld(side, worldId);
		te = world.getTileEntity(pos);
	}

	@Override
	public void afterCall() {}

}
