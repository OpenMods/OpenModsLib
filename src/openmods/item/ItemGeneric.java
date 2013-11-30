package openmods.item;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class ItemGeneric extends Item {

	protected Map<Integer, IMetaItem> metaitems = Maps.newHashMap();

	public ItemGeneric(int id) {
		super(id);
		setHasSubtypes(true);
		setMaxDamage(0);
	}

	public void registerItem(int id, IMetaItem item) {
		IMetaItem prev = metaitems.put(id, item);
		Preconditions.checkState(prev == null, "Config error: replacing meta item %s with %s", prev, item);
	}

	public void initRecipes() {
		for (IMetaItem item : metaitems.values()) {
			item.addRecipe();
		}
	}

	@Override
	public void registerIcons(IconRegister register) {
		for (IMetaItem item : metaitems.values()) {
			item.registerIcons(register);
		}
	}

	@Override
	public Icon getIconFromDamage(int i) {
		IMetaItem meta = getMeta(i);
		if (meta != null) { return meta.getIcon(); }
		return null;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		IMetaItem meta = getMeta(stack.getItemDamage());
		if (meta != null) { return "item." + meta.getUnlocalizedName(stack); }
		return "";
	}

	@Override
	public boolean onItemUse(ItemStack itemStack, EntityPlayer player, World world, int x, int y, int z, int side, float par8, float par9, float par10) {
		IMetaItem meta = getMeta(itemStack.getItemDamage());
		if (meta != null) { return meta.onItemUse(itemStack, player, world, x, y, z, side, par8, par9, par10); }
		return true;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
		IMetaItem meta = getMeta(itemStack.getItemDamage());
		if (meta != null) { return meta.onItemRightClick(itemStack, player, world); }
		return itemStack;
	}

	@Override
	public boolean hitEntity(ItemStack itemStack, EntityLivingBase target, EntityLivingBase player) {
		IMetaItem meta = getMeta(itemStack.getItemDamage());
		if (meta != null) { return meta.hitEntity(itemStack, target, player); }
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasEffect(ItemStack itemStack, int pass) {
		IMetaItem meta = getMeta(itemStack.getItemDamage());
		return meta != null? meta.hasEffect(pass) : false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void getSubItems(int id, CreativeTabs tab, List subItems) {
		for (Entry<Integer, IMetaItem> entry : metaitems.entrySet())
			entry.getValue().addToCreativeList(id, entry.getKey(), subItems);
	}

	public IMetaItem getMeta(int id) {
		return metaitems.get(id);
	}

	public IMetaItem getMeta(ItemStack itemStack) {
		return getMeta(itemStack.getItemDamage());
	}

	public ItemStack newItemStack(int id) {
		return newItemStack(id, 1);
	}

	public ItemStack newItemStack(int id, int number) {
		return new ItemStack(this, number, id);
	}

	public ItemStack newItemStack(IMetaItem meta, int size) {
		for (Entry<Integer, IMetaItem> o : metaitems.entrySet()) {
			if (o.getValue().equals(meta)) { return newItemStack(o.getKey(), size); }
		}
		return null;
	}
}