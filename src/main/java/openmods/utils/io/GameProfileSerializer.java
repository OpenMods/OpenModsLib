package openmods.utils.io;

import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.UUID;
import net.minecraft.network.PacketBuffer;

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
		output.writeVarInt(properties.size());
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
		final String uuidStr = input.readString(Short.MAX_VALUE);
		UUID uuid = Strings.isNullOrEmpty(uuidStr)? null : UUID.fromString(uuidStr);
		final String name = input.readString(Short.MAX_VALUE);
		GameProfile result = new GameProfile(uuid, name);
		int propertyCount = input.readVarInt();

		final PropertyMap properties = result.getProperties();
		for (int i = 0; i < propertyCount; ++i) {
			String key = input.readString(Short.MAX_VALUE);
			String value = input.readString(Short.MAX_VALUE);
			if (input.readBoolean()) {
				String signature = input.readString(Short.MAX_VALUE);
				properties.put(key, new Property(key, value, signature));
			} else {
				properties.put(key, new Property(key, value));
			}

		}

		return result;
	}
}
