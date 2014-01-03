package openmods.utils;

import java.lang.annotation.Annotation;
import java.util.Map;

import com.google.common.collect.Maps;

public class AnnotationMap {
	private final Map<Class<? extends Annotation>, Annotation> annotations = Maps.newHashMap();

	public AnnotationMap() {}

	public AnnotationMap(Annotation[] annotations) {
		for (Annotation a : annotations)
			this.annotations.put(a.annotationType(), a);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Class<? extends T> cls) {
		Annotation a = annotations.get(cls);
		return cls.isInstance(a)? (T)a : null;
	}

	public void put(Annotation a) {
		annotations.put(a.annotationType(), a);
	}
}