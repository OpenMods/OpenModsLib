package openmods.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import openmods.Log;
import openmods.fakeplayer.FakePlayerPool;
import openmods.fakeplayer.FakePlayerPool.PlayerUserReturning;
import openmods.fakeplayer.OpenModsFakePlayer;
import openmods.utils.BlockManipulator;
import openmods.utils.BlockProperties;

//TODO: Review later
public class EntityBlock extends Entity implements IEntityAdditionalSpawnData {

	private static final String TAG_TILE_ENTITY = "TileEntity";
	private static final String TAG_BLOCK_META = "BlockMeta";
	private static final String TAG_BLOCK_NAME = "BlockName";
	private static final int OBJECT_BLOCK_NAME = 11;
	private static final int OBJECT_BLOCK_META = 12;
	private boolean hasGravity = false;
	/* Should this entity return to a block on the ground? */
	private boolean shouldDrop = true;
	private boolean hasAirResistance = true;

	public static final ForgeDirection[] PLACE_DIRECTIONS = { ForgeDirection.UNKNOWN, ForgeDirection.UP, ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST, ForgeDirection.DOWN };

	public EntityBlock(World world) {
		super(world);
		setSize(0.925F, 0.925F);
	}

	private void setHeight(float height) {
		this.height = height;
		yOffset = 0;
	}

	public static EntityBlock create(EntityPlayer player, World world, int x, int y, int z) {
		return create(player, world, x, y, z, EntityBlock.class);
	}

	public static EntityBlock create(EntityPlayer player, World world, int x, int y, int z, Class<? extends EntityBlock> klazz) {

		Block block = world.getBlock(x, y, z);

		if (block.isAir(world, x, y, z)) return null;

		int meta = world.getBlockMetadata(x, y, z);

		final EntityBlock entity;
		try {
			entity = klazz.getConstructor(World.class).newInstance(world);
		} catch (Throwable t) {
			Log.warn(t, "Failed to create EntityBlock(%s) at %d,%d,%d", klazz, x, y, z);
			return null;
		}

		entity.setBlockNameAndMeta(BlockProperties.getBlockName(block), meta);

		final TileEntity te = world.getTileEntity(x, y, z);
		if (te != null) {
			entity.tileEntity = new NBTTagCompound();
			te.writeToNBT(entity.tileEntity);
		}

		final boolean blockRemoved = new BlockManipulator(world, player, x, y, z).setSilentTeRemove(true).remove();
		if (!blockRemoved) return null;

		entity.setPositionAndRotation(x + 0.5, y + 0.5, z + 0.5, 0, 0);

		return entity;
	}

	private NBTTagCompound tileEntity;

	@Override
	protected void entityInit() {
		dataWatcher.addObject(OBJECT_BLOCK_NAME, BlockProperties.getBlockName(Blocks.bedrock));
		dataWatcher.addObject(OBJECT_BLOCK_META, 0);
	}

	public void setBlockNameAndMeta(String name, int meta) {
		this.dataWatcher.updateObject(OBJECT_BLOCK_NAME, name);
		this.dataWatcher.updateObject(OBJECT_BLOCK_META, meta);
	}

	public String getBlockName() {
		return dataWatcher.getWatchableObjectString(OBJECT_BLOCK_NAME);
	}

	public Block getBlock() {
		return BlockProperties.getBlockByName(getBlockName());
	}

	public int getBlockMeta() {
		return dataWatcher.getWatchableObjectInt(OBJECT_BLOCK_META);
	}

	public void setShouldDrop(boolean bool) {
		shouldDrop = bool;
	}

	public void setHasAirResistance(boolean bool) {
		this.hasAirResistance = bool;
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {

		String blockName = tag.getString(TAG_BLOCK_NAME);

		Block block = BlockProperties.getBlockByName(blockName);

		if (block == null) {
			setDead();
			return;
		}

		int blockMeta = tag.getInteger(TAG_BLOCK_META);
		setBlockNameAndMeta(blockName, blockMeta);

		if (tag.hasKey(TAG_TILE_ENTITY, Constants.NBT.TAG_COMPOUND)) this.tileEntity = tag.getCompoundTag(TAG_TILE_ENTITY);
		else this.tileEntity = null;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		tag.setString(TAG_BLOCK_NAME, getBlockName());
		tag.setInteger(TAG_BLOCK_META, getBlockMeta());
		if (tileEntity != null) tag.setTag(TAG_TILE_ENTITY, tileEntity.copy());
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

		Block block = getBlock();
		if (block == null) setDead();
		else setHeight((float)block.getBlockBoundsMaxY());

		if (worldObj instanceof WorldServer && shouldPlaceBlock()) {
			int x = MathHelper.floor_double(posX);
			int y = MathHelper.floor_double(posY);
			int z = MathHelper.floor_double(posZ);

			if (!tryPlaceBlock((WorldServer)worldObj, x, y, z)) dropBlock();

			setDead();
		}
	}

	protected boolean shouldPlaceBlock() {
		return onGround && shouldDrop;
	}

	private boolean tryPlaceBlock(WorldServer world, final int baseX, final int baseY, final int baseZ) {
		return FakePlayerPool.instance.executeOnPlayer(world, new PlayerUserReturning<Boolean>() {

			@Override
			public Boolean usePlayer(OpenModsFakePlayer fakePlayer) {
				for (ForgeDirection dir : PLACE_DIRECTIONS) {
					int x = baseX + dir.offsetX;
					int y = baseY + dir.offsetY;
					int z = baseZ + dir.offsetZ;
					if (!worldObj.isAirBlock(x, y, z)) continue;

					boolean blockPlaced = new BlockManipulator(worldObj, fakePlayer, x, y, z).place(getBlock(), getBlockMeta());
					if (!blockPlaced) continue;

					if (tileEntity != null) {
						tileEntity.setInteger("x", x);
						tileEntity.setInteger("y", y);
						tileEntity.setInteger("z", z);
						TileEntity te = worldObj.getTileEntity(x, y, z);
						te.readFromNBT(tileEntity);
					}
					return true;
				}
				return false;
			}
		});

	}

	private void dropBlock() {
		ItemStack item = new ItemStack(getBlock(), 1, getBlockMeta());

		entityDropItem(item, 0.1f);

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
	@SideOnly(Side.CLIENT)
	public float getShadowSize() {
		return 0.0F;
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
	public void writeSpawnData(ByteBuf data) {
		data.writeBoolean(hasGravity);
	}

	@Override
	public boolean attackEntityFrom(DamageSource p_70097_1_, float p_70097_2_) {
		if (!isDead && !worldObj.isRemote) dropBlock();
		setDead();
		return false;
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		hasGravity = additionalData.readBoolean();
	}
}