package openmods.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import openmods.Log;
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
import openmods.config.game.IRegisterableBlock;
import openmods.geometry.Orientation;
import openmods.inventory.IInventoryProvider;
import openmods.tileentity.OpenTileEntity;
import openmods.utils.BlockNotifyFlags;
import openmods.utils.BlockUtils;

public class OpenBlock extends Block implements IRegisterableBlock {

	public static class TwoDirections extends OpenBlock {
		public TwoDirections(Material material) {
			super(material);
		}

		@Override
		public BlockRotationMode getRotationMode() {
			return BlockRotationMode.TWO_DIRECTIONS;
		}
	}

	public static class ThreeDirections extends OpenBlock {
		public ThreeDirections(Material material) {
			super(material);
		}

		@Override
		public BlockRotationMode getRotationMode() {
			return BlockRotationMode.THREE_DIRECTIONS;
		}
	}

	public static class FourDirections extends OpenBlock {
		public FourDirections(Material material) {
			super(material);
		}

		@Override
		public BlockRotationMode getRotationMode() {
			return BlockRotationMode.FOUR_DIRECTIONS;
		}
	}

	public static class SixDirections extends OpenBlock {
		public SixDirections(Material material) {
			super(material);
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

		private TileEntityCapability(Class<?> intf) {
			this.intf = intf;
		}
	}

	public enum BlockPlacementMode {
		ENTITY_ANGLE,
		SURFACE
	}

	private final Set<TileEntityCapability> teCapabilities = EnumSet.noneOf(TileEntityCapability.class);

	private Object modInstance = null;
	private Class<? extends TileEntity> teClass = null;

	private BlockPlacementMode blockPlacementMode = BlockPlacementMode.ENTITY_ANGLE;

	public final BlockRotationMode rotationMode;

	public final IProperty<Orientation> propertyOrientation;

	protected Orientation inventoryRenderOrientation;

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

	public OpenBlock(Material material) {
		super(material);
		setHardness(1.0F);

		// I dont think vanilla actually uses this..
		this.isBlockContainer = false;

		this.rotationMode = getRotationMode();
		Preconditions.checkNotNull(this.rotationMode);
		this.propertyOrientation = this.rotationMode.property;
	}

	protected void setPlacementMode(BlockPlacementMode mode) {
		this.blockPlacementMode = mode;
	}

	protected BlockRotationMode getRotationMode() {
		return BlockRotationMode.NONE;
	}

	protected IProperty<Orientation> getPropertyOrientation() {
		return getRotationMode().property;
	}

	protected BlockPlacementMode getPlacementMode() {
		return this.blockPlacementMode;
	}

	protected void setInventoryRenderOrientation(Orientation orientation) {
		inventoryRenderOrientation = orientation;
	}

	protected Orientation getOrientation(IBlockAccess world, BlockPos pos) {
		final IBlockState state = world.getBlockState(pos);
		return getOrientation(state);
	}

	public Orientation getOrientation(IBlockState state) {
		// TODO fix semi-hack
		return (state.getBlock() instanceof OpenBlock)? state.getValue(propertyOrientation) : Orientation.XP_YP;
	}

	public boolean shouldDropFromTeAfterBreak() {
		return true;
	}

	public boolean shouldOverrideHarvestWithTeLogic() {
		return hasCapability(TileEntityCapability.CUSTOM_HARVEST_DROPS);
	}

	public static OpenBlock getOpenBlock(IBlockAccess world, BlockPos blockPos) {
		if (world == null)
			return null;
		Block block = world.getBlockState(blockPos).getBlock();
		if (block instanceof OpenBlock)
			return (OpenBlock)block;
		return null;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
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

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		if (hasCapability(TileEntityCapability.CUSTOM_PICK_ITEM)) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof ICustomPickItem)
				return ((ICustomPickItem)te).getPickBlock(player);
		}

		return suppressPickBlock()? null : super.getPickBlock(state, target, world, pos, player);
	}

	private static List<ItemStack> getTileBreakDrops(TileEntity te) {
		List<ItemStack> breakDrops = Lists.newArrayList();
		BlockUtils.getTileInventoryDrops(te, breakDrops);
		if (te instanceof ICustomBreakDrops)
			((ICustomBreakDrops)te).addDrops(breakDrops);
		return breakDrops;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if (shouldDropFromTeAfterBreak()) {
			final TileEntity te = world.getTileEntity(pos);
			if (te != null) {
				if (te instanceof IBreakAwareTile)
					((IBreakAwareTile)te).onBlockBroken();

				for (ItemStack stack : getTileBreakDrops(te))
					BlockUtils.dropItemStackInWorld(world, pos, stack);

				world.removeTileEntity(pos);
			}
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack) {
		player.addStat(StatList.getBlockStats(this));
		player.addExhaustion(0.025F);

		if (canSilkHarvest(world, pos, state, player) && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0) {
			handleSilkTouchDrops(world, player, pos, state, te);
		} else {
			handleNormalDrops(world, player, pos, state, te, stack);
		}
	}

	protected void handleNormalDrops(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack) {
		harvesters.set(player);
		final int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack);

		boolean addNormalDrops = true;

		if (te instanceof ICustomHarvestDrops) {
			final ICustomHarvestDrops dropper = (ICustomHarvestDrops)te;
			final List<ItemStack> drops = Lists.newArrayList();
			dropper.addHarvestDrops(player, drops, fortune, false);

			ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, fortune, 1.0f, false, player);
			for (ItemStack drop : drops)
				spawnAsEntity(world, pos, drop);

			addNormalDrops = !dropper.suppressBlockHarvestDrops();
		}

		if (addNormalDrops)
			dropBlockAsItem(world, pos, state, fortune);

		harvesters.set(null);
	}

	protected void handleSilkTouchDrops(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te) {
		List<ItemStack> items = Lists.newArrayList();

		boolean addNormalDrops = true;

		if (te instanceof ICustomHarvestDrops) {
			final ICustomHarvestDrops dropper = (ICustomHarvestDrops)te;

			dropper.addHarvestDrops(player, items, 0, true);
			addNormalDrops = !dropper.suppressBlockHarvestDrops();
		}

		if (addNormalDrops) {
			final ItemStack drop = createStackedBlock(state);
			if (drop != null)
				items.add(drop);
		}

		ForgeEventFactory.fireBlockHarvesting(items, world, pos, state, 0, 1.0f, true, player);
		for (ItemStack stack : items)
			spawnAsEntity(world, pos, stack);
	}

	@Override
	public void setupBlock(ModContainer container, String id, Class<? extends TileEntity> tileEntity, ItemBlock itemBlock) {
		this.modInstance = container.getMod();

		if (tileEntity != null) {
			this.teClass = tileEntity;
			isBlockContainer = true;

			for (TileEntityCapability capability : TileEntityCapability.values())
				if (capability.intf.isAssignableFrom(teClass))
					teCapabilities.add(capability);
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
			if (isNeighborBlockSolid(world, blockPos, side))
				return true;
		}
		return false;
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos blockPos, Block neighbour) {
		if (hasCapabilities(TileEntityCapability.NEIGBOUR_LISTENER, TileEntityCapability.SURFACE_ATTACHEMENT)) {
			final TileEntity te = world.getTileEntity(blockPos);
			if (te instanceof INeighbourAwareTile)
				((INeighbourAwareTile)te).onNeighbourChanged(neighbour);

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
	public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
		if (hasCapability(TileEntityCapability.NEIGBOUR_TE_LISTENER)) {
			final TileEntity te = world.getTileEntity(pos);
			if (te instanceof INeighbourTeAwareTile)
				((INeighbourTeAwareTile)te).onNeighbourTeChanged(pos);
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
	public boolean onBlockActivated(World world, BlockPos blockPos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (hasCapabilities(TileEntityCapability.GUI_PROVIDER, TileEntityCapability.ACTIVATE_LISTENER)) {
			final TileEntity te = world.getTileEntity(blockPos);

			if (te instanceof IHasGui && ((IHasGui)te).canOpenGui(player) && !player.isSneaking()) {
				if (!world.isRemote)
					openGui(player, world, blockPos);
				return true;
			}

			// TODO Expand for new args
			if (te instanceof IActivateAwareTile)
				return ((IActivateAwareTile)te).onBlockActivated(player, side, hitX, hitY, hitZ);
		}

		return false;
	}

	@SuppressWarnings("deprecation") // TODO review
	@Override
	public boolean eventReceived(IBlockState state, World world, BlockPos blockPos, int eventId, int eventParam) {
		if (eventId < 0 && !world.isRemote) {
			switch (eventId) {
				case EVENT_ADDED: {
					if (hasCapability(TileEntityCapability.ADD_LISTENER)) {
						final IAddAwareTile te = getTileEntity(world, blockPos, IAddAwareTile.class);
						if (te != null)
							te.onAdded();
					}
				}
					break;
			}

			return false;
		}
		if (isBlockContainer) {
			super.eventReceived(state, world, blockPos, eventId, eventParam);
			TileEntity te = world.getTileEntity(blockPos);
			return te != null? te.receiveClientEvent(eventId, eventParam) : false;
		} else {
			return super.eventReceived(state, world, blockPos, eventId, eventParam);
		}
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

	protected Orientation calculateOrientationAfterPlace(BlockPos pos, EnumFacing facing, EntityLivingBase placer) {
		if (blockPlacementMode == BlockPlacementMode.SURFACE) {
			return rotationMode.getPlacementOrientationFromSurface(pos, facing);
		} else {
			return rotationMode.getPlacementOrientationFromEntity(pos, placer);
		}
	}

	public boolean canBlockBePlaced(World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ, int itemMetadata, EntityPlayer player) {
		return true;
	}

	@Override
	public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		final Orientation orientation = calculateOrientationAfterPlace(pos, facing, placer);
		return getStateFromMeta(meta).withProperty(propertyOrientation, orientation);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos blockPos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, blockPos, state, placer, stack);

		if (hasCapability(TileEntityCapability.PLACE_LISTENER)) {
			final TileEntity te = world.getTileEntity(blockPos);
			if (te instanceof IPlaceAwareTile)
				((IPlaceAwareTile)te).onBlockPlacedBy(state, placer, stack);
		}
	}

	protected boolean isOnTopOfSolidBlock(World world, BlockPos blockPos, EnumFacing side) {
		return side == EnumFacing.UP
				&& isNeighborBlockSolid(world, blockPos, EnumFacing.DOWN);
	}

	public void openGui(EntityPlayer player, World world, BlockPos blockPos) {
		player.openGui(modInstance, OPEN_MODS_TE_GUI, world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}

	public Orientation getOrientationFromMeta(int meta) {
		return rotationMode.fromValue(meta & rotationMode.mask);
	}

	public int getMetaFromOrientation(Orientation orientation) {
		return rotationMode.toValue(orientation);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState()
				.withProperty(propertyOrientation, getOrientationFromMeta(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		final Orientation orientation = state.getValue(propertyOrientation);
		return getMetaFromOrientation(orientation);
	}

	@SideOnly(Side.CLIENT)
	public Orientation getInventoryRenderOrientation() {
		return inventoryRenderOrientation != null? inventoryRenderOrientation : rotationMode.getInventoryRenderOrientation();
	}

	@Override
	protected BlockStateContainer createBlockState() {
		// WARNING: this is called from superclass, so rotationMode is not set yet
		return new BlockStateContainer(this, getPropertyOrientation());
	}

	@Override
	public boolean rotateBlock(World worldObj, BlockPos blockPos, EnumFacing axis) {
		if (!canRotateWithTool())
			return false;

		final IBlockState currentState = worldObj.getBlockState(blockPos);

		final Orientation orientation = currentState.getValue(propertyOrientation);

		final Orientation newOrientation = rotationMode.calculateToolRotation(orientation, axis);

		if (newOrientation != null) {
			if (rotationMode.isPlacementValid(newOrientation)) {
				final IBlockState newState = createNewStateAfterRotation(worldObj, blockPos, currentState, propertyOrientation, newOrientation);
				worldObj.setBlockState(blockPos, newState, BlockNotifyFlags.ALL);
			} else {
				Log.info("Invalid tool rotation: [%s] %s: (%s): %s->%s", rotationMode, axis, blockPos, orientation, newOrientation);
				return false;
			}
		}

		if (teCapabilities.contains(TileEntityCapability.SURFACE_ATTACHEMENT)) {
			final ISurfaceAttachment te = getTileEntity(worldObj, blockPos, ISurfaceAttachment.class);
			if (te == null)
				return false;

			breakBlockIfSideNotSolid(worldObj, blockPos, te.getSurfaceDirection());
		}

		return true;
	}

	protected IBlockState createNewStateAfterRotation(World worldObj, BlockPos blockPos, IBlockState currentState, IProperty<Orientation> currentOrientation,
			Orientation newOrientation) {
		return currentState.withProperty(propertyOrientation, newOrientation);
	}

	public boolean canRotateWithTool() {
		return rotationMode.toolRotationAllowed();
	}

	@Override
	public EnumFacing[] getValidRotations(World worldObj, BlockPos pos) {
		if (!canRotateWithTool())
			return RotationAxis.NO_AXIS;
		return rotationMode.rotationAxes;
	}
}
