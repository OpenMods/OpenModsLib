package openmods.item;

import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class MetaGeneric implements IMetaItem {

	public static class SmeltingRecipe {
		public final ItemStack input;
		public final ItemStack result;
		public final float experience;

		private SmeltingRecipe(ItemStack input, ItemStack result, float experience) {
			this.input = input;
			this.result = result.copy();
			this.experience = experience;
		}
	}

	private final String mod;
	private final String name;

	private Object[] recipes;
	private boolean visibleInCreative = true;

	public MetaGeneric(String mod, String name, Object... recipes) {
		this.mod = mod;
		this.name = name;
		this.recipes = recipes;
	}

	public MetaGeneric hideFromCreative() {
		visibleInCreative = false;
		return this;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return String.format("%s.%s", mod, name);
	}

	@Override
	public boolean hitEntity(ItemStack itemStack, EntityLivingBase target, EntityLivingBase player) {
		return false;
	}

	@Override
	public boolean onItemUse(ItemStack itemStack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
		return false;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, EntityPlayer player, World world) {
		return itemStack;
	}

	@Override
	public void addRecipe() {
		if (recipes == null) return;

		final List<IRecipe> craftingRecipes = CraftingManager.getInstance().getRecipeList();
		for (Object tmp : recipes) {
			if (tmp instanceof SmeltingRecipe) {
				SmeltingRecipe recipe = (SmeltingRecipe)tmp;
				FurnaceRecipes.instance().addSmeltingRecipe(recipe.input, recipe.result, recipe.experience);
			} else if (tmp instanceof IRecipe) {
				craftingRecipes.add((IRecipe)tmp);
			} else throw new IllegalArgumentException("Invalid recipe object: "
					+ tmp);
		}
	}

	@Override
	public void addToCreativeList(Item item, int meta, List<ItemStack> result) {
		if (visibleInCreative) {
			result.add(new ItemStack(item, 1, meta));
		}
	}

	@Override
	public boolean hasEffect() {
		return false;
	}

	@Override
	public ResourceLocation getLocation() {
		return new ResourceLocation(mod, name);
	}

}