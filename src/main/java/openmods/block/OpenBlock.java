package openmods.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.Log;
import openmods.api.*;
import openmods.config.game.IRegisterableBlock;
import openmods.sync.SyncableDirection;
import openmods.tileentity.OpenTileEntity;
import openmods.tileentity.SyncedTileEntity;
import openmods.utils.BlockNotifyFlags;
import openmods.utils.BlockUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class OpenBlock extends Block implements IRegisterableBlock {

	private static final String SYNCED_ROTATION_VAR = "_rotation2";
	public static final int OPEN_MODS_TE_GUI = -1;

	/***
	 * The block rotation mode. Defines how many levels of rotation
	 * a block can have
	 */
	public enum BlockRotationMode {
		NONE(),
		FOUR_DIRECTIONS(ForgeDirection.UP, ForgeDirection.DOWN),
		SIX_DIRECTIONS(ForgeDirection.VALID_DIRECTIONS),
		TWENTYFOUR_DIRECTIONS(ForgeDirection.VALID_DIRECTIONS);

		private BlockRotationMode(ForgeDirection... rotations) {
			this.rotations = rotations;
		}

		private final ForgeDirection[] rotations;
	}

	/***
	 * The block placement mode. Does it rotate based on the surface
	 * it's placed on, or the
	 */
	public enum BlockPlacementMode {
		ENTITY_ANGLE,
		SURFACE
	}

	private String blockName;
	private String modId;

	/**
	 * The tile entity class associated with this block
	 */
	private Class<? extends TileEntity> teClass = null;
	protected BlockRotationMode blockRotationMode;
	protected BlockPlacementMode blockPlacementMode;
	protected ForgeDirection inventoryRenderRotation = ForgeDirection.WEST;

	public IIcon[] textures = new IIcon[6];

	protected OpenBlock(Material material) {
		super(material);
		setHardness(1.0F);
		setRotationMode(BlockRotationMode.NONE);
		setPlacementMode(BlockPlacementMode.ENTITY_ANGLE);

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

	@SideOnly(Side.CLIENT)
	public ForgeDirection getInventoryRenderRotation() {
		return inventoryRenderRotation;
	}

	/**
	 * Set block bounds based on rotation
	 * 
	 * @param direction
	 *            direction to apply bounds to
	 */
	public void setBoundsBasedOnRotation(ForgeDirection direction) {

	}

	/**
	 * Helper function to get the OpenBlock class for a block in the world
	 * 
	 * @param world
	 *            world to get the block from
	 * @param x
	 *            X coord
	 * @param y
	 *            Y coord
	 * @param z
	 *            Z coord
	 * @return OpenBlock instance of the block, or null if invalid
	 */
	public static OpenBlock getOpenBlock(IBlockAccess world, int x, int y, int z) {
		if (world == null) return null;
		Block block = world.getBlock(x, y, z);
		if (block instanceof OpenBlock) return (OpenBlock)block;
		return null;
	}

	@Override
	public TileEntity createTileEntity(World world, int metadata) {
		final TileEntity te = createTileEntity();

		te.blockType = this;
		if (te instanceof OpenTileEntity) {
			((OpenTileEntity)te).setup();
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
		try {
			if (teClass != null) return teClass.getConstructor(new Class[0]).newInstance();
		} catch (NoSuchMethodException nsm) {
			Log.warn(nsm, "Notice: Cannot create TE automatically due to constructor requirements");
		} catch (Exception ex) {
			Log.warn(ex, "Notice: Error creating tile entity");
		}
		return null;
	}

	public Class<? extends TileEntity> getTileClass() {
		return teClass;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister registry) {
		this.blockIcon = registry.registerIcon(String.format("%s:%s", modId, blockName));
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block par5, int par6) {
		final TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof IBreakAwareTile) ((IBreakAwareTile)te).onBlockBroken();
		world.removeTileEntity(x, y, z);
		super.breakBlock(world, x, y, z, par5, par6);
	}

	private void getTileEntityDrops(TileEntity te, List<ItemStack> result, int fortune) {
		if (te != null) {
			BlockUtils.getTileInventoryDrops(te, result);
			if (te instanceof ISpecialDrops) ((ISpecialDrops)te).addDrops(result, fortune);
			getCustomTileEntityDrops(te, result, fortune);
		}
	}

	protected void getCustomTileEntityDrops(TileEntity te, List<ItemStack> result, int fortune) {}

	protected boolean hasNormalDrops() {
		return true;
	}

	protected boolean hasTileEntityDrops() {
		return true;
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		// This is last place we have TE, before it's removed,
		// When removed by player, it will be already unavailable in
		// getBlockDropped
		// TODO: evaluate, if new behaviour can be used
		if (willHarvest && hasTileEntityDrops() && !player.capabilities.isCreativeMode) {
			final TileEntity te = world.getTileEntity(x, y, z);

			boolean result = super.removedByPlayer(world, player, x, y, z, willHarvest);

			if (result) {
				List<ItemStack> teDrops = Lists.newArrayList();
				getTileEntityDrops(te, teDrops, 0);
				for (ItemStack drop : teDrops)
					dropBlockAsItem(world, x, y, z, drop);
			}

			return result;
		}

		return super.removedByPlayer(world, player, x, y, z, willHarvest);
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		ArrayList<ItemStack> result = Lists.newArrayList();
		if (hasNormalDrops()) result.addAll(super.getDrops(world, x, y, z, metadata, fortune));
		if (hasTileEntityDrops()) {
			final TileEntity te = world.getTileEntity(x, y, z);
			getTileEntityDrops(te, result, fortune);
		}

		return result;
	}

	@Override
	public void setupBlock(String modId, String blockName, Class<? extends TileEntity> tileEntity, Class<? extends ItemBlock> itemClass) {
		this.blockName = blockName;
		this.modId = modId;

		if (tileEntity != null) {
			this.teClass = tileEntity;
			isBlockContainer = true;

			if (blockRotationMode == BlockRotationMode.TWENTYFOUR_DIRECTIONS) {
				Preconditions.checkArgument(SyncedTileEntity.class.isAssignableFrom(tileEntity),
						"To use 24-direction rotations TE class '%s' needs to implement SyncedTileEntity", tileEntity);
			}
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

	/***
	 * An extended block placement function which includes ALL the details
	 * you'll ever need.
	 * This is called if your ItemBlock extends ItemOpenBlock
	 */
	public void onBlockPlacedBy(World world, EntityPlayer player, ItemStack stack, int x, int y, int z, ForgeDirection side, float hitX, float hitY, float hitZ, int meta) {
		ForgeDirection additionalRotation = null;

		// We use both for 24's, so force to angle
		if (getRotationMode() == BlockRotationMode.TWENTYFOUR_DIRECTIONS) {
			setPlacementMode(BlockPlacementMode.ENTITY_ANGLE);
		}

		switch (this.blockPlacementMode) {
			case SURFACE:
				meta = side.getOpposite().ordinal();
				break;
			default:
				switch (getRotationMode()) {
					case FOUR_DIRECTIONS:
						meta = BlockUtils.get2dOrientation(player).ordinal();
						break;
					case SIX_DIRECTIONS:
						meta = BlockUtils.get3dOrientation(player).ordinal();
						break;
					case TWENTYFOUR_DIRECTIONS:
						meta = side.getOpposite().ordinal();
						additionalRotation = BlockUtils.get2dOrientation(player);
						break;
					default:
						break;
				}
		}
		world.setBlockMetadataWithNotify(x, y, z, meta, BlockNotifyFlags.ALL);

		TileEntity te = world.getTileEntity(x, y, z);

		if (additionalRotation != null) {
			Preconditions.checkState(te instanceof SyncedTileEntity,
					"For 6+ levels of rotation you need to use a SyncedTileEntity, but '%s' on block '%s' is not one", te, this);
			SyncedTileEntity ste = (SyncedTileEntity)te;
			ste.addSyncedObject(SYNCED_ROTATION_VAR, new SyncableDirection(additionalRotation));
			ste.sync();
		}

		if (te instanceof IPlaceAwareTile) ((IPlaceAwareTile)te).onBlockPlacedBy(player, side, stack, hitX, hitY, hitZ);
	}

	@Override
	public final boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
		return canPlaceBlockOnSide(world, x, y, z, ForgeDirection.getOrientation(side).getOpposite());
	}

	/***
	 * 
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @param side
	 * @return
	 */
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
		ForgeDirection rotation = ForgeDirection.getOrientation(metadata);
		ForgeDirection dir = ForgeDirection.getOrientation(side);
		switch (getRotationMode()) {
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

	public abstract boolean shouldRenderBlock();

	protected abstract Object getModInstance();

	public void openGui(EntityPlayer player, World world, int x, int y, int z) {
		player.openGui(getModInstance(), OPEN_MODS_TE_GUI, world, x, y, z);
	}

	public boolean useTESRForInventory() {
		return true;
	}

	public boolean canRotateWithTool() {
		return true;
	}

	@Override
	public boolean rotateBlock(World worldObj, int x, int y, int z, ForgeDirection axis) {
		return canRotateWithTool() && RotationHelper.rotateBlock(this, worldObj, x, y, z, axis);
	}

	@Override
	public ForgeDirection[] getValidRotations(World worldObj, int x, int y, int z) {
		if (!canRotateWithTool()) return BlockRotationMode.NONE.rotations;
		return blockRotationMode.rotations;
	}
}
