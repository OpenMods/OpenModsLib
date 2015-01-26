package openmods.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import openmods.utils.ByteUtils;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

import cpw.mods.fml.common.registry.GameData;

public class SyncableBlock extends SyncableObjectBase implements ISyncableValueProvider<Block> {

	private Block block;

	@Override
	public void readFromStream(DataInputStream stream) {
		int blockId = ByteUtils.readVLI(stream);
		block = Block.getBlockById(blockId);
	}

	@Override
	public void writeToStream(DataOutputStream stream) {
		int blockId = Block.getIdFromBlock(block);
		if (blockId < 0) blockId = 0;
		ByteUtils.writeVLI(stream, blockId);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		String blockName = GameData.getBlockRegistry().getNameForObject(this.block);
		if (!Strings.isNullOrEmpty(blockName)) nbt.setString(name, blockName);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		String blockName = nbt.getString(name);
		if (!Strings.isNullOrEmpty(blockName)) block = GameData.getBlockRegistry().getObject(blockName);
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
