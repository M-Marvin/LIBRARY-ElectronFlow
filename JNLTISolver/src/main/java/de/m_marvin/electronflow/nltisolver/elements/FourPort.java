package de.m_marvin.electronflow.nltisolver.elements;

import de.m_marvin.electronflow.nltisolver.network.IndexedNetwork.StampingContext;

public abstract class FourPort extends Element {

	protected final String nodeA;
	protected final String nodeB;
	protected final String nodeC;
	protected final String nodeD;
	
	protected int nodeAid;
	protected int nodeBid;
	protected int nodeCid;
	protected int nodeDid;
	
	public FourPort(String name, String nodeA, String nodeB, String nodeC, String nodeD) {
		super(name);
		this.nodeA = nodeA;
		this.nodeB = nodeB;
		this.nodeC = nodeC;
		this.nodeD = nodeD;
	}
	
	@Override
	public String[] nodeNames() {
		return new String[] {
			this.nodeA,
			this.nodeB,
			this.nodeC,
			this.nodeD
		};
	}
	
	@Override
	public int[] nodes() {
		return new int[] {
			this.nodeAid,
			this.nodeBid,
			this.nodeCid,
			this.nodeDid
		};
	}

	@Override
	public void index(StampingContext ctx) {
		this.nodeAid = ctx.nodeId(this.nodeA);
		this.nodeBid = ctx.nodeId(this.nodeB);
		this.nodeCid = ctx.nodeId(this.nodeC);
		this.nodeDid = ctx.nodeId(this.nodeD);
	}
	
}
