package openmods.entity;

import io.netty.buffer.ByteBuf;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import openmods.fakeplayer.FakePlayerPool;
import openmods.utils.BlockManipulator;
import openmods.utils.NbtUtils;

public class EntityBlock extends Entity implements IEntityAdditionalSpawnData {

	private static final String TAG_TILE_ENTITY = "TileEntity";
	private static final String TAG_BLOCK_META = "BlockMeta";
	private static final String TAG_BLOCK_ID = "BlockId";

	public static final Direction[] PLACE_DIRECTIONS = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP };
	private static final String TAG_BLOCK_STATE_ID = "BlockState";

	private boolean hasGravity = false;
	/* Should this entity return to a block on the ground? */
	private boolean shouldDrop = true;
	private boolean hasAirResistance = true;

	private BlockState blockState;
	private CompoundNBT tileEntity;

	public EntityBlock(World world) {
		super(world);
		setSize(0.925F, 0.925F);
	}

	public EntityBlock(World world, BlockState state, CompoundNBT tileEntity) {
		super(world);
		setSize(0.925F, 0.925F);
	}

	public static void registerFixes(DataFixer fixers, final Class<? extends EntityBlock> cls) {
		fixers.registerWalker(FixTypes.ENTITY, (fixer, compound, versionIn) -> {
			if (EntityList.getKey(cls).equals(new ResourceLocation(compound.getString("id")))) {
				if (compound.hasKey(TAG_TILE_ENTITY, Constants.NBT.TAG_COMPOUND)) {
					final CompoundNBT teTag = compound.getCompoundTag(TAG_TILE_ENTITY);
					final CompoundNBT fixedTeTag = fixer.process(FixTypes.BLOCK_ENTITY, teTag, versionIn);
					compound.setTag(TAG_TILE_ENTITY, fixedTeTag);
				}
			}

			return compound;
		});
	}

	public interface EntityFactory {
		EntityBlock create(World world);
	}

	public static EntityBlock create(PlayerEntity player, World world, BlockPos pos) {
		return create(player, world, pos, EntityBlock::new);
	}

	public static EntityBlock create(LivingEntity creator, World world, BlockPos pos, EntityFactory factory) {
		if (world.isAirBlock(pos)) return null;

		if (!(creator instanceof PlayerEntity)) return null;

		final PlayerEntity player = (PlayerEntity)creator;

		final EntityBlock entity = factory.create(world);

		entity.blockState = world.getBlockState(pos);

		final TileEntity te = world.getTileEntity(pos);
		if (te != null) {
			entity.tileEntity = te.writeToNBT(new CompoundNBT());
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
	protected void readEntityFromNBT(CompoundNBT tag) {
		if (tag.hasKey(TAG_BLOCK_STATE_ID)) {
			final int blockStateId = tag.getInteger(TAG_BLOCK_STATE_ID);
			this.blockState = Block.getStateById(blockStateId);
		} else {
			int meta = tag.getByte(TAG_BLOCK_META) & 255;

			final ResourceLocation blockId = NbtUtils.readResourceLocation(tag.getCompoundTag(TAG_BLOCK_ID));
			final Block block = Block.REGISTRY.getObject(blockId);
			this.blockState = block.getStateFromMeta(meta);
		}
		if (tag.hasKey(TAG_TILE_ENTITY, Constants.NBT.TAG_COMPOUND)) this.tileEntity = tag.getCompoundTag(TAG_TILE_ENTITY);
		else this.tileEntity = null;
	}

	@Override
	protected void writeEntityToNBT(CompoundNBT tag) {
		tag.setInteger(TAG_BLOCK_STATE_ID, Block.getStateId(blockState));
		if (tileEntity != null) tag.setTag(TAG_TILE_ENTITY, tileEntity.copy());
	}

	@Override
	public void writeSpawnData(ByteBuf data) {
		data.writeInt(Block.getStateId(blockState));
		data.writeBoolean(hasGravity);
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		this.blockState = Block.getStateById(additionalData.readInt());
		this.hasGravity = additionalData.readBoolean();
	}

	@Override
	public void onUpdate() {
		if (posY < -500.0D) {
			setDead();
			return;
		}

		if (hasGravity) {
			motionY -= 0.03999999910593033D;
		}
		if (hasAirResistance) {
			motionX *= 0.98;
			motionY *= 0.98;
			motionZ *= 0.98;
		}
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;

		extinguish();
		move(MoverType.SELF, motionX, motionY, motionZ);

		final Block block = blockState.getBlock();
		if (block == Blocks.AIR) setDead();
		// TODO missing functionality, fix (fake world access?)
		// setHeight((float)block.getBlockBoundsMaxY());

		if (world instanceof ServerWorld && shouldPlaceBlock()) {
			final BlockPos dropPos = new BlockPos(getEntityBoundingBox().getCenter());
			if (!tryPlaceBlock((ServerWorld)world, dropPos)) dropBlock();

			setDead();
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
			tileEntity.setInteger("x", pos.getX());
			tileEntity.setInteger("y", pos.getY());
			tileEntity.setInteger("z", pos.getZ());
			TileEntity te = world.getTileEntity(pos);
			te.readFromNBT(tileEntity);
		}

		return true;
	}

	private void dropBlock() {
		final Block block = blockState.getBlock();

		Random rand = world.rand;

		final int count = block.quantityDropped(blockState, 0, rand);
		for (int i = 0; i < count; i++) {
			final Item item = block.getItemDropped(blockState, rand, 0);
			ItemStack toDrop = new ItemStack(item, 1, block.damageDropped(blockState));
			entityDropItem(toDrop, 0.1f);
		}

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
		return !isDead;
	}

	@Override
	public boolean canBePushed() {
		return !isDead;
	}

	@Override
	protected void dealFireDamage(int i) {}

	@Override
	public boolean attackEntityFrom(DamageSource p_70097_1_, float p_70097_2_) {
		if (!isDead && !world.isRemote) dropBlock();
		setDead();
		return false;
	}

	@Override
	protected void entityInit() {}
}