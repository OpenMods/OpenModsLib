package openmods.model.textureditem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelRotation;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import openmods.model.CustomBakedModel;

public class TexturedItemModelGeometry implements IModelGeometry<TexturedItemModelGeometry> {
	private final ResourceLocation untexturedModel;
	private final ResourceLocation texturedModel;
	private final Set<ResourceLocation> placeholders;

	public TexturedItemModelGeometry(ResourceLocation untexturedModel, ResourceLocation texturedModel, Set<ResourceLocation> placeholders) {
		this.untexturedModel = untexturedModel;
		this.texturedModel = texturedModel;
		this.placeholders = placeholders;
	}

	@Override
	public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		final Set<Pair<String, String>> missingTextures = Sets.newHashSet();
		Set<RenderMaterial> result = Sets.newHashSet(modelGetter.apply(texturedModel).getTextures(modelGetter, missingTextures));
		result.removeIf(r -> r.getTextureLocation().equals(MissingTextureSprite.getLocation()));
		return result;
	}

	@Override
	public IBakedModel bake(IModelConfiguration owner, ModelBakery modelBakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform transform, ItemOverrideList overrides, ResourceLocation modelLocation) {
		final IBakedModel untexturedBakedModel = modelBakery.getBakedModel(untexturedModel, ModelRotation.X0_Y0, spriteGetter);
		// Need model just for overrides, bake it with missing textures to avoid placeholder warnings
		final IBakedModel texturedBakedModel = modelBakery.getBakedModel(texturedModel, ModelRotation.X0_Y0, s -> spriteGetter.apply(new RenderMaterial(s.getAtlasLocation(), MissingTextureSprite.getLocation())));
		final ItemOverrideList texturedItemOverrides = new TexturedItemOverrides(untexturedBakedModel, texturedModel, placeholders, modelBakery, texturedBakedModel.getOverrides().getOverrides());
		return new BakedModel(owner, spriteGetter, transform, texturedItemOverrides);
	}

	private static class BakedModel extends CustomBakedModel {
		private final ItemOverrideList overrideList;

		public BakedModel(IModelConfiguration configuration, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform transform, final ItemOverrideList itemOverrideList) {
			super(configuration, spriteGetter, transform);
			this.overrideList = itemOverrideList;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return overrideList;
		}

		@Nonnull
		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
			return ImmutableList.of();
		}
	}

}
