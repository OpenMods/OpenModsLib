package openmods.integration.modules;

import static openmods.integration.IntegrationConditions.classExists;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import openmods.integration.IntegrationModule;
import openmods.inventory.legacy.CustomSinks;
import openmods.inventory.legacy.CustomSinks.ICustomSink;
import openmods.inventory.legacy.CustomSinks.ICustomSinkProvider;

public class BuildCraftPipes extends IntegrationModule {

	public BuildCraftPipes() {
		super(classExists("buildcraft.api.transport.IPipeTile"));
	}

	public interface BuildCraftAccess {
		public int tryAcceptIntoPipe(TileEntity possiblePipe, ItemStack nextStack, boolean doInsert, EnumFacing direction);

		public boolean isPipe(TileEntity tile);
	}

	private static class Dummy implements BuildCraftAccess {
		@Override
		public int tryAcceptIntoPipe(TileEntity possiblePipe, ItemStack nextStack, boolean doInsert, EnumFacing direction) {
			return 0;
		}

		@Override
		public boolean isPipe(TileEntity tile) {
			return false;
		}
	}

	private static class Live implements BuildCraftAccess {

		@Override
		public int tryAcceptIntoPipe(TileEntity possiblePipe, ItemStack nextStack, boolean doInsert, EnumFacing direction) {
			// if (possiblePipe instanceof IPipeTile) return ((IPipeTile)possiblePipe).injectItem(nextStack, doInsert, direction.getOpposite());
			return 0;
		}

		@Override
		public boolean isPipe(TileEntity tile) {
			return false; // tile instanceof IPipeTile;
		}
	}

	private static BuildCraftAccess access = new Dummy();

	public static BuildCraftAccess access() {
		return access;
	}

	@Override
	public void load() {
		access = new Live();

		CustomSinks.registerCustomSink(new ICustomSinkProvider() {
			@Override
			public CustomSinks.ICustomSink create(final TileEntity te) {
				if (!access.isPipe(te)) return null;

				return new ICustomSink() {
					@Override
					public int accept(ItemStack stack, boolean doInsert, EnumFacing direction) {
						return access.tryAcceptIntoPipe(te, stack, doInsert, direction);
					}
				};
			}
		});
	}

	@Override
	public String name() {
		return "BuildCraft pipes";
	}

}
