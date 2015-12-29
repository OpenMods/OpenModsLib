package openmods.item;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public interface IMetaItem {

	public String getUnlocalizedName(ItemStack stack);

	public boolean hitEntity(ItemStack itemStack, EntityLivingBase target, EntityLivingBase player);

	public boolean onItemUse(ItemStack itemStack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float par8, float par9, float par10);

	public ItemStack onItemRightClick(ItemStack itemStack, EntityPlayer player, World world);

	public void addRecipe();

	public void addToCreativeList(Item item, int meta, List<ItemStack> result);

	public boolean hasEffect();
}
