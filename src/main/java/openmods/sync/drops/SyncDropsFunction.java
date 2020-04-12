package openmods.sync.drops;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootFunction;
import net.minecraft.loot.LootFunctionType;
import net.minecraft.loot.LootParameter;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.tileentity.TileEntity;
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

	@Override
	public LootFunctionType getFunctionType() {
		return OpenMods.SYNC_DROPS_TYPE;
	}

	public static class Serializer extends LootFunction.Serializer<SyncDropsFunction> {
		public SyncDropsFunction deserialize(JsonObject object, JsonDeserializationContext deserializationContext, ILootCondition[] conditionsIn) {
			return new SyncDropsFunction(conditionsIn);
		}

		@Override
		public void serialize(JsonObject p_230424_1_, SyncDropsFunction p_230424_2_, JsonSerializationContext p_230424_3_) {
		}
	}
}
