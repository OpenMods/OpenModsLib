package openmods.model.itemstate;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.IModelData;
import openmods.state.ItemState;
import org.apache.commons.lang3.tuple.Pair;

public class ItemStateOverrideList extends ItemOverrideList {

	private class BakedModelWrapper implements IBakedModel {

		protected final IBakedModel original;

		public BakedModelWrapper(IBakedModel original) {
			this.original = original;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
			return original.getQuads(state, side, rand);
		}

		@Nonnull @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
			return original.getQuads(state, side, rand, extraData);
		}

		@Override
		public boolean isAmbientOcclusion() {
			return original.isAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return original.isGui3d();
		}

		@Override
		public boolean isBuiltInRenderer() {
			return original.isBuiltInRenderer();
		}

		@Override
		public TextureAtlasSprite getParticleTexture() {
			return original.getParticleTexture();
		}

		@Override public TextureAtlasSprite getParticleTexture(@Nonnull IModelData data) {
			return original.getParticleTexture(data);
		}

		@Override
		@SuppressWarnings("deprecation")
		public ItemCameraTransforms getItemCameraTransforms() {
			return original.getItemCameraTransforms();
		}

		@Override
		public ItemOverrideList getOverrides() {
			return ItemStateOverrideList.this;
		}

		@Override
		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
			return original.handlePerspective(cameraTransformType);
		}
	}

	IBakedModel wrapModel(IBakedModel original) {
		return new BakedModelWrapper(original);
	}

	private final Map<ItemState, IBakedModel> models;

	public ItemStateOverrideList(Map<ItemState, IBakedModel> models) {
		super();
		this.models = ImmutableMap.copyOf(models);
	}

	@Override
	public IBakedModel getModelWithOverrides(IBakedModel originalModel, @Nonnull ItemStack stack, World world, LivingEntity entity) {
		final Item item = stack.getItem();
		if (item instanceof IStateItem) {
			final ItemState state = ((IStateItem)item).getState(stack, world, entity);
			final IBakedModel result = models.get(state);
			if (result != null)
				return result;
		}

		return originalModel;
	}
}
