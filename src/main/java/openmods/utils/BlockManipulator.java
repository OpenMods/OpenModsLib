package openmods.utils;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;

public class BlockManipulator {

	public final World world;

	public final PlayerEntity player;

	public final BlockPos blockPos;

	private boolean spawnProtection = true;

	private boolean eventCheck = true;

	private boolean silentTeRemove = false;

	private int blockPlaceFlags = Constants.BlockFlags.DEFAULT;

	public BlockManipulator(@Nonnull World world, @Nonnull PlayerEntity player, BlockPos blockPos) {
		Preconditions.checkNotNull(world);
		this.world = world;

		Preconditions.checkNotNull(player);
		this.player = player;
		this.blockPos = blockPos;
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
		if (!world.isBlockLoaded(blockPos)) return false;

		if (spawnProtection) {
			if (!world.isBlockModifiable(player, blockPos)) return false;
		}

		if (eventCheck) {
			final BlockState blockState = world.getBlockState(blockPos);
			BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, blockPos, blockState, player);
			event.setExpToDrop(0);

			MinecraftForge.EVENT_BUS.post(event);

			if (event.isCanceled()) return false;
		}

		if (silentTeRemove) world.removeTileEntity(blockPos);

		return world.setBlockState(blockPos, Blocks.AIR.getDefaultState());
	}

	public boolean place(BlockState state, Direction direction, Hand hand) {
		if (!world.isBlockLoaded(blockPos)) return false;

		if (spawnProtection) {
			if (!world.isBlockModifiable(player, blockPos)) return false;
		}

		final BlockSnapshot snapshot = BlockSnapshot.getBlockSnapshot(world, blockPos);

		if (!world.setBlockState(blockPos, state, blockPlaceFlags)) return false;

		if (ForgeEventFactory.onBlockPlace(player, snapshot, direction)) {
			world.restoringBlockSnapshots = true;
			snapshot.restore(true, false);
			world.restoringBlockSnapshots = false;
			return false;
		}

		return true;
	}
}
