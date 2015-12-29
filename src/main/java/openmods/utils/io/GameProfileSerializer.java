package openmods.utils.io;

import java.util.UUID;

import net.minecraft.network.PacketBuffer;

import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

public class GameProfileSerializer implements IStreamSerializer<GameProfile> {
	public static final IStreamSerializer<GameProfile> INSTANCE = new GameProfileSerializer();

	@Override
	public void writeToStream(GameProfile o, PacketBuffer output) {
		write(o, output);
	}

	@Override
	public GameProfile readFromStream(PacketBuffer input) {
		return read(input);
	}

	public static void write(GameProfile o, PacketBuffer output) {
		final UUID uuid = o.getId();
		output.writeString(uuid == null? "" : uuid.toString());
		output.writeString(Strings.nullToEmpty(o.getName()));
		final PropertyMap properties = o.getProperties();
		output.writeVarIntToBuffer(properties.size());
		for (Property p : properties.values()) {
			output.writeString(p.getName());
			output.writeString(p.getValue());

			final String signature = p.getSignature();
			if (signature != null) {
				output.writeBoolean(true);
				output.writeString(signature);
			} else {
				output.writeBoolean(false);
			}
		}
	}

	public static GameProfile read(PacketBuffer input) {
		final String uuidStr = input.readStringFromBuffer(0xFFFF);
		UUID uuid = Strings.isNullOrEmpty(uuidStr)? null : UUID.fromString(uuidStr);
		final String name = input.readStringFromBuffer(0xFFFF);
		GameProfile result = new GameProfile(uuid, name);
		int propertyCount = input.readVarIntFromBuffer();

		final PropertyMap properties = result.getProperties();
		for (int i = 0; i < propertyCount; ++i) {
			String key = input.readStringFromBuffer(0xFFFF);
			String value = input.readStringFromBuffer(0xFFFF);
			if (input.readBoolean()) {
				String signature = input.readStringFromBuffer(0xFFFF);
				properties.put(key, new Property(key, value, signature));
			} else {
				properties.put(key, new Property(key, value));
			}

		}

		return result;
	}
}
