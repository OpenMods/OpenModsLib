package openmods.block;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import openmods.api.*;
import openmods.config.game.IRegisterableBlock;
import openmods.geometry.Orientation;
import openmods.inventory.IInventoryProvider;
import openmods.tileentity.OpenTileEntity;
import openmods.utils.BlockNotifyFlags;

import com.google.common.base.Preconditions;

public abstract class OpenBlock extends Block implements IRegisterableBlock {
	public static final int OPEN_MODS_TE_GUI = -1;
	private static final int EVENT_ADDED = -1;

	private enum TileEntityCapability {
		GUI_PROVIDER(IHasGui.class),
		ACTIVATE_LISTENER(IActivateAwareTile.class),
		SURFACE_ATTACHEMENT(ISurfaceAttachment.class),
		BREAK_LISTENER(IBreakAwareTile.class),
		PLACER_LISTENER(IPlacerAwareTile.class),
		PLACE_LISTENER(IPlaceAwareTile.class),
		ADD_LISTENER(IAddAwareTile.class),
		CUSTOM_PICK_ITEM(ICustomPickItem.class),
		CUSTOM_BREAK_DROPS(ICustomBreakDrops.class),
		CUSTOM_HARVEST_DROPS(ICustomHarvestDrops.class),
		INVENTORY(IInventory.class),
		INVENTORY_PROVIDER(IInventoryProvider.class),
		NEIGBOUR_LISTENER(INeighbourAwareTile.class);

		public final Class<?> intf;

		private TileEntityCapability(Class<?> intf) {
			this.intf = intf;
		}
	}

	public enum BlockPlacementMode {
		ENTITY_ANGLE,
		SURFACE
	}

	public enum RenderMode {
		TESR_ONLY,
		BLOCK_ONLY,
		BOTH
	}

	private final Set<TileEntityCapability> teCapabilities = EnumSet.noneOf(TileEntityCapability.class);

	/**
	 * The tile entity class associated with this block
	 */
	private Class<? extends TileEntity> teClass = null;
	protected BlockRotationMode blockRotationMode = BlockRotationMode.NONE;
	protected BlockPlacementMode blockPlacementMode = BlockPlacementMode.ENTITY_ANGLE;
	protected Orientation inventoryRenderOrientation;
	protected RenderMode renderMode = RenderMode.BLOCK_ONLY;

	public boolean hasCapability(TileEntityCapability capability) {
		return teCapabilities.contains(capability);
	}

	public boolean hasCapabilities(TileEntityCapability capability1, TileEntityCapability capability2) {
		return hasCapability(capability1) || hasCapability(capability2);
	}

	public boolean hasCapabilities(TileEntityCapability... capabilities) {
		for (TileEntityCapability capability : capabilities)
			if (teCapabilities.contains(capability)) return true;

		return false;
	}

	protected OpenBlock(Material material) {
		super(material);
		setHardness(1.0F);

		// I dont think vanilla actually uses this..
		isBlockContainer = false;
	}

	protected void setPlacementMode(BlockPlacementMode mode) {
		this.blockPlacementMode = mode;
	}

	protected void setRotationMode(BlockRotationMode mode) {
		this.blockRotationMode = mode;
	}

	public BlockRotationMode getRotationMode() {
		return this.blockRotationMode;
	}

	protected BlockPlacementMode getPlacementMode() {
		return this.blockPlacementMode;
	}

	protected void setInventoryRenderOrientation(Orientation orientation) {
		inventoryRenderOrientation = orientation;
	}

	protected void setRenderMode(RenderMode renderMode) {
		this.renderMode = renderMode;
	}

	public Orientation getOrientation(int metadata) {
		final BlockRotationMode rotationMode = getRotationMode();
		return rotationMode.fromValue(metadata & rotationMode.mask);
	}

	@SideOnly(Side.CLIENT)
	public Orientation getInventoryRenderOrientation() {
		return inventoryRenderOrientation != null? inventoryRenderOrientation : getRotationMode().getInventoryRenderOrientation();
	}

	@SideOnly(Side.CLIENT)
	public int getInventoryRenderMetadata(int itemMetadata) {
		final BlockRotationMode rotationMode = getRotationMode();
		final Orientation renderOrientation = inventoryRenderOrientation != null? inventoryRenderOrientation : rotationMode.getInventoryRenderOrientation();
		return rotationMode.toValue(renderOrientation);
	}

	public void setBlockBounds(AxisAlignedBB aabb) {
		this.maxX = aabb.maxX;
		this.maxY = aabb.maxY;
		this.maxZ = aabb.maxZ;

		this.minX = aabb.minX;
		this.minY = aabb.minY;
		this.minZ = aabb.minZ;
	}

	public boolean shouldDropFromTeAfterBreak() {
		return true;
	}

	public boolean shouldOverrideHarvestWithTeLogic() {
		return hasCapability(TileEntityCapability.CUSTOM_HARVEST_DROPS);
	}

	public void setBoundsBasedOnOrientation(Orientation orientation) {}

	public static OpenBlock getOpenBlock(IBlockAccess world, BlockPos blockPos) {
		if (world == null) return null;
		Block block = world.getBlockState(blockPos).getBlock();
		if (block instanceof OpenBlock) return (OpenBlock)block;
		return null;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		final TileEntity te = createTileEntity();
		if (te instanceof OpenTileEntity) ((OpenTileEntity)te).setup();
		return te;
	}

	protected TileEntity createTileEntity() {
		if (teClass == null) return null;
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

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos blockPos, EntityPlayer player) {
		if (hasCapability(TileEntityCapability.CUSTOM_PICK_ITEM)) {
			TileEntity te = world.getTileEntity(blockPos);
			if (te instanceof ICustomPickItem) return ((ICustomPickItem)te).getPickBlock();
		}

		return suppressPickBlock()? null : super.getPickBlock(target, world, blockPos, player);
	}

	@Override
	public void setupBlock(String modId, String blockName, Class<? extends TileEntity> tileEntity, Class<? extends ItemBlock> itemClass) {
		if (tileEntity != null) {
			this.teClass = tileEntity;
			isBlockContainer = true;

			for (TileEntityCapability capability : TileEntityCapability.values())
				if (capability.intf.isAssignableFrom(teClass)) teCapabilities.add(capability);
		}
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return teClass != null;
	}

	public final static boolean isNeighborBlockSolid(IBlockAccess world, BlockPos blockPos, EnumFacing side) {
		return world.isSideSolid(blockPos.offset(side), side.getOpposite(), false);
	}

	public final static boolean areNeighborBlocksSolid(World world, BlockPos blockPos, EnumFacing... sides) {
		for (EnumFacing side : sides) {
			if (isNeighborBlockSolid(world, blockPos, side)) return true;
		}
		return false;
	}

	@Override
	public void onNeighborBlockChange(World world, BlockPos blockPos, IBlockState state, Block neighbour) {
		if (hasCapabilities(TileEntityCapability.NEIGBOUR_LISTENER, TileEntityCapability.SURFACE_ATTACHEMENT)) {
			final TileEntity te = world.getTileEntity(blockPos);
			if (te instanceof INeighbourAwareTile) ((INeighbourAwareTile)te).onNeighbourChanged(neighbour);

			if (te instanceof ISurfaceAttachment) {
				final EnumFacing direction = ((ISurfaceAttachment)te).getSurfaceDirection();
				breakBlockIfSideNotSolid(world, blockPos, direction);
			}
		}
	}

	protected void breakBlockIfSideNotSolid(World world, BlockPos blockPos, EnumFacing direction) {
		if (!isNeighborBlockSolid(world, blockPos, direction)) {
			world.destroyBlock(blockPos, true);
		}
	}

	@Override
	public void onBlockAdded(World world, BlockPos blockPos, IBlockState state) {
		super.onBlockAdded(world, blockPos, state);

		if (hasCapability(TileEntityCapability.ADD_LISTENER)) {
			world.addBlockEvent(blockPos, this, EVENT_ADDED, 0);
		}
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos blockPos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (hasCapabilities(TileEntityCapability.GUI_PROVIDER, TileEntityCapability.ACTIVATE_LISTENER)) {
			final TileEntity te = world.getTileEntity(blockPos);

			if (te instanceof IHasGui && ((IHasGui)te).canOpenGui(player) && !player.isSneaking()) {
				if (!world.isRemote) openGui(player, world, blockPos);
				return true;
			}

			if (te instanceof IActivateAwareTile) return ((IActivateAwareTile)te).onBlockActivated(player, side, hitX, hitY, hitZ);
		}

		return false;
	}

	@Override
	public boolean onBlockEventReceived(World world, BlockPos blockPos, IBlockState state, int eventId, int eventParam) {
		if (eventId < 0 && !world.isRemote) {
			switch (eventId) {
				case EVENT_ADDED: {
					if (hasCapability(TileEntityCapability.ADD_LISTENER)) {
						final IAddAwareTile te = getTileEntity(world, blockPos, IAddAwareTile.class);
						if (te != null) te.onAdded();
					}
				}
					break;
			}

			return false;
		}
		if (isBlockContainer) {
			super.onBlockEventReceived(world, blockPos, state, eventId, eventParam);
			TileEntity te = world.getTileEntity(blockPos);
			return te != null? te.receiveClientEvent(eventId, eventParam) : false;
		} else {
			return super.onBlockEventReceived(world, blockPos, state, eventId, eventParam);
		}
	}

	protected void setupDimensionsFromCenter(float x, float y, float z, float width, float height, float depth) {
		setupDimensions(x - width, y, z - depth, x + width, y + height, z + depth);
	}

	protected void setupDimensions(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBox(World world, BlockPos blockPos) {
		setBlockBoundsBasedOnState(world, blockPos);
		return super.getSelectedBoundingBox(world, blockPos);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos blockPos, IBlockState state) {
		setBlockBoundsBasedOnState(world, blockPos);
		return super.getCollisionBoundingBox(world, blockPos, state);
	}

	@SuppressWarnings("unchecked")
	public static <U> U getTileEntity(IBlockAccess world, BlockPos blockPos, Class<? extends U> cls) {
		final TileEntity te = world.getTileEntity(blockPos);
		return (cls.isInstance(te))? (U)te : null;
	}

	@SuppressWarnings("unchecked")
	public <U extends TileEntity> U getTileEntity(IBlockAccess world, BlockPos blockPos) {
		Preconditions.checkNotNull(teClass, "This block has no tile entity");
		final TileEntity te = world.getTileEntity(blockPos);
		return (teClass.isInstance(te))? (U)te : null;
	}

	public boolean canPlaceBlock(World world, EntityPlayer player, ItemStack stack, BlockPos blockPos, EnumFacing sideDir, Orientation blockOrientation, float hitX, float hitY, float hitZ, int newMeta) {
		return getRotationMode().isPlacementValid(blockOrientation);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos blockPos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, blockPos, state, placer, stack);

		if (hasCapability(TileEntityCapability.PLACER_LISTENER)) {
			final TileEntity te = world.getTileEntity(blockPos);
			if (te instanceof IPlacerAwareTile) ((IPlacerAwareTile)te).onBlockPlacedBy(placer, stack);
		}
	}

	/***
	 * An extended block placement function which includes ALL the details
	 * you'll ever need.
	 * This is called if your ItemBlock extends ItemOpenBlock
	 */
	// TODO actually call this
	public void afterBlockPlaced(World world, EntityPlayer player, ItemStack stack, BlockPos blockPos, EnumFacing side, Orientation blockOrientation, float hitX, float hitY, float hitZ, int itemMeta) {
		int blockMeta = getRotationMode().toValue(blockOrientation);

		// silently set meta, since we want to notify TE before neighbors
		world.setBlockMetadataWithNotify(blockPos, blockMeta, BlockNotifyFlags.NONE);

		notifyTileEntity(world, player, stack, blockPos, side, blockOrientation, hitX, hitY, hitZ);

		world.markBlockForUpdate(blockPos);
		if (!world.isRemote) world.notifyBlockChange(blockPos, this);
	}

	protected void notifyTileEntity(World world, EntityPlayer player, ItemStack stack, BlockPos blockPos, EnumFacing side, Orientation blockOrientation, float hitX, float hitY, float hitZ) {
		if (hasCapability(TileEntityCapability.PLACE_LISTENER)) {
			final TileEntity te = world.getTileEntity(blockPos);
			if (te instanceof IPlaceAwareTile) ((IPlaceAwareTile)te).onBlockPlacedBy(player, side, stack, hitX, hitY, hitZ);
		}
	}

	protected void setRotationMeta(World world, BlockPos blockPos, Orientation blockOrientation) {
		int blockMeta = getRotationMode().toValue(blockOrientation);
		world.setBlockMetadataWithNotify(blockPos, blockMeta, BlockNotifyFlags.ALL);
	}

	public Orientation calculatePlacementSide(EntityPlayer player, EnumFacing side) {
		if (blockPlacementMode == BlockPlacementMode.SURFACE) {
			return getRotationMode().getPlacementOrientationFromSurface(side);
		} else {
			return getRotationMode().getPlacementOrientationFromEntity(player);
		}
	}

	protected boolean isOnTopOfSolidBlock(World world, BlockPos blockPos, EnumFacing side) {
		return side == EnumFacing.DOWN
				&& isNeighborBlockSolid(world, blockPos, EnumFacing.DOWN);
	}

	protected abstract Object getModInstance();

	public void openGui(EntityPlayer player, World world, BlockPos blockPos) {
		player.openGui(getModInstance(), OPEN_MODS_TE_GUI, world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}

	public final boolean shouldRenderBlock() {
		return renderMode != RenderMode.TESR_ONLY;
	}

	public final boolean shouldRenderTesrInInventory() {
		return renderMode != RenderMode.BLOCK_ONLY;
	}

	public boolean canRotateWithTool() {
		return getRotationMode() != BlockRotationMode.NONE;
	}

	public RotationHelper createRotationHelper(World world, BlockPos blockPos) {
		return new RotationHelper(getRotationMode(), world, blockPos);
	}

	@Override
	public boolean rotateBlock(World worldObj, BlockPos blockPos, EnumFacing axis) {
		if (!canRotateWithTool()) return false;
		if (!createRotationHelper(worldObj, blockPos).rotateWithTool(axis)) return false;

		if (teCapabilities.contains(TileEntityCapability.SURFACE_ATTACHEMENT)) {
			final ISurfaceAttachment te = getTileEntity(worldObj, blockPos, ISurfaceAttachment.class);
			if (te == null) return false;

			breakBlockIfSideNotSolid(worldObj, blockPos, te.getSurfaceDirection());
		}

		return true;
	}

	@Override
	public EnumFacing[] getValidRotations(World worldObj, BlockPos pos) {
		if (!canRotateWithTool()) return RotationAxis.NO_AXIS;
		return getRotationMode().rotationAxes;
	}
}
