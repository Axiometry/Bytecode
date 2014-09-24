package me.axiometry.bytecode.shade;

import org.apache.commons.lang.builder.HashCodeBuilder;

public final class FieldIdentifier implements Identifier {
	private final String className, fieldName, fieldDescriptor;

	public FieldIdentifier(String className, String fieldName, String fieldDescriptor) {
		this.className = className;
		this.fieldName = fieldName;
		this.fieldDescriptor = fieldDescriptor;
	}

	@Override
	public String getClassName() {
		return className;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFieldDescriptor() {
		return fieldDescriptor;
	}

	@Override
	public boolean canShadeInto(Identifier identifier) {
		if(identifier instanceof FieldIdentifier) {
			FieldIdentifier fieldIdentifier = (FieldIdentifier) identifier;
			if(className.equals(fieldIdentifier.className) && fieldDescriptor.equals(fieldIdentifier.fieldDescriptor))
				return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FieldIdentifier) {
			FieldIdentifier identifier = (FieldIdentifier) obj;
			if(className.equals(identifier.className) && fieldName.equals(identifier.fieldName) && fieldDescriptor.equals(identifier.fieldDescriptor))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(className).append(fieldName).toHashCode();
	}

	@Override
	public String toString() {
		return "FieldIdentifier@" + className + "/" + fieldName + "(" + fieldDescriptor + ")";
	}
}
