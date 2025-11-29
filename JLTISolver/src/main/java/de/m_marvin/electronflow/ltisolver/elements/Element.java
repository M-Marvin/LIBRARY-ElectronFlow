package de.m_marvin.electronflow.ltisolver.elements;

import java.util.regex.Pattern;

import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork;
import de.m_marvin.unimat.impl.MatrixNd;

public abstract class Element {

	public static final Pattern ID_FILTER = Pattern.compile("[\\w\\d]");
	
	protected final String name;
	
	public Element(String name) {
		if (!name.startsWith(type()))
			throw new IllegalArgumentException("component id must start with type string: " + type());
		if (!ID_FILTER.matcher(name).find())
			throw new IllegalArgumentException("component id must match the filter: " + ID_FILTER.pattern());
		this.name = name;
	}

	public String name() {
		return this.name;
	}
	
	public abstract String type();
	public abstract int[] nodes();
	public abstract String configInfo();

	public int[] vsourceIds() {
		return new int[0];
	}
	
	public abstract void stampMatricies(IndexedNetwork.StampingContext ctx, MatrixNd A, MatrixNd z);
	
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append(name());
		for (int node : nodes())
			buff.append(String.format("\t%04d", node));
		buff.append('\t').append(configInfo());
		return buff.toString();
	}
	
}
