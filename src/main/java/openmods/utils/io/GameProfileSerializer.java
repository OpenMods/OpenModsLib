package openmods.utils.io;

import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;
import openmods.utils.ByteUtils;

public class GameProfileSerializer implements IStreamSerializer<GameProfile> {
	public static final IStreamSerializer<GameProfile> INSTANCE = new GameProfileSerializer();

	@Override
	public void writeToStream(GameProfile o, DataOutput output) throws IOException {
		write(o, output);
	}

	@Override
	public GameProfile readFromStream(DataInput input) throws IOException {
		return read(input);
	}

	public static void write(GameProfile o, DataOutput output) throws IOException {
		final UUID uuid = o.getId();
		output.writeUTF(uuid == null? "" : uuid.toString());
		output.writeUTF(Strings.nullToEmpty(o.getName()));
		final PropertyMap properties = o.getProperties();
		ByteUtils.writeVLI(output, properties.size());
		for (Property p : properties.values()) {
			output.writeUTF(p.getName());
			output.writeUTF(p.getValue());

			final String signature = p.getSignature();
			if (signature != null) {
				output.writeBoolean(true);
				output.writeUTF(signature);
			} else {
				output.writeBoolean(false);
			}
		}
	}

	public static GameProfile read(DataInput input) throws IOException {
		final String uuidStr = input.readUTF();
		UUID uuid = Strings.isNullOrEmpty(uuidStr)? null : UUID.fromString(uuidStr);
		final String name = input.readUTF();
		GameProfile result = new GameProfile(uuid, name);
		int propertyCount = ByteUtils.readVLI(input);

		final PropertyMap properties = result.getProperties();
		for (int i = 0; i < propertyCount; ++i) {
			String key = input.readUTF();
			String value = input.readUTF();
			if (input.readBoolean()) {
				String signature = input.readUTF();
				properties.put(key, new Property(key, value, signature));
			} else {
				properties.put(key, new Property(key, value));
			}

		}

		return result;
	}
}
