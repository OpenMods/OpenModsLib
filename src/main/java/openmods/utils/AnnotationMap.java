package openmods.utils;

import com.google.common.collect.Maps;
import java.lang.annotation.Annotation;
import java.util.Map;

public class AnnotationMap {
	private final Map<Class<? extends Annotation>, Annotation> annotations = Maps.newHashMap();

	public AnnotationMap() {}

	public AnnotationMap(Annotation[] annotations) {
		for (Annotation a : annotations)
			this.annotations.put(a.annotationType(), a);
	}

	public boolean hasAnnotation(Class<? extends Annotation> cls) {
		return annotations.get(cls) != null;
	}

	public <T extends Annotation> T get(Class<? extends T> cls) {
		Annotation a = annotations.get(cls);
		return cls.cast(a);
	}

	public void put(Annotation a) {
		annotations.put(a.annotationType(), a);
	}
}