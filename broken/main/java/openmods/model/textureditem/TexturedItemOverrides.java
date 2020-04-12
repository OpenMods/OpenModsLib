package openmods.model.textureditem;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemOverride;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.apache.commons.lang3.tuple.Pair;

public class TexturedItemOverrides extends ItemOverrideList {

	private final IBakedModel untexturedModel;
	private final IUnbakedModel texturedModel;
	private final Set<String> texturesToReplace;
	private final VertexFormat format;
	private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
	private final ModelBakery bakery;
	private final List<ItemOverride> texturedModelOverrides;

	private final LoadingCache<Pair<ResourceLocation, Optional<ResourceLocation>>, IBakedModel> textureOverrides = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).build(new CacheLoader<Pair<ResourceLocation, Optional<ResourceLocation>>, IBakedModel>() {
		@Override
		public IBakedModel load(Pair<ResourceLocation, Optional<ResourceLocation>> key) {
			final IUnbakedModel overrideModel = getOverrideModel(key.getRight());
			final IModel retexturedModel = retextureModel(overrideModel, key.getKey());
			return retexturedModel.bake(bakery, bakedTextureGetter, state, format);
		}
	});

	public TexturedItemOverrides(IBakedModel untexturedModel, IUnbakedModel texturedModel, final ModelBakery bakery, List<ItemOverride> texturedModelOverrides, Set<String> texturesToReplace, ISprite state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		super();
		this.untexturedModel = untexturedModel;
		this.texturedModel = texturedModel;
		this.bakery = bakery;
		this.texturesToReplace = ImmutableSet.copyOf(texturesToReplace);
		this.state = state;
		this.format = format;
		this.bakedTextureGetter = bakedTextureGetter;
		this.texturedModelOverrides = texturedModelOverrides;
	}

	@Override
	public IBakedModel getOverrideModel(IBakedModel model, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity livingEntity) {
		final Optional<ResourceLocation> texture = getTextureFromStack(stack);
		return texture.isPresent()? rebakeModel(texture.get(), stack, world, livingEntity) : untexturedModel;
	}

	private IBakedModel rebakeModel(ResourceLocation texture, @Nonnull ItemStack stack, World world, LivingEntity entity) {
		@SuppressWarnings("deprecation")
		final Optional<ResourceLocation> overrideLocation = applyOverride(stack, world, entity);
		return textureOverrides.getUnchecked(Pair.of(texture, overrideLocation));
	}

	private Optional<ResourceLocation> applyOverride(ItemStack stack, World world, LivingEntity entity) {
		// TODO 1.14 AT transform
		for (ItemOverride override : texturedModelOverrides) {
			if (override.matchesOverride(stack, world, entity)) {
				return Optional.of(override.getLocation());
			}
		}

		return Optional.empty();
	}

	private IUnbakedModel getOverrideModel(Optional<ResourceLocation> overrideLocation) {
		if (overrideLocation.isPresent()) {
			final ResourceLocation location = overrideLocation.get();
			return ModelLoaderRegistry.getModelOrLogError(location, "Couldn't load model: " + location);
		} else {
			return texturedModel;
		}
	}

	private IBakedModel retextureModel(IBakedModel overrideModel, ResourceLocation texture) {
		final ImmutableMap.Builder<String, String> textures = ImmutableMap.builder();
		for (String t : texturesToReplace)
			textures.put(t, texture.toString());

		return overrideModel.retexture(textures.build());
	}

	private static Optional<ResourceLocation> getTextureFromStack(@Nonnull ItemStack stack) {
		// TODO 1.14 Request flat map from Forge!
		return stack.getCapability(ItemTextureCapability.CAPABILITY, null)
				.map(IItemTexture::getTexture)
				.orElse(Optional.empty());
	}
}
