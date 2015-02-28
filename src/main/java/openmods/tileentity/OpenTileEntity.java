package openmods.tileentity;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.api.IInventoryCallback;
import openmods.block.OpenBlock;
import openmods.inventory.GenericInventory;
import openmods.network.DimCoord;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.IRpcTargetProvider;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.TileEntityRpcTarget;
import openmods.network.senders.IPacketSender;
import openmods.reflection.TypeUtils;
import openmods.utils.Coord;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class OpenTileEntity extends TileEntity implements IRpcTargetProvider {

	private boolean initialized = false;
	private boolean isActive = false;

	private boolean isUsedForClientInventoryRendering = false;

	/** Place for TE specific setup. Called once upon creation */
	public void setup() {}

	public Coord getPosition() {
		return new Coord(xCoord, yCoord, zCoord);
	}

	public DimCoord getDimCoords() {
		return new DimCoord(worldObj.provider.dimensionId, xCoord, yCoord, zCoord);
	}

	public ForgeDirection getRotation() {
		final Block block = getBlockType();
		if (!(block instanceof OpenBlock)) return ForgeDirection.NORTH;
		final OpenBlock openBlock = (OpenBlock)block;

		if (isUsedForClientInventoryRendering) return openBlock.getInventoryRenderRotation();
		return openBlock.getRotation(getBlockMetadata());
	}

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

	private TileEntity getTileEntity(int x, int y, int z) {
		return (worldObj != null && worldObj.blockExists(x, y, z))? worldObj.getTileEntity(x, y, z) : null;
	}

	public TileEntity getTileInDirection(ForgeDirection direction) {
		return getNeighbour(direction.offsetX, direction.offsetY, direction.offsetZ);
	}

	public TileEntity getNeighbour(int dx, int dy, int dz) {
		return getTileEntity(xCoord + dx, yCoord + dy, zCoord + dz);
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

	public void openGui(Object instance, EntityPlayer player) {
		player.openGui(instance, -1, worldObj, xCoord, yCoord, zCoord);
	}

	public AxisAlignedBB getBB() {
		return AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 1, zCoord + 1);
	}

	public boolean isRenderedInInventory() {
		return isUsedForClientInventoryRendering;
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
		worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this);
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
		return (worldObj.getTileEntity(xCoord, yCoord, zCoord) == this) && (player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0D);
	}
}
