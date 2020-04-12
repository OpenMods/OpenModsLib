package openmods.sync.drops;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootFunction;
import net.minecraft.world.storage.loot.LootParameter;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraft.world.storage.loot.conditions.ILootCondition;
import openmods.OpenMods;

public class SyncDropsFunction extends LootFunction {
	private SyncDropsFunction(ILootCondition[] conditionsIn) {
		super(conditionsIn);
	}

	@Override
	public Set<LootParameter<?>> getRequiredParameters() {
		return ImmutableSet.of(LootParameters.BLOCK_ENTITY);
	}

	@Override
	protected ItemStack doApply(ItemStack stack, LootContext context) {
		final TileEntity te = context.get(LootParameters.BLOCK_ENTITY);
		if (te instanceof DroppableTileEntity) {
			((DroppableTileEntity)te).getDropSerializer().write(stack);
		}
		return stack;
	}

	public static LootFunction.Builder<?> builder() {
		return builder(SyncDropsFunction::new);
	}

	public static class Serializer extends LootFunction.Serializer<SyncDropsFunction> {
		public Serializer() {
			super(OpenMods.location("sync_drops"), SyncDropsFunction.class);
		}

		public SyncDropsFunction deserialize(JsonObject object, JsonDeserializationContext deserializationContext, ILootCondition[] conditionsIn) {
			return new SyncDropsFunction(conditionsIn);
		}
	}
}
