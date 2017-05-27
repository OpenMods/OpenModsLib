package openmods.network.rpc;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import net.minecraftforge.fml.common.registry.RegistryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import openmods.OpenMods;
import openmods.network.Dispatcher;
import openmods.network.ExtendedOutboundHandler;
import openmods.network.senders.IPacketSender;
import openmods.utils.CommonRegistryCallbacks;
import org.objectweb.asm.Type;

public class RpcCallDispatcher extends Dispatcher {

	public static final RpcCallDispatcher INSTANCE = new RpcCallDispatcher();

	public static final String CHANNEL_NAME = "OpenMods|RPC";

	public final Senders senders;

	private static class MethodsCallbacks extends CommonRegistryCallbacks<Method, MethodEntry> {
		@Override
		protected Method getWrappedObject(MethodEntry entry) {
			return entry.method;
		}
	}

	private final IForgeRegistry<MethodEntry> methodRegistry = new RegistryBuilder<MethodEntry>()
			.setIDRange(0, 0x0FFFFF)
			.setName(OpenMods.location("rpc_methods"))
			.setType(MethodEntry.class)
			.addCallback(new MethodsCallbacks())
			.create();

	private static class TargetTypeCallbacks extends CommonRegistryCallbacks<Class<? extends IRpcTarget>, TargetTypeProvider> {
		@Override
		protected Class<? extends IRpcTarget> getWrappedObject(TargetTypeProvider entry) {
			return entry.getTargetClass();
		}
	}

	private final IForgeRegistry<TargetTypeProvider> targetRegistry = new RegistryBuilder<TargetTypeProvider>()
			.setIDRange(0, 0xFF)
			.setName(OpenMods.location("rpc_targets"))
			.setType(TargetTypeProvider.class)
			.addCallback(new TargetTypeCallbacks())
			.create();

	private RpcProxyFactory proxyFactory = new RpcProxyFactory(methodRegistry);

	private final Map<Side, FMLEmbeddedChannel> channels;

	private RpcCallDispatcher() {
		this.channels = NetworkRegistry.INSTANCE.newChannel(CHANNEL_NAME, new RpcCallCodec(targetRegistry, methodRegistry), new RpcCallInboundHandler());
		ExtendedOutboundHandler.install(this.channels);

		this.senders = new Senders();
	}

	@Override
	protected FMLEmbeddedChannel getChannel(Side side) {
		return channels.get(side);
	}

	public static final String ID_FIELDS_SEPARATOR = ";";

	private static String getCurrentMod() {
		ModContainer mc = Loader.instance().activeModContainer();
		Preconditions.checkState(mc != null, "This method can be only used during mod initialization");
		String prefix = mc.getModId().toLowerCase();
		return prefix;
	}

	public RpcCallDispatcher registerInterface(Class<?> intf) {
		return registerInterface(getCurrentMod(), intf);
	}

	public RpcCallDispatcher registerInterface(String domain, Class<?> intf) {
		Preconditions.checkArgument(intf.isInterface(), "Class %s is not interface", intf);

		for (Method m : intf.getMethods()) {
			if (m.isAnnotationPresent(RpcIgnore.class)) continue;
			Preconditions.checkArgument(m.getReturnType() == void.class, "RPC methods cannot have return type (method = %s)", m);

			final String entry = m.getDeclaringClass().getName() + ID_FIELDS_SEPARATOR + m.getName() + ID_FIELDS_SEPARATOR + Type.getMethodDescriptor(m);
			final ResourceLocation location = new ResourceLocation(domain, entry);

			methodRegistry.register(new MethodEntry(m).setRegistryName(location));
		}
		return this;
	}

	public <T> T createProxy(IRpcTarget wrapper, IPacketSender sender, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		return proxyFactory.createProxy(getClass().getClassLoader(), sender, wrapper, mainIntf, extraIntf);
	}

	public RpcCallDispatcher registerTargetWrapper(Class<? extends IRpcTarget> targetClass) {
		return registerTargetWrapper(getCurrentMod(), targetClass);
	}

	private RpcCallDispatcher registerTargetWrapper(String domain, final Class<? extends IRpcTarget> cls) {
		final Constructor<? extends IRpcTarget> ctor;
		try {
			ctor = cls.getConstructor();
		} catch (Exception e) {
			throw new IllegalArgumentException("Class " + cls + " has no parameterless constructor");
		}

		final ResourceLocation targetId = new ResourceLocation(domain, cls.getName());

		targetRegistry.register(new TargetTypeProvider() {

			@Override
			public Class<? extends IRpcTarget> getTargetClass() {
				return cls;
			}

			@Override
			public IRpcTarget createRpcTarget() {
				try {
					return ctor.newInstance();
				} catch (Exception e) {
					throw Throwables.propagate(e);
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
