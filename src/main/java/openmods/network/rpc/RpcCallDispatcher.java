package openmods.network.rpc;

import com.google.common.base.Preconditions;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import openmods.OpenMods;
import openmods.network.Dispatcher;
import openmods.network.ExtendedOutboundHandler;
import openmods.network.senders.IPacketSender;
import openmods.utils.CommonRegistryCallbacks;
import openmods.utils.RegistrationContextBase;
import org.objectweb.asm.Type;

@EventBusSubscriber
public class RpcCallDispatcher extends Dispatcher {

	private static RpcCallDispatcher INSTANCE;

	public static RpcCallDispatcher instance() {
		return INSTANCE;
	}

	private static class MethodsCallbacks extends CommonRegistryCallbacks<Method, MethodEntry> {
		@Override
		protected Method getWrappedObject(MethodEntry entry) {
			return entry.method;
		}
	}

	private static class TargetTypeCallbacks extends CommonRegistryCallbacks<Class<? extends IRpcTarget>, TargetTypeProvider> {
		@Override
		protected Class<? extends IRpcTarget> getWrappedObject(TargetTypeProvider entry) {
			return entry.getTargetClass();
		}
	}

	@SubscribeEvent
	public static void registerRegistry(RegistryEvent.NewRegistry e) {
		final IForgeRegistry<MethodEntry> methodRegistry = new RegistryBuilder<MethodEntry>()
				.setIDRange(0, 0x0FFFFF)
				.setName(OpenMods.location("rpc_methods"))
				.setType(MethodEntry.class)
				.addCallback(new MethodsCallbacks())
				.create();

		final IForgeRegistry<TargetTypeProvider> targetRegistry = new RegistryBuilder<TargetTypeProvider>()
				.setIDRange(0, 0xFF)
				.setName(OpenMods.location("rpc_targets"))
				.setType(TargetTypeProvider.class)
				.addCallback(new TargetTypeCallbacks())
				.create();

		INSTANCE = new RpcCallDispatcher(methodRegistry, targetRegistry);
	}

	public static final String CHANNEL_NAME = "OpenMods|RPC";

	public final Senders senders;

	private final RpcProxyFactory proxyFactory;

	private final Map<Side, FMLEmbeddedChannel> channels;

	private RpcCallDispatcher(IForgeRegistry<MethodEntry> methodRegistry, IForgeRegistry<TargetTypeProvider> targetRegistry) {
		this.channels = NetworkRegistry.INSTANCE.newChannel(CHANNEL_NAME, new RpcCallCodec(targetRegistry, methodRegistry), new RpcCallInboundHandler());
		ExtendedOutboundHandler.install(this.channels);

		this.senders = new Senders();

		this.proxyFactory = new RpcProxyFactory(methodRegistry);
	}

	@Override
	protected FMLEmbeddedChannel getChannel(Side side) {
		return channels.get(side);
	}

	public <T> T createProxy(IRpcTarget wrapper, IPacketSender sender, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		return proxyFactory.createProxy(getClass().getClassLoader(), sender, wrapper, mainIntf, extraIntf);
	}

	public static final String ID_FIELDS_SEPARATOR = ";";

	public static class MethodRegistrationContext extends RegistrationContextBase<MethodEntry> {

		public MethodRegistrationContext(IForgeRegistry<MethodEntry> registry, String domain) {
			super(registry, domain);
		}

		public MethodRegistrationContext(IForgeRegistry<MethodEntry> registry) {
			super(registry);
		}

		public MethodRegistrationContext registerInterface(Class<?> intf) {
			Preconditions.checkArgument(intf.isInterface(), "Class %s is not interface", intf);

			for (Method m : intf.getMethods()) {
				if (m.isAnnotationPresent(RpcIgnore.class)) continue;
				Preconditions.checkArgument(m.getReturnType() == void.class, "RPC methods cannot have return type (method = %s)", m);

				final String entry = m.getDeclaringClass().getName() + ID_FIELDS_SEPARATOR + m.getName() + ID_FIELDS_SEPARATOR + Type.getMethodDescriptor(m);
				final ResourceLocation location = new ResourceLocation(domain, entry);

				registry.register(new MethodEntry(m).setRegistryName(location));
			}
			return this;
		}

	}

	public static MethodRegistrationContext startMethodRegistration(IForgeRegistry<MethodEntry> registry) {
		return new MethodRegistrationContext(registry);
	}

	public static MethodRegistrationContext startMethodRegistration(String domain, IForgeRegistry<MethodEntry> registry) {
		return new MethodRegistrationContext(registry, domain);
	}

	public static class TargetRegistrationContext extends RegistrationContextBase<TargetTypeProvider> {

		public TargetRegistrationContext(IForgeRegistry<TargetTypeProvider> registry, String domain) {
			super(registry, domain);
		}

		public TargetRegistrationContext(IForgeRegistry<TargetTypeProvider> registry) {
			super(registry);
		}

		public TargetRegistrationContext registerTargetWrapper(final Class<? extends IRpcTarget> cls) {
			final Constructor<? extends IRpcTarget> ctor;
			try {
				ctor = cls.getConstructor();
			} catch (Exception e) {
				throw new IllegalArgumentException("Class " + cls + " has no parameterless constructor");
			}

			final ResourceLocation targetId = new ResourceLocation(domain, cls.getName());

			registry.register(new TargetTypeProvider() {

				@Override
				public Class<? extends IRpcTarget> getTargetClass() {
					return cls;
				}

				@Override
				public IRpcTarget createRpcTarget() {
					try {
						return ctor.newInstance();
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public String toString() {
					return "RPC target wrapper{" + cls + "}";
				}
			}.setRegistryName(targetId));

			return this;
		}
	}

	public static TargetRegistrationContext startTargetRegistration(IForgeRegistry<TargetTypeProvider> registry) {
		return new TargetRegistrationContext(registry);
	}

	public static TargetRegistrationContext startTargetRegistration(String domain, IForgeRegistry<TargetTypeProvider> registry) {
		return new TargetRegistrationContext(registry, domain);
	}

}
