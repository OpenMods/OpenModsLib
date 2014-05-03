package openmods.asm;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class FieldMatcher {
	private final String clsName;
	private final String description;
	private final String srgName;
	private final String mcpName;

	public FieldMatcher(String clsName, String description, String mcpName, String srgName) {
		this.clsName = clsName;
		this.description = description;
		this.srgName = srgName;
		this.mcpName = mcpName;
	}

	public boolean match(String fieldName, String fieldDesc) {
		if (!fieldDesc.equals(description)) return false;
		if (fieldName.equals(mcpName)) return true;
		if (!VisitorHelper.useSrgNames()) return false;
		String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(clsName, fieldName, fieldDesc);
		return mapped.equals(srgName);
	}
}