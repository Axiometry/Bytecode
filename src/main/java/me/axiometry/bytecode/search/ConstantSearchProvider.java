package me.axiometry.bytecode.search;

import me.axiometry.bytecode.ClassSource;

public interface ConstantSearchProvider {
	public void addSource(ClassSource source);
	public void removeSource(ClassSource source);
	public ClassSource[] getSources();

	public ConstantSearcher search(SearchFunction... functions);

	public interface ConstantSearcher {
		public SearchResult next();

		public SearchFunction[] getFunctions();
		public ClassSource[] getSources();
	}
}
