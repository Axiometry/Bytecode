package me.axiometry.bytecode.search;

public final class MethodReference implements Reference {
	private final String className, methodName, methodDescriptor;

	public MethodReference(String className, String methodName, String methodDescriptor) {
		this.className = className;
		this.methodName = methodName;
		this.methodDescriptor = methodDescriptor;
	}

	public String getClassName() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getMethodDescriptor() {
		return methodDescriptor;
	}
}
