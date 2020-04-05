package openmods.renderer.shaders;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;

public class ShaderProgram {
	private final int program;

	private final List<Integer> shaders;

	private final Object2IntMap<String> uniforms = new Object2IntOpenHashMap<>();

	private final Object2IntMap<String> attributes = new Object2IntOpenHashMap<>();

	ShaderProgram(int program, List<Integer> shaders) {
		this.program = program;
		this.shaders = ImmutableList.copyOf(shaders);
	}

	public void bind() {
		ShaderHelper.methods().glUseProgram(program);
	}

	public void release() {
		ShaderHelper.methods().glUseProgram(0);
	}

	public void destroy() {
		for (Integer shader : shaders)
			ShaderHelper.methods().glDeleteShader(shader);

		ShaderHelper.methods().glUseProgram(0);
		ShaderHelper.methods().glDeleteProgram(program);
	}

	private int getUniformLocation(String uniform) {
		return uniforms.computeIntIfAbsent(uniform, key -> ShaderHelper.methods().glGetUniformLocation(program, key));
	}

	private int getAttributeLocation(String attribute) {
		return attributes.computeIntIfAbsent(attribute, key -> ShaderHelper.methods().glGetAttribLocation(program, key));
	}

	public void uniform1i(String name, int val) {
		final int location = getUniformLocation(name);
		if (location >= 0) ShaderHelper.methods().glUniform1i(location, val);
	}

	public void uniform1f(String name, float val) {
		final int location = getUniformLocation(name);
		if (location >= 0) ShaderHelper.methods().glUniform1f(location, val);
	}

	public void uniform3f(String name, float x, float y, float z) {
		final int location = getUniformLocation(name);
		if (location >= 0) ShaderHelper.methods().glUniform3f(location, x, y, z);
	}

	public int getProgram() {
		return program;
	}

	public void instanceAttributePointer(String attrib, int size, int type, boolean normalized, int stride, long offset) {
		final int index = getAttributeLocation(attrib);
		if (index >= 0) instanceAttributePointer(index, size, type, normalized, stride, offset);
	}

	public void instanceAttributePointer(int index, int size, int type, boolean normalized, int stride, long offset) {
		attributePointer(index, size, type, normalized, stride, offset);
		ArraysHelper.methods().glVertexAttribDivisor(index, 1);
	}

	public void attributePointer(String attrib, int size, int type, boolean normalized, int stride, long offset) {
		final int index = getAttributeLocation(attrib);
		if (index >= 0) attributePointer(index, size, type, normalized, stride, offset);
	}

	public void attributePointer(int index, int size, int type, boolean normalized, int stride, long offset) {
		ShaderHelper.methods().glVertexAttribPointer(index, size, type, normalized, stride, offset);
		ShaderHelper.methods().glEnableVertexAttribArray(index);
	}
}
