package openmods.network.rpc;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
import openmods.utils.SneakyThrower;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
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
			if (evt instanceof NetworkEvent.LoginPayloadEvent || evt instanceof NetworkEvent.ChannelRegistrationChangeEvent) {
				return;
			}
			final NetworkEvent.Context source = evt.getSource().get();
			try {
				executeCall(codec.decode(evt.getPayload(), source));
			} catch (final IOException e) {
				Log.warn(e, "Failed to receive message");
			}
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
				final ICustomPacket<IPacket<?>> packet = sender.getDirection().buildPacket(Pair.of(payload, 0), CHANNEL_ID);
				sender.send(packet.getThis());
			} catch (final IOException e) {
				Log.warn(e, "Failed to send RPC call");
			}
		};
	}

	public static class MethodRegistrationContext {
		private IForgeRegistry<MethodEntry> registry;

		public MethodRegistrationContext(IForgeRegistry<MethodEntry> registry) {
			this.registry = registry;
		}

		public MethodRegistrationContext registerInterface(final ResourceLocation id, final Class<?> intf) {
			Preconditions.checkArgument(intf.isInterface(), "Class %s is not interface", intf);

			for (Method m : intf.getMethods()) {
				Preconditions.checkArgument(m.getReturnType() == void.class, "RPC methods cannot have return type (method = %s)", m);
				final RpcMethod method = m.getAnnotation(RpcMethod.class);
				if (method != null) {
					final ResourceLocation location = new ResourceLocation(id.getNamespace(), id.getPath() + "/" + method.value());
					registry.register(new MethodEntry(m).setRegistryName(location));
				}
			}
			return this;
		}

	}

	public static MethodRegistrationContext startMethodRegistration(IForgeRegistry<MethodEntry> registry) {
		return new MethodRegistrationContext(registry);
	}

	public static class TargetRegistrationContext {
		private IForgeRegistry<TargetTypeProvider> registry;

		public TargetRegistrationContext(IForgeRegistry<TargetTypeProvider> registry) {
			this.registry = registry;
		}

		public <T extends IRpcTarget> TargetRegistrationContext registerTargetWrapper(final ResourceLocation id, final Class<T> cls, final Supplier<T> ctor) {
			registry.register(new TargetTypeProvider() {
				@Override
				public Class<? extends IRpcTarget> getTargetClass() {
					return cls;
				}

				@Override
				public IRpcTarget createRpcTarget() {
					return ctor.get();
				}

				@Override
				public String toString() {
					return "RPC target wrapper{" + cls + "}";
				}
			}.setRegistryName(id));

			return this;
		}
	}

	public static TargetRegistrationContext startTargetRegistration(IForgeRegistry<TargetTypeProvider> registry) {
		return new TargetRegistrationContext(registry);
	}

}
