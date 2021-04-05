package openmods.model.multipart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import openmods.model.CustomBakedModel;

public class MultipartGeometry implements IModelGeometry<MultipartGeometry> {
	private final List<IUnbakedModel> parts;

	MultipartGeometry(List<IUnbakedModel> parts) {
		this.parts = parts;
	}

	@Override
	public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList overrides, ResourceLocation modelLocation) {
		ImmutableList.Builder<IBakedModel> bakedParts = ImmutableList.builder();
		IModelTransform ownerTransform = owner.getCombinedTransform();
		for (IUnbakedModel part : parts) {
			bakedParts.add(part.bakeModel(bakery, spriteGetter, ownerTransform, modelLocation));
		}
		return new RootBaked(owner, spriteGetter, ownerTransform, bakedParts.build());
	}

	@Override
	public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		Set<RenderMaterial> textures = new HashSet<>();
		for (IUnbakedModel part : parts) {
			textures.addAll(part.getTextures(modelGetter, missingTextureErrors));
		}
		return textures;
	}

	private static class CompositeBaked extends CustomBakedModel {
		protected final List<IBakedModel> bakedParts;

		public CompositeBaked(final IModelConfiguration owner, final Function<RenderMaterial, TextureAtlasSprite> spriteGetter, final IModelTransform transforms, List<IBakedModel> bakedParts) {
			super(owner, spriteGetter, transforms);
			this.bakedParts = bakedParts;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
			List<BakedQuad> quads = Lists.newArrayList();
			for (IBakedModel model : bakedParts) {
				quads.addAll(model.getQuads(state, side, rand, EmptyModelData.INSTANCE));
			}
			return quads;
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemOverrideList.EMPTY;
		}
	}

	private static class RootBaked extends CompositeBaked {
		private final ItemOverrideList overrideList;

		public RootBaked(final IModelConfiguration owner, final Function<RenderMaterial, TextureAtlasSprite> spriteGetter, final IModelTransform transforms, List<IBakedModel> bakedParts) {
			super(owner, spriteGetter, transforms, bakedParts);

			overrideList = new ItemOverrideList() {
				@Override
				public IBakedModel getOverrideModel(IBakedModel model, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity livingEntity) {
					List<IBakedModel> overriddenParts = bakedParts.stream().map(part -> part.getOverrides().getOverrideModel(part, stack, world, livingEntity)).collect(Collectors.toList());
					if (overriddenParts.equals(bakedParts)) {
						return RootBaked.this;
					}
					return new CompositeBaked(owner, spriteGetter, transforms, overriddenParts);
				}
			};
		}

		@Override
		public ItemOverrideList getOverrides() {
			return overrideList;
		}
	}
}
