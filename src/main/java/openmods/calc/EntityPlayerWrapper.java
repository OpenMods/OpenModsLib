package openmods.calc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import info.openmods.calc.types.multi.Cons;
import info.openmods.calc.types.multi.StructWrapper;
import info.openmods.calc.types.multi.StructWrapper.ExposeMethod;
import info.openmods.calc.types.multi.StructWrapper.ExposeProperty;
import info.openmods.calc.types.multi.TypedValue;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import openmods.utils.EnchantmentUtils;

public class EntityPlayerWrapper {

	private final WeakReference<EntityPlayer> player;
	private final TypedValue nullValue;

	public EntityPlayerWrapper(EntityPlayer player, TypedValue nullValue) {
		this.player = new WeakReference<EntityPlayer>(player);
		this.nullValue = nullValue;
	}

	@ExposeProperty
	public Boolean isValid() {
		return this.player.get() != null;
	}

	private EntityPlayer player() {
		final EntityPlayer player = this.player.get();
		Preconditions.checkNotNull(player, "Object no longer valid");
		return player;
	}

	@ExposeProperty
	public String name() {
		return player().getGameProfile().getName();
	}

	@ExposeProperty
	public String uuid() {
		return player().getGameProfile().getId().toString();
	}

	@ExposeProperty
	public Double hp() {
		return Double.valueOf(player().getHealth());
	}

	@ExposeProperty
	public BigInteger xp() {
		return BigInteger.valueOf(EnchantmentUtils.getPlayerXP(player()));
	}

	@ExposeProperty
	public BigInteger level() {
		return BigInteger.valueOf(player().experienceLevel);
	}

	@ExposeProperty
	public Double xpBar() {
		return Double.valueOf(player().experience);
	}

	@ExposeProperty
	public Double x() {
		return player().posX;
	}

	@ExposeProperty
	public Double y() {
		return player().posX;
	}

	@ExposeProperty
	public Double z() {
		return player().posX;
	}

	@ExposeProperty
	public Double vx() {
		return player().motionX;
	}

	@ExposeProperty
	public Double vy() {
		return player().motionY;
	}

	@ExposeProperty
	public Double vz() {
		return player().motionZ;
	}

	@ExposeProperty
	public Double yaw() {
		return Double.valueOf(player().rotationYaw);
	}

	@ExposeProperty
	public Double pitch() {
		return Double.valueOf(player().rotationPitch);
	}

	@ExposeProperty
	public Boolean creative() {
		return player().capabilities.isCreativeMode;
	}

	@ExposeProperty
	public Boolean flying() {
		return player().capabilities.isFlying;
	}

	@ExposeProperty
	public Boolean inAir() {
		return player().isAirBorne;
	}

	@ExposeProperty
	public Boolean burning() {
		return player().isBurning();
	}

	// TODO blocking and other attrs

	public static class WorldWrapper {
		private final WeakReference<World> world;

		public WorldWrapper(World world) {
			this.world = new WeakReference<World>(world);
		}

		public World world() {
			final World world = this.world.get();
			Preconditions.checkNotNull(world, "Object no longer valid");
			return world;
		}

		@ExposeProperty
		public String name() {
			return world().getWorldInfo().getWorldName();
		}

		@ExposeProperty
		public String type() {
			return world().getProviderName();
		}

		@ExposeProperty
		public BigInteger dimension() {
			return BigInteger.valueOf(world().provider.getDimension());
		}

		@ExposeProperty
		public BigInteger totalTime() {
			return BigInteger.valueOf(world().getTotalWorldTime());
		}

		@ExposeProperty
		public BigInteger time() {
			return BigInteger.valueOf(world().getWorldTime());
		}
	}

	@ExposeProperty
	public StructWrapper world() {
		return StructWrapper.create(new WorldWrapper(player().getEntityWorld()));
	}

	@ExposeProperty
	public BigInteger inventorySize() {
		return BigInteger.valueOf(player().inventory.getSizeInventory());
	}

	public static class EnchantmentWrapper {
		private final Enchantment ench;

		@ExposeProperty
		public final BigInteger level;

		public EnchantmentWrapper(Enchantment ench, int level) {
			this.ench = ench;
			this.level = BigInteger.valueOf(level);
		}

		@ExposeProperty
		public String name() {
			return ench.getName();
		}
	}

	public class ItemStackWrapper {
		@Nonnull
		private final ItemStack itemStack;

		public ItemStackWrapper(@Nonnull ItemStack itemStack) {
			this.itemStack = itemStack;
		}

		@ExposeProperty
		public BigInteger count() {
			return BigInteger.valueOf(itemStack.getCount());
		}

		@ExposeProperty
		public BigInteger damage() {
			return BigInteger.valueOf(itemStack.getItemDamage());
		}

		@ExposeProperty
		public String name() {
			return itemStack.getUnlocalizedName();
		}

		@ExposeProperty
		public String displayName() {
			return itemStack.getDisplayName();
		}

		@ExposeProperty(raw = true)
		public TypedValue enchantments() {
			final List<TypedValue> result = Lists.newArrayList();

			final Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(itemStack);

			for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet())
				if (e.getKey() != null)
					result.add(StructWrapper.create(nullValue.domain, new EnchantmentWrapper(e.getKey(), e.getValue())));

			return Cons.createList(result, nullValue);
		}

	}

	@ExposeMethod
	public StructWrapper inventoryItem(BigInteger slotId) {
		final InventoryPlayer inventory = player().inventory;
		final int slot = slotId.intValue();
		Preconditions.checkState(slot >= 0 && slot < inventory.getSizeInventory(), "Invalid slot");
		return StructWrapper.create(new ItemStackWrapper(inventory.getStackInSlot(slot)));
	}

	@ExposeProperty
	public StructWrapper currentItem() {
		final InventoryPlayer inventory = player().inventory;
		return StructWrapper.create(new ItemStackWrapper(inventory.getCurrentItem()));
	}
}
