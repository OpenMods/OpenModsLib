package openmods.model.textureditem;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemOverride;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelRotation;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

public class TexturedItemOverrides extends ItemOverrideList {
	private final IBakedModel untexturedModel;
	private final ResourceLocation texturedModelLocation;
	private final Set<ResourceLocation> placeholders;
	private final ModelBakery bakery;
	private final List<ItemOverride> texturedModelOverrides;

	private final LoadingCache<Pair<ResourceLocation, Optional<ResourceLocation>>, IBakedModel> textureOverrides = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).build(new CacheLoader<Pair<ResourceLocation, Optional<ResourceLocation>>, IBakedModel>() {
		@Override
		public IBakedModel load(Pair<ResourceLocation, Optional<ResourceLocation>> key) {
			final ResourceLocation modelToBake = key.getRight().orElse(texturedModelLocation);
			IUnbakedModel template = bakery.getUnbakedModel(modelToBake);
			return template.bakeModel(bakery, renderMaterial -> {
				ResourceLocation actualSprite;
				if (placeholders.contains(renderMaterial.getTextureLocation())) {
					actualSprite = key.getLeft();
				} else {
					actualSprite = renderMaterial.getTextureLocation();
				}
				return Minecraft.getInstance().getAtlasSpriteGetter(renderMaterial.getAtlasLocation()).apply(actualSprite);
			}, ModelRotation.X0_Y0, modelToBake);
		}
	});

	public TexturedItemOverrides(IBakedModel untexturedModel, ResourceLocation texturedModelLocation, Set<ResourceLocation> placeholders, ModelBakery bakery, List<ItemOverride> texturedModelOverrides) {
		super();
		this.untexturedModel = untexturedModel;
		this.texturedModelLocation = texturedModelLocation;
		this.placeholders = placeholders;
		this.bakery = bakery;
		this.texturedModelOverrides = texturedModelOverrides;
	}

	@Override
	public IBakedModel getOverrideModel(IBakedModel model, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity livingEntity) {
		final Optional<ResourceLocation> texture = getTextureFromStack(stack);
		return texture.map(t -> getTexturedModel(t, stack, world, livingEntity)).orElse(untexturedModel);
	}

	private IBakedModel getTexturedModel(ResourceLocation texture, @Nonnull ItemStack stack, ClientWorld world, LivingEntity entity) {
		final Optional<ResourceLocation> overrideLocation = applyOverride(stack, world, entity);
		return textureOverrides.getUnchecked(Pair.of(texture, overrideLocation));
	}

	private Optional<ResourceLocation> applyOverride(ItemStack stack, ClientWorld world, LivingEntity entity) {
		// TODO 1.14 AT transform
		for (ItemOverride override : texturedModelOverrides) {
			if (override.matchesOverride(stack, world, entity)) {
				return Optional.of(override.getLocation());
			}
		}

		return Optional.empty();
	}

	private static Optional<ResourceLocation> getTextureFromStack(ItemStack stack) {
		return stack.getCapability(ItemTextureCapability.CAPABILITY, null).resolve().flatMap(IItemTexture::getTexture);
	}
}
