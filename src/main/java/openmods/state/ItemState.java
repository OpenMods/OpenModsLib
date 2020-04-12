package openmods.state;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.item.Item;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.state.StateHolder;

public class ItemState extends StateHolder<StateItem, ItemState> {
	private final String variant;

	private ItemState(StateItem objectIn, ImmutableMap<Property<?>, Comparable<?>> propertiesIn, final MapCodec<ItemState> codec) {
		super(objectIn, propertiesIn, codec);

		variant = getProperties().stream()
				.map(this::propToString)
				.map(s -> s.toLowerCase(Locale.ROOT))
				.collect(Collectors.joining(","));
	}

	public Item getItem() {
		return instance;
	}

	public static StateContainer<StateItem, ItemState> createContainer(final StateItem owner, final Consumer<StateContainer.Builder<StateItem, ItemState>> setup) {
		final StateContainer.Builder<StateItem, ItemState> builder = new StateContainer.Builder<>(owner);
		setup.accept(builder);
		return builder.func_235882_a_(StateItem::getDefaultState, ItemState::new);
	}

	public static StateContainer<StateItem, ItemState> createContainer(final StateItem owner, final Property<?>... properties) {
		final StateContainer.Builder<StateItem, ItemState> builder = new StateContainer.Builder<>(owner);
		builder.add(properties);
		return builder.func_235882_a_(StateItem::getDefaultState, ItemState::new);
	}

	public String getVariant() {
		return variant;
	}

	private <T extends Comparable<T>> String propToString(Property<T> prop) {
		return prop.getName(get(prop));
	}
}
