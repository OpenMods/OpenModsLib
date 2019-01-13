package openmods.geometry;

import java.util.List;
import java.util.Map;

public interface IHitboxSupplier {

	List<Hitbox> asList();

	Map<String, Hitbox> asMap();

}
