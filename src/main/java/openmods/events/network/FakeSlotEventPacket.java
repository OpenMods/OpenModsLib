package openmods.events.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import cpw.mods.fml.common.network.ByteBufUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import openmods.Log;
import openmods.network.DimCoord;
import openmods.network.event.EventDirection;
import openmods.network.event.NetworkEvent;
import openmods.network.event.NetworkEventMeta;

@NetworkEventMeta(direction = EventDirection.C2S)
public class FakeSlotEventPacket extends BlockEventPacket {
	ItemStack stack;
	int slot;

	public FakeSlotEventPacket() {}

	public FakeSlotEventPacket(ItemStack stack, int slot, DimCoord dimCoords) {
		super(dimCoords.dimension, dimCoords.x, dimCoords.y, dimCoords.z);

		this.slot = slot;
		this.stack = stack;
	}

	@Override
	protected void readFromStream(DataInput input) throws IOException {
		super.readFromStream(input);
		this.slot = input.readInt();
		int length = input.readInt();

		if (length > 0) {
			byte[] data = new byte[length];
			input.readFully(data, 0, length);

			ItemStack stack = ByteBufUtils.readItemStack(Unpooled.copiedBuffer(data));
			this.stack = stack;
		}
	}

	@Override
	protected void writeToStream(DataOutput output) throws IOException {
		super.writeToStream(output);
		output.writeInt(slot);

		ByteBuf buffer = Unpooled.buffer();
		ByteBufUtils.writeItemStack(buffer, stack);
		byte[] data = buffer.array();

		output.writeInt(data.length);
		output.write(data);
	}

}
