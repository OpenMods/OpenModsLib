package openmods.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.ObjectHolder;
import openmods.OpenMods;
import openmods.fakeplayer.FakePlayerPool;
import openmods.utils.BlockManipulator;

public class EntityBlock extends Entity implements IEntityAdditionalSpawnData {

	@ObjectHolder(OpenMods.ENTITY_BLOCK)
	public static EntityType<?> TYPE;

	public static final Direction[] PLACE_DIRECTIONS = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP };
	private static final String TAG_BLOCK_STATE = "BlockState";
	private static final String TAG_TILE_ENTITY = "TileEntity";

	private boolean hasGravity = false;
	/* Should this entity return to a block on the ground? */
	private boolean shouldDrop = true;
	private boolean hasAirResistance = true;

	private BlockState blockState;
	private CompoundNBT tileEntity;

	public EntityBlock(final EntityType<?> type, World world) {
		super(type, world);
	}

	public interface EntityFactory {
		EntityBlock create(World world);
	}

	public static EntityBlock create(final World world) {
		return new EntityBlock(TYPE, world);
	}

	public static EntityBlock create(PlayerEntity player, World world, BlockPos pos) {
		return create(player, world, pos, EntityBlock::create);
	}

	public static EntityBlock create(LivingEntity creator, World world, BlockPos pos, EntityFactory factory) {
		if (world.isAirBlock(pos)) return null;

		if (!(creator instanceof PlayerEntity)) return null;

		final PlayerEntity player = (PlayerEntity)creator;

		final EntityBlock entity = factory.create(world);

		entity.blockState = world.getBlockState(pos);

		final TileEntity te = world.getTileEntity(pos);
		if (te != null) {
			entity.tileEntity = te.write(new CompoundNBT());
		}

		final boolean blockRemoved = new BlockManipulator(world, player, pos).setSilentTeRemove(true).remove();
		if (!blockRemoved) return null;

		entity.setPositionAndRotation(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0);

		return entity;
	}

	public BlockState getBlockState() {
		return blockState;
	}

	public void setShouldDrop(boolean bool) {
		shouldDrop = bool;
	}

	public void setHasAirResistance(boolean bool) {
		this.hasAirResistance = bool;
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void readAdditional(CompoundNBT tag) {
		this.blockState = NBTUtil.readBlockState(tag.getCompound(TAG_BLOCK_STATE));
		if (tag.contains(TAG_TILE_ENTITY, Constants.NBT.TAG_COMPOUND)) this.tileEntity = tag.getCompound(TAG_TILE_ENTITY);
		else this.tileEntity = null;
	}

	@Override
	protected void writeAdditional(CompoundNBT tag) {
		tag.put(TAG_BLOCK_STATE, NBTUtil.writeBlockState(blockState));
		if (tileEntity != null) tag.put(TAG_TILE_ENTITY, tileEntity.copy());
	}

	@Override
	public void writeSpawnData(PacketBuffer data) {
		data.writeInt(Block.getStateId(blockState));
		data.writeBoolean(hasGravity);
	}

	@Override
	public void readSpawnData(PacketBuffer additionalData) {
		this.blockState = Block.getStateById(additionalData.readInt());
		this.hasGravity = additionalData.readBoolean();
	}

	@Override
	public void tick() {
		if (getPosY() < -500.0D) {
			remove();
			return;
		}

		if (!this.hasNoGravity()) {
			this.setMotion(this.getMotion().add(0.0D, -0.04D, 0.0D));
		}

		if (hasAirResistance) {
			this.setMotion(getMotion().scale(0.98));
		}

		prevPosX = getPosX();
		prevPosY = getPosY();
		prevPosZ = getPosZ();

		extinguish();
		this.move(MoverType.SELF, this.getMotion());

		final Block block = blockState.getBlock();
		if (block == Blocks.AIR) remove();
		// TODO missing functionality, fix (fake world access?)
		// setHeight((float)block.getBlockBoundsMaxY());

		if (world instanceof ServerWorld && shouldPlaceBlock()) {
			final BlockPos dropPos = new BlockPos(getBoundingBox().getCenter());
			if (!tryPlaceBlock((ServerWorld)world, dropPos)) dropBlock();

			remove();
		}
	}

	protected boolean shouldPlaceBlock() {
		return onGround && shouldDrop;
	}

	private boolean tryPlaceBlock(final ServerWorld world, final BlockPos pos) {
		return FakePlayerPool.instance.executeOnPlayer(world, fakePlayer -> {
			if (tryPlaceBlock(fakePlayer, world, pos, Direction.UP)) return true;

			for (Direction dir : PLACE_DIRECTIONS) {
				if (tryPlaceBlock(fakePlayer, world, pos.offset(dir), dir)) return true;
			}
			return false;
		});

	}

	private boolean tryPlaceBlock(PlayerEntity player, ServerWorld world, BlockPos pos, Direction fromSide) {
		if (!world.isAirBlock(pos)) return false;

		boolean blockPlaced = new BlockManipulator(world, player, pos).place(blockState, fromSide, Hand.MAIN_HAND);
		if (!blockPlaced) return false;

		if (tileEntity != null) {
			tileEntity.putInt("x", pos.getX());
			tileEntity.putInt("y", pos.getY());
			tileEntity.putInt("z", pos.getZ());
			TileEntity te = world.getTileEntity(pos);
			te.read(blockState, tileEntity);
		}

		return true;
	}

	private void dropBlock() {
		final Block block = blockState.getBlock();

		// TODO 1.14 redo drops

		if (tileEntity instanceof IInventory) {
			IInventory inv = (IInventory)tileEntity;
			for (int i = 0; i < inv.getSizeInventory(); i++) {
				entityDropItem(inv.getStackInSlot(i), 0.1f);
			}
		}
	}

	public void setHasGravity(boolean gravity) {
		this.hasGravity = gravity;
	}

	public boolean hasGravity() {
		return hasGravity;
	}

	@Override
	public boolean canRenderOnFire() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith() {
		return !isAlive();
	}

	@Override
	public boolean canBePushed() {
		return !removed;
	}

	@Override
	public boolean attackEntityFrom(DamageSource p_70097_1_, float p_70097_2_) {
		if (!isAlive() && !world.isRemote) dropBlock();
		remove();
		return false;
	}

	@Override
	protected void registerData() {}

	@Override public IPacket<?> createSpawnPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}
}