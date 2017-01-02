package openmods.entity;

import io.netty.buffer.ByteBuf;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import openmods.fakeplayer.FakePlayerPool;
import openmods.fakeplayer.FakePlayerPool.PlayerUserReturning;
import openmods.fakeplayer.OpenModsFakePlayer;
import openmods.utils.BlockManipulator;
import openmods.utils.NbtUtils;

public class EntityBlock extends Entity implements IEntityAdditionalSpawnData {

	private static final String TAG_TILE_ENTITY = "TileEntity";
	private static final String TAG_BLOCK_META = "BlockMeta";
	private static final String TAG_BLOCK_ID = "BlockId";

	public static final EnumFacing[] PLACE_DIRECTIONS = { EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN };

	private boolean hasGravity = false;
	/* Should this entity return to a block on the ground? */
	private boolean shouldDrop = true;
	private boolean hasAirResistance = true;

	private IBlockState blockState;
	private NBTTagCompound tileEntity;

	public EntityBlock(World world) {
		super(world);
		setSize(0.925F, 0.925F);
	}

	public EntityBlock(World world, IBlockState state, NBTTagCompound tileEntity) {
		super(world);
		setSize(0.925F, 0.925F);
	}

	private void setHeight(float height) {
		this.height = height;
	}

	public interface EntityFactory {
		public EntityBlock create(World world);
	}

	public static EntityBlock create(EntityPlayer player, World world, BlockPos pos) {
		return create(player, world, pos, new EntityFactory() {
			@Override
			public EntityBlock create(World world) {
				return new EntityBlock(world);
			}
		});
	}

	public static EntityBlock create(EntityPlayer player, World world, BlockPos pos, EntityFactory factory) {
		if (world.isAirBlock(pos)) return null;

		final EntityBlock entity = factory.create(world);

		entity.blockState = world.getBlockState(pos);

		final TileEntity te = world.getTileEntity(pos);
		if (te != null) {
			entity.tileEntity = new NBTTagCompound();
			te.writeToNBT(entity.tileEntity);
		}

		final boolean blockRemoved = new BlockManipulator(world, player, pos).setSilentTeRemove(true).remove();
		if (!blockRemoved) return null;

		entity.setPositionAndRotation(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0);

		return entity;
	}

	public IBlockState getBlockState() {
		return blockState;
	}

	public void setShouldDrop(boolean bool) {
		shouldDrop = bool;
	}

	public void setHasAirResistance(boolean bool) {
		this.hasAirResistance = bool;
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		int meta = tag.getByte(TAG_BLOCK_META) & 255;

		final ResourceLocation blockId = NbtUtils.readResourceLocation(tag.getCompoundTag(TAG_BLOCK_ID));
		final Block block = Block.blockRegistry.getObject(blockId);
		this.blockState = block.getStateFromMeta(meta);

		if (tag.hasKey(TAG_TILE_ENTITY, Constants.NBT.TAG_COMPOUND)) this.tileEntity = tag.getCompoundTag(TAG_TILE_ENTITY);
		else this.tileEntity = null;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		final Block block = blockState.getBlock();
		final ResourceLocation blockId = Block.blockRegistry.getNameForObject(block);
		tag.setTag(TAG_BLOCK_ID, NbtUtils.store(blockId));
		tag.setByte(TAG_BLOCK_META, (byte)block.getMetaFromState(this.blockState));
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
		moveEntity(motionX, motionY, motionZ);

		final Block block = blockState.getBlock();
		if (block == null) setDead();
		else setHeight((float)block.getBlockBoundsMaxY());

		if (worldObj instanceof WorldServer && shouldPlaceBlock()) {
			final BlockPos dropPos = new BlockPos(posX, posY, posZ);
			if (!tryPlaceBlock((WorldServer)worldObj, dropPos)) dropBlock();

			setDead();
		}
	}

	protected boolean shouldPlaceBlock() {
		return onGround && shouldDrop;
	}

	private boolean tryPlaceBlock(final WorldServer world, final BlockPos pos) {
		return FakePlayerPool.instance.executeOnPlayer(world, new PlayerUserReturning<Boolean>() {

			@Override
			public Boolean usePlayer(OpenModsFakePlayer fakePlayer) {
				if (tryPlaceBlock(fakePlayer, world, pos, EnumFacing.DOWN)) return true;

				for (EnumFacing dir : PLACE_DIRECTIONS) {
					if (tryPlaceBlock(fakePlayer, world, pos, dir)) return true;
				}
				return false;
			}
		});

	}

	private boolean tryPlaceBlock(EntityPlayer player, WorldServer world, BlockPos pos, EnumFacing fromSide) {
		if (!worldObj.isAirBlock(pos)) return false;

		boolean blockPlaced = new BlockManipulator(worldObj, player, pos).place(blockState, fromSide);
		if (!blockPlaced) return false;

		if (tileEntity != null) {
			tileEntity.setInteger("x", pos.getX());
			tileEntity.setInteger("y", pos.getY());
			tileEntity.setInteger("z", pos.getZ());
			TileEntity te = worldObj.getTileEntity(pos);
			te.readFromNBT(tileEntity);
		}

		return true;
	}

	private void dropBlock() {
		final Block block = blockState.getBlock();

		Random rand = worldObj.rand;

		final int count = block.quantityDropped(blockState, 0, rand);
		for (int i = 0; i < count; i++) {
			final Item item = block.getItemDropped(blockState, rand, 0);
			if (item != null) {
				ItemStack toDrop = new ItemStack(item, 1, block.damageDropped(blockState));
				entityDropItem(toDrop, 0.1f);
			}
		}

		if (tileEntity instanceof IInventory) {
			IInventory inv = (IInventory)tileEntity;
			for (int i = 0; i < inv.getSizeInventory(); i++) {
				ItemStack is = inv.getStackInSlot(i);
				if (is != null) entityDropItem(is, 0.1f);
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
		if (!isDead && !worldObj.isRemote) dropBlock();
		setDead();
		return false;
	}

	@Override
	protected void entityInit() {}
}