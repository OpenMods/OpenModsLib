package openmods.sync.drops;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.lang.reflect.Field;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import openmods.reflection.FieldAccess;
import openmods.sync.ISyncableObject;
import openmods.utils.ItemUtils;

public class DropTagSerializer {

	private final Map<String, ISyncableObject> objects = Maps.newHashMap();

	public void addObject(String name, ISyncableObject object) {
		ISyncableObject prev = objects.put(name, object);
		Preconditions.checkState(prev == null, "Duplicate on name %s, values = '%s' -> '%s'", name, prev, object);
	}

	public void addFields(Object target) {
		Class<?> cls = target.getClass();
		while (cls != Object.class) {
			for (Field field : cls.getDeclaredFields()) {
				StoreOnDrop marker = field.getAnnotation(StoreOnDrop.class);
				if (marker == null) continue;

				Preconditions.checkArgument(ISyncableObject.class.isAssignableFrom(field.getType()),
						"Field '%s' has SyncableDrop annotation, but isn't ISyncableObject", field);

				final FieldAccess<ISyncableObject> wrappedField = FieldAccess.create(field);
				final ISyncableObject obj = wrappedField.get(target);
				Preconditions.checkNotNull(obj, "Field '%s' contains null", field);

				final String suggestedName = field.getName();
				final String name = Strings.isNullOrEmpty(marker.name())? suggestedName : marker.name();

				addObject(name, obj);
			}
			cls = cls.getSuperclass();
		}
	}

	public void write(NBTTagCompound tag) {
		for (Map.Entry<String, ISyncableObject> e : objects.entrySet())
			e.getValue().writeToNBT(tag, e.getKey());
	}

	public void read(NBTTagCompound tag, boolean skipEmpty) {
		for (Map.Entry<String, ISyncableObject> e : objects.entrySet()) {
			final String key = e.getKey();
			if (!skipEmpty || tag.hasKey(key)) e.getValue().readFromNBT(tag, key);
		}
	}

	@Nonnull
	public ItemStack write(ItemStack stack) {
		NBTTagCompound tag = ItemUtils.getItemTag(stack);
		write(tag);
		return stack;
	}

	@Nonnull
	public void read(ItemStack stack, boolean skipEmpty) {
		NBTTagCompound tag = ItemUtils.getItemTag(stack);
		read(tag, skipEmpty);
	}

}
