package openmods.utils.io;

import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.BlockPos;
import openmods.utils.NbtUtils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

public abstract class TypeRW<T> implements INBTSerializer<T>, IStreamSerializer<T>, IStringSerializer<T> {

	public static final TypeRW<Integer> INTEGER = new TypeRW<Integer>() {

		@Override
		public void writeToStream(Integer o, PacketBuffer output) {
			output.writeInt(o);
		}

		@Override
		public Integer readFromStream(PacketBuffer input) {
			return input.readInt();
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagInt;
		}

		@Override
		public Integer readFromNBT(NBTTagCompound tag, String name) {
			return tag.getInteger(name);
		}

		@Override
		public void writeToNBT(Integer o, NBTTagCompound tag, String name) {
			tag.setInteger(name, o);
		}

		@Override
		public Integer readFromString(String s) {
			try {
				return Integer.parseInt(s);
			} catch (NumberFormatException e) {
				throw new StringConversionException("integer", s, e);
			}
		}
	};

	public static final TypeRW<Float> FLOAT = new TypeRW<Float>() {

		@Override
		public Float readFromNBT(NBTTagCompound tag, String name) {
			return tag.getFloat(name);
		}

		@Override
		public void writeToNBT(Float o, NBTTagCompound tag, String name) {
			tag.setFloat(name, o);
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagFloat;
		}

		@Override
		public void writeToStream(Float o, PacketBuffer output) {
			output.writeFloat(o);
		}

		@Override
		public Float readFromStream(PacketBuffer input) {
			return input.readFloat();
		}

		@Override
		public Float readFromString(String s) {
			try {
				return Float.parseFloat(s);
			} catch (NumberFormatException e) {
				throw new StringConversionException("float", s, e);
			}
		}

	};

	public static final TypeRW<Double> DOUBLE = new TypeRW<Double>() {

		@Override
		public Double readFromNBT(NBTTagCompound tag, String name) {
			return tag.getDouble(name);
		}

		@Override
		public void writeToNBT(Double o, NBTTagCompound tag, String name) {
			tag.setDouble(name, o);
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagDouble;
		}

		@Override
		public void writeToStream(Double o, PacketBuffer output) {
			output.writeDouble(o);
		}

		@Override
		public Double readFromStream(PacketBuffer input) {
			return input.readDouble();
		}

		@Override
		public Double readFromString(String s) {
			try {
				return Double.parseDouble(s);
			} catch (NumberFormatException e) {
				throw new StringConversionException("double", s, e);
			}
		}
	};

	public static final TypeRW<String> STRING = new TypeRW<String>() {

		@Override
		public String readFromNBT(NBTTagCompound tag, String name) {
			return tag.getString(name);
		}

		@Override
		public void writeToNBT(String o, NBTTagCompound tag, String name) {
			tag.setString(name, o);
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagString;
		}

		@Override
		public void writeToStream(String o, PacketBuffer output) {
			output.writeString(Strings.nullToEmpty(o));
		}

		@Override
		public String readFromStream(PacketBuffer input) {
			return input.readStringFromBuffer(0xFFFF);
		}

		@Override
		public String readFromString(String s) {
			return s;
		}
	};

	public static final TypeRW<Short> SHORT = new TypeRW<Short>() {

		@Override
		public Short readFromNBT(NBTTagCompound tag, String name) {
			return tag.getShort(name);
		}

		@Override
		public void writeToNBT(Short o, NBTTagCompound tag, String name) {
			tag.setShort(name, o);
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagShort;
		}

		@Override
		public void writeToStream(Short o, PacketBuffer output) {
			output.writeShort(o);
		}

		@Override
		public Short readFromStream(PacketBuffer input) {
			return input.readShort();
		}

		@Override
		public Short readFromString(String s) {
			try {
				return Short.parseShort(s);
			} catch (NumberFormatException e) {
				throw new StringConversionException("short", s, e);
			}
		}
	};

	public static final TypeRW<Byte> BYTE = new TypeRW<Byte>() {

		@Override
		public Byte readFromNBT(NBTTagCompound tag, String name) {
			return tag.getByte(name);
		}

		@Override
		public void writeToNBT(Byte o, NBTTagCompound tag, String name) {
			tag.setByte(name, o);
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagByte;
		}

		@Override
		public void writeToStream(Byte o, PacketBuffer output) {
			output.writeByte(o);
		}

		@Override
		public Byte readFromStream(PacketBuffer input) {
			return input.readByte();
		}

		@Override
		public Byte readFromString(String s) {
			try {
				return Byte.parseByte(s);
			} catch (NumberFormatException e) {
				throw new StringConversionException("byte", s, e);
			}
		}
	};

	public static final TypeRW<Boolean> BOOL = new TypeRW<Boolean>() {

		@Override
		public Boolean readFromNBT(NBTTagCompound tag, String name) {
			return tag.getBoolean(name);
		}

		@Override
		public void writeToNBT(Boolean o, NBTTagCompound tag, String name) {
			tag.setBoolean(name, o);
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagByte;
		}

		@Override
		public void writeToStream(Boolean o, PacketBuffer output) {
			output.writeBoolean(o);
		}

		@Override
		public Boolean readFromStream(PacketBuffer input) {
			return input.readBoolean();
		}

		@Override
		public Boolean readFromString(String s) {
			if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
			else if (s.equalsIgnoreCase("false")) return Boolean.FALSE;

			throw new StringConversionException("bool", s, "true", "false");
		}
	};

	public static final TypeRW<Long> LONG = new TypeRW<Long>() {

		@Override
		public Long readFromNBT(NBTTagCompound tag, String name) {
			return tag.getLong(name);
		}

		@Override
		public void writeToNBT(Long o, NBTTagCompound tag, String name) {
			tag.setLong(name, o);
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagLong;
		}

		@Override
		public void writeToStream(Long o, PacketBuffer output) {
			output.writeLong(o);
		}

		@Override
		public Long readFromStream(PacketBuffer input) {
			return input.readLong();
		}

		@Override
		public Long readFromString(String s) {
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException e) {
				throw new StringConversionException("long", s, e);
			}
		}
	};

	public static final TypeRW<byte[]> BYTE_ARRAY = new TypeRW<byte[]>() {

		@Override
		public byte[] readFromString(String s) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeToStream(byte[] o, PacketBuffer output) {
			output.writeByteArray(o);
		}

		@Override
		public byte[] readFromStream(PacketBuffer input) {
			return input.readByteArray();
		}

		@Override
		public void writeToNBT(byte[] o, NBTTagCompound tag, String name) {
			tag.setByteArray(name, o);
		}

		@Override
		public byte[] readFromNBT(NBTTagCompound tag, String name) {
			return tag.getByteArray(name);
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagByteArray;
		}
	};

	public static final IStreamSerializer<Integer> VLI_SERIALIZABLE = new IStreamSerializer<Integer>() {

		@Override
		public void writeToStream(Integer o, PacketBuffer output) {
			output.writeVarIntToBuffer(o);
		}

		@Override
		public Integer readFromStream(PacketBuffer input) {
			return input.readVarIntFromBuffer();
		}
	};

	public static final IStreamSerializer<Character> CHAR = new IStreamSerializer<Character>() {
		@Override
		public void writeToStream(Character o, PacketBuffer output) {
			output.writeChar(o);
		}

		@Override
		public Character readFromStream(PacketBuffer input) {
			return input.readChar();
		}
	};

	public static final ISerializer<BlockPos> BLOCK_POS = new ISerializer<BlockPos>() {

		@Override
		public BlockPos readFromStream(PacketBuffer input) {
			return input.readBlockPos();
		}

		@Override
		public void writeToStream(BlockPos o, PacketBuffer output) {
			output.writeBlockPos(o);
		}

		@Override
		public BlockPos readFromNBT(NBTTagCompound tag, String name) {
			NBTTagCompound coordTag = tag.getCompoundTag(name);
			return NbtUtils.readBlockPos(coordTag);
		}

		@Override
		public void writeToNBT(BlockPos o, NBTTagCompound tag, String name) {
			tag.setTag(name, NbtUtils.store(o));
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagCompound;
		}
	};

	public static final ISerializer<UUID> UUID = new ISerializer<UUID>() {

		@Override
		public UUID readFromStream(PacketBuffer input) {
			return input.readUuid();
		}

		@Override
		public void writeToStream(UUID o, PacketBuffer output) {
			output.writeUuid(o);
		}

		@Override
		public UUID readFromNBT(NBTTagCompound tag, String name) {
			return NbtUtils.readUuid(tag);
		}

		@Override
		public void writeToNBT(UUID o, NBTTagCompound tag, String name) {
			tag.setTag(name, NbtUtils.store(o));
		}

		@Override
		public boolean checkTagType(NBTBase tag) {
			return tag instanceof NBTTagCompound;
		}
	};

	public static final Map<Class<?>, TypeRW<?>> UNIVERSAL_SERIALIZERS = ImmutableMap.<Class<?>, TypeRW<?>> builder()
			.put(Integer.class, INTEGER)
			.put(int.class, INTEGER)
			.put(Boolean.class, BOOL)
			.put(boolean.class, BOOL)
			.put(Byte.class, BYTE)
			.put(byte.class, BYTE)
			.put(Double.class, DOUBLE)
			.put(double.class, DOUBLE)
			.put(Float.class, FLOAT)
			.put(float.class, FLOAT)
			.put(Long.class, LONG)
			.put(long.class, LONG)
			.put(Short.class, SHORT)
			.put(short.class, SHORT)
			.put(String.class, STRING)
			.build();

	public static final Map<Class<?>, IStreamSerializer<?>> STREAM_SERIALIZERS = ImmutableMap.<Class<?>, IStreamSerializer<?>> builder()
			.putAll(UNIVERSAL_SERIALIZERS)
			.put(Character.class, CHAR)
			.put(char.class, CHAR)
			.put(BlockPos.class, BLOCK_POS)
			.put(UUID.class, UUID)
			.build();

	public static final Map<Class<?>, INBTSerializer<?>> NBT_SERIALIZERS = ImmutableMap.<Class<?>, INBTSerializer<?>> builder()
			.putAll(UNIVERSAL_SERIALIZERS)
			.put(BlockPos.class, BLOCK_POS)
			.put(UUID.class, UUID)
			.build();

	public static final Map<Class<?>, IStringSerializer<?>> STRING_SERIALIZERS = ImmutableMap.<Class<?>, IStringSerializer<?>> builder()
			.putAll(UNIVERSAL_SERIALIZERS)
			.build();

	@SuppressWarnings("unchecked")
	public static <T> IStringSerializer<T> getStringSerializer(Class<? extends T> cls) {
		return (IStringSerializer<T>)STRING_SERIALIZERS.get(cls);
	}
}
