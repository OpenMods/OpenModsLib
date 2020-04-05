package openmods.structured;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.reflect.Field;
import net.minecraft.util.Util;

public abstract class FieldContainer implements IStructureContainer<IStructureElement> {

	private static final int NULL = -1;

	private final Object2IntMap<Field> fields = Util.make(new Object2IntOpenHashMap<>(), map -> map.defaultReturnValue(NULL));

	@Override
	public void createElements(IElementAddCallback<IStructureElement> callback) {
		for (Field field : getClass().getFields()) {
			field.setAccessible(true);
			if (!field.isAnnotationPresent(StructureField.class)) continue;

			final ElementField fieldWrapper = new ElementField(this, field);
			final int fieldId = callback.addElement(fieldWrapper);
			fields.put(field, fieldId);
		}
	}

	public Integer getElementIdForField(Field field) {
		final int id = fields.getInt(field);
		return id != NULL? id : null;
	}

}
