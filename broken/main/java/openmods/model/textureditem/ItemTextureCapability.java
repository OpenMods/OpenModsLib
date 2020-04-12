package openmods.model.textureditem;

import java.util.Optional;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class ItemTextureCapability {

	@CapabilityInject(IItemTexture.class)
	public static Capability<IItemTexture> CAPABILITY;

	public static void register() {
		CapabilityManager.INSTANCE.register(IItemTexture.class, new Capability.IStorage<IItemTexture>() {
			@Override
			public INBT writeNBT(Capability<IItemTexture> capability, IItemTexture instance, Direction side) {
				return null;
			}

			@Override
			public void readNBT(Capability<IItemTexture> capability, IItemTexture instance, Direction side, INBT nbt) {}
		}, () -> Optional::empty);
	}

}
