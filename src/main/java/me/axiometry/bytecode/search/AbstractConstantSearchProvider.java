package me.axiometry.bytecode.search;

import java.util.*;

import me.axiometry.bytecode.ClassSource;

public abstract class AbstractConstantSearchProvider implements ConstantSearchProvider {
	private static final ClassSource[] EMPTY_SOURCE_ARRAY = new ClassSource[0];

	private final List<ClassSource> sources;

	public AbstractConstantSearchProvider() {
		sources = Collections.synchronizedList(new ArrayList<ClassSource>());
	}

	@Override
	public void addSource(ClassSource source) {
		sources.add(source);
	}

	@Override
	public void removeSource(ClassSource source) {
		sources.remove(source);
	}

	@Override
	public ClassSource[] getSources() {
		return sources.toArray(EMPTY_SOURCE_ARRAY);
	}
}
