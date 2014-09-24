package me.axiometry.bytecode.search;

public abstract class SearchResult {
	private final SearchFunction function;
	private final Object constant;

	public SearchResult(SearchFunction function, Object constant) {
		this.function = function;
		this.constant = constant;
	}

	public SearchFunction getFunction() {
		return function;
	}

	public Object getConstant() {
		return constant;
	}
}