package openmods.structured;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class FieldContainer implements IStructureContainer<IStructureElement> {

	private final Map<Field, ElementField> fields = Maps.newHashMap();

	@Override
	public List<IStructureElement> createElements() {
		List<IStructureElement> result = Lists.newArrayList();
		for (Field field : getClass().getFields()) {
			field.setAccessible(true);
			if (!field.isAnnotationPresent(StructureField.class)) continue;

			final ElementField fieldWrapper = new ElementField(this, field);
			result.add(fieldWrapper);
			fields.put(field, fieldWrapper);
		}

		return result;
	}

	public IStructureElement getElementForField(Field field) {
		return fields.get(field);
	}

}
