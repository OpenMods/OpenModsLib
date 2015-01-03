package openmods.sync.drops;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StoreOnDrop {
	public String name() default "";
}
