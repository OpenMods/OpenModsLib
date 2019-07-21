package openmods.model.eval;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.IModelPart;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.animation.IJoint;
import net.minecraftforge.common.property.IExtendedBlockState;
import openmods.model.BakedModelAdapter;

public class BakedEvalModel extends BakedModelAdapter {

	private final IModel model;
	private final IModelState originalState;
	private final VertexFormat format;
	private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
	private final ITransformEvaluator evaluator;

	public BakedEvalModel(IModel model, IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, ITransformEvaluator evaluator) {
		super(model.bake(state, format, bakedTextureGetter), PerspectiveMapWrapper.getTransforms(state));
		this.model = model;
		this.originalState = state;
		this.format = format;
		this.bakedTextureGetter = bakedTextureGetter;
		this.evaluator = evaluator;
	}

	private IBakedModel bakeModelWithTransform(IModelState transform) {
		final IModelState compositeState = new ModelStateComposition(this.originalState, transform);
		return model.bake(compositeState, format, bakedTextureGetter);
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
	public List<BakedQuad> getQuads(BlockState state, Direction side, long rand) {

		if (state instanceof IExtendedBlockState) {
			final IExtendedBlockState extState = (IExtendedBlockState)state;

			final EvalModelState args = extState.getValue(EvalModelState.PROPERTY);
			if (args != null) { return (args.isShortLived()? shortTermCache : longTermCache).getUnchecked(args.getArgs()).getQuads(state, side, rand); }
		}

		return super.getQuads(state, side, rand);
	}
}
