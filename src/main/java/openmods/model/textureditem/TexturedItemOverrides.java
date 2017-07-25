package openmods.model.textureditem;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IRetexturableModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import org.apache.commons.lang3.tuple.Pair;

public class TexturedItemOverrides extends ItemOverrideList {

	private final IBakedModel untexturedModel;
	private final IModel texturedModel;
	private final Set<String> texturesToReplace;
	private final IModelState state;
	private final VertexFormat format;
	private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;

	private final LoadingCache<Pair<ResourceLocation, Optional<ResourceLocation>>, IBakedModel> textureOverrides = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).build(new CacheLoader<Pair<ResourceLocation, Optional<ResourceLocation>>, IBakedModel>() {
		@Override
		public IBakedModel load(Pair<ResourceLocation, Optional<ResourceLocation>> key) throws Exception {
			final IModel overrideModel = getOverrideModel(key.getRight());
			final IModel retexturedModel = retextureModel(overrideModel, key.getKey());
			return retexturedModel.bake(state, format, bakedTextureGetter);
		}
	});

	public TexturedItemOverrides(IBakedModel untexturedModel, IModel texturedModel, List<ItemOverride> texturedModelOverrides, Set<String> texturesToReplace, IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		super(texturedModelOverrides);
		this.untexturedModel = untexturedModel;
		this.texturedModel = texturedModel;
		this.texturesToReplace = ImmutableSet.copyOf(texturesToReplace);
		this.state = state;
		this.format = format;
		this.bakedTextureGetter = bakedTextureGetter;
	}

	@Override
	public IBakedModel handleItemState(IBakedModel originalModel, @Nonnull ItemStack stack, World world, EntityLivingBase entity) {
		final Optional<ResourceLocation> texture = getTextureFromStack(stack);
		return texture.isPresent()? rebakeModel(texture.get(), stack, world, entity) : untexturedModel;
	}

	private IBakedModel rebakeModel(ResourceLocation texture, @Nonnull ItemStack stack, World world, EntityLivingBase entity) {
		@SuppressWarnings("deprecation")
		final Optional<ResourceLocation> overrideLocation = Optional.fromNullable(applyOverride(stack, world, entity));
		return textureOverrides.getUnchecked(Pair.of(texture, overrideLocation));
	}

	private IModel getOverrideModel(Optional<ResourceLocation> overrideLocation) {
		if (overrideLocation.isPresent()) {
			final ResourceLocation location = overrideLocation.get();
			return ModelLoaderRegistry.getModelOrLogError(location, "Couldn't load model: " + location);
		} else {
			return texturedModel;
		}
	}

	private IModel retextureModel(IModel overrideModel, ResourceLocation texture) {
		if (overrideModel instanceof IRetexturableModel) {
			final ImmutableMap.Builder<String, String> textures = ImmutableMap.builder();
			for (String t : texturesToReplace)
				textures.put(t, texture.toString());

			return ((IRetexturableModel)overrideModel).retexture(textures.build());
		}

		return overrideModel;
	}

	private static Optional<ResourceLocation> getTextureFromStack(@Nonnull ItemStack stack) {
		if (stack.hasCapability(ItemTextureCapability.CAPABILITY, null)) {
			final IItemTexture fluidRender = stack.getCapability(ItemTextureCapability.CAPABILITY, null);
			return fluidRender.getTexture();
		}

		return Optional.absent();
	}
}
