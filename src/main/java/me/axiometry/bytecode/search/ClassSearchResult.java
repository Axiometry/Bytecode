package me.axiometry.bytecode.search;

import me.axiometry.bytecode.ClassSource;

public class ClassSearchResult extends SearchResult {
	private final ClassSource source;
	private final String className;

	public ClassSearchResult(SearchFunction function, Object constant, ClassSource source, String className) {
		super(function, constant);

		this.source = source;
		this.className = className;
	}

	public ClassSource getSource() {
		return source;
	}

	public String getClassName() {
		return className;
	}
}
