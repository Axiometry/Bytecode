package me.axiometry.bytecode.search;

import me.axiometry.bytecode.ClassSource;

public class FieldSearchResult extends ClassSearchResult {
	private final String fieldName;
	private final String fieldDescriptor;

	public FieldSearchResult(SearchFunction function, Object constant, ClassSource source, String className, String fieldName, String fieldDescriptor) {
		super(function, constant, source, className);

		this.fieldName = fieldName;
		this.fieldDescriptor = fieldDescriptor;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFieldDescriptor() {
		return fieldDescriptor;
	}
}
