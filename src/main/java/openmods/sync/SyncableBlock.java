package openmods.sync;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.GameData;
import openmods.utils.NbtUtils;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

public class SyncableBlock extends SyncableObjectBase implements ISyncableValueProvider<Block> {

	private Block block;

	@Override
	public void readFromStream(PacketBuffer stream) {
		final int blockId = stream.readVarIntFromBuffer();
		block = Block.getBlockById(blockId);
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		int blockId = Block.getIdFromBlock(block);
		if (blockId < 0) blockId = 0;
		stream.writeVarIntToBuffer(blockId);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		ResourceLocation location = GameData.getBlockRegistry().getNameForObject(this.block);
		if (location != null) {
			final NBTTagCompound entry = NbtUtils.store(location);
			nbt.setTag(name, entry);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		if (nbt.hasKey(name, Constants.NBT.TAG_STRING)) {
			final String blockName = nbt.getString(name);
			if (!Strings.isNullOrEmpty(blockName)) {
				final ResourceLocation blockLocation = new ResourceLocation(blockName);
				this.block = GameData.getBlockRegistry().getObject(blockLocation);
			}
		} else if (nbt.hasKey(name, Constants.NBT.TAG_COMPOUND)) {
			final NBTTagCompound entry = nbt.getCompoundTag(name);
			final ResourceLocation blockLocation = NbtUtils.readResourceLocation(entry);
			this.block = GameData.getBlockRegistry().getObject(blockLocation);
		} else {
			this.block = null;
		}
	}

	@Override
	public Block getValue() {
		return Objects.firstNonNull(block, Blocks.air);
	}

	public void setValue(Block block) {
		if (this.block != block) {
			this.block = Objects.firstNonNull(block, Blocks.air);
			markDirty();
		}
	}

	public boolean containsValidBlock() {
		return block != null && block != Blocks.air;
	}
}
