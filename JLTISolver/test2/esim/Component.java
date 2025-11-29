package test.esim;

import java.util.regex.Pattern;

import de.m_marvin.unimat.impl.MatrixNd;

public abstract class Component {

	public static final Pattern ID_FILTER = Pattern.compile("[\\w\\d]");
	
	protected final String name;
	
	public Component(String name) {
		if (!name.startsWith(type()))
			throw new IllegalArgumentException("element id must start with type string: " + type());
		if (!ID_FILTER.matcher(name).find())
			throw new IllegalArgumentException("element id must match the filter: " + ID_FILTER.pattern());
		this.name = name;
	}

	public String name() {
		return this.name;
	}
	
	public abstract String type();
	public abstract int[] nodes();
	public int[] vsourceIds() { return new int[0]; }
	public abstract String configInfo();
	
	public abstract void stampMatricies(Network.Context ctx, MatrixNd A, MatrixNd E, MatrixNd z);
	
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
