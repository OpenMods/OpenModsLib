package openmods.model.itemstate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import openmods.state.State;
import org.apache.commons.lang3.tuple.Pair;

public class ItemStateOverrideList extends ItemOverrideList {

	private class BakedModelWrapper implements IBakedModel {

		protected final IBakedModel original;

		public BakedModelWrapper(IBakedModel original) {
			this.original = original;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
			return original.getQuads(state, side, rand);
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
		public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
			return original.handlePerspective(cameraTransformType);
		}
	}

	IBakedModel wrapModel(IBakedModel original) {
		return new BakedModelWrapper(original);
	}

	private final Map<State, IBakedModel> models;

	public ItemStateOverrideList(Map<State, IBakedModel> models) {
		super(ImmutableList.of());
		this.models = ImmutableMap.copyOf(models);
	}

	@Override
	public IBakedModel handleItemState(IBakedModel originalModel, @Nonnull ItemStack stack, World world, EntityLivingBase entity) {
		final Item item = stack.getItem();
		if (item instanceof IStateItem) {
			final State state = ((IStateItem)item).getState(stack, world, entity);
			final IBakedModel result = models.get(state);
			if (result != null)
				return result;
		}

		return originalModel;
	}
}
