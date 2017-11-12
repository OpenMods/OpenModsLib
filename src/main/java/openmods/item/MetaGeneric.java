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

public class MetaGeneric implements IMetaItem {

	private final String mod;
	private final String name;

	private boolean visibleInCreative = true;

	public MetaGeneric(String mod, String name) {
		this.mod = mod;
		this.name = name;
	}

	public MetaGeneric hideFromCreative() {
		visibleInCreative = false;
		return this;
	}

	@Override
	public String getUnlocalizedName(@Nonnull ItemStack stack) {
		return String.format("%s.%s", mod, name);
	}

	@Override
	public boolean hitEntity(@Nonnull ItemStack itemStack, EntityLivingBase target, EntityLivingBase player) {
		return false;
	}

	@Override
	public EnumActionResult onItemUse(@Nonnull ItemStack itemStack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		return EnumActionResult.PASS;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(@Nonnull ItemStack itemStack, World world, EntityPlayer player, EnumHand hand) {
		return ActionResult.newResult(EnumActionResult.PASS, itemStack);
	}

	@Override
	public void addToCreativeList(Item item, int meta, NonNullList<ItemStack> result) {
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