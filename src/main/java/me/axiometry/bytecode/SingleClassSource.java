package me.axiometry.bytecode;

import java.io.*;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

public class SingleClassSource implements ClassSource {
	private final String name;
	private final byte[] data;

	public SingleClassSource(String name, InputStream in) throws IOException {
		this.name = name;

		data = ByteStreams.toByteArray(in);
	}

	public SingleClassSource(String name, byte[] data) {
		this.name = name;
		this.data = data.clone();
	}

	@Override
	public Map<String, byte[]> getClasses() {
		return ImmutableMap.of(name, data.clone());
	}
}
