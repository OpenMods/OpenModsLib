package openmods.config.properties;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigProperty {
	public String name() default "";

	public String category();

	public String comment() default "";
}
