package openmods.structured;

import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.lang.reflect.Field;

public abstract class FieldContainer implements IStructureContainer<IStructureElement> {

	private static final int NULL = -1;

	private final TObjectIntMap<Field> fields = new TObjectIntHashMap<Field>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, NULL);

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
		final int id = fields.get(field);
		return id != NULL? id : null;
	}

}
