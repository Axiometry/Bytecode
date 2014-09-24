package me.axiometry.bytecode.search;

import me.axiometry.bytecode.ClassSource;

public class MethodSearchResult extends ClassSearchResult {
	private final String methodName;
	private final String methodDescriptor;

	public MethodSearchResult(SearchFunction function, Object constant, ClassSource source, String className, String methodName, String methodDescriptor) {
		super(function, constant, source, className);

		this.methodName = methodName;
		this.methodDescriptor = methodDescriptor;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getMethodDescriptor() {
		return methodDescriptor;
	}
}
