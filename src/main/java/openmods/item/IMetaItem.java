package openmods.item;

import javax.annotation.Nonnull;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IMetaItem {

	public String getUnlocalizedName(@Nonnull ItemStack stack);

	public boolean hitEntity(@Nonnull ItemStack itemStack, EntityLivingBase target, EntityLivingBase player);

	public EnumActionResult onItemUse(@Nonnull ItemStack itemStack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ);

	public ActionResult<ItemStack> onItemRightClick(@Nonnull ItemStack itemStack, World world, EntityPlayer player, EnumHand hand);

	public void addToCreativeList(Item item, int meta, NonNullList<ItemStack> result);

	public boolean hasEffect();

	public ResourceLocation getLocation();
}
