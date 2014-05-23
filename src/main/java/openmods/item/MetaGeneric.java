package openmods.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class MetaGeneric implements IMetaItem {

	public static class SmeltingRecipe {
		public final int itemId;
		public final int itemMeta;
		public final ItemStack result;
		public final float experience;

		private SmeltingRecipe(int itemId, int itemMeta, ItemStack result, float experience) {
			this.itemId = itemId;
			this.itemMeta = itemMeta;
			this.result = result.copy();
			this.experience = experience;
		}
	}

	private final String mod;
	private final String name;
	private IIcon icon;
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
	public IIcon getIcon() {
		return icon;
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
	public boolean onItemUse(ItemStack itemStack, EntityPlayer player, World world, int x, int y, int z, int side, float par8, float par9, float par10) {
		return false;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, EntityPlayer player, World world) {
		return itemStack;
	}

	@Override
	public void registerIcons(IIconRegister register) {
		registerIcon(register, name);
	}

	protected void registerIcon(IIconRegister register, String name) {
		icon = register.registerIcon(String.format("%s:%s", mod, name));
	}

	@Override
	public void addRecipe() {
		if (recipes == null) return;

		final FurnaceRecipes furnaceRecipes = FurnaceRecipes.smelting();
		@SuppressWarnings("unchecked")
		final List<IRecipe> craftingRecipes = CraftingManager.getInstance().getRecipeList();
		for (Object tmp : recipes) {
			if (tmp instanceof SmeltingRecipe) {
				SmeltingRecipe recipe = (SmeltingRecipe)tmp;
				furnaceRecipes.addSmelting(recipe.itemId, recipe.itemMeta, recipe.result, recipe.experience);
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
	public boolean hasEffect(int renderPass) {
		return false;
	}

}