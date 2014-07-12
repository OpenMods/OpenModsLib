package openmods.network.event;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkEventMeta {
	public boolean compressed() default false;

	public boolean chunked() default false;

	public EventDirection direction() default EventDirection.ANY;
}
