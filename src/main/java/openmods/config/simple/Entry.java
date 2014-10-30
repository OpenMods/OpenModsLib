package openmods.config.simple;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entry {
	public static final String SAME_AS_FIELD = "";

	public String name() default SAME_AS_FIELD;

	public String[] comment() default {};

	public int version() default 0;
}
