package openmods.recipe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.registries.IForgeRegistryEntry;
import openmods.utils.CollectionUtils;

public class EnchantingRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

	// referenced from _factories.json, update when moving!
	public static class Condition implements IConditionFactory {

		@Override
		public BooleanSupplier parse(JsonContext context, JsonObject json) {
			final String enchId = JSONUtils.getString(json, "id");
			return () -> Enchantment.REGISTRY.containsKey(new ResourceLocation(enchId));
		}
	}

	// referenced from _factories.json, update when moving!
	public static class Factory implements IRecipeFactory {

		@Override
		public IRecipe parse(JsonContext context, JsonObject json) {
			final JsonObject sourceData = JSONUtils.getJsonObject(json, "source");
			final IRecipe source = CraftingHelper.getRecipe(sourceData, context);

			final Map<ResourceLocation, Integer> enchantmentMap = Maps.newHashMap();

			final JsonObject enchData = JSONUtils.getJsonObject(json, "enchantments");
			for (Map.Entry<String, JsonElement> e : enchData.entrySet()) {
				final ResourceLocation enchId = new ResourceLocation(e.getKey());
				final int enchLevel = e.getValue().getAsInt();
				CollectionUtils.putOnce(enchantmentMap, enchId, enchLevel);
			}

			final List<EnchantmentData> enchantments = Lists.newArrayList();
			for (Map.Entry<ResourceLocation, Integer> e : enchantmentMap.entrySet()) {
				final ResourceLocation enchId = e.getKey();
				final Enchantment ench = Enchantment.REGISTRY.getObject(enchId);
				Preconditions.checkNotNull(ench, "Unknown enchantment: %s", enchId);
				enchantments.add(new EnchantmentData(ench, e.getValue()));
			}

			return new EnchantingRecipe(source, enchantments);
		}

	}

	private final IRecipe source;

	private final List<EnchantmentData> enchantments;

	public EnchantingRecipe(IRecipe source, List<EnchantmentData> enchantments) {
		this.source = source;
		this.enchantments = ImmutableList.copyOf(enchantments);
	}

	private ItemStack enchantItem(ItemStack output) {
		if (output == null) return null;

		if (output.getItem() == Items.BOOK) {
			final CompoundNBT originalTag = output.getTagCompound();
			output = new ItemStack(Items.ENCHANTED_BOOK);
			output.setTagCompound(originalTag != null? originalTag.copy() : null);
			for (EnchantmentData e : enchantments)
				EnchantedBookItem.addEnchantment(output, e);
		} else if (output.getItem() == Items.ENCHANTED_BOOK) {
			for (EnchantmentData e : enchantments)
				EnchantedBookItem.addEnchantment(output, e);
		} else {
			for (EnchantmentData e : enchantments)
				output.addEnchantment(e.enchantment, e.enchantmentLevel);
		}

		return output;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return enchantItem(source.getRecipeOutput());
	}

	@Override
	public ItemStack getCraftingResult(CraftingInventory inv) {
		return enchantItem(source.getCraftingResult(inv));
	}

	@Override
	public boolean matches(CraftingInventory inv, World worldIn) {
		return source.matches(inv, worldIn);
	}

	@Override
	public boolean canFit(int width, int height) {
		return source.canFit(width, height);
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
		return source.getRemainingItems(inv);
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return source.getIngredients();
	}

	@Override
	public boolean isDynamic() {
		return source.isDynamic();
	}

	@Override
	public String getGroup() {
		return source.getGroup();
	}

}
