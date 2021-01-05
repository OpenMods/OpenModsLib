package openmods.tileentity;

import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.IParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;
import openmods.api.IInventoryCallback;
import openmods.block.BlockRotationMode;
import openmods.block.IBlockRotationMode;
import openmods.block.OpenBlock;
import openmods.geometry.LocalDirections;
import openmods.geometry.Orientation;
import openmods.inventory.GenericInventory;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.IRpcTargetProvider;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.TileEntityRpcTarget;
import openmods.reflection.TypeUtils;
import openmods.utils.BlockUtils;

public abstract class OpenTileEntity extends TileEntity implements IRpcTargetProvider {

	public OpenTileEntity(TileEntityType<?> type) {
		super(type);
	}

	/** Place for TE specific setup. Called once upon creation */
	public void setup() {}

	public Orientation getOrientation() {
		final BlockState state = world.getBlockState(pos);
		return getOrientation(state);
	}

	public Orientation getOrientation(BlockState state) {
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return Orientation.XP_YP;
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.getOrientation(state);
	}

	public IBlockRotationMode getRotationMode() {
		final BlockState state = world.getBlockState(pos);
		return getRotationMode(state);
	}

	public IBlockRotationMode getRotationMode(BlockState state) {
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock.Orientable)) return BlockRotationMode.NONE;
		final OpenBlock.Orientable openBlock = (OpenBlock.Orientable)block;
		return openBlock.getRotationMode();
	}

	public Direction getFront() {
		final BlockState state = world.getBlockState(pos);
		return getFront(state);
	}

	public Direction getFront(BlockState state) {
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return Direction.NORTH;
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.getFront(state);
	}

	public Direction getBack() {
		return getFront().getOpposite();
	}

	public LocalDirections getLocalDirections() {
		final BlockState state = world.getBlockState(pos);
		final Block block = state.getBlock();
		if (!(block instanceof OpenBlock)) return LocalDirections.fromFrontAndTop(Direction.NORTH, Direction.UP);
		final OpenBlock openBlock = (OpenBlock)block;
		return openBlock.getLocalDirections(state);
	}

	public boolean isAddedToWorld() {
		return world != null;
	}

	protected TileEntity getTileEntity(BlockPos blockPos) {
		return (world != null && world.isBlockLoaded(blockPos))? world.getTileEntity(blockPos) : null;
	}

	public TileEntity getTileInDirection(Direction direction) {
		return getTileEntity(pos.offset(direction));
	}

	public boolean isAirBlock(Direction direction) {
		return world != null && world.isAirBlock(getPos().offset(direction));
	}

	protected void playSoundAtBlock(SoundEvent sound, SoundCategory category, float volume, float pitch) {
		BlockUtils.playSoundAtPos(world, pos, sound, category, volume, pitch);
	}

	protected void playSoundAtBlock(SoundEvent sound, float volume, float pitch) {
		playSoundAtBlock(sound, SoundCategory.BLOCKS, volume, pitch);
	}

	protected void spawnParticle(IParticleData particle, double dx, double dy, double dz, double vx, double vy, double vz) {
		world.addParticle(particle, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, vx, vy, vz);
	}

	protected void spawnParticle(IParticleData particle, double vx, double vy, double vz) {
		spawnParticle(particle, 0.5, 0.5, 0.5, vx, vy, vz);
	}

	public void sendBlockEvent(int event, int param) {
		world.addBlockEvent(pos, getBlockState().getBlock(), event, param);
	}

	public AxisAlignedBB getBB() {
		return new AxisAlignedBB(pos, pos.add(1, 1, 1));
	}

	@Override
	public IRpcTarget createRpcTarget() {
		return new TileEntityRpcTarget(this);
	}

	public <T> T createProxy(final PacketDistributor.PacketTarget sender, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		TypeUtils.isInstance(this, mainIntf, extraIntf);
		return RpcCallDispatcher.instance().createProxy(createRpcTarget(), sender, mainIntf, extraIntf);
	}

	public <T> T createClientRpcProxy(Class<? extends T> mainIntf, Class<?>... extraIntf) {
		return createProxy(PacketDistributor.SERVER.noArg(), mainIntf, extraIntf);
	}

	public <T> T createServerRpcProxy(Class<? extends T> mainIntf, Class<?>... extraIntf) {
		final ChunkManager chunkManager = ((ServerWorld)getWorld()).getChunkProvider().chunkManager;
		return createProxy(PacketDistributor.NMLIST.with(() ->
						chunkManager.getTrackingPlayers(new ChunkPos(pos), false)
								.map(p -> p.connection.netManager)
								.collect(Collectors.toList())),
				mainIntf, extraIntf);
	}

	public void markUpdated() {
		world.markChunkDirty(pos, this);
	}

	protected IInventoryCallback createInventoryCallback() {
		return (inventory, slotNumber) -> markUpdated();
	}

	protected GenericInventory registerInventoryCallback(GenericInventory inventory) {
		return inventory.addCallback(createInventoryCallback());
	}

	public boolean isValid(PlayerEntity player) {
		return (world.getTileEntity(pos) == this) && (player.getDistanceSq((double)this.pos.getX() + 0.5D, (double)this.pos.getY() + 0.5D, (double)this.pos.getZ() + 0.5D) > 64.0D);
	}
}
