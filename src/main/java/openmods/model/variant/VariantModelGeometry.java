package openmods.model.variant;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.geometry.IModelGeometry;

public class VariantModelGeometry implements IModelGeometry<VariantModelGeometry> {
	private final Evaluator evaluator;
	private final IUnbakedModel base;
	private final List<Pair<Predicate<VariantModelState>, IUnbakedModel>> parts;

	public VariantModelGeometry(Evaluator evaluator, IUnbakedModel base, List<Pair<Predicate<VariantModelState>, IUnbakedModel>> parts) {
		this.evaluator = evaluator;
		this.base = base;
		this.parts = parts;
	}

	private static class BakedModel extends BakedModelWrapper<IBakedModel> {
		private final LoadingCache<VariantModelState, Collection<IBakedModel>> cache;

		public BakedModel(IBakedModel originalModel, Evaluator evaluator, List<Pair<Predicate<VariantModelState>, IBakedModel>> parts) {
			super(originalModel);

			cache = CacheBuilder.newBuilder()
					.expireAfterAccess(5, TimeUnit.MINUTES)
					.build(
							new CacheLoader<VariantModelState, Collection<IBakedModel>>() {
								@Override
								public Collection<IBakedModel> load(VariantModelState state) {
									final VariantModelState full = state.expand(evaluator);
									return parts.stream().filter(e -> e.getFirst().test(full)).map(Pair::getSecond).collect(ImmutableSet.toImmutableSet());
								}
							});
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData extState) {
			final VariantModelState modelState = getModelSelectors(extState);

			final List<BakedQuad> result = Lists.newArrayList(originalModel.getQuads(state, side, rand, extState));

			for (final IBakedModel part : cache.getUnchecked(modelState)) {
				result.addAll(part.getQuads(state, side, rand, extState));
			}
			return result;
		}

		private static VariantModelState getModelSelectors(IModelData state) {
			if (state != null) {
				final Supplier<VariantModelState> data = state.getData(VariantModelState.PROPERTY);
				if (data != null) {
					return data.get();
				}
			}

			return VariantModelState.EMPTY;
		}
	}

	@Override
	public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList overrides, ResourceLocation modelLocation) {
		final IBakedModel base = this.base.bakeModel(bakery, spriteGetter, modelTransform, modelLocation);
		List<Pair<Predicate<VariantModelState>, IBakedModel>> parts = this.parts.stream().map(p -> Pair.of(p.getFirst(), p.getSecond().bakeModel(bakery, spriteGetter, modelTransform, modelLocation))).collect(Collectors.toList());
		return new BakedModel(base, evaluator, parts);
	}

	@Override
	public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		return Stream.concat(
				base.getTextures(modelGetter, missingTextureErrors).stream(),
				parts.stream().flatMap(p -> p.getSecond().getTextures(modelGetter, missingTextureErrors).stream())
		).collect(Collectors.toSet());
	}
}
