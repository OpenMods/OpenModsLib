package openmods.tileentity;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import openmods.api.IInventoryCallback;
import openmods.block.BlockRotationMode;
import openmods.block.IBlockRotationMode;
import openmods.block.OpenBlock;
import openmods.geometry.LocalDirections;
import openmods.geometry.Orientation;
import openmods.inventory.GenericInventory;
import openmods.network.DimCoord;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.IRpcTargetProvider;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.TileEntityRpcTarget;
import openmods.network.senders.IPacketSender;
import openmods.reflection.TypeUtils;
import openmods.utils.BlockUtils;

public abstract class OpenTileEntity extends TileEntity implements IRpcTargetProvider {

	/** Place for TE specific setup. Called once upon creation */
	public void setup() {}

	public DimCoord getDimCoords() {
		return new DimCoord(worldObj.provider.getDimension(), pos);
	}

	public Orientation getOrientation() {
		final IBlockState state = worldObj.getBlockState(pos);
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return Orientation.XP_YP;
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.getOrientation(state);
	}

	public IBlockRotationMode getRotationMode() {
		final IBlockState state = worldObj.getBlockState(pos);
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return BlockRotationMode.NONE;
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.rotationMode;
	}

	public EnumFacing getFront() {
		final IBlockState state = worldObj.getBlockState(pos);
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return EnumFacing.NORTH;
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.getFront(state);
	}

	public EnumFacing getBack() {
		return getFront().getOpposite();
	}

	public LocalDirections getLocalDirections() {
		final IBlockState state = worldObj.getBlockState(pos);
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return LocalDirections.fromFrontAndTop(EnumFacing.NORTH, EnumFacing.UP);
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.getLocalDirections(state);
	}

	public boolean isAddedToWorld() {
		return worldObj != null;
	}

	protected TileEntity getTileEntity(BlockPos blockPos) {
		return (worldObj != null && worldObj.isBlockLoaded(blockPos))? worldObj.getTileEntity(blockPos) : null;
	}

	public TileEntity getTileInDirection(EnumFacing direction) {
		return getTileEntity(pos.offset(direction));
	}

	public boolean isAirBlock(EnumFacing direction) {
		return worldObj != null && worldObj.isAirBlock(getPos().offset(direction));
	}

	protected void playSoundAtBlock(SoundEvent sound, SoundCategory category, float volume, float pitch) {
		BlockUtils.playSoundAtPos(worldObj, pos, sound, category, volume, pitch);
	}

	protected void playSoundAtBlock(SoundEvent sound, float volume, float pitch) {
		playSoundAtBlock(sound, SoundCategory.BLOCKS, volume, pitch);
	}

	protected void spawnParticle(EnumParticleTypes particle, double dx, double dy, double dz, double vx, double vy, double vz, int... args) {
		worldObj.spawnParticle(particle, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, vx, vy, vz, args);
	}

	protected void spawnParticle(EnumParticleTypes particle, double vx, double vy, double vz, int... args) {
		spawnParticle(particle, 0.5, 0.5, 0.5, vx, vy, vz, args);
	}

	public void sendBlockEvent(int event, int param) {
		worldObj.addBlockEvent(pos, getBlockType(), event, param);
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}

	public void openGui(Object instance, EntityPlayer player) {
		player.openGui(instance, -1, worldObj, pos.getX(), pos.getY(), pos.getZ());
	}

	public AxisAlignedBB getBB() {
		return new AxisAlignedBB(pos, pos.add(1, 1, 1));
	}

	@Override
	public IRpcTarget createRpcTarget() {
		return new TileEntityRpcTarget(this);
	}

	public <T> T createProxy(final IPacketSender sender, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		TypeUtils.isInstance(this, mainIntf, extraIntf);
		return RpcCallDispatcher.INSTANCE.createProxy(createRpcTarget(), sender, mainIntf, extraIntf);
	}

	public <T> T createClientRpcProxy(Class<? extends T> mainIntf, Class<?>... extraIntf) {
		final IPacketSender sender = RpcCallDispatcher.INSTANCE.senders.client;
		return createProxy(sender, mainIntf, extraIntf);
	}

	public <T> T createServerRpcProxy(Class<? extends T> mainIntf, Class<?>... extraIntf) {
		final IPacketSender sender = RpcCallDispatcher.INSTANCE.senders.block.bind(getDimCoords());
		return createProxy(sender, mainIntf, extraIntf);
	}

	public void markUpdated() {
		worldObj.markChunkDirty(pos, this);
	}

	protected IInventoryCallback createInventoryCallback() {
		return new IInventoryCallback() {
			@Override
			public void onInventoryChanged(IInventory inventory, int slotNumber) {
				markUpdated();
			}
		};
	}

	protected GenericInventory registerInventoryCallback(GenericInventory inventory) {
		return inventory.addCallback(createInventoryCallback());
	}

	public boolean isValid(EntityPlayer player) {
		return (worldObj.getTileEntity(pos) == this) && (player.getDistanceSqToCenter(pos) <= 64.0D);
	}
}
