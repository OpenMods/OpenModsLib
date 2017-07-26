package openmods.geometry;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import openmods.Log;

public class HitboxManager implements IResourceManagerReloadListener {

	@SuppressWarnings("serial")
	private static class HitboxList extends ArrayList<Hitbox> {}

	private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Vec3d.class, new JsonDeserializer<Vec3d>() {
		@Override
		public Vec3d deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonArray jsonarray = JsonUtils.getJsonArray(json, "vector");

			if (jsonarray.size() != 3) throw new JsonParseException("Expected 3 float values, found: " + jsonarray.size());

			final float[] coords = new float[3];
			for (int i = 0; i < 3; ++i)
				coords[i] = JsonUtils.getFloat(jsonarray.get(i), "[" + i + "]") / 16.0f;

			return new Vec3d(coords[0], coords[1], coords[2]);
		}
	}).create();

	private class Holder implements IHitboxSupplier {
		private final ResourceLocation location;

		private List<Hitbox> list;

		private Map<String, Hitbox> map;

		public Holder(ResourceLocation location) {
			this.location = new ResourceLocation(location.getResourceDomain(), "hitboxes/" + location.getResourcePath() + ".json");
		}

		private void reload() {
			this.list = ImmutableList.copyOf(load(location));

			final Map<String, Hitbox> builder = Maps.newLinkedHashMap();
			for (Hitbox hb : list)
				builder.put(hb.name, hb);

			this.map = ImmutableMap.copyOf(builder);
		}

		@Override
		public List<Hitbox> asList() {
			if (list == null)
				reload();

			return list;
		}

		@Override
		public Map<String, Hitbox> asMap() {
			if (map == null)
				reload();

			return map;
		}

	}

	private IResourceManager resourceManager;

	private final Map<ResourceLocation, Holder> holders = Maps.newHashMap();

	private List<Hitbox> load(ResourceLocation location) {
		final List<Hitbox> result = Lists.newArrayList();
		if (resourceManager != null) {
			try {
				IResource resource = resourceManager.getResource(location);

				Closer closer = Closer.create();
				try {
					final InputStream is = closer.register(resource.getInputStream());
					final Reader reader = closer.register(new InputStreamReader(is, Charsets.UTF_8));
					final HitboxList list = GSON.fromJson(reader, HitboxList.class);
					result.addAll(list);
				} catch (Throwable t) {
					throw closer.rethrow(t);
				} finally {
					closer.close();
				}
			} catch (IOException e) {
				Log.warn(e, "Failed to find hitbox %s", location);
			}
		}

		return result;
	}

	public IHitboxSupplier get(ResourceLocation location) {
		synchronized (holders) {
			Holder result = holders.get(location);
			if (result == null) {
				result = new Holder(location);
				holders.put(location, result);
			}
			return result;
		}
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		this.resourceManager = resourceManager;

		if (resourceManager != null) {
			synchronized (holders) {
				for (Holder holder : holders.values())
					holder.reload();
			}
		}
	}

}
