package openmods.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.api.*;
import openmods.config.game.IRegisterableBlock;
import openmods.context.ContextManager;
import openmods.context.VariableKey;
import openmods.tileentity.OpenTileEntity;
import openmods.utils.BlockNotifyFlags;
import openmods.utils.BlockUtils;

import com.google.common.collect.Lists;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class OpenBlock extends Block implements IRegisterableBlock {
	public static final int OPEN_MODS_TE_GUI = -1;

	private static final VariableKey<ArrayList<ItemStack>> DROP_OVERRIDE = VariableKey.create();

	public enum BlockPlacementMode {
		ENTITY_ANGLE,
		SURFACE
	}

	public enum RenderMode {
		TESR_ONLY,
		BLOCK_ONLY,
		BOTH
	}

	private String blockName;
	private String modId;

	/**
	 * The tile entity class associated with this block
	 */
	private Class<? extends TileEntity> teClass = null;
	protected BlockRotationMode blockRotationMode = BlockRotationMode.NONE;
	protected BlockPlacementMode blockPlacementMode = BlockPlacementMode.ENTITY_ANGLE;
	protected ForgeDirection inventoryRenderRotation = ForgeDirection.WEST;
	protected RenderMode renderMode = RenderMode.BLOCK_ONLY;

	public IIcon[] textures = new IIcon[6];

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

	protected void setInventoryRenderRotation(ForgeDirection rotation) {
		inventoryRenderRotation = rotation;
	}

	protected void setRenderMode(RenderMode renderMode) {
		this.renderMode = renderMode;
	}

	public ForgeDirection getRotation(int metadata) {
		return blockRotationMode.fromValue(metadata & blockRotationMode.mask);
	}

	@SideOnly(Side.CLIENT)
	public ForgeDirection getInventoryRenderRotation() {
		return inventoryRenderRotation;
	}

	public boolean shouldDropFromTeAfterBreak() {
		return true;
	}

	public boolean shouldOverrideHarvestWithTeLogic() {
		return teClass != null && ICustomHarvestDrops.class.isAssignableFrom(teClass);
	}

	public void setBoundsBasedOnRotation(ForgeDirection direction) {}

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

	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister registry) {
		this.blockIcon = registry.registerIcon(String.format("%s:%s", modId, blockName));
	}

	protected boolean suppressPickBlock() {
		return false;
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
		if (teClass != null && ICustomPickItem.class.isAssignableFrom(teClass)) {
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
		TileEntity te = world.getTileEntity(x, y, z);

		if (te instanceof ICustomHarvestDrops) {
			ICustomHarvestDrops dropper = (ICustomHarvestDrops)te;

			ArrayList<ItemStack> drops;
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
		return null;
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		if (willHarvest && shouldOverrideHarvestWithTeLogic()) {
			ArrayList<ItemStack> drops = getDropsWithTileEntity(world, player, x, y, z);
			if (drops != null) ContextManager.set(DROP_OVERRIDE, drops);
		}

		return super.removedByPlayer(world, player, x, y, z, willHarvest);
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		ArrayList<ItemStack> result = ContextManager.remove(DROP_OVERRIDE);

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
		this.blockName = blockName;
		this.modId = modId;

		if (tileEntity != null) {
			this.teClass = tileEntity;
			isBlockContainer = true;
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
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof INeighbourAwareTile) ((INeighbourAwareTile)te).onNeighbourChanged(neighbour);

		if (te instanceof ISurfaceAttachment) {
			ForgeDirection direction = ((ISurfaceAttachment)te).getSurfaceDirection();
			if (!isNeighborBlockSolid(world, x, y, z, direction)) {
				world.func_147480_a(x, y, z, true);
			}
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		TileEntity te = world.getTileEntity(x, y, z);

		if (te instanceof IHasGui && ((IHasGui)te).canOpenGui(player) && !player.isSneaking()) {
			if (!world.isRemote) openGui(player, world, x, y, z);
			return true;
		}

		if (te instanceof IActivateAwareTile) return ((IActivateAwareTile)te).onBlockActivated(player, side, hitX, hitY, hitZ);
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return isOpaqueCube();
	}

	@Override
	public boolean onBlockEventReceived(World world, int x, int y, int z, int eventId, int eventParam) {
		super.onBlockEventReceived(world, x, y, z, eventId, eventParam);
		TileEntity te = getTileEntity(world, x, y, z, TileEntity.class);
		if (te != null) { return te.receiveClientEvent(eventId, eventParam); }
		return false;
	}

	protected void setupDimensionsFromCenter(float x, float y, float z, float width, float height, float depth) {
		setupDimensions(x - width, y, z - depth, x + width, y + height, z
				+ depth);
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
	public static <U> U getTileEntity(IBlockAccess world, int x, int y, int z, Class<U> T) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te != null && T.isAssignableFrom(te.getClass())) { return (U)te; }
		return null;
	}

	public boolean canPlaceBlock(World world, EntityPlayer player, ItemStack stack, int x, int y, int z, ForgeDirection sideDir, ForgeDirection blockDirection, float hitX, float hitY, float hitZ, int newMeta) {
		return blockRotationMode.isValid(blockDirection);
	}

	/***
	 * An extended block placement function which includes ALL the details
	 * you'll ever need.
	 * This is called if your ItemBlock extends ItemOpenBlock
	 */
	public void afterBlockPlaced(World world, EntityPlayer player, ItemStack stack, int x, int y, int z, ForgeDirection side, ForgeDirection blockDir, float hitX, float hitY, float hitZ, int itemMeta) {
		int blockMeta = blockRotationMode.toValue(blockDir);

		// silently set meta, since we want to notify TE before neighbors
		world.setBlockMetadataWithNotify(x, y, z, blockMeta, BlockNotifyFlags.NONE);

		notifyTileEntity(world, player, stack, x, y, z, side, hitX, hitY, hitZ);

		world.markBlockForUpdate(x, y, z);
		if (!world.isRemote) world.notifyBlockChange(x, y, z, this);
	}

	protected void notifyTileEntity(World world, EntityPlayer player, ItemStack stack, int x, int y, int z, ForgeDirection side, float hitX, float hitY, float hitZ) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof IPlaceAwareTile) ((IPlaceAwareTile)te).onBlockPlacedBy(player, side, stack, hitX, hitY, hitZ);
	}

	protected void setRotationMeta(World world, int x, int y, int z, ForgeDirection blockDir) {
		int blockMeta = blockRotationMode.toValue(blockDir);
		world.setBlockMetadataWithNotify(x, y, z, blockMeta, BlockNotifyFlags.ALL);
	}

	public ForgeDirection calculateSide(EntityPlayer player, ForgeDirection side) {
		if (blockPlacementMode == BlockPlacementMode.SURFACE) {
			return side.getOpposite();
		} else {
			switch (getRotationMode()) {
				case TWO_DIRECTIONS: {
					ForgeDirection normalDir = BlockUtils.get2dOrientation(player);
					switch (normalDir) {
						case EAST:
						case WEST:
							return ForgeDirection.WEST;
						case NORTH:
						case SOUTH:
						default:
							return ForgeDirection.NORTH;
					}
				}
				case FOUR_DIRECTIONS:
					return BlockUtils.get2dOrientation(player);
				case SIX_DIRECTIONS:
					return BlockUtils.get3dOrientation(player);
				default:
					return ForgeDirection.UNKNOWN;
			}
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
		ForgeDirection direction = rotateSideByMetadata(side, world.getBlockMetadata(x, y, z));
		IIconProvider provider = getTileEntity(world, x, y, z, IIconProvider.class);
		IIcon teIcon = null;
		if (provider != null) teIcon = provider.getIcon(direction);
		return teIcon != null? teIcon : getUnrotatedTexture(direction, world, x, y, z);
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

	/***
	 * I'm sure there's a better way of doing this, but the idea is that we
	 * rotate the block based on the metadata (rotation), so when we try to get
	 * a texture we're referencing the side when 'unrotated'
	 */
	public ForgeDirection rotateSideByMetadata(int side, int metadata) {
		final BlockRotationMode rotationMode = getRotationMode();
		ForgeDirection rotation = rotationMode.fromValue(metadata);
		ForgeDirection dir = ForgeDirection.getOrientation(side);
		switch (rotationMode) {
			case FOUR_DIRECTIONS:
			case NONE:
				switch (rotation) {
					case EAST:
						dir = dir.getRotation(ForgeDirection.DOWN);
						break;
					case SOUTH:
						dir = dir.getRotation(ForgeDirection.UP);
						dir = dir.getRotation(ForgeDirection.UP);
						break;
					case WEST:
						dir = dir.getRotation(ForgeDirection.UP);
						break;
					default:
						break;
				}
				return dir;
			default:
				switch (rotation) {
					case DOWN:
						dir = dir.getRotation(ForgeDirection.SOUTH);
						dir = dir.getRotation(ForgeDirection.SOUTH);
						break;
					case EAST:
						dir = dir.getRotation(ForgeDirection.NORTH);
						break;
					case NORTH:
						dir = dir.getRotation(ForgeDirection.WEST);
						break;
					case SOUTH:
						dir = dir.getRotation(ForgeDirection.EAST);
						break;
					case WEST:
						dir = dir.getRotation(ForgeDirection.SOUTH);
						break;
					default:
						break;

				}
		}
		return dir;
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
		return true;
	}

	@Override
	public boolean rotateBlock(World worldObj, int x, int y, int z, ForgeDirection axis) {
		return canRotateWithTool() && RotationHelper.rotate(blockRotationMode, worldObj, x, y, z, axis);
	}

	@Override
	public ForgeDirection[] getValidRotations(World worldObj, int x, int y, int z) {
		if (!canRotateWithTool()) return RotationAxis.NO_AXIS;
		return blockRotationMode.rotations;
	}
}
