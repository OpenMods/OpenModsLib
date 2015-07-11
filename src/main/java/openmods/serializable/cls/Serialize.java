package openmods.serializable.cls;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Serialize {

	public int rank() default 0;

	public boolean nullable() default true;
}