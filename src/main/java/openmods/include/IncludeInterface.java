package openmods.include;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface IncludeInterface {
	Class<?> value() default Object.class;
}
