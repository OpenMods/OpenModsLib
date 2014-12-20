package openmods.inventory.legacy;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.Lists;

public class CustomSinks {

	public interface ICustomSink {
		public int accept(ItemStack stack, boolean doInsert, ForgeDirection direction);
	}

	public interface ICustomSinkProvider {
		public ICustomSink create(TileEntity te);
	}

	private static final List<ICustomSinkProvider> sinkProviders = Lists.newArrayList();

	public static ICustomSink createSink(TileEntity te) {
		for (ICustomSinkProvider provider : sinkProviders) {
			ICustomSink sink = provider.create(te);
			if (sink != null) return sink;
		}
		return null;
	}

	public static void registerCustomSink(ICustomSinkProvider provider) {
		sinkProviders.add(provider);
	}

}
