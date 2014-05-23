package openmods.tileentity;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.block.OpenBlock;
import openmods.network.events.TileEntityMessageEventPacket;
import openmods.utils.Coord;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class OpenTileEntity extends TileEntity {

	private boolean initialized = false;
	private boolean isActive = false;

	private boolean isUsedForClientInventoryRendering = false;

	/** set up the tile entity! called once upon creation */
	public void setup() {
		/** */
	}

	public Coord getPosition() {
		return new Coord(xCoord, yCoord, zCoord);
	}

	/**
	 * Get the current block rotation
	 * 
	 * @return the block rotation
	 */
	public ForgeDirection getRotation() {
		if (isUsedForClientInventoryRendering) { return getBlock().getInventoryRenderRotation(); }
		return ForgeDirection.getOrientation(getMetadata());
	}

	/**
	 * @param block
	 * @param metadata
	 */
	@SideOnly(Side.CLIENT)
	public void prepareForInventoryRender(Block block, int metadata) {
		if (this.worldObj != null) {
			System.out.println("SEVERE PROGRAMMER ERROR! Inventory Render on World TileEntity. Expect hell!");
		} // But of course, we continue, because YOLO.
		isUsedForClientInventoryRendering = true;
		this.blockType = block;
		this.blockMetadata = metadata;
	}

	@Override
	public void updateEntity() {
		isActive = true;
		if (!initialized) {
			initialize();
			initialized = true;
		}
	}

	/**
	 * This is called once the TE has been added to the world
	 */
	protected void initialize() {
		/* only called if you call super.updateEntity() on your updateEntity() */
	}

	public boolean isLoaded() {
		return initialized;
	}

	public boolean isAddedToWorld() {
		return worldObj != null;
	}

	protected boolean isActive() {
		return isActive;
	}

	@Override
	public void onChunkUnload() {
		isActive = false;
	}

	public TileEntity getTileInDirection(ForgeDirection direction) {
		int x = xCoord + direction.offsetX;
		int y = yCoord + direction.offsetY;
		int z = zCoord + direction.offsetZ;
		/*
		 * TODO: Mikee, getBlockTileEntity returns null anyway, why the extra
		 * block check ?
		 */
		if (worldObj != null && worldObj.blockExists(x, y, z)) { return worldObj.getTileEntity(x, y, z); }
		return null;
	}

	@Override
	public String toString() {
		return String.format("%s,%s,%s", xCoord, yCoord, zCoord);
	}

	public boolean isAirBlock(ForgeDirection direction) {
		return worldObj != null
				&& worldObj.isAirBlock(xCoord + direction.offsetX, yCoord
						+ direction.offsetY, zCoord + direction.offsetZ);
	}

	public void sendBlockEvent(int key, int value) {
		worldObj.addBlockEvent(xCoord, yCoord, zCoord, worldObj.getBlock(xCoord, yCoord, zCoord), key, value);
	}

	@Override
	public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, World world, int x, int y, int z) {
		return oldBlock != newBlock;
	}

	public OpenBlock getBlock() {
		/* Hey look what I found */
		if (this.blockType instanceof OpenBlock) { /*
													 * This has broken other
													 * mods in the past, not
													 * this one!
													 */
			return (OpenBlock)this.blockType;
		}
		return OpenBlock.getOpenBlock(worldObj, xCoord, yCoord, zCoord);
	}

	public int getMetadata() {
		if (blockMetadata > -1) { return blockMetadata; }
		return this.blockMetadata = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
	}

	public void openGui(Object instance, EntityPlayer player) {
		player.openGui(instance, -1, worldObj, xCoord, yCoord, zCoord);
	}

	public AxisAlignedBB getBB() {
		return AxisAlignedBB.getAABBPool().getAABB(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 1, zCoord + 1);
	}

	public boolean isRenderedInInventory() {
		return isUsedForClientInventoryRendering;
	}

	public void onEvent(TileEntityMessageEventPacket event) {
		/* when an event is received */
	}
}
