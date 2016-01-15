package openmods.network.event;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkEventMeta {
	public EventDirection direction() default EventDirection.ANY;
}
