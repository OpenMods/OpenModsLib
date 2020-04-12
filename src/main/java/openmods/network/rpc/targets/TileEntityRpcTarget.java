package openmods.network.rpc.targets;

import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
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
		output.writeResourceLocation(te.getWorld().getDimensionKey().getLocation());
		output.writeBlockPos(te.getPos());
	}

	@Override
	public void readFromStreamStream(LogicalSide side, PacketBuffer input) {
		RegistryKey<World> worldId = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, input.readResourceLocation());
		BlockPos pos = input.readBlockPos();

		World world = WorldUtils.getWorld(side, worldId);
		te = world.getTileEntity(pos);
	}

	@Override
	public void afterCall() {}

}
