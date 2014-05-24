package openmods.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.utils.BlockNotifyFlags;
import openmods.utils.BlockProperties;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

//TODO: Review later
public class EntityBlock extends Entity implements IEntityAdditionalSpawnData {

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

	public static EntityBlock create(World world, int x, int y, int z) {
		return create(world, x, y, z, EntityBlock.class);
	}

	public static EntityBlock create(World world, int x, int y, int z, Class<? extends EntityBlock> klazz) {

		Block block = world.getBlock(x, y, z);

		if (block == null) return null;

		int meta = world.getBlockMetadata(x, y, z);

		EntityBlock entity = null;
		try {
			entity = klazz.getConstructor(World.class).newInstance(world);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (entity == null) return null;

		entity.setBlockNameAndMeta(BlockProperties.getBlockName(block), meta);

		if (block instanceof BlockContainer) {
			TileEntity te = world.getTileEntity(x, y, z);
			if (te != null) {
				entity.tileEntity = te;
				te.invalidate();
				world.removeTileEntity(x, y, z);
			}
		}

		world.setBlockToAir(x, y, z);

		entity.setPositionAndRotation(x + 0.5, y + 0.5, z + 0.5, 0, 0);

		return entity;
	}

	public TileEntity tileEntity;

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

		String blockName = tag.getString("BlockName");

		Block block = BlockProperties.getBlockByName(blockName);

		if (block == null) {
			setDead();
			return;
		}

		int blockMeta = tag.getInteger("BlockMeta");
		setBlockNameAndMeta(blockName, blockMeta);

		NBTBase teTag = tag.getTag("TileEntity");

		if (teTag instanceof NBTTagCompound) {
			tileEntity = TileEntity.createAndLoadEntity((NBTTagCompound)teTag);
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		tag.setString("BlockName", getBlockName());
		tag.setInteger("BlockMeta", getBlockMeta());

		if (tileEntity != null) {
			NBTTagCompound teTag = new NBTTagCompound();
			tileEntity.writeToNBT(teTag);
			tag.setTag("TileEntity", teTag);
		}
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

		if (shouldPlaceBlock()) {
			int x = MathHelper.floor_double(posX);
			int y = MathHelper.floor_double(posY);
			int z = MathHelper.floor_double(posZ);

			if (!tryPlaceBlock(x, y, z)) {
				dropBlock();
			}

			setDead();
		}
	}

	protected boolean shouldPlaceBlock() {
		return onGround && shouldDrop;
	}

	private boolean tryPlaceBlock(int baseX, int baseY, int baseZ) {
		for (ForgeDirection dir : PLACE_DIRECTIONS) {
			int x = baseX + dir.offsetX;
			int y = baseY + dir.offsetY;
			int z = baseZ + dir.offsetZ;
			if (!worldObj.isAirBlock(x, y, z)) continue;

			worldObj.setBlock(x, y, z, getBlock(), getBlockMeta(), BlockNotifyFlags.ALL);

			if (tileEntity != null) {
				tileEntity.xCoord = x;
				tileEntity.yCoord = y;
				tileEntity.zCoord = z;
				tileEntity.validate();
				worldObj.setTileEntity(x, y, z, tileEntity);
			}
			return true;
		}
		return false;
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
	public void readSpawnData(ByteBuf additionalData) {
		hasGravity = additionalData.readBoolean();
	}

}