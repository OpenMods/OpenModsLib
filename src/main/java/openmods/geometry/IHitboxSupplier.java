package openmods.geometry;

import java.util.List;
import java.util.Map;

public interface IHitboxSupplier {

	public List<Hitbox> asList();

	public Map<String, Hitbox> asMap();

}
