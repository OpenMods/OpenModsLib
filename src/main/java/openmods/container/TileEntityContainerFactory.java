package openmods.container;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.IContainerFactory;

public class TileEntityContainerFactory<C extends Container, T extends TileEntity> implements IContainerFactory<C> {
	public TileEntityContainerFactory(Ctor<C, T> ctor, TileEntityType<T> type) {
		this.ctor = ctor;
		this.type = type;
	}

	@FunctionalInterface
	public interface Ctor<C extends Container, T extends TileEntity> {
		C create(final IInventory player, int id, T tileEntity);
	}

	private final Ctor<C, T> ctor;
	private final TileEntityType<T> type;

	@Override
	@SuppressWarnings("unchecked")
	public C create(int windowId, PlayerInventory inv, PacketBuffer data) {
		BlockPos blockPos = data.readBlockPos();
		TileEntity te = inv.player.world.getTileEntity(blockPos);
		if (te == null || te.getType() != type) {
			throw new IllegalStateException("Invalid entity at position " + blockPos + ", expected " + type + ", got " + te);
		}
		return ctor.create(inv, windowId, (T)te);
	}
}
