package openmods.state;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.item.Item;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.StateHolder;

public class ItemState extends StateHolder<Item, ItemState> {
	private final String variant;

	public ItemState(Item objectIn, ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn) {
		super(objectIn, propertiesIn);

		variant = getProperties().stream()
				.map(this::propToString)
				.map(s -> s.toLowerCase(Locale.ROOT))
				.collect(Collectors.joining(","));
	}

	public Item getItem() {
		return object;
	}

	public static StateContainer<Item, ItemState> createContainer(final Item owner, final Consumer<StateContainer.Builder<Item, ItemState>> setup) {
		final StateContainer.Builder<Item, ItemState> builder = new StateContainer.Builder<>(owner);
		setup.accept(builder);
		return builder.create(ItemState::new);
	}

	public static StateContainer<Item, ItemState> createContainer(final Item owner, final IProperty<?>... properties) {
		final StateContainer.Builder<Item, ItemState> builder = new StateContainer.Builder<>(owner);
		builder.add(properties);
		return builder.create(ItemState::new);
	}

	public String getVariant() {
		return variant;
	}

	private <T extends Comparable<T>> String propToString(IProperty<T> prop) {
		return prop.getName(get(prop));
	}
}
