package me.axiometry.bytecode.shade;

public interface Identifier {
	public String getClassName();

	public boolean canShadeInto(Identifier identifier);
}
