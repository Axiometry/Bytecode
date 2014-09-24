package me.axiometry.bytecode.search;

import me.axiometry.bytecode.ClassSource;

public class MethodInstructionSearchResult extends MethodSearchResult {
	private final int opcode;

	public MethodInstructionSearchResult(	SearchFunction function,
	                                     	Object constant,
	                                     	ClassSource source,
	                                     	String className,
	                                     	String methodName,
	                                     	String methodDescriptor,
											int opcode) {
		super(function, constant, source, className, methodName, methodDescriptor);

		this.opcode = opcode;
	}

	public int getOpcode() {
		return opcode;
	}
}
