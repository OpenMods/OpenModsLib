package openmods.utils.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;

public class BufferedResourceWrapper implements IResource {
	private final IResource resource;
	private final InputStream stream;

	private static InputStream ensureStreamIsBuffered(InputStream is) {
		return is instanceof BufferedInputStream? is : new BufferedInputStream(is);
	}

	public BufferedResourceWrapper(IResource resource) {
		this.resource = resource;
		this.stream = ensureStreamIsBuffered(resource.getInputStream());
	}

	@Override
	public InputStream getInputStream() {
		return stream;
	}

	@Override
	public ResourceLocation getResourceLocation() {
		return resource.getResourceLocation();
	}

	@Override
	public void close() throws IOException {
		resource.close();
	}

	@Override
	public boolean hasMetadata() {
		return resource.hasMetadata();
	}

	@Override
	public <T extends IMetadataSection> T getMetadata(String sectionName) {
		return resource.getMetadata(sectionName);
	}

	@Override
	public String getResourcePackName() {
		return resource.getResourcePackName();
	}

}