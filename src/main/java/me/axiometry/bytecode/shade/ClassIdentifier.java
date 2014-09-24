package me.axiometry.bytecode.shade;

public final class ClassIdentifier implements Identifier {
	private final String className;

	public ClassIdentifier(String className) {
		this.className = className;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public boolean canShadeInto(Identifier identifier) {
		return identifier instanceof ClassIdentifier;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ClassIdentifier && className.equals(((ClassIdentifier) obj).className);
	}

	@Override
	public int hashCode() {
		return className.hashCode();
	}

	@Override
	public String toString() {
		return "ClassIdentifier@" + className;
	}
}
