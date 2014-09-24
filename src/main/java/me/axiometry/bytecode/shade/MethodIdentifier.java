package me.axiometry.bytecode.shade;

import org.apache.commons.lang.builder.HashCodeBuilder;

public final class MethodIdentifier implements Identifier {
	private final String className, methodName, methodDescriptor;

	public MethodIdentifier(String className, String methodName, String methodDescriptor) {
		this.className = className;
		this.methodName = methodName;
		this.methodDescriptor = methodDescriptor;
	}

	@Override
	public String getClassName() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getMethodDescriptor() {
		return methodDescriptor;
	}

	@Override
	public boolean canShadeInto(Identifier identifier) {
		if(identifier instanceof MethodIdentifier) {
			MethodIdentifier methodIdentifier = (MethodIdentifier) identifier;
			if(className.equals(methodIdentifier.className) && methodDescriptor.equals(methodIdentifier.methodDescriptor))
				return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MethodIdentifier) {
			MethodIdentifier identifier = (MethodIdentifier) obj;
			if(className.equals(identifier.className) && methodName.equals(identifier.methodName) && methodDescriptor.equals(identifier.methodDescriptor))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(className).append(methodName).append(methodDescriptor).toHashCode();
	}

	@Override
	public String toString() {
		return "MethodIdentifier@" + className + "/" + methodName + methodDescriptor;
	}
}
