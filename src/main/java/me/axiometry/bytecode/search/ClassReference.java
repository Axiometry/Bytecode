package me.axiometry.bytecode.search;

public final class ClassReference implements Reference {
	private final String className;

	public ClassReference(String className) {
		this.className = className;
	}

	public String getClassName() {
		return className;
	}
}
