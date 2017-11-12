package openmods.sync;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import openmods.utils.NbtUtils;

public class SyncableBlock extends SyncableObjectBase implements ISyncableValueProvider<Block> {

	private Block block;

	@Override
	public void readFromStream(PacketBuffer stream) {
		final int blockId = stream.readVarInt();
		block = Block.getBlockById(blockId);
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		int blockId = Block.getIdFromBlock(block);
		if (blockId < 0) blockId = 0;
		stream.writeVarInt(blockId);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		ResourceLocation location = Block.REGISTRY.getNameForObject(this.block);
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
				this.block = Block.REGISTRY.getObject(blockLocation);
			}
		} else if (nbt.hasKey(name, Constants.NBT.TAG_COMPOUND)) {
			final NBTTagCompound entry = nbt.getCompoundTag(name);
			final ResourceLocation blockLocation = NbtUtils.readResourceLocation(entry);
			this.block = Block.REGISTRY.getObject(blockLocation);
		} else {
			this.block = null;
		}
	}

	@Override
	public Block getValue() {
		return MoreObjects.firstNonNull(block, Blocks.AIR);
	}

	public void setValue(Block block) {
		if (this.block != block) {
			this.block = MoreObjects.firstNonNull(block, Blocks.AIR);
			markDirty();
		}
	}

	public boolean containsValidBlock() {
		return block != null && block != Blocks.AIR;
	}
}
