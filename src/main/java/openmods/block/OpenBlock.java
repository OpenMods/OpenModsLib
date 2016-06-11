package openmods.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.api.IActivateAwareTile;
import openmods.api.IAddAwareTile;
import openmods.api.IBreakAwareTile;
import openmods.api.ICustomBreakDrops;
import openmods.api.ICustomHarvestDrops;
import openmods.api.ICustomPickItem;
import openmods.api.IHasGui;
import openmods.api.IIconProvider;
import openmods.api.INeighbourAwareTile;
import openmods.api.IPlaceAwareTile;
import openmods.api.IPlacerAwareTile;
import openmods.api.ISurfaceAttachment;
import openmods.config.game.IRegisterableBlock;
import openmods.geometry.BlockSpaceTransform;
import openmods.geometry.Orientation;
import openmods.inventory.IInventoryProvider;
import openmods.tileentity.OpenTileEntity;
import openmods.utils.BlockNotifyFlags;
import openmods.utils.BlockUtils;

public abstract class OpenBlock extends Block implements IRegisterableBlock {
	public static final int OPEN_MODS_TE_GUI = -1;
	private static final int EVENT_ADDED = -1;

	private enum TileEntityCapability {
		ICON_PROVIDER(IIconProvider.class),
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

	public IIcon[] textures = new IIcon[6];

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

	public static OpenBlock getOpenBlock(IBlockAccess world, int x, int y, int z) {
		if (world == null) return null;
		Block block = world.getBlock(x, y, z);
		if (block instanceof OpenBlock) return (OpenBlock)block;
		return null;
	}

	@Override
	public TileEntity createTileEntity(World world, int metadata) {
		final TileEntity te = createTileEntity();

		if (te != null) {
			te.blockType = this;
			if (te instanceof OpenTileEntity) {
				((OpenTileEntity)te).setup();
			}
		}

		return te;
	}

	public TileEntity createTileEntityForRender() {
		final TileEntity te = createTileEntity();
		Preconditions.checkNotNull(te, "Trying to get rendering TE for '%s', but it's not configured", this);
		te.blockType = this;
		te.blockMetadata = 0;
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
	@SuppressWarnings("deprecation")
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
		if (hasCapability(TileEntityCapability.CUSTOM_PICK_ITEM)) {
			TileEntity te = world.getTileEntity(x, y, z);
			if (te instanceof ICustomPickItem) return ((ICustomPickItem)te).getPickBlock();
		}

		return suppressPickBlock()? null : super.getPickBlock(target, world, x, y, z);
	}

	private static List<ItemStack> getTileBreakDrops(TileEntity te) {
		List<ItemStack> breakDrops = Lists.newArrayList();
		BlockUtils.getTileInventoryDrops(te, breakDrops);
		if (te instanceof ICustomBreakDrops) ((ICustomBreakDrops)te).addDrops(breakDrops);
		return breakDrops;
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		if (shouldDropFromTeAfterBreak()) {
			final TileEntity te = world.getTileEntity(x, y, z);
			if (te != null) {
				if (te instanceof IBreakAwareTile) ((IBreakAwareTile)te).onBlockBroken();

				for (ItemStack stack : getTileBreakDrops(te))
					BlockUtils.dropItemStackInWorld(world, x, y, z, stack);

				world.removeTileEntity(x, y, z);
			}
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	protected ArrayList<ItemStack> getDropsWithTileEntity(World world, EntityPlayer player, int x, int y, int z) {
		if (hasCapability(TileEntityCapability.CUSTOM_HARVEST_DROPS)) {
			final TileEntity te = world.getTileEntity(x, y, z);

			if (te instanceof ICustomHarvestDrops) {
				final ICustomHarvestDrops dropper = (ICustomHarvestDrops)te;

				final ArrayList<ItemStack> drops;
				if (!dropper.suppressNormalHarvestDrops()) {
					final int metadata = world.getBlockMetadata(x, y, z);
					int fortune = player != null? EnchantmentHelper.getFortuneModifier(player) : 0;
					drops = super.getDrops(world, x, y, z, metadata, fortune);
				} else {
					drops = Lists.newArrayList();
				}

				dropper.addHarvestDrops(player, drops);
				return drops;
			}
		}

		return null;
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		if (willHarvest && shouldOverrideHarvestWithTeLogic()) {
			List<ItemStack> drops = getDropsWithTileEntity(world, player, x, y, z);
			if (drops != null) BlockDropsStore.instance.storeDrops(world, x, y, z, drops);
		}

		return super.removedByPlayer(world, player, x, y, z, willHarvest);
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		ArrayList<ItemStack> result = BlockDropsStore.instance.harvestDrops(world, x, y, z);

		// Case A - drops stored earlier (by this.removedByPlayer) and TE is already dead
		if (result != null) return result;

		// Case B - drops removed in other way (explosion) but TE may be still alive
		result = getDropsWithTileEntity(world, null, x, y, z);
		if (result != null) return result;

		// Case C - TE is dead, just drop vanilla stuff
		return super.getDrops(world, x, y, z, metadata, fortune);
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
	public boolean hasTileEntity(int metadata) {
		return teClass != null;
	}

	public final static boolean isNeighborBlockSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
		x += side.offsetX;
		y += side.offsetY;
		z += side.offsetZ;
		return world.isSideSolid(x, y, z, side.getOpposite(), false);
	}

	public final static boolean areNeighborBlocksSolid(World world, int x, int y, int z, ForgeDirection... sides) {
		for (ForgeDirection side : sides) {
			if (isNeighborBlockSolid(world, x, y, z, side)) { return true; }
		}
		return false;
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
		if (hasCapabilities(TileEntityCapability.NEIGBOUR_LISTENER, TileEntityCapability.SURFACE_ATTACHEMENT)) {
			final TileEntity te = world.getTileEntity(x, y, z);
			if (te instanceof INeighbourAwareTile) ((INeighbourAwareTile)te).onNeighbourChanged(neighbour);

			if (te instanceof ISurfaceAttachment) {
				final ForgeDirection direction = ((ISurfaceAttachment)te).getSurfaceDirection();
				breakBlockIfSideNotSolid(world, x, y, z, direction);
			}
		}
	}

	protected void breakBlockIfSideNotSolid(World world, int x, int y, int z, ForgeDirection direction) {
		if (!isNeighborBlockSolid(world, x, y, z, direction)) {
			world.func_147480_a(x, y, z, true);
		}
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);

		if (hasCapability(TileEntityCapability.ADD_LISTENER)) {
			world.addBlockEvent(x, y, z, this, EVENT_ADDED, 0);
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (hasCapabilities(TileEntityCapability.GUI_PROVIDER, TileEntityCapability.ACTIVATE_LISTENER)) {
			final TileEntity te = world.getTileEntity(x, y, z);

			if (te instanceof IHasGui && ((IHasGui)te).canOpenGui(player) && !player.isSneaking()) {
				if (!world.isRemote) openGui(player, world, x, y, z);
				return true;
			}

			if (te instanceof IActivateAwareTile) return ((IActivateAwareTile)te).onBlockActivated(player, side, hitX, hitY, hitZ);
		}

		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return isOpaqueCube();
	}

	@Override
	public boolean onBlockEventReceived(World world, int x, int y, int z, int eventId, int eventParam) {
		if (eventId < 0 && !world.isRemote) {
			switch (eventId) {
				case EVENT_ADDED: {
					if (hasCapability(TileEntityCapability.ADD_LISTENER)) {
						final IAddAwareTile te = getTileEntity(world, x, y, z, IAddAwareTile.class);
						if (te != null) te.onAdded();
					}
				}
					break;
			}

			return false;
		}
		if (isBlockContainer) {
			super.onBlockEventReceived(world, x, y, z, eventId, eventParam);
			TileEntity te = world.getTileEntity(x, y, z);
			return te != null? te.receiveClientEvent(eventId, eventParam) : false;
		} else {
			return super.onBlockEventReceived(world, x, y, z, eventId, eventParam);
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
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getSelectedBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	@SuppressWarnings("unchecked")
	public static <U> U getTileEntity(IBlockAccess world, int x, int y, int z, Class<? extends U> cls) {
		final TileEntity te = world.getTileEntity(x, y, z);
		return (cls.isInstance(te))? (U)te : null;
	}

	@SuppressWarnings("unchecked")
	public <U extends TileEntity> U getTileEntity(IBlockAccess world, int x, int y, int z) {
		Preconditions.checkNotNull(teClass, "This block has no tile entity");
		final TileEntity te = world.getTileEntity(x, y, z);
		return (teClass.isInstance(te))? (U)te : null;
	}

	public boolean canPlaceBlock(World world, EntityPlayer player, ItemStack stack, int x, int y, int z, ForgeDirection sideDir, Orientation blockOrientation, float hitX, float hitY, float hitZ, int newMeta) {
		return getRotationMode().isPlacementValid(blockOrientation);
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, x, y, z, placer, stack);

		if (hasCapability(TileEntityCapability.PLACER_LISTENER)) {
			final TileEntity te = world.getTileEntity(x, y, z);
			if (te instanceof IPlacerAwareTile) ((IPlacerAwareTile)te).onBlockPlacedBy(placer, stack);
		}
	}

	/***
	 * An extended block placement function which includes ALL the details
	 * you'll ever need.
	 * This is called if your ItemBlock extends ItemOpenBlock
	 */
	public void afterBlockPlaced(World world, EntityPlayer player, ItemStack stack, int x, int y, int z, ForgeDirection side, Orientation blockOrientation, float hitX, float hitY, float hitZ, int itemMeta) {
		int blockMeta = getRotationMode().toValue(blockOrientation);

		// silently set meta, since we want to notify TE before neighbors
		world.setBlockMetadataWithNotify(x, y, z, blockMeta, BlockNotifyFlags.NONE);

		notifyTileEntity(world, player, stack, x, y, z, side, blockOrientation, hitX, hitY, hitZ);

		world.markBlockForUpdate(x, y, z);
		if (!world.isRemote) world.notifyBlockChange(x, y, z, this);
	}

	protected void notifyTileEntity(World world, EntityPlayer player, ItemStack stack, int x, int y, int z, ForgeDirection side, Orientation blockOrientation, float hitX, float hitY, float hitZ) {
		if (hasCapability(TileEntityCapability.PLACE_LISTENER)) {
			final TileEntity te = world.getTileEntity(x, y, z);
			if (te instanceof IPlaceAwareTile) ((IPlaceAwareTile)te).onBlockPlacedBy(player, side, stack, hitX, hitY, hitZ);
		}
	}

	protected void setRotationMeta(World world, int x, int y, int z, Orientation blockOrientation) {
		int blockMeta = getRotationMode().toValue(blockOrientation);
		world.setBlockMetadataWithNotify(x, y, z, blockMeta, BlockNotifyFlags.ALL);
	}

	public Orientation calculatePlacementSide(EntityPlayer player, ForgeDirection side) {
		if (blockPlacementMode == BlockPlacementMode.SURFACE) {
			return getRotationMode().getPlacementOrientationFromSurface(side);
		} else {
			return getRotationMode().getPlacementOrientationFromEntity(player);
		}
	}

	@Override
	public final boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
		return canPlaceBlockOnSide(world, x, y, z, ForgeDirection.getOrientation(side).getOpposite());
	}

	public boolean canPlaceBlockOnSide(World world, int x, int y, int z, ForgeDirection side) {
		return canPlaceBlockAt(world, x, y, z); // default to vanilla rules
	}

	protected boolean isOnTopOfSolidBlock(World world, int x, int y, int z, ForgeDirection side) {
		return side == ForgeDirection.DOWN
				&& isNeighborBlockSolid(world, x, y, z, ForgeDirection.DOWN);
	}

	public void setTexture(ForgeDirection direction, IIcon icon) {
		textures[direction.ordinal()] = icon;
	}

	public void setTextures(IIcon icon, ForgeDirection... directions) {
		for (ForgeDirection direction : directions)
			textures[direction.ordinal()] = icon;
	}

	protected IIcon getUnrotatedTexture(ForgeDirection direction) {
		if (direction != ForgeDirection.UNKNOWN) {
			final int directionId = direction.ordinal();
			if (textures[directionId] != null) return textures[directionId];
		}
		return blockIcon;
	}

	/**
	 * This method should be overriden if needed. We're getting the texture for
	 * the UNROTATED block for a particular side (direction). Feel free to look
	 * up data in the TileEntity to grab additional information here
	 */
	public IIcon getUnrotatedTexture(ForgeDirection direction, IBlockAccess world, int x, int y, int z) {
		return getUnrotatedTexture(direction);
	}

	/**
	 * Get the texture, but rotate the block around the metadata rotation first
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
		final ForgeDirection direction = rotateSideByMetadata(side, world.getBlockMetadata(x, y, z));
		IIcon iconOverride = null;
		if (hasCapability(TileEntityCapability.ICON_PROVIDER)) {
			IIconProvider provider = getTileEntity(world, x, y, z, IIconProvider.class);
			if (provider != null) iconOverride = provider.getIcon(direction);
		}
		return iconOverride != null? iconOverride : getUnrotatedTexture(direction, world, x, y, z);
	}

	/***
	 * This is called by the blockrenderer when rendering an item into the
	 * inventory.
	 * We'll return the block, rotated as we wish, but without any additional
	 * texture
	 * changes that are caused by the blocks current state
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public final IIcon getIcon(int side, int metadata) {
		ForgeDirection newRotation = rotateSideByMetadata(side, metadata);
		return getUnrotatedTexture(newRotation);
	}

	public ForgeDirection rotateSideByMetadata(int side, int metadata) {
		final ForgeDirection dir = ForgeDirection.getOrientation(side);
		return rotateSideByMetadata(dir, metadata);
	}

	public ForgeDirection rotateSideByMetadata(ForgeDirection side, int metadata) {
		final Orientation rotation = getOrientation(metadata);
		return rotation.globalToLocalDirection(side);
	}

	public Vec3 rotateVectorByMetadata(Vec3 vec, int metadata) {
		return rotateVectorByMetadata(vec.xCoord, vec.yCoord, vec.zCoord, metadata);
	}

	public Vec3 rotateVectorByMetadata(double x, double y, double z, int metadata) {
		final Orientation rotation = getOrientation(metadata);
		return rotateVectorByDirection(rotation, x, y, z);
	}

	public Vec3 rotateVectorByDirection(Orientation orientation, double x, double y, double z) {
		return BlockSpaceTransform.instance.mapWorldToBlock(orientation, x, y, z);
	}

	public void setDefaultTexture(IIcon icon) {
		this.blockIcon = icon;
	}

	protected abstract Object getModInstance();

	public void openGui(EntityPlayer player, World world, int x, int y, int z) {
		player.openGui(getModInstance(), OPEN_MODS_TE_GUI, world, x, y, z);
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

	public RotationHelper createRotationHelper(World world, int x, int y, int z) {
		return new RotationHelper(getRotationMode(), world, x, y, z);
	}

	@Override
	public boolean rotateBlock(World worldObj, int x, int y, int z, ForgeDirection axis) {
		if (!canRotateWithTool()) return false;
		if (!createRotationHelper(worldObj, x, y, z).rotateWithTool(axis)) return false;

		if (teCapabilities.contains(TileEntityCapability.SURFACE_ATTACHEMENT)) {
			final ISurfaceAttachment te = getTileEntity(worldObj, x, y, z, ISurfaceAttachment.class);
			if (te == null) return false;

			breakBlockIfSideNotSolid(worldObj, x, y, z, te.getSurfaceDirection());
		}

		return true;
	}

	@Override
	public ForgeDirection[] getValidRotations(World worldObj, int x, int y, int z) {
		if (!canRotateWithTool()) return RotationAxis.NO_AXIS;
		return getRotationMode().rotationAxes;
	}
}
