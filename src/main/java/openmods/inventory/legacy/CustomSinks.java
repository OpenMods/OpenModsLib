package openmods.inventory.legacy;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public class CustomSinks {

	public interface ICustomSink {
		public int accept(ItemStack stack, boolean doInsert, EnumFacing direction);
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
