package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import openmods.calc.BinaryFunction;
import openmods.calc.Environment;
import openmods.calc.Frame;

public class CapabilitiesChecker {

	private static final String ATTR_CAPABILITIES = "capabilities";

	private static class CapabilityChecker {
		public final String name;
		public final Predicate<MetaObject> predicate;

		public CapabilityChecker(String name, Predicate<MetaObject> predicate) {
			this.name = name;
			this.predicate = predicate;
		}
	}

	private static class CapabilityCheckSymbol extends BinaryFunction<TypedValue> {
		@Override
		protected TypedValue call(TypedValue left, TypedValue right) {
			final MetaObject mo = left.getMetaObject();
			final CapabilityChecker checker = right.as(CapabilityChecker.class, "second 'can' argument");

			return left.domain.create(Boolean.class, checker.predicate.apply(mo));
		}
	}

	public static void register(TypeDomain domain, Environment<TypedValue> env) {
		domain.registerType(CapabilityChecker.class, "capability", createCapabilityMetaObject());

		final Map<String, TypedValue> capablities = Maps.newHashMap();

		for (MetaObject.SlotInfo info : MetaObject.slots) {
			final String lcName = info.name.toLowerCase(Locale.ROOT);
			final CapabilityChecker checker = new CapabilityChecker(lcName, info.isPresent);
			capablities.put(lcName, domain.create(CapabilityChecker.class, checker));
		}

		env.setGlobalSymbol(ATTR_CAPABILITIES, domain.create(SimpleNamespace.class, new SimpleNamespace(capablities)));
		env.setGlobalSymbol("can", new CapabilityCheckSymbol());
	}

	private static MetaObject createCapabilityMetaObject() {
		return MetaObject.builder()
				.set(new MetaObject.SlotDecompose() {
					@Override
					public Optional<List<TypedValue>> tryDecompose(TypedValue self, TypedValue input, int variableCount, Frame<TypedValue> frame) {
						final CapabilityChecker checker = self.as(CapabilityChecker.class);
						final MetaObject mo = input.getMetaObject();

						if (checker.predicate.apply(mo)) {
							List<TypedValue> result = ImmutableList.of(input);
							return Optional.of(result);
						}

						return Optional.absent();
					}
				})
				.set(new MetaObject.SlotStr() {
					@Override
					public String str(TypedValue self, Frame<TypedValue> frame) {
						return ATTR_CAPABILITIES + "." + self.as(CapabilityChecker.class).name;
					}
				})
				.set(new MetaObject.SlotRepr() {
					@Override
					public String repr(TypedValue self, Frame<TypedValue> frame) {
						return ATTR_CAPABILITIES + "." + self.as(CapabilityChecker.class).name;
					}
				})
				.build();
	}

}
