package openmods.utils;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;

public class BlockManipulator {

	public final World world;

	public final EntityPlayer player;

	public int x;

	public int y;

	public int z;

	private boolean spawnProtection = true;

	private boolean eventCheck = true;

	private boolean silentTeRemove = false;

	private int blockPlaceFlags = BlockNotifyFlags.ALL;

	public BlockManipulator(@Nonnull World world, @Nonnull EntityPlayer player, int x, int y, int z) {
		Preconditions.checkNotNull(world);
		this.world = world;

		Preconditions.checkNotNull(player);
		this.player = player;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public BlockManipulator setSpawnProtection(boolean value) {
		this.spawnProtection = value;
		return this;
	}

	public BlockManipulator setEventCheck(boolean value) {
		this.eventCheck = value;
		return this;
	}

	public BlockManipulator setSilentTeRemove(boolean value) {
		this.silentTeRemove = value;
		return this;
	}

	public BlockManipulator setBlockPlaceFlags(int value) {
		this.blockPlaceFlags = value;
		return this;
	}

	public boolean remove() {
		if (!world.blockExists(x, y, z)) return false;

		if (spawnProtection) {
			if (!world.canMineBlock(player, x, y, z)) return false;
		}

		if (eventCheck) {
			final Block block = world.getBlock(x, y, z);
			final int meta = world.getBlockMetadata(x, y, z);
			BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(x, y, z, world, block, meta, player);
			event.setExpToDrop(0);

			MinecraftForge.EVENT_BUS.post(event);

			if (event.isCanceled()) return false;
		}

		if (silentTeRemove) world.removeTileEntity(x, y, z);

		return world.setBlockToAir(x, y, z);
	}

	public boolean place(Block block, int meta) {
		if (!world.blockExists(x, y, z)) return false;

		if (spawnProtection) {
			if (!world.canMineBlock(player, x, y, z)) return false;
		}

		final BlockSnapshot snapshot = net.minecraftforge.common.util.BlockSnapshot.getBlockSnapshot(world, x, y, z);

		if (!world.setBlock(x, y, z, block, meta, blockPlaceFlags)) return false;

		if (ForgeEventFactory.onPlayerBlockPlace(player, snapshot, net.minecraftforge.common.util.ForgeDirection.UNKNOWN).isCanceled()) {
			world.restoringBlockSnapshots = true;
			snapshot.restore(true, false);
			world.restoringBlockSnapshots = false;
			return false;
		}

		return true;
	}
}
