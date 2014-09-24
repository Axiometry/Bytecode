package me.axiometry.bytecode.search;

public final class FieldReference implements Reference {
	private final String className, fieldName, fieldDescriptor;

	public FieldReference(String className, String fieldName, String fieldDescriptor) {
		this.className = className;
		this.fieldName = fieldName;
		this.fieldDescriptor = fieldDescriptor;
	}

	public String getClassName() {
		return className;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFieldDescriptor() {
		return fieldDescriptor;
	}
}
