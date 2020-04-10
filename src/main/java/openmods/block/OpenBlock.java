package openmods.block;

import com.google.common.base.Preconditions;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import openmods.api.IActivateAwareTile;
import openmods.api.IAddAwareTile;
import openmods.api.IBreakAwareTile;
import openmods.api.ICustomBreakDrops;
import openmods.api.ICustomHarvestDrops;
import openmods.api.ICustomPickItem;
import openmods.api.IHasGui;
import openmods.api.INeighbourAwareTile;
import openmods.api.INeighbourTeAwareTile;
import openmods.api.IPlaceAwareTile;
import openmods.api.ISurfaceAttachment;
import openmods.geometry.LocalDirections;
import openmods.geometry.Orientation;
import openmods.inventory.IInventoryProvider;
import openmods.tileentity.OpenTileEntity;

public class OpenBlock extends Block {

	public static class TwoDirections extends OpenBlock {
		public TwoDirections(Properties properties) {
			super(properties);
		}

		@Override
		public BlockRotationMode getRotationMode() {
			return BlockRotationMode.TWO_DIRECTIONS;
		}
	}

	public static class ThreeDirections extends OpenBlock {
		public ThreeDirections(Properties properties) {
			super(properties);
		}

		@Override
		public BlockRotationMode getRotationMode() {
			return BlockRotationMode.THREE_DIRECTIONS;
		}
	}

	public static class FourDirections extends OpenBlock {
		public FourDirections(Properties properties) {
			super(properties);
		}

		@Override
		public BlockRotationMode getRotationMode() {
			return BlockRotationMode.FOUR_DIRECTIONS;
		}
	}

	public static class SixDirections extends OpenBlock {
		public SixDirections(Properties properties) {
			super(properties);
		}

		@Override
		public BlockRotationMode getRotationMode() {
			return BlockRotationMode.SIX_DIRECTIONS;
		}
	}

	public static final int OPEN_MODS_TE_GUI = -1;
	private static final int EVENT_ADDED = -1;

	private enum TileEntityCapability {
		GUI_PROVIDER(IHasGui.class),
		ACTIVATE_LISTENER(IActivateAwareTile.class),
		SURFACE_ATTACHEMENT(ISurfaceAttachment.class),
		BREAK_LISTENER(IBreakAwareTile.class),
		PLACE_LISTENER(IPlaceAwareTile.class),
		ADD_LISTENER(IAddAwareTile.class),
		CUSTOM_PICK_ITEM(ICustomPickItem.class),
		CUSTOM_BREAK_DROPS(ICustomBreakDrops.class),
		CUSTOM_HARVEST_DROPS(ICustomHarvestDrops.class),
		INVENTORY(IInventory.class),
		INVENTORY_PROVIDER(IInventoryProvider.class),
		NEIGBOUR_LISTENER(INeighbourAwareTile.class),
		NEIGBOUR_TE_LISTENER(INeighbourTeAwareTile.class);

		public final Class<?> intf;

		TileEntityCapability(Class<?> intf) {
			this.intf = intf;
		}
	}

	public enum BlockPlacementMode {
		ENTITY_ANGLE,
		SURFACE
	}

	private final Set<TileEntityCapability> teCapabilities = EnumSet.noneOf(TileEntityCapability.class);

	private Class<? extends TileEntity> teClass = null;

	private BlockPlacementMode blockPlacementMode = BlockPlacementMode.ENTITY_ANGLE;

	public final IBlockRotationMode rotationMode;

	public final Property<Orientation> propertyOrientation;

	private boolean requiresInitialization;

	public boolean hasCapability(TileEntityCapability capability) {
		return teCapabilities.contains(capability);
	}

	public boolean hasCapabilities(TileEntityCapability capability1, TileEntityCapability capability2) {
		return hasCapability(capability1) || hasCapability(capability2);
	}

	public boolean hasCapabilities(TileEntityCapability... capabilities) {
		for (TileEntityCapability capability : capabilities)
			if (teCapabilities.contains(capability))
				return true;

		return false;
	}

	public OpenBlock(Block.Properties properties) {
		super(properties);
		this.rotationMode = getRotationMode();
		Preconditions.checkNotNull(this.rotationMode);
		this.propertyOrientation = this.rotationMode.getProperty();
	}

	protected void setPlacementMode(BlockPlacementMode mode) {
		this.blockPlacementMode = mode;
	}

	protected IBlockRotationMode getRotationMode() {
		return BlockRotationMode.NONE;
	}

	protected Property<Orientation> getPropertyOrientation() {
		return getRotationMode().getProperty();
	}

	protected BlockPlacementMode getPlacementMode() {
		return this.blockPlacementMode;
	}

	protected Orientation getOrientation(IBlockReader world, BlockPos pos) {
		final BlockState state = world.getBlockState(pos);
		return getOrientation(state);
	}

	public Orientation getOrientation(BlockState state) {
		// sometimes we get air block...
		if (state.getBlock() != this) return Orientation.XP_YP;
		return state.get(propertyOrientation);
	}

	public Direction getFront(BlockState state) {
		return rotationMode.getFront(getOrientation(state));
	}

	public Direction getBack(BlockState state) {
		return getFront(state).getOpposite();
	}

	public LocalDirections getLocalDirections(BlockState state) {
		return rotationMode.getLocalDirections(getOrientation(state));
	}

	public boolean shouldDropFromTeAfterBreak() {
		return true;
	}

	public boolean shouldOverrideHarvestWithTeLogic() {
		return hasCapability(TileEntityCapability.CUSTOM_HARVEST_DROPS);
	}

	public static OpenBlock getOpenBlock(IBlockReader world, BlockPos blockPos) {
		if (world == null)
			return null;
		Block block = world.getBlockState(blockPos).getBlock();
		if (block instanceof OpenBlock)
			return (OpenBlock)block;
		return null;
	}

	@Nullable @Override public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		final TileEntity te = createTileEntity();
		if (te instanceof OpenTileEntity)
			((OpenTileEntity)te).setup();
		return te;
	}

	protected TileEntity createTileEntity() {
		if (teClass == null)
			return null;
		try {
			return teClass.newInstance();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create TE with class " + teClass, ex);
		}
	}

	public Class<? extends TileEntity> getTileClass() {
		return teClass;
	}

	protected boolean suppressPickBlock() {
		return false;
	}

	@Override public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
		if (hasCapability(TileEntityCapability.CUSTOM_PICK_ITEM)) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof ICustomPickItem)
				return ((ICustomPickItem)te).getPickBlock(player);
		}

		return suppressPickBlock()? ItemStack.EMPTY : super.getPickBlock(state, target, world, pos, player);
	}

	// TODO 1.14 Block drops!

	public OpenBlock setTileEntity(final Class<? extends TileEntity> tileEntity) {
		if (tileEntity != null) {
			this.teClass = tileEntity;

			for (TileEntityCapability capability : TileEntityCapability.values())
				if (capability.intf.isAssignableFrom(teClass))
					teCapabilities.add(capability);
		}
		return this;
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return teClass != null;
	}

	// TODO 1.14 review rules
	public static boolean isNeighborBlockSolid(IWorldReader world, BlockPos blockPos, Direction side) {
		return func_220055_a(world, blockPos, side);
	}

	public static boolean areNeighborBlocksSolid(World world, BlockPos blockPos, Direction... sides) {
		for (Direction side : sides) {
			if (isNeighborBlockSolid(world, blockPos, side))
				return true;
		}
		return false;
	}

	@Override
	public void neighborChanged(BlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
		if (hasCapabilities(TileEntityCapability.NEIGBOUR_LISTENER, TileEntityCapability.SURFACE_ATTACHEMENT)) {
			final TileEntity te = world.getTileEntity(pos);
			if (te instanceof INeighbourAwareTile)
				((INeighbourAwareTile)te).onNeighbourChanged(fromPos, blockIn);

			if (te instanceof ISurfaceAttachment) {
				final Direction direction = ((ISurfaceAttachment)te).getSurfaceDirection();
				breakBlockIfSideNotSolid(world, pos, direction);
			}
		}
	}

	protected void breakBlockIfSideNotSolid(World world, BlockPos blockPos, Direction direction) {
		if (!isNeighborBlockSolid(world, blockPos, direction)) {
			world.destroyBlock(blockPos, true);
		}
	}

	@Override
	public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor) {
		if (hasCapability(TileEntityCapability.NEIGBOUR_TE_LISTENER)) {
			final TileEntity te = world.getTileEntity(pos);
			if (te instanceof INeighbourTeAwareTile)
				((INeighbourTeAwareTile)te).onNeighbourTeChanged(pos);
		}
	}

	@Override public void onBlockAdded(BlockState state, World world, BlockPos blockPos, BlockState oldState, boolean isMoving) {
		super.onBlockAdded(state, world, blockPos, oldState, isMoving);

		if (requiresInitialization || hasCapability(TileEntityCapability.ADD_LISTENER)) {
			world.addBlockEvent(blockPos, this, EVENT_ADDED, 0);
		}
	}

	@Override public boolean onBlockActivated(BlockState state, World world, BlockPos blockPos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		if (hasCapabilities(TileEntityCapability.GUI_PROVIDER, TileEntityCapability.ACTIVATE_LISTENER)) {
			final TileEntity te = world.getTileEntity(blockPos);

			if (te instanceof IHasGui && ((IHasGui)te).canOpenGui(player) && !player.isSneaking()) {
				if (!world.isRemote)
					openGui(player, world, blockPos);
				return true;
			}

			// TODO Expand for new args
			if (te instanceof IActivateAwareTile)
				return ((IActivateAwareTile)te).onBlockActivated(player, hand, hit);
		}

		return false;
	}

	@SuppressWarnings("deprecation") // TODO review
	@Override
	public boolean eventReceived(BlockState state, World world, BlockPos blockPos, int eventId, int eventParam) {
		if (eventId < 0 && !world.isRemote) {
			switch (eventId) {
				case EVENT_ADDED:
					return onBlockAddedNextTick(world, blockPos, state);
			}

			return false;
		}
		if (teClass != null) {
			super.eventReceived(state, world, blockPos, eventId, eventParam);
			TileEntity te = world.getTileEntity(blockPos);
			return te != null && te.receiveClientEvent(eventId, eventParam);
		} else {
			return super.eventReceived(state, world, blockPos, eventId, eventParam);
		}
	}

	protected boolean onBlockAddedNextTick(World world, BlockPos blockPos, BlockState state) {
		if (hasCapability(TileEntityCapability.ADD_LISTENER)) {
			final IAddAwareTile te = getTileEntity(world, blockPos, IAddAwareTile.class);
			if (te != null)
				te.onAdded();
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public static <U> U getTileEntity(IBlockReader world, BlockPos blockPos, Class<? extends U> cls) {
		final TileEntity te = world.getTileEntity(blockPos);
		return (cls.isInstance(te))? (U)te : null;
	}

	@SuppressWarnings("unchecked")
	public <U extends TileEntity> U getTileEntity(IBlockReader world, BlockPos blockPos) {
		Preconditions.checkNotNull(teClass, "This block has no tile entity");
		final TileEntity te = world.getTileEntity(blockPos);
		return (teClass.isInstance(te))? (U)te : null;
	}

	protected Orientation calculateOrientationAfterPlace(BlockPos pos, Direction facing, LivingEntity placer) {
		if (blockPlacementMode == BlockPlacementMode.SURFACE) {
			return rotationMode.getOrientationFacing(facing);
		} else {
			return rotationMode.getPlacementOrientationFromEntity(pos, placer);
		}
	}

	public boolean canBlockBePlaced(World world, BlockPos pos, Hand hand, Direction side, float hitX, float hitY, float hitZ, int itemMetadata, PlayerEntity player) {
		return true;
	}

	// TODO re-eval with whole context
	@Nullable @Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		final Orientation orientation = calculateOrientationAfterPlace(context.getPos(), context.getFace(), context.getPlayer());
		return getDefaultState().with(propertyOrientation, orientation);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos blockPos, BlockState state, LivingEntity placer, @Nonnull ItemStack stack) {
		super.onBlockPlacedBy(world, blockPos, state, placer, stack);

		if (hasCapability(TileEntityCapability.PLACE_LISTENER)) {
			final TileEntity te = world.getTileEntity(blockPos);
			if (te instanceof IPlaceAwareTile)
				((IPlaceAwareTile)te).onBlockPlacedBy(state, placer, stack);
		}
	}

	protected boolean isOnTopOfSolidBlock(World world, BlockPos blockPos, Direction side) {
		return side == Direction.UP
				&& isNeighborBlockSolid(world, blockPos, Direction.DOWN);
	}

	public void openGui(PlayerEntity player, World world, BlockPos blockPos) {
		// TODO 1.14 reimplement
	}

	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(getPropertyOrientation());
	}

	// TODO 1.14 Reimplement when hook available
	public boolean canRotateWithTool() {
		return rotationMode.toolRotationAllowed();
	}

	@Nullable @Override public Direction[] getValidRotations(BlockState state, IBlockReader world, BlockPos pos) {
		if (!canRotateWithTool())
			return RotationAxis.NO_AXIS;
		return rotationMode.getToolRotationAxes();
	}

	public OpenBlock setRequiresInitialization(boolean value) {
		this.requiresInitialization = value;
		return this;
	}
}
