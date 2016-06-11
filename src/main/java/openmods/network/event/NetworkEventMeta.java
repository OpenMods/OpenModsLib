package openmods.network.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkEventMeta {
	public boolean compressed() default false;

	public boolean chunked() default false;

	public EventDirection direction() default EventDirection.ANY;
}
