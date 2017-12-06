package openmods.item;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import openmods.OpenMods;

public abstract class ItemGeneric extends Item {

	protected Map<Integer, IMetaItem> metaitems = Maps.newHashMap();

	public ItemGeneric() {
		setHasSubtypes(true);
		setMaxDamage(0);
	}

	public void registerItems(IMetaItemFactory... factories) {
		for (IMetaItemFactory m : factories)
			if (m.isEnabled()) registerItem(m.getMeta(), m.createMetaItem());
	}

	public void registerItem(int id, IMetaItem item) {
		IMetaItem prev = metaitems.put(id, item);
		Preconditions.checkState(prev == null, "Config error: replacing meta item %s with %s", prev, item);

		OpenMods.proxy.registerCustomItemModel(this, id, item.getLocation());
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		IMetaItem meta = getMeta(stack.getItemDamage());
		if (meta != null) { return "item." + meta.getUnlocalizedName(stack); }
		return "";
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		final ItemStack itemStack = player.getHeldItem(hand);
		IMetaItem meta = getMeta(itemStack.getMetadata());
		return (meta != null)
				? meta.onItemUse(itemStack, player, world, pos, hand, facing, hitX, hitY, hitZ)
				: EnumActionResult.PASS;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		final ItemStack itemStack = player.getHeldItem(hand);
		IMetaItem meta = getMeta(itemStack.getMetadata());
		return meta != null
				? meta.onItemRightClick(itemStack, world, player, hand)
				: ActionResult.newResult(EnumActionResult.PASS, itemStack);
	}

	@Override
	public boolean hitEntity(ItemStack itemStack, EntityLivingBase target, EntityLivingBase player) {
		IMetaItem meta = getMeta(itemStack.getItemDamage());
		if (meta != null) { return meta.hitEntity(itemStack, target, player); }
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasEffect(ItemStack itemStack) {
		IMetaItem meta = getMeta(itemStack.getItemDamage());
		return meta != null? meta.hasEffect() : false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
		if (isInCreativeTab(tab)) {
			for (Entry<Integer, IMetaItem> entry : metaitems.entrySet())
				entry.getValue().addToCreativeList(this, entry.getKey(), subItems);
		}
	}

	public IMetaItem getMeta(int id) {
		return metaitems.get(id);
	}

	public IMetaItem getMeta(@Nonnull ItemStack itemStack) {
		return getMeta(itemStack.getItemDamage());
	}

	@Nonnull
	public ItemStack newItemStack(int id) {
		return newItemStack(id, 1);
	}

	@Nonnull
	public ItemStack newItemStack(int id, int number) {
		return new ItemStack(this, number, id);
	}

	@Nonnull
	public ItemStack newItemStack(IMetaItem meta, int size) {
		for (Entry<Integer, IMetaItem> o : metaitems.entrySet()) {
			if (o.getValue().equals(meta)) { return newItemStack(o.getKey(), size); }
		}
		return ItemStack.EMPTY;
	}
}