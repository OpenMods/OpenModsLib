package openmods.network.event;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkEventCustomType {
	public Class<? extends INetworkEventType> value();
}
