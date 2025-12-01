package de.m_marvin.electronflow.ltisolver.elements;

import de.m_marvin.electronflow.ltisolver.network.IndexedNetwork.StampingContext;

public abstract class TwoPort extends Element {
	
	protected final String nodeA;
	protected final String nodeB;
	
	protected int nodeAid = 0;
	protected int nodeBid = 0;
	
	public TwoPort(String name, String nodeA, String nodeB) {
		super(name);
		this.nodeA = nodeA;
		this.nodeB = nodeB;
	}

	@Override
	public String[] nodeNames() {
		return new String[] {
			this.nodeA,
			this.nodeB
		};
	}
	
	@Override
	public int[] nodes() {
		return new int[] {
			this.nodeAid,
			this.nodeBid
		};
	}
	
	@Override
	public void index(StampingContext ctx) {
		this.nodeAid = ctx.nodeId(this.nodeA);
		this.nodeBid = ctx.nodeId(this.nodeB);
	}
	
}
