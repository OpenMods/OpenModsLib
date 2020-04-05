package openmods.recipe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;
import openmods.OpenMods;
import openmods.utils.CollectionUtils;

public class EnchantingRecipe implements ICraftingRecipe {

	public static final ResourceLocation INNER_RECIPE_DUMMY_ID = OpenMods.location("dummy");

	private static final ResourceLocation ENCHANTMENT_EXISTS_ID = OpenMods.location("enchantment_exists");

	public static class EnchantmentExists implements ICondition {
		private final ResourceLocation id;

		public EnchantmentExists(ResourceLocation id) {
			this.id = id;
		}

		@Override
		public ResourceLocation getID() {
			return ENCHANTMENT_EXISTS_ID;
		}

		@Override
		public boolean test() {
			return ForgeRegistries.ENCHANTMENTS.containsKey(id);
		}
	}

	public static class EchantmentExistsConditionSerializer implements IConditionSerializer<EnchantmentExists> {

		@Override
		public void write(JsonObject json, EnchantmentExists value) {
			json.addProperty("id", value.id.toString());
		}

		@Override
		public EnchantmentExists read(JsonObject json) {
			final String id = JSONUtils.getString(json, "id");
			return new EnchantmentExists(new ResourceLocation(id));
		}

		@Override
		public ResourceLocation getID() {
			return ENCHANTMENT_EXISTS_ID;
		}
	}

	@ObjectHolder("openmods:enchanting")
	public static IRecipeSerializer<EnchantingRecipe> ENCHANTING_SERIALIZER;

	public static class Factory extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<EnchantingRecipe> {

		@Override
		public EnchantingRecipe read(ResourceLocation recipeId, JsonObject json) {
			final JsonObject sourceData = JSONUtils.getJsonObject(json, "source");
			@SuppressWarnings("unchecked")
			final IRecipe<CraftingInventory> source = (IRecipe<CraftingInventory>)RecipeManager.deserializeRecipe(INNER_RECIPE_DUMMY_ID, sourceData);

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
				final Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(enchId);
				Preconditions.checkNotNull(ench, "Unknown enchantment: %s", enchId);
				enchantments.add(new EnchantmentData(ench, e.getValue()));
			}

			return new EnchantingRecipe(recipeId, source, enchantments);
		}

		@Override
		public EnchantingRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
			final int enchCount = buffer.readVarInt();
			final List<EnchantmentData> enchantments = Lists.newArrayList();
			for (int i = 0; i < enchCount; i++) {
				final ResourceLocation enchId = buffer.readResourceLocation();
				final int level = buffer.readVarInt();
				enchantments.add(new EnchantmentData(ForgeRegistries.ENCHANTMENTS.getValue(enchId), level));
			}
			final ResourceLocation sourceDeserializer = buffer.readResourceLocation();
			@SuppressWarnings("unchecked")
			final IRecipeSerializer<?> deserializer = ForgeRegistries.RECIPE_SERIALIZERS.getValue(sourceDeserializer);
			@SuppressWarnings("unchecked")
			final IRecipe<CraftingInventory> source = (IRecipe<CraftingInventory>)deserializer.read(INNER_RECIPE_DUMMY_ID, buffer);
			return new EnchantingRecipe(recipeId, source, enchantments);
		}

		@Override
		public void write(PacketBuffer buffer, EnchantingRecipe recipe) {
			buffer.writeVarInt(recipe.enchantments.size());
			for (EnchantmentData enchantmentData : recipe.enchantments) {
				buffer.writeResourceLocation(enchantmentData.enchantment.getRegistryName());
				buffer.writeVarInt(enchantmentData.enchantmentLevel);
			}
			@SuppressWarnings("unchecked")
			final IRecipeSerializer<IRecipe<CraftingInventory>> serializer = (IRecipeSerializer<IRecipe<CraftingInventory>>)recipe.source.getSerializer();
			buffer.writeResourceLocation(serializer.getRegistryName());
			serializer.write(buffer, recipe.source);
		}
	}

	@SubscribeEvent
	public static void registerRecipeSerializers(RegistryEvent.Register<IRecipeSerializer<?>> evt) {
		final IForgeRegistry<IRecipeSerializer<?>> registry = evt.getRegistry();
		registry.register(new EnchantingRecipe.Factory().setRegistryName(OpenMods.location("enchanting")));
	}

	private final IRecipe<CraftingInventory> source;

	private final List<EnchantmentData> enchantments;

	private final ResourceLocation id;

	public EnchantingRecipe(final ResourceLocation id, IRecipe<CraftingInventory> source, List<EnchantmentData> enchantments) {
		this.id = id;
		this.source = source;
		this.enchantments = ImmutableList.copyOf(enchantments);
	}

	private ItemStack enchantItem(ItemStack output) {
		if (output == null) return null;

		if (output.getItem() == Items.BOOK) {
			final CompoundNBT originalTag = output.getTag();
			output = new ItemStack(Items.ENCHANTED_BOOK);
			output.setTag(originalTag != null? originalTag.copy() : null);
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

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public IRecipeSerializer<?> getSerializer() {
		return ENCHANTING_SERIALIZER;
	}

	@Override
	public IRecipeType<?> getType() {
		return IRecipeType.CRAFTING;
	}
}
