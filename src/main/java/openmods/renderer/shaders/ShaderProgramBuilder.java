package openmods.renderer.shaders;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.OpenGLException;

public class ShaderProgramBuilder {

	private final Map<ResourceLocation, Integer> shadersToLoad = Maps.newLinkedHashMap();

	public ShaderProgramBuilder() {}

	public void addShader(ResourceLocation source, int type) {
		shadersToLoad.put(source, type);
	}

	public ShaderProgram build() {
		final int program = ShaderHelper.methods().glCreateProgram();
		final List<Integer> shaders = Lists.newArrayList();
		if (program == 0) throw new OpenGLException("Error creating program object");

		for (Map.Entry<ResourceLocation, Integer> e : shadersToLoad.entrySet()) {
			final int shader = createShader(e.getKey(), e.getValue());
			ShaderHelper.methods().glAttachShader(program, shader);
			shaders.add(shader);
		}

		ShaderHelper.methods().glLinkProgram(program);
		if (ShaderHelper.methods().glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) throw new OpenGLException("Shader link error: " + ShaderHelper.methods().getProgramLogInfo(program));

		for (Integer shader : shaders)
			ShaderHelper.methods().glDetachShader(program, shader);

		ShaderHelper.methods().glValidateProgram(program);
		if (ShaderHelper.methods().glGetProgrami(program, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) throw new OpenGLException("Shader validate error: " + ShaderHelper.methods().getProgramLogInfo(program));

		return new ShaderProgram(program, shaders);
	}

	private static int createShader(ResourceLocation source, int type) {
		int shader = 0;
		try {
			shader = ShaderHelper.methods().glCreateShader(type);
			if (shader == 0) throw new OpenGLException("Error creating shader object");

			ShaderHelper.methods().glShaderSource(shader, readShaderSource(source));
			ShaderHelper.methods().glCompileShader(shader);
			if (ShaderHelper.methods().glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) throw new OpenGLException("Shader compile error: " + ShaderHelper.methods().getShaderLogInfo(shader));

			return shader;
		} catch (Throwable t) {
			ShaderHelper.methods().glDeleteShader(shader);
			throw Throwables.propagate(t);
		}
	}

	private static String readShaderSource(ResourceLocation source) {
		try {
			final InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(source).getInputStream();
			final Iterator<String> lines = IOUtils.lineIterator(is, Charsets.UTF_8);
			final StringBuilder out = new StringBuilder();
			Joiner.on('\n').appendTo(out, lines);
			return out.toString();
		} catch (IOException e) {
			throw new OpenGLException("Failed to read resource " + source, e);
		}

	}
}
