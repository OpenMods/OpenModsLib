package openmods.model.eval;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.IExtendedBlockState;
import openmods.model.BakedModelAdapter;

public class BakedEvalModel extends BakedModelAdapter {

	private IModel model;
	private IModelState originalState;
	private VertexFormat format;
	private Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;
	private IEvaluator evaluator;

	public BakedEvalModel(IModel model, IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, IEvaluator evaluator) {
		super(model.bake(state, format, bakedTextureGetter), MapWrapper.getTransforms(state));
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

	private final LoadingCache<Map<String, Float>, IBakedModel> cache = CacheBuilder.newBuilder()
			.expireAfterAccess(5, TimeUnit.MINUTES)
			.build(
					new CacheLoader<Map<String, Float>, IBakedModel>() {
						@Override
						public IBakedModel load(Map<String, Float> key) throws Exception {
							final IModelState clipTransform = evaluator.evaluate(key);
							return bakeModelWithTransform(clipTransform);
						}
					});

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {

		if (state instanceof IExtendedBlockState) {
			final IExtendedBlockState extState = (IExtendedBlockState)state;

			final EvalModelState args = extState.getValue(EvalModelState.PROPERTY);
			if (args != null) return cache.getUnchecked(args.getArgs()).getQuads(state, side, rand);
		}

		return super.getQuads(state, side, rand);
	}
}
