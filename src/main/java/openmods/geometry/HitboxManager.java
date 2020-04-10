package openmods.geometry;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class HitboxManager extends JsonReloadListener {

	public HitboxManager() {
		super(GSON, "hitboxes");
	}

	@SuppressWarnings("serial")
	private static class Hitboxes {
		@SerializedName("boxes")
		private List<Hitbox> hitboxes;
	}

	private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Vec3d.class, (JsonDeserializer<Vec3d>)(json, typeOfT, context) -> {
		JsonArray jsonarray = JSONUtils.getJsonArray(json, "vector");

		if (jsonarray.size() != 3) throw new JsonParseException("Expected 3 float values, found: " + jsonarray.size());

		final float[] coords = new float[3];
		for (int i = 0; i < 3; ++i)
			coords[i] = JSONUtils.getFloat(jsonarray.get(i), "[" + i + "]") / 16.0f;

		return new Vec3d(coords[0], coords[1], coords[2]);
	}).create();

	private class Holder implements IHitboxSupplier {
		private final ResourceLocation location;

		private List<Hitbox> list;

		private Map<String, Hitbox> map;

		public Holder(ResourceLocation location) {
			this.location = location;
		}

		private void reload() {
			this.list = resources.get(location).hitboxes;

			final Map<String, Hitbox> builder = Maps.newLinkedHashMap();
			for (Hitbox hb : list)
				builder.put(hb.name, hb);

			this.map = ImmutableMap.copyOf(builder);
		}

		@Override
		public List<Hitbox> asList() {
			return list;
		}

		@Override
		public Map<String, Hitbox> asMap() {
			return map;
		}

	}

	private final Map<ResourceLocation, Holder> holders = Maps.newHashMap();

	private final Map<ResourceLocation, Hitboxes> resources = Maps.newHashMap();

	public IHitboxSupplier get(ResourceLocation location) {
		synchronized (holders) {
			return holders.computeIfAbsent(location, Holder::new);
		}
	}

	@Override
	protected void apply(final Map<ResourceLocation, JsonObject> data, final IResourceManager resourceManagerIn, final IProfiler profilerIn) {
		resources.clear();
		data.forEach((resourceLocation, jsonObject) -> {
			resources.put(resourceLocation, GSON.fromJson(jsonObject, Hitboxes.class));
		});

		holders.values().forEach(Holder::reload);
	}
}
