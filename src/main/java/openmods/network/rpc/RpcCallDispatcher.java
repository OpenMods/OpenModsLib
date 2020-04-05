package openmods.network.rpc;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.network.ICustomPacket;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import openmods.Log;
import openmods.OpenMods;
import openmods.utils.CommonRegistryCallbacks;
import openmods.utils.RegistrationContextBase;
import openmods.utils.SneakyThrower;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;

@EventBusSubscriber
public class RpcCallDispatcher {

	private static RpcCallDispatcher INSTANCE;

	public static RpcCallDispatcher instance() {
		return INSTANCE;
	}

	private RpcCallCodec codec;

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
				.disableSaving()
				.addCallback(new MethodsCallbacks())
				.create();

		final IForgeRegistry<TargetTypeProvider> targetRegistry = new RegistryBuilder<TargetTypeProvider>()
				.setIDRange(0, 0xFF)
				.setName(OpenMods.location("rpc_targets"))
				.setType(TargetTypeProvider.class)
				.disableSaving()
				.addCallback(new TargetTypeCallbacks())
				.create();

		INSTANCE = new RpcCallDispatcher(methodRegistry, targetRegistry);
	}

	private static final ResourceLocation CHANNEL_ID = OpenMods.location("rpc");
	private static final String PROTOCOL_VERSION = Integer.toString(1);

	private final RpcProxyFactory proxyFactory;

	private RpcCallDispatcher(IForgeRegistry<MethodEntry> methodRegistry, IForgeRegistry<TargetTypeProvider> targetRegistry) {
		this.proxyFactory = new RpcProxyFactory(methodRegistry);
		codec = new RpcCallCodec(targetRegistry, methodRegistry);

		EventNetworkChannel channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_ID)
				.clientAcceptedVersions(PROTOCOL_VERSION::equals)
				.serverAcceptedVersions(PROTOCOL_VERSION::equals)
				.networkProtocolVersion(() -> PROTOCOL_VERSION)
				.eventNetworkChannel();

		channel.addListener(evt -> {
			final NetworkEvent.Context source = evt.getSource().get();
			source.enqueueWork(() -> {
				try {
					executeCall(codec.decode(evt.getPayload(), source));
				} catch (final IOException e) {
					Log.warn(e, "Failed to receive message");
				}
			});
			source.setPacketHandled(true);
		});
	}

	private void executeCall(RpcCall call) {
		try {
			Object target = call.target.getTarget();
			Preconditions.checkNotNull(target, "Target wrapper %s returned null object");
			call.method.method.invoke(target, call.args);
			call.target.afterCall();
		} catch (Throwable t) {
			throw SneakyThrower.sneakyThrow(t);
		}
	}

	public <T> T createProxy(IRpcTarget wrapper, PacketDistributor.PacketTarget sender, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		return proxyFactory.createProxy(getClass().getClassLoader(), wrapSender(sender), wrapper, mainIntf, extraIntf);
	}

	private Consumer<RpcCall> wrapSender(PacketDistributor.PacketTarget sender) {
		return rpcCall -> {
			try {
				final PacketBuffer payload = codec.encode(rpcCall);
				// TODO 1.14 Do something with that int
				final ICustomPacket<IPacket<?>> packet = sender.getDirection().buildPacket(Pair.of(payload, 0), CHANNEL_ID);
				sender.send(packet.getThis());
			} catch (final IOException e) {
				Log.warn(e, "Failed to send RPC call");
			}
		};
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
