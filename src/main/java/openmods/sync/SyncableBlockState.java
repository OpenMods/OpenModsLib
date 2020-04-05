package openmods.sync;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;

public class SyncableBlockState extends SyncableObjectBase implements ISyncableValueProvider<BlockState> {
	private BlockState state = Blocks.AIR.getDefaultState();

	@Override
	public void readFromStream(PacketBuffer buf) {
		final int id = buf.readVarInt();
		state = Block.getStateById(id);
	}

	@Override
	public void writeToStream(PacketBuffer buf) {
		final int id = Block.getStateId(state);
		buf.writeVarInt(id);
	}

	@Override
	public void writeToNBT(CompoundNBT nbt, String name) {
		nbt.put(name, NBTUtil.writeBlockState(state));
	}

	@SuppressWarnings("deprecation")
	@Override
	public void readFromNBT(CompoundNBT nbt, String name) {
		state = Blocks.AIR.getDefaultState();

		if (nbt.contains(name, Constants.NBT.TAG_COMPOUND)) {
			final CompoundNBT tag = nbt.getCompound(name);
			state = NBTUtil.readBlockState(tag);
		}
	}

	@Override
	public BlockState getValue() {
		return state;
	}

	public void setValue(BlockState state) {
		this.state = state;
		markDirty();
	}

	public boolean isAir() {
		return state == Blocks.AIR.getDefaultState();
	}

}
