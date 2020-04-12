package openmods.model.eval;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BasicState;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.model.IModelPart;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.animation.IJoint;
import openmods.model.BakedModelAdapter;

public class BakedEvalModel extends BakedModelAdapter {

	private final IUnbakedModel model;
	private final ModelBakery bakery;
	private final ISprite originalState;
	private final VertexFormat format;
	private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
	private final ITransformEvaluator evaluator;

	public BakedEvalModel(IUnbakedModel model, final ModelBakery bakery, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, ITransformEvaluator evaluator) {
		super(model.bakeModel(bakery, bakedTextureGetter, format), PerspectiveMapWrapper.getTransforms(state.getState()));
		this.model = model;
		this.bakery = bakery;
		this.originalState = state;
		this.format = format;
		this.bakedTextureGetter = bakedTextureGetter;
		this.evaluator = evaluator;
	}

	private IBakedModel bakeModelWithTransform(IModelState transform) {
		final ISprite compositeState = new BasicState(new ModelStateComposition(this.originalState.getState(), transform), originalState.isUvLock());
		return model.bake(bakery, bakedTextureGetter, compositeState, format);
	}

	private final CacheLoader<Map<String, Float>, IBakedModel> loader = new CacheLoader<Map<String, Float>, IBakedModel>() {
		@Override
		public IBakedModel load(final Map<String, Float> key) {
			final IModelState clipTransform = part -> {
				if (!part.isPresent()) return Optional.empty();

				final IModelPart maybeJoint = part.get();
				if (!(maybeJoint instanceof IJoint)) return Optional.empty();

				final IJoint joint = (IJoint)part.get();
				return Optional.of(evaluator.evaluate(joint, key));
			};
			return bakeModelWithTransform(clipTransform);
		}
	};

	private final LoadingCache<Map<String, Float>, IBakedModel> longTermCache = CacheBuilder.newBuilder()
			.expireAfterAccess(5, TimeUnit.MINUTES)
			.build(loader);

	private final LoadingCache<Map<String, Float>, IBakedModel> shortTermCache = CacheBuilder.newBuilder()
			.expireAfterAccess(100, TimeUnit.MILLISECONDS)
			.maximumSize(200)
			.build(loader);

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand, IModelData extState) {
		if (extState != null) {
			final EvalModelState args = extState.getData(EvalModelState.PROPERTY);
			if (args != null) {
				return (args.isShortLived()? shortTermCache : longTermCache).getUnchecked(args.getArgs()).getQuads(state, side, rand, extState);
			}
		}

		return super.getQuads(state, side, rand, extState);
	}
}
